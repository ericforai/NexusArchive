一旦我所属的文件夹有所变化，请更新我。
本目录存放业务服务层。
用于封装核心业务逻辑。
借阅模块服务已迁移至 `com.nexusarchive.modules.borrowing.app`。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `AbnormalVoucherService.java` | 服务接口 | 异常凭证服务 |
| `adapter/` | 目录入口 | adapter 子目录 |
| `ArchivalCodeGenerator.java` | Java 类 | 档案号生成器 |
| `ArchivalPackageService.java` | 服务接口 | AIP 归档信息包服务 |
| `ArchiveApprovalService.java` | 服务接口 | 归档审批服务 |
| `ArchiveExportService.java` | 服务接口 | 档案导出服务 |
| `ArchiveHealthCheckService.java` | 服务接口 | 档案健康检查服务 |
| `ArchiveRelationService.java` | 服务接口 | 档案关联关系服务 |
| `ArchiveSearchService.java` | 服务接口 | 档案搜索服务 |
| `ArchiveSecurityService.java` | 服务接口 | 档案安全服务 |
| `ArchiveService.java` | 服务接口 | 档案核心服务 |
| `AttachmentService.java` | 服务接口 | 附件服务 |
| `AuditLogQueryService.java` | 服务接口 | 审计日志查询服务 |
| `AuditLogService.java` | 服务接口 | 审计日志服务 |
| `AuthService.java` | 服务接口 | 认证服务 |
| `AutoAssociationService.java` | 服务接口 | 自动关联服务 |
| `BasFondsService.java` | 服务接口 | 全宗服务 |
| `ComplianceCheckService.java` | 服务接口 | 合规检查服务 |
| `converter/` | 目录入口 | converter 子目录 |
| `CustomUserDetailsService.java` | 服务接口 | 自定义用户详情服务 |
| `DataScopeService.java` | 服务接口 | 数据权限服务 |
| `DestructionService.java` | 服务接口 | 销毁服务 |
| `DigitalSignatureService.java` | 服务接口 | 数字签名服务 |
| `ErpDiagnosisService.java` | 服务接口 | ERP 诊断服务 |
| `ErpScenarioService.java` | 服务接口 | ERP 场景管理服务 |
| `ErpSubInterfaceService.java` | 服务接口 | ERP 子接口管理服务 |
| `ErpOrgSyncService.java` | 服务接口 | ERP 组织同步服务（从 YonSuite 同步组织架构到 sys_entity 表） |
| `erp/` | 目录入口 | ERP 业务服务子目录 |
| `EntityService.java` | 服务接口 | 法人服务（含树形操作方法） |
| `FileStorageService.java` | 服务接口 | 文件存储服务 |
| `FourNatureCheckService.java` | 服务接口 | 四性检测服务 |
| `FourNatureCoreService.java` | 服务接口 | 四性检测核心服务 |
| `GlobalSearchService.java` | 服务接口 | 全局搜索服务 |
| `IArchiveRelationService.java` | 服务接口 | 档案关联关系接口 |
| `IAutoAssociationService.java` | 服务接口 | 自动关联接口 |
| `impl/` | 目录入口 | impl 子目录 |
| `IngestService.java` | 服务接口 | 归档请求服务 |
| `LicenseService.java` | 服务接口 | License 服务 |
| `LoginAttemptService.java` | 服务接口 | 登录尝试服务 |
| `MonitoringService.java` | 服务接口 | 监控服务 |
| `NotificationService.java` | 服务接口 | 通知服务 |
| `ofd/` | 目录入口 | ofd 子目录 |
| `OfdConvertService.java` | 服务接口 | OFD 转换服务 |
| `OpenAppraisalService.java` | 服务接口 | 开放鉴定服务 |
| `parser/` | 目录入口 | parser 子目录 |
| `PasswordPolicyValidator.java` | Java 类 | 密码策略验证器 |
| `PermissionService.java` | 服务接口 | 权限服务 |
| `PositionService.java` | 服务接口 | 岗位服务 |
| `PreArchiveCheckService.java` | 服务接口 | 预归档检查服务 |
| `PreArchiveSubmitService.java` | 服务接口 | 预归档提交服务（归档完成保留原始格式） |
| `ReconciliationService.java` | 服务接口 | 对账服务 |
| `RoleService.java` | 服务接口 | 角色服务 |
| `RoleValidationService.java` | 服务接口 | 角色验证服务 |
| `search/` | 目录入口 | search 子目录 |
| `signature/` | 目录入口 | signature 子目录 |
| `SmartParserService.java` | 服务接口 | 智能解析服务 |
| `StandardReportGenerator.java` | Java 类 | 标准报表生成器 |
| `StatsService.java` | 服务接口 | 统计服务 |
| `strategy/` | 目录入口 | strategy 子目录 |
| `SystemSettingService.java` | 服务接口 | 系统设置服务 |
| `TimestampService.java` | 服务接口 | 时间戳服务 |
| `TokenBlacklistService.java` | 服务接口 | Token 黑名单服务 |
| `UserService.java` | 服务接口 | 用户服务 |
| `VolumeService.java` | 服务接口 | 案卷服务 |
| `VoucherPdfGeneratorService.java` | 服务接口 | 凭证 PDF 生成服务 |
| `WarehouseService.java` | 服务接口 | 库房服务 |
| `WorkflowService.java` | 服务接口 | 工作流服务 |
