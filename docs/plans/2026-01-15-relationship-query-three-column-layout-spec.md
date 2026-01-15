# 穿透联查功能 - 三栏布局设计 Spec 提案

**日期**: 2026-01-15  
**状态**: 📋 Spec 提案  
**优先级**: P1  
**关联文档**: 
- `docs/plans/2026-01-13-relation-query-deep-dive-design.md` (渐进式展开设计)
- `docs/plans/2026-01-15-relationship-query-demo-data-plan.md` (Demo 数据计划)
- `docs/plans/2026-01-15-relationship-query-voucher-center-opec.md` (以记账凭证为中心 OPEC 提案) ⚠️ **重要修复**

---

## 📋 需求概述

基于用户反馈，恢复并改进穿透联查功能的三栏布局设计：
- **中心固定**: 以查询的档案为中心节点
- **左侧展示**: 上游数据（依据、原始凭证等）
- **右侧展示**: 下游数据（资金流、归档等）
- **矩形卡片**: 统一使用矩形卡片展示，信息清晰
- **关系说明**: 底部以列表形式展示详细关系说明

### 核心优势

相比图谱式布局，三栏布局具有以下优势：
1. ✅ **信息层级清晰**: 明确区分上游/下游关系
2. ✅ **业务语义直观**: 左侧=来源/依据，右侧=流向/归档
3. ✅ **阅读体验友好**: 符合从左到右的阅读习惯
4. ✅ **关系说明详细**: 底部列表可展示更多关系细节

---

## 🎨 UI/UX 设计

### 布局结构

```
┌─────────────────────────────────────────────────────────────┐
│  搜索栏: [输入档号] [查询] [重置]                            │
├─────────────┬───────────────────┬───────────────────────────┤
│             │                   │                           │
│  上游数据   │   核心单据        │   下游数据               │
│  (左侧栏)   │   (中心栏)        │   (右侧栏)               │
│             │                   │                           │
│ ┌────────┐  │  ┌─────────────┐ │  ┌────────┐              │
│ │合同    │  │  │ [核心标记]   │ │  │凭证    │              │
│ │CON-xxx │  │  │ CON-2023-098│ │  │JZ-xxx  │              │
│ │¥150,000│  │  │ 年度技术服务│ │  │¥58,000 │              │
│ │        │  │  │ 协议        │ │  │        │              │
│ └────────┘  │  │ ¥150,000.00 │ │  └────────┘              │
│             │  │ 2023-01-15  │ │  ┌────────┐              │
│ ┌────────┐  │  │ [生效中]    │ │  │回单    │              │
│ │发票1   │  │  └─────────────┘ │  │HD-xxx  │              │
│ │FP-xxx  │  │                  │  └────────┘              │
│ └────────┘  │                  │                           │
│             │                  │                           │
│ "暂无上游"  │                  │ "暂无下游"                │
│             │                  │                           │
├─────────────┴───────────────────┴───────────────────────────┤
│  关系说明:                                                    │
│  • CON-2023-098 → JZ-202311-0052  [依据]                    │
│  • FP-2023-001 → JZ-202311-0052  [原始凭证]                 │
│  • JZ-202311-0052 → HD-2023-001  [资金流]                   │
│  • JZ-202311-0052 → REP-2023-11  [归档]                     │
└─────────────────────────────────────────────────────────────┘
```

### 组件结构

```
RelationshipQueryView (主容器)
├── SearchHeader (搜索栏)
│   ├── SearchInput
│   ├── SearchButton
│   └── ResetButton
├── ThreeColumnLayout (三栏布局容器)
│   ├── UpstreamColumn (左侧栏 - 上游数据)
│   │   ├── ColumnHeader ("上游数据")
│   │   ├── ArchiveCard[] (档案卡片列表)
│   │   └── EmptyState ("暂无上游数据")
│   ├── CenterColumn (中心栏 - 核心单据)
│   │   ├── CenterCard (核心档案卡片，特殊样式)
│   │   └── RelationDescription (关系说明列表)
│   └── DownstreamColumn (右侧栏 - 下游数据)
│       ├── ColumnHeader ("下游数据")
│       ├── ArchiveCard[] (档案卡片列表)
│       └── EmptyState ("暂无下游数据")
└── DetailDrawer (右侧详情抽屉，可选)
    └── ArchiveDetailPanel
```

