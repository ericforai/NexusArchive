// Input: React
// Output: FormModal 组件
// Pos: 通用复用组件 - 表单模态框

import React, { ReactNode } from 'react';
import { BaseModal, BaseModalProps } from './BaseModal';

export interface FormModalProps extends BaseModalProps {
  /** 提交按钮文本 */
  submitText?: string;
  /** 取消按钮文本 */
  cancelText?: string;
  /** 提交中状态 */
  isSubmitting?: boolean;
  /** 表单提交回调 */
  onSubmit: (e: React.FormEvent) => void | Promise<void>;
  /** 错误信息 */
  error?: string | null;
}

export const FormModal: React.FC<FormModalProps> = ({
  submitText = '提交',
  cancelText = '取消',
  isSubmitting = false,
  onSubmit,
  error,
  children,
  ...baseProps
}) => {
  const footer = (
    <>
      <button
        type="button"
        onClick={baseProps.onClose}
        className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors"
        disabled={isSubmitting}
      >
        {cancelText}
      </button>
      <button
        type="submit"
        form="modal-form"
        disabled={isSubmitting}
        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {submitText}
      </button>
    </>
  );

  return (
    <BaseModal {...baseProps} footer={footer}>
      <form id="modal-form" onSubmit={onSubmit} className="space-y-4">
        {children}
        {error && (
          <div className="flex items-center gap-2 p-3 bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400 rounded-xl text-sm">
            {error}
          </div>
        )}
      </form>
    </BaseModal>
  );
};

export default FormModal;
