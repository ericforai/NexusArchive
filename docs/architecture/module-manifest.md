# Module Manifest（模块清单）

> 本清单是模块边界与依赖关系的单一事实来源（SSOT）。
> **版本**: 2.3.0
> **更新日期**: 2026-03-09
> **自动生成**: 通过 scripts/discover-frontend-modules.js

---

## Frontend Modules

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FE.PAGES | 页面容器层 | src/pages/Auth, src/pages/admin, src/pages/archives, src/pages/audit, src/pages/collection, src/pages/debug, src/pages/demo, src/pages/matching, src/pages/operations, src/pages/panorama, src/pages/portal, src/pages/pre-archive, src/pages/product-website, src/pages/quality, src/pages/scan, src/pages/security, src/pages/settings, src/pages/stats, src/pages/system, src/pages/utilization | 页面级容器组件 | 188 files | ✅ ACTIVE |
| FE.COMPONENTS | 通用组件层 | src/components/GlobalSearch, src/components/Sidebar, src/components/TopBar, src/components/admin, src/components/archive, src/components/auth, src/components/borrowing, src/components/common, src/components/debug, src/components/dev, src/components/hooks, src/components/layout, src/components/matching, src/components/modals, src/components/operations, src/components/org, src/components/pages, src/components/panorama, src/components/pool-dashboard, src/components/pool-kanban, src/components/preview, src/components/relation-graph, src/components/scan, src/components/settings, src/components/table, src/components/voucher, src/components/watermark | 可复用 UI 组件 | 201 files | ✅ ACTIVE |
| FE.STORE | 状态管理层 | src/store/__tests__ | Zustand 全局状态 | 8 files | ✅ ACTIVE |
| FE.API | API 客户端层 | src/src/api | 后端 API 调用封装 | 48 files | ✅ ACTIVE |
| FE.HOOKS | 自定义 Hooks | src/hooks/__tests__ | React 自定义 Hooks | 19 files | ✅ ACTIVE |
| FE.UTILS | 工具函数层 | src/src/utils | 通用工具函数 | 6 files | ✅ ACTIVE |

### 前端子模块详情

#### FE.PAGES - 页面容器层

- `src/pages/Auth`: 6 files
- `src/pages/admin`: 22 files
- `src/pages/archives`: 24 files
- `src/pages/audit`: 4 files
- `src/pages/collection`: 6 files
- `src/pages/debug`: 3 files
- `src/pages/demo`: 2 files
- `src/pages/matching`: 5 files
- `src/pages/operations`: 19 files
- `src/pages/panorama`: 9 files
- `src/pages/portal`: 2 files
- `src/pages/pre-archive`: 11 files
- `src/pages/product-website`: 34 files
- `src/pages/quality`: 7 files
- `src/pages/scan`: 2 files
- `src/pages/security`: 3 files
- `src/pages/settings`: 15 files
- `src/pages/stats`: 2 files
- `src/pages/system`: 6 files
- `src/pages/utilization`: 5 files

#### FE.COMPONENTS - 通用组件层

- `src/components/GlobalSearch`: 4 files
- `src/components/Sidebar`: 1 files
- `src/components/TopBar`: 6 files
- `src/components/admin`: 1 files
- `src/components/archive`: 1 files
- `src/components/auth`: 3 files
- `src/components/borrowing`: 4 files
- `src/components/common`: 29 files
- `src/components/debug`: 1 files
- `src/components/dev`: 1 files
- `src/components/hooks`: 4 files
- `src/components/layout`: 3 files
- `src/components/matching`: 1 files
- `src/components/modals`: 7 files
- `src/components/operations`: 10 files
- `src/components/org`: 7 files
- `src/components/pages`: 2 files
- `src/components/panorama`: 1 files
- `src/components/pool-dashboard`: 3 files
- `src/components/pool-kanban`: 11 files
- `src/components/preview`: 13 files
- `src/components/relation-graph`: 7 files
- `src/components/scan`: 3 files
- `src/components/settings`: 45 files
- `src/components/table`: 8 files
- `src/components/voucher`: 14 files
- `src/components/watermark`: 2 files

#### FE.STORE - 状态管理层

- `src/store/__tests__`: 1 files

#### FE.HOOKS - 自定义 Hooks

- `src/hooks/__tests__`: 6 files

## Backend Modules

### 核心架构层 (Core Layers)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.CONTROLLER | Controller Layer | `com.nexusarchive.controller` | REST API 端点 | `com.nexusarchive.service..`, `com.nexusarchive.dto..` | ✅ 活跃 |
| BE.SERVICE | Service Layer | `com.nexusarchive.service` | 业务逻辑实现 | `com.nexusarchive.mapper..`, `com.nexusarchive.entity..`, `com.nexusarchive.dto..` | ✅ 活跃 |
| BE.MAPPER | Mapper Layer | `com.nexusarchive.mapper` | 数据访问层 (MyBatis-Plus) | `com.nexusarchive.entity..` | ✅ 活跃 |
| BE.ENTITY | Entity Layer | `com.nexusarchive.entity` | 数据模型定义 | 通用库 | 64 files | ✅ 活跃 |
| BE.DTO | DTO Layer | `com.nexusarchive.dto` | 数据传输对象 | `com.nexusarchive.entity..` | ✅ 活跃 |
| BE.CONFIG | Config Layer | `com.nexusarchive.config` | 系统配置 | Spring 生态 | ✅ 活跃 |
| BE.SECURITY | Security Layer | `com.nexusarchive.security` | 安全认证授权 | Spring Security | ✅ 活跃 |

