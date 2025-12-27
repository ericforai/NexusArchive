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
| `useSmartMatching.ts` | 智能匹配核心逻辑 Hook |
| `useArchiveListController.ts` | 列表数据加载与状态管理 Hook |
| `useArchiveActions.ts` | 档案操作（归档、删除、导出等）Hook |
| `index.ts` | 模块入口导出 |

## 架构约束

- 禁止依赖 `src/components/*`
- 被 `src/pages/*` 和页面组件调用