---

## 📐 详细设计

### 1. 三栏布局规格

| 栏位 | 宽度 | 对齐方式 | 内容 |
|------|------|---------|------|
| 左侧栏（上游） | 300px | 左对齐 | 来源单据列表 |
| 中心栏 | flex: 1 | 居中 | 核心单据 + 关系说明 |
| 右侧栏（下游） | 300px | 右对齐 | 流向单据列表 |

### 2. 档案卡片设计

#### 普通卡片（上游/下游）

```
┌──────────────────────────────┐
│ [类型标签]              [状态]│
│ ┌────┐                       │
│ │图标│  FP-2025-01-001       │
│ └────┘  高铁票发票-北京南至...│
│          ¥ 553.00            │
│          2025-01-06          │
│          [原始凭证] →        │
└──────────────────────────────┘
```

**尺寸**: 280px × 140px  
**间距**: 16px (卡片之间)

#### 中心卡片（核心单据）

```
┌──────────────────────────────┐
│ [核心单据]                    │
│ ┌────┐                       │
│ │图标│  CON-2023-098          │
│ └────┘  年度技术服务协议      │
│          ¥ 150,000.00        │
│          2023-01-15          │
│          [生效中]            │
│ 关系: 核心单据               │
└──────────────────────────────┘
```

**尺寸**: 360px × 180px  
**特殊样式**:
- 金色/橙色边框 (`#f59e0b`)
- 背景色 (`bg-amber-50`)
- 阴影增强 (`shadow-xl`)
- 顶部"核心单据"标签

### 3. 关系说明列表

位于中心栏底部，展示所有关系的文本列表：

```
关系说明:
• CON-2023-098 → JZ-202311-0052  [依据]
• FP-2023-001 → JZ-202311-0052  [原始凭证]
• FP-2023-002 → JZ-202311-0052  [原始凭证]
• JZ-202311-0052 → HD-2023-001  [资金流]
• JZ-202311-0052 → REP-2023-11  [归档]
```

**样式**:
- 列表项可点击，点击后高亮对应卡片
- 关系类型用彩色标签区分（与连线颜色一致）
- 支持滚动（最多显示 20 条）

---

## 🔧 技术实现

### 数据结构

```typescript
interface RelationGraphData {
  centerNode: RelationNode;        // 中心节点
  upstreamNodes: RelationNode[];   // 上游节点（左侧）
  downstreamNodes: RelationNode[]; // 下游节点（右侧）
  relations: RelationEdge[];       // 所有关系
}

interface RelationNode {
  id: string;
  code: string;
  name: string;
  type: ArchiveType;
  amount?: string;
  date?: string;
  status?: string;
}

interface RelationEdge {
  id: string;
  from: string;
  to: string;
  relationType: RelationType;
  description?: string;
}
```

### 关系分类逻辑

```typescript
// 根据关系类型和方向判断上游/下游
function classifyRelations(
  centerId: string,
  relations: RelationEdge[]
): { upstream: RelationNode[], downstream: RelationNode[] } {
  const upstream: RelationNode[] = [];
  const downstream: RelationNode[] = [];
  
  relations.forEach(rel => {
    if (rel.to === centerId) {
      // 指向中心节点 → 上游（来源/依据）
      upstream.push(getNodeById(rel.from));
    } else if (rel.from === centerId) {
      // 从中心节点出发 → 下游（流向/归档）
      downstream.push(getNodeById(rel.to));
    }
  });
  
  return { upstream, downstream };
}
```

### 组件实现

#### ArchiveCard 组件

```typescript
interface ArchiveCardProps {
  node: RelationNode;
  relationType?: RelationType;  // 用于显示关系标签
  onClick?: () => void;
  isCenter?: boolean;
}

export const ArchiveCard: React.FC<ArchiveCardProps> = ({
  node,
  relationType,
  onClick,
  isCenter = false
}) => {
  const meta = ARCHIVE_TYPE_STYLES[node.type];
  const relationLabel = relationType ? RELATION_TYPE_LABELS[relationType] : null;
  
  return (
    <div
      className={cn(
        "rounded-xl border-2 shadow-md cursor-pointer transition-all hover:shadow-lg hover:scale-105",
        isCenter 
          ? "bg-amber-50 border-amber-400 w-[360px] h-[180px]" 
          : "bg-white border-slate-200 w-[280px] h-[140px]"
      )}
      onClick={onClick}
    >
      {/* 卡片内容 */}
    </div>
  );
};
```