### 模块化组件 (Modularized Components)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.BORROWING | Borrowing Module | `com.nexusarchive.modules.borrowing` | 借阅全生命周期（申请/审批/归还/取消） | `com.nexusarchive.common..`, `com.nexusarchive.security..`, `com.nexusarchive.service.DataScopeService`, `com.nexusarchive.service.ArchiveService` | ✅ 锁定 v2.0 |
| BE.INGEST | Ingest Module | `com.nexusarchive.service.ingest` | SIP 摄取处理 | `com.nexusarchive.service..`, `com.nexusarchive.common..` | ✅ 活跃 v2.0 |
| BE.VOUCHER | Voucher Module | `com.nexusarchive.service.voucher` | 凭证处理 | `com.nexusarchive.service..`, `com.nexusarchive.common..` | ✅ 活跃 v2.0 |
| BE.MATCHING | Matching Engine | `com.nexusarchive.engine.matching` | 对账匹配引擎 (Strategy Pattern) | `com.nexusarchive.service..`, `com.nexusarchive.common..` | ✅ 活跃 v2.0 |

### 集成层 (Integration Layer)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.INTEGRATION | Integration Layer | `com.nexusarchive.integration` | 外部系统集成 (ERP 适配器) | `com.nexusarchive.service..`, Spring Integration | ✅ 活跃 |
| BE.ERP_PLUGINS | ERP Plugins | `com.nexusarchive.service.erp.plugin` | ERP 插件 (Plugin Architecture) | `com.nexusarchive.integration..`, `com.nexusarchive.service..` | ✅ 活跃 v2.0 |
| BE.AI | AI 驱动模块 | `com.nexusarchive.integration.erp.ai` | AI 驱动的 ERP 适配自动生成 | `com.nexusarchive.integration..`, `com.nexusarchive.service..`, Swagger Parser | ✅ NEW (2026-01-02) |

#### BE.AI - AI 驱动模块子组件

- `parser/`: OpenAPI 文档解析器
- `mapper/`: 业务语义映射器
- `generator/`: 代码生成器
- `agent/`: Agent 编排器
- `controller/`: REST API 控制器

### ERP 集成子模块 (v2.4)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.ERP.ADAPTER_YONSUITE | YonSuite 适配器 | `integration.erp.adapter.YonSuiteErpAdapter` | 用友 YonSuite ERP 集成 | Spring Integration, `integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_KINGDEE | 金蝶适配器 | `integration.erp.adapter.KingdeeAdapter` | 金蝶云星空 ERP 集成 | Spring Integration, `integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_WEAVER | 浪潮适配器 | `integration.erp.adapter.WeaverAdapter` | 浪潮 GS ERP 集成 | Spring Integration, `integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_WEAVER_E10 | 泛微E10适配器 | `integration.erp.adapter.WeaverE10Adapter` | 泛微 E10 OA 集成 | Spring Integration, `integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.ADAPTER_GENERIC | 通用适配器 | `integration.erp.adapter.GenericErpAdapter` | 通用 ERP 集成协议 | Spring Integration, `integration.erp.dto..` | ✅ 活跃 v2.4 |
| BE.ERP.METADATA | 元数据管理 | `integration.erp.annotation..`, `integration.erp.registry..` | 适配器元数据注册与查询 | `integration.erp.dto..` | ✅ 活跃 v2.4 |

### 基础设施 (Infrastructure)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.COMMON | Common | `com.nexusarchive.common` | 通用工具类、异常、结果 | 通用库 | ✅ 基础 |
| BE.UTIL | Util | `com.nexusarchive.util` | 工具类集合 | 通用库 | ✅ 基础 |
| BE.ANNOTATION | Annotation | `com.nexusarchive.annotation` | 自定义注解 | 无 | ✅ 基础 |
| BE.ASPECT | Aspect | `com.nexusarchive.aspect` | AOP 切面 | Spring AOP | ✅ 基础 |
| BE.EVENT | Event | `com.nexusarchive.event` | 事件定义 | Spring Events | ✅ 基础 |
| BE.LISTENER | Listener | `com.nexusarchive.listener` | 事件监听器 | Spring Events | ✅ 基础 |
| BE.SERIALIZER | Serializer | `com.nexusarchive.serializer` | 序列化配置 | Jackson | ✅ 基础 |
| BE.EXCEPTION | Exception | `com.nexusarchive.exception` | 异常定义 | 无 | ✅ 基础 |
| BE.REPOSITORY | Repository | `com.nexusarchive.repository` | 自定义 Repository | Spring Data | ⏳ 实验性 |
| BE.INFRASTRUCTURE | Infrastructure | `com.nexusarchive.infrastructure` | 基础设施抽象 | DDD 架构 | ⏳ 实验性 |

---

## 备注

- 本 Manifest 通过 `ModuleGovernanceService.discoverNewModules()` 自动发现维护
- 允许依赖以"模块级 + 关键共享服务"为准，不代表细粒度包级规则
- 更细粒度约束由 dependency-cruiser (前端) 和 ArchUnit (后端) 兜底
- 状态说明: ✅ 活跃 = 正常使用，✅ 锁定 = 模块边界已固定，⏳ 实验性/待收敛 = 正在演进

---

## 更新日志

- **2026-01-06 v2.5.0**: 同步 2026-01-05 重大重构后的文件统计，补全后端核心实体清单
- **2026-01-02 v2.4.0**: ERP 模块重构 - 统一 Adapter 架构，添加注解驱动的元数据系统
- **2026-01-02 v2.2.0**: 新增凭证预览抽屉系统模块（FE.ARCHIVES, FE.DRAWER, FE.VOUCHER_PREVIEW, FE.DRAWER_STORE）
- **2026-01-01 v2.1.0**: 添加模块化组件 (INGEST, VOUCHER, MATCHING)、基础设施模块、集成层
- **2025-12-31 v2.0.0**: 初始版本，包含 BORROWING 试点模块
