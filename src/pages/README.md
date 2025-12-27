// Input: 页面入口组件
// Output: 极简架构说明
// Pos: src/pages/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 页面入口层 (Pages)

本目录作为路由 (Routes) 直接引用的页面容器。

## 目录定位

- **路由终点**: `src/routes/index.tsx` 中的组件应统一定位于此。
- **模块装配**: 页面层只通过 `src/features/<module>/index.ts` 入口引入业务能力。
- **UI 注入**: 负责把 features 的 application 能力注入到 UI 组件（components）。
