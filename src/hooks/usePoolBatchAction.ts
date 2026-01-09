// Input: Pool API, React state management
// Output: Batch action hook for pool kanban
// Pos: 批量操作 Hook

import { useCallback, useState } from 'react';
import { poolApi } from '@/api/pool';

/**
 * 支持的批量操作类型
 */
export type BatchActionType =
  | 'recheck'      // 重新检测
  | 'delete'       // 删除
  | 'edit'         // 编辑元数据
  | 'match'        // 智能匹配
  | 'archive'      // 提交归档
  | 'cancel';      // 取消归档

/**
 * 批量操作结果
 */
export interface BatchActionResult {
  success: boolean;
  message: string;
  successCount: number;
  failCount: number;
  errors?: Array<{ id: string; error: string }>;
}

/**
 * Hook 返回值
 */
export interface UsePoolBatchActionReturn {
  // 状态
  state: {
    selection: Set<string>;        // 选中的卡片 ID
    isExecuting: boolean;           // 操作执行中
    result: BatchActionResult | null; // 操作结果
  };
  // 选择方法
  selectAll: (cardIds: string[]) => void;
  toggleSelection: (cardId: string) => void;
  clearSelection: () => void;
  isSelected: (cardId: string) => boolean;
  getSelectedCount: () => number;
  getSelectedIds: () => string[];
  // 批量操作
  executeAction: (
    action: BatchActionType,
    selectedIds?: string[]
  ) => Promise<BatchActionResult>;
  clearResult: () => void;
}

/**
 * 批量操作 Hook
 *
 * 提供卡片选择管理和批量操作功能
 */
export function usePoolBatchAction(): UsePoolBatchActionReturn {
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [isExecuting, setIsExecuting] = useState(false);
  const [result, setResult] = useState<BatchActionResult | null>(null);

  /**
   * 全选指定卡片
   */
  const selectAll = useCallback((cardIds: string[]) => {
    setSelection(new Set(cardIds));
  }, []);

  /**
   * 切换单个卡片选择状态
   */
  const toggleSelection = useCallback((cardId: string) => {
    setSelection(prev => {
      const newSelection = new Set(prev);
      if (newSelection.has(cardId)) {
        newSelection.delete(cardId);
      } else {
        newSelection.add(cardId);
      }
      return newSelection;
    });
  }, []);

  /**
   * 清空选择
   */
  const clearSelection = useCallback(() => {
    setSelection(new Set());
  }, []);

  /**
   * 检查卡片是否被选中
   */
  const isSelected = useCallback((cardId: string): boolean => {
    return selection.has(cardId);
  }, [selection]);

  /**
   * 获取选中数量
   */
  const getSelectedCount = useCallback((): number => {
    return selection.size;
  }, [selection]);

  /**
   * 获取选中的 ID 列表
   */
  const getSelectedIds = useCallback((): string[] => {
    return Array.from(selection);
  }, [selection]);

  /**
   * 清空操作结果
   */
  const clearResult = useCallback(() => {
    setResult(null);
  }, []);

  /**
   * 执行批量操作
   */
  const executeAction = useCallback(
    async (action: BatchActionType, selectedIds?: string[]): Promise<BatchActionResult> => {
      // 使用传入的 IDs 或当前选择的 IDs
      const idsToProcess = selectedIds || getSelectedIds();

      if (idsToProcess.length === 0) {
        const emptyResult: BatchActionResult = {
          success: false,
          message: '请先选择要操作的文件',
          successCount: 0,
          failCount: 0,
        };
        setResult(emptyResult);
        return emptyResult;
      }

      setIsExecuting(true);
      const errors: Array<{ id: string; error: string }> = [];
      let successCount = 0;
      let failCount = 0;

      try {
        switch (action) {
          case 'delete': {
            // 批量删除
            for (const id of idsToProcess) {
              try {
                await poolApi.delete(id);
                successCount++;
              } catch (error) {
                failCount++;
                errors.push({
                  id,
                  error: error instanceof Error ? error.message : '删除失败',
                });
              }
            }
            break;
          }

          case 'archive': {
            // 批量归档
            try {
              await poolApi.archiveItems(idsToProcess);
              successCount = idsToProcess.length;
            } catch (error) {
              failCount = idsToProcess.length;
              errors.push({
                id: 'batch',
                error: error instanceof Error ? error.message : '归档失败',
              });
            }
            break;
          }

          case 'recheck':
          case 'edit':
          case 'match':
          case 'cancel': {
            // 这些操作暂时返回成功，等待后续 API 实现
            successCount = idsToProcess.length;
            break;
          }

          default:
            throw new Error(`未知的操作类型: ${action}`);
        }

        const actionResult: BatchActionResult = {
          success: failCount === 0,
          message: formatResultMessage(action, successCount, failCount),
          successCount,
          failCount,
          errors: errors.length > 0 ? errors : undefined,
        };

        setResult(actionResult);
        return actionResult;
      } catch (error) {
        const errorResult: BatchActionResult = {
          success: false,
          message: error instanceof Error ? error.message : '操作失败',
          successCount,
          failCount: idsToProcess.length - successCount,
          errors: [{ id: 'unknown', error: '未知错误' }],
        };
        setResult(errorResult);
        return errorResult;
      } finally {
        setIsExecuting(false);
      }
    },
    [selection, getSelectedIds]
  );

  return {
    state: { selection, isExecuting, result },
    selectAll,
    toggleSelection,
    clearSelection,
    isSelected,
    getSelectedCount,
    getSelectedIds,
    executeAction,
    clearResult,
  };
}

/**
 * 格式化操作结果消息
 */
function formatResultMessage(
  action: BatchActionType,
  successCount: number,
  failCount: number
): string {
  const actionLabels: Record<BatchActionType, string> = {
    recheck: '重新检测',
    delete: '删除',
    edit: '编辑',
    match: '匹配',
    archive: '归档',
    cancel: '取消',
  };

  if (failCount === 0) {
    return `${actionLabels[action]}成功 ${successCount} 条`;
  }

  if (successCount === 0) {
    return `${actionLabels[action]}失败 ${failCount} 条`;
  }

  return `${actionLabels[action]}完成：成功 ${successCount} 条，失败 ${failCount} 条`;
}