#### ThreeColumnLayout 组件

```typescript
export const ThreeColumnLayout: React.FC<{
  centerNode: RelationNode;
  upstreamNodes: RelationNode[];
  downstreamNodes: RelationNode[];
  relations: RelationEdge[];
  onNodeClick?: (nodeId: string) => void;
}> = ({ centerNode, upstreamNodes, downstreamNodes, relations, onNodeClick }) => {
  return (
    <div className="flex h-full gap-4 px-6 py-4">
      {/* 左侧栏 - 上游 */}
      <div className="w-[300px] flex flex-col">
        <h3 className="text-lg font-semibold mb-4 text-slate-700">上游数据</h3>
        <div className="flex-1 overflow-y-auto space-y-4">
          {upstreamNodes.length > 0 ? (
            upstreamNodes.map(node => (
              <ArchiveCard
                key={node.id}
                node={node}
                relationType={getRelationType(node.id, centerNode.id, relations)}
                onClick={() => onNodeClick?.(node.id)}
              />
            ))
          ) : (
            <EmptyColumnState message="暂无上游数据" />
          )}
        </div>
      </div>

      {/* 中心栏 */}
      <div className="flex-1 flex flex-col items-center">
        <ArchiveCard
          node={centerNode}
          isCenter
          onClick={() => onNodeClick?.(centerNode.id)}
        />
        
        {/* 关系说明列表 */}
        <RelationDescriptionList
          centerId={centerNode.id}
          relations={relations}
          nodes={[...upstreamNodes, ...downstreamNodes, centerNode]}
          onRelationClick={(nodeId) => onNodeClick?.(nodeId)}
        />
      </div>

      {/* 右侧栏 - 下游 */}
      <div className="w-[300px] flex flex-col">
        <h3 className="text-lg font-semibold mb-4 text-slate-700">下游数据</h3>
        <div className="flex-1 overflow-y-auto space-y-4">
          {downstreamNodes.length > 0 ? (
            downstreamNodes.map(node => (
              <ArchiveCard
                key={node.id}
                node={node}
                relationType={getRelationType(centerNode.id, node.id, relations)}
                onClick={() => onNodeClick?.(node.id)}
              />
            ))
          ) : (
            <EmptyColumnState message="暂无下游数据" />
          )}
        </div>
      </div>
    </div>
  );
};
```

---

## 🔄 交互逻辑

### 1. 初始化流程

```
用户输入档号 → 点击查询
    ↓
调用 API: GET /relations/{archiveId}/graph
    ↓
获取图谱数据（nodes + edges）
    ↓
识别中心节点（centerId）
    ↓
分类关系：
  - 指向中心的 → 上游（左侧）
  - 从中心出发的 → 下游（右侧）
    ↓
渲染三栏布局
```

### 2. 点击卡片交互

- **点击普通卡片（上游/下游）**: 
  - 将该卡片设为中心节点
  - 重新查询关系数据
  - 重新分类并刷新布局
  
- **点击中心卡片**:
  - 在右侧详情抽屉显示完整信息
  - 高亮关系说明列表中的相关项

### 3. 关系说明列表交互

- **点击关系项**:
  - 高亮对应的卡片（上游或下游）
  - 显示连接线动画（可选）

---

## 📊 关系类型与方向语义

| 关系类型 | 典型方向 | 语义 | 位置 |
|---------|---------|------|------|
| BASIS (依据) | 其他 → 凭证 | 合同、申请单作为凭证依据 | 左侧（上游） |
| ORIGINAL_VOUCHER (原始凭证) | 发票 → 凭证 | 发票作为凭证原始凭证 | 左侧（上游） |
| CASH_FLOW (资金流) | 凭证 → 回单 | 凭证产生资金流转 | 右侧（下游） |
| ARCHIVE (归档) | 凭证 → 报表 | 凭证归档到报表/账簿 | 右侧（下游） |

**注意**: 关系是双向的，但展示时根据 `from/to` 与中心节点的关系决定位置。

---

## 🎯 实施计划

### Phase 1: 组件开发 (2-3 天)

