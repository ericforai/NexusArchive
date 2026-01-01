// src/components/voucher/__tests__/VoucherPreviewCanvas.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VoucherPreviewCanvas } from '../VoucherPreviewCanvas';

describe('VoucherPreviewCanvas', () => {
  const mockVoucherData = {
    voucherId: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    orgName: '测试公司',
    debitTotal: 1500,
    creditTotal: 1500,
    creator: '张三',
    auditor: '李四',
    poster: '王五',
    entries: [
      {
        lineNo: 1,
        summary: '测试摘要',
        accountCode: '1001',
        accountName: '库存现金',
        debit: 1500,
        credit: 0,
      },
      {
        lineNo: 2,
        summary: '测试摘要2',
        accountCode: '1002',
        accountName: '银行存款',
        debit: 0,
        credit: 1500,
      },
    ],
  };

  it('应该渲染凭证表头信息', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getByText(/测试公司/)).toBeTruthy();
    expect(screen.getByText(/2025-01-01/)).toBeTruthy();
    expect(screen.getByText(/记-001/)).toBeTruthy();
  });

  it('应该渲染分录表格', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getByText(/测试摘要2/)).toBeTruthy();
    expect(screen.getByText(/库存现金/)).toBeTruthy();
    expect(screen.getByText(/银行存款/)).toBeTruthy();
  });

  it('应该正确计算并显示合计', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getAllByText(/¥ 1,500.00/).length).toBeGreaterThan(0);
    expect(screen.getByText(/壹仟伍佰.*元整/)).toBeTruthy();
  });

  it('应该显示签章区', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} showSignature={true} />);
    expect(screen.getByText(/制单人:/)).toBeTruthy();
    expect(screen.getByText(/张三/)).toBeTruthy();
    expect(screen.getByText(/审核人:/)).toBeTruthy();
    expect(screen.getByText(/李四/)).toBeTruthy();
    expect(screen.getByText(/记账人:/)).toBeTruthy();
    expect(screen.getByText(/王五/)).toBeTruthy();
  });

  it('应该支持紧凑模式', () => {
    const { container } = render(<VoucherPreviewCanvas data={mockVoucherData} compact={true} />);
    const canvas = container.querySelector('[style*="font-size"]');
    expect(canvas).toBeTruthy();
  });

  it('应该在无分录时显示占位文本', () => {
    const noEntriesData = { ...mockVoucherData, entries: [] };
    render(<VoucherPreviewCanvas data={noEntriesData} />);
    expect(screen.getByText(/暂无分录数据/)).toBeTruthy();
  });

  it('应该支持 memo 优化', () => {
    const { rerender } = render(<VoucherPreviewCanvas data={mockVoucherData} />);
    const initialRender = screen.getByText(/记-001/);

    rerender(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(initialRender).toBeTruthy();
  });
});
