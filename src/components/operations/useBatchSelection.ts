// Input: React Hooks, Ant Design Table rowSelection 配置
// Output: 批量选择状态管理 Hook (rowSelection + 操作方法)
// Pos: src/components/operations/useBatchSelection.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';

/**
 * 批量选择限制
 */
export const MAX_SELECTION_LIMIT = 100;

/**
 * 选择操作结果
 */
export interface SelectionResult {
  success: boolean;
  reason?: string;
}

/**
 * 批量选择状态
 */
export interface BatchSelectionState {
  selectedIds: Set<number>;
  selectAllMode: boolean;
}

/**
 * Ant Design Table rowSelection 配置
 */
export interface RowSelectionConfig {
  type: 'checkbox';
  selectedRowKeys: number[];
  onChange: (selectedRowKeys: number[]) => void;
  getCheckboxProps?: (record: any) => { disabled?: boolean };
}

/**
 * 批量选择 Hook 返回值
 */
export interface UseBatchSelectionReturn {
  // 状态
  selectedIds: Set<number>;
  selectAllMode: boolean;

  // Ant Design Table rowSelection 配置
  rowSelection: RowSelectionConfig;

  // 操作方法
  clearSelection: () => void;
  toggleSelection: (id: number) => SelectionResult;
  setSelectedIds: (ids: Set<number> | number[]) => void;
  selectAll: (allIds: number[]) => SelectionResult;
  getSelectedCount: () => number;
  isSelected: (id: number) => boolean;
}

/**
 * 批量选择 Hook
 *
 * 提供跨页选择状态管理、全选/反选控制和数量统计
 * 兼容 Ant Design Table 的 rowSelection 配置
 *
 * @returns 批量选择状态和操作方法
 *
 * @example
 * ```tsx
 * function MyTable() {
 *   const {
 *     rowSelection,
 *     selectedCount,
 *     clearSelection
 *   } = useBatchSelection();
 *
 *   return (
 *     <Table
 *       rowSelection={rowSelection}
 *       dataSource={data}
 *     />
 *   );
 * }
 * ```
 */
export function useBatchSelection(): UseBatchSelectionReturn {
  const [selectedIds, setSelectedIdsState] = useState<Set<number>>(new Set());
  const [selectAllMode, setSelectAllMode] = useState<boolean>(false);

  /**
   * 处理 Table rowSelection 的 onChange 回调
   */
  const handleSelectionChange = useCallback((selectedRowKeys: number[]) => {
    // 检查是否超出限制
    if (selectedRowKeys.length > MAX_SELECTION_LIMIT) {
      console.warn(
        `[useBatchSelection] Selection limit exceeded: ${selectedRowKeys.length} > ${MAX_SELECTION_LIMIT}`
      );
      return;
    }

    setSelectedIdsState(new Set(selectedRowKeys));

    // 如果当前是全选模式，但选中数量少于实际数量，则退出全选模式
    if (selectAllMode && selectedRowKeys.length < MAX_SELECTION_LIMIT) {
      setSelectAllMode(false);
    }
  }, [selectAllMode]);

  /**
   * 清空选择
   */
  const clearSelection = useCallback(() => {
    setSelectedIdsState(new Set());
    setSelectAllMode(false);
  }, []);

  /**
   * 切换单条选中状态
   */
  const toggleSelection = useCallback((id: number): SelectionResult => {
    setSelectedIdsState((prev) => {
      const newSet = new Set(prev);

      if (newSet.has(id)) {
        newSet.delete(id);
        // 取消选中时退出全选模式
        setSelectAllMode(false);
      } else {
        // 检查是否超出限制
        if (newSet.size >= MAX_SELECTION_LIMIT) {
          return prev; // 不更新状态
        }
        newSet.add(id);
      }

      return newSet;
    });

    return { success: true };
  }, []);

  /**
   * 设置选中的 ID 集合
   */
  const setSelectedIds = useCallback((ids: Set<number> | number[]) => {
    const newIds = ids instanceof Set ? ids : new Set(ids);

    // 检查是否超出限制
    if (newIds.size > MAX_SELECTION_LIMIT) {
      console.warn(
        `[useBatchSelection] Selection limit exceeded: ${newIds.size} > ${MAX_SELECTION_LIMIT}`
      );
      return;
    }

    setSelectedIdsState(newIds);
    setSelectAllMode(false);
  }, []);

  /**
   * 全选所有记录
   */
  const selectAll = useCallback((allIds: number[]): SelectionResult => {
    if (allIds.length > MAX_SELECTION_LIMIT) {
      return {
        success: false,
        reason: `Cannot select more than ${MAX_SELECTION_LIMIT} items at once`
      };
    }

    setSelectedIdsState(new Set(allIds));
    setSelectAllMode(true);

    return { success: true };
  }, []);

  /**
   * 获取选中数量
   */
  const getSelectedCount = useCallback(() => {
    return selectedIds.size;
  }, [selectedIds]);

  /**
   * 检查是否选中
   */
  const isSelected = useCallback((id: number): boolean => {
    return selectedIds.has(id);
  }, [selectedIds]);

  /**
   * Ant Design Table rowSelection 配置
   */
  const rowSelection = useMemo<RowSelectionConfig>(() => ({
    type: 'checkbox',
    selectedRowKeys: Array.from(selectedIds),
    onChange: handleSelectionChange,
    getCheckboxProps: (record: any) => ({
      disabled: record?.disabled || false
    })
  }), [selectedIds, handleSelectionChange]);

  return {
    // 状态
    selectedIds,
    selectAllMode,

    // rowSelection 配置
    rowSelection,

    // 操作方法
    clearSelection,
    toggleSelection,
    setSelectedIds,
    selectAll,
    getSelectedCount,
    isSelected
  };
}

/**
 * 默认导出
 */
export default useBatchSelection;
