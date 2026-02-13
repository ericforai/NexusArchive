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
| `RelationshipQueryView.tsx` | 穿透联查页面 | 输入档号查询关联关系图谱（2026-02-13 更新） |
| `relationDrilldown.ts` | 联查工具函数 | 主线识别、缺口检测、打印范围聚合（2026-02-13 新增） |
| `BorrowingView.tsx` | 借阅管理页面 | 档案借阅申请与审批 |
| `WarehouseView.tsx` | 库房视图 | 档案库房位置可视化 |
| `manifest.config.ts` | 模块清单 | 模块导出配置 |

## 2026-02-13 更新

### 穿透联查 (RelationshipQueryView)
- **功能**: 输入任意档号，展示完整业务链路图谱；支持空态一键加载演示数据、主线识别与批量打印
- **组件**:
  - `SearchBar`: 搜索栏组件
  - `ThreeColumnLayout`: 三栏关系图布局（上游/中心/下游）
  - `ArchiveDetailDrawer`: 节点详情抽屉（默认附件页）
  - `EmptyState`: 空状态组件
- **交互**:
  - 空态下可直接点击“加载示例数据”，自动填充并查询默认演示档号
  - 点击任意节点打开详情抽屉并预览关联附件
  - 优先使用后端方向解析（`directionalView`）展示上下游；仅在缺失时降级到本地主线识别
  - 批量打印范围按“主线 + 手动展开节点”聚合并去重
- **状态管理**: 使用 `useRelationGraphStore`
- **API**: `/api/relations/{archiveId}/graph`
