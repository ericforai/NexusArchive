// src/pages/archives/ArchiveDetailPage.tsx
/**
 * Archive Detail Page - 凭证详情页（全屏展开视图）
 *
 * 职责：提供全屏凭证详情视图，从 Drawer "展开到新页"功能触发
 */

import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';
import { Tabs, Breadcrumb } from 'antd';
// VoucherDTO 类型由 useVoucherData hook 内部使用
import { VoucherMetadata, VoucherPreviewCanvas, OriginalDocumentPreview } from '../../components/voucher';
import { useVoucherData } from './hooks/useVoucherData';
import { archivesApi } from '../../api/archives';

type TabKey = 'metadata' | 'voucher' | 'attachments';

export const ArchiveDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // 档案数据状态（从 API 获取）
  const [archiveData, setArchiveData] = React.useState<any>(null);
  const [_archiveLoading, setArchiveLoading] = React.useState(false);

  // 从 API 获取真实档案数据
  useEffect(() => {
    if (!id) return;

    const fetchArchive = async () => {
      setArchiveLoading(true);
      try {
        const response = await archivesApi.getArchiveById(id);
        if (response?.code === 200 && response.data) {
          setArchiveData(response.data);
        } else {
          if (import.meta.env.DEV) console.warn('[ArchiveDetailPage] Archive not found for id:', id);
          setArchiveData(null);
        }
      } catch (error) {
        if (import.meta.env.DEV) console.error('[ArchiveDetailPage] Failed to fetch archive:', error);
        setArchiveData(null);
      } finally {
        setArchiveLoading(false);
      }
    };

    fetchArchive();
  }, [id]);

  // 从 API 数据构建 row 对象
  const row = React.useMemo(() => {
    if (!id) return null;
    if (archiveData) {
      return {
        id: archiveData.id,
        code: archiveData.archiveCode || archiveData.erpVoucherNo || archiveData.id,
        archivalCode: archiveData.archiveCode,
        ...archiveData,
      };
    }
    // Fallback：数据加载中时显示 ID
    return { id, code: id, archivalCode: id };
  }, [id, archiveData]);

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
      children: (
        <div className="p-4 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {voucherData ? (
            <VoucherMetadata data={voucherData} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: (
        <div className="p-4 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {voucherData ? (
            <VoucherPreviewCanvas data={voucherData} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-0 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>加载中...</p>
            </div>
          ) : voucherData?.attachments && voucherData.attachments.length > 0 ? (
            <OriginalDocumentPreview archiveId={row?.id || id} files={voucherData.attachments} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>暂无附件</p>
            </div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50" data-archive-id={id}>
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-[1600px] mx-auto px-6 py-4">
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
      <div className="max-w-[1600px] mx-auto px-6 py-6">
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
