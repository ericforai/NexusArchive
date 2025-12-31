// Input: React, lucide-react 图标
// Output: DetailModal 组件
// Pos: 通用复用组件 - 详情模态框

import React, { ReactNode } from 'react';
import { FileText } from 'lucide-react';
import { BaseModal, BaseModalProps } from './BaseModal';

export interface DetailModalProps extends BaseModalProps {
  /** 副标题 */
  subtitle?: string;
  /** 详情内容 */
  details?: ReactNode;
  /** 右侧操作按钮 */
  actions?: ReactNode;
}

export const DetailModal: React.FC<DetailModalProps> = ({
  title,
  subtitle,
  details,
  actions,
  children,
  ...baseProps
}) => {
  const header = (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
          <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-slate-800 dark:text-white">{title}</h3>
          {subtitle && (
            <p className="text-sm text-slate-500 dark:text-slate-400 truncate max-w-[280px]">{subtitle}</p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-2">
        {actions}
        <button
          onClick={baseProps.onClose}
          className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          type="button"
        >
          ×
        </button>
      </div>
    </div>
  );

  return (
    <BaseModal {...baseProps} header={header} maxWidth="4xl">
      {details}
      {children}
    </BaseModal>
  );
};

export default DetailModal;
