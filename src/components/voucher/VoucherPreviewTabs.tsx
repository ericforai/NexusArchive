// src/components/voucher/VoucherPreviewTabs.tsx
import React, { useState } from 'react';
import { Tabs } from 'antd';
import { VoucherPreviewCanvas } from './VoucherPreviewCanvas';
import { OriginalDocumentPreview } from './OriginalDocumentPreview';

export interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

export interface AttachmentDTO {
  id: string;
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

export interface VoucherDTO {
  voucherId: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  orgName?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  creator?: string;
  auditor?: string;
  poster?: string;
  entries?: VoucherEntryDTO[];
  // Additional optional fields for metadata display
  attachments?: AttachmentDTO[];
  createdTime?: string;
  id?: string; // Alias for voucherId in some contexts
}

interface VoucherPreviewTabsProps {
  voucherData: VoucherDTO;
  attachments?: AttachmentDTO[];
  defaultTab?: 'voucher' | 'attachments';
}

export const VoucherPreviewTabs: React.FC<VoucherPreviewTabsProps> = ({
  voucherData,
  attachments = [],
  defaultTab = 'voucher',
}) => {
  const [activeTab, setActiveTab] = useState(defaultTab);

  const handleTabChange = (key: string) => {
    setActiveTab(key as 'voucher' | 'attachments');
  };

  const tabItems = [
    {
      key: 'voucher',
      label: '会计凭证',
      children: <VoucherPreviewCanvas data={voucherData} />,
    },
    {
      key: 'attachments',
      label: `关联附件${attachments.length > 0 ? ` (${attachments.length})` : ''}`,
      children: <OriginalDocumentPreview files={attachments} />,
    },
  ];

  return (
    <div className="h-full w-full bg-white">
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        items={tabItems}
        className="voucher-preview-tabs"
      />
    </div>
  );
};
