# Module Manifest（模块清单）

> 本清单是模块边界与依赖关系的单一事实来源（SSOT）。
> **版本**: 2.2.0
> **更新日期**: 2026-01-02
> **自动生成**: 通过 `ModuleGovernanceService.discoverNewModules()` 发现

---

## Frontend Modules

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FE.ARCHIVES | Archives | `src/pages/archives` + `src/components/voucher` | 档案管理/凭证预览（Drawer+Page 双模式） | `src/api`, `src/store`, `src/utils`, `src/hooks`, `src/types.ts` | ✅ 活跃 v2.2 |
| FE.SYS | SYS(Settings) | `src/features/settings` + `src/pages/settings` + `src/components/settings` | 系统基础配置/字典/日志（不含 Admin/Fonds） | `src/api`, `src/store`, `src/utils`, `src/hooks`, `src/types.ts` | ✅ 锁定 |
| FE.ADMIN | Admin | `src/pages/admin` + 相关组件 | 用户/角色/全宗等"活"业务数据 | `src/api`, `src/store`, `src/utils`, `src/types.ts` | ⏳ 待收敛 |
| FE.SHARED | Shared | `src/api`, `src/store`, `src/utils`, `src/hooks`, `src/types.ts`, `src/constants.tsx`, `src/queryClient.ts` | 跨模块通用能力与基础设施 | 无跨模块依赖 | ✅ 基础 |

### 新增组件模块 (v2.2)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FE.DRAWER | Drawer UI | `src/pages/archives/ArchiveDetailDrawer.tsx` | 凭证预览抽屉（响应式 50vw/70vw/100vw） | `src/store/useDrawerStore`, `src/components/voucher` | ✅ 活跃 v2.2 |
| FE.VOUCHER_PREVIEW | Voucher Preview | `src/components/voucher/*` | 凭证预览组件（Metadata/Canvas/Tabs） | `src/api`, `src/types.ts` | ✅ 活跃 v2.2 |
| FE.DRAWER_STORE | Drawer State | `src/store/useDrawerStore.ts` | 抽屉状态管理（Zustand） | 无 | ✅ 活跃 v2.2 |

---

## Backend Modules

### 核心架构层 (Core Layers)

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.CONTROLLER | Controller Layer | `com.nexusarchive.controller` | REST API 端点 | `com.nexusarchive.service..`, `com.nexusarchive.dto..` | ✅ 活跃 |
| BE.SERVICE | Service Layer | `com.nexusarchive.service` | 业务逻辑实现 | `com.nexusarchive.mapper..`, `com.nexusarchive.entity..`, `com.nexusarchive.dto..` | ✅ 活跃 |
| BE.MAPPER | Mapper Layer | `com.nexusarchive.mapper` | 数据访问层 (MyBatis-Plus) | `com.nexusarchive.entity..` | ✅ 活跃 |
| BE.ENTITY | Entity Layer | `com.nexusarchive.entity` | 数据模型定义 | 通用库 | ✅ 活跃 |
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

- **2026-01-02 v2.2.0**: 新增凭证预览抽屉系统模块（FE.ARCHIVES, FE.DRAWER, FE.VOUCHER_PREVIEW, FE.DRAWER_STORE）
- **2026-01-01 v2.1.0**: 添加模块化组件 (INGEST, VOUCHER, MATCHING)、基础设施模块、集成层
- **2025-12-31 v2.0.0**: 初始版本，包含 BORROWING 试点模块
