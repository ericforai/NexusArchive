# Data Ownership Map（数据主权清单）

> 目的：明确每个核心对象的唯一所有者，避免多裁判写入。
> 当前以试点 + Core 现状为基线，逐步细化。
>
> **最后更新**: 2026-03-14
> **更新内容**: 补充新模块的数据所有权

---

## 核心对象与 Owner

| 对象 | Owner 模块 | 权威来源 | 主要写入入口 | 备注 |
| --- | --- | --- | --- | --- |
| **已模块化的对象** |
| Borrowing | BE.BORROWING | `modules/borrowing`（BorrowingMapper） | `BorrowingFacade` | ✅ 模块化完成 |
| Archive | BE.CORE | `ArchiveService` / `acc_archive` | `ArchiveService` | 核心服务，职责集中 |
| SignatureVerificationRecord | BE.SIGNATURE | `modules/signature`（SignatureVerificationRecordMapper） | `SignatureVerificationRecordService` | ✅ 模块化完成 |
| DocumentSection | BE.DOCUMENT | `modules/document`（DocumentSectionMapper） | `DocumentWorkflowService` | ✅ 模块化完成 |
| **Core 对象** |
| User | BE.CORE | `UserMapper` / `sys_user` | `UserService` | 用户主数据 |
| Role | BE.CORE | `RoleMapper` / `sys_role` | `RoleService` | 角色主数据 |
| Org | BE.CORE | `OrgMapper` / `sys_org` | `OrgService` | 组织主数据 |
| AuditLog | BE.CORE | `sys_audit_log` | `AuditLogService` | 合规日志 |
| SystemSetting | BE.CORE | 系统配置表 | `SystemSettingService` | SYS 前端仅消费 |
| Dict | BE.CORE | 数据字典表 | 字典服务 | SYS 前端仅消费 |
| IntegrationConfig | BE.CORE | `sys_erp_config` | `ErpConfigService` | 集成配置 |
| License | BE.CORE | License 存储 | `LicenseService` | 授权 |
| **预归档对象** |
| ArcFileContent | BE.PRE_ARCHIVE | `arc_file_content` | `PreArchiveSubmitService` | ⚠️ 通过 ArchiveMapper 间接写入 Archive |
| ArcFileMetadataIndex | BE.PRE_ARCHIVE | `arc_file_metadata_index` | `PoolService` | 元数据索引 |
| **销毁对象** |
| Destruction | BE.DESTRUCTION | `biz_destruction` | `DestructionService` | ⚠️ 通过 ArchiveMapper 间接更新 Archive |
| DestructionLog | BE.DESTRUCTION | `destruction_log` | `DestructionLogService` | 销毁日志 |
| **审批对象** |
| ArchiveApproval | BE.CORE | `biz_archive_approval` | `ArchiveApprovalService` | 审批流程 |
| **案卷对象** |
| Volume | BE.CORE | `acc_archive_volume` | `VolumeService` | 案卷管理 |

---

## 约束说明

- **唯一写路径**：任何对象的创建/变更必须走 Owner 模块入口。
- **快照允许**：业务侧可保存快照用于审计，但不得回写主数据。
- **待补齐项**：其他业务域对象需逐步补齐 Owner 与写路径。

---

## 跨模块写入（已接受的例外）

以下场景违反"唯一写路径"原则，但经过评估后被接受：

| 写入方 | 对象 | 写入方式 | 原因 | 风险控制 |
|--------|------|----------|------|----------|
| `PreArchiveSubmitService` | Archive | 通过 `ArchiveMapper` 直接写入 | ERP 同步场景，ID 相同时需更新而非创建 | ✅ 事务保护 + 集成测试 |
| `DestructionApprovalServiceImpl` | Archive | 通过 `ArchiveMapper` 更新状态 | 销毁审批流程需要状态机同步 | ✅ 状态机约束 + 集成测试 |
| `ArchiveApprovalServiceImpl` | ArcFileContent | 通过 `PreArchiveSubmitService` 间接更新 | 审批通过触发完成归档 | ✅ @Lazy 避免 + 集成测试 |

**详细分析**: 参见 [Module Dependency Status](module-dependency-status.md)

---

## 模块化进度

| 模块 | 状态 | 说明 |
|------|------|------|
| `modules/borrowing` | ✅ 完成 | Borrowing 模块化标杆 |
| `modules/archivecore` | ✅ 完成 | Archive 核心模块 |
| `modules/signature` | ✅ 完成 | 签名验证模块 |
| `modules/document` | ✅ 完成 | 文档工作流模块 |
| 预归档 | ⚠️ 部分 | PoolService 独立，PreArchiveSubmitService 跨边界 |
| 销毁 | ⚠️ 部分 | DestructionService 独立，但直接操作 ArchiveMapper |
| 审批 | ⚠️ 部分 | ArchiveApprovalService 独立，但调用 PreArchiveSubmitService |
