# 批量操作 API 实现任务清单

- [x] 在 `poolApi` 中添加批量检测接口 (`checkBatch`, `checkAllPending`)
- [x] 在 `ReportsPreArchiveView.tsx` 中实现批量归档和重新检测功能
- [x] 在 `PoolPage.tsx` (记账凭证库) 中实现相同功能以保持一致性
- [x] 在 `LedgersPreArchiveView.tsx` (会计账簿库) 中实现相同功能
- [x] 在 `OtherAccountingMaterialsView.tsx` (其他会计资料库) 中实现相同功能
- [x] 验证后端接口匹配情况 (`PoolController`)
- [x] 修复前端 lint 错误 (导入缺失与类型定义)
- [x] 修复架构警告 (`hooks-only-in-features-or-pages`)
  - [x] 重构 `PoolDashboard` 为纯展示组件
  - [x] 迁移 `DashboardStats` 接口至配置层
  - [x] 在所有 4 个预处理视图中透传统计数据
- [ ] 用户验收测试
