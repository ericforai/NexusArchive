# 穿透联查渐进式展开设计

**日期**: 2026-01-13
**状态**: 设计已确认
**优先级**: 中

---

## 需求概述

实现"输入任意档号，自动生成全链路业务关系"的X光穿透效果。用户可点击任意节点动态展开其关联网络，形成可探索的关系图谱。

### 核心决策

| 决策项 | 方案 |
|--------|------|
| 穿透深度 | 渐进式：默认1度，点击展开 |
| 关系发现 | 仅展示已有关联（依赖 `acc_archive_relation` 表） |
| 交互方式 | 保持+展开：中心固定，节点动态展开 |
| 可视化技术 | @xyflow/react (已依赖 v12.9.3) |
| 画布模式 | 无限画布 + 聚焦模式 |
| 节点配色 | 保持现有类型配色 |
| 关系线 | 双向箭头 |
| 数据加载 | 混合模式：缓存 + 后台刷新 |
| 边界处理 | 3度自动折叠 |

---

## 架构设计

### 组件结构

```
RelationshipQueryView (主容器)
├── RelationGraphCanvas (@xyflow/react 画布)
│   ├── RelationNodeComponent (自定义节点)
│   └── RelationEdgeComponent (自定义连线)
├── NodeDetailDrawer (右侧详情抽屉)
└── GraphControls (控制栏：缩放、适配视图、重置)
```

### 状态管理 (Zustand)

```typescript
interface RelationGraphState {
  // 图谱数据
  nodes: Node[];
  edges: Edge[];
  centerNodeId: string;

  // 展开/折叠状态
  expandedNodes: Set<string>;
  nodeDepths: Map<string, number>;

  // 缓存
  loadedRelations: Map<string, RelationGraphDto>;

  // 操作
  expandNode: (nodeId: string) => Promise<void>;
  collapseNode: (nodeId: string) => void;
  resetGraph: () => void;
}
```

### 数据流

```
1. 用户输入档号 → GET /relations/{archiveId}/graph → 初始化图谱（1度关系）
2. 用户点击节点 → 检查缓存/调用 API → 合并新节点/边到图谱
3. 展开时检查深度 → 超过3度则折叠最早的节点
4. 后台异步刷新已展开节点的数据
```

---

## 组件设计

### 节点组件 (RelationNodeComponent)

```typescript
interface NodeData {
  id: string;
  code: string;        // 档号
  name: string;        // 名称
  type: 'contract' | 'invoice' | 'voucher' | 'receipt' | 'report' | 'ledger';
  amount: string;
  date: string;
  status: string;
  depth: number;       // 距中心的深度 (0=中心)
  isCenter: boolean;
  isExpanded: boolean;
}
```

**类型配色**

| 类型 | 背景 | 边框 | 图标 |
|------|------|------|------|
| contract | indigo-50 | indigo-400 | Building |
| invoice | purple-50 | purple-400 | Receipt |
| voucher | blue-50 | blue-400 | FileText |
| receipt | emerald-50 | emerald-400 | CreditCard |
| report | amber-50 | amber-400 | FileSpreadsheet |
| ledger | slate-50 | slate-400 | FileSpreadsheet |

**节点结构**

```
┌─────────────────────────────────┐
│ [图标]  类型标签         [展开] │
├─────────────────────────────────┤
│ 档号: JZ-202311-0052            │
│ 名称: 采购付款凭证               │
├─────────────────────────────────┤
│ 金额: ¥ 12,500.00   [已归档]    │
│ 日期: 2023-11-15                │
└─────────────────────────────────┘
```

**中心节点特殊样式**
- 更大尺寸 (220x140)
- 金色边框 + 阴影
- 脉冲动画效果
- 标签显示"核心单据"

---

### 连线组件 (RelationEdgeComponent)

```typescript
interface EdgeData {
  relationType: string;  // BASIS, ORIGINAL_VOUCHER, CASH_FLOW, ARCHIVE, SYSTEM_AUTO
  description: string;
}
```

