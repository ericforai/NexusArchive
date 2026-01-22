# 批量上传与审计日志修复验收报告 (2026-01-21)

## 1. 修复内容概览

本轮修复解决了导致系统 500 错误的核心问题，并优化了审计日志的存储稳定性。

### 🛑 核心修复：后端 500 错误 (NumberFormatException)
- **问题根源**：`CollectionBatchController` 将用户 ID 视为 `Long` 类型，而系统其余部分统一使用 `String`（如 `user_admin_001`）。
- **修复方案**：
  - 重构 `CollectionBatch` 实体、服务及控制器，全面支持 `String` 类型的用户 ID。
  - 增加数据库迁移脚本 `V2026012102__fix_collection_batch_user_id.sql`，将 `collection_batch.created_by` 类型从 `BIGINT` 修正为 `character varying`。
  - 在 `CollectionBatchServiceImpl` 中显式设置 `createdTime` 和 `lastModifiedTime`，解决了因缺失自动填充处理器导致的数据库 `NOT NULL` 约束冲突。
  - 修复 `BatchNumberGenerator` 和 `BatchAuditHelper` 中的类型不匹配。

### 🛡️ 审计日志稳定性 (Data Truncation Fix)
- **问题根源**：`StreamingPreviewServiceImpl` 直接将 `UserDetails.toString()` 存入审计日志的 `username` 列，导致超出 `255` 字符限制。
- **修复方案**：
  - 优化用户名提取逻辑，优先获取 `UserDetails.getUsername()`，确保记录的是纯净的用户名字符串。
  - 同时在 `ArchivalAuditAspect` 中增加了防御性截断逻辑。

### 🧩 系统一致性修复 (Compilation Fix)
- **问题根源**：`OriginalVoucher` 实体中的字段名与多个 Service 调用的方法名不符，导致编译失败。
- **修复方案**：
  - 将 `archivalCategory` 字段重命名为 `voucherCategory`（并保留数据库映射），恢复了 `getVoucherCategory()` 方法。

### 🎨 前端 UI 优化 (AntD Alert)
- **修复内容**：将 `BatchUploadView.tsx` 中弃用的 `Alert` 组件 `message` 属性替换为 `title`，消除了控制台警告。

---

## 2. 验证结果

### 后端健康检查
- 运行状态：`UP`
- API 响应：`200 OK`
- 数据库迁移：已成功应用 `V2026012102`。

### 核心功能测试
- **批次创建**：经模拟测试，使用非数字用户 ID（如 `user_admin_001`）创建批次不再触发 `NumberFormatException`。
- **审计日志**：预览操作产生的审计日志现在能正确记录用户名，且不再因长度超限导致 500 错误。

---

## 3. 专家联合建议

- **合规专家 (Compliance Authority)**：审计日志已恢复正常记录，满足档案管理中对操作人追溯的合规要求。建议定期检查 `sys_audit_log` 的文件完整性。
- **信创架构师 (Xinchuang Architect)**：数据库字段类型调整符合私有化部署的灵活性要求。建议后续统一全系统的 ID 策略，避免混合使用 `Long` 和 `String`。
- **交付专家 (Delivery Strategist)**：本次修复通过 Flyway 自动化迁移，支持离线环境直接升级。

---
> [!IMPORTANT]
> **后续操作**：
> 请刷新前端页面，尝试进行一次完整的批量上传操作以最终确认。
