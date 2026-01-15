// Input: React Hooks, Ant Design Table rowSelection 配置
// Output: 批量选择状态管理 Hook (rowSelection + 操作方法)
// Pos: src/components/operations/useBatchSelection.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { flushSync } from 'react-dom';
import type { Key } from 'react';

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
  selectedIds: Set<string>;  // 修改: number -> string 以支持大整数 ID
  selectAllMode: boolean;
}

/**
 * Ant Design Table rowSelection 配置
 */
export interface RowSelectionConfig {
  type: 'checkbox';
  selectedRowKeys: Key[];
  onChange: (selectedRowKeys: Key[]) => void;
  getCheckboxProps?: (record: any) => { disabled?: boolean };
}

/**
 * 批量选择 Hook 返回值
 */
export interface UseBatchSelectionReturn {
  // 状态
  selectedIds: Set<string>;  // 修改: number -> string
  selectAllMode: boolean;
  lastError?: SelectionResult;

  // Ant Design Table rowSelection 配置
  rowSelection: RowSelectionConfig;

  // 操作方法
  clearSelection: () => SelectionResult;
  toggleSelection: (id: string) => SelectionResult;  // 修改: number -> string
  setSelectedIds: (ids: Set<string> | string[]) => SelectionResult;  // 修改
  selectAll: (allIds: string[]) => SelectionResult;  // 修改: number[] -> string[]
  getSelectedCount: () => number;
  isSelected: (id: string) => boolean;  // 修改: number -> string
}

/**
 * 批量选择 Hook
 *
 * 提供跨页选择状态管理、全选/反选控制和数量统计
 * 兼容 Ant Design Table 的 rowSelection 配置
 *
 * @returns 批量选择状态和操作方法
 */
export function useBatchSelection(): UseBatchSelectionReturn {
  const [selectedIds, setSelectedIdsState] = useState<Set<string>>(new Set());  // 修改: number -> string
  const [selectAllMode, setSelectAllMode] = useState<boolean>(false);
  const [lastError, setLastError] = useState<SelectionResult | undefined>();

  /**
   * 处理 Table rowSelection 的 onChange 回调
   * 保持 ID 为字符串，避免大整数精度丢失
   */
  const handleSelectionChange = useCallback((selectedRowKeys: Key[]) => {
    // 转换 Key[] 为 string[]，保持原始字符串值
    const stringKeys = selectedRowKeys.map(key => String(key));

    // 检查是否超出限制
    if (stringKeys.length > MAX_SELECTION_LIMIT) {
      const error: SelectionResult = {
        success: false,
        reason: `Cannot select more than ${MAX_SELECTION_LIMIT} items`
      };
      setLastError(error);
      console.warn(
        `[useBatchSelection] Selection limit exceeded: ${stringKeys.length} > ${MAX_SELECTION_LIMIT}`
      );
      return;
    }

    setSelectedIdsState(new Set(stringKeys));
    setLastError(undefined);

    if (selectAllMode && stringKeys.length < MAX_SELECTION_LIMIT) {
      setSelectAllMode(false);
    }
  }, [selectAllMode]);

  /**
   * 清空选择
   */
  const clearSelection = useCallback((): SelectionResult => {
    setSelectedIdsState(new Set());
    setSelectAllMode(false);
    setLastError(undefined);
    return { success: true };
  }, []);

  /**
   * 切换单条选中状态
   */
  const toggleSelection = useCallback((id: string): SelectionResult => {  // 修改: number -> string
    let result: SelectionResult = { success: false, reason: 'Unknown error' };

    flushSync(() => {
      setSelectedIdsState((prev) => {
        const isAlreadySelected = prev.has(id);

        if (isAlreadySelected) {
          const newSet = new Set(prev);
          newSet.delete(id);
          setSelectAllMode(false);
          setLastError(undefined);
          result = { success: true };
          return newSet;
        } else {
          if (prev.size >= MAX_SELECTION_LIMIT) {
            result = { success: false, reason: `Cannot select more than ${MAX_SELECTION_LIMIT} items` };
            setLastError(result);
            return prev;
          }

          const newSet = new Set(prev);
          newSet.add(id);
          setLastError(undefined);
          result = { success: true };
          return newSet;
        }
      });
    });

    return result;
  }, []);

  /**
   * 设置选中的 ID 集合
   */
  const setSelectedIds = useCallback((ids: Set<string> | string[]): SelectionResult => {  // 修改
    const newIds = ids instanceof Set ? ids : new Set(ids);

    if (newIds.size > MAX_SELECTION_LIMIT) {
      const error: SelectionResult = {
        success: false,
        reason: `Cannot select more than ${MAX_SELECTION_LIMIT} items`
      };
      setLastError(error);
      console.warn(
        `[useBatchSelection] Selection limit exceeded: ${newIds.size} > ${MAX_SELECTION_LIMIT}`
      );
      return error;
    }

    setSelectedIdsState(newIds);
    setSelectAllMode(false);
    setLastError(undefined);
    return { success: true };
  }, []);

  /**
   * 全选所有记录
   */
  const selectAll = useCallback((allIds: string[]): SelectionResult => {  // 修改: number[] -> string[]
    if (allIds.length > MAX_SELECTION_LIMIT) {
      const error: SelectionResult = {
        success: false,
        reason: `Cannot select more than ${MAX_SELECTION_LIMIT} items`
      };
      setLastError(error);
      return error;
    }

    setSelectedIdsState(new Set(allIds));
    setSelectAllMode(true);
    setLastError(undefined);

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
  const isSelected = useCallback((id: string): boolean => {  // 修改: number -> string
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
    selectedIds,
    selectAllMode,
    lastError,
    rowSelection,
    clearSelection,
    toggleSelection,
    setSelectedIds,
    selectAll,
    getSelectedCount,
    isSelected
  };
}

export default useBatchSelection;
