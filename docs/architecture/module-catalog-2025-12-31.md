# NexusArchive 系统模块清单

**版本**: 2.0.0
**生成日期**: 2025-12-31
**系统名称**: 电子会计档案管理系统

---

## 目录

1. [后端模块 (Java/Spring Boot)](#后端模块)
2. [前端模块 (React/TypeScript)](#前端模块)
3. [模块依赖关系](#模块依赖关系)

---

## 后端模块

### 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller 层                           │
│                    (REST API 端点)                          │
├─────────────────────────────────────────────────────────────┤
│                       Service 层                            │
│                   (业务逻辑实现)                             │
├─────────────────────────────────────────────────────────────┤
│                     Integration 层                          │
│                 (外部系统集成)                              │
├─────────────────────────────────────────────────────────────┤
│                      Mapper 层                              │
│                (MyBatis-Plus 数据访问)                      │
├─────────────────────────────────────────────────────────────┤
│                      Entity 层                              │
│                   (数据模型定义)                            │
├─────────────────────────────────────────────────────────────┤
│                       Config 层                             │
│                  (系统配置)                                 │
└─────────────────────────────────────────────────────────────┘
```

---

### 1. Controller 层 - API 端点模块

| 模块名称 | 文件 | 功能描述 | API 前缀 |
|---------|------|---------|----------|
| **认证管理** | | | |
| 认证控制器 | AuthController.java | 用户登录、登出、Token刷新 | /api/auth |
| 票据控制器 | AuthTicketController.java | 认证票据申请、审批、验证 | /api/auth-ticket |
| **档案管理** | | | |
| 档案控制器 | ArchiveController.java | 档案 CRUD、批量操作 | /api/archives |
| 档案审批控制器 | ArchiveApprovalController.java | 档案审批流程 | /api/archives/approval |
| 档案导出控制器 | ArchiveExportController.java | 档案批量导出 | /api/archives/export |
| 档案文件控制器 | ArchiveFileController.java | 文件上传、下载、预览 | /api/archives/files |
| 档案预览控制器 | ArchivePreviewController.java | 在线预览、水印处理 | /api/archives/preview |
| 批次提交控制器 | ArchiveSubmitBatchController.java | 归档批次管理 | /api/batch |
| **搜索与查询** | | | |
| 全局搜索控制器 | GlobalSearchController.java | 跨模块全文搜索 | /api/search |
| 高级搜索控制器 | AdvancedArchiveSearchController.java | 复杂条件查询 | /api/archives/advanced-search |
| 统计控制器 | StatsController.java | 数据统计报表 | /api/stats |
| 性能指标控制器 | PerformanceMetricsController.java | 系统性能监控 | /api/metrics |
| **审计与合规** | | | |
| 审计日志控制器 | AuditLogController.java | 审计日志查询、导出 | /api/audit-logs |
| 审计验真控制器 | AuditLogVerificationController.java | 审计验真报告生成 | /api/audit/verification |
| 合规控制器 | ComplianceController.java | 合规检查、报告 | /api/compliance |
| 对账控制器 | ReconciliationController.java | ERP对账 | /api/reconciliation |
| **销毁管理** | | | |
| 销毁控制器 | DestructionController.java | 销毁计划、执行 | /api/destruction |
| 异常凭证控制器 | AbnormalVoucherController.java | 异常凭证处理 | /api/abnormal-vouchers |
| 原始凭证控制器 | OriginalVoucherController.java | 原始凭证管理 | /api/original-vouchers |
| **ERP 集成** | | | |
| ERP配置控制器 | ErpConfigController.java | ERP连接配置 | /api/erp/config |
| ERP场景控制器 | ErpScenarioController.java | 业务场景配置 | /api/erp/scenarios |
| 摄取控制器 | IngestController.java | 数据摄取接口 | /api/ingest |
| Web摄取控制器 | WebIngestController.java | Web数据摄取 | /api/web-ingest |
| **组织架构** | | | |
| 组织控制器 | AdminOrgController.java | 组织架构管理 | /api/admin/orgs |
| 全宗控制器 | BasFondsController.java | 全宗管理 | /api/fonds |
| 实体控制器 | EntityController.java | 法人实体管理 | /api/entities |
| 实体配置控制器 | EntityConfigController.java | 实体配置管理 | /api/entity-config |
| **用户管理** | | | |
| 用户控制器 | UserController.java | 用户个人设置 | /api/user |
| 管理员用户控制器 | AdminUserController.java | 用户管理 | /api/admin/users |
| 角色控制器 | AdminRoleController.java | 角色管理 | /api/admin/roles |
| 权限控制器 | AdminPermissionController.java | 权限管理 | /api/admin/permissions |
| **系统配置** | | | |
| 系统配置控制器 | SystemConfigController.java | 系统参数配置 | /api/system/config |
| 许可证控制器 | LicenseController.java | 许可证管理 | /api/license |
| 监控控制器 | MonitoringController.java | 系统监控 | /api/monitoring |
| 健康检查控制器 | HealthController.java | 健康检查 | /actuator/health |
| **历史导入** | | | |
| 历史导入控制器 | LegacyImportController.java | 历史数据导入 | /api/legacy-import |

**API 端点总数**: 30+ 个控制器，100+ 个端点

---

### 2. Service 层 - 业务逻辑模块

#### 核心业务服务

| 服务模块 | 文件 | 职责描述 |
|---------|------|---------|
| **档案管理服务组** | | |
| 档案主服务 | ArchiveService.java | 档案 CRUD 核心业务 |
| 档案读取服务 | ArchiveReadService.java | 档案查询、检索 |
| 档案写入服务 | ArchiveWriteService.java | 档案创建、更新 |
| 档案搜索服务 | ArchiveSearchService.java | 全文搜索 |
| 档案安全服务 | ArchiveSecurityService.java | 权限控制、脱敏 |
| 档案冻结服务 | ArchiveFreezeService.java | 冻结/保全操作 |
| 档案健康检查服务 | ArchiveHealthCheckService.java | 完整性检查 |
| **认证授权服务组** | | |
| 认证服务 | AuthService.java | 登录、Token生成 |
| 认证票据服务 | AuthTicketService.java | 票据生命周期 |
| 票据审批服务 | AuthTicketApprovalService.java | 审批流程 |
| 票据验证服务 | AuthTicketValidationService.java | 票据验证 |
| 票据过期服务 | AuthTicketExpirationService.java | 过期处理 |
| 多因素认证服务 | MfaService.java | MFA 配置、验证 |
| **生命周期服务组** | | |
| 档案审批服务 | ArchiveApprovalService.java | 审批工作流 |
| 销毁服务 | DestructionService.java | 销毁计划管理 |
| 销毁审批服务 | DestructionApprovalService.java | 销毁审批 |
| 销毁执行服务 | DestructionExecutionService.java | 销毁执行 |
| 销毁日志服务 | DestructionLogService.java | 销毁记录 |
| 档案评估服务 | ArchiveAppraisalService.java | 价值评估 |
| 开放评估服务 | OpenAppraisalService.java | 开放鉴定 |
| **审计合规服务组** | | |
| 审计日志服务 | AuditLogService.java | 日志记录 |
| 审计日志查询服务 | AuditLogQueryService.java | 日志查询 |
| 审计日志抽样服务 | AuditLogSamplingService.java | 日志抽样 |
| 审计验真服务 | AuditLogVerificationService.java | 验证报告 |
| 审计证据包服务 | AuditEvidencePackageService.java | 证据包生成 |
| 合规检查服务 | ComplianceCheckService.java | 合规规则检查 |
| 四性检查服务 | FourNatureCheckService.java | 四性检测 |
| **用户管理服务组** | | |
| 用户服务 | UserService.java | 用户 CRUD |
| 用户生命周期服务 | UserLifecycleService.java | 入职/离职处理 |
| 登录尝试服务 | LoginAttemptService.java | 登录记录 |
| 令牌黑名单服务 | TokenBlacklistService.java | Token撤销 |
| **组织管理服务组** | | |
| 组织服务 | OrgService.java | 组织架构 |
| 职位服务 | PositionService.java | 职位管理 |
| 角色服务 | RoleService.java | 角色管理 |
| 权限服务 | PermissionService.java | 权限管理 |

#### 模块化拆分服务 (2025-12-31 重构)

| 服务模块 | 原始行数 | 拆分后 | 拆分模块 |
|---------|---------|-------|---------|
| **PDF 生成服务组** | | | |
| VoucherPdfGeneratorService | 1058 | 151 | PaymentPdfGenerator, CollectionPdfGenerator, VoucherPdfGenerator, PdfDataParser, PdfFontLoader, PdfUtils |
| **对账服务组** | | | |
| ReconciliationServiceImpl | 991 | 482 | ErpDataFetcher, ArchiveAggregator, EvidenceVerifier, SubjectExtractor, ReconciliationUtils |
| **案卷服务组** | | | |
| VolumeService | 794 | 139 | VolumeAssembler, VolumeWorkflowService, AipPackageExporter, VolumeQuery, VolumePdfGenerator, VolumeUtils |
| **批处理服务组** | | | |
| ArchiveSubmitBatchServiceImpl | 779 | 186 | BatchManager, BatchItemManager, BatchWorkflowService, FourNatureChecker |
| **历史导入服务组** | | | |
| LegacyImportServiceImpl | 722 | 102 | LegacyFileParser, LegacyDataConverter, LegacyImportOrchestrator, LegacyImportUtils |

#### 技术服务

| 服务模块 | 文件 | 职责描述 |
|---------|------|---------|
| **文件处理服务组** | | |
| 文件存储服务 | FileStorageService.java | 文件存储抽象 |
| 文件存储策略服务 | FileStoragePolicyService.java | 存储策略 |
| 文件哈希去重服务 | FileHashDedupService.java | 文件去重 |
| 附件服务 | AttachmentService.java | 附件管理 |
| **解析服务组** | | |
| OFD 转换服务 | OfdConvertService.java | OFD 格式转换 |
| 智能解析服务 | SmartParserService.java | 智能识别 |
| 发票解析服务 | InvoiceParserService.java | 发票解析 |
| **搜索服务组** | | |
| 全局搜索服务 | GlobalSearchService.java | 跨模块搜索 |
| 高级搜索服务 | AdvancedArchiveSearchService.java | 复杂查询 |
| 档案索引服务 | ArchiveIndexService.java | 索引维护 |
| **工作流服务组** | | |
| 工作流服务 | WorkflowService.java | 工作流引擎 |
| 档案批处理服务 | ArchiveBatchService.java | 批量处理 |

#### ERP 集成服务

| 服务模块 | 文件 | 职责描述 |
|---------|------|---------|
| **ERP 适配器** | | |
| ERP 适配器接口 | ErpAdapter.java | 适配器抽象 |
| ERP 适配器工厂 | ErpAdapterFactory.java | 适配器创建 |
| 金蝶适配器 | KingdeeAdapter.java | 金蝶 ERP 集成 |
| 用友适配器 | WeaverAdapter.java | 用友 ERP 集成 |
| 用友 E10 适配器 | WeaverE10Adapter.java | 用友 E10 集成 |
| 云 suite 适配器 | YonSuiteErpAdapter.java | 云 suite 集成 |
| **ERP 服务** | | |
| ERP 渠道服务 | ErpChannelService.java | 渠道聚合 |
| ERP 同步服务 | ErpSyncService.java | 数据同步 |
| ERP 诊断服务 | ErpDiagnosisService.java | 连接诊断 |
| ERP 场景服务 | ErpScenarioService.java | 场景管理 |

**Service 模块总数**: 80+ 个服务类

---

### 3. Entity 层 - 数据模型模块

#### 核心业务实体

| 实体类别 | 实体名称 | 表名 | 描述 |
|---------|---------|------|------|
| **档案核心** | | | |
| 档案 | Archive.java | acc_archive | 档案主表 |
| 档案附件 | ArchiveAttachment.java | acc_archive_attachment | 附件关联 |
| 档案关联 | ArchiveRelation.java | acc_archive_relation | 关联关系 |
| 档案审批 | ArchiveApproval.java | acc_archive_approval | 审批记录 |
| 档案批次 | ArchiveBatch.java | acc_archive_batch | 批次表 |
| 批次项 | ArchiveBatchItem.java | acc_archive_batch_item | 批次明细 |
| 提交批次 | ArchiveSubmitBatch.java | acc_submit_batch | 提交批次 |
| **认证授权** | | | |
| 认证票据 | AuthTicket.java | acc_auth_ticket | 认证票据 |
| 员工生命周期 | EmployeeLifecycleEvent.java | sys_employee_lifecycle_event | 员工事件 |
| **销毁管理** | | | |
| 销毁 | Destruction.java | acc_destruction | 销毁计划 |
| 销毁日志 | DestructionLog.java | acc_destruction_log | 销毁日志 |
| 异常凭证 | AbnormalVoucher.java | acc_abnormal_voucher | 异常凭证 |
| 原始凭证 | OriginalVoucher.java | acc_original_voucher | 原始凭证 |
| 原始凭证文件 | OriginalVoucherFile.java | acc_original_voucher_file | 凭证文件 |
| 原始凭证类型 | OriginalVoucherType.java | acc_original_voucher_type | 凭证类型 |
| **审计日志** | | | |
| 审计检查日志 | AuditInspectionLog.java | sys_audit_inspection_log | 检查日志 |
| 系统审计日志 | SysAuditLog.java | sys_audit_log | 审计日志 |
| 同步历史 | SyncHistory.java | erp_sync_history | 同步记录 |
| 转换日志 | ConvertLog.java | sys_convert_log | 转换记录 |
| **用户权限** | | | |
| 用户 | User.java | sys_user | 用户表 |
| 用户 MFA 配置 | UserMfaConfig.java | sys_user_mfa_config | MFA配置 |
| 角色 | Role.java | sys_role | 角色表 |
| 权限 | Permission.java | sys_permission | 权限表 |
| 组织 | Org.java | sys_org | 组织表 |
| 职位 | Position.java | sys_position | 职位表 |
| 访问审核 | AccessReview.java | sys_access_review | 访问审核 |
| **ERP 集成** | | | |
| ERP 配置 | ErpConfig.java | erp_config | ERP配置 |
| ERP 场景 | ErpScenario.java | erp_scenario | 业务场景 |
| ERP 子接口 | ErpSubInterface.java | erp_sub_interface | 子接口 |
| 历史导入任务 | LegacyImportTask.java | legacy_import_task | 导入任务 |
| **系统配置** | | | |
| 系统设置 | SystemSetting.java | sys_setting | 系统参数 |
| 文件存储策略 | FileStoragePolicy.java | sys_file_storage_policy | 存储策略 |
| 文件哈希去重范围 | FileHashDedupScope.java | sys_file_hash_dedup_scope | 去重范围 |
| 实体配置 | EntityConfig.java | sys_entity_config | 实体配置 |
| 周期锁 | PeriodLock.java | sys_period_lock | 期间锁 |
| **性能监控** | | | |
| 系统性能指标 | SystemPerformanceMetrics.java | sys_performance_metrics | 性能指标 |
| 完整性检查 | IntegrityCheck.java | sys_integrity_check | 完整性检查 |
| **基础数据** | | | |
| 全宗 | BasFonds.java | bas_fonds | 全宗表 |
| 全宗历史 | FondsHistory.java | bas_fonds_history | 全宗历史 |
| 案卷 | Volume.java | acc_archive_volume | 案卷表 |
| 位置 | Location.java | bas_location | 位置表 |
| 池服务 | PoolService.java | bas_pool_service | 池服务 |
| 仓库服务 | WarehouseService.java | bas_warehouse_service | 仓库服务 |

**Entity 模块总数**: 50+ 个实体类

---

### 4. Config 层 - 配置模块

| 配置类别 | 配置名称 | 功能描述 |
|---------|---------|---------|
| **安全配置** | | |
| SecurityConfig | Spring Security 配置 | 认证授权框架 |
| JwtAuthenticationFilter | JWT 认证过滤器 | Token 验证 |
| RestAccessDeniedHandler | 访问拒绝处理器 | 403 处理 |
| RestAuthenticationEntryPoint | 认证入口点 | 401 处理 |
| LicenseValidationFilter | 许可证验证过滤器 | 许可证检查 |
| XssFilter | XSS 过滤器 | XSS 防护 |
| DataMaskingConfig | 数据脱敏配置 | 敏感数据脱敏 |
| **数据库配置** | | |
| MyBatisPlusConfig | MyBatis-Plus 配置 | ORM 框架 |
| MybatisConfig | MyBatis 配置 | SQL 映射 |
| PostgresJsonTypeHandler | PostgreSQL JSON 处理器 | JSONB 类型处理 |
| EncryptTypeHandler | 加密类型处理器 | 字段加密 |
| **Web 配置** | | |
| WebMvcConfig | Web MVC 配置 | MVC 框架 |
| JacksonConfig | Jackson 配置 | JSON 序列化 |
| AsyncConfig | 异步配置 | 异步任务 |
| SchedulingConfig | 调度配置 | 定时任务 |
| **监控配置** | | |
| MonitoringService | 监控服务配置 | 系统监控 |
| RateLimitFilter | 速率限制过滤器 | API 限流 |
| PerformanceMetricsService | 性能指标服务 | 性能监控 |
| **数据迁移** | | |
| ResilientFlywayRunner | 弹性 Flyway 运行器 | 数据库迁移 |
| MigrationGatekeeperInterceptor | 迁移门卫拦截器 | 迁移控制 |
| DatabaseEnvironmentGuard | 数据库环境守卫 | 环境保护 |
| **业务配置** | | |
| FondsContextFilter | 全宗上下文过滤器 | 多全宗隔离 |
| CrossFondsAccessInterceptor | 跨全宗访问拦截器 | 跨全宗控制 |
| EntitySchemaValidator | 实体模式验证器 | 实体验证 |
| GlobalExceptionHandler | 全局异常处理器 | 统一异常处理 |
| MatchingExecutorConfig | 匹配执行器配置 | 智能匹配 |
| ArchiveValidationPolicy | 档案验证策略 | 验证规则 |

**Config 模块总数**: 25+ 个配置类

---

### 5. Integration 层 - 集成模块

#### ERP 集成模块

| 集成模块 | 文件 | 支持系统 | 功能 |
|---------|------|---------|------|
| **适配器层** | | | |
| ERP 适配器 | ErpAdapter.java | - | 适配器接口 |
| 适配器工厂 | ErpAdapterFactory.java | - | 适配器工厂 |
| 通用 ERP 适配器 | GenericErpAdapter.java | 通用 | 标准 REST API |
| 金蝶适配器 | KingdeeAdapter.java | 金蝶云星空 | 金蝶 ERP |
| 用友适配器 | WeaverAdapter.java | 用友 NC | 用友 ERP |
| 用友 E10 适配器 | WeaverE10Adapter.java | 用友 E10 | 用友 E10 |
| 云 suite 适配器 | YonSuiteErpAdapter.java | 用友云 suite | 云 suite |
| **云 suite 专用** | | | |
| 云 suite 客户端 | YonSuiteClient.java | 云 suite | HTTP 客户端 |
| 云 suite 认证服务 | YonAuthService.java | 云 suite | OAuth 认证 |
| 云 suite 凭证同步 | YonSuiteVoucherSyncService.java | 云 suite | 凭证同步 |
| 云 suite 签名验证 | YonSuiteSignatureValidator.java | 云 suite | Webhook 验证 |

#### 集成 DTO

| DTO 名称 | 用途 |
|---------|------|
| UnifiedDocumentDTO | 统一文档模型 |
| VoucherDTO | 凭证数据传输 |
| AccountSummaryDTO | 科目汇总 |
| AttachmentDTO | 附件信息 |
| BatchIngestRequest/Response | 批量摄取 |
| ConnectionTestResult | 连接测试 |

---

## 前端模块

### 技术栈

- **框架**: React 19
- **语言**: TypeScript 5.8
- **构建**: Vite 6
- **UI**: Ant Design 6
- **状态**: Zustand
- **路由**: React Router 7

---

### 1. Features 功能模块

| 功能模块 | 文件 | 职责描述 |
|---------|------|---------|
| **档案管理** | features/archives/ | | |
| 档案操作 | useArchiveActions.ts | 档案 CRUD 操作 |
| 列表控制 | useArchiveListController.ts | 列表状态管理 |
| 智能匹配 | useSmartMatching.ts | 智能匹配 Hook |
| **借阅管理** | features/borrowing/ | | |
| 借阅功能 | index.ts | 借阅业务逻辑 |
| **合规管理** | features/compliance/ | | |
| 合规功能 | index.ts | 合规检查 |
| **设置模块** | features/settings/ | | |
| 应用层 | application/ | 设置 API Hook |
| 领域层 | domain/ | 设置类型定义 |
| 基础设施层 | infrastructure/ | 设置 API 客户端 |

**Features 模块总数**: 4 个主要功能模块

---

### 2. Components 共享组件

| 组件类别 | 组件名称 | 功能描述 |
|---------|---------|---------|
| **核心组件** | | | |
| 全局搜索 | GlobalSearch.tsx | 跨模块搜索 |
| 侧边栏 | Sidebar.tsx | 导航侧边栏 |
| 顶部栏 | TopBar.tsx | 顶部导航栏 |
| 水印覆盖层 | WatermarkOverlay.tsx | 预览水印 |
| 销毁仓库视图 | DestructionRepositoryView.tsx | 销毁仓库 |
| 开放库存视图 | OpenInventoryView.tsx | 开放档案 |
| 关联视图 | RelationshipView.tsx | 档案关联 |
| 关联可视化 | RelationshipVisualizer.tsx | 关联可视化 |
| **通用组件** | | | |
| 合规雷达 | ComplianceRadar.tsx | 合规评分雷达 |
| 演示徽章 | DemoBadge.tsx | 演示环境标识 |
| 错误边界 | ErrorBoundary.tsx | React 错误捕获 |
| 全宗切换器 | FondsSwitcher.tsx | 全宗切换 |
| 元数据编辑 | MetadataEditModal.tsx | 元数据编辑 |
| OFD 查看器 | OfdViewer.tsx | OFD 文件预览 |
| 对账报告 | ReconciliationReport.tsx | 对账报告展示 |
| **认证组件** | | | |
| 登录卡片 | auth/LoginCard.tsx | 登录表单 |
| **组织组件** | | | |
| 组织树 | org/Tree.tsx | 组织架构树 |
| **设置组件** | settings/ | | |
| 基础设置 | BasicSettings.tsx | 系统基础设置 |
| 集成设置 | IntegrationSettings.tsx | ERP 集成配置 |
| 许可证设置 | LicenseSettings.tsx | 许可证管理 |
| 组织设置 | OrgSettings.tsx | 组织架构配置 |
| 角色设置 | RoleSettings.tsx | 角色权限配置 |
| 安全设置 | SecuritySettings.tsx | 安全策略配置 |
| 设置布局 | SettingsLayout.tsx | 设置页面布局 |
| 用户设置 | UserSettings.tsx | 用户个人设置 |

**Components 模块总数**: 30+ 个共享组件

---

### 3. Pages 页面模块

| 页面类别 | 页面名称 | 路由路径 | 功能描述 |
|---------|---------|---------|---------|
| **认证页面** | | | |
| 登录页 | Login/index.tsx | /login | 用户登录 |
| MFA 验证页 | MfaVerifyPage.tsx | /mfa-verify | 多因素认证 |
| **管理页面** | | | |
| 访问审查 | AccessReviewPage.tsx | /admin/access-review | 访问权限审查 |
| 企业架构 | EnterpriseArchitecturePage.tsx | /admin/enterprise-architecture | 企业架构管理 |
| 实体管理 | EntityManagementPage.tsx | /admin/entities | 法人实体管理 |
| 实体配置 | EntityConfigPage.tsx | /admin/entity-config | 实体配置 |
| 全宗管理 | FondsManagement.tsx | /admin/fonds | 全宗管理 |
| 全宗历史 | FondsHistoryPage.tsx | /admin/fonds-history | 全宗历史记录 |
| 历史数据导入 | LegacyImportPage.tsx | /admin/legacy-import | 历史数据导入 |
| 职位管理 | PositionManagement.tsx | /admin/positions | 职位管理 |
| 用户生命周期 | UserLifecyclePage.tsx | /admin/users/lifecycle | 用户生命周期 |
| **档案管理** | | | |
| 档案列表 | ArchiveListPage.tsx | /archives | 档案列表查询 |
| 档案列表视图 | ArchiveListView.tsx | - | 档案列表组件 |
| **审计页面** | | | |
| 审计证据包 | AuditEvidencePackagePage.tsx | /audit/evidence-package | 审计证据包 |
| 审计验真 | AuditVerificationPage.tsx | /audit/verification | 审计验真 |
| **资料收集** | | | |
| 四性报告 | FourNatureReportView.tsx | /collection/four-nature | 四性检测报告 |
| 在线受理 | OnlineReceptionView.tsx | /collection/online | 在线数据接收 |
| **匹配页面** | | | |
| 凭证匹配 | VoucherMatchingPage.tsx | /matching/vouchers | 凭证智能匹配 |
| 上向导 | OnboardingWizard.tsx | /matching/onboarding | 配置向导 |
| **作业管理** | | | |
| 鉴定列表 | AppraisalListPage.tsx | /operations/appraisals | 鉴定任务列表 |
| 档案审批 | ArchiveApprovalView.tsx | /operations/approval | 档案审批 |
| 档案批次 | ArchiveBatchView.tsx | /operations/batches | 批次管理 |
| 销毁审批 | DestructionApprovalPage.tsx | /operations/destruction/approval | 销毁审批 |
| 销毁执行 | DestructionExecutionPage.tsx | /operations/destruction/execution | 销毁执行 |
| 销毁视图 | DestructionView.tsx | /operations/destruction | 销毁管理 |
| 过期档案 | ExpiredArchivesPage.tsx | /operations/expired | 过期档案 |
| 冻结保全 | FreezeHoldPage.tsx | /operations/freeze-hold | 冻结/保全 |
| 冻结详情 | FreezeHoldDetailPage.tsx | /operations/freeze-hold/:id | 冻结详情 |
| 开放鉴定 | OpenAppraisalView.tsx | /operations/open-appraisal | 开放鉴定 |
| 案卷管理 | VolumeManagement.tsx | /operations/volumes | 案卷管理 |
| **全景视图** | | | |
| 档案全景 | ArchivalPanoramaView.tsx | /panorama | 档案全景视图 |
| **门户** | | | |
| 仪表板 | Dashboard.tsx | / | 主页仪表板 |
| **安全页面** | | | |
| 票据申请 | AuthTicketApplyPage.tsx | /security/auth-ticket/apply | 票据申请 |
| 票据列表 | AuthTicketListPage.tsx | /security/auth-ticket/list | 票据列表 |
| **设置页面** | | | |
| 审计日志 | AuditLogView.tsx | /settings/audit-logs | 审计日志查询 |
| 基础设置 | BasicSettingsPage.tsx | /settings/basic | 基础设置 |
| 集成设置 | IntegrationSettingsPage.tsx | /settings/integration | 集成设置 |
| MFA 设置 | MfaSettingsPage.tsx | /settings/mfa | 多因素认证设置 |
| 组织设置 | OrgSettingsPage.tsx | /settings/org | 组织设置 |
| 角色设置 | RoleSettingsPage.tsx | /settings/roles | 角色权限设置 |
| 安全设置 | SecuritySettingsPage.tsx | /settings/security | 安全策略设置 |
| 用户设置 | UserSettingsPage.tsx | /settings/users | 用户设置 |
| **统计页面** | | | |
| 统计视图 | StatsView.tsx | /stats | 数据统计 |
| **利用页面** | | | |
| 借阅视图 | BorrowingView.tsx | /utilization/borrowing | 档案借阅 |
| 关联查询 | RelationshipQueryView.tsx | /utilization/relationships | 关联查询 |
| 仓库视图 | WarehouseView.tsx | /utilization/warehouse | 仓库管理 |

**Pages 模块总数**: 40+ 个页面

---

### 4. API 客户端模块

| API 模块 | 文件 | 功能描述 |
|---------|------|---------|
| 核心接口 | | | |
| 档案 API | archives.ts | 档案 CRUD |
| 附件 API | attachments.ts | 附件管理 |
| 认证 API | auth.ts | 登录认证 |
| 授权票据 API | authTicket.ts | 票据管理 |
| MFA API | mfa.ts | 多因素认证 |
| 审计 API | audit.ts | 审计日志 |
| 审计验真 API | auditVerification.ts | 审计验真 |
| 销毁 API | destruction.ts | 销毁管理 |
| 冻结保全 API | freezeHold.ts | 冻结/保全 |
| 搜索 API | search.ts | 全文搜索 |
| 统计 API | stats.ts | 数据统计 |
| 工作流 API | workflow.ts | 工作流 |
| 管理接口 | | | |
| 管理员 API | admin.ts | 管理员操作 |
| 档案审批 API | archiveApproval.ts | 审批流程 |
| 档案批次 API | archiveBatch.ts | 批次操作 |
| 异常数据 API | abnormal.ts | 异常处理 |
| 原始凭证 API | originalVoucher.ts | 原始凭证 |
| 集成接口 | | | |
| ERP API | erp.ts | ERP 集成 |
| 历史导入 API | legacyImport.ts | 历史导入 |
| 智能匹配 API | matching.ts | 智能匹配 |
| 自动关联 API | autoAssociation.ts | 自动关联 |
| 组织接口 | | | |
| 全宗 API | fonds.ts | 全宗管理 |
| 全宗历史 API | fondsHistory.ts | 全宗历史 |
| 实体 API | entity.ts | 法人实体 |
| 实体配置 API | entityConfig.ts | 实体配置 |
| 企业架构 API | enterpriseArchitecture.ts | 企业架构 |
| 用户接口 | | | |
| 用户生命周期 API | userLifecycle.ts | 用户生命周期 |
| 其他接口 | | | |
| 借阅 API | borrowing.ts | 借阅管理 |
| 预览 API | preview.ts | 文件预览 |
| 池 API | pool.ts | 池服务 |
| 仓库 API | warehouse.ts | 仓库管理 |
| 许可证 API | license.ts | 许可证 |
| 导航 API | nav.ts | 导航菜单 |
| 通知 API | notifications.ts | 通知消息 |
| 开放鉴定 API | openAppraisal.ts | 开放鉴定 |

**API 模块总数**: 35+ 个 API 客户端

---

### 5. Store 状态管理模块

| Store 模块 | 文件 | 状态内容 |
|-----------|------|---------|
| 应用状态 | useAppStore.ts | 全局应用状态 |
| 认证状态 | useAuthStore.ts | 用户认证状态 |
| 全宗状态 | useFondsStore.ts | 当前全宗 |
| 主题状态 | useThemeStore.ts | UI 主题 |

---

## 模块依赖关系

### 后端核心依赖流

```
Controller 层
    ↓ 依赖
Service 层
    ↓ 依赖
Integration 层 / Mapper 层
    ↓ 依赖
Entity 层
    ↓ 使用
Config 层
```

### 前端核心依赖流

```
Pages 层
    ↓ 使用
Features 层 / Components 层
    ↓ 调用
API 层
    ↓ 管理
Store 层
```

---

## 模块统计总结

### 后端模块统计

| 层级 | 模块数量 | 代码行数 (估算) |
|-----|---------|---------------|
| Controller | 30+ | ~8,000 |
| Service | 80+ | ~15,000 |
| Entity | 50+ | ~6,000 |
| Config | 25+ | ~4,000 |
| Integration | 20+ | ~5,000 |
| **总计** | **205+** | **~38,000** |

### 前端模块统计

| 层级 | 模块数量 | 文件数 |
|-----|---------|--------|
| Pages | 40+ | 50+ |
| Components | 30+ | 40+ |
| Features | 4 | 20+ |
| API | 35+ | 35+ |
| Store | 4 | 4 |
| **总计** | **113+** | **149+** |

---

## 模块化重构成果 (2025-12-31)

### 已拆分的大型模块

| 原模块 | 原始行数 | 拆分后模块数 | 主服务行数 | 减少比例 |
|-------|---------|------------|-----------|---------|
| VoucherPdfGeneratorService | 1058 | 6 | 151 | 86% |
| ReconciliationServiceImpl | 991 | 5 | 482 | 51% |
| VolumeService | 794 | 6 | 139 | 82% |
| ArchiveSubmitBatchServiceImpl | 779 | 4 | 186 | 76% |
| LegacyImportServiceImpl | 722 | 4 | 102 | 86% |
| **合计** | **4344** | **25** | **1060** | **76%** |

### 设计原则遵循

- ✅ 单一职责原则 (SRP)
- ✅ 依赖倒置原则 (DIP)
- ✅ 接口隔离原则 (ISP)
- ✅ Facade 协调器模式
- ✅ < 300 行/模块
- ✅ < 10 圈复杂度/模块

---

**文档版本**: 1.0
**生成工具**: Claude Code
**最后更新**: 2025-12-31
