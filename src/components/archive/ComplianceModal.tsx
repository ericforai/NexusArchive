import React from 'react';
import { X, ShieldCheck, CheckCircle2 } from 'lucide-react';

interface ComplianceModalProps {
    isOpen: boolean;
    onClose: () => void;
    dataCount: number;
}

/**
 * 四性检测模态框组件
 * 
 * 从 ArchiveListView 拆分出来
 */
export const ComplianceModal: React.FC<ComplianceModalProps> = ({
    isOpen,
    onClose,
    dataCount
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl border border-slate-100">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-indigo-100 rounded-lg text-indigo-600"><ShieldCheck size={24} /></div>
                        <div>
                            <h3 className="text-lg font-bold text-slate-800">四性检测智能诊断报告</h3>
                            <p className="text-xs text-slate-500">依据标准 DA/T 70-2018 执行检测</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
                </div>

                <div className="p-8 grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* Overall Score */}
                    <div className="md:col-span-1 flex flex-col items-center justify-center bg-slate-50 rounded-2xl p-6 border border-slate-100">
                        <div className="relative w-32 h-32 flex items-center justify-center">
                            <svg className="w-full h-full transform -rotate-90">
                                <circle cx="64" cy="64" r="60" stroke="#e2e8f0" strokeWidth="8" fill="transparent" />
                                <circle cx="64" cy="64" r="60" stroke="#4f46e5" strokeWidth="8" fill="transparent" strokeDasharray="377" strokeDashoffset="37.7" strokeLinecap="round" />
                            </svg>
                            <div className="absolute flex flex-col items-center">
                                <span className="text-4xl font-bold text-indigo-600">90</span>
                                <span className="text-xs font-bold text-slate-400 uppercase">综合得分</span>
                            </div>
                        </div>
                        <p className="text-sm text-center mt-4 text-slate-600">检测对象: <span className="font-bold">{dataCount}</span> 份档案<br />状态: <span className="text-emerald-600 font-bold">通过</span></p>
                    </div>

                    {/* Details */}
                    <div className="md:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {[
                            { label: '真实性', score: 98, color: 'bg-emerald-500', items: ['电子签名有效', '时间戳完整'] },
                            { label: '完整性', score: 100, color: 'bg-blue-500', items: ['元数据齐全', '附件无缺失'] },
                            { label: '可用性', score: 95, color: 'bg-cyan-500', items: ['格式标准兼容', '索引库正常'] },
                            { label: '安全性', score: 85, color: 'bg-amber-500', items: ['病毒扫描通过', '权限配置待优化'] },
                        ].map((item, idx) => (
                            <div key={idx} className="border border-slate-100 rounded-xl p-4 hover:shadow-sm transition-shadow">
                                <div className="flex justify-between items-center mb-2">
                                    <span className="font-bold text-slate-700">{item.label}</span>
                                    <span className={`text-xs font-bold text-white px-2 py-0.5 rounded-full ${item.color}`}>{item.score}</span>
                                </div>
                                <div className="w-full bg-slate-100 h-1.5 rounded-full mb-3">
                                    <div className={`h-full rounded-full ${item.color}`} style={{ width: `${item.score}%` }}></div>
                                </div>
                                <ul className="space-y-1">
                                    {item.items.map((check, i) => (
                                        <li key={i} className="text-xs text-slate-500 flex items-center gap-1.5">
                                            <CheckCircle2 size={10} className="text-emerald-500" /> {check}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end">
                    <button onClick={onClose} className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 shadow-lg shadow-indigo-500/20 transition-all active:scale-95">生成详细报告</button>
                </div>
            </div>
        </div>
    );
};

export default ComplianceModal;
