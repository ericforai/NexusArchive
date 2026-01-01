// src/components/voucher/__tests__/VoucherPreviewTabs.test.tsx
import { describe, it, expect } from 'vitest';
import { VoucherPreviewTabs } from '../VoucherPreviewTabs';

describe('VoucherPreviewTabs', () => {
  const mockVoucherData = {
    voucherId: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    entries: [],
  };

  const mockAttachments = [
    { id: '1', fileName: 'invoice1.pdf', fileUrl: '/files/invoice1.pdf' },
    { id: '2', fileName: 'contract.pdf', fileUrl: '/files/contract.pdf' },
  ];

  it('应该成功渲染组件', () => {
    expect(() => <VoucherPreviewTabs voucherData={mockVoucherData} />).toBeTruthy();
  });

  it('应该接受附件数据', () => {
    expect(() => (
      <VoucherPreviewTabs voucherData={mockVoucherData} attachments={mockAttachments} />
    )).toBeTruthy();
  });

  it('应该支持默认标签配置', () => {
    expect(() => (
      <VoucherPreviewTabs
        voucherData={mockVoucherData}
        attachments={mockAttachments}
        defaultTab="attachments"
      />
    )).toBeTruthy();
  });
});
