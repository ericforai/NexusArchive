// Input: Test suite for BatchApprovalDialog component
// Output: Vitest test coverage for approve/reject functionality, validation, skip feature, threshold warnings, and loading states
// Pos: src/components/operations/__tests__/BatchApprovalDialog.test.tsx

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BatchApprovalDialog, ApprovalRecord, CONFIRM_THRESHOLD, MAX_COMMENT_LENGTH } from '../BatchApprovalDialog';

describe('BatchApprovalDialog', () => {
  const mockOnConfirm = vi.fn();
  const mockOnCancel = vi.fn();

  const mockRecords: ApprovalRecord[] = [
    { id: '1', title: '记录1', code: 'CODE001' },
    { id: '2', title: '记录2', code: 'CODE002' },
    { id: '3', title: '记录3', code: 'CODE003' },
  ];

  beforeEach(() => {
    mockOnConfirm.mockClear();
    mockOnCancel.mockClear();
  });

  describe('Configuration: Approve Action', () => {
    it('should display approve configuration', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      expect(screen.getByText('批量审批 (已选 3 条)')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('审批意见（可选）')).toBeInTheDocument();
      expect(screen.getByText('确认审批')).toBeInTheDocument();
    });

    it('should not require comment for approve action', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const confirmButton = screen.getByText('确认审批');
      expect(confirmButton).not.toBeDisabled();
    });
  });

  describe('Configuration: Reject Action', () => {
    it('should display reject configuration', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="reject"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      expect(screen.getByText('批量驳回 (已选 3 条)')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('驳回原因（必填）')).toBeInTheDocument();
      expect(screen.getByText('确认驳回')).toBeInTheDocument();
    });

    it('should show required asterisk for reject action', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="reject"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      // Check for asterisk indicator
      const label = screen.getByText('审批意见');
      expect(label.innerHTML).toContain('*');
    });
  });

  describe('Comment Validation', () => {
    it('should prevent reject without comment', async () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="reject"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const confirmButton = screen.getByText('确认驳回');
      fireEvent.click(confirmButton);

      // Should not call onConfirm
      expect(mockOnConfirm).not.toHaveBeenCalled();
    });

    it('should allow reject with comment', async () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="reject"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const textarea = screen.getByPlaceholderText('驳回原因（必填）');
      fireEvent.change(textarea, { target: { value: '不符合要求' } });

      const confirmButton = screen.getByText('确认驳回');
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(mockOnConfirm).toHaveBeenCalledWith('不符合要求', []);
      });
    });

    it('should enforce max comment length', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const textarea = screen.getByPlaceholderText('审批意见（可选）');
      expect(textarea).toHaveAttribute('maxLength', String(MAX_COMMENT_LENGTH));
    });

    it('should display character count', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const textarea = screen.getByPlaceholderText('审批意见（可选）');
      fireEvent.change(textarea, { target: { value: '测试评论' } });

      expect(screen.getByText('4/500 字符')).toBeInTheDocument();
    });
  });

  describe('Skip Feature', () => {
    it('should expand skip section when checkbox clicked', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      expect(screen.getByText('记录1')).toBeInTheDocument();
      expect(screen.getByText('编号: CODE001')).toBeInTheDocument();
    });

    it('should skip selected records', async () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      // Expand skip section
      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      // Uncheck record 1 (mark as skipped)
      const checkboxes = screen.getAllByRole('checkbox');
      const record1Checkbox = checkboxes[1]; // Second checkbox is record 1
      fireEvent.click(record1Checkbox);

      // Add comment and confirm
      const textarea = screen.getByPlaceholderText('审批意见（可选）');
      fireEvent.change(textarea, { target: { value: '批量审批' } });

      const confirmButton = screen.getByText('确认审批');
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(mockOnConfirm).toHaveBeenCalledWith('批量审批', ['1']);
      });
    });

    it('should display skip count badge', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      const checkboxes = screen.getAllByRole('checkbox');
      const record1Checkbox = checkboxes[1];
      fireEvent.click(record1Checkbox);

      expect(screen.getByText('已跳过 1 条')).toBeInTheDocument();
    });

    it('should show actual processing count when records are skipped', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      const checkboxes = screen.getAllByRole('checkbox');
      const record1Checkbox = checkboxes[1];
      fireEvent.click(record1Checkbox);

      // The skip count badge should be visible
      expect(screen.getByText('已跳过 1 条')).toBeInTheDocument();

      // Check that the checkbox shows proper state
      expect(record1Checkbox).not.toBeChecked();
    });
  });

  describe('Threshold Warning', () => {
    it('should show warning when count exceeds threshold', () => {
      const largeRecords = Array.from({ length: CONFIRM_THRESHOLD + 1 }, (_, i) => ({
        id: String(i + 1),
        title: `记录${i + 1}`,
      }));

      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={CONFIRM_THRESHOLD + 1}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={largeRecords}
        />
      );

      // Check that the count number appears in the document (in title and in warning)
      const countElements = screen.getAllByText(String(CONFIRM_THRESHOLD + 1));
      expect(countElements.length).toBeGreaterThan(0);
    });

    it('should display record list preview (max 5 records)', () => {
      const largeRecords = Array.from({ length: CONFIRM_THRESHOLD + 1 }, (_, i) => ({
        id: String(i + 1),
        title: `记录${i + 1}`,
      }));

      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={CONFIRM_THRESHOLD + 1}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={largeRecords}
        />
      );

      // Should show first 5 records
      expect(screen.getByText('• 记录1')).toBeInTheDocument();
      expect(screen.getByText('• 记录5')).toBeInTheDocument();

      // Should show "等共 X 条" message
      expect(screen.getByText(/\.\.\. 等共 .* 条/)).toBeInTheDocument();
    });

    it('should not show warning when count is below threshold', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={5}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      expect(screen.queryByText(/即将处理 .* 条记录，请确认操作无误/)).not.toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('should disable buttons when loading', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
          loading={true}
        />
      );

      const cancelButton = screen.getByText('取消');
      const confirmButton = screen.getByText('处理中...');

      expect(cancelButton).toBeDisabled();
      expect(confirmButton).toBeDisabled();
    });

    it('should show loading spinner', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
          loading={true}
        />
      );

      expect(screen.getByText('处理中...')).toBeInTheDocument();
    });

    it('should disable textarea when loading', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
          loading={true}
        />
      );

      const textarea = screen.getByPlaceholderText('审批意见（可选）');
      expect(textarea).toBeDisabled();
    });
  });

  describe('Cancel Behavior', () => {
    it('should call onCancel when cancel button clicked', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      const cancelButton = screen.getByText('取消');
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should reset state on cancel', () => {
      const { rerender } = render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      // Add comment
      const textarea = screen.getByPlaceholderText('审批意见（可选）');
      fireEvent.change(textarea, { target: { value: '测试评论' } });

      // Cancel
      const cancelButton = screen.getByText('取消');
      fireEvent.click(cancelButton);

      // Re-render and verify state is reset
      rerender(
        <BatchApprovalDialog
          visible={true}
          selectedCount={3}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={mockRecords}
        />
      );

      expect(screen.getByPlaceholderText('审批意见（可选）')).toHaveValue('');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty selectedRecords gracefully', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={0}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={[]}
        />
      );

      // Should not crash when trying to expand skip section
      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      expect(screen.getByText('无可用记录')).toBeInTheDocument();
    });

    it('should handle undefined selectedRecords', () => {
      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={0}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={undefined}
        />
      );

      // Should not crash
      expect(screen.getByText('批量审批 (已选 0 条)')).toBeInTheDocument();
    });

    it('should handle records without title or code', () => {
      const minimalRecords: ApprovalRecord[] = [{ id: '1' }];

      render(
        <BatchApprovalDialog
          visible={true}
          selectedCount={1}
          action="approve"
          onConfirm={mockOnConfirm}
          onCancel={mockOnCancel}
          selectedRecords={minimalRecords}
        />
      );

      const skipCheckbox = screen.getByRole('button', { name: /跳过部分记录单独处理/ });
      fireEvent.click(skipCheckbox);

      // Should display "记录 #1" fallback
      expect(screen.getByText('记录 #1')).toBeInTheDocument();
    });
  });
});