- [ ] 创建 `ArchiveCard` 组件（普通 + 中心样式）
- [ ] 创建 `ThreeColumnLayout` 组件
- [ ] 创建 `RelationDescriptionList` 组件
- [ ] 创建 `EmptyColumnState` 组件

### Phase 2: 数据分类逻辑 (1 天)

- [ ] 实现关系分类函数 `classifyRelations()`
- [ ] 实现关系类型获取函数 `getRelationType()`
- [ ] 更新 `useRelationGraphStore` 支持三栏布局数据

### Phase 3: 集成与交互 (1-2 天)

- [ ] 修改 `RelationshipQueryView` 使用三栏布局
- [ ] 实现点击卡片切换中心节点
- [ ] 实现关系说明列表交互
- [ ] 保留右侧详情抽屉功能

### Phase 4: 样式优化 (1 天)

- [ ] 卡片样式细化
- [ ] 响应式适配（小屏幕时改为垂直堆叠）
- [ ] 动画效果（卡片高亮、关系连线）

### Phase 5: 测试与优化 (1 天)

- [ ] 功能测试（各种关系类型）
- [ ] 性能测试（大量节点）
- [ ] 用户体验测试

---

## 🧪 测试用例

### 功能测试

| # | 测试场景 | 预期结果 |
|---|---------|---------|
| 1 | 输入档号查询 | 正确显示三栏布局，中心节点居中 |
| 2 | 有上游数据 | 左侧栏显示上游卡片列表 |
| 3 | 有下游数据 | 右侧栏显示下游卡片列表 |
| 4 | 无上游数据 | 左侧显示"暂无上游数据" |
| 5 | 无下游数据 | 右侧显示"暂无下游数据" |
| 6 | 关系说明列表 | 底部正确显示所有关系 |
| 7 | 点击上游卡片 | 该卡片成为新的中心节点 |
| 8 | 点击下游卡片 | 该卡片成为新的中心节点 |
| 9 | 点击关系说明项 | 高亮对应卡片 |

### 边界情况

- [ ] 中心节点无关联关系（空状态）
- [ ] 关系类型混合（既有上游又有下游）
- [ ] 大量节点（左右栏滚动）
- [ ] 节点名称过长（文本截断）

---

## 📝 API 兼容性

现有 API 无需修改，直接使用：

```
GET /api/relations/{archiveId}/graph
```

响应结构完全兼容，只需在前端进行分类处理。

---

## 🔄 与现有实现的关系

### 保留现有功能

- ✅ 保持 `SimpleGraphView` 作为可选视图（可通过切换按钮选择）
- ✅ 保留右侧详情抽屉功能
- ✅ 保留搜索和重置功能

### 新增功能

- ✅ 三栏布局作为默认视图
- ✅ 关系说明列表
- ✅ 卡片点击切换中心节点

### 视图切换（可选）

可在页面右上角添加视图切换按钮：
- 📊 **图谱视图**: 当前 `SimpleGraphView`（保留）
- 📋 **三栏视图**: 新的三栏布局（默认）

---

## ✅ 验收标准

### 功能完整性

- [ ] 三栏布局正确显示
- [ ] 上游/下游数据正确分类
- [ ] 关系说明列表完整展示
- [ ] 卡片点击交互正常
- [ ] 关系说明项点击高亮正常

### 视觉效果

- [ ] 中心卡片样式突出
- [ ] 卡片间距合理
- [ ] 颜色搭配协调
- [ ] 响应式适配良好

### 用户体验

- [ ] 信息层级清晰
- [ ] 交互流畅
- [ ] 加载状态明确
- [ ] 错误提示友好

---

## 📚 参考资料

- `docs/plans/2026-01-13-relation-query-deep-dive-design.md` - 渐进式展开设计
- `src/pages/utilization/RelationshipQueryView.tsx` - 当前实现
- `src/components/relation-graph/SimpleGraphView.tsx` - 图谱视图实现
- `src/store/useRelationGraphStore.ts` - 状态管理
- `src/types/relationGraph.ts` - 类型定义

---

## 🎨 设计稿参考

基于用户反馈的图片描述：
- 三栏布局，中心卡片突出
- 矩形卡片展示，信息清晰
- 关系说明在底部列表展示
- 右侧详情抽屉（可选）

---

**下一步**: 等待评审确认后开始实施。