**连线样式**
- 双向箭头（两端都有箭头标记）
- 默认灰色 (#94a3b8)，悬停时主色 (#3b82f6)
- 中间显示关系类型徽章
- 平滑曲线 (Bezier)

---

## 交互逻辑

### 展开节点流程

```
用户点击节点
    ↓
检查是否已展开
    ├─ 已展开 → 折叠该节点（移除其所有子孙节点）
    └─ 未展开 → 继续
        ↓
    检查深度限制
        ├─ 当前深度 ≥ 3 → 提示，折叠最早的1度节点
        └─ 当前深度 < 3 → 继续
            ↓
        检查缓存
            ├─ 有缓存 → 使用缓存，后台刷新
            └─ 无缓存 → 调用 API /relations/{nodeId}/graph
            ↓
        合并新节点/边到图谱
            ↓
        标记节点为已展开
            ↓
        触发 fitView() 聚焦新节点
            ↓
        300ms 后后台异步刷新数据
```

### 自动折叠逻辑

```typescript
const expandNode = async (nodeId: string) => {
  const currentDepth = nodeDepths.get(nodeId) ?? 0;

  // 深度检查
  if (currentDepth >= 3) {
    // 找到深度为 1 的节点，折叠其中一个
    const depth1Nodes = Array.from(nodeDepths.entries())
      .filter(([_, depth]) => depth === 1)
      .map(([id]) => id);
    if (depth1Nodes.length > 0) {
      collapseNode(depth1Nodes[0]);
    }
  }

  // 继续展开...
};
```

### 画布控制

| 操作 | 实现 |
|------|------|
| 自动适配 | `reactFlow.fitView({ padding: 0.2 })` |
| 缩放 | `zoomIn` / `zoomOut`，范围 0.3x - 2x |
| 重置 | 清空展开状态，恢复初始图谱 |
| 拖拽 | React Flow 内置 |

---

## 错误处理

### API 调用失败

| 场景 | 处理方式 |
|------|---------|
| 401 未授权 | 跳转登录页，清空图谱状态 |
| 403 无权限 | Toast "无权访问该档案关系" |
| 404 不存在 | Toast "档案不存在" |
| 500 网络错误 | Toast "加载失败，点击重试" |
| 超时 | 15秒后提示超时，允许重试 |

### 空状态

| 场景 | 显示 |
|------|------|
| 无关联节点 | "暂无关联关系"空状态 |
| 搜索无结果 | "未找到档案，请检查档号" |
| 加载中 | 节点骨架屏 + Loading |

### 数据一致性

```typescript
// 防止重复节点
const mergeNodes = (existing: Node[], incoming: Node[]) => {
  const existingIds = new Set(existing.map(n => n.id));
  return [...existing, ...incoming.filter(n => !existingIds.has(n.id))];
};

// 防止重复边
const mergeEdges = (existing: Edge[], incoming: Edge[]) => {
  const existingKeys = new Set(existing.map(e => `${e.source}-${e.target}`));
  return [...existing, ...incoming.filter(e => !existingKeys.has(`${e.source}-${e.target}`))];
};
```

---

## 测试计划

### 关键测试用例

| # | 测试场景 | 预期结果 |
|---|---------|---------|
| 1 | 输入档号搜索 | 正确加载初始图谱（1度关系） |
| 2 | 点击节点展开 | 新节点正确添加，连线正确 |
| 3 | 展开到3度 | 自动折叠最早的1度节点 |
| 4 | 点击已展开节点 | 正确折叠，子孙节点移除 |
| 5 | API 失败 | 显示错误提示，不影响其他节点 |
| 6 | 缓存命中 | 秒开，无 API 调用 |
| 7 | 后台刷新 | 数据更新，节点状态同步 |
| 8 | 缩放拖拽 | 画布响应正常 |
| 9 | 搜索空结果 | 显示空状态提示 |
| 10 | 无权限 | 提示正确，不崩溃 |

### 测试层级

| 层级 | 工具 | 覆盖内容 |
|------|------|---------|
| 单元测试 | Vitest | Store 状态逻辑、深度计算、缓存管理 |
| 组件测试 | Vitest + Testing Library | 节点渲染、点击事件 |
| 集成测试 | Vitest + MSW | API 调用、数据合并 |
| E2E 测试 | Playwright | 完整用户流程 |

---

## 实施步骤

### Phase 1: 基础设施

- [ ] 创建 `src/store/relationGraphStore.ts`
- [ ] 定义类型 `src/types/relationGraph.ts`
- [ ] 设置 React Flow 基础画布

### Phase 2: 节点与连线

- [ ] 实现 `RelationNodeComponent`
- [ ] 实现 `RelationEdgeComponent`
- [ ] 应用类型配色样式

### Phase 3: 交互逻辑

- [ ] 实现展开/折叠逻辑
- [ ] 实现深度控制（3度自动折叠）
- [ ] 实现缓存机制
- [ ] 实现 fitView 聚焦

### Phase 4: 集成与优化

- [ ] 集成到 `RelationshipQueryView`
- [ ] 错误处理与边界情况
- [ ] 性能优化
- [ ] 编写测试

---

## 后端 API

现有 API 已满足需求，无需新增：

```
GET /relations/{archiveId}/graph  -- 获取档案关系图谱
```

响应结构：
```json
{
  "centerId": "string",
  "nodes": [
    {
      "id": "string",
      "code": "string",
      "name": "string",
      "type": "contract|invoice|voucher|receipt|report|ledger",
      "amount": "string",
      "date": "string",
      "status": "string"
    }
  ],
  "edges": [
    {
      "from": "string",
      "to": "string",
      "relationType": "string",
      "description": "string"
    }
  ]
}
```

---

## 参考资料

- `src/pages/utilization/RelationshipQueryView.tsx` - 现有关系联查页面
- `src/components/RelationshipVisualizer.tsx` - 现有关系可视化组件
- `nexusarchive-java/.../RelationController.java` - 后端 API
- React Flow 文档: https://reactflow.dev/
