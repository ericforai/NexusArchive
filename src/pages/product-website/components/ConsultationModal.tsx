// Input: isOpen 状态、onClose 回调
// Output: 咨询模态框组件
// Pos: src/pages/product-website/components/ConsultationModal.tsx

import React, { useEffect } from 'react';
import { X, Phone, Clock } from 'lucide-react';

interface ConsultationModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ConsultationModal: React.FC<ConsultationModalProps> = ({ isOpen, onClose }) => {
  // ESC 键关闭
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  // 禁止背景滚动
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
      onClick={handleBackdropClick}
    >
      <div className="relative w-full max-w-md bg-[#0B1120] border border-slate-800 rounded-2xl shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* 关闭按钮 */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white transition-colors"
          aria-label="关闭"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="p-8 text-center">
          {/* 标题 */}
          <h3 className="text-2xl font-bold text-white mb-2">联系专家咨询</h3>
          <p className="text-slate-400 mb-8">我们随时为您提供专业的电子档案管理咨询服务</p>

          {/* 手机号 - 超大突出显示 */}
          <div className="mb-8">
            <a
              href="tel:15317270756"
              className="flex items-center justify-center gap-3 group"
            >
              <Phone className="w-10 h-10 text-cyan-400" />
              <span className="text-5xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500 group-hover:from-cyan-300 group-hover:to-blue-400 transition-all">
                153-1727-0756
              </span>
            </a>
          </div>

          {/* 一键拨打按钮 */}
          <a
            href="tel:15317270756"
            className="inline-flex items-center gap-2 px-8 py-4 bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-bold text-lg rounded-xl shadow-[0_0_20px_rgba(16,185,129,0.4)] hover:shadow-[0_0_30px_rgba(16,185,129,0.6)] transition-all transform hover:scale-105"
          >
            <Phone className="w-5 h-5" />
            一键拨打
          </a>

          {/* 工作时间 */}
          <div className="mt-8 flex items-center justify-center gap-2 text-slate-500">
            <Clock className="w-4 h-4" />
            <span className="text-sm">7×24 小时服务</span>
          </div>
        </div>
      </div>
    </div>
  );
};
