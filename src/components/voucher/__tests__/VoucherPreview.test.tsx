// src/components/voucher/__tests__/VoucherPreview.test.tsx
import { describe, it, expect } from 'vitest';
import { VoucherPreview } from '../VoucherPreview';

describe('VoucherPreview', () => {
  const mockVoucherData = {
    id: 'test-001',
    voucherId: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    orgName: '测试公司',
    debitTotal: 1500,
    entries: [],
  };

  it('应该支持水平布局', () => {
    expect(() => (
      <VoucherPreview data={mockVoucherData} layout="horizontal" />
    )).toBeTruthy();
  });

  it('应该支持垂直布局', () => {
    expect(() => (
      <VoucherPreview data={mockVoucherData} layout="vertical" />
    )).toBeTruthy();
  });

  it('应该支持紧凑模式', () => {
    expect(() => (
      <VoucherPreview data={mockVoucherData} size="compact" />
    )).toBeTruthy();
  });

  it('应该支持大尺寸', () => {
    expect(() => (
      <VoucherPreview data={mockVoucherData} size="large" />
    )).toBeTruthy();
  });

  it('应该成功渲染组件', () => {
    expect(() => <VoucherPreview data={mockVoucherData} />).toBeTruthy();
  });
});
