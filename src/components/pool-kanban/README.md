# pool-kanban - 预归档池看板组件

一旦我所属的文件夹有所变化，请更新我。

## 目录功能

电子凭证池看板视图组件，提供四列看板展示档案预处理流程，支持子状态标签页过滤、批量操作、响应式布局和空列折叠功能。
子状态标签页使用 Tabs items 模式，加载态 Spin 采用嵌套 tip 写法。

## 文件清单

| 文件 | 类型 | 功能描述 |
|------|------|----------|
| `PoolKanbanView.tsx` | React 组件 | 看板主视图组件，集成所有子组件和逻辑 |
| `PoolKanbanView.css` | 样式 | 看板视图样式 |
| `KanbanCard.tsx` | React 组件 | 卡片组件，显示单个凭证信息 |
| `KanbanCard.css` | 样式 | 卡片样式 |
| `KanbanColumn.tsx` | React 组件 | 列组件，包含子状态标签页和操作按钮 |
| `KanbanColumn.css` | 样式 | 列样式 |
| `CollapsedColumn.tsx` | React 组件 | 折叠列组件，空列折叠时显示 |
| `CollapsedColumn.css` | 样式 | 折叠列样式 |
| `BatchActionBar.tsx` | React 组件 | 批量操作栏，显示选中数量和操作按钮 |
| `BatchActionBar.css` | 样式 | 批量操作栏样式 |
| `index.ts` | 导出模块 | 组件统一导出 |
| `__tests__/PoolKanbanView.test.tsx` | 单元测试 | 看板视图测试 |
| `__tests__/KanbanCard.test.tsx` | 单元测试 | 卡片组件测试 |
| `__tests__/KanbanColumn.test.tsx` | 单元测试 | 列组件测试 |
| `__tests__/CollapsedColumn.test.tsx` | 单元测试 | 折叠列组件测试 |
| `__tests__/BatchActionBar.test.tsx` | 单元测试 | 批量操作栏测试 |
