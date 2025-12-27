// Input: archives 页面模块
// Output: 极简架构说明
// Pos: src/pages/archives/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案管理页面 (Archives Pages)

本目录包含档案管理相关的页面容器组件。

## 文件清单

| 文件 | 功能 |
| --- | --- |
| `ArchiveListPage.tsx` | 档案列表页面容器，接收 `routeConfig` 并渲染 `ArchiveListView` |

## 架构约束

- Page 层仅负责"胶水"逻辑
- 禁止在此编写底层 HTML/CSS
- 业务逻辑应抽离至 `src/features/archives/`
