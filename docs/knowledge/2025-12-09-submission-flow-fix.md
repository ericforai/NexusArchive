# 归档申请流程修复与优化总结 (2025-12-09)

## 1. 问题背景
在开发电子凭证池提交归档申请功能时，遇到了以下一系列阻碍性问题：
1. **静默失败**：前端提示“已提交 0 个申请”，但无报错详情。
2. **数据库约束报错**：提交时频繁报 `null value in column "fiscal_year"`、`org_name` 或 `created_at`。
3. **状态流转断裂**：提交成功后文件仍停留在“待归档”列表，用户无感知。
4. **审批流程Bug**：审批页面 UI 错乱，且“批准”操作无法闭环。

## 2. 核心技术复盘

### 2.1 批量操作的错误处理 (Batch Error Handling)
**问题**：后端在 `submitBatchForArchival` 中捕获了单个文件的异常并记录日志，但并未将这些异常反馈给前端。导致前端只能根据返回的成功列表大小（0）来判断。
**解决方案**：
引入 `BatchOperationResult<T>` 泛型类，明确区分“成功项”和“失败项”。
```java
// 旧：只返回成功列表
List<ArchiveApproval> submitBatch(...)

// 新：返回详细结果
Result<BatchOperationResult<ArchiveApproval>> submitBatch(...)
```
前端适配该结构，使用 Toast 清晰展示：“成功 2 个，失败 1 个（原因：文件不存在）”。

### 2.2 实体自动填充失效 (FieldFill.INSERT Failure)
**问题**：即使在 MybatisPlus 实体上加了 `@TableField(fill = FieldFill.INSERT)`，在复杂的 Service 层事务调用中，直接 `new ArchiveApproval()` 并插入可能会导致自动填充失效，引发 `null value in column "created_at"` 错误。
**解决方案**：
在 Service 层**显式**设置关键时间戳字段，确保事务的原子性和数据的完整性。
```java
approval.setCreatedTime(LocalDateTime.now());
approval.setLastModifiedTime(LocalDateTime.now());
```

### 2.3 严格的状态同步 (State Synchronization)
归档流程涉及三个核心实体的状态流转，必须保存强一致性：
1. **ArcFileContent** (预归档文件)
2. **Archive** (正式档案元数据)
3. **ArchiveApproval** (审批记录)

**流程映射**：
*   **提交申请**：
    *   File: `PENDING_ARCHIVE` -> `PENDING_APPROVAL`
    *   Archive: Created (Status: `PENDING`)
    *   Approval: Created (Status: `PENDING`)
*   **审批通过**：
    *   Approval: `PENDING` -> `APPROVED`
    *   Archive: `PENDING` -> `ARCHIVED`
    *   File: `PENDING_APPROVAL` -> `ARCHIVED` (必须同步更新，否则文件会“卡”在中间状态)

## 3. 架构优化点
1. **新增状态枚举**：在 `PreArchiveStatus` 中正式引入 `PENDING_APPROVAL` (归档审批中)，填补了提交后到审批前的状态真空。
2. **UI 健壮性**：针对 SM4 加密的 `ArchiveTitle`（密文极长）导致的表格崩坏，在前端增加了截断显示 (`truncate`)。
3. **数据完整性**：在 `PoolController` 的统计接口中补全了对新状态的统计，防止数据看板与列表不一致。

## 4. 后续建议
*   对于跨表的状态同步，建议未来引入领域事件 (Domain Events) 或简单的观察者模式，解耦 Service 层逻辑。
*   加强对加密字段的前端展示处理，所有可能展示密文的地方都应考虑截断或解密展示。
