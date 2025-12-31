// Input: React, lucide-react 图标
// Output: ConfirmModal 组件
// Pos: 通用复用组件 - 确认对话框

import React from 'react';
import { AlertTriangle, Info, CheckCircle, XCircle } from 'lucide-react';
import { BaseModal, BaseModalProps } from './BaseModal';

export type ConfirmVariant = 'info' | 'success' | 'warning' | 'danger';

export interface ConfirmModalProps extends Omit<BaseModalProps, 'footer' | 'children'> {
  /** 确认按钮文本 */
  confirmText?: string;
  /** 取消按钮文本 */
  cancelText?: string;
  /** 确认回调 */
  onConfirm: () => void | Promise<void>;
  /** 确认中状态 */
  isConfirming?: boolean;
  /** 变体类型 */
  variant?: ConfirmVariant;
  /** 内容描述 */
  description?: string;
}

const variantConfig = {
  info: {
    icon: Info,
    iconBg: 'bg-blue-100 dark:bg-blue-900/30',
    iconColor: 'text-blue-600 dark:text-blue-400',
    buttonClass: 'bg-blue-600 hover:bg-blue-700',
  },
  success: {
    icon: CheckCircle,
    iconBg: 'bg-green-100 dark:bg-green-900/30',
    iconColor: 'text-green-600 dark:text-green-400',
    buttonClass: 'bg-green-600 hover:bg-green-700',
  },
  warning: {
    icon: AlertTriangle,
    iconBg: 'bg-amber-100 dark:bg-amber-900/30',
    iconColor: 'text-amber-600 dark:text-amber-400',
    buttonClass: 'bg-amber-600 hover:bg-amber-700',
  },
  danger: {
    icon: XCircle,
    iconBg: 'bg-rose-100 dark:bg-rose-900/30',
    iconColor: 'text-rose-600 dark:text-rose-400',
    buttonClass: 'bg-rose-600 hover:bg-rose-700',
  },
};

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  confirmText = '确认',
  cancelText = '取消',
  onConfirm,
  isConfirming = false,
  variant = 'info',
  description,
  ...baseProps
}) => {
  const config = variantConfig[variant];
  const Icon = config.icon;

  const handleConfirm = async () => {
    await onConfirm();
  };

  const footer = (
    <>
      <button
        type="button"
        onClick={baseProps.onClose}
        className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors"
        disabled={isConfirming}
      >
        {cancelText}
      </button>
      <button
        type="button"
        onClick={handleConfirm}
        disabled={isConfirming}
        className={`px-4 py-2 text-white rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${config.buttonClass}`}
      >
        {confirmText}
      </button>
    </>
  );

  return (
    <BaseModal {...baseProps} footer={footer} maxWidth="md">
      <div className="flex items-start gap-4">
        <div className={`p-3 rounded-lg ${config.iconBg} ${config.iconColor} shrink-0`}>
          <Icon size={24} />
        </div>
        <div className="flex-1">
          <h4 className="text-lg font-semibold text-slate-800 dark:text-white mb-2">
            {baseProps.title}
          </h4>
          {description && (
            <p className="text-slate-600 dark:text-slate-300">
              {description}
            </p>
          )}
        </div>
      </div>
    </BaseModal>
  );
};

export default ConfirmModal;
