// Input: React、lucide-react 图标
// Output: React 组件 MatchPreviewModal
// Pos: src/pages/archives/MatchPreviewModal.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { X, CheckCircle2, Zap, Receipt, AlertTriangle } from 'lucide-react';

interface MatchPreviewRow {
    voucherId?: string;
    voucherNo?: string;
    date?: string;
    amount?: string;
    matchScore?: number;
    _previewStatus?: 'high' | 'medium';
    _proposedLinks?: Array<{ code: string; docId: string; evidenceRole: string; linkType: string }>;
}

interface MatchPreviewModalProps {
    isOpen: boolean;
    onClose: () => void;
    data: MatchPreviewRow[];
    onConfirm: () => void;
}

/**
 * 智能匹配预览模态框组件
 * 
 * 从 ArchiveListView 拆分出来
 */
export const MatchPreviewModal: React.FC<MatchPreviewModalProps> = ({
    isOpen,
    onClose,
    data,
    onConfirm
}) => {
    if (!isOpen) return null;

    const filteredData = data.filter(r => r._previewStatus);

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl border border-slate-100 flex flex-col max-h-[85vh]">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                            <Zap size={20} className="text-amber-500" /> 智能匹配结果预演
                        </h3>
                        <p className="text-xs text-slate-500 mt-1">请确认以下匹配建议，高置信度将自动关联，疑似匹配需人工审核</p>
                    </div>
                    <button onClick={onClose}><X size={20} className="text-slate-400" /></button>
                </div>

                <div className="flex-1 overflow-auto p-0">
                    <table className="w-full text-left border-collapse">
                        <thead className="sticky top-0 bg-slate-50 z-10 shadow-sm">
                            <tr>
                                <th className="p-4 text-xs font-medium text-slate-500">凭证号</th>
                                <th className="p-4 text-xs font-medium text-slate-500">摘要/金额</th>
                                <th className="p-4 text-xs font-medium text-slate-500">推荐关联</th>
                                <th className="p-4 text-xs font-medium text-slate-500">匹配度</th>
                                <th className="p-4 text-xs font-medium text-slate-500">建议操作</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filteredData.map((row, idx) => (
                                <tr key={idx} className="hover:bg-slate-50">
                                    <td className="p-4 font-medium text-slate-700">{row.voucherNo}</td>
                                    <td className="p-4">
                                        <div className="text-xs text-slate-500">{row.date}</div>
                                        <div className="font-mono font-bold text-slate-700">{row.amount}</div>
                                    </td>
                                    <td className="p-4">
                                        {row._proposedLinks?.map((link, i) => (
                                            <div key={i} className="flex items-center gap-2 text-xs bg-slate-100 px-2 py-1 rounded mb-1">
                                                <Receipt size={12} className="text-slate-400" />
                                                <span>{link.code}</span>
                                            </div>
                                        ))}
                                    </td>
                                    <td className="p-4">
                                        <div className="flex items-center gap-2">
                                            <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                                                <div className={`h-full rounded-full ${(row.matchScore || 0) >= 90 ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${row.matchScore}%` }}></div>
                                            </div>
                                            <span className={`text-xs font-bold ${(row.matchScore || 0) >= 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{row.matchScore}%</span>
                                        </div>
                                    </td>
                                    <td className="p-4">
                                        {row._previewStatus === 'high' ? (
                                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium border border-emerald-100">
                                                <CheckCircle2 size={12} /> 自动关联
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-amber-50 text-amber-600 text-xs font-medium border border-amber-100">
                                                <AlertTriangle size={12} /> 需人工确认
                                            </span>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {filteredData.length === 0 && (
                                <tr><td colSpan={5} className="p-8 text-center text-slate-400">本次运行未发现新的匹配项</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-3">
                    <button onClick={onClose} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">取消</button>
                    <button
                        onClick={onConfirm}
                        className="px-6 py-2 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 shadow-lg shadow-primary-500/30 transition-all active:scale-95 flex items-center gap-2"
                        disabled={filteredData.length === 0}
                    >
                        <CheckCircle2 size={18} /> 确认并应用
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MatchPreviewModal;
