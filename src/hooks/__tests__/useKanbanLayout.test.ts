// Input: useKanbanLayout hook test suite
// Output: Vitest tests for kanban layout hook
// Pos: 测试看板布局 Hook

import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  useKanbanLayout,
  calculateColumnCardCounts,
  areAllColumnsEmpty,
  getFirstNonEmptyColumnId,
  MIN_COLUMN_WIDTH,
  MAX_COLUMN_WIDTH,
  COLLAPSED_COLUMN_WIDTH,
} from '../useKanbanLayout';

// Mock ResizeObserver
class MockResizeObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

global.ResizeObserver = MockResizeObserver as any;

describe('useKanbanLayout', () => {
  let mockContainerRef: React.RefObject<HTMLElement>;
  let mockElement: HTMLElement;

  beforeEach(() => {
    // 创建模拟容器元素
    mockElement = {
      clientWidth: 1200,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    } as unknown as HTMLElement;

    mockContainerRef = {
      current: mockElement,
    };

    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should start with minimum column width', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.columnWidth).toBe(MIN_COLUMN_WIDTH);
    });

    it('should start with no collapsed columns', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.collapsedColumns.size).toBe(0);
    });

    it('should start with zero container width before resize', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.containerWidth).toBe(0);
    });

    it('should not be overflowing initially', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.isOverflowing).toBe(false);
    });
  });

  describe('column width calculation', () => {
    it('should calculate column width based on container width', async () => {
      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: mockContainerRef })
      );

      await waitFor(() => {
        expect(result.current.containerWidth).toBe(1200);
      });

      // 1200px 容器，4列，每列约 (1200 - 32 - 48) / 4 = 280px
      expect(result.current.columnWidth).toBeGreaterThanOrEqual(MIN_COLUMN_WIDTH);
      expect(result.current.columnWidth).toBeLessThanOrEqual(MAX_COLUMN_WIDTH);
    });

    it('should respect minimum column width', () => {
      const smallElement = { clientWidth: 500 } as unknown as HTMLElement;
      const smallRef = { current: smallElement };

      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: smallRef })
      );

      // 即使容器很小，列宽也不应小于最小值
      expect(result.current.columnWidth).toBeGreaterThanOrEqual(MIN_COLUMN_WIDTH);
    });

    it('should respect maximum column width', () => {
      const largeElement = { clientWidth: 3000 } as unknown as HTMLElement;
      const largeRef = { current: largeElement };

      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: largeRef })
      );

      // 即使容器很大，列宽也不应超过最大值
      expect(result.current.columnWidth).toBeLessThanOrEqual(MAX_COLUMN_WIDTH);
    });

    it('should allow manual column width setting within bounds', () => {
      const { result } = renderHook(() => useKanbanLayout());

      act(() => {
        result.current.setColumnWidth(300);
      });

      expect(result.current.columnWidth).toBe(300);
    });

    it('should clamp column width to minimum when set too low', () => {
      const { result } = renderHook(() => useKanbanLayout());

      act(() => {
        result.current.setColumnWidth(100);
      });

      expect(result.current.columnWidth).toBe(MIN_COLUMN_WIDTH);
    });

    it('should clamp column width to maximum when set too high', () => {
      const { result } = renderHook(() => useKanbanLayout());

      act(() => {
        result.current.setColumnWidth(1000);
      });

      expect(result.current.columnWidth).toBe(MAX_COLUMN_WIDTH);
    });

    it('should recalculate width when requested', async () => {
      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: mockContainerRef })
      );

      await waitFor(() => {
        expect(result.current.containerWidth).toBeGreaterThan(0);
      });

      // 重新计算会基于当前容器的 clientWidth
      act(() => {
        result.current.recalculateWidth();
      });

      // 验证重新计算后列宽在合法范围内
      expect(result.current.columnWidth).toBeGreaterThanOrEqual(MIN_COLUMN_WIDTH);
      expect(result.current.columnWidth).toBeLessThanOrEqual(MAX_COLUMN_WIDTH);
    });
  });

  describe('empty column detection', () => {
    const mockCardsByColumn = {
      pending: [{ id: '1' }, { id: '2' }],
      'needs-attention': [],
      ready: [{ id: '3' }],
      processing: [],
    };

    it('should identify empty columns', () => {
      const { result } = renderHook(() => useKanbanLayout());

      const emptyColumns = result.current.getEmptyColumns(mockCardsByColumn);

      expect(emptyColumns).toEqual(['needs-attention', 'processing']);
      expect(emptyColumns).toHaveLength(2);
    });

    it('should return empty array when all columns have cards', () => {
      const { result } = renderHook(() => useKanbanLayout());

      const fullColumns = {
        pending: [{ id: '1' }],
        'needs-attention': [{ id: '2' }],
        ready: [{ id: '3' }],
        processing: [{ id: '4' }],
      };

      const emptyColumns = result.current.getEmptyColumns(fullColumns);

      expect(emptyColumns).toEqual([]);
    });

    it('should correctly check if column has cards', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.hasCards('pending', mockCardsByColumn)).toBe(true);
      expect(result.current.hasCards('ready', mockCardsByColumn)).toBe(true);
      expect(result.current.hasCards('needs-attention', mockCardsByColumn)).toBe(false);
      expect(result.current.hasCards('processing', mockCardsByColumn)).toBe(false);
    });

    it('should handle missing column keys gracefully', () => {
      const { result } = renderHook(() => useKanbanLayout());

      const incompleteData = {
        pending: [{ id: '1' }],
      };

      expect(result.current.hasCards('ready', incompleteData)).toBe(false);
      expect(result.current.getEmptyColumns(incompleteData)).toContain('ready');
    });
  });

  describe('collapse state management', () => {
    it('should toggle column collapse state', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.isCollapsed('pending')).toBe(false);

      act(() => {
        result.current.toggleCollapse('pending');
      });

      expect(result.current.isCollapsed('pending')).toBe(true);
      expect(result.current.collapsedColumns.has('pending')).toBe(true);

      act(() => {
        result.current.toggleCollapse('pending');
      });

      expect(result.current.isCollapsed('pending')).toBe(false);
    });

    it('should maintain multiple collapsed columns', () => {
      const { result } = renderHook(() => useKanbanLayout());

      act(() => {
        result.current.toggleCollapse('pending');
        result.current.toggleCollapse('ready');
      });

      expect(result.current.collapsedColumns.size).toBe(2);
      expect(result.current.isCollapsed('pending')).toBe(true);
      expect(result.current.isCollapsed('ready')).toBe(true);
      expect(result.current.isCollapsed('processing')).toBe(false);
    });

    it('should expand all columns', () => {
      const { result } = renderHook(() => useKanbanLayout());

      act(() => {
        result.current.toggleCollapse('pending');
        result.current.toggleCollapse('ready');
        result.current.toggleCollapse('processing');
      });

      expect(result.current.collapsedColumns.size).toBe(3);

      act(() => {
        result.current.expandAll();
      });

      expect(result.current.collapsedColumns.size).toBe(0);
      expect(result.current.isCollapsed('pending')).toBe(false);
      expect(result.current.isCollapsed('ready')).toBe(false);
      expect(result.current.isCollapsed('processing')).toBe(false);
    });

    it('should collapse all empty columns', () => {
      const { result } = renderHook(() => useKanbanLayout());

      const mockCardsByColumn = {
        pending: [{ id: '1' }],
        'needs-attention': [],
        ready: [],
        processing: [{ id: '2' }],
      };

      act(() => {
        result.current.collapseAllEmpty(mockCardsByColumn);
      });

      expect(result.current.collapsedColumns.size).toBe(2);
      expect(result.current.isCollapsed('needs-attention')).toBe(true);
      expect(result.current.isCollapsed('ready')).toBe(true);
      expect(result.current.isCollapsed('pending')).toBe(false);
      expect(result.current.isCollapsed('processing')).toBe(false);
    });
  });

  describe('overflow detection', () => {
    it('should detect when content overflows container', async () => {
      // 小容器会导致溢出
      const smallElement = { clientWidth: 800 } as unknown as HTMLElement;
      const smallRef = { current: smallElement };

      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: smallRef })
      );

      await waitFor(() => {
        expect(result.current.containerWidth).toBe(800);
      });

      // 4列 * 280px + 3 * 16px + 32px = 1160px > 800px
      // 应该检测到溢出
      expect(result.current.isOverflowing).toBe(true);
    });

    it('should not overflow when container is wide enough', async () => {
      const largeElement = { clientWidth: 2000 } as unknown as HTMLElement;
      const largeRef = { current: largeElement };

      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: largeRef })
      );

      await waitFor(() => {
        expect(result.current.containerWidth).toBe(2000);
      });

      // 宽容器应该不溢出
      expect(result.current.isOverflowing).toBe(false);
    });

    it('should reduce overflow when columns are collapsed', async () => {
      const element = { clientWidth: 800 } as unknown as HTMLElement;
      const ref = { current: element };

      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: ref })
      );

      await waitFor(() => {
        expect(result.current.containerWidth).toBe(800);
      });

      expect(result.current.isOverflowing).toBe(true);

      // 折叠两列后应该减少溢出
      act(() => {
        result.current.toggleCollapse('pending');
        result.current.toggleCollapse('ready');
      });

      // 折叠后列宽可能会变大，但总内容宽度应该减少
      const visibleColumnCount = result.current.getVisibleColumnCount();
      expect(visibleColumnCount).toBe(2);
    });
  });

  describe('helper functions', () => {
    it('should return correct visible column count', () => {
      const { result } = renderHook(() => useKanbanLayout());

      expect(result.current.getVisibleColumnCount()).toBe(4);

      act(() => {
        result.current.toggleCollapse('pending');
      });

      expect(result.current.getVisibleColumnCount()).toBe(3);

      act(() => {
        result.current.toggleCollapse('ready');
        result.current.toggleCollapse('processing');
      });

      expect(result.current.getVisibleColumnCount()).toBe(1);
    });

    it('should calculate total content width correctly', () => {
      const { result } = renderHook(() => useKanbanLayout());

      // 4列 * 列宽 + 3 * 16px间距 + 32px内边距
      const expectedWidth = 4 * result.current.columnWidth + 3 * 16 + 32;
      expect(result.current.getTotalContentWidth()).toBe(expectedWidth);
    });

    it('should adjust total content width when columns are collapsed', () => {
      const { result } = renderHook(() => useKanbanLayout());

      const normalWidth = result.current.getTotalContentWidth();

      act(() => {
        result.current.toggleCollapse('pending');
      });

      const collapsedWidth = result.current.getTotalContentWidth();

      // 折叠后宽度应该减少
      expect(collapsedWidth).toBeLessThan(normalWidth);
    });
  });

  describe('utility functions', () => {
    describe('calculateColumnCardCounts', () => {
      it('should calculate card counts for each column', () => {
        const cardsByColumn = {
          pending: [{ id: '1' }, { id: '2' }, { id: '3' }],
          'needs-attention': [{ id: '4' }],
          ready: [],
          processing: [{ id: '5' }, { id: '6' }],
        };

        const counts = calculateColumnCardCounts(cardsByColumn);

        expect(counts.pending).toBe(3);
        expect(counts['needs-attention']).toBe(1);
        expect(counts.ready).toBe(0);
        expect(counts.processing).toBe(2);
      });

      it('should handle empty data', () => {
        const counts = calculateColumnCardCounts({});

        expect(counts.pending).toBe(0);
        expect(counts.ready).toBe(0);
        expect(counts.processing).toBe(0);
        expect(counts['needs-attention']).toBe(0);
      });
    });

    describe('areAllColumnsEmpty', () => {
      it('should return true when all columns are empty', () => {
        const emptyData = {
          pending: [],
          'needs-attention': [],
          ready: [],
          processing: [],
        };

        expect(areAllColumnsEmpty(emptyData)).toBe(true);
      });

      it('should return false when at least one column has cards', () => {
        const dataWithCards = {
          pending: [],
          'needs-attention': [{ id: '1' }],
          ready: [],
          processing: [],
        };

        expect(areAllColumnsEmpty(dataWithCards)).toBe(false);
      });

      it('should handle incomplete data', () => {
        const incompleteData = {
          pending: [],
        };

        // 未定义的列默认为空数组，所以所有列都为空
        expect(areAllColumnsEmpty(incompleteData)).toBe(true);
      });
    });

    describe('getFirstNonEmptyColumnId', () => {
      it('should return first non-empty column ID', () => {
        const data = {
          pending: [],
          'needs-attention': [{ id: '1' }],
          ready: [{ id: '2' }],
          processing: [],
        };

        expect(getFirstNonEmptyColumnId(data)).toBe('needs-attention');
      });

      it('should return first column ID when first column has cards', () => {
        const data = {
          pending: [{ id: '1' }],
          'needs-attention': [{ id: '2' }],
          ready: [{ id: '3' }],
          processing: [{ id: '4' }],
        };

        expect(getFirstNonEmptyColumnId(data)).toBe('pending');
      });

      it('should return null when all columns are empty', () => {
        const emptyData = {
          pending: [],
          'needs-attention': [],
          ready: [],
          processing: [],
        };

        expect(getFirstNonEmptyColumnId(emptyData)).toBeNull();
      });

      it('should return null for empty data', () => {
        expect(getFirstNonEmptyColumnId({})).toBeNull();
      });
    });
  });

  describe('when disabled', () => {
    it('should not observe container when disabled', () => {
      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: mockContainerRef, enabled: false })
      );

      // 当 disabled 时，ResizeObserver 不会被设置，containerWidth 保持为 0
      expect(result.current.containerWidth).toBe(0);
      expect(result.current.columnWidth).toBe(MIN_COLUMN_WIDTH);
    });

    it('should maintain minimum width when disabled', () => {
      const { result } = renderHook(() =>
        useKanbanLayout({ containerRef: mockContainerRef, enabled: false })
      );

      expect(result.current.columnWidth).toBe(MIN_COLUMN_WIDTH);
      expect(result.current.containerWidth).toBe(0);
    });
  });

  describe('constants', () => {
    it('should export minimum column width constant', () => {
      expect(MIN_COLUMN_WIDTH).toBe(280);
    });

    it('should export maximum column width constant', () => {
      expect(MAX_COLUMN_WIDTH).toBe(400);
    });

    it('should export collapsed column width constant', () => {
      expect(COLLAPSED_COLUMN_WIDTH).toBe(48);
    });
  });
});
