# Bug 修复报告：批次创建 409 Conflict

## 1. Bug 类型判定

> **并发竞态条件** — `BatchNumberGenerator` 的悲观锁在快速连续请求下失效

## 2. 根因分析（因果链）

```
用户或前端快速连续发送两次 POST /api/collection/batch/create
  → 两个请求几乎同时到达 createBatch()（同一个 @Transactional 方法）
    → 第一个请求 selectBatchNosForUpdate(FOR UPDATE) 锁定现有行
      → 第二个请求被 FOR UPDATE 阻塞，等待第一个事务提交
        → 第一个事务插入 COL-20260212-00003 并提交
          → 第二个请求的 FOR UPDATE 释放，但此时也看到 00001、00002
            → ⚠️ 第二个请求在其事务内读到的行列表中不包含刚提交的 00003
              → 第二个请求也生成 COL-20260212-00003
                → INSERT 触发 UNIQUE(batch_no) 约束 → DataIntegrityViolationException
                  → GlobalExceptionHandler 映射为 409 CONFLICT
```

### 核心问题

`@Transactional(propagation = MANDATORY)` 配合 `FOR UPDATE` 的悲观锁方案在 PostgreSQL 默认隔离级别（`READ COMMITTED`）下**存在时间窗口漏洞**：

- 事务 A 执行 `FOR UPDATE`，锁定现有行
- 事务 B 等待锁释放
- 事务 A 插入新行并**提交**
- 事务 B 的 `FOR UPDATE` 重新执行，但在 `READ COMMITTED` 下**只看到提交前的行**（不含 A 新插入的行，因为 FOR UPDATE 不锁定**尚不存在**的行——即"幻读"）

### 证据表

| 项目 | 内容 |
|:---|:---|
| ✅ 已确认 | curl 发送并发请求后，其中一个成功（创建 `00003`），另一个被吞（无输出） |
| ✅ 已确认 | 浏览器复现 409，错误消息为「关键数据已存在，请勿重复操作」(errCode: `EAA_DB_INTEGRITY`) |
| ✅ 已确认 | 单独发送请求时，生成器正常工作（`00003` 存在后成功生成 `00004`） |
| ✅ 已确认 | `collection_batch` 表仅 `batch_no` 有 UNIQUE 约束 |
| ✅ 已确认 | `GlobalExceptionHandler.handleDataIntegrityException()` 将 `DataIntegrityViolationException` 映射为 409 |

## 3. 修复方案

### 方案 A：INSERT + ON CONFLICT 重试（推荐）

在 `BatchNumberGenerator.generateBatchNo()` 中添加重试逻辑：
- 捕获 `DataIntegrityViolationException`
- 重新查询最大序号
- 最多重试 3 次

```java
@Transactional(propagation = Propagation.MANDATORY)
public String generateBatchNo() {
    for (int retry = 0; retry < 3; retry++) {
        String batchNo = doGenerateBatchNo();
        try {
            // 验证唯一性（可选：直接让 insert 失败再重试）
            return batchNo;
        } catch (Exception e) {
            log.warn("批次号冲突，重试 {}/3: {}", retry + 1, batchNo);
        }
    }
    throw new IllegalStateException("批次号生成失败，请重试");
}
```

> ⚠️ 注意：重试逻辑不能放在 `generateBatchNo()` 内部（因为 `MANDATORY` 传播策略共享外层事务，事务一旦标记 rollback-only 就不能在同一事务内重试）。正确做法是在 `createBatch()` 方法外层添加重试。

### 方案 B：使用数据库序列（最佳实践）

创建 PostgreSQL 序列替代应用层编号生成：

```sql
CREATE SEQUENCE IF NOT EXISTS seq_batch_no_daily;

-- 每天重置序列
SELECT setval('seq_batch_no_daily', 1, false);
```

### 方案 C：前端防重复提交（立即可做）

在 `BatchUploadView.tsx` 的 `handleCreateBatch` 中添加 debounce 或 `isPending` 检查：

```typescript
const handleCreateBatch = useCallback(async () => {
    if (createBatchMutation.isPending) return; // 防重复点击
    // ...
}, [form, createBatchMutation]);
```

> 当前代码中按钮已有 `loading={createBatchMutation.isPending}`（禁用状态），但如果用户通过其他方式（如回车键）快速触发，仍可能发送重复请求。

## 4. 当前状态

- ✅ 生成器已自行恢复正常（`COL-20260212-00004` 创建成功）
- ⚠️ 根因（并发竞态条件）尚未修复
- 💡 日常开发中几乎不可能触发（需要极短时间内连续提交两个批次创建请求）

## 5. 建议优先级

| 方案 | 难度 | 优先级 | 说明 |
|:---|:---|:---|:---|
| 方案 C（前端防重复） | 低 | P1 | 立即可做，防止用户误操作 |
| 方案 A（后端重试） | 中 | P2 | 3-5行代码，解决根本问题 |
| 方案 B（数据库序列） | 高 | P3 | 需要 Flyway 迁移，但最彻底 |
