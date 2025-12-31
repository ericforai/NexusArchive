# 后端核心实体与接口变更通知 (Schema & API Updates)

**发布日期**: 2025-12-31
**涉及模块**: Archive Service, Borrowing Service, Audit Service, Storage Service, Import Service
**优先级**: High (涉及数据库 Schema 变更)

---

## 1. 核心实体变更 (Entity Changes)

### 1.1 `Archive` (档案实体)
新增以下字段以支持销毁流程与合规性要求：
- `destructionStatus` (String): 销毁状态 (`NORMAL`, `APPRAISING`, `FROZEN`, `DESTROYED` 等)。
- `retentionStartDate` (LocalDate): 保管期限起算日期 (对应 `@TableField("retention_start_date")`)。
- `categoryCode` (String): (Existing but emphasized) 明确作为档案门类代码，用于 `ArchiveValidationPolicy`。

### 1.2 `Borrowing` (借阅实体)
新增字段以支持跨全宗查询与销毁校验：
- `fondsNo` (String): 全宗号 (冗余字段，优化查询)。
- `archiveYear` (Integer): 档案年度 (冗余字段，优化查询)。

### 1.3 `BorrowingStatus` (借阅状态枚举)
新增静态方法：
- `borrowedCodes()`: 返回所有表示“在借中”的状态代码列表 (如 `["APPROVED"]`)。

### 1.4 `SysAuditLog` (系统审计日志)
新增字段以支持链路追踪：
- `traceId` (String): 追踪ID (`@TableField("trace_id")`)，用于关联同一业务操作（如导入、销毁）涉及的多条日志。

### 1.5 `Destruction` (销毁申请实体)
新增字段：
- `approvalSnapshot` (String): 审批链快照 (JSON)，用于固化保存审批过程中的签名与意见。

---

## 2. 服务接口变更 (Service API Updates)

### 2.1 `AuditLogService`
新增方法：
- `logCrossFondsAccess(...)`: 专门用于记录跨全宗访问的审计日志，包含源全宗、目标全宗、票据ID等详细信息。

### 2.2 `FileStorageService`
新增方法以完善文件生命周期管理：
- `getFileInfo(String relativePath)`: 获取文件元信息（大小、修改时间）。
- `softDelete(String relativePath)`: 软删除（移动到回收站）。
- `hardDelete(String relativePath)`: 硬删除（物理删除）。

### 2.3 `Result<T>` (通用返回对象)
新增方法：
- `error(String message)`: `fail(String message)` 的别名方法，提供更语义化的错误返回。

---

## 3. 策略与逻辑变更

### 3.1 档案校验策略 (`ArchiveValidationPolicy`)
- 新增 `ArchiveValidationPolicy` 类。
- `ArchiveService` 不再直接包含白名单校验逻辑，改由策略类处理。
- 移除了 `ArchiveService` 中的原生 SQL 查询，全面转向 MyBatis Mapper (`ArcFileContentMapper.selectAttachmentsByArchiveId`)。

### 3.2 历史数据导入 (`LegacyImportServiceImpl`)
- 修复了 `Map.of` 容量限制问题，改用 `Map.ofEntries`。
- 修正了审计日志调用参数，确保合规记录。
- 完善了 `ImportRow` 到 `Archive` 的字段映射，包括 `docDate` 和 `amount`。

---

## 4. 行动指南 (Action Items)

1. **数据库迁移**: 请确保执行最新的 Flyway 迁移脚本 (如涉及)，或确认 JPA/Hibernate 自动更新了 DDL。
2. **代码同步**: 依赖上述实体或服务的模块请及时更新代码，避免 `NoSuchMethodError` 或 `NoSuchFieldError`。
3. **测试**: 涉及档案销毁、借阅、导入功能的测试用例建议重新运行。
