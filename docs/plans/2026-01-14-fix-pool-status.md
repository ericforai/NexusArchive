# 预归档状态定义修复实施计划

## 问题背景
用户反馈预归档池页面数据加载为空。经排查，虽然 API 429 错误已修复，但数据库中的状态值 (`READY_TO_ARCHIVE`, `COMPLETED`) 与前端请求的状态值 (`PENDING_ARCHIVE`, `ARCHIVED`) 不一致，导致后端查询不到数据。

## 涉及文件
- [MODIFY] `src/features/archives/controllers/types.ts`
- [MODIFY] `src/features/archives/controllers/utils.ts`

## 修改方案

### 1. 更新类型定义
`src/features/archives/controllers/types.ts`

```typescript
// 更新为与数据库一致的状态
export type PoolStatusFilter = 
    | 'all' 
    | 'PENDING_CHECK' 
    | 'NEEDS_ACTION'      // 替代 PENDING_METADATA / CHECK_FAILED
    | 'READY_TO_MATCH'    // 新增，对应后端逻辑
    | 'READY_TO_ARCHIVE'  // 替代 PENDING_ARCHIVE
    | 'COMPLETED'         // 替代 ARCHIVED
    | 'PENDING_APPROVAL'  // 保留
    | null;
```

### 2. 更新状态映射
`src/features/archives/controllers/utils.ts`

```typescript
const PRE_ARCHIVE_STATUS_LABELS: Record<string, { label: string }> = {
    PENDING_CHECK: { label: '待检测' },
    NEEDS_ACTION: { label: '待处理' },      // 对应 CHECK_FAILED / PENDING_METADATA
    READY_TO_MATCH: { label: '待匹配' },
    READY_TO_ARCHIVE: { label: '准备归档' }, // 对应 PENDING_ARCHIVE
    COMPLETED: { label: '已归档' },          // 对应 ARCHIVED
    PENDING_APPROVAL: { label: '归档审批中' },
};
```

## 验证计划

### 1. 手动验证
- **步骤**:
    1. 刷新 `http://localhost:15175/system/pre-archive/pool`。
    2. 检查列表是否显示数据。
    3. 检查顶部看板统计数字是否显示。
