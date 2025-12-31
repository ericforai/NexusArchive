一旦我所属的文件夹有所变化，请更新我。
本目录存放业务服务层。
用于封装核心业务逻辑。
借阅模块服务已迁移至 `com.nexusarchive.modules.borrowing.app`。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `AbnormalVoucherService.java` | 服务接口 | AbnormalVoucherService 服务定义 |
| `adapter/` | 目录入口 | adapter 子目录 |
| `ArchivalCodeGenerator.java` | Java 类 | ArchivalCodeGenerator 类 |
| `ArchivalPackageService.java` | 服务接口 | ArchivalPackageService 服务定义 |
| `ArchiveApprovalService.java` | 服务接口 | ArchiveApprovalService 服务定义 |
| `ArchiveExportService.java` | 服务接口 | ArchiveExportService 服务定义 |
| `ArchiveHealthCheckService.java` | 服务接口 | ArchiveHealthCheckService 服务定义 |
| `ArchiveRelationService.java` | 服务接口 | ArchiveRelationService 服务定义 |
| `ArchiveSearchService.java` | 服务接口 | ArchiveSearchService 服务定义 |
| `ArchiveSecurityService.java` | 服务接口 | ArchiveSecurityService 服务定义 |
| `ArchiveService.java` | 服务接口 | ArchiveService 服务定义 |
| `AttachmentService.java` | 服务接口 | AttachmentService 服务定义 |
| `AuditLogQueryService.java` | 服务接口 | AuditLogQueryService 服务定义 |
| `AuditLogService.java` | 服务接口 | AuditLogService 服务定义 |
| `AuthService.java` | 服务接口 | AuthService 服务定义 |
| `AutoAssociationService.java` | 服务接口 | AutoAssociationService 服务定义 |
| `BasFondsService.java` | 服务接口 | BasFondsService 服务定义 |
| `ComplianceCheckService.java` | 服务接口 | ComplianceCheckService 服务定义 |
| `converter/` | 目录入口 | converter 子目录 |
| `CustomUserDetailsService.java` | 服务接口 | CustomUserDetailsService 服务定义 |
| `DataScopeService.java` | 服务接口 | DataScopeService 服务定义 |
| `DestructionService.java` | 服务接口 | DestructionService 服务定义 |
| `DigitalSignatureService.java` | 服务接口 | DigitalSignatureService 服务定义 |
| `ErpDiagnosisService.java` | 服务接口 | ErpDiagnosisService 服务定义 |
| `ErpScenarioService.java` | 服务接口 | ERP 场景管理服务（委托协调） |
| `ErpSubInterfaceService.java` | 服务接口 | ERP 子接口管理服务 |
| `erp/` | 目录入口 | ERP 业务服务子目录 |
| `FileStorageService.java` | 服务接口 | FileStorageService 服务定义 |
| `FourNatureCheckService.java` | 服务接口 | FourNatureCheckService 服务定义 |
| `FourNatureCoreService.java` | 服务接口 | FourNatureCoreService 服务定义 |
| `GlobalSearchService.java` | 服务接口 | GlobalSearchService 服务定义 |
| `IArchiveRelationService.java` | 服务接口 | IArchiveRelationService 服务定义 |
| `IAutoAssociationService.java` | 服务接口 | IAutoAssociationService 服务定义 |
| `impl/` | 目录入口 | impl 子目录 |
| `IngestService.java` | 服务接口 | IngestService 服务定义 |
| `LicenseService.java` | 服务接口 | LicenseService 服务定义 |
| `LoginAttemptService.java` | 服务接口 | LoginAttemptService 服务定义 |
| `MonitoringService.java` | 服务接口 | MonitoringService 服务定义 |
| `NotificationService.java` | 服务接口 | NotificationService 服务定义 |
| `ofd/` | 目录入口 | ofd 子目录 |
| `OfdConvertService.java` | 服务接口 | OfdConvertService 服务定义 |
| `OpenAppraisalService.java` | 服务接口 | OpenAppraisalService 服务定义 |
| `OrgService.java` | 服务接口 | OrgService 服务定义 |
| `parser/` | 目录入口 | parser 子目录 |
| `PasswordPolicyValidator.java` | Java 类 | PasswordPolicyValidator 类 |
| `PermissionService.java` | 服务接口 | PermissionService 服务定义 |
| `PositionService.java` | 服务接口 | PositionService 服务定义 |
| `PreArchiveCheckService.java` | 服务接口 | PreArchiveCheckService 服务定义 |
| `PreArchiveSubmitService.java` | 服务接口 | PreArchiveSubmitService 服务定义 |
| `ReconciliationService.java` | 服务接口 | ReconciliationService 服务定义 |
| `RoleService.java` | 服务接口 | RoleService 服务定义 |
| `RoleValidationService.java` | 服务接口 | RoleValidationService 服务定义 |
| `search/` | 目录入口 | search 子目录 |
| `signature/` | 目录入口 | signature 子目录 |
| `SmartParserService.java` | 服务接口 | SmartParserService 服务定义 |
| `StandardReportGenerator.java` | Java 类 | StandardReportGenerator 类 |
| `StatsService.java` | 服务接口 | StatsService 服务定义 |
| `strategy/` | 目录入口 | strategy 子目录 |
| `SystemSettingService.java` | 服务接口 | SystemSettingService 服务定义 |
| `TimestampService.java` | 服务接口 | TimestampService 服务定义 |
| `TokenBlacklistService.java` | 服务接口 | TokenBlacklistService 服务定义 |
| `UserService.java` | 服务接口 | UserService 服务定义 |
| `VolumeService.java` | 服务接口 | VolumeService 服务定义 |
| `VoucherPdfGeneratorService.java` | 服务接口 | VoucherPdfGeneratorService 服务定义 |
| `WarehouseService.java` | 服务接口 | WarehouseService 服务定义 |
| `WorkflowService.java` | 服务接口 | WorkflowService 服务定义 |
