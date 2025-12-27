# Contract Catalog（对外契约清单）

> 目的：明确模块对外暴露的 Command / Query / Event，并为版本化提供入口。

---

## Borrowing（Backend）

### Command / API

| 名称 | 位置 | 说明 |
| --- | --- | --- |
| `createBorrowing` | `BorrowingFacade` / `POST /borrowing` | 创建借阅申请 |
| `approveBorrowing` | `BorrowingFacade` / `POST /borrowing/{id}/approve` | 审批借阅 |
| `returnArchive` | `BorrowingFacade` / `POST /borrowing/{id}/return` | 归还借阅 |
| `cancelBorrowing` | `BorrowingFacade` / `POST /borrowing/{id}/cancel` | 取消借阅 |

### Query

| 名称 | 位置 | 说明 |
| --- | --- | --- |
| `getBorrowings` | `BorrowingFacade` / `GET /borrowing` | 借阅列表查询 |

### Event

- 暂无（TBD）

### DTO / 类型

- `com.nexusarchive.modules.borrowing.api.dto.*`

---

## SYS(Settings)（Frontend）

### Command / API

| 名称 | 位置 | 说明 |
| --- | --- | --- |
| `useAdminSettingsApi` | `src/features/settings/index.ts` | 系统参数/用户/角色/组织相关变更 |
| `useIntegrationSettingsApi` | `src/features/settings/index.ts` | 集成配置变更 |
| `useLicenseSettingsApi` | `src/features/settings/index.ts` | License 更新 |

### Query

| 名称 | 位置 | 说明 |
| --- | --- | --- |
| `useAuditSettingsApi` | `src/features/settings/index.ts` | 审计日志查询 |

### Event

- 暂无（TBD）

---

## 版本化策略（最小要求）

- 对外契约变更必须有版本策略（路径 / header / DTO 版本字段）。
- 未声明版本策略的变更视为破坏性变更。
