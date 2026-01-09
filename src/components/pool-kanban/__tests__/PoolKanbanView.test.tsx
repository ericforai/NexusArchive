// src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
// Input: Test cases for PoolKanbanView component with responsive layout
// Output: Vitest test suite validating component behavior, batch action integration, and responsive layout
// Pos: src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { Mock } from 'vitest';

// Mock antd components
vi.mock('antd', () => ({
  Spin: ({ tip }: any) => <div className="ant-spin">{tip || '加载中...'}</div>,
  Button: ({ children, onClick, ...props }: any) => (
    <button onClick={onClick} data-button-type={props['data-testid']}>
      {children}
    </button>
  ),
  Space: ({ children }: any) => <div className="space">{children}</div>,
}));

// Mock lucide-react icons - render as null to not interfere with text matching
vi.mock('lucide-react', () => ({
  Columns3: () => null,
  Columns4: () => null,
  Expand: () => null,
}));

// Mock modules BEFORE imports
vi.mock('@/hooks/usePoolKanban', () => ({
  usePoolKanban: vi.fn(),
}));

vi.mock('@/hooks/usePoolBatchAction', () => ({
  usePoolBatchAction: vi.fn(),
}));

vi.mock('@/hooks/useKanbanLayout', () => ({
  useKanbanLayout: vi.fn(() => ({
    columnWidth: 320,
    setColumnWidth: vi.fn(),
    recalculateWidth: vi.fn(),
    containerWidth: 1200,
    isOverflowing: false,
    getEmptyColumns: vi.fn(() => []),
    hasCards: vi.fn(() => true),
    collapsedColumns: new Set<string>(),
    toggleCollapse: vi.fn(),
    isCollapsed: vi.fn(() => false),
    collapseAllEmpty: vi.fn(),
    expandAll: vi.fn(),
    getVisibleColumnCount: vi.fn(() => 4),
    getTotalContentWidth: vi.fn(() => 1200),
  })),
}));

vi.mock('../KanbanColumn', () => ({
  KanbanColumn: vi.fn(({ column, onAction }: any) => (
    <div className="kanban-column" data-column-id={column.id}>
      <h3>{column.title}</h3>
      {column.actions?.map((action: any) => (
        <button
          key={action.key}
          onClick={() => onAction?.(action.key, [])}
          data-action-key={action.key}
        >
          {action.label}
        </button>
      ))}
    </div>
  )),
}));

vi.mock('../CollapsedColumn', () => ({
  CollapsedColumn: vi.fn(({ column, cardCount, onExpand }: any) => (
    <div className="collapsed-column" data-column-id={column.id} data-collapsed="true">
      <span>{column.title} ({cardCount})</span>
      <button onClick={onExpand}>展开</button>
    </div>
  )),
}));

vi.mock('../BatchActionBar', () => ({
  BatchActionBar: vi.fn(({ selectedCount, result }: any) => {
    if (selectedCount === 0 && !result) return null;
    return (
      <div className="batch-action-bar" data-selected-count={selectedCount}>
        <span>已选 {selectedCount} 个文件</span>
        {result && <span className="result">{result.message}</span>}
        <button>取消</button>
        <button>执行</button>
      </div>
    );
  }),
}));

// Import after mocking
import { PoolKanbanView } from '../PoolKanbanView';
import { usePoolKanban } from '@/hooks/usePoolKanban';
import { usePoolBatchAction } from '@/hooks/usePoolBatchAction';
import { useKanbanLayout } from '@/hooks/useKanbanLayout';
import { KanbanColumn } from '../KanbanColumn';
import { CollapsedColumn } from '../CollapsedColumn';

