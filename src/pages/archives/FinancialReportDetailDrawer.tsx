// Input: 财务报告数据 row、开关状态、回调函数
// Output: 财务报告 PDF 预览抽屉组件（使用预览组件公共 API）
// Pos: src/pages/archives/FinancialReportDetailDrawer.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Financial Report Detail Drawer - 财务报告预览抽屉
 *
 * 职责：财务报告 PDF 预览抽屉，显示报告的 PDF 内容
 * 类似于凭证预览，但专门用于财务报告的 PDF 查看
 */
import React, { useMemo } from 'react';
import { Drawer, Tabs, Alert } from 'antd';
import { FileText, X, FileQuestion } from 'lucide-react';
import type { DrawerProps } from 'antd';
import type { GenericRow } from '../../types';
import { SmartFilePreview } from '../../components/preview';

interface FinancialReportDetailDrawerProps {
  open: boolean;
  onClose: () => void;
  row: GenericRow | null;
}

type TabKey = 'preview' | 'metadata';

// Responsive size calculation
const getDrawerSize = (): DrawerProps['size'] => {
  const width = window.innerWidth;
  if (width >= 1536) return 'large';
  return 'default';
};

// 无文件的演示数据 ID 列表（仅包含真正没有文件关联的报告）
const DEMO_DATA_IDS = new Set([
  'FR-M-2025-001', 'FR-M-2025-002', 'FR-M-2025-003', 'FR-M-2025-004', 'FR-M-2025-005',
  'FR-Q-2025-001', 'FR-Q-2025-002', 'FR-S-2025-001',
  'FR-A-2024-001', 'FR-SP-2025-001',
  'demo-reimb-bb-001'
]);

export const FinancialReportDetailDrawer: React.FC<FinancialReportDetailDrawerProps> = ({
  open,
  onClose,
  row,
}) => {
  const [activeTab, setActiveTab] = React.useState<TabKey>('preview');

  const drawerSize: DrawerProps['size'] = useMemo(() => {
    if (typeof window === 'undefined') return 'large';
    return getDrawerSize();
  }, []);

  // 报告类型标签 - 移动到此处以符合 Hook 调用规则
  const reportTypeLabel = useMemo(() => {
    if (!row) return '财务报告';
    const typeMap: Record<string, string> = {
      'MONTHLY': '月度财务报告',
      'QUARTERLY': '季度财务报告',
      'SEMI_ANNUAL': '半年度财务报告',
      'ANNUAL': '年度财务报告',
      'SPECIAL': '专项财务报告',
    };
    return typeMap[row.type || ''] || row.type || '财务报告';
  }, [row]);

  // Don't render if closed or no row
  if (!open || !row) return null;

  // 获取档案ID用于预览 - 使用 archiveCode 而不是 id
  // 因为后端 getArchiveById 需要通过 archive_code 查找文件关联
  const archiveId = row.archiveCode || row.code || row.archivalCode || row.id;
  console.log("[FinancialReportDetailDrawer] archiveId:", archiveId, "row:", row);

  // 判断是否为无文件的演示数据
  const isDemoDataWithoutFile = DEMO_DATA_IDS.has(row.id || '');

  const tabItems = [
    {
      key: 'preview',
      label: (
        <span className="flex items-center gap-2">
          <FileText size={14} />
          <span>报告预览</span>
        </span>
      ),
      children: (
        <div className="h-full bg-slate-100">
          {isDemoDataWithoutFile ? (
            <div className="flex flex-col items-center justify-center h-full text-slate-500">
              <FileQuestion size={48} className="mb-4 text-slate-300" />
              <p className="font-medium text-slate-700">该报告暂无关联的 PDF 文件</p>
              <p className="text-sm text-slate-400 mt-2">演示数据仅用于列表展示，不含实际文件</p>
            </div>
          ) : (
            <SmartFilePreview
              key={archiveId} // 当 archiveId 变化时重新挂载组件
              archiveId={archiveId}
              fileId={`${row.title || 'report'}.pdf`} // 传递文件名用于类型检测
              showToolbar={true}
              showFileNav={false}
              className="h-full"
            />
          )}
        </div>
      ),
    },
    {
      key: 'metadata',
      label: '报告信息',
      children: (
        <div className="p-6">
          <div className="space-y-4">
            {/* 无文件提示 */}
            {isDemoDataWithoutFile && (
              <Alert
                type="info"
                showIcon
                message="演示数据"
                description="这是演示数据，仅用于列表展示，不包含实际的 PDF 文件。要测试 PDF 预览功能，请选择其他有文件的报告。"
              />
            )}

            <div className="flex items-center justify-between pb-4 border-b">
              <h3 className="text-lg font-semibold">{reportTypeLabel}</h3>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">报告编号</span>
                <p className="font-medium mt-1">{row.reportNo || row.code || '-'}</p>
              </div>
              <div>
                <span className="text-slate-500">报告期间</span>
                <p className="font-medium mt-1">{row.period || '-'}</p>
              </div>
              <div>
                <span className="text-slate-500">年度</span>
                <p className="font-medium mt-1">{row.year || row.fiscalYear || '-'}</p>
              </div>
              <div>
                <span className="text-slate-500">会计单位</span>
                <p className="font-medium mt-1">{row.unit || row.orgName || '-'}</p>
              </div>
            </div>

            {row.summary && (
              <div className="pt-4 border-t">
                <span className="text-slate-500 text-sm">摘要</span>
                <p className="mt-2 text-sm text-slate-700 whitespace-pre-wrap">{row.summary}</p>
              </div>
            )}
          </div>
        </div>
      ),
    },
  ];

  return (
    <Drawer
      data-testid="financial-report-detail-drawer"
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
            <div className="p-2 bg-emerald-100 text-emerald-600 rounded-lg">
              <FileText size={18} />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-800">{reportTypeLabel}</h3>
              <p className="text-xs text-slate-500">{row.title || row.code || row.id}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
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

export default FinancialReportDetailDrawer;
