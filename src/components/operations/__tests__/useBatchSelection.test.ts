// src/components/operations/__tests__/useBatchSelection.test.ts

import { renderHook, act } from '@testing-library/react';
import { useBatchSelection, MAX_SELECTION_LIMIT } from '../useBatchSelection';

describe('useBatchSelection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('basic selection', () => {
    it('should initialize with empty selection', () => {
      const { result } = renderHook(() => useBatchSelection());

      expect(result.current.selectedIds).toEqual(new Set());
      expect(result.current.selectAllMode).toBe(false);
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should toggle item selection', () => {
      const { result } = renderHook(() => useBatchSelection());

      // 选中 ID 1
      act(() => {
        const toggleResult = result.current.toggleSelection(1);
        expect(toggleResult.success).toBe(true);
      });

      expect(result.current.isSelected(1)).toBe(true);
      expect(result.current.getSelectedCount()).toBe(1);

      // 取消选中 ID 1
      act(() => {
        result.current.toggleSelection(1);
      });

      expect(result.current.isSelected(1)).toBe(false);
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should clear all selections', () => {
      const { result } = renderHook(() => useBatchSelection());

      // 选中多个 ID
      act(() => {
        result.current.toggleSelection(1);
        result.current.toggleSelection(2);
        result.current.toggleSelection(3);
      });

      expect(result.current.getSelectedCount()).toBe(3);

      // 清空选择
      act(() => {
        result.current.clearSelection();
      });

      expect(result.current.getSelectedCount()).toBe(0);
      expect(result.current.selectedIds).toEqual(new Set());
      expect(result.current.selectAllMode).toBe(false);
    });

    it('should set selected IDs from array', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.setSelectedIds([1, 2, 3, 4, 5]);
      });

      expect(result.current.getSelectedCount()).toBe(5);
      expect(result.current.isSelected(1)).toBe(true);
      expect(result.current.isSelected(3)).toBe(true);
      expect(result.current.isSelected(6)).toBe(false);
    });

    it('should set selected IDs from Set', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.setSelectedIds(new Set([10, 20, 30]));
      });

      expect(result.current.getSelectedCount()).toBe(3);
      expect(result.current.isSelected(10)).toBe(true);
      expect(result.current.isSelected(20)).toBe(true);
    });
  });

  describe('rowSelection configuration', () => {
    it('should provide compatible rowSelection config', () => {
      const { result } = renderHook(() => useBatchSelection());

      expect(result.current.rowSelection).toEqual({
        type: 'checkbox',
        selectedRowKeys: [],
        onChange: expect.any(Function),
        getCheckboxProps: expect.any(Function)
      });
    });

    it('should update selectedRowKeys when selection changes', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.toggleSelection(1);
        result.current.toggleSelection(2);
      });

      expect(result.current.rowSelection.selectedRowKeys).toEqual([1, 2]);
    });

    it('should call onChange when table selection changes', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.rowSelection.onChange([5, 10, 15]);
      });

      expect(result.current.selectedIds).toEqual(new Set([5, 10, 15]));
      expect(result.current.getSelectedCount()).toBe(3);
    });

    it('should return checkbox props', () => {
      const { result } = renderHook(() => useBatchSelection());

      const normalRecord = { id: 1 };
      const disabledRecord = { id: 2, disabled: true };

      expect(result.current.rowSelection.getCheckboxProps?.(normalRecord)).toEqual({
        disabled: false
      });
      expect(result.current.rowSelection.getCheckboxProps?.(disabledRecord)).toEqual({
        disabled: true
      });
    });
  });

  describe('selectAll', () => {
    it('should select all items when within limit', () => {
      const { result } = renderHook(() => useBatchSelection());

      const allIds = Array.from({ length: 50 }, (_, i) => i + 1);

      act(() => {
        const selectResult = result.current.selectAll(allIds);
        expect(selectResult.success).toBe(true);
      });

      expect(result.current.getSelectedCount()).toBe(50);
      expect(result.current.selectAllMode).toBe(true);
    });

    it('should fail when trying to select more than limit', () => {
      const { result } = renderHook(() => useBatchSelection());

      const allIds = Array.from({ length: MAX_SELECTION_LIMIT + 1 }, (_, i) => i + 1);

      act(() => {
        const selectResult = result.current.selectAll(allIds);
        expect(selectResult.success).toBe(false);
        expect(selectResult.reason).toContain(`Cannot select more than ${MAX_SELECTION_LIMIT}`);
      });

      expect(result.current.getSelectedCount()).toBe(0);
      expect(result.current.selectAllMode).toBe(false);
    });

    it('should select exactly MAX_SELECTION_LIMIT items', () => {
      const { result } = renderHook(() => useBatchSelection());

      const allIds = Array.from({ length: MAX_SELECTION_LIMIT }, (_, i) => i + 1);

      act(() => {
        const selectResult = result.current.selectAll(allIds);
        expect(selectResult.success).toBe(true);
      });

      expect(result.current.getSelectedCount()).toBe(MAX_SELECTION_LIMIT);
      expect(result.current.selectAllMode).toBe(true);
    });
  });

  describe('boundary limits', () => {
    it('should prevent selecting more than MAX_SELECTION_LIMIT', () => {
      const { result } = renderHook(() => useBatchSelection());

      // 尝试选中超过限制的数量
      const tooManyIds = Array.from({ length: MAX_SELECTION_LIMIT + 10 }, (_, i) => i + 1);

      act(() => {
        result.current.setSelectedIds(tooManyIds);
      });

      // 应该被阻止，保持原状态
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should prevent toggle beyond limit', () => {
      const { result } = renderHook(() => useBatchSelection());

      // 先选中 MAX_SELECTION_LIMIT - 1 个
      const ids = Array.from({ length: MAX_SELECTION_LIMIT - 1 }, (_, i) => i + 1);

      act(() => {
        result.current.setSelectedIds(ids);
      });

      expect(result.current.getSelectedCount()).toBe(MAX_SELECTION_LIMIT - 1);

      // 再尝试 toggle 一个新的
      act(() => {
        result.current.toggleSelection(MAX_SELECTION_LIMIT);
      });

      expect(result.current.getSelectedCount()).toBe(MAX_SELECTION_LIMIT);

      // 再尝试 toggle 一个新的（应该失败）
      act(() => {
        result.current.toggleSelection(MAX_SELECTION_LIMIT + 1);
      });

      expect(result.current.getSelectedCount()).toBe(MAX_SELECTION_LIMIT);
    });

    it('should prevent table onChange beyond limit', () => {
      const { result } = renderHook(() => useBatchSelection());

      const tooManyKeys = Array.from({ length: MAX_SELECTION_LIMIT + 5 }, (_, i) => i + 1);

      act(() => {
        result.current.rowSelection.onChange(tooManyKeys);
      });

      // 应该被阻止，保持原状态
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should allow exactly MAX_SELECTION_LIMIT items', () => {
      const { result } = renderHook(() => useBatchSelection());

      const exactLimitIds = Array.from({ length: MAX_SELECTION_LIMIT }, (_, i) => i + 1);

      act(() => {
        result.current.setSelectedIds(exactLimitIds);
      });

      expect(result.current.getSelectedCount()).toBe(MAX_SELECTION_LIMIT);
    });
  });

  describe('selectAllMode', () => {
    it('should set selectAllMode to true when selectAll is called', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.selectAll([1, 2, 3]);
      });

      expect(result.current.selectAllMode).toBe(true);
    });

    it('should exit selectAllMode when toggleSelection removes an item', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.selectAll([1, 2, 3]);
      });

      expect(result.current.selectAllMode).toBe(true);

      act(() => {
        result.current.toggleSelection(1);
      });

      expect(result.current.selectAllMode).toBe(false);
    });

    it('should exit selectAllMode when clearSelection is called', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.selectAll([1, 2, 3]);
      });

      expect(result.current.selectAllMode).toBe(true);

      act(() => {
        result.current.clearSelection();
      });

      expect(result.current.selectAllMode).toBe(false);
    });

    it('should exit selectAllMode when setSelectedIds is called', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.selectAll([1, 2, 3]);
      });

      expect(result.current.selectAllMode).toBe(true);

      act(() => {
        result.current.setSelectedIds([4, 5]);
      });

      expect(result.current.selectAllMode).toBe(false);
    });
  });

  describe('edge cases', () => {
    it('should handle empty array in setSelectedIds', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.setSelectedIds([1, 2, 3]);
      });

      expect(result.current.getSelectedCount()).toBe(3);

      act(() => {
        result.current.setSelectedIds([]);
      });

      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should handle duplicate IDs in setSelectedIds array', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.setSelectedIds([1, 2, 2, 3, 3, 3]);
      });

      // Set 会自动去重
      expect(result.current.getSelectedCount()).toBe(3);
    });

    it('should handle toggle same ID multiple times', () => {
      const { result } = renderHook(() => useBatchSelection());

      act(() => {
        result.current.toggleSelection(1);
        result.current.toggleSelection(1);
        result.current.toggleSelection(1);
      });

      // toggle 三次：选中 → 取消 → 选中
      expect(result.current.isSelected(1)).toBe(true);
    });

    it('should handle clearSelection when already empty', () => {
      const { result } = renderHook(() => useBatchSelection());

      expect(result.current.getSelectedCount()).toBe(0);

      act(() => {
        result.current.clearSelection();
      });

      expect(result.current.getSelectedCount()).toBe(0);
      expect(result.current.selectAllMode).toBe(false);
    });
  });
});
