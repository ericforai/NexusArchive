// src/pages/archives/ArchiveDetailPage.tsx
/**
 * Archive Detail Page - 凭证详情页（全屏展开视图）
 *
 * 职责：提供全屏凭证详情视图，从 Drawer "展开到新页"功能触发
 */

import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';
import { Tabs, Breadcrumb } from 'antd';
import type { VoucherDTO } from '../../components/voucher';
import { VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';
import { OriginalDocumentPreview } from '../../components/voucher/OriginalDocumentPreview';
import { useVoucherData } from './hooks/useVoucherData';

// Simulate row from URL param (in real implementation, fetch data by ID)
const createMockRowFromId = (id: string): any => ({
  id,
  code: `凭证-${id.slice(-6)}`,
  archivalCode: `ARCH-${id.slice(-6)}`,
});

type TabKey = 'metadata' | 'voucher' | 'attachments';

export const ArchiveDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // Simulate row from URL param
  const row = React.useMemo(() => (id ? createMockRowFromId(id) : null), [id]);

  // 使用自定义 hook 获取凭证数据
  const { voucherData, isLoading } = useVoucherData({
    row,
    enabled: !!id,
  });

  const handleBack = () => {
    navigate(-1); // Go back to previous page
  };

  if (!id) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-slate-500">无效的档案 ID</p>
      </div>
    );
  }

  const tabItems = [
    {
      key: 'metadata',
      label: '业务元数据',
      children: voucherData ? (
        <div className="p-6">
          <VoucherMetadata data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: voucherData ? (
        <div className="p-6">
          <VoucherPreviewCanvas data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-6">
          <OriginalDocumentPreview files={voucherData?.attachments || []} />
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50" data-archive-id={id}>
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <Breadcrumb
            items={[
              { title: '档案管理' },
              { title: '凭证详情' },
              { title: row?.code || id }
            ]}
            className="mb-4"
          />
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={handleBack}
                className="p-2 hover:bg-slate-100 rounded-lg text-slate-600 transition-colors"
                title="返回"
              >
                <ArrowLeft size={20} />
              </button>
              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
                  <FileText size={20} />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-slate-800">凭证详情</h1>
                  <p className="text-sm text-slate-500 font-mono">{row?.code || id}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-6 py-6">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as TabKey)}
            className="w-full"
            items={tabItems}
          />
        </div>
      </div>
    </div>
  );
};

export default ArchiveDetailPage;
