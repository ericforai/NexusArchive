一旦我所属的文件夹有所变化，请更新我。

## 目录功能
本目录存放系统的 REST API 控制器（Controller）。
负责接收外部 HTTP 请求，进行简单的参数校验，并分发给对应的 Service 层处理。
借阅模块控制器已迁移至 `com.nexusarchive.modules.borrowing.api`。

## 文件清单

| 文件 | 地位 | 功能描述 |
| :--- | :--- | :--- |
| AbnormalVoucherController.java | 控制器类 | 提供异常凭证查询、核销等管理接口 |
| AdminOrgController.java | 控制器类 | 提供组织机构管理及 ERP 同步接口 |
| AdminPermissionController.java | 控制器类 | 提供系统权限元数据管理接口 |
| AdminRoleController.java | 控制器类 | 提供角色管理及权限分配接口 |
| AdminUserController.java | 控制器类 | 提供系统用户账户管理接口 |
| AdvancedArchiveSearchController.java | 控制器类 | 提供多维度的档案高级检索接口 |
| ArchitectureManagementController.java | 控制器类 | 提供系统架构元数据与模块化信息接口 |
| ArchiveApprovalController.java | 控制器类 | 提供归档申请、审核、驳回等流程接口 |
| ArchiveController.java | 控制器类 | 提供档案基础属性查询、更新及生命周期管理接口 |
| ArchiveExportController.java | 控制器类 | 提供档案数据导出任务发起与状态查询接口 |
| ArchiveFileController.java | 控制器类 | 提供档案关联文件的精细化管理接口 |
| ArchiveOperationController.java | 控制器类 | 提供档案作业（如加签、加密）的批量操作接口 |
| ArchivePreviewController.java | 控制器类 | 提供档案多格式预览及水印渲染接口 |
| ArchiveSubmitBatchController.java | 控制器类 | 提供采集批次批量提交归档的专用接口 |
| AsyncMonitorController.java | 控制器类 | 提供异步任务（如四性检测）的状态监控接口 |
| AttachmentController.java | 控制器类 | 提供档案附件的上传、关联与检索接口 |
| AuditLogController.java | 控制器类 | 提供系统审计日志的查询与报表展示接口 |
| AuditLogVerificationController.java | 控制器类 | 提供审计日志哈希链完整性验证接口 |
| AuthController.java | 控制器类 | 提供系统登录、退出、令牌刷新及用户信息接口 |
| AuthTicketController.java | 控制器类 | 提供跨全宗访问授权票据的管理接口 |
| BankReceiptController.java | 控制器类 | 提供银行电子回单的专用查询与下载接口 |
| BasFondsController.java | 控制器类 | 提供全宗元数据查询、配置及切换接口 |
| CertificateController.java | 控制器类 | 提供系统安全证书（电子签名用）的管理接口 |
| CollectionBatchController.java | 控制器类 | 提供手动上传采集批次的生命周期管理接口 |
| ComplianceController.java | 控制器类 | 提供档案合规性（DA/T 94）检查与统计接口 |
| DatabaseFixController.java | 控制器类 | 提供系统维护专用的数据库修复辅助接口 |
| DebugController.java | 控制器类 | 提供开发调试阶段的诊断信息接口 |
| DestructionController.java | 控制器类 | 提供档案销毁申请与全生命周期审计接口 |
| EnterpriseArchitectureController.java | 控制器类 | 提供集团化架构及企业元数据查询接口 |
| EntityConfigController.java | 控制器类 | 提供业务实体（法人）的动态配置接口 |
| EntityController.java | 控制器类 | 提供业务实体树形结构的查询与管理接口 |
| ErpConfigController.java | 控制器类 | 提供第三方 ERP 集成参数的配置接口 |
| ErpScenarioController.java | 控制器类 | 提供 ERP 同步场景的配置、触发与历史查询接口 |
| ErpSsoController.java | 控制器类 | 提供第三方系统单点登录（SSO）集成接口 |
| FondsHistoryController.java | 控制器类 | 提供全宗重命名等关键变更的历史查询接口 |
| GenericYonSuiteController.java | 控制器类 | 提供 YonSuite 集成的通用中转与适配接口 |
| GlobalSearchController.java | 控制器类 | 提供全系统跨模块的全文搜索接口 |
| HealthController.java | 控制器类 | 提供系统的健康检查与依赖状态探测接口 |
| IngestController.java | 控制器类 | 提供标准 SIP 报文入库及异步归档控制接口 |
| LegacyImportController.java | 控制器类 | 提供历史存量档案的批量导入与映射接口 |
| LicenseController.java | 控制器类 | 提供系统授权许可（License）的管理接口 |
| ModuleGovernanceController.java | 控制器类 | 提供系统模块边界与架构防御状态查询接口 |
| MonitoringController.java | 控制器类 | 提供实时性能指标与资源使用情况监控接口 |
| NavController.java | 控制器类 | 提供前端导航菜单与动态路由配置接口 |
| NotificationController.java | 控制器类 | 提供系统内部站内信与通知管理接口 |
| OfdConvertController.java | 控制器类 | 提供国家标准 OFD 格式转换的控制接口 |
| OnboardingController.java | 控制器类 | 提供新全宗入驻与初始化向导接口 |
| OpenAppraisalController.java | 控制器类 | 提供档案开放范围鉴定的业务控制接口 |
| OpsController.java | 控制器类 | 提供档案底层作业（如哈希校验）的操作接口 |
| OriginalVoucherController.java | 控制器类 | 提供原始凭证、业务单据的精细化查询接口 |
| PerformanceMetricsController.java | 控制器类 | 提供系统关键路径响应时间的统计接口 |
| PoolController.java | 控制器类 | 提供记账凭证池的元数据补录、关联与预览接口 |
| PositionController.java | 控制器类 | 提供基于档案实体库房的库位排架管理接口 |
| PreviewController.java | 控制器类 | 提供文件预览、 presigned-url 生成等辅助接口 |
| QualityMonitorController.java | 控制器类 | 提供归档数据质量的实时监控与评分接口 |
| ReconciliationController.java | 控制器类 | 提供自动对账任务的状态监控与手动对齐接口 |
| RelationController.java | 控制器类 | 提供档案知识图谱与全链条关联关系的查询接口 |
| SalesOrderController.java | 控制器类 | 提供销售类原始单据的专用查询与下载接口 |
| ScanFolderMonitorController.java | 控制器类 | 提供扫描文件夹监控与自动采集任务接口 |
| ScanWorkspaceController.java | 控制器类 | 提供基于 Web 的扫描作业工作区管理接口 |
| SignatureController.java | 控制器类 | 提供电子签章验签、证书链验证等管理接口 |
| SqlAuditRuleController.java | 控制器类 | 提供系统 SQL 审计规则与越权探测管理接口 |
| StatsController.java | 控制器类 | 提供系统核心业务指标的图形化统计接口 |
| SystemConfigController.java | 控制器类 | 提供系统全局参数、开关及常量设置接口 |
| TicketSyncController.java | 控制器类 | 提供同步任务票据的状态同步接口 |
| TimestampController.java | 控制器类 | 提供基于 TSA 标准的可信时间戳管理接口 |
| UserController.java | 控制器类 | 提供当前登录用户的个人信息与偏好设置接口 |
| VolumeController.java | 控制器类 | 提供案卷（案卷级档案）的组卷与查询接口 |
| VoucherMatchingController.java | 控制器类 | 提供记账凭证与原始凭证的自动匹配控制接口 |
| WarehouseController.java | 控制器类 | 提供实物档案入库、盘点及库位管理接口 |
| WebIngestController.java | 控制器类 | 提供面向 Web 端上传的轻量级入库接口 |
| WorkflowController.java | 控制器类 | 提供业务流程实例状态查询与干预接口 |
| YonSuiteSsoController.java | 控制器类 | 提供 YonSuite 特定的 SSO 协议适配接口 |
| YundunOidcController.java | 控制器类 | 提供云盾 OIDC 认证协议的深度集成接口 |
| YundunSdkController.java | 控制器类 | 提供云盾安全 SDK 的服务端集成接口 |
