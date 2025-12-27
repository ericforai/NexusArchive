// Input: 档案业务领域模块
// Output: 极简架构说明
// Pos: src/features/archives/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案业务领域 (Archives Feature)

本目录包含档案管理核心业务逻辑与配置。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `routeConfigs.ts` | 强类型路由配置 (ArchiveRouteMode) |
| `index.ts` | 模块入口导出 |

## 架构约束

- 禁止依赖 `src/components/*`
- 被 `src/pages/*` 和页面组件调用
