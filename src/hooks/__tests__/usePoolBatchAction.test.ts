// Input: usePoolBatchAction hook
// Output: Tests for batch action hook
// Pos: 测试批量操作 Hook

import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { usePoolBatchAction } from '../usePoolBatchAction';
import { poolApi } from '@/api/pool';

// Mock poolApi
vi.mock('@/api/pool', () => ({
  poolApi: {
    delete: vi.fn(),
    archiveItems: vi.fn(),
  },
}));

describe('usePoolBatchAction', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should start with empty selection', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      expect(result.current.state.selection.size).toBe(0);
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should start with not executing state', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      expect(result.current.state.isExecuting).toBe(false);
    });

    it('should start with no result', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      expect(result.current.state.result).toBeNull();
    });
  });

  describe('selection management', () => {
    it('should add card to selection when toggled', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.toggleSelection('card-1');
      });

      expect(result.current.isSelected('card-1')).toBe(true);
      expect(result.current.getSelectedCount()).toBe(1);
    });

    it('should remove card from selection when toggled twice', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.toggleSelection('card-1');
      });
      act(() => {
        result.current.toggleSelection('card-1');
      });

      expect(result.current.isSelected('card-1')).toBe(false);
      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should select all cards when selectAll is called', () => {
      const { result } = renderHook(() => usePoolBatchAction());
      const cardIds = ['card-1', 'card-2', 'card-3'];

      act(() => {
        result.current.selectAll(cardIds);
      });

      expect(result.current.getSelectedCount()).toBe(3);
      expect(result.current.isSelected('card-1')).toBe(true);
      expect(result.current.isSelected('card-2')).toBe(true);
      expect(result.current.isSelected('card-3')).toBe(true);
    });

    it('should clear selection when clearSelection is called', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });
      act(() => {
        result.current.clearSelection();
      });

      expect(result.current.getSelectedCount()).toBe(0);
    });

    it('should get selected IDs as array', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2', 'card-3']);
      });

      const ids = result.current.getSelectedIds();
      expect(ids).toEqual(['card-1', 'card-2', 'card-3']);
      expect(Array.isArray(ids)).toBe(true);
    });
  });

  describe('batch delete', () => {
    it('should execute batch delete successfully', async () => {
      const mockDelete = vi.mocked(poolApi.delete).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('delete');
      });

      expect(mockDelete).toHaveBeenCalledTimes(2);
      expect(actionResult?.success).toBe(true);
      expect(actionResult?.successCount).toBe(2);
      expect(actionResult?.failCount).toBe(0);
      expect(result.current.state.result?.message).toContain('删除成功 2 条');
    });

    it('should handle partial failures in batch delete', async () => {
      const _mockDelete = vi.mocked(poolApi.delete)
        .mockResolvedValueOnce(undefined)
        .mockRejectedValueOnce(new Error('删除失败'));
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('delete');
      });

      expect(actionResult?.success).toBe(false);
      expect(actionResult?.successCount).toBe(1);
      expect(actionResult?.failCount).toBe(1);
      expect(actionResult?.errors).toHaveLength(1);
      expect(result.current.state.result?.message).toContain('完成：成功 1 条，失败 1 条');
    });

    it('should return empty selection error when no items selected', async () => {
      const { result } = renderHook(() => usePoolBatchAction());

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('delete');
      });

      expect(actionResult?.success).toBe(false);
      expect(actionResult?.message).toContain('请先选择要操作的文件');
    });
  });

  describe('batch archive', () => {
    it('should execute batch archive successfully', async () => {
      const mockArchive = vi.mocked(poolApi.archiveItems).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2', 'card-3']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('archive');
      });

      expect(mockArchive).toHaveBeenCalledWith(['card-1', 'card-2', 'card-3']);
      expect(actionResult?.success).toBe(true);
      expect(actionResult?.successCount).toBe(3);
    });

    it('should handle archive API failure', async () => {
      vi.mocked(poolApi.archiveItems).mockRejectedValue(new Error('归档失败'));
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('archive');
      });

      expect(actionResult?.success).toBe(false);
      expect(actionResult?.failCount).toBe(1);
    });
  });

  describe('non-API actions', () => {
    it('should handle recheck action', async () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('recheck');
      });

      expect(actionResult?.success).toBe(true);
      expect(actionResult?.message).toContain('重新检测成功 2 条');
    });

    it('should handle edit action', async () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('edit');
      });

      expect(actionResult?.success).toBe(true);
      expect(actionResult?.message).toContain('编辑成功 1 条');
    });

    it('should handle match action', async () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('match');
      });

      expect(actionResult?.success).toBe(true);
      expect(actionResult?.message).toContain('匹配成功 1 条');
    });
  });

  describe('execution state', () => {
    it('should set isExecuting during operation', async () => {
      vi.mocked(poolApi.delete).mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      act(() => {
        result.current.executeAction('delete');
      });

      expect(result.current.state.isExecuting).toBe(true);

      await waitFor(() => {
        expect(result.current.state.isExecuting).toBe(false);
      });
    });
  });

  describe('result management', () => {
    it('should store result after action', async () => {
      vi.mocked(poolApi.delete).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      await act(async () => {
        await result.current.executeAction('delete');
      });

      expect(result.current.state.result).not.toBeNull();
      expect(result.current.state.result?.success).toBe(true);
    });

    it('should clear result when clearResult is called', async () => {
      vi.mocked(poolApi.delete).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1']);
      });

      await act(async () => {
        await result.current.executeAction('delete');
      });

      act(() => {
        result.current.clearResult();
      });

      expect(result.current.state.result).toBeNull();
    });
  });

  describe('custom selected IDs', () => {
    it('should use provided IDs instead of current selection', async () => {
      vi.mocked(poolApi.delete).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      // Set up selection with 2 items
      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });

      // But execute with custom IDs (3 items)
      let actionResult: any;
      await act(async () => {
        actionResult = await result.current.executeAction('delete', [
          'card-3',
          'card-4',
          'card-5',
        ]);
      });

      expect(actionResult?.successCount).toBe(3);
      expect(vi.mocked(poolApi.delete)).toHaveBeenCalledTimes(3);
    });
  });

  describe('selection persistence', () => {
    it('should maintain selection state after action', async () => {
      vi.mocked(poolApi.delete).mockResolvedValue(undefined);
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.selectAll(['card-1', 'card-2']);
      });

      await act(async () => {
        await result.current.executeAction('delete');
      });

      // Selection should still be there after action completes
      expect(result.current.getSelectedCount()).toBe(2);
      expect(result.current.isSelected('card-1')).toBe(true);
    });
  });

  describe('rapid toggle', () => {
    it('should handle rapid toggle operations correctly', () => {
      const { result } = renderHook(() => usePoolBatchAction());

      act(() => {
        result.current.toggleSelection('card-1');
        result.current.toggleSelection('card-2');
        result.current.toggleSelection('card-1'); // Unselect card-1
        result.current.toggleSelection('card-3');
      });

      expect(result.current.isSelected('card-1')).toBe(false);
      expect(result.current.isSelected('card-2')).toBe(true);
      expect(result.current.isSelected('card-3')).toBe(true);
      expect(result.current.getSelectedCount()).toBe(2);
    });
  });
});
