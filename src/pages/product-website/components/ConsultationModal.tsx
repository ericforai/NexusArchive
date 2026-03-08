// Input: 无（独立组件）
// Output: 咨询联系方式模态框
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
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  // 阻止背景滚动
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

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      onClick={onClose}
    >
      {/* 背景遮罩 */}
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200" />

      {/* 模态框内容 */}
      <div
        className="relative bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl max-w-md w-full animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 关闭按钮 */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* 标题 */}
        <div className="p-8 pb-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl flex items-center justify-center">
              <Phone className="w-6 h-6 text-white" />
            </div>
            <h2 className="text-2xl font-bold text-white">预约专家顾问</h2>
          </div>
          <p className="text-slate-400 mt-4">
            我们的专业顾问将为您提供电子会计档案管理系统的定制化解决方案
          </p>
        </div>

        {/* 联系方式 */}
        <div className="px-8 pb-8">
          {/* 手机号 - 超大突出 */}
          <a
            href="tel:15317270756"
            className="block p-8 bg-slate-800/50 hover:bg-slate-800 border-2 border-cyan-500/30 hover:border-cyan-500 rounded-2xl transition-all group text-center mb-4"
          >
            <Phone className="w-10 h-10 text-cyan-400 mx-auto mb-4" />
            <p className="text-sm text-slate-400 mb-2">咨询热线</p>
            <p className="text-4xl font-bold text-white tracking-wider mb-6 group-hover:text-cyan-400 transition-colors">
              153-1727-0756
            </p>
            <div className="inline-flex items-center gap-2 px-8 py-4 bg-emerald-500 hover:bg-emerald-400 text-slate-900 font-bold rounded-xl transition-all shadow-lg">
              <Phone className="w-5 h-5" />
              一键拨打
            </div>
          </a>

          {/* 工作时间 */}
          <div className="flex items-center justify-center gap-3 p-4 bg-slate-800/30 rounded-xl border border-slate-700/50">
            <Clock className="w-5 h-5 text-amber-400" />
            <p className="text-slate-300">7×24小时服务</p>
          </div>
        </div>

        {/* 底部提示 */}
        <div className="px-8 pb-4">
          <p className="text-sm text-slate-500 text-center">
            点击上方按钮直接拨打，或在工作时间内联系我们
          </p>
        </div>
      </div>
    </div>
  );
};
