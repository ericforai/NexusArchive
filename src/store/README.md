// Input: 全局状态管理
// Output: 极简架构说明
// Pos: src/store/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 状态管理层 (Store)

本目录使用 **Zustand** 管理应用的全局共享状态。

## 主要 Store

- `useUserStore`: 存储当前登录用户信息、权限。
- `useConfigStore`: 存储系统级配置（如信创环境、主题）。
- `useArchiveStore`: 跨页面的档案筛选与操作缓存。

## 建议

1. **按需创建**: 仅在多个不相干组件需要共享状态时才使用 Store。
2. **派生状态**: 优先使用组件内的 `useMemo` 计算派生状态，避免在 Store 中存储冗余数据。
