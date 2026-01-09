// src/components/pool-kanban/KanbanCard.test.tsx
// Input: Test cases for KanbanCard component
// Output: Vitest test suite validating component behavior
// Pos: src/components/pool-kanban/KanbanCard.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanCard } from './KanbanCard';

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  FileText: ({ size, children }: any) => <span data-icon="FileText">{children}</span>,
  Calendar: ({ size, children }: any) => <span data-icon="Calendar">{children}</span>,
  DollarSign: ({ size, children }: any) => <span data-icon="DollarSign">{children}</span>,
  Building: ({ size, children }: any) => <span data-icon="Building">{children}</span>,
}));

describe('KanbanCard', () => {
  const mockCard = {
    id: 'test-id',
    code: 'V001',
    summary: '测试凭证',
    fileName: 'test.pdf',
    amount: '12345.67',
    date: '2025-01-09',
    docDate: '2025-01-09',
    source: '测试公司',
    status: 'DRAFT',
    type: 'INVOICE',
  };

  it('should render card information', () => {
    render(<KanbanCard card={mockCard as any} selected={false} />);

    expect(screen.getByText('测试凭证')).toBeInTheDocument();
    expect(screen.getByText('test.pdf')).toBeInTheDocument();
    expect(screen.getByText('¥12,345.67')).toBeInTheDocument();
    expect(screen.getByText('2025-01-09')).toBeInTheDocument();
    expect(screen.getByText('测试公司')).toBeInTheDocument();
  });

  it('should show selected state', () => {
    const { container } = render(
      <KanbanCard card={mockCard as any} selected={true} />
    );

    const card = container.querySelector('.kanban-card');
    expect(card).toHaveClass('kanban-card--selected');
  });

  it('should call onSelect when selection area is clicked', () => {
    const onSelect = vi.fn();
    const { container } = render(
      <KanbanCard card={mockCard as any} selected={false} onSelect={onSelect} />
    );

    const selectArea = container.querySelector('.kanban-card__select-area');
    fireEvent.click(selectArea!);

    expect(onSelect).toHaveBeenCalledWith('test-id');
  });

  it('should call onAction when action button is clicked', () => {
    const onAction = vi.fn();
    const { container } = render(
      <KanbanCard card={mockCard as any} selected={false} onAction={onAction} />
    );

    // Use querySelector to find buttons since Ant Design Button wraps text in span elements
    const buttons = container.querySelectorAll('.kanban-card__actions button');
    fireEvent.click(buttons[0]);

    expect(onAction).toHaveBeenCalledWith('test-id', 'view');
  });

  it('should hide amount when not available', () => {
    const cardWithoutAmount = { ...mockCard, amount: '' };
    render(<KanbanCard card={cardWithoutAmount as any} selected={false} />);

    expect(screen.queryByText(/¥/)).not.toBeInTheDocument();
  });
});
