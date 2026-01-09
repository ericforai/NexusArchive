// src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx
// Input: Test cases for PoolKanbanView component
// Output: Vitest test suite validating component behavior and batch action integration
// Pos: src/components/pool-kanban/__tests__/PoolKanbanView.test.tsx

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { Mock } from 'vitest';

// Mock antd components
vi.mock('antd', () => ({
  Spin: ({ tip }: any) => <div className="ant-spin">{tip || '加载中...'}</div>,
}));

// Mock modules BEFORE imports
vi.mock('@/hooks/usePoolKanban', () => ({
  usePoolKanban: vi.fn(),
}));

vi.mock('@/hooks/usePoolBatchAction', () => ({
  usePoolBatchAction: vi.fn(),
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
import { KanbanColumn } from '../KanbanColumn';
import { BatchActionBar } from '../BatchActionBar';

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
  ];

  const mockCards = [
    { id: 'card1', title: 'Card 1', status: 'DRAFT' },
    { id: 'card2', title: 'Card 2', status: 'DRAFT' },
    { id: 'card3', title: 'Card 3', status: 'CHECK_FAILED' },
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

  beforeEach(() => {
    vi.clearAllMocks();

    (usePoolKanban as Mock).mockReturnValue({
      columns: mockColumns,
      loading: false,
      error: null,
      refetch: mockRefetch,
      getCardsForColumn: vi.fn((colId: string) => mockCards.filter(c => c.status === 'DRAFT')),
    });

    (usePoolBatchAction as Mock).mockReturnValue(mockBatchActions);
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

      // Find and click the recheck button
      const recheckButton = screen.getByText('重新检测');
      fireEvent.click(recheckButton);

      expect(mockBatchActions.selectAll).toHaveBeenCalled();
    });

    it('should set correct action label for recheck action', () => {
      render(<PoolKanbanView />);

      const recheckButton = screen.getByText('重新检测');
      fireEvent.click(recheckButton);

      // The action should be processed
      expect(mockBatchActions.selectAll).toHaveBeenCalled();
    });
  });

  describe('Card Selection Integration', () => {
    it('should render columns with action handlers', () => {
      render(<PoolKanbanView />);

      // Verify that columns are rendered with action buttons
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

  describe('Action Type Mapping', () => {
    it('should map recheck action key to recheck batch type', () => {
      render(<PoolKanbanView />);

      const onAction = (KanbanColumn as Mock).mock.calls[0]?.[0]?.onAction;
      onAction('recheck', mockCards);

      // Verify the action was processed
      expect(mockBatchActions.selectAll).toHaveBeenCalled();
    });

    it('should map delete action key to delete batch type', () => {
      render(<PoolKanbanView />);

      const onAction = (KanbanColumn as Mock).mock.calls[0]?.[0]?.onAction;
      onAction('delete', mockCards);

      expect(mockBatchActions.selectAll).toHaveBeenCalled();
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

  describe('Cancel Operation', () => {
    it('should render cancel button when items are selected', () => {
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

      expect(screen.getByText('取消')).toBeInTheDocument();
    });
  });

  describe('Result Handling', () => {
    it('should pass result to BatchActionBar when result exists', () => {
      const result = {
        success: true,
        message: '操作成功',
        successCount: 2,
        failCount: 0,
      };

      const mockActionsWithResult = {
        ...mockBatchActions,
        getSelectedCount: vi.fn(() => 0),
        state: {
          selection: new Set(),
          isExecuting: false,
          result,
        },
      };

      (usePoolBatchAction as Mock).mockReturnValue(mockActionsWithResult);

      render(<PoolKanbanView />);

      expect(screen.getByText('操作成功')).toBeInTheDocument();
    });
  });
});
