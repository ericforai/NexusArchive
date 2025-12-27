// Input: 页面入口组件
// Output: 极简架构说明
// Pos: src/pages/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 页面入口层 (Pages)

本目录作为路由 (Routes) 直接引用的页面容器。

## 目录定位

- **路由终点**: `src/routes/index.tsx` 中的组件应统一定位于此。
- **状态注入**: 负责从 Store 或 API 获取初始数据并分发给 Components。

## 迁移计划

> [!NOTE]
> 当前多为 `src/components/*View.tsx`，后续将逐步重构为本目录下的页面组件。

1. `ArchiveListView.tsx` -> `src/pages/Archives/ArchiveList.tsx`
2. `LoginView.tsx` -> `src/pages/Auth/Login.tsx`
3. ...
