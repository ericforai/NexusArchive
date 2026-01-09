// src/pages/archives/ArchiveDetailDrawer.tsx
/**
 * Archive Detail Drawer - 凭证预览抽屉
 *
 * 职责：凭证预览抽屉的 UI 布局和组件组装
 * 变更理由：替换 Modal 为 Drawer，提升 UX；Spin tip 采用嵌套写法
 */

import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Drawer, Tabs, Collapse, Button, List, Tag, Space, message, Spin, Empty } from 'antd';
import { FileText, X, ExternalLink, CloudDownload, Link, Download } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { DrawerProps } from 'antd';
import type { VoucherDTO } from '../../components/voucher';
import { VoucherMetadata, VoucherPreviewCanvas, OriginalDocumentPreview } from '../../components/voucher';
import type { ModuleConfig, GenericRow } from '../../types';
import { useVoucherData } from './hooks/useVoucherData';
import { VoucherExportButton } from './components/VoucherExportButton';
import { yonsuiteApi } from '../../api/yonsuite';
import type { VoucherAttachment } from '../../api/yonsuite';

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

type TabKey = 'metadata' | 'voucher' | 'attachments' | 'yonsuite';

// Responsive size calculation
const getDrawerSize = (): DrawerProps['size'] => {
  const width = window.innerWidth;
  if (width >= 1280) return 'large'; // Large screens
  return 'default';  // Medium and small screens
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

  // YonSuite 附件相关状态
  const [yonsuiteAttachments, setYonsuiteAttachments] = useState<VoucherAttachment[]>([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);
  const [attachmentsFetched, setAttachmentsFetched] = useState(false);

  // 查询 YonSuite 附件
  const handleFetchYonsuiteAttachments = useCallback(async () => {
    if (!row?.id) {
      message.warning('需要凭证 ID 才能查询附件');
      return;
    }
    setAttachmentsLoading(true);
    try {
      const configId = 1;
      const response = await yonsuiteApi.queryVoucherAttachments(configId, [row.id]);
      if (response && response.data && response.data[row.id]) {
        setYonsuiteAttachments(response.data[row.id]);
        setAttachmentsFetched(true);
        message.success(`查询到 ${response.data[row.id].length} 个附件`);
      } else {
        setYonsuiteAttachments([]);
        setAttachmentsFetched(true);
        message.info('该凭证在 YonSuite 中没有附件');
      }
    } catch (error: any) {
      console.error('查询 YonSuite 附件失败:', error);
      message.error(`查询失败: ${error.message || '未知错误'}`);
      setYonsuiteAttachments([]);
    } finally {
      setAttachmentsLoading(false);
    }
  }, [row?.id]);

  // 当 row 变化时重置附件状态
  useEffect(() => {
    if (row?.id) {
      setAttachmentsFetched(false);
      setYonsuiteAttachments([]);
    }
  }, [row?.id]);

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

  const drawerSize: DrawerProps['size'] = useMemo(() => {
    if (typeof window === 'undefined') return 'large';
    return getDrawerSize();
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
    {
      key: 'yonsuite',
      label: (
        <Space size={4}>
          <CloudDownload size={14} />
          <span>YonSuite 附件</span>
          {attachmentsFetched && (
            <Tag color={yonsuiteAttachments.length > 0 ? 'success' : 'default'}>
              {yonsuiteAttachments.length}
            </Tag>
          )}
        </Space>
      ),
      children: (
        <div className="p-4">
          {!attachmentsFetched ? (
            <div className="text-center py-8">
              <p className="text-slate-500 mb-4">查询 YonSuite 中的凭证附件</p>
              <Button
                type="primary"
                icon={<CloudDownload size={14} />}
                loading={attachmentsLoading}
                onClick={handleFetchYonsuiteAttachments}
              >
                查询附件
              </Button>
            </div>
          ) : (
            <>
              {attachmentsLoading ? (
                <div className="flex items-center justify-center h-32">
                  <Spin tip="查询中...">
                    <div style={{ minHeight: 48 }} />
                  </Spin>
                </div>
              ) : yonsuiteAttachments.length > 0 ? (
                <List
                  dataSource={yonsuiteAttachments}
                  renderItem={(item) => (
                    <List.Item
                      className="!px-2"
                      actions={[
                        <Button
                          type="text"
                          size="small"
                          icon={<Download size={14} />}
                          onClick={() => message.info(`下载 ${item.fileName}`)}
                        />
                      ]}
                    >
                      <List.Item.Meta
                        avatar={<Link size={14} className="text-blue-500" />}
                        title={item.fileName || item.name}
                        description={
                          <Space size="middle">
                            <span>{(item.fileSize / 1024).toFixed(1)} KB</span>
                            {item.fileExtension && (
                              <Tag>{item.fileExtension}</Tag>
                            )}
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              ) : (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="暂无附件"
                />
              )}
              {/* 重新查询按钮 */}
              <div className="text-center mt-4 pt-4 border-t border-slate-100">
                <Button
                  size="small"
                  onClick={handleFetchYonsuiteAttachments}
                >
                  重新查询
                </Button>
              </div>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <Drawer
      data-testid="archive-detail-drawer"
      open={open}
      onClose={onClose}
      size={drawerSize}
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
