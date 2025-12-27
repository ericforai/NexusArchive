# Module Boundaries (Boundary Map + Contracts + Allowed Imports)

本文件用于固化模块化边界与依赖约束（可机器校验）。

## 自审材料入口

- [Module Manifest](module-manifest.md)
- [Data Ownership Map](data-ownership-map.md)
- [Contract Catalog](contract-catalog.md)
- [Variability Registry](variability-registry.md)
- [Self-Review SOP](self-review-sop.md)

## Boundary Map

### Frontend (Code Modules)

| 模块 | 范围 | 入口 | 备注 |
| --- | --- | --- | --- |
| SYS(Settings) | `src/features/settings` + `src/pages/settings` + `src/components/settings` | `src/features/settings/index.ts` | 仅基础配置/字典/日志；不含 Admin/Fonds |
| Admin | `src/pages/admin` + 相关组件 | `src/pages/admin/AdminLayout.tsx` | Fonds/用户角色等“活”业务数据 |

### Backend (Modules)

| 模块 | 层级 | 对外入口 | 备注 |
| --- | --- | --- | --- |
| Borrowing | `api/app/domain/infra` | `BorrowingFacade` + `api.dto` | 仅 app 与 dto 可被外部依赖 |

## Contract 列表

### Frontend

- `src/features/settings/index.ts`
  - `useAdminSettingsApi`
  - `useIntegrationSettingsApi`
  - `useLicenseSettingsApi`
  - `useAuditSettingsApi`
  - settings domain types

### Backend

- `com.nexusarchive.modules.borrowing.app.BorrowingFacade`
- `com.nexusarchive.modules.borrowing.api.dto.*`

## Allowed Imports 列表

### Frontend

- `src/pages/settings/*` 只能从 `src/features/settings/index.ts` 引入模块能力
- `src/pages/*` 仅允许从 `src/features/<module>/index.ts` 引入（禁止 deep import）
- `src/components/**` 禁止引入 `features/pages/api/store`
- `src/routes/**` 仅允许引入 `pages` + `layouts` + `components/common` + `auth`

### Backend

- 外部模块仅允许依赖：
  - `com.nexusarchive.modules.borrowing.app..`
  - `com.nexusarchive.modules.borrowing.api.dto..`
- 禁止外部访问：
  - `com.nexusarchive.modules.borrowing.domain..`
  - `com.nexusarchive.modules.borrowing.infra..`
