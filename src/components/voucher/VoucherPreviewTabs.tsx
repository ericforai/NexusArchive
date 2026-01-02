// src/components/voucher/VoucherPreviewTabs.tsx
import React, { useState } from 'react';
import { Tabs } from 'antd';
import { VoucherPreviewCanvas } from './VoucherPreviewCanvas';
import { OriginalDocumentPreview } from './OriginalDocumentPreview';
import type { VoucherDTO, AttachmentDTO } from './types';

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
