# 技术复盘：2025-12-10 归档审批功能修复

> **问题严重性**: 🔴 高  
> **修复时间**: 2025-12-10  
> **影响范围**: 归档提交、审批流程

---

## 问题一：预归档池 "归档审批中" 计数为 0

### 现象
提交归档后，"归档审批中" 标签的计数始终为 0，尽管审批列表中已有待审批项。

### 根因分析
`PoolController.java` 中的统计查询逻辑错误使用了 `TEMP-POOL-` 前缀过滤：

```java
queryWrapper.likeRight("archival_code", "TEMP-POOL-")
            .eq("pre_archive_status", status);
```

当文件提交归档时，其 `archival_code` 从临时档号 (`TEMP-POOL-XXX`) 更新为正式档号 (`QZ-2025-XXX`)，导致不再匹配 `TEMP-POOL-` 前缀条件，从统计结果中消失。

### 解决方案
修改查询逻辑，基于 `pre_archive_status` 字段判断：

```java
// Before
queryWrapper.likeRight("archival_code", "TEMP-POOL-").eq("pre_archive_status", status);

// After
queryWrapper.eq("pre_archive_status", status);
```

---

## 问题二：审批操作返回 500 错误

### 现象
点击 "批准" 或 "拒绝" 按钮无响应，后端返回 `500 Internal Server Error`。

### 根因分析
错误信息：
```
ERROR: value too long for type character varying(255)
```

`Archive` 实体的 `title` 和 `summary` 字段使用 SM4 加密存储：
```java
@TableField(typeHandler = EncryptTypeHandler.class)
private String title;
```

加密后的数据长度显著膨胀（Base64 编码），超出 `VARCHAR(255)` 限制。

### 解决方案
创建 Flyway 迁移 `V33__increase_archive_column_lengths.sql`：

```sql
ALTER TABLE acc_archive ALTER COLUMN title TYPE VARCHAR(1000);
ALTER TABLE acc_archive ALTER COLUMN summary TYPE VARCHAR(2000);
ALTER TABLE acc_archive ALTER COLUMN creator TYPE VARCHAR(500);
```

---

## 预防措施

1. **加密字段规范**: 使用 SM4 加密的字段，数据库列长度应为明文长度的 **3 倍**。
2. **文档更新**: 已将规范添加至 [数据库治理规范](../guides/database-governance.md)。
3. **代码审查**: 新增加密字段时，必须评估存储长度。

---

## 相关文件

| 文件 | 修改内容 |
|------|----------|
| `PoolController.java` | 移除 `TEMP-POOL-` 前缀过滤 |
| `V33__increase_archive_column_lengths.sql` | 扩展加密字段长度 |
| `database-governance.md` | 新增 SM4 加密字段规范 |
