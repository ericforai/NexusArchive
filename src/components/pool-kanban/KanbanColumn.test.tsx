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
  MoreHorizontal: ({ size }: any) => <span data-icon="MoreHorizontal" data-size={size}>›</span>,
}));

// Mock Ant Design components to use actual components for testing
vi.mock('antd', async () => {
  const ActualAntd = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...ActualAntd,
    // Use actual Tabs, Button, Badge, and Dropdown for better testing
    Tabs: ActualAntd.Tabs,
    Button: ActualAntd.Button,
    Badge: ActualAntd.Badge,
    Dropdown: ActualAntd.Dropdown,
  };
});

// Mock KanbanCard
vi.mock('./KanbanCard', () => ({
  KanbanCard: ({ card, selected, onSelect, onAction }: any) => (
    <div className="kanban-card" data-card-id={card.id}>
      <span className="card-title">{card.summary || card.code}</span>
      <span className="card-status">{card.status}</span>
      <button onClick={() => onSelect?.(card.id)}>select</button>
      <button onClick={() => onAction?.(card.id, 'view')}>view</button>
    </div>
  ),
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

  it('should render sub-state tabs', () => {
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

  it('should render column action buttons', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Find buttons in the actions area
    const actionButtons = container.querySelectorAll('.kanban-column__actions button');
    const buttonTexts = Array.from(actionButtons).map(btn => btn.textContent?.trim().replace(/\s+/g, ''));

    expect(buttonTexts.some(t => t === '重新检测')).toBe(true);
    expect(buttonTexts.some(t => t === '删除')).toBe(true);
  });

  it('should call onAction when column action is clicked', () => {
    const onAction = vi.fn();

    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={onAction}
      />
    );

    const actionButtons = container.querySelectorAll('.kanban-column__actions button');
    const recheckButton = Array.from(actionButtons).find(btn => btn.textContent === '重新检测');
    fireEvent.click(recheckButton!);

    expect(onAction).toHaveBeenCalledWith('recheck', mockCards);
  });
});
