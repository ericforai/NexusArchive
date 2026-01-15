一旦我所属的文件夹有所变化，请更新我。

// Input: React、SVG、Zustand
// Output: 关系图谱组件说明
// Pos: src/components/relation-graph/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 关系图谱组件 (Relation Graph)

本目录包含**穿透联查**功能的可视化组件，用于展示档案之间的关联关系。

## 文件清单

| 文件 | 角色 | 功能 |
| --- | --- | --- |
| `ThreeColumnLayout.tsx` | 三栏布局组件 | **默认视图**：左侧上游、中心核心、右侧下游 |
| `ArchiveCard.tsx` | 档案卡片组件 | 普通卡片 + 中心卡片样式，支持高亮 |
| `SimpleGraphView.tsx` | 图谱视图组件 | 纯 CSS + SVG 实现，支持缩放拖拽（保留作为备用） |
| `RelationNode.tsx` | React Flow 节点 | React Flow 自定义节点组件（已废弃） |
| `RelationEdge.tsx` | React Flow 连线 | React Flow 自定义连线组件（已废弃） |
| `RelationGraphCanvas.tsx` | React Flow 画布 | React Flow 画布容器（已废弃） |
| `index.ts` | 导出模块 | 组件统一导出 |

## 设计决策

### 采用三栏布局（ThreeColumnLayout）作为默认视图
- **原因**: 用户反馈图谱式布局连线不清楚，三栏布局更清晰直观
- **方案**: 三栏布局（左侧上游、中心核心、右侧下游）
- **优势**: 
  - 信息层级清晰（上游→中心→下游）
  - 业务语义直观（左侧=来源，右侧=流向）
  - 阅读体验友好（符合从左到右的阅读习惯）
  - 界面简洁，聚焦核心信息（卡片展示 + 关系标签）

### 保留图谱视图（SimpleGraphView）作为备用
- **实现**: 纯 CSS + SVG 实现
- **用途**: 需要查看完整关系网络时使用（可通过视图切换功能选择）

## 功能特性

| 功能 | 说明 |
| --- | --- |
| **三栏布局** | 左侧上游数据、中心核心单据、右侧下游数据 |
| **矩形卡片** | 统一使用矩形卡片展示，中心卡片特殊样式（金色边框） |
| **关系标签** | 卡片上显示关系类型标签（依据、原始凭证、资金流、归档） |
| **点击查看** | 点击任何卡片打开详情抽屉查看详细信息 |
| **关系分类** | 自动根据关系方向分类上游/下游（指向中心=上游，从中心出发=下游） |

## 状态管理

使用 `useRelationGraphStore` (`src/store/useRelationGraphStore.ts`) 管理图谱状态。

## API 依赖

- `GET /api/relations/{archiveId}/graph` - 获取档案关系图谱
- 数据来源: `archive_relation` 表
