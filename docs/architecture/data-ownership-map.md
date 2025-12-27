# Data Ownership Map（数据主权清单）

> 目的：明确每个核心对象的唯一所有者，避免多裁判写入。
> 当前以试点 + Core 现状为基线，逐步细化。

---

## 核心对象与 Owner

| 对象 | Owner 模块 | 权威来源 | 主要写入入口 | 备注 |
| --- | --- | --- | --- | --- |
| Borrowing | BE.BORROWING | `modules/borrowing`（BorrowingMapper） | `BorrowingFacade` | 借阅唯一主权 |
| Archive | BE.CORE | `ArchiveService` / `acc_archive` | `ArchiveService` | 归档主数据 |
| User | BE.CORE | `UserMapper` / `sys_user` | `UserService` | 用户主数据 |
| Role | BE.CORE | `RoleMapper` / `sys_role` | `RoleService` | 角色主数据 |
| Org | BE.CORE | `OrgMapper` / `sys_org` | `OrgService` | 组织主数据 |
| AuditLog | BE.CORE | `sys_audit_log` | 审计服务 | 合规日志 |
| SystemSetting | BE.CORE | 系统配置表 | 系统配置服务 | SYS 前端仅消费 |
| Dict | BE.CORE | 数据字典表 | 字典服务 | SYS 前端仅消费 |
| IntegrationConfig | BE.CORE | ERP 配置表 | ERP 配置服务 | 集成配置 |
| License | BE.CORE | License 存储 | License 服务 | 授权 |

---

## 约束说明

- **唯一写路径**：任何对象的创建/变更必须走 Owner 模块入口。
- **快照允许**：业务侧可保存快照用于审计，但不得回写主数据。
- **待补齐项**：其他业务域对象需逐步补齐 Owner 与写路径。
