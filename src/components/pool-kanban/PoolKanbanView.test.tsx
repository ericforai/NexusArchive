// src/components/pool-kanban/PoolKanbanView.test.tsx
// Input: Test cases for PoolKanbanView component
// Output: Vitest test suite validating component behavior
// Pos: src/components/pool-kanban/PoolKanbanView.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PoolKanbanView } from './PoolKanbanView';
import { usePoolKanban } from '@/hooks/usePoolKanban';

// Mock hooks
vi.mock('@/hooks/usePoolKanban');
vi.mock('./KanbanColumn', () => ({
  KanbanColumn: ({ column, cards, selectedIds, onSelectionChange }: any) => (
    <div className="kanban-column" data-column-id={column.id}>
      <h3>{column.title}</h3>
      <span className="card-count">Count: {cards.length}</span>
      {cards.length > 0 && (
        <button
          className="select-card-btn"
          data-testid={`select-${column.id}`}
          onClick={() => cards.forEach((c: any) => onSelectionChange(c.id))}
        >
          Select Card
        </button>
      )}
    </div>
  ),
}));

const mockUsePoolKanban = usePoolKanban as unknown as {
  mockReturnValue: (value: any) => void;
};

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

describe('PoolKanbanView', () => {
  const defaultMockReturnValue = {
    columns: [
      {
        id: 'pending',
        title: '待处理',
        subStates: [{ value: 'DRAFT', label: '草稿' }],
        actions: [{ key: 'recheck', label: '重新检测' }],
      },
      {
        id: 'needs-attention',
        title: '需要处理',
        subStates: [{ value: 'CHECK_FAILED', label: '检测失败' }],
        actions: [{ key: 'edit-metadata', label: '编辑元数据' }],
      },
      {
        id: 'ready',
        title: '准备就绪',
        subStates: [{ value: 'MATCHED', label: '已匹配' }],
        actions: [{ key: 'smart-match', label: '智能匹配' }],
      },
      {
        id: 'processing',
        title: '处理中',
        subStates: [{ value: 'ARCHIVED', label: '已归档' }],
        actions: [{ key: 'view-detail', label: '查看详情' }],
      },
    ],
    cards: [
      { id: '1', status: 'DRAFT', summary: 'Card 1', code: 'C001' },
      { id: '2', status: 'DRAFT', summary: 'Card 2', code: 'C002' },
    ],
    loading: false,
    error: null,
    refetch: vi.fn(),
    getCardsForColumn: vi.fn((columnId: string) => {
      if (columnId === 'pending') {
        return [
          { id: '1', status: 'DRAFT', summary: 'Card 1', code: 'C001' },
          { id: '2', status: 'DRAFT', summary: 'Card 2', code: 'C002' },
        ];
      }
      return [];
    }),
    getSubStateCount: vi.fn(() => 1),
    getTotalCount: vi.fn(() => 2),
  };

  beforeEach(() => {
    mockUsePoolKanban.mockReturnValue(defaultMockReturnValue);
  });

  it('should render kanban board with title', () => {
    const wrapper = createWrapper();
    render(<PoolKanbanView />, { wrapper });

    expect(screen.getByText('电子凭证池')).toBeInTheDocument();
  });

  it('should render all four columns', () => {
    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    const columns = container.querySelectorAll('.kanban-column');
    expect(columns).toHaveLength(4);
  });

  it('should render column titles', () => {
    const wrapper = createWrapper();
    render(<PoolKanbanView />, { wrapper });

    expect(screen.getByText('待处理')).toBeInTheDocument();
    expect(screen.getByText('需要处理')).toBeInTheDocument();
    expect(screen.getByText('准备就绪')).toBeInTheDocument();
    expect(screen.getByText('处理中')).toBeInTheDocument();
  });

  it('should show loading state when loading', () => {
    mockUsePoolKanban.mockReturnValue({
      ...defaultMockReturnValue,
      loading: true,
    });

    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    expect(container.querySelector('.pool-kanban-view--loading')).toBeInTheDocument();
  });

  it('should show error state when error occurs', () => {
    const mockRefetch = vi.fn();
    mockUsePoolKanban.mockReturnValue({
      columns: [],
      cards: [],
      loading: false,
      error: new Error('Network error'),
      refetch: mockRefetch,
      getCardsForColumn: vi.fn(() => []),
      getSubStateCount: vi.fn(() => 0),
      getTotalCount: vi.fn(() => 0),
    });

    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    expect(container.querySelector('.pool-kanban-view--error')).toBeInTheDocument();
    expect(screen.getByText('加载失败: Network error')).toBeInTheDocument();
  });

  it('should render batch bar when cards are selected', async () => {
    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    // Initially no batch bar
    expect(container.querySelector('.pool-kanban-view__batch-bar')).not.toBeInTheDocument();

    // Click to select a card using testid
    const selectButton = screen.getByTestId('select-pending');
    fireEvent.click(selectButton);

    // Batch bar should appear
    await waitFor(() => {
      expect(container.querySelector('.pool-kanban-view__batch-bar')).toBeInTheDocument();
    });
  });

  it('should show selected count in batch bar', async () => {
    const wrapper = createWrapper();
    render(<PoolKanbanView />, { wrapper });

    // Click to select a card
    const selectButton = screen.getByTestId('select-pending');
    fireEvent.click(selectButton);

    await waitFor(() => {
      expect(screen.getByText('已选 2 个文件')).toBeInTheDocument();
    });
  });

  it('should clear selection when cancel button is clicked', async () => {
    const wrapper = createWrapper();
    const { container } = render(<PoolKanbanView />, { wrapper });

    // Click to select cards
    const selectButton = screen.getByTestId('select-pending');
    fireEvent.click(selectButton);

    await waitFor(() => {
      expect(container.querySelector('.pool-kanban-view__batch-bar')).toBeInTheDocument();
    });

    // Click cancel button
    const cancelButton = screen.getByText('取消选择');
    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(container.querySelector('.pool-kanban-view__batch-bar')).not.toBeInTheDocument();
    });
  });

  it('should call refetch when retry button is clicked in error state', () => {
    const mockRefetch = vi.fn();
    mockUsePoolKanban.mockReturnValue({
      columns: [],
      cards: [],
      loading: false,
      error: new Error('Network error'),
      refetch: mockRefetch,
      getCardsForColumn: vi.fn(() => []),
      getSubStateCount: vi.fn(() => 0),
      getTotalCount: vi.fn(() => 0),
    });

    const wrapper = createWrapper();
    render(<PoolKanbanView />, { wrapper });

    const retryButton = screen.getByText('重试');
    fireEvent.click(retryButton);

    expect(mockRefetch).toHaveBeenCalled();
  });
});
