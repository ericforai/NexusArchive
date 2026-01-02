// src/pages/archives/ArchiveDetailDrawer.tsx
/**
 * Archive Detail Drawer - 凭证预览抽屉
 *
 * 职责：凭证预览抽屉的 UI 布局和组件组装
 * 变更理由：替换 Modal 为 Drawer，提升 UX
 */

import React, { useEffect, useMemo } from 'react';
import { Drawer, Tabs } from 'antd';
import { FileText, X, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { DrawerProps } from 'antd';
import type { VoucherDTO } from '../../components/voucher';
import { VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';
import { OriginalDocumentPreview } from '../../components/voucher/OriginalDocumentPreview';
import type { ModuleConfig, GenericRow } from '../../types';
import { useVoucherData } from './hooks/useVoucherData';
import { VoucherExportButton } from './components/VoucherExportButton';

interface ArchiveDetailDrawerProps {
  open: boolean;
  onClose: () => void;
  row: GenericRow | null;
  config: ModuleConfig;
  isPoolView: boolean;

  // AIP 导出（仅归档模式）
  onAipExport?: (row: GenericRow) => void;
  isExporting?: string | null;
}

type TabKey = 'metadata' | 'voucher' | 'attachments';

// Responsive width calculation
const getDrawerWidth = (): string | number => {
  const width = window.innerWidth;
  if (width >= 1280) return '50vw'; // Large screens
  if (width >= 768) return '70vw';  // Medium screens
  return '100vw'; // Small screens (full screen)
};

export const ArchiveDetailDrawer: React.FC<ArchiveDetailDrawerProps> = ({
  open,
  onClose,
  row,
  config,
  isPoolView,
  onAipExport,
  isExporting,
}) => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // 使用自定义 hook 获取凭证数据
  const { voucherData } = useVoucherData({ row, enabled: open });

  // Route listener: auto-close on navigation
  useEffect(() => {
    const unlisten = () => {
      // React Router v7 doesn't have history.listen, using location changes
      // The drawer will close due to parent component's route monitoring
    };
    return unlisten;
  }, []);

  // Close drawer when route changes (handled by parent)
  // This is a placeholder for any additional cleanup
  useEffect(() => {
    if (!open) {
      setActiveTab('metadata'); // Reset tab when drawer closes
    }
  }, [open]);

  const handleExpandToPage = () => {
    if (!row?.id) return;
    navigate(`/system/archives/${row.id}`);
  };

  const drawerWidth: string | number = useMemo(() => {
    if (typeof window === 'undefined') return '50vw';
    return getDrawerWidth();
  }, []);

  // Don't render if closed or no row
  if (!open || !row) return null;

  const tabItems = [
    {
      key: 'metadata',
      label: '业务元数据',
      children: voucherData ? (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <VoucherMetadata data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-full text-slate-400">
          <p>暂无凭证数据</p>
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: voucherData ? (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <VoucherPreviewCanvas data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-full text-slate-400">
          <p>暂无凭证数据</p>
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <OriginalDocumentPreview files={voucherData?.attachments || []} />
        </div>
      ),
    },
  ];

  return (
    <Drawer
      data-testid="archive-detail-drawer"
      open={open}
      onClose={onClose}
      // eslint-disable-next-line antd/no-deprecated
      width={drawerWidth}
      placement="right"
      maskClosable={true}
      keyboard={true}
      destroyOnClose={true}
      styles={{
        body: { padding: 0 },
        header: { padding: '16px 24px', borderBottom: '1px solid #f0f0f0' }
      }}
      closeIcon={null}
      title={
        <div className="flex items-center justify-between w-full pr-8">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
              <FileText size={18} />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-800">凭证预览</h3>
              <p className="text-xs text-slate-500 font-mono">{row.code}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* AIP 导出按钮（仅归档模式） */}
            {!isPoolView && onAipExport && (row.archivalCode || row.code) && (
              <VoucherExportButton
                onExport={() => onAipExport(row)}
                isExporting={isExporting === (row.archivalCode || row.code)}
              />
            )}
            {/* 展开到新页按钮 */}
            <button
              onClick={handleExpandToPage}
              className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
              title="展开到新页"
            >
              <ExternalLink size={16} />
            </button>
            {/* 关闭按钮 */}
            <button
              data-testid="close-drawer"
              onClick={onClose}
              className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
              title="关闭"
            >
              <X size={18} />
            </button>
          </div>
        </div>
      }
    >
      <div className="h-full bg-white">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as TabKey)}
          className="h-full"
          items={tabItems}
        />
      </div>
    </Drawer>
  );
};

export default ArchiveDetailDrawer;
