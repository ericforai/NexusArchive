// src/components/pool-kanban/KanbanCard.test.tsx
// Input: Test cases for KanbanCard component including column-level actions
// Output: Vitest test suite validating component behavior with dynamic actions
// Pos: src/components/pool-kanban/KanbanCard.test.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanCard } from './KanbanCard';
import type { ColumnAction } from '@/config/pool-columns.config';

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  FileText: ({ size, children }: any) => <span data-icon="FileText">{children}</span>,
  Calendar: ({ size, children }: any) => <span data-icon="Calendar">{children}</span>,
  DollarSign: ({ size, children }: any) => <span data-icon="DollarSign">{children}</span>,
  Building: ({ size, children }: any) => <span data-icon="Building">{children}</span>,
}));

// Mock Ant Design components to use actual components for testing
vi.mock('antd', async () => {
  const ActualAntd = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...ActualAntd,
    // Use actual Button and Badge for better testing
    Button: ActualAntd.Button,
    Badge: ActualAntd.Badge,
  };
});

// Mock pool-columns.config
vi.mock('@/config/pool-columns.config', () => ({
  getSubStateLabel: (status: string) => {
    const labels: Record<string, string> = {
      DRAFT: '草稿',
      PENDING_CHECK: '待检测',
      CHECK_FAILED: '检测失败',
      PENDING_METADATA: '待补录',
      MATCH_PENDING: '待匹配',
      MATCHED: '已匹配',
      PENDING_ARCHIVE: '待归档',
      PENDING_APPROVAL: '审批中',
      ARCHIVING: '归档中',
      ARCHIVED: '已归档',
    };
    return labels[status] || status;
  },
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

  describe('basic rendering', () => {
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

    it('should hide amount when not available', () => {
      const cardWithoutAmount = { ...mockCard, amount: '' };
      render(<KanbanCard card={cardWithoutAmount as any} selected={false} />);

      expect(screen.queryByText(/¥/)).not.toBeInTheDocument();
    });

    it('should render without summary', () => {
      const cardWithoutSummary = { ...mockCard, summary: '' };
      render(<KanbanCard card={cardWithoutSummary as any} selected={false} />);

      // 当 summary 为空时，标题显示 code，文件名仍显示 fileName
      expect(screen.getByText('V001')).toBeInTheDocument(); // title
      expect(screen.getByText('test.pdf')).toBeInTheDocument(); // fileName
    });
  });

  describe('selection', () => {
    it('should call onSelect when selection area is clicked', () => {
      const onSelect = vi.fn();
      const { container } = render(
        <KanbanCard card={mockCard as any} selected={false} onSelect={onSelect} />
      );

      const selectArea = container.querySelector('.kanban-card__select-area');
      fireEvent.click(selectArea!);

      expect(onSelect).toHaveBeenCalledWith('test-id');
    });

    it('should show cancel button when selected', () => {
      render(<KanbanCard card={mockCard as any} selected={true} onSelect={vi.fn()} />);

      expect(screen.getByLabelText('取消选择')).toBeInTheDocument();
    });

    it('should call onSelect when cancel button is clicked', () => {
      const onSelect = vi.fn();
      render(<KanbanCard card={mockCard as any} selected={true} onSelect={onSelect} />);

      const cancelButton = screen.getByLabelText('取消选择');
      fireEvent.click(cancelButton);

      expect(onSelect).toHaveBeenCalledWith('test-id');
    });
  });

  describe('basic actions', () => {
    it('should render basic action buttons (view, edit, delete)', () => {
      render(<KanbanCard card={mockCard as any} selected={false} onAction={vi.fn()} />);

      expect(screen.getByRole('button', { name: '查看' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '编辑' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '删除' })).toBeInTheDocument();
    });

    it('should call onAction with view when view button is clicked', () => {
      const onAction = vi.fn();
      render(<KanbanCard card={mockCard as any} selected={false} onAction={onAction} />);

      const viewButton = screen.getByRole('button', { name: '查看' });
      fireEvent.click(viewButton);

      expect(onAction).toHaveBeenCalledWith('test-id', 'view');
    });

    it('should call onAction with edit when edit button is clicked', () => {
      const onAction = vi.fn();
      render(<KanbanCard card={mockCard as any} selected={false} onAction={onAction} />);

      const editButton = screen.getByRole('button', { name: '编辑' });
      fireEvent.click(editButton);

      expect(onAction).toHaveBeenCalledWith('test-id', 'edit');
    });

    it('should call onAction with delete when delete button is clicked', () => {
      const onAction = vi.fn();
      render(<KanbanCard card={mockCard as any} selected={false} onAction={onAction} />);

      const deleteButton = screen.getByRole('button', { name: '删除' });
      fireEvent.click(deleteButton);

      expect(onAction).toHaveBeenCalledWith('test-id', 'delete');
    });
  });

  describe('column-level actions', () => {
    const mockColumnActions: ColumnAction[] = [
      { key: 'recheck', label: '重新检测' },
      { key: 'edit-metadata', label: '编辑元数据' },
      { key: 'smart-match', label: '智能匹配' },
    ];

    it('should render column actions when columnActions prop is provided', () => {
      render(
        <KanbanCard
          card={mockCard as any}
          selected={false}
          onAction={vi.fn()}
          columnActions={mockColumnActions}
        />
      );

      // Basic actions
      expect(screen.getByRole('button', { name: '查看' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '编辑' })).toBeInTheDocument();

      // Column actions (excluding delete)
      expect(screen.getByRole('button', { name: '重新检测' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '编辑元数据' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '智能匹配' })).toBeInTheDocument();

      // Delete button should always be present
      expect(screen.getByRole('button', { name: '删除' })).toBeInTheDocument();
    });

    it('should call onAction with column action key when column action button is clicked', () => {
      const onAction = vi.fn();
      render(
        <KanbanCard
          card={mockCard as any}
          selected={false}
          onAction={onAction}
          columnActions={mockColumnActions}
        />
      );

      const recheckButton = screen.getByRole('button', { name: '重新检测' });
      fireEvent.click(recheckButton);

      expect(onAction).toHaveBeenCalledWith('test-id', 'recheck');
    });

    it('should render danger column actions with danger prop', () => {
      const dangerActions: ColumnAction[] = [
        { key: 'cancel-archive', label: '取消归档', danger: true },
      ];

      const { container } = render(
        <KanbanCard
          card={mockCard as any}
          selected={false}
          onAction={vi.fn()}
          columnActions={dangerActions}
        />
      );

      const dangerButton = screen.getByRole('button', { name: '取消归档' });
      expect(dangerButton).toHaveClass('ant-btn-dangerous');
    });

    it('should exclude delete action from columnActions (delete handled separately)', () => {
      const actionsWithDelete: ColumnAction[] = [
        { key: 'recheck', label: '重新检测' },
        { key: 'delete', label: '删除', danger: true },
      ];

      render(
        <KanbanCard
          card={mockCard as any}
          selected={false}
          onAction={vi.fn()}
          columnActions={actionsWithDelete}
        />
      );

      // Delete should only appear once (from the always-rendered delete button)
      const deleteButtons = screen.getAllByRole('button', { name: '删除' });
      expect(deleteButtons.length).toBe(1);
    });

    it('should work without columnActions prop (backward compatibility)', () => {
      render(<KanbanCard card={mockCard as any} selected={false} onAction={vi.fn()} />);

      // Should only have basic actions
      expect(screen.getByRole('button', { name: '查看' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '编辑' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '删除' })).toBeInTheDocument();

      // Should not have column-specific actions
      expect(screen.queryByRole('button', { name: '重新检测' })).not.toBeInTheDocument();
    });
  });

  describe('conditional rendering', () => {
    it('should not render docDate when not provided', () => {
      const cardWithoutDate = { ...mockCard, docDate: undefined, date: undefined };
      render(<KanbanCard card={cardWithoutDate as any} selected={false} />);

      expect(screen.queryByText('2025-01-09')).not.toBeInTheDocument();
    });

    it('should not render source when not provided', () => {
      const cardWithoutSource = { ...mockCard, source: undefined };
      const { container } = render(
        <KanbanCard card={cardWithoutSource as any} selected={false} />
      );

      expect(container.textContent).not.toContain('测试公司');
    });
  });
});
