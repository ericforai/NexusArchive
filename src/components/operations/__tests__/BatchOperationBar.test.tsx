// Input: React、@testing-library/react、lucide-react
// Output: BatchOperationBar 组件测试
// Pos: src/components/operations/__tests__/BatchOperationBar.test.tsx

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BatchOperationBar } from '../BatchOperationBar';

describe('BatchOperationBar', () => {
  const mockProps = {
    selectedCount: 5,
    totalCount: 100,
    onBatchApprove: vi.fn(),
    onBatchReject: vi.fn(),
    onSelectAll: vi.fn(),
    onClear: vi.fn(),
    loading: false
  };

  it('should not render when selectedCount is 0', () => {
    const { container } = render(
      <BatchOperationBar {...mockProps} selectedCount={0} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('should render selected count', () => {
    render(<BatchOperationBar {...mockProps} />);

    expect(screen.getByText(/已选择/)).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText(/共 100 条筛选结果/)).toBeInTheDocument();
  });

  it('should render select all button when totalCount exists', () => {
    render(<BatchOperationBar {...mockProps} />);

    expect(screen.getByText('全选所有')).toBeInTheDocument();
  });

  it('should not render select all button when totalCount is undefined', () => {
    render(<BatchOperationBar {...mockProps} totalCount={undefined} />);

    expect(screen.queryByText('全选所有')).not.toBeInTheDocument();
  });

  it('should render action buttons', () => {
    render(<BatchOperationBar {...mockProps} />);

    expect(screen.getByText('批量批准')).toBeInTheDocument();
    expect(screen.getByText('批量拒绝')).toBeInTheDocument();
    expect(screen.getByText('清空')).toBeInTheDocument();
  });

  it('should call onBatchApprove when approve button is clicked', () => {
    render(<BatchOperationBar {...mockProps} />);

    const approveButton = screen.getByText('批量批准');
    fireEvent.click(approveButton);

    expect(mockProps.onBatchApprove).toHaveBeenCalledTimes(1);
  });

  it('should call onBatchReject when reject button is clicked', () => {
    render(<BatchOperationBar {...mockProps} />);

    const rejectButton = screen.getByText('批量拒绝');
    fireEvent.click(rejectButton);

    expect(mockProps.onBatchReject).toHaveBeenCalledTimes(1);
  });

  it('should call onClear when clear button is clicked', () => {
    render(<BatchOperationBar {...mockProps} />);

    const clearButton = screen.getByText('清空');
    fireEvent.click(clearButton);

    expect(mockProps.onClear).toHaveBeenCalledTimes(1);
  });

  it('should call onSelectAll when select all button is clicked', () => {
    render(<BatchOperationBar {...mockProps} />);

    const selectAllButton = screen.getByText('全选所有');
    fireEvent.click(selectAllButton);

    expect(mockProps.onSelectAll).toHaveBeenCalledTimes(1);
  });

  it('should disable buttons when loading', () => {
    render(<BatchOperationBar {...mockProps} loading={true} />);

    const processButtons = screen.getAllByText('处理中...');
    const clearButton = screen.getByText('清空');

    expect(processButtons).toHaveLength(2);
    expect(processButtons[0]).toBeDisabled();
    expect(processButtons[1]).toBeDisabled();
    expect(clearButton).toBeDisabled();
  });

  it('should show over limit warning when selectedCount > 100', () => {
    render(<BatchOperationBar {...mockProps} selectedCount={101} />);

    expect(screen.getByText(/批量操作最多支持 100 条记录/)).toBeInTheDocument();

    const approveButton = screen.getByText('批量批准');
    const rejectButton = screen.getByText('批量拒绝');

    expect(approveButton).toBeDisabled();
    expect(rejectButton).toBeDisabled();
  });

  it('should not show select all button when already over limit', () => {
    render(<BatchOperationBar {...mockProps} selectedCount={101} />);

    expect(screen.queryByText('全选所有')).not.toBeInTheDocument();
  });

  // 边界测试：totalCount === 100
  it('should show select all button when totalCount === 100 and selectedCount < 100', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={50}
        totalCount={100}
      />
    );

    expect(screen.getByText('全选所有')).toBeInTheDocument();
  });

  // 边界测试：totalCount === 101（超过限制）
  it('should not show select all button when totalCount === 101', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={50}
        totalCount={101}
      />
    );

    expect(screen.queryByText('全选所有')).not.toBeInTheDocument();
  });

  // 边界测试：selectedCount === 100（精确边界）
  it('should enable buttons when selectedCount === 100', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={100}
        totalCount={100}
      />
    );

    const approveButton = screen.getByText('批量批准');
    const rejectButton = screen.getByText('批量拒绝');

    expect(approveButton).not.toBeDisabled();
    expect(rejectButton).not.toBeDisabled();
    expect(screen.queryByText(/批量操作最多支持 100 条记录/)).not.toBeInTheDocument();
  });

  // 边界测试：selectedCount === 99（未超限）
  it('should not show warning when selectedCount === 99', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={99}
        totalCount={100}
      />
    );

    expect(screen.queryByText(/批量操作最多支持 100 条记录/)).not.toBeInTheDocument();
  });

  // 边界测试：selectedCount === 101（刚好超限）
  it('should show warning and disable buttons when selectedCount === 101', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={101}
        totalCount={150}
      />
    );

    expect(screen.getByText(/批量操作最多支持 100 条记录/)).toBeInTheDocument();

    const approveButton = screen.getByText('批量批准');
    const rejectButton = screen.getByText('批量拒绝');

    expect(approveButton).toBeDisabled();
    expect(rejectButton).toBeDisabled();
  });

  // 边界测试：totalCount === selectedCount（已全选）
  it('should not show select all button when totalCount === selectedCount', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={20}
        totalCount={20}
      />
    );

    expect(screen.queryByText('全选所有')).not.toBeInTheDocument();
  });

  // 边界测试：totalCount < selectedCount（异常情况）
  it('should not show select all button when totalCount < selectedCount', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={50}
        totalCount={30}
      />
    );

    expect(screen.queryByText('全选所有')).not.toBeInTheDocument();
  });

  // 组合测试：loading + over limit
  it('should disable buttons when both loading and over limit', () => {
    render(
      <BatchOperationBar
        {...mockProps}
        selectedCount={150}
        loading={true}
      />
    );

    const processButtons = screen.getAllByText('处理中...');
    const overLimitButtons = screen.getAllByTitle('超过 100 条限制');

    expect(overLimitButtons).toHaveLength(2);
    expect(overLimitButtons[0]).toBeDisabled();
    expect(overLimitButtons[1]).toBeDisabled();
    expect(processButtons).toHaveLength(2);
  });
});
