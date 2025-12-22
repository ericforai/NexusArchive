// Input: React、lucide-react 图标、本地模块 common/DemoBadge
// Output: React 组件 LinkModal
// Pos: 归档流程组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { X } from 'lucide-react';
import { DemoBadge } from '../common/DemoBadge';

interface LinkCandidate {
    id: string;
    type?: string;
    code: string;
    name: string;
    amount?: string;
    date?: string;
    score: number;
}

interface LinkModalProps {
    isOpen: boolean;
    onClose: () => void;
    candidates: LinkCandidate[];
    selectedIds: string[];
    onToggleSelection: (id: string) => void;
    onConfirm: () => void;
    demoMode: boolean;
}

/**
 * 手动凭证关联模态框组件
 * 
 * 从 ArchiveListView 拆分出来
 */
export const LinkModal: React.FC<LinkModalProps> = ({
    isOpen,
    onClose,
    candidates,
    selectedIds,
    onToggleSelection,
    onConfirm,
    demoMode
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-100 flex flex-col max-h-[90vh]">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <h3 className="text-lg font-bold text-slate-800">手动凭证关联</h3>
                    <button onClick={onClose}><X size={20} className="text-slate-400" /></button>
                </div>
                <div className="p-6 overflow-y-auto flex-1 space-y-2">
                    <DemoBadge text="关联候选为演示数据，接入真实关联接口后可关闭演示模式。" />
                    {demoMode ? (
                        candidates.map(c => (
                            <div
                                key={c.id}
                                onClick={() => onToggleSelection(c.id)}
                                className={`flex justify-between p-3 rounded-xl border cursor-pointer ${selectedIds.includes(c.id) ? 'border-primary-500 bg-primary-50' : 'border-slate-200'}`}
                            >
                                <div>
                                    <p className="font-bold">{c.name}</p>
                                    <p className="text-xs text-slate-500">{c.code}</p>
                                </div>
                                <div className="text-right">
                                    <span className={`font-bold ${c.score > 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{c.score}%</span>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="text-sm text-slate-500">当前为生产模式，未接入关联候选接口。</div>
                    )}
                </div>
                <div className="p-6 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                    <button onClick={onClose} className="px-4 py-2 text-slate-600">取消</button>
                    <button
                        onClick={onConfirm}
                        disabled={!demoMode || selectedIds.length === 0}
                        className="px-6 py-2 bg-primary-600 text-white rounded-lg disabled:opacity-50"
                    >
                        确认关联
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LinkModal;
