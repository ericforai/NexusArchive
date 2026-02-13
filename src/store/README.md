一旦我所属的文件夹有所变化，请更新我。

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
| `useRelationGraphStore` | 关系图谱状态（2026-01-13 新增） | ✅ 活跃 |

## 2026-01-13 更新

- ✅ 新增 `useRelationGraphStore` - 穿透联查关系图谱状态管理
  - `nodes`: 图谱节点列表
  - `edges`: 图谱连线列表
  - `centerNodeId`: 中心节点ID
  - `expandedNodeIds`: 已展开节点集合
  - `nodeDepths`: 节点深度映射
  - `initializeGraph(archiveId)`: 初始化图谱
  - `expandNode(nodeId)`: 展开节点关联（最多3度）
  - `collapseNode(nodeId)`: 折叠节点
  - `resetGraph()`: 重置图谱

## 2026-02-13 更新

- ✅ `useRelationGraphStore.initializeGraph` 调整为保留查询返回的全量关系图节点与边
  - 支撑前端三栏布局的多层级链路展示与逐层展开
  - 兼容现有错误处理、重置逻辑与节点抽屉预览流程
- ✅ 新增 `directionalView` 状态
  - 优先承载后端返回的上下游方向解析结果（upstream/downstream/layers/mainline）
  - 三栏布局优先消费后端方向，避免前端固定流程推断造成“上下游重复”

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
