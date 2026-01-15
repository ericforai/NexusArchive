一旦我所属的文件夹有所变化，请更新我。

// Input: 业务组件
// Output: 档案业务操作视图
// Pos: src/pages/operations/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 档案业务操作 (Operations)

本目录包含档案全生命周期的核心业务操作视图，包括归档、鉴定、销毁等。

## 文件清单

| 文件 | 类型 | 功能 |
| --- | --- | --- |
| `ArchiveApprovalView.tsx` | React组件 | 归档审批视图，处理归档申请的审核流程。支持批量审批。 |
| `ArchiveBatchView.tsx` | React组件 | 归档批次管理，负责批次的创建、校验、提交和归档执行。支持批量审批。 |
| `DestructionApprovalPage.tsx` | React组件 | 档案销毁审批，处理销毁申请的审核。支持批量审批。 |
| `DestructionExecutionPage.tsx` | React组件 | 档案销毁执行，处理经批准档案的物理/逻辑销毁确认。 |
| `DestructionView.tsx` | React组件 | 档案销毁管理主页，查看销毁状态与记录。 |
| `ExpiredArchivesPage.tsx` | React组件 | 到期档案列表，展示已超过保管期限的档案。 |
| `FreezeHoldPage.tsx` | React组件 | 档案冻结管理，处理因诉讼等原因需要暂缓销毁的档案。 |
| `OpenAppraisalView.tsx` | React组件 | 开放鉴定视图，处理档案的开放范围鉴定。 |
| `VolumeManagement.tsx` | React组件 | 案卷管理，负责案卷的组卷与维护。 |

## 批量操作功能

以下视图已集成批量操作能力（详见 `src/components/operations/`）：

- **ArchiveApprovalView**: 批量批准/拒绝归档申请
- **ArchiveBatchView**: 批量批准/拒绝归档批次
- **DestructionApprovalPage**: 批量批准/拒绝销毁申请

### 批量操作限制

| 限制项 | 值 |
|--------|-----|
| 单次最多选择 | 100 条 |
| 确认阈值 | 10 条（超过时显示清单确认） |
| 结果展示 | ≤50 条立即弹窗详情；>50 条转后台任务 |
