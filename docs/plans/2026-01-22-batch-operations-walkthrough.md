# 批量操作 API 集成完成报告 (Walkthrough)

## 概览
本次任务成功实现了预归档库（凭证、账簿、报表及其他资料）的“批量归档”与“重新检测”后端 API 集成。填补了前端 UI 触发器与后端业务逻辑之间的空白。

## 变更详情

### 1. API 层更新
- **文件**: [pool.ts](file:///Users/user/nexusarchive/src/api/pool.ts)
- **改动**: 新增 `checkBatch` (批量检测) 和 `checkAllPending` (检测所有待检测) 方法，对接后端 `PoolController` 接口。

### 2. 架构警告修复 (Frontend Boundaries)
- **原因**: `PoolDashboard.tsx` 位于 `src/components`，根据项目架构规则（`dependency-cruiser`），不得直接引入 `src/hooks` 中的业务 Hook。
- **改动**: 
  - 将 `PoolDashboard` 重构为纯展示组件，改为通过 `props` 接收 `stats`。
  - 将 `DashboardStats` 接口移动到 [pool-columns.config.ts](file:///Users/user/nexusarchive/src/config/pool-columns.config.ts) 层。
  - 在所有 4 个引用页面（凭证、账簿、报表、其他资料）中统一调用 Hook 并向下透传数据。

### 3. 前端视图实现
对以下四个核心预归档视图进行了统一的功能实现，包括添加 `Modal` 确认框、加载状态提示及操作后的数据刷新：
- **财务报告库**: [ReportsPreArchiveView.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/ReportsPreArchiveView.tsx)
- **记账凭证库**: [PoolPage.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/PoolPage.tsx)
- **会计账簿库**: [LedgersPreArchiveView.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/LedgersPreArchiveView.tsx)
- **其他会计资料库**: [OtherAccountingMaterialsView.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/OtherAccountingMaterialsView.tsx)

## 核心逻辑
```typescript
const handleBatchArchive = useCallback(async (status: SimplifiedPreArchiveStatus) => {
    // 1. 获取对应门类和状态的 ID 列表
    const items = await poolApi.getListByStatus(status, category);
    const ids = items.map(item => item.id);
    
    // 2. 根据状态调用不同 API
    if (status === 'READY_TO_ARCHIVE') {
        await poolApi.archiveItems(ids); // 批量提交归档申请
    } else {
        await poolApi.checkBatch(ids); // 批量触发四性检测
    }
    
    // 3. 刷新 React Query 缓存
    queryClient.invalidateQueries({ queryKey: ['pool'] });
}, [...]);
```

## 验证结论
- **后端支持**: 确认后端 `PoolController` 已提供 `/pool/submit/batch` 和 `/pool/check/batch` 接口。
- **UI 交互**: 已添加 `antd` 的 `Modal.confirm` 以防误操作，并使用 `message.loading` 提供异步反馈。
- **架构合规**: 修复了 `dependency-cruiser` 报告的 `hooks-only-in-features-or-pages` 警告。
- **一致性**: 所有四个库的操作逻辑保持高度一致，门类过滤正确。
