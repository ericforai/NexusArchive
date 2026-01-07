// Input: 批量操作组件导出
// Output: 批量操作模块公共 API
// Pos: src/components/operations/index.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 批量操作组件模块
 *
 * 提供通用的批量选择、审批和结果展示组件
 */

// Hooks
export { useBatchSelection, MAX_SELECTION_LIMIT } from './useBatchSelection';

// Components
export { BatchOperationBar } from './BatchOperationBar';
export { BatchApprovalDialog } from './BatchApprovalDialog';
export { BatchResultModal } from './BatchResultModal';

// Types
export type {
  BatchSelectionState,
  SelectionResult,
  RowSelectionConfig,
  UseBatchSelectionReturn
} from './useBatchSelection';
export type { BatchOperationBarProps } from './BatchOperationBar';
export type { BatchApprovalDialogProps, ApprovalRecord } from './BatchApprovalDialog';
export type { BatchResultModalProps, BatchError } from './BatchResultModal';

/**
 * 使用示例：
 *
 * ```typescript
 * import {
 *   useBatchSelection,
 *   BatchOperationBar,
 *   BatchApprovalDialog,
 *   BatchResultModal
 * } from '@components/operations';
 * ```
 */
