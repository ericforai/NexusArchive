// src/components/pool-kanban/__tests__/BatchActionBar.test.tsx
// Input: Test cases for BatchActionBar component
// Output: Vitest test suite validating component behavior and rendering
// Pos: src/components/pool-kanban/__tests__/BatchActionBar.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BatchActionBar } from '../BatchActionBar';
import type { BatchActionResult } from '@/hooks/usePoolBatchAction';

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  CheckCircle2: ({ children }: any) => <span data-icon="CheckCircle2">{children}</span>,
  XCircle: ({ children }: any) => <span data-icon="XCircle">{children}</span>,
  Loader2: ({ children }: any) => <span data-icon="Loader2">{children}</span>,
}));

describe('BatchActionBar', () => {
  const mockOnExecute = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    mockOnExecute.mockClear();
    mockOnCancel.mockClear();
  });

  /**
   * Helper function to find text content that may be split across elements
   */
  const getTextContent = (container: HTMLElement, selector: string): string => {
    const element = container.querySelector(selector);
    return element?.textContent || '';
  };

  describe('Rendering', () => {
    it('should render null when no items are selected and no result', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      expect(container.firstChild).toBe(null);
    });

    it('should render selection count when items are selected', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={5}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const countText = getTextContent(container, '.batch-action-bar__count');
      expect(countText).toContain('已选');
      expect(countText).toContain('5');
      expect(countText).toContain('个文件');
    });

    it('should render result message when operation has result', () => {
      const successResult: BatchActionResult = {
        success: true,
        message: '操作成功 5 条',
        successCount: 5,
        failCount: 0,
      };

      render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={successResult}
        />
      );

      expect(screen.getByText('操作成功 5 条')).toBeInTheDocument();
    });

    it('should display custom action label', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={3}
          actionLabel="执行重新检测"
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const buttons = Array.from(container.querySelectorAll('button'));
      const executeButton = buttons.find(btn => btn.textContent?.includes('执行重新检测'));
      expect(executeButton).toBeInTheDocument();
    });

    it('should show loading state when executing', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={3}
          actionLabel="执行删除"
          isExecuting={true}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const buttons = Array.from(container.querySelectorAll('button'));
      const loadingButton = buttons.find(btn => btn.textContent?.includes('执行中'));
      expect(loadingButton).toBeInTheDocument();
    });
  });

  describe('Hide/Show Logic', () => {
    it('should be hidden when selectedCount is 0 and no result', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      expect(container.querySelector('.batch-action-bar')).not.toBeInTheDocument();
    });

    it('should be visible when selectedCount > 0', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={1}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      expect(container.querySelector('.batch-action-bar')).toBeInTheDocument();
    });

    it('should remain visible with result even when selectedCount is 0', () => {
      const result: BatchActionResult = {
        success: true,
        message: '完成',
        successCount: 1,
        failCount: 0,
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={result}
        />
      );

      expect(container.querySelector('.batch-action-bar')).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('should call onExecute when execute button is clicked', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={3}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      // Find the execute button by class - Ant Design Button with type="primary"
      const executeButton = container.querySelector('.ant-btn-primary');
      expect(executeButton).toBeInTheDocument();
      fireEvent.click(executeButton!);

      expect(mockOnExecute).toHaveBeenCalledTimes(1);
    });

    it('should call onCancel when cancel button is clicked', () => {
      render(
        <BatchActionBar
          selectedCount={3}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const cancelButton = screen.getByRole('button', { name: '取消选择' });
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should call onCancel when close button is clicked after result', () => {
      const result: BatchActionResult = {
        success: true,
        message: '完成',
        successCount: 1,
        failCount: 0,
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={result}
        />
      );

      // Find any button - after result there's only one "close" button
      const closeButton = container.querySelector('button');
      expect(closeButton).toBeInTheDocument();
      fireEvent.click(closeButton!);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should disable cancel button during execution', () => {
      render(
        <BatchActionBar
          selectedCount={3}
          isExecuting={true}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const cancelButton = screen.getByRole('button', { name: '取消选择' });
      expect(cancelButton).toBeDisabled();
    });
  });

  describe('Result Display', () => {
    it('should show success result with green styling', () => {
      const successResult: BatchActionResult = {
        success: true,
        message: '重新检测成功 5 条',
        successCount: 5,
        failCount: 0,
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={successResult}
        />
      );

      const resultElement = container.querySelector('.batch-action-bar__result--success');
      expect(resultElement).toBeInTheDocument();
    });

    it('should show error result with red styling', () => {
      const errorResult: BatchActionResult = {
        success: false,
        message: '删除失败 2 条',
        successCount: 0,
        failCount: 2,
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={errorResult}
        />
      );

      const resultElement = container.querySelector('.batch-action-bar__result--error');
      expect(resultElement).toBeInTheDocument();
    });

    it('should show error details when errors are present', () => {
      const errorResult: BatchActionResult = {
        success: false,
        message: '部分失败',
        successCount: 1,
        failCount: 2,
        errors: [
          { id: 'file1', error: '文件不存在' },
          { id: 'file2', error: '权限不足' },
        ],
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={errorResult}
        />
      );

      // Check error list container exists
      const errorList = container.querySelector('.batch-action-bar__error-list');
      expect(errorList).toBeInTheDocument();

      // Check individual errors are in the text content
      const errorListText = errorList?.textContent || '';
      expect(errorListText).toContain('file1');
      expect(errorListText).toContain('文件不存在');
      expect(errorListText).toContain('file2');
      expect(errorListText).toContain('权限不足');
    });

    it('should limit displayed errors to 3 items', () => {
      const errorResult: BatchActionResult = {
        success: false,
        message: '部分失败',
        successCount: 0,
        failCount: 5,
        errors: [
          { id: 'file1', error: '错误1' },
          { id: 'file2', error: '错误2' },
          { id: 'file3', error: '错误3' },
          { id: 'file4', error: '错误4' },
          { id: 'file5', error: '错误5' },
        ],
      };

      const { container } = render(
        <BatchActionBar
          selectedCount={0}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
          result={errorResult}
        />
      );

      const errorList = container.querySelector('.batch-action-bar__error-list');
      const errorListText = errorList?.textContent || '';
      expect(errorListText).toContain('还有 2 个错误');
    });
  });

  describe('CSS Classes and Styling', () => {
    it('should apply correct CSS class for fixed bottom bar', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={1}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const bar = container.querySelector('.batch-action-bar');
      expect(bar).toBeInTheDocument();
    });

    it('should apply selected class styling', () => {
      const { container } = render(
        <BatchActionBar
          selectedCount={5}
          isExecuting={false}
          onExecute={mockOnExecute}
          onCancel={mockOnCancel}
        />
      );

      const countText = container.querySelector('.batch-action-bar__count');
      expect(countText).toBeInTheDocument();
    });
  });
});
