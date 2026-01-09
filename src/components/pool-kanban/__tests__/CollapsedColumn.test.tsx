// src/components/pool-kanban/__tests__/CollapsedColumn.test.tsx
// Input: Test cases for CollapsedColumn component
// Output: Vitest test suite validating component behavior
// Pos: src/components/pool-kanban/__tests__/CollapsedColumn.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CollapsedColumn } from '../CollapsedColumn';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';

// Mock lucide-react
vi.mock('lucide-react', () => ({
  ChevronRight: ({ size }: any) => (
    <svg data-icon="ChevronRight" width={size} height={size}>
      <circle cx="8" cy="8" r="6" />
    </svg>
  ),
}));

describe('CollapsedColumn', () => {
  const mockColumn = POOL_COLUMN_GROUPS[0];
  const mockOnExpand = vi.fn();

  it('should render collapsed column with vertical title', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    expect(container.querySelector('.collapsed-column')).toBeInTheDocument();
    expect(container.querySelector('.collapsed-column__title-text')).toHaveTextContent('待处理');
  });

  it('should display card count badge', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={12} onExpand={mockOnExpand} />
    );

    const countBadge = container.querySelector('.collapsed-column__count-badge');
    expect(countBadge).toHaveTextContent('12');
  });

  it('should display zero card count', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={0} onExpand={mockOnExpand} />
    );

    const countBadge = container.querySelector('.collapsed-column__count-badge');
    expect(countBadge).toHaveTextContent('0');
  });

  it('should call onExpand when expand button is clicked', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const expandButton = container.querySelector('.collapsed-column__expand');
    fireEvent.click(expandButton!);

    expect(mockOnExpand).toHaveBeenCalledTimes(1);
  });

  it('should call onExpand when column is clicked', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const column = container.querySelector('.collapsed-column');
    fireEvent.click(column!);

    expect(mockOnExpand).toHaveBeenCalledTimes(1);
  });

  it('should render expand button with chevron icon', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const icon = container.querySelector('[data-icon="ChevronRight"]');
    expect(icon).toBeInTheDocument();
  });

  it('should have correct CSS classes', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const column = container.querySelector('.collapsed-column');
    expect(column).toHaveClass('collapsed-column');

    expect(container.querySelector('.collapsed-column__title')).toBeInTheDocument();
    expect(container.querySelector('.collapsed-column__count')).toBeInTheDocument();
    expect(container.querySelector('.collapsed-column__expand')).toBeInTheDocument();
    expect(container.querySelector('.collapsed-column__hint')).toBeInTheDocument();
  });

  it('should display hint text on hover', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const hint = container.querySelector('.collapsed-column__hint');
    expect(hint).toHaveTextContent('点击展开');
  });

  it('should have correct aria-label for accessibility', () => {
    const { container } = render(
      <CollapsedColumn column={mockColumn} cardCount={5} onExpand={mockOnExpand} />
    );

    const title = container.querySelector('.collapsed-column__title');
    expect(title).toHaveAttribute('aria-label', '待处理');

    const expandButton = container.querySelector('.collapsed-column__expand');
    expect(expandButton).toHaveAttribute('aria-label', '展开 待处理');
  });

  it('should render different columns correctly', () => {
    const readyColumn = POOL_COLUMN_GROUPS[2]; // 准备就绪
    const { container } = render(
      <CollapsedColumn column={readyColumn} cardCount={23} onExpand={mockOnExpand} />
    );

    expect(container.querySelector('.collapsed-column__title-text')).toHaveTextContent('准备就绪');
    expect(container.querySelector('.collapsed-column__count-badge')).toHaveTextContent('23');
  });
});
