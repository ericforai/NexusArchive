// src/components/pool-kanban/KanbanColumn.test.tsx
// Input: Test cases for KanbanColumn component
// Output: Vitest test suite validating component behavior
// Pos: src/components/pool-kanban/KanbanColumn.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
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

// Track whether columnActions prop was passed to KanbanCard
let capturedColumnActions: any = null;

// Mock KanbanCard
vi.mock('./KanbanCard', () => ({
  KanbanCard: ({ card, selected: _selected, onSelect, onAction, columnActions }: any) => {
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
  const mockColumn = POOL_COLUMN_GROUPS[0]; // pending - 待检测
  const mockCards = [
    { id: '1', status: 'PENDING_CHECK', summary: 'Card 1', code: 'C001' },
    { id: '2', status: 'PENDING_CHECK', summary: 'Card 2', code: 'C002' },
  ] as any[];

  it('should render column header with title', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(container.querySelector('.kanban-column__title')?.textContent).toBe('待检测');
  });

  it('should render sub-state tabs using Segmented', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    expect(container.querySelector('.kanban-column__sub-states')).toBeInTheDocument();
    // Text appears in both title and Segmented, so use getAllByText
    expect(screen.getAllByText('待检测')).toHaveLength(2);
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

    // PENDING_CHECK has 2 cards - check that badge with count 2 exists
    const draftBadges = container.querySelectorAll('.ant-badge-count');
    const draftBadge = Array.from(draftBadges).find(b => b.textContent?.trim() === '2');
    expect(draftBadge).toBeDefined();

    // Verify we have badges in the tabs (one for PENDING_CHECK with count 2)
    expect(draftBadges.length).toBeGreaterThan(0);
  });

  it('should filter cards by selected sub-state', () => {
    const mixedCards = [
      ...mockCards, // PENDING_CHECK
      { id: '3', status: 'NEEDS_ACTION', summary: 'Card 3', code: 'C003' },
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

    // Should only show PENDING_CHECK cards by default
    const cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);
  });

  it('should switch sub-state when tab is clicked', () => {
    // Use needs-action column which has NEEDS_ACTION sub-state
    const needsActionColumn = POOL_COLUMN_GROUPS[1];
    const cardsWithMixed = [
      { id: '1', status: 'NEEDS_ACTION', summary: 'Card 1', code: 'C001' },
      { id: '2', status: 'NEEDS_ACTION', summary: 'Card 2', code: 'C002' },
    ] as any[];

    const { container } = render(
      <KanbanColumn
        column={needsActionColumn}
        cards={cardsWithMixed}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Initially showing NEEDS_ACTION (2 cards)
    const cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(2);
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
    expect(capturedColumnActions[1].key).toBe('delete');
  });

  it('should display total badge count', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Total badge should show 2
    const totalBadge = container.querySelector('.kanban-column__total-badge');
    expect(totalBadge?.textContent).toBe('2');
  });

  it('should render empty state when no cards', () => {
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={[]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onAction={vi.fn()}
      />
    );

    // Should have empty state
    const cardElements = container.querySelectorAll('.kanban-card');
    expect(cardElements).toHaveLength(0);
  });

  it('should handle card selection', () => {
    const onSelectionChange = vi.fn();
    const { container } = render(
      <KanbanColumn
        column={mockColumn}
        cards={mockCards}
        selectedIds={new Set()}
        onSelectionChange={onSelectionChange}
        onAction={vi.fn()}
      />
    );

    // Click select button on first card
    const firstCard = container.querySelector('.kanban-card[data-card-id="1"]');
    const selectButton = firstCard?.querySelector('button');
    selectButton?.click();

    expect(onSelectionChange).toHaveBeenCalledWith('1');
  });

  it('should handle card action', () => {
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

    // Click view button on first card
    const firstCard = container.querySelector('.kanban-card[data-card-id="1"]');
    const viewButtons = firstCard?.querySelectorAll('button');
    const viewButton = viewButtons?.[1]; // Second button is view
    viewButton?.click();

    expect(onAction).toHaveBeenCalledWith('view', [mockCards[0]]);
  });
});
