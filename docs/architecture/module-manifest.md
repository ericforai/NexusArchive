# Module Manifest（模块清单）

> 本清单是模块边界与依赖关系的单一事实来源（SSOT）。
> 当前覆盖试点模块 + 核心共享模块，其他业务域后续补齐。

---

## Frontend Modules

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FE.SYS | SYS(Settings) | `src/features/settings` + `src/pages/settings` + `src/components/settings` | 系统基础配置/字典/日志（不含 Admin/Fonds） | `src/api`, `src/store`, `src/utils`, `src/hooks`, `src/types.ts` | ✅ 锁定 |
| FE.ADMIN | Admin | `src/pages/admin` + 相关组件 | 用户/角色/全宗等“活”业务数据 | `src/api`, `src/store`, `src/utils`, `src/types.ts` | ⏳ 待收敛 |
| FE.SHARED | Shared | `src/api`, `src/store`, `src/utils`, `src/hooks`, `src/types.ts`, `src/constants.tsx`, `src/queryClient.ts` | 跨模块通用能力与基础设施 | 无跨模块依赖 | ✅ 基础 |

---

## Backend Modules

| 模块 ID | 名称 | 范围 | 职责一句话 | 允许依赖 | 状态 |
| --- | --- | --- | --- | --- | --- |
| BE.BORROWING | Borrowing | `com.nexusarchive.modules.borrowing` | 借阅全生命周期（申请/审批/归还/取消） | `com.nexusarchive.common..`, `com.nexusarchive.security..`, `com.nexusarchive.service.DataScopeService`, `com.nexusarchive.service.ArchiveService` | ✅ 锁定 |
| BE.CORE | Core (Legacy) | `com.nexusarchive.{controller,service,mapper,entity,common,config,security}` | 现存核心域与基础设施（待模块化拆分） | Spring/DB/通用库 | ⏳ 待拆分 |

---

## 备注

- 本 Manifest 仅覆盖已锁定试点模块 + Core 基座，后续需补齐其他业务域模块。
- 允许依赖以“模块级 + 关键共享服务”为准，不代表细粒度包级规则。更细粒度约束由 ESLint/ArchUnit 兜底。
