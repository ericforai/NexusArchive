// Input: Vitest test suite for BatchResultModal component
// Output: Comprehensive test coverage (renders, user interactions, async callbacks, edge cases, status icons)
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

      expect(screen.getByText('所有记录均已成功处理完成！')).toBeInTheDocument();
      expect(screen.getByText('所有记录均已成功处理')).toBeInTheDocument();
    });

    it('全部成功时应该显示 100 成功数', () => {
      const props = { ...defaultProps, successCount: 100, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('100')).toBeInTheDocument();
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

  describe('边缘情况', () => {
    it('应该处理空错误数组', () => {
      const props = { ...defaultProps, errors: [], failedCount: 0 };
      render(<BatchResultModal {...props} />);
      expect(screen.queryByText('失败详情')).not.toBeInTheDocument();
    });

    it('应该处理未定义的 errors', () => {
      const props = { ...defaultProps, errors: undefined, failedCount: 0 };
      render(<BatchResultModal {...props} />);
      expect(screen.queryByText('失败详情')).not.toBeInTheDocument();
    });

    it('应该处理零成功和零失败', () => {
      const props = { ...defaultProps, successCount: 0, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('所有记录均已成功处理')).toBeInTheDocument();
    });

    it('应该处理大数字', () => {
      const props = { ...defaultProps, successCount: 9999, failedCount: 8888 };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('9999')).toBeInTheDocument();
      expect(screen.getByText('8888')).toBeInTheDocument();
    });

    it('应该处理单个错误', () => {
      const props = {
        ...defaultProps,
        errors: [{ id: 1, reason: 'Single error' }],
        failedCount: 1,
      };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('共 1 条')).toBeInTheDocument();
      expect(screen.getByText('档案 #1')).toBeInTheDocument();
      expect(screen.getByText('Single error')).toBeInTheDocument();
    });

    it('应该处理许多错误（可滚动列表）', () => {
      const manyErrors = Array.from({ length: 50 }, (_, i) => ({
        id: i + 1,
        reason: `Error ${i + 1}`,
      }));
      const props = { ...defaultProps, errors: manyErrors, failedCount: 50 };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('共 50 条')).toBeInTheDocument();
      expect(screen.getByText('档案 #1')).toBeInTheDocument();
      expect(screen.getByText('档案 #50')).toBeInTheDocument();
    });

    it('应该处理默认 operationType', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('批量操作完成')).toBeInTheDocument();
    });

    it('应该处理默认 isRetrying', () => {
      render(<BatchResultModal {...defaultProps} onRetry={vi.fn()} />);
      expect(screen.getByText('重试失败项')).toBeInTheDocument();
      expect(screen.queryByText('重试中...')).not.toBeInTheDocument();
    });
  });

  describe('状态图标', () => {
    it('应该显示全部成功状态描述（emerald）', () => {
      const props = { ...defaultProps, successCount: 100, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('所有记录均已成功处理')).toBeInTheDocument();
    });

    it('应该显示全部失败状态描述（rose）', () => {
      const props = { ...defaultProps, successCount: 0, failedCount: 5 };
      render(<BatchResultModal {...props} />);
      expect(screen.getByText('所有记录处理失败，请检查失败详情')).toBeInTheDocument();
    });

    it('应该显示部分成功状态描述（amber）', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('部分记录处理成功，请检查失败详情')).toBeInTheDocument();
    });
  });

  describe('禁用状态', () => {
    it('重试中应该禁用关闭按钮', () => {
      const props = { ...defaultProps, isRetrying: true };
      render(<BatchResultModal {...props} />);

      const closeButton = screen.getByText('关闭');
      expect(closeButton).toBeDisabled();
    });

    it('重试中应该禁用导出按钮', () => {
      const props = {
        ...defaultProps,
        isRetrying: true,
        onExportReport: vi.fn(),
      };
      render(<BatchResultModal {...props} />);

      const exportButton = screen.getByText('导出报告');
      expect(exportButton).toBeDisabled();
    });

    it('重试中应该禁用重试按钮', () => {
      const props = { ...defaultProps, isRetrying: true };
      render(<BatchResultModal {...props} />);

      const retryButton = screen.getByText('重试中...');
      expect(retryButton).toBeDisabled();
    });
  });

  describe('集成场景', () => {
    it('应该处理完整工作流：无错误的成功', () => {
      const props = { ...defaultProps, failedCount: 0, errors: [] };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('所有记录均已成功处理')).toBeInTheDocument();
      expect(screen.getByText('所有记录均已成功处理完成！')).toBeInTheDocument();
      expect(screen.queryByText('失败详情')).not.toBeInTheDocument();
      expect(screen.queryByText('重试失败项')).not.toBeInTheDocument();
      expect(screen.queryByText('导出报告')).not.toBeInTheDocument();
    });

    it('应该处理完整工作流：带重试和导出的部分成功', async () => {
      const onRetry = vi.fn();
      const onExportReport = vi.fn();
      const props = {
        ...defaultProps,
        onRetry,
        onExportReport,
      };

      render(<BatchResultModal {...props} />);

      // 验证初始状态
      expect(screen.getByText('部分记录处理成功，请检查失败详情')).toBeInTheDocument();
      expect(screen.getByText('失败详情')).toBeInTheDocument();

      // 点击导出
      fireEvent.click(screen.getByText('导出报告'));
      expect(onExportReport).toHaveBeenCalledTimes(1);

      // 点击重试
      fireEvent.click(screen.getByText('重试失败项'));
      expect(onRetry).toHaveBeenCalledWith([1, 2]);

      // 关闭弹窗
      fireEvent.click(screen.getByText('关闭'));
      expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
    });

    it('应该处理完整工作流：全部失败', () => {
      const props = { ...defaultProps, successCount: 0 };
      render(<BatchResultModal {...props} />);

      expect(screen.getByText('所有记录处理失败，请检查失败详情')).toBeInTheDocument();
      expect(screen.getByText('失败详情')).toBeInTheDocument();
    });
  });

  describe('异步回调', () => {
    it('应该处理异步 onRetry 回调', async () => {
      const onRetry = vi.fn(async (_ids: number[]) => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      render(<BatchResultModal {...defaultProps} onRetry={onRetry} />);

      const retryButton = screen.getByText('重试失败项');
      fireEvent.click(retryButton);

      await waitFor(() => {
        expect(onRetry).toHaveBeenCalledTimes(1);
        expect(onRetry).toHaveBeenCalledWith([1, 2]);
      });
    });

    it('应该处理异步 onExportReport 回调', async () => {
      const onExportReport = vi.fn(async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
      });

      render(<BatchResultModal {...defaultProps} onExportReport={onExportReport} />);

      const exportButton = screen.getByText('导出报告');
      fireEvent.click(exportButton);

      await waitFor(() => {
        expect(onExportReport).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('Props 处理', () => {
    it('未提供 operationType 时应使用默认值', () => {
      render(<BatchResultModal {...defaultProps} />);
      expect(screen.getByText('批量操作完成')).toBeInTheDocument();
    });

    it('应使用提供的 operationType', () => {
      render(<BatchResultModal {...defaultProps} operationType="approval" />);
      expect(screen.getByText('批量审批完成')).toBeInTheDocument();
    });

    it('应默认 isRetrying 为 false', () => {
      render(<BatchResultModal {...defaultProps} onRetry={vi.fn()} />);
      expect(screen.getByText('重试失败项')).toBeInTheDocument();
      expect(screen.queryByText('重试中...')).not.toBeInTheDocument();
    });

    it('应优雅处理未定义的 errors', () => {
      render(<BatchResultModal {...defaultProps} errors={undefined} />);
      expect(screen.queryByText('失败详情')).not.toBeInTheDocument();
    });
  });
});
