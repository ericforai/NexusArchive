// Input: 自定义 hooks
// Output: 批次 hooks 模块导出
// Pos: src/pages/operations/archive-batch/hooks/index.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export { useArchiveBatchData } from './useArchiveBatchData';
export type { UseArchiveBatchDataReturn } from './useArchiveBatchData';

export { useBatchModals } from './useBatchModals';
export type { UseBatchModalsReturn, ApprovalAction } from './useBatchModals';

export { useBatchOperations } from './useBatchOperations';
export type { UseBatchOperationsReturn } from './useBatchOperations';
