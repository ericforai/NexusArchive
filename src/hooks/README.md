<!-- 一旦我所属的文件夹有所变化，请更新我。-->
<!-- 本目录存放自定义 React Hooks。-->
<!-- 用于封装数据获取、权限逻辑与文档守卫。-->
<!-- 最后更新: 2026-01-09 -->

## 用途

本目录集中管理项目自定义 React Hooks，包括：
- 数据获取与状态封装（React Query 集成）
- 权限检查与认证状态
- 文档自洽性守卫（开发辅助）
- 看板数据管理

## 目录结构

```
src/hooks/
├── index.ts                    # Hook 统一导出
├── useArchives.ts              # 档案数据获取
├── usePoolKanban.ts            # 预归档池看板数据管理
├── usePermissions.ts           # 权限检查
├── useDocumentationGuard.ts    # 文档自洽性守卫
├── useReconciliation.ts        # 对账相关逻辑
├── useMonitoring.ts            # 监控数据获取
├── useGlobalSearchApi.ts        # 全局搜索 API
├── useSettings.ts              # 设置相关状态管理
└── __tests__/                  # Hook 单元测试（如需要）
```

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `index.ts` | 聚合入口 | Hook 统一导出 |
| `useArchives.ts` | Hook | 档案数据获取与状态封装 |
| `usePoolKanban.ts` | Hook | 预归档池看板数据管理，按列和子状态分组 |
| `usePermissions.ts` | Hook | 权限检查与权限状态 |
| `useDocumentationGuard.ts` | Hook | 文档自洽性守卫，代码变更后提醒更新文档 |
| `useReconciliation.ts` | Hook | 对账相关逻辑 |
| `useMonitoring.ts` | Hook | 监控数据获取 |
| `useGlobalSearchApi.ts` | Hook | 全局搜索 API |
| `useSettings.ts` | Hook | 设置相关状态管理 |
