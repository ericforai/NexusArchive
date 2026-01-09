// Input: React、lucide-react
// Output: FilterModal 筛选弹窗组件
// Pos: 通用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { X } from 'lucide-react';

/**
 * 筛选弹窗组件属性
 */
export interface FilterModalProps {
    /** 是否打开 */
    isOpen: boolean;
    /** 关闭回调 */
    onClose: () => void;
    /** 重置回调 */
    onReset: () => void;
    /** 确定回调（可选，默认调用 onClose） */
    onConfirm?: () => void;
    /** 标题（可选，默认"筛选条件"） */
    title?: string;
    /** 筛选器内容 */
    children: React.ReactNode;
}

/**
 * 筛选弹窗组件
 *
 * 统一的筛选弹窗样式，确保内容在一屏内完整显示
 */
export const FilterModal: React.FC<FilterModalProps> = ({
    isOpen,
    onClose,
    onReset,
    onConfirm,
    title = '筛选条件',
    children,
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-start justify-center pt-16 p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
                {/* Header */}
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <h3 className="text-lg font-bold text-slate-800">{title}</h3>
                    <button onClick={onClose}>
                        <X size={20} className="text-slate-400 hover:text-slate-600" />
                    </button>
                </div>

                {/* Content */}
                <div className="p-6 space-y-4">
                    {children}
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                    <button
                        onClick={onReset}
                        className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg"
                    >
                        重置
                    </button>
                    <button
                        onClick={() => {
                            onConfirm?.();
                            onClose();
                        }}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                    >
                        确定
                    </button>
                </div>
            </div>
        </div>
    );
};

export default FilterModal;
