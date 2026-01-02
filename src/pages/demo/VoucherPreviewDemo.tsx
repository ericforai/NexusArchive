// src/pages/demo/VoucherPreviewDemo.tsx
import React from 'react';
import { VoucherPreview, VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';

const mockVoucherData = {
  id: 'demo-001',
  voucherId: 'voucher-demo-001',
  voucherNo: '001',
  voucherWord: '记',
  voucherDate: '2025-01-01',
  orgName: '示例公司',
  accountPeriod: '2025-01',
  accbookCode: 'BR01',
  summary: '测试凭证摘要',
  debitTotal: 1500,
  creditTotal: 1500,
  createdTime: '2025-01-01T10:00:00',
  creator: '张三',
  auditor: '李四',
  poster: '王五',
  entries: [
    {
      lineNo: 1,
      summary: '收到货款',
      accountCode: '1001',
      accountName: '库存现金',
      debit: 1500,
      credit: 0,
    },
    {
      lineNo: 2,
      summary: '收到货款',
      accountCode: '1002',
      accountName: '银行存款',
      debit: 0,
      credit: 1500,
    },
  ],
  attachments: [
    { id: '1', fileName: '发票.pdf', fileUrl: '/files/invoice.pdf', type: 'invoice' },
  ],
};

export const VoucherPreviewDemo: React.FC = () => {
  return (
    <div className="p-8 space-y-8">
      <h1 className="text-2xl font-bold">凭证预览组件演示</h1>

      <section>
        <h2 className="text-xl font-semibold mb-4">完整预览（水平布局）</h2>
        <VoucherPreview data={mockVoucherData} layout="horizontal" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">完整预览（垂直布局）</h2>
        <VoucherPreview data={mockVoucherData} layout="vertical" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">紧凑模式</h2>
        <VoucherPreview data={mockVoucherData} size="compact" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">仅元数据</h2>
        <div className="w-72">
          <VoucherMetadata data={mockVoucherData} />
        </div>
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">仅凭证渲染</h2>
        <div className="max-w-2xl">
          <VoucherPreviewCanvas data={mockVoucherData} />
        </div>
      </section>
    </div>
  );
};

export default VoucherPreviewDemo;
