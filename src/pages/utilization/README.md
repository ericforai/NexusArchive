一旦我所属的文件夹有所变化，请更新我。

// Input: React、Zustand
// Output: 利用分析页面说明
// Pos: src/pages/utilization/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 利用分析页面 (Utilization)

本目录包含档案利用分析相关的页面组件。

## 文件清单

| 文件 | 角色 | 功能 |
| --- | --- | --- |
| `RelationshipQueryView.tsx` | 穿透联查页面 | 输入档号查询关联关系图谱（2026-01-13 更新） |
| `BorrowingView.tsx` | 借阅管理页面 | 档案借阅申请与审批 |
| `WarehouseView.tsx` | 库房视图 | 档案库房位置可视化 |
| `manifest.config.ts` | 模块清单 | 模块导出配置 |

## 2026-01-13 更新

### 穿透联查 (RelationshipQueryView)
- **功能**: 输入任意档号，点击节点展开其关联关系（最多3度）
- **组件**:
  - `SearchBar`: 搜索栏组件
  - `SimpleGraphView`: 关系图谱画布（纯 CSS + SVG）
  - `NodeDetailDrawer`: 节点详情抽屉
  - `HelpTip`: 使用提示组件
  - `EmptyState`: 空状态组件
- **交互**:
  - 右上角按钮缩放 + 鼠标滚轮缩放
  - 拖拽空白区域移动画布
  - 点击非中心节点展开/折叠关联
- **状态管理**: 使用 `useRelationGraphStore`
- **API**: `/api/relations/{archiveId}/graph`
