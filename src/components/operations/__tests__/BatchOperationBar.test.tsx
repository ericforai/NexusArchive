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
});
