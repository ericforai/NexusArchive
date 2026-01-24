# ERP 接口相关代码和文档整理

> 本文档整理了 NexusArchive 系统中所有与 ERP 接口相关的代码文件和文档。
> 最后更新: 2026-01-24 (新增销售订单同步功能)

---

## 目录

- [一、文档](#一文档)
- [二、后端代码](#二后端代码)
- [三、前端代码](#三前端代码)
- [四、API 端点汇总](#四api-端点汇总)
- [五、数据模型](#五数据模型)
- [六、支持的 ERP 系统](#六支持的-erp-系统)

---

## 一、文档

### 1.1 核心文档

| 文档路径 | 说明 |
|---------|------|
| `docs/knowledge/erp_integration.md` | ERP 集成与电子文件生成原则 (数据同步、PDF 生成标准) |
| `docs/guides/用友集成.md` | YonSuite 集成完整指南 (Webhook 配置、故障排除) |
| `docs/guides/ERP集成配置.md` | ERP 配置管理说明 |
| `docs/guides/integration-operation-manual.md` | 集成中心操作手册 |
| `docs/guides/weaver_integration_technical_brief.md` | 泛微集成技术说明 |
| `docs/architecture/erp-adapter-development-guide.md` | ERP 适配器开发指南 |
| `docs/architecture/erp-ai-usage-guide.md` | AI 适配器生成使用指南 |
| `docs/development/erp-adapter-guide.md` | ERP 适配器开发教程 |

### 1.2 计划文档

| 文档路径 | 说明 |
|---------|------|
| `docs/plans/2026-01-02-erp-modularization-refactor.md` | ERP 模块化重构计划 |
| `docs/plans/2026-01-02-erp-ai-adaptation-design.md` | ERP AI 适配设计方案 |
| `docs/plans/2026-01-02-erp-ai-adaptation-mvp-plan.md` | AI 适配 MVP 实施计划 |
| `docs/plans/2026-01-06-erp-mapping-framework.md` | ERP 映射框架设计 |
| `docs/plans/2026-01-06-erp-api-security-fixes.md` | ERP API 安全修复 |
| `docs/plans/2026-01-08-erp-mapping-framework-implementation.md` | ERP 映射框架实现 |
| `docs/plans/2026-01-09-sap-integration.md` | SAP 集成计划 |
| `docs/plans/2026-01-24-sales-order-sync-design.md` | **(NEW)** 销售订单同步设计文档 |
| `docs/plans/2026-01-24-sales-order-sync-implementation.md` | **(NEW)** 销售订单同步实施计划 |

### 1.3 报告文档

| 文档路径 | 说明 |
|---------|------|
| `docs/reports/paranoid-debugging-erp-mapping-fix.md` | ERP 映射调试报告 |
| `docs/reports/enterprise-architecture-implementation-analysis.md` | 企业架构实现分析 |
| `docs/reports/enterprise-architecture-clarification.md` | 企业架构说明 |
| `docs/reports/user-lifecycle-org-integration-issue.md` | 用户生命周期组织集成问题 |

---

## 二、后端代码

### 2.1 核心 Adapter 接口

| 文件路径 | 说明 |
|---------|------|
| `integration/erp/adapter/ErpAdapter.java` | ERP 适配器统一接口 (定义所有适配器必须实现的方法) |
| `integration/erp/adapter/ErpAdapterFactory.java` | 适配器工厂 (动态创建和获取适配器实例) |
| `integration/erp/adapter/GenericErpAdapter.java` | 通用适配器 (基于 JSON 映射规则) |
| `integration/erp/adapter/WeaverAdapter.java` | 泛微适配器 |
| `integration/erp/adapter/WeaverE10Adapter.java` | 泛微 E10 适配器 |
| `integration/erp/adapter/KingdeeErpAdapter.java` | 金蝶云星空适配器 |
| `integration/erp/adapter/yonsuite/YonsuiteErpAdapter.java` | YonSuite 适配器 (AI 生成) |
| `integration/erp/annotation/ErpAdapterAnnotation.java` | 适配器元数据注解 |
| `integration/erp/registry/ErpMetadataRegistry.java` | 元数据注册中心 |

### 2.2 AI 适配器生成

| 文件路径 | 说明 |
|---------|------|
| `integration/erp/ai/agent/ErpAdaptationOrchestrator.java` | AI 编排器 (协调整个适配器生成流程) |
| `integration/erp/ai/controller/ErpAdaptationController.java` | AI 适配 REST 控制器 |
| `integration/erp/ai/deploy/ErpAdapterAutoDeployService.java` | 自动部署服务 |
| `integration/erp/ai/deploy/CompilationService.java` | 代码编译服务 |
| `integration/erp/ai/deploy/CodeStorageService.java` | 代码存储服务 |
| `integration/erp/ai/deploy/DatabaseRegistrationService.java` | 数据库注册服务 |
| `integration/erp/ai/deploy/TestExecutionService.java` | 测试执行服务 |
| `integration/erp/ai/generator/ErpAdapterCodeGenerator.java` | 适配器代码生成器 |
| `integration/erp/ai/generator/GeneratedCode.java` | 生成代码模型 |
| `integration/erp/ai/parser/OpenApiDocumentParser.java` | OpenAPI 文档解析器 |
| `integration/erp/ai/parser/OpenApiDefinition.java` | OpenAPI 定义模型 |
| `integration/erp/ai/identifier/ErpTypeIdentifier.java` | ERP 类型识别器 |
| `integration/erp/ai/identifier/ScenarioNamer.java` | 场景命名器 |
| `integration/erp/ai/identifier/ScenarioName.java` | 场景名称注解 |
| `integration/erp/ai/mapper/BusinessSemanticMapper.java` | 业务语义映射器 |
| `integration/erp/ai/mapper/ApiIntent.java` | API 意图定义 |
| `integration/erp/ai/mapper/StandardScenario.java` | 标准场景定义 |

### 2.3 映射引擎

| 文件路径 | 说明 |
|---------|------|
| `integration/erp/mapping/ErpMapper.java` | 映射接口 |
| `integration/erp/mapping/GroovyMappingEngine.java` | Groovy 脚本映射引擎 |
| `integration/erp/mapping/MappingConfig.java` | 映射配置模型 |
| `integration/erp/mapping/ObjectMapping.java` | 对象映射定义 |
| `integration/erp/mapping/MappingException.java` | 映射异常 |
| `integration/erp/mapping/MappingConfigNotFoundException.java` | 映射配置未找到异常 |
| `integration/erp/mapping/MappingConfigParseException.java` | 映射配置解析异常 |
| `integration/erp/mapping/MappingScriptException.java` | 映射脚本异常 |

### 2.4 YonSuite 客户端

| 文件路径 | 说明 |
|---------|------|
| `integration/yonsuite/client/YonSuiteClient.java` | HTTP 客户端基类 |
| `integration/yonsuite/client/YonSuiteVoucherClient.java` | 凭证客户端 |
| `integration/yonsuite/client/YonSuiteCollectionClient.java` | 收款单客户端 |
| `integration/yonsuite/client/YonSuitePaymentClient.java` | 付款单客户端 |
| `integration/erp/adapter/client/YonSuiteAuthClient.java` | 认证客户端 |
| `integration/erp/adapter/client/YonSuiteCollectionClient.java` | 收款单客户端 (新) |
| `integration/erp/adapter/client/YonSuitePaymentClient.java` | 付款单客户端 (新) |
| `integration/erp/adapter/client/YonSuiteFeedbackClient.java` | 归档状态回写客户端 |
| `integration/erp/adapter/client/YonSuiteRefundClient.java` | 退款单客户端 |
| `integration/erp/adapter/client/YonSuitePaymentApplyClient.java` | **(NEW)** 付款申请单同步客户端 |
| `integration/yonsuite/client/YonSuiteSalesOrderClient.java` | **(NEW)** 销售订单同步客户端 |
| `integration/yonsuite/client/YonSuiteHttpExecutor.java` | HTTP 执行器 |

### 2.5 YonSuite 控制器

| 文件路径 | 说明 |
|---------|------|
| `integration/yonsuite/controller/YonSuiteWebhookController.java` | Webhook 接收控制器 |
| `integration/yonsuite/controller/YonSuiteCollectionController.java` | 收款单接口控制器 |
| `integration/yonsuite/controller/YonPaymentTestController.java` | 测试接口控制器 |
| `integration/yonsuite/controller/YonPaymentApplyFileController.java` | **(NEW)** 付款申请单文件控制器 |
| `integration/erp/controller/ErpIngestController.java` | SIP 批量接入控制器 |

### 2.6 YonSuite 安全

| 文件路径 | 说明 |
|---------|------|
| `integration/yonsuite/security/YonSuiteSignatureValidator.java` | Webhook 签名验证器 |
| `integration/yonsuite/security/YonSuiteEventCrypto.java` | 事件加密/解密 |
| `integration/yonsuite/security/WebhookNonceStore.java` | Nonce 防重放存储 |

### 2.7 YonSuite 服务

| 文件路径 | 说明 |
|---------|------|
| `integration/yonsuite/service/YonAuthService.java` | 认证服务 |
| `integration/yonsuite/service/YonPaymentListService.java` | 付款单列表服务 |
| `integration/yonsuite/service/YonRefundListService.java` | 退款单列表服务 |
| `integration/yonsuite/service/YonPaymentApplyFileService.java` | **(NEW)** 付款申请单文件服务 |
| `integration/yonsuite/service/YonPaymentApplySyncService.java` | **(NEW)** 付款申请单同步服务 |
| `integration/yonsuite/service/YonSuiteSalesOrderSyncService.java` | **(NEW)** 销售订单同步服务 |
| `integration/yonsuite/event/YonSuiteVoucherEvent.java` | 凭证事件模型 |

### 2.7b MyBatis Mapper

| 文件路径 | 说明 |
|---------|------|
| `mapper/SdSalesOrderMapper.java` | **(NEW)** 销售订单 MyBatis Mapper |
| `mapper/SdSalesOrderDetailMapper.java` | **(NEW)** 销售订单明细 MyBatis Mapper |

### 2.8 YonSuite DTO

| 文件路径 | 说明 |
|---------|------|
| `integration/yonsuite/dto/SalesOutListRequest.java` | 销售出库列表请求 |
| `integration/yonsuite/dto/SalesOutListResponse.java` | 销售出库列表响应 |
| `integration/yonsuite/dto/SalesOutDetailResponse.java` | 销售出库详情响应 |
| `integration/yonsuite/dto/YonVoucherListRequest.java` | 凭证列表请求 |
| `integration/yonsuite/dto/YonVoucherDetailRequest.java` | 凭证详情请求 |
| `integration/yonsuite/dto/YonVoucherDetailResponse.java` | 凭证详情响应 |
| `integration/yonsuite/dto/YonCollectionBillRequest.java` | 收款单请求 |
| `integration/yonsuite/dto/YonCollectionBillResponse.java` | 收款单响应 |
| `integration/yonsuite/dto/YonCollectionDetailResponse.java` | 收款详情响应 |
| `integration/yonsuite/dto/YonCollectionFileRequest.java` | 收款附件请求 |
| `integration/yonsuite/dto/YonCollectionFileResponse.java` | 收款附件响应 |
| `integration/yonsuite/dto/YonPaymentDetailResponse.java` | 付款详情响应 |
| `integration/yonsuite/dto/YonRefundListRequest.java` | 退款列表请求 |
| `integration/yonsuite/dto/YonRefundListResponse.java` | 退款列表响应 |
| `integration/yonsuite/dto/YonRefundFileRequest.java` | 退款附件请求 |
| `integration/yonsuite/dto/YonRefundFileResponse.java` | 退款附件响应 |
| `integration/yonsuite/dto/YonCloseInfoRequest.java` | 关账信息请求 |
| `integration/yonsuite/dto/YonCloseInfoResponse.java` | 关账信息响应 |
| `integration/yonsuite/dto/VoucherAttachmentRequest.java` | 凭证附件请求 |
| `integration/yonsuite/dto/VoucherAttachmentResponse.java` | 凭证附件响应 |
| `integration/yonsuite/dto/YonAttachmentListResponse.java` | 附件列表响应 |
| `integration/yonsuite/dto/YonPaymentApplyFileRequest.java` | **(NEW)** 付款申请单文件请求 |
| `integration/yonsuite/dto/YonPaymentApplyFileResponse.java` | **(NEW)** 付款申请单文件响应 |
| `integration/yonsuite/dto/YonPaymentApplyListRequest.java` | **(NEW)** 付款申请单列表请求 |
| `integration/yonsuite/dto/YonPaymentApplyListResponse.java` | **(NEW)** 付款申请单列表响应 |
| `integration/yonsuite/dto/SalesOrderListRequest.java` | **(NEW)** 销售订单列表请求 |
| `integration/yonsuite/dto/SalesOrderListResponse.java` | **(NEW)** 销售订单列表响应 |
| `integration/yonsuite/dto/SalesOrderDetailResponse.java` | **(NEW)** 销售订单详情响应 |
| `integration/yonsuite/mapper/SalesOutMapper.java` | 销售出库 Mapper |
| `integration/yonsuite/mapper/SalesOrderDataMapper.java` | **(NEW)** 销售订单数据映射器 |

### 2.9 核心 Controller

| 文件路径 | 说明 |
|---------|------|
| `controller/ErpConfigController.java` | ERP 配置 REST API |
| `controller/ErpScenarioController.java` | 业务场景 REST API |
| `controller/GenericYonSuiteController.java` | 通用 YonSuite 端点 |
| `controller/SalesOrderController.java` | **(NEW)** 销售订单 REST API |

### 2.10 核心服务

| 文件路径 | 说明 |
|---------|------|
| `service/ErpConfigService.java` | 配置管理服务 |
| `service/ErpScenarioService.java` | 场景管理服务 |
| `service/ErpDiagnosisService.java` | 健康诊断服务 |
| `service/ErpOrgSyncService.java` | 组织架构同步服务 |
| `service/ErpFeedbackService.java` | 归档状态回写服务 |
| `service/ErpSubInterfaceService.java` | 子接口管理服务 |

### 2.11 核心实体类

| 文件路径 | 说明 |
|---------|------|
| `entity/ErpConfig.java` | ERP 配置实体 |
| `entity/ErpScenario.java` | 业务场景实体 |
| `entity/ErpSubInterface.java` | 子接口实体 |
| `entity/SalesOrder.java` | **(NEW)** 销售订单实体 |
| `entity/SalesOrderDetail.java` | **(NEW)** 销售订单明细实体 |

### 2.12 核心 DTO

| 文件路径 | 说明 |
|---------|------|
| `integration/erp/dto/ErpConfig.java` | ERP 配置 DTO |
| `integration/erp/dto/ErpMetadata.java` | ERP 元数据 |
| `integration/erp/dto/VoucherDTO.java` | 凭证 DTO |
| `integration/erp/dto/AttachmentDTO.java` | 附件 DTO |
| `integration/erp/dto/ConnectionTestResult.java` | 连接测试结果 |
| `integration/erp/dto/BatchIngestRequest.java` | 批量接入请求 |
| `integration/erp/dto/BatchIngestResponse.java` | 批量接入响应 |
| `integration/erp/dto/IngestResult.java` | 接入结果 |
| `integration/erp/dto/ErpPageRequest.java` | 分页请求 |
| `integration/erp/dto/ErpPageResponse.java` | 分页响应 |
| `integration/erp/dto/FeedbackResult.java` | 回写结果 |
| `integration/erp/dto/AccountSummaryDTO.java` | 科目汇总 DTO |
| `integration/erp/dto/CloseInfoResult.java` | 关账信息结果 |
| `integration/erp/dto/kingdee/KingdeeAuthResponse.java` | 金蝶认证响应 |
| `integration/erp/dto/kingdee/KingdeeVoucherListResponse.java` | 金蝶凭证列表 |
| `integration/erp/dto/kingdee/KingdeeVoucherDetailResponse.java` | 金蝶凭证详情 |

### 2.13 核心模型

| 文件路径 | 说明 |
|---------|------|
| `integration/core/model/UnifiedDocumentDTO.java` | 统一文档模型 |

### 2.14 异常

| 文件路径 | 说明 |
|---------|------|
| `integration/erp/exception/PeriodNotClosedException.java` | 期间未关账异常 |

### 2.15 Mock 控制器

| 文件路径 | 说明 |
|---------|------|
| `integration/mock/YonSuiteMockController.java` | YonSuite Mock 控制器 (用于测试) |

### 2.16 数据库迁移

| 文件路径 | 说明 |
|---------|------|
| `src/main/resources/db/migration/V86__create_erp_adapter_tables.sql` | AI 适配器表 (sys_erp_adapter, sys_erp_adapter_scenario) |
| `src/main/resources/db/migration/V95__add_accbook_mapping_to_erp_config.sql` | 账套-全宗映射字段 (accbook_mapping) |
| `src/main/resources/db/migration/V98__add_sap_interface_type_to_erp_config.sql` | SAP 接口类型字段 (sap_interface_type) |
| `src/main/resources/db/migration/V101__sys_entity_add_parent_id.sql` | 组织架构树结构 (parent_id) |
| `src/main/resources/db/migration/V102__create_sales_order_tables.sql` | **(NEW)** 销售订单表 (sd_sales_order, sd_sales_order_detail) |

---

## 三、前端代码

### 3.1 API 客户端

| 文件路径 | 说明 |
|---------|------|
| `src/api/erp.ts` | ERP 集成 API 客户端 (配置、场景、同步、AI 适配器) |
| `src/api/yonsuite.ts` | YonSuite 专用 API 客户端 |
| `src/api/yonsuite-payment-apply.ts` | **(NEW)** 付款申请单文件 API 客户端 |
| `src/api/yonsuite-payment-apply-sync.ts` | **(NEW)** 付款申请单同步 API 客户端 |
| `src/api/sales-order.ts` | **(NEW)** 销售订单同步 API 客户端 |

### 3.2 集成设置页面

| 文件路径 | 说明 |
|---------|------|
| `src/components/settings/integration/IntegrationSettingsPage.tsx` | 主集成设置页面 (组合器模式) |
| `src/components/settings/integration/index.ts` | 模块导出 |
| `src/components/settings/integration/manifest.config.ts` | 模块清单 |

### 3.3 集成设置组件

| 文件路径 | 说明 |
|---------|------|
| `src/components/settings/integration/components/ConnectorForm.tsx` | 连接器配置表单 |
| `src/components/settings/integration/components/DiagnosisPanel.tsx` | 健康诊断面板 |
| `src/components/settings/integration/components/SapInterfaceConfigForm.tsx` | SAP 接口配置表单 |
| `src/components/settings/integration/components/SapInterfaceTypes.tsx` | SAP 接口类型选择器 |
| `src/components/settings/integration/components/ScenarioCard.tsx` | 场景卡片 |
| `src/components/settings/integration/components/ErpConfigList.tsx` | ERP 配置列表 |
| `src/components/settings/integration/components/ErpConfigCard.tsx` | ERP 配置卡片 |
| `src/components/settings/integration/components/ConnectionHealthBadge.tsx` | 连接健康状态徽章 |
| `src/components/settings/integration/components/ScenarioDrawer.tsx` | 场景抽屉 |
| `src/components/settings/integration/components/ScenarioSummaryCard.tsx` | 场景摘要卡片 |
| `src/components/settings/integration/components/ParamsEditor.tsx` | 参数编辑器 |
| `src/components/settings/integration/components/SalesOrderSyncPanel.tsx` | **(NEW)** 销售订单同步面板 |

### 3.4 集成设置 Hooks

| 文件路径 | 说明 |
|---------|------|
| `src/components/settings/integration/hooks/useErpConfigManager.ts` | 配置管理 Hook |
| `src/components/settings/integration/hooks/useScenarioSyncManager.ts` | 场景同步 Hook |
| `src/components/settings/integration/hooks/useConnectorModal.ts` | 连接器弹窗 Hook |
| `src/components/settings/integration/hooks/useIntegrationDiagnosis.ts` | 集成诊断 Hook |
| `src/components/settings/integration/hooks/useAiAdapterHandler.ts` | AI 适配器处理 Hook |
| `src/components/settings/integration/hooks/useParamsEditor.ts` | 参数编辑 Hook |

### 3.5 集成设置类型

| 文件路径 | 说明 |
|---------|------|
| `src/components/settings/integration/types.ts` | TypeScript 类型定义 |

### 3.6 集成设置测试

| 文件路径 | 说明 |
|---------|------|
| `src/components/settings/integration/components/__tests__/ErpConfigList.test.tsx` | ERP 配置列表测试 |
| `src/components/settings/integration/components/__tests__/ErpConfigCard.test.tsx` | ERP 配置卡片测试 |
| `src/components/settings/integration/components/__tests__/IntegrationSettingsPage.test.tsx` | 集成设置页面测试 |
| `src/components/settings/integration/components/__tests__/SapInterfaceConfigForm.test.tsx` | SAP 配置表单测试 |
| `src/components/settings/integration/components/__tests__/ScenarioDrawer.test.tsx` | 场景抽屉测试 |
| `src/components/settings/integration/components/__tests__/ScenarioSummaryCard.test.tsx` | 场景摘要卡片测试 |
| `src/components/settings/integration/components/__tests__/ConnectionHealthBadge.test.tsx` | 健康状态徽章测试 |
| `src/components/settings/integration/hooks/__tests__/useErpConfigManager.test.ts` | 配置管理 Hook 测试 |
| `src/components/settings/integration/hooks/__tests__/useScenarioSyncManager.test.ts` | 场景同步 Hook 测试 |
| `src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts` | 连接器弹窗 Hook 测试 |
| `src/components/settings/integration/hooks/__tests__/useIntegrationDiagnosis.test.ts` | 集成诊断 Hook 测试 |
| `src/components/settings/integration/hooks/__tests__/useAiAdapterHandler.test.ts` | AI 适配器 Hook 测试 |
| `src/components/settings/integration/hooks/__tests__/useParamsEditor.test.ts` | 参数编辑 Hook 测试 |

### 3.7 相关页面

| 文件路径 | 说明 |
|---------|------|
| `src/pages/collection/OnlineReceptionView.tsx` | 在线接收页面 (业务人员操作界面) |
| `src/pages/admin/EnterpriseArchitecturePage.tsx` | 企业架构页面 |
| `src/pages/settings/ErpPreviewPage.tsx` | ERP 预览页面 |

---

## 四、API 端点汇总

### 4.1 配置管理

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/erp/config` | 获取所有配置 |
| POST | `/api/erp/config` | 保存配置 (新增/更新) |
| DELETE | `/api/erp/config/{id}` | 删除配置 |
| GET | `/api/erp/config/types` | 获取支持的适配器类型 |
| POST | `/api/erp/config/{id}/test` | 测试连接 |
| GET | `/api/erp/config/{id}/diagnose` | 健康诊断 |
| GET | `/api/erp/config/{id}/close-check-mode` | 获取关账检查模式 |
| PUT | `/api/erp/config/{id}/close-check-mode` | 更新关账检查模式 |
| POST | `/api/erp/config/{configId}/sync-all` | 批量同步所有场景 |

### 4.2 业务场景

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/erp/scenario/list/{configId}` | 获取场景列表 |
| GET | `/api/erp/scenario/channels` | 获取集成通道 (在线接收页面) |
| PUT | `/api/erp/scenario` | 更新场景配置 |
| POST | `/api/erp/scenario/{id}/sync` | 触发同步 |
| GET | `/api/erp/scenario/{id}/sync/status/{taskId}` | 查询同步任务状态 |
| GET | `/api/erp/scenario/{id}/interfaces` | 获取子接口列表 |
| PUT | `/api/erp/scenario/interface/toggle/{id}` | 切换子接口启用状态 |
| GET | `/api/erp/scenario/{id}/history` | 获取同步历史 |
| PUT | `/api/erp/scenario/{id}/params` | 更新场景参数 |

### 4.3 AI 适配器

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/erp-ai/adapt` | 生成适配器代码 |
| POST | `/erp-ai/deploy` | 生成并部署适配器 |
| POST | `/erp-ai/preview` | 预览 OpenAPI 文档中的场景 |
| GET | `/erp-ai/preview/{sessionId}` | 预览生成的代码 |
| POST | `/erp-ai/adapt-deploy` | 简化版部署接口 |

### 4.4 YonSuite 集成

| 方法 | 端点 | 说明 |
|------|------|------|
| GET/POST | `/api/integration/yonsuite/webhook` | 接收 Webhook 推送 |
| POST | `/api/integration/yonsuite/collection/sync` | 同步收款单 |
| GET | `/api/integration/yonsuite/collection/file` | 获取收款单附件 |
| POST | `/api/integration/yonsuite/payment/sync` | 同步付款单 |
| GET | `/api/integration/yonsuite/payment/file` | 获取付款单附件 |
| GET | `/api/integration/yonsuite/voucher/detail` | 获取凭证详情 |
| GET | `/api/integration/yonsuite/voucher/attachment` | 获取凭证附件 |
| POST | `/api/integration/yonsuite/payment-apply/file/url` | **(NEW)** 查询付款申请单文件下载地址 |
| GET | `/api/integration/yonsuite/payment-apply/file/url/{fileId}` | **(NEW)** 查询单个文件下载地址 |
| GET | `/api/integration/yonsuite/payment-apply/file/health` | **(NEW)** 健康检查 |

### 4.5 SIP 批量接入

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/integration/erp/ingest` | 批量接入归档信息包 |

### 4.6 对账 (Phase 4)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/reconciliation/trigger` | 触发对账 |
| GET | `/api/reconciliation/history` | 获取对账历史 |

### 4.7 监控 (Phase 4)

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/monitoring/integration` | 获取集成监控数据 |

---

## 五、数据模型

### 5.1 sys_erp_config (ERP 配置表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 (自增) |
| name | VARCHAR | 配置名称 (如: 用友生产环境) |
| erp_type | VARCHAR | ERP 类型 (YONSUITE/KINGDEE/SAP/GENERIC) |
| config_json | TEXT | 配置 JSON (含加密的凭证) |
| is_active | INT | 是否启用 (1=启用, 0=禁用) |
| accbook_mapping | TEXT | 账套-全宗映射 JSON: `{"BR01": "FONDS_A"}` |
| sap_interface_type | VARCHAR | SAP 接口类型 (ODATA/RFC/IDOC/GATEWAY) |
| created_time | TIMESTAMP | 创建时间 |
| last_modified_time | TIMESTAMP | 最后修改时间 |

### 5.2 sys_erp_scenario (业务场景表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 (自增) |
| config_id | BIGINT | 关联配置 ID (外键到 sys_erp_config) |
| scenario_key | VARCHAR | 场景代码 (SALES_OUT/RECEIPT/PAYMENT 等) |
| name | VARCHAR | 场景名称 |
| description | VARCHAR | 场景描述 |
| sync_strategy | VARCHAR | 同步策略 (REALTIME/CRON/MANUAL) |
| cron_expression | VARCHAR | Cron 表达式 (当策略为 CRON 时) |
| params_json | TEXT | 场景参数 JSON |
| is_active | BOOLEAN | 是否启用 |
| last_sync_time | TIMESTAMP | 最后同步时间 |
| last_sync_status | VARCHAR | 最后同步状态 |
| last_sync_msg | VARCHAR | 最后同步消息 |

### 5.3 sys_erp_sub_interface (子接口表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 (自增) |
| scenario_id | BIGINT | 关联场景 ID |
| interface_key | VARCHAR | 接口代码 |
| interface_name | VARCHAR | 接口名称 |
| description | VARCHAR | 接口描述 |
| config_json | TEXT | 接口配置 JSON |
| is_active | BOOLEAN | 是否启用 |
| sort_order | INT | 排序号 |

### 5.4 sys_erp_adapter (AI 适配器表)

| 字段 | 类型 | 说明 |
|------|------|------|
| adapter_id | VARCHAR(100) | 适配器唯一标识 (主键) |
| adapter_name | VARCHAR(200) | 适配器名称 |
| erp_type | VARCHAR(50) | ERP 系统类型 |
| base_url | VARCHAR(500) | API 基础 URL |
| enabled | BOOLEAN | 是否启用 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 5.5 sys_erp_adapter_scenario (适配器场景映射表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | SERIAL | 主键 (自增) |
| adapter_id | VARCHAR(100) | 适配器 ID (外键) |
| scenario_code | VARCHAR(50) | 场景代码 |
| created_time | TIMESTAMP | 创建时间 |

### 5.6 sys_entity (组织架构表 - 支持 YonSuite 组织同步)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| parent_id | BIGINT | 父节点 ID (支持树形结构) |
| entity_code | VARCHAR | 组织代码 |
| entity_name | VARCHAR | 组织名称 |
| order_num | INT | 排序号 |
| ... | ... | 其他字段 |

---

## 六、支持的 ERP 系统

| ERP 系统 | 适配器标识 | 状态 | 说明 |
|---------|-----------|------|------|
| 用友 YonSuite | `yonsuite` | ✅ 生产可用 | 支持凭证、收款单、付款单、退款单同步 |
| 金蝶云星空 | `kingdee` | ✅ 已实现 | 支持凭证同步 |
| 泛微 E9 | `weaver` | ✅ 已实现 | 基础集成 |
| 泛微 E10 | `weaver_e10` | ✅ 已实现 | 增强版集成 |
| SAP | `sap` | 🚧 计划中 | 支持 ODATA/RFC/IDOC 接口 |
| 通用 HTTP API | `generic` | ✅ 已实现 | 基于 JSON 映射规则 |

---

## 七、核心功能说明

### 7.1 适配器模式

所有 ERP 集成实现统一的 `ErpAdapter` 接口，确保:

- **统一接口**: 连接测试、凭证同步、附件获取
- **工厂模式**: `ErpAdapterFactory` 动态创建适配器
- **元数据注解**: `@ErpAdapterAnnotation` 声明式配置
- **自动注册**: Spring 自动扫描并注册适配器

### 7.2 AI 适配器生成

支持从 OpenAPI 规范自动生成适配器:

1. 上传 OpenAPI/Swagger 文档
2. AI 解析 API 定义
3. 生成 Java 适配器代码
4. 编译并热部署到运行时
5. 注册到 `sys_erp_adapter` 表

### 7.3 账套-全宗映射

- **合规性**: 一个全宗只能映射一个 ERP 账套
- **后端强制路由**: 同步接口根据用户当前全宗自动路由
- **存储格式**: JSON `{"BR01": "FONDS_A", "BR02": "FONDS_B"}`

### 7.4 组织架构同步

- **数据源**: YonSuite 组织树 API
- **同步方式**: 调用 `treeversionsync` 和 `treemembersync`
- **存储**: `sys_entity` 表，支持树形结构 (`parent_id`)
- **增量同步**: 基于 `pubts` 时间戳

### 7.5 Webhook 集成

- **签名验证**: HMAC-SHA256
- **防重放**: Nonce 存储
- **事件解密**: `YonSuiteEventCrypto`
- **健壮性**: 支持 GET 健康检查和空 Body POST

---

## 八、安全特性

| 特性 | 实现 |
|------|------|
| 凭证加密 | SM4 加密存储在 `config_json` |
| Webhook 签名 | HMAC-SHA256 验证 |
| 防重放攻击 | Nonce 存储 (Redis) |
| 审计日志 | 所有配置变更记录到 `sys_audit_log` |
| 权限控制 | 需要 ADMIN 角色管理 ERP 配置 |

---

## 九、相关链接

- [主文档首页](../README.md)
- [ERP 集成原则](knowledge/erp_integration.md)
- [用友集成指南](guides/用友集成.md)
- [适配器开发指南](architecture/erp-adapter-development-guide.md)
- [AI 适配器使用指南](architecture/erp-ai-usage-guide.md)
