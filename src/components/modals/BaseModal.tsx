// Input: React, lucide-react 图标, react-dom
// Output: BaseModal 组件
// Pos: 通用复用组件 - 模态框基础组件

import React, { ReactNode } from 'react';
import { X } from 'lucide-react';
import { createPortal } from 'react-dom';

export interface BaseModalProps {
  /** 是否打开模态框 */
  isOpen: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 标题 */
  title?: string;
  /** 自定义 header */
  header?: ReactNode;
  /** 子内容 */
  children: ReactNode;
  /** 自定义 footer */
  footer?: ReactNode;
  /** 最大宽度 */
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl' | 'full';
  /** 是否显示关闭按钮 */
  showCloseButton?: boolean;
  /** 点击 backdrop 是否关闭 */
  closeOnBackdropClick?: boolean;
  /** 自定义类名 */
  className?: string;
}

const maxWidthClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
  '2xl': 'max-w-2xl',
  '4xl': 'max-w-4xl',
  full: 'max-w-full',
};

export const BaseModal: React.FC<BaseModalProps> = ({
  isOpen,
  onClose,
  title,
  header,
  children,
  footer,
  maxWidth = 'lg',
  showCloseButton = true,
  closeOnBackdropClick = true,
  className = '',
}) => {
  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (closeOnBackdropClick && e.target === e.currentTarget) {
      onClose();
    }
  };

  const content = (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        data-testid="modal-backdrop"
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={handleBackdropClick}
      />

      {/* Modal */}
      <div
        className={`relative bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full ${maxWidthClasses[maxWidth]} mx-4 animate-in fade-in zoom-in-95 duration-200 ${className}`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        {(title || header) && (
          <div className="flex items-center justify-between p-5 border-b border-slate-200 dark:border-slate-700">
            {header || (
              <>
                <h3 className="text-lg font-semibold text-slate-800 dark:text-white">{title}</h3>
                {showCloseButton && (
                  <button
                    onClick={onClose}
                    className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                    type="button"
                  >
                    <X size={20} />
                  </button>
                )}
              </>
            )}
          </div>
        )}

        {/* Content */}
        <div className="p-5">
          {children}
        </div>

        {/* Footer */}
        {footer && (
          <div className="flex items-center justify-end gap-3 p-5 border-t border-slate-200 dark:border-slate-700">
            {footer}
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(content, document.body);
};

export default BaseModal;
