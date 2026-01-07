// Input: Test suite for BatchResultModal component
// Output: Vitest test coverage for result display, error handling, retry functionality, and export report
// Pos: src/components/operations/__tests__/BatchResultModal.test.tsx

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BatchResultModal, BatchResultModalProps } from '../BatchResultModal';

describe('BatchResultModal', () => {
  const defaultProps: BatchResultModalProps = {
    visible: true,
    successCount: 95,
    failedCount: 5,
    errors: [
      { id: 1, reason: '状态不允许审批' },
      { id: 2, reason: '权限不足' },
    ],
    onClose: vi.fn(),
    onRetry: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基础渲染', () => {
    it('应该渲染弹窗', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('批量操作完成')).toBeInTheDocument();
    });

    it('应该显示正确的成功/失败统计', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('95')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
    });

    it('审批操作类型应该显示正确标题', () => {
      render(<BatchResultModal {...defaultProps} operationType="approval" />);
      expect(screen.getByText('批量审批完成')).toBeInTheDocument();
    });

    it('应该显示失败详情列表', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('档案 #1')).toBeInTheDocument();
      expect(screen.getByText('档案 #2')).toBeInTheDocument();
      expect(screen.getByText('状态不允许审批')).toBeInTheDocument();
      expect(screen.getByText('权限不足')).toBeInTheDocument();
    });
  });

  describe('全部成功状态', () => {
    it('应该显示全部成功状态', () => {
      const props = { ...defaultProps, successCount: 100, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('🎉 所有记录均已成功处理完成！')).toBeInTheDocument();
      expect(screen.getByText('所有记录均已成功处理')).toBeInTheDocument();
    });

    it('全部成功时不应该显示重试按钮', () => {
      const props = { ...defaultProps, successCount: 100, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);

      expect(screen.queryByText('重试失败项')).not.toBeInTheDocument();
    });
  });

  describe('全部失败状态', () => {
    it('应该显示全部失败状态', () => {
      const props = { ...defaultProps, successCount: 0, failedCount: 5 };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('所有记录处理失败，请检查失败详情')).toBeInTheDocument();
    });
  });

  describe('部分成功状态', () => {
    it('应该显示部分成功状态', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('部分记录处理成功，请检查失败详情')).toBeInTheDocument();
    });
  });

  describe('交互行为', () => {
    it('点击关闭按钮应该调用 onClose', () => {
      render(<BatchResultModal {...defaultProps} />);
      const closeButton = screen.getByText('关闭');
      fireEvent.click(closeButton);
      expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
    });

    it('点击重试按钮应该调用 onRetry', async () => {
      render(<BatchResultModal {...defaultProps} />);
      const retryButton = screen.getByText('重试失败项');
      fireEvent.click(retryButton);
      await waitFor(() => {
        expect(defaultProps.onRetry).toHaveBeenCalledWith([1, 2]);
      });
    });

    it('重试中状态应该禁用按钮', () => {
      const props = { ...defaultProps, isRetrying: true };
      render(<BatchResultModal {...props} />);

      const closeButton = screen.getByText('关闭');
      const retryButton = screen.getByText('重试中...');

      expect(closeButton).toBeDisabled();
      expect(retryButton).toBeDisabled();
    });
  });

  describe('导出报告功能', () => {
    it('应该显示导出报告按钮', () => {
      const props = {
        ...defaultProps,
        onExportReport: vi.fn(),
      };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('导出报告')).toBeInTheDocument();
    });

    it('点击导出报告应该调用 onExportReport', async () => {
      const onExportReport = vi.fn();
      const props = { ...defaultProps, onExportReport };
      render(<BatchResultModal {...props} />);

      const exportButton = screen.getByText('导出报告');
      fireEvent.click(exportButton);
      await waitFor(() => {
        expect(onExportReport).toHaveBeenCalledTimes(1);
      });
    });

    it('没有失败时不应该显示导出报告按钮', () => {
      const props = {
        ...defaultProps,
        failedCount: 0,
        errors: [],
        onExportReport: vi.fn(),
      };
      render(<BatchResultModal {...props} />);
      expect(screen.queryByText('导出报告')).not.toBeInTheDocument();
    });
  });

  describe('无 onRetry 回调', () => {
    it('不应该显示重试按钮', () => {
      const props = { ...defaultProps, onRetry: undefined };
      render(<BatchResultModal {...props} />);
      expect(screen.queryByText('重试失败项')).not.toBeInTheDocument();
    });
  });

  describe('可访问性', () => {
    it('应该有正确的语义化 HTML', () => {
      render(<BatchResultModal {...defaultProps} />);
      const successCount = screen.getByText('95');
      const failedCount = screen.getByText('5');

      expect(successCount).toBeInTheDocument();
      expect(failedCount).toBeInTheDocument();
    });
  });
});
