// Input: React、SVG、Zustand
// Output: 关系图谱组件说明
// Pos: src/components/relation-graph/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 关系图谱组件 (Relation Graph)

本目录包含**穿透联查**功能的可视化组件，用于展示档案之间的关联关系。

## 文件清单

| 文件 | 角色 | 功能 |
| --- | --- | --- |
| `SimpleGraphView.tsx` | 主视图组件 | 纯 CSS + SVG 实现，支持缩放拖拽，节点点击展开 |
| `RelationNode.tsx` | React Flow 节点 | React Flow 自定义节点组件（已废弃） |
| `RelationEdge.tsx` | React Flow 连线 | React Flow 自定义连线组件（已废弃） |
| `RelationGraphCanvas.tsx` | React Flow 画布 | React Flow 画布容器（已废弃） |
| `index.ts` | 导出模块 | 组件统一导出 |

## 设计决策

### 采用简单实现（SimpleGraphView）
- **原因**: React Flow (`@xyflow/react`) 在当前环境中存在渲染问题（连线不显示、小地图空白）
- **方案**: 纯 CSS + SVG 实现
- **优势**: 完全可控、轻量、易于调试

## 功能特性

| 功能 | 说明 |
| --- | --- |
| **图谱展示** | 上游节点（左）→ 中心节点（中）→ 下游节点（右） |
| **双向连线** | SVG 带双向箭头，显示关系类型标签 |
| **渐进展开** | 点击节点展开其关联关系（最多3度） |
| **自动折叠** | 超过3度时自动折叠最早的1度节点 |
| **缩放拖拽** | 右上角按钮 + 鼠标滚轮缩放，拖拽平移 |
| **详情抽屉** | 点击节点显示详细信息 |

## 状态管理

使用 `useRelationGraphStore` (`src/store/useRelationGraphStore.ts`) 管理图谱状态。

## API 依赖

- `GET /api/relations/{archiveId}/graph` - 获取档案关系图谱
- 数据来源: `archive_relation` 表
