// src/components/voucher/__tests__/VoucherMetadata.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VoucherMetadata } from '../VoucherMetadata';

describe('VoucherMetadata', () => {
  const mockVoucherData = {
    id: 'test-storage-id-12345',
    voucherId: 'voucher-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    debitTotal: 1500,
    createdTime: '2025-01-01T10:00:00',
    attachments: [
      { id: '1', type: 'invoice', name: '发票1.pdf' },
      { id: '2', type: 'contract', name: '合同.pdf' },
      { id: '3', name: '其他发票.pdf' },
    ],
  };

  it('应该渲染业务元数据标题', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/业务元数据/)).toBeTruthy();
  });

  it('应该显示默认字段', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/凭证号/)).toBeTruthy();
    expect(screen.getByText(/金额/)).toBeTruthy();
    expect(screen.getByText(/业务日期/)).toBeTruthy();
  });

  it('应该正确格式化凭证号', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/记-001/)).toBeTruthy();
  });

  it('应该正确格式化金额', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/¥ 1,500.00/)).toBeTruthy();
  });

  it('应该正确计算关联发票数', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getAllByText(/2/).length).toBeGreaterThan(0);
  });

  it('应该支持紧凑模式', () => {
    const { container } = render(<VoucherMetadata data={mockVoucherData} compact={true} />);
    expect(container.querySelector('.text-sm')).toBeTruthy();
  });

  it('应该支持自定义字段', () => {
    render(<VoucherMetadata data={mockVoucherData} fields={['voucherNo', 'debitTotal']} />);
    expect(screen.getByText(/凭证号/)).toBeTruthy();
    expect(screen.getByText(/金额/)).toBeTruthy();
    // 其他字段不应该显示
    expect(screen.queryByText(/业务日期/)).toBeFalsy();
  });
});
