// src/components/pool-kanban/KanbanColumn.test.tsx
// Input: Test cases for KanbanColumn component
// Output: Vitest test suite validating component behavior
// Pos: src/components/pool-kanban/KanbanColumn.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanColumn } from './KanbanColumn';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';

// Mock lucide-react
vi.mock('lucide-react', () => ({
  MoreHorizontal: ({ size }: any) => <span data-icon="MoreHorizontal" size={size}>›</span>,
}));

// Track whether columnActions prop was passed to KanbanCard
let capturedColumnActions: any = null;

// Mock KanbanCard
vi.mock('./KanbanCard', () => ({
  KanbanCard: ({ card, selected, onSelect, onAction, columnActions }: any) => {
    // Capture columnActions for testing
    capturedColumnActions = columnActions;
    return (
      <div className="kanban-card" data-card-id={card.id}>
        <span className="card-title">{card.summary || card.code}</span>
        <span className="card-status">{card.status}</span>
        <button onClick={() => onSelect?.(card.id)}>select</button>
        <button onClick={() => onAction?.(card.id, 'view')}>view</button>
        {columnActions && (
          <div className="card-column-actions">
            {columnActions.map((a: any) => (
              <span key={a.key} data-action-key={a.key}>{a.label}</span>
            ))}
          </div>
        )}
      </div>
    );
  },
}));

describe('KanbanColumn', () => {
  const mockColumn = POOL_COLUMN_GROUPS[0];
  const mockCards = [
    { id: '1', status: 'DRAFT', summary: 'Card 1', code: 'C001' },
    { id: '2', status: 'DRAFT', summary: 'Card 2', code: 'C002' },
  ] as any[];

  it('should render column header with title', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('待处理')).toBeInTheDocument();
  });

  it('should render sub-state tabs using Segmented', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('草稿')).toBeInTheDocument();
    expect(screen.getByText('待检测')).toBeInTheDocument();
  });

  it('should show card count in tabs', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // DRAFT has 2 cards - check that badge with count 2 exists
    const draftBadges = container.querySelectorAll('.ant-badge-count');
    const draftBadge = Array.from(draftBadges).find(b => b.textContent?.trim() === '2');
    expect(draftBadge).toBeDefined();

    // Verify we have badges in the tabs (one for DRAFT with count 2)
    expect(draftBadges.length).toBeGreaterThan(0);
  });

  it('should filter cards by selected sub-state', () => {
    const mixedCards = [
      ...mockCards, // DRAFT
      { id: '3', status: 'PENDING_CHECK', summary: 'Card 3', code: 'C003' },
    ] as any[];

    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mixedCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Should only show DRAFT cards by default
    const cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);
  });

  it('should switch sub-state when tab is clicked', () => {
    const cardsWithPending = [
      ...mockCards,
      { id: '3', status: 'PENDING_CHECK', summary: 'Card 3', code: 'C003' },
    ] as any[];

    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={cardsWithPending}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Initially showing DRAFT (2 cards)
    let cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);

    // Click on PENDING_CHECK tab
    const pendingTab = screen.getByText(/待检测/);
    fireEvent.click(pendingTab);

    // Now showing PENDING_CHECK (1 card)
    cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(1);
  });

  it('should pass column actions to cards', () => {
    render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Verify columnActions were passed to KanbanCard
    expect(capturedColumnActions).toBeDefined();
    expect(capturedColumnActions).toHaveLength(2);
    expect(capturedColumnActions[0].key).toBe('recheck');
    expect(capturedColumnActions[0].label).toBe('重新检测');
    expect(capturedColumnActions[1].key).toBe('delete');
    expect(capturedColumnActions[1].label).toBe('删除');
  });

  it('should show empty state when no cards for selected sub-state', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={[]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(screen.getByText('暂无文件')).toBeInTheDocument();
  });
});
