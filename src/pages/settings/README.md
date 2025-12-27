// Input: 系统设置页面容器
// Output: 极简架构说明
// Pos: src/pages/settings/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# Settings Pages

本目录为系统设置模块的 Page 容器层，仅负责：

1. 从 `src/features/settings` 入口获取业务 API（application）
2. 向 `src/components/settings` 注入 props
3. 作为路由出口（routes 仅引用 pages）

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `SettingsLayoutPage.tsx` | 布局容器 | 设置页整体布局 |
| `BasicSettingsPage.tsx` | 页面容器 | 基础设置 |
| `UserSettingsPage.tsx` | 页面容器 | 用户管理 |
| `RoleSettingsPage.tsx` | 页面容器 | 角色权限 |
| `OrgSettingsPage.tsx` | 页面容器 | 组织架构 |
| `SecuritySettingsPage.tsx` | 页面容器 | 安全合规 |
| `IntegrationSettingsPage.tsx` | 页面容器 | 集成中心 |
| `AuditLogView.tsx` | 页面容器 | 审计日志 |
