// Input: 全局状态管理
// Output: 极简架构说明
// Pos: src/store/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 状态管理层 (Store)

本目录使用 **Zustand** 管理应用的全局共享状态。

## 主要 Store

| Store | 职责 | 状态 |
| --- | --- | --- |
| `useUserStore` | 当前登录用户信息、权限 | ✅ 活跃 |
| `useAuthStore` | 认证状态、Token 管理 | ✅ 活跃 |
| `useConfigStore` | 系统级配置（如信创环境、主题） | ✅ 活跃 |
| `useArchiveStore` | 跨页面的档案筛选与操作缓存 | ✅ 活跃 |
| `useFondsStore` | 全宗选择器状态 | ✅ 活跃 |
| `useDrawerStore` | 凭证预览抽屉状态（2026-01-02 新增） | ✅ 活跃 |

## 2026-01-02 更新

- ✅ 新增 `useDrawerStore` - 抽屉状态管理
  - `isOpen`: 抽屉开闭状态
  - `activeTab`: 当前激活标签页
  - `archiveId`: 当前查看档案ID
  - `expandedMode`: 是否展开到全页模式

## 建议

1. **按需创建**: 仅在多个不相干组件需要共享状态时才使用 Store。
2. **派生状态**: 优先使用组件内的 `useMemo` 计算派生状态，避免在 Store 中存储冗余数据。
3. **最小化状态**: Store 仅存储核心状态，派生状态通过计算获取。
