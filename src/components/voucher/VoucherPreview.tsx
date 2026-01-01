// src/components/voucher/VoucherPreview.tsx
import React from 'react';
import { VoucherMetadata } from './VoucherMetadata';
import { VoucherPreviewTabs } from './VoucherPreviewTabs';

interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

interface AttachmentDTO {
  id: string;
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

interface VoucherDTO {
  id: string;
  voucherId?: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  orgName?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  createdTime?: string;
  creator?: string;
  auditor?: string;
  poster?: string;
  entries?: VoucherEntryDTO[];
  attachments?: AttachmentDTO[];
}

type LayoutType = 'horizontal' | 'vertical';
type SizeType = 'compact' | 'normal' | 'large';

interface VoucherPreviewProps {
  data: VoucherDTO;
  attachments?: AttachmentDTO[];
  layout?: LayoutType;
  size?: SizeType;
  defaultTab?: 'voucher' | 'attachments';
}

export const VoucherPreview: React.FC<VoucherPreviewProps> = ({
  data,
  attachments,
  layout = 'horizontal',
  size = 'normal',
  defaultTab = 'voucher',
}) => {
  const isCompact = size === 'compact';
  const isLarge = size === 'large';

  // 水平布局（左右分栏）
  if (layout === 'horizontal') {
    return (
      <div className={`flex gap-4 ${isLarge ? 'h-[600px]' : 'h-[500px]'}`}>
        {/* 左侧元数据 */}
        <div className={isCompact ? 'w-64' : isLarge ? 'w-80' : 'w-72'}>
          <VoucherMetadata data={data} compact={isCompact} />
        </div>

        {/* 右侧预览 */}
        <div className="flex-1 border border-slate-200 rounded-lg overflow-hidden">
          <VoucherPreviewTabs
            voucherData={data}
            attachments={attachments}
            defaultTab={defaultTab}
          />
        </div>
      </div>
    );
  }

  // 垂直布局（上下堆叠）
  return (
    <div className="flex flex-col gap-4">
      {/* 上方元数据 */}
      <VoucherMetadata data={data} compact={isCompact} />

      {/* 下方预览 */}
      <div className="border border-slate-200 rounded-lg overflow-hidden" style={{ minHeight: '400px' }}>
        <VoucherPreviewTabs
          voucherData={data}
          attachments={attachments}
          defaultTab={defaultTab}
        />
      </div>
    </div>
  );
};