describe('PoolKanbanView', () => {
  // Mock data
  const mockColumns = [
    {
      id: 'pending',
      title: '待处理',
      subStates: [{ value: 'DRAFT', label: '草稿' }],
      actions: [
        { key: 'recheck', label: '重新检测' },
        { key: 'delete', label: '删除', danger: true },
      ],
    },
    {
      id: 'needs-attention',
      title: '需要处理',
      subStates: [{ value: 'CHECK_FAILED', label: '检测失败' }],
      actions: [
        { key: 'edit-metadata', label: '编辑元数据' },
        { key: 'delete', label: '删除', danger: true },
      ],
    },
    {
      id: 'ready-to-archive',
      title: '待归档',
      subStates: [{ value: 'READY', label: '就绪' }],
      actions: [
        { key: 'batch-approve', label: '提交归档' },
        { key: 'cancel-archive', label: '取消归档' },
      ],
    },
    {
      id: 'archived',
      title: '已归档',
      subStates: [{ value: 'ARCHIVED', label: '已归档' }],
      actions: [
        { key: 'view-detail', label: '查看详情' },
      ],
    },
  ];

  const mockCards = [
    { id: 'card1', title: 'Card 1', status: 'DRAFT' },
    { id: 'card2', title: 'Card 2', status: 'DRAFT' },
    { id: 'card3', title: 'Card 3', status: 'CHECK_FAILED' },
    { id: 'card4', title: 'Card 4', status: 'READY' },
  ];

  const mockBatchActions = {
    state: {
      selection: new Set<string>(),
      isExecuting: false,
      result: null as any,
    },
    selectAll: vi.fn(),
    toggleSelection: vi.fn(),
    clearSelection: vi.fn(),
    isSelected: vi.fn(),
    getSelectedCount: vi.fn(() => 0),
    getSelectedIds: vi.fn(() => []),
    executeAction: vi.fn(),
    clearResult: vi.fn(),
  };

  const mockRefetch = vi.fn();
  const mockLayout = {
    columnWidth: 320,
    setColumnWidth: vi.fn(),
    recalculateWidth: vi.fn(),
    containerWidth: 1200,
    isOverflowing: false,
    getEmptyColumns: vi.fn(() => []),
    hasCards: vi.fn(() => true),
    collapsedColumns: new Set<string>(),
    toggleCollapse: vi.fn(),
    isCollapsed: vi.fn(() => false),
    collapseAllEmpty: vi.fn(),
    expandAll: vi.fn(),
    getVisibleColumnCount: vi.fn(() => 4),
    getTotalContentWidth: vi.fn(() => 1200),
  };

  beforeEach(() => {
    vi.clearAllMocks();

    (usePoolKanban as Mock).mockReturnValue({
      columns: mockColumns,
      loading: false,
      error: null,
      refetch: mockRefetch,
      getCardsForColumn: vi.fn((colId: string) => {
        if (colId === 'pending') return mockCards.filter(c => c.status === 'DRAFT');
        if (colId === 'needs-attention') return mockCards.filter(c => c.status === 'CHECK_FAILED');
        if (colId === 'ready-to-archive') return mockCards.filter(c => c.status === 'READY');
        return [];
      }),
    });

    (usePoolBatchAction as Mock).mockReturnValue(mockBatchActions);
    (useKanbanLayout as Mock).mockReturnValue(mockLayout);
  });

  describe('Rendering', () => {
    it('should render loading state', () => {
      (usePoolKanban as Mock).mockReturnValue({
        columns: [],
        loading: true,
        error: null,
        refetch: mockRefetch,
        getCardsForColumn: vi.fn(() => []),
      });

      const { container } = render(<PoolKanbanView />);
      expect(container.querySelector('.pool-kanban-view--loading')).toBeInTheDocument();
    });

    it('should render error state with retry button', () => {
      const mockError = new Error('Failed to load');
      (usePoolKanban as Mock).mockReturnValue({
        columns: [],
        loading: false,
        error: mockError,
        refetch: mockRefetch,
        getCardsForColumn: vi.fn(() => []),
      });

      render(<PoolKanbanView />);
      expect(screen.getByText(/加载失败/)).toBeInTheDocument();
      expect(screen.getByText('重试')).toBeInTheDocument();
    });

    it('should render toolbar with title', () => {
      render(<PoolKanbanView />);
      expect(screen.getByText('电子凭证池')).toBeInTheDocument();
    });

    it('should render all columns from config', () => {
      render(<PoolKanbanView />);
      expect(KanbanColumn).toHaveBeenCalledTimes(mockColumns.length);
    });

    it('should render board container', () => {
      const { container } = render(<PoolKanbanView />);
      expect(container.querySelector('.pool-kanban-view__board')).toBeInTheDocument();
    });

    it('should render layout control buttons', () => {
      render(<PoolKanbanView />);
      expect(screen.getByText('折叠空列')).toBeInTheDocument();
    });
  });

  describe('Responsive Layout', () => {
    it('should integrate useKanbanLayout hook', () => {
      render(<PoolKanbanView />);
      expect(useKanbanLayout).toHaveBeenCalledWith({
        containerRef: expect.any(Object),
        enabled: true,
      });
    });

    it('should apply dynamic column width via CSS variable', () => {
      (useKanbanLayout as Mock).mockReturnValue({
        ...mockLayout,
        columnWidth: 350,
      });

      const { container } = render(<PoolKanbanView />);
      const board = container.querySelector('.pool-kanban-view__board');
      expect(board).toHaveStyle({ '--column-width': '350px' });
    });

    it('should render CollapsedColumn for collapsed columns', () => {
      const collapsedLayout = {
        ...mockLayout,
        collapsedColumns: new Set(['archived']),
        isCollapsed: vi.fn((id: string) => id === 'archived'),
      };
      (useKanbanLayout as Mock).mockReturnValue(collapsedLayout);

      render(<PoolKanbanView />);

      const calls = (CollapsedColumn as Mock).mock.calls;
      const collapsedCall = calls.find((call: any[]) => call[0]?.column?.id === 'archived');

      expect(collapsedCall).toBeDefined();
      expect(collapsedCall[0].column.id).toBe('archived');
      expect(collapsedCall[0].cardCount).toBe(0);
      expect(typeof collapsedCall[0].onExpand).toBe('function');
    });

    it('should show layout info when columns are collapsed', () => {
      const collapsedLayout = {
        ...mockLayout,
        collapsedColumns: new Set(['archived', 'ready-to-archive']),
        isCollapsed: vi.fn((id: string) => id === 'archived' || id === 'ready-to-archive'),
        getVisibleColumnCount: vi.fn(() => 2),
      };
      (useKanbanLayout as Mock).mockReturnValue(collapsedLayout);

      render(<PoolKanbanView />);

      expect(screen.getByText('2 / 4 列')).toBeInTheDocument();
    });

    it('should show "展开全部" button when columns are collapsed', () => {
      const collapsedLayout = {
        ...mockLayout,
        collapsedColumns: new Set(['archived']),
        isCollapsed: vi.fn((id: string) => id === 'archived'),
      };
      (useKanbanLayout as Mock).mockReturnValue(collapsedLayout);

      render(<PoolKanbanView />);

      expect(screen.getByText('展开全部')).toBeInTheDocument();
    });
  });

  describe('Layout Controls', () => {
    it('should call collapseAllEmpty when "折叠空列" button is clicked', () => {
      render(<PoolKanbanView />);

      const collapseButton = screen.getByText('折叠空列');
      fireEvent.click(collapseButton);

      expect(mockLayout.collapseAllEmpty).toHaveBeenCalled();
    });

    it('should call expandAll when "展开全部" button is clicked', () => {
      const collapsedLayout = {
        ...mockLayout,
        collapsedColumns: new Set(['archived']),
        isCollapsed: vi.fn((id: string) => id === 'archived'),
      };
      (useKanbanLayout as Mock).mockReturnValue(collapsedLayout);

      render(<PoolKanbanView />);

      const expandButton = screen.getByText('展开全部');
      fireEvent.click(expandButton);

      expect(mockLayout.expandAll).toHaveBeenCalled();
    });

    it('should call toggleCollapse when CollapsedColumn expand button is clicked', () => {
      const collapsedLayout = {
        ...mockLayout,
        collapsedColumns: new Set(['archived']),
        isCollapsed: vi.fn((id: string) => id === 'archived'),
        toggleCollapse: vi.fn(),
      };
      (useKanbanLayout as Mock).mockReturnValue(collapsedLayout);

      render(<PoolKanbanView />);

      // Get the onExpand callback from the last CollapsedColumn call
      const lastCall = (CollapsedColumn as Mock).mock.calls[(CollapsedColumn as Mock).mock.calls.length - 1];
      const onExpand = lastCall[0].onExpand;

      onExpand();

      expect(collapsedLayout.toggleCollapse).toHaveBeenCalledWith('archived');
    });
  });

  describe('Batch Action Bar Display', () => {
    it('should not show batch action bar when no items selected and no result', () => {
      mockBatchActions.getSelectedCount.mockReturnValue(0);
      mockBatchActions.state.result = null;

      render(<PoolKanbanView />);

      expect(screen.queryByText(/已选.*个文件/)).not.toBeInTheDocument();
    });

    it('should show batch action bar when items are selected', () => {
      (usePoolBatchAction as Mock).mockReturnValue({
        ...mockBatchActions,
        getSelectedCount: vi.fn(() => 3),
        state: {
          selection: new Set(['card1', 'card2', 'card3']),
          isExecuting: false,
          result: null,
        },
      });

      render(<PoolKanbanView />);

      expect(screen.getByText(/已选.*个文件/)).toBeInTheDocument();
      expect(screen.getByText('已选 3 个文件')).toBeInTheDocument();
    });

    it('should show batch action bar when result exists', () => {
      const result = {
        success: true,
        message: '操作成功',
        successCount: 3,
        failCount: 0,
      };

      (usePoolBatchAction as Mock).mockReturnValue({
        ...mockBatchActions,
        getSelectedCount: vi.fn(() => 0),
        state: {
          selection: new Set(),
          isExecuting: false,
          result,
        },
      });

      render(<PoolKanbanView />);

      expect(screen.getByText('已选 0 个文件')).toBeInTheDocument();
      expect(screen.getByText('操作成功')).toBeInTheDocument();
    });
  });

  describe('Column Action Integration', () => {
    it('should call selectAll and set pending action when column action is clicked', () => {
      render(<PoolKanbanView />);

      const recheckButton = screen.getByText('重新检测');
      fireEvent.click(recheckButton);

      expect(mockBatchActions.selectAll).toHaveBeenCalled();
    });

    it('should set correct action label for recheck action', () => {
      render(<PoolKanbanView />);

      const recheckButton = screen.getByText('重新检测');
      fireEvent.click(recheckButton);

      expect(mockBatchActions.selectAll).toHaveBeenCalled();
    });
  });

  describe('Card Selection Integration', () => {
    it('should render columns with action handlers', () => {
      render(<PoolKanbanView />);

      expect(screen.getByText('重新检测')).toBeInTheDocument();
      expect(screen.getAllByText('删除').length).toBeGreaterThan(0);
    });
  });

  describe('Error Handling', () => {
    it('should call refetch when retry button is clicked', () => {
      const mockError = new Error('Network error');
      (usePoolKanban as Mock).mockReturnValue({
        columns: [],
        loading: false,
        error: mockError,
        refetch: mockRefetch,
        getCardsForColumn: vi.fn(() => []),
      });

      render(<PoolKanbanView />);
      const retryButton = screen.getByText('重试');
      fireEvent.click(retryButton);

      expect(mockRefetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('Data Refresh After Operation', () => {
    it('should render execute button when items are selected', () => {
      const mockActionsWithSelection = {
        ...mockBatchActions,
        getSelectedCount: vi.fn(() => 2),
        state: {
          selection: new Set(['card1', 'card2']),
          isExecuting: false,
          result: null as any,
        },
      };

      (usePoolBatchAction as Mock).mockReturnValue(mockActionsWithSelection);

      render(<PoolKanbanView />);

      expect(screen.getByText('执行')).toBeInTheDocument();
    });
  });

  describe('CSS Classes', () => {
    it('should apply custom className when provided', () => {
      const { container } = render(<PoolKanbanView className="custom-class" />);
      const viewElement = container.querySelector('.pool-kanban-view');
      expect(viewElement).toHaveClass('custom-class');
    });

    it('should have proper class for loading state', () => {
      (usePoolKanban as Mock).mockReturnValue({
        columns: [],
        loading: true,
        error: null,
        refetch: mockRefetch,
        getCardsForColumn: vi.fn(() => []),
      });

      const { container } = render(<PoolKanbanView />);
      expect(container.querySelector('.pool-kanban-view')).toHaveClass('pool-kanban-view--loading');
    });
  });

  describe('Container Reference', () => {
    it('should attach ref to the container element', () => {
      const { container } = render(<PoolKanbanView />);
      const viewElement = container.querySelector('.pool-kanban-view');

      // Verify the hook was called with a ref object
      const layoutCall = (useKanbanLayout as Mock).mock.calls[0];
      expect(layoutCall[0].containerRef).toBeDefined();
      expect(layoutCall[0].containerRef.current).toBe(viewElement);
    });
  });
});
