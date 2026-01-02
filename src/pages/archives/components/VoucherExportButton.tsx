// src/pages/archives/components/VoucherExportButton.tsx
/**
 * 凭证 AIP 导出按钮
 *
 * 职责：处理 AIP 导出的 UI 和逻辑
 * 变更理由：导出功能变化
 */

import React from 'react';
import { Package, Loader2 } from 'lucide-react';

interface VoucherExportButtonProps {
  onExport: () => void;
  isExporting?: boolean;
  label?: string;
}

export const VoucherExportButton: React.FC<VoucherExportButtonProps> = ({
  onExport,
  isExporting = false,
  label = '导出AIP',
}) => {
  return (
    <button
      onClick={onExport}
      disabled={isExporting}
      className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:text-emerald-700 hover:border-emerald-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
    >
      {isExporting ? (
        <Loader2 size={14} className="animate-spin" />
      ) : (
        <Package size={14} />
      )}
      {isExporting ? '导出中...' : label}
    </button>
  );
};
