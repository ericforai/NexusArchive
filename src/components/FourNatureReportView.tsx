import React from 'react';
import { CheckCircle, XCircle, Shield, FileCheck, Lock, Eye, Printer, X } from 'lucide-react';

interface FourNatureReportViewProps {
    onClose: () => void;
}

export const FourNatureReportView: React.FC<FourNatureReportViewProps> = ({ onClose }) => {
    const checkItems = [
        {
            category: '真实性 (Authenticity)',
            icon: <Shield className="w-5 h-5 text-blue-600" />,
            items: [
                { name: '电子签名验证', status: 'pass', detail: '签名有效，证书未过期' },
                { name: '数字摘要校验 (SM3)', status: 'pass', detail: '哈希值匹配，未被篡改' },
                { name: '时间戳有效性', status: 'pass', detail: '授时中心时间戳验证通过' }
            ]
        },
        {
            category: '完整性 (Integrity)',
            icon: <FileCheck className="w-5 h-5 text-emerald-600" />,
            items: [
                { name: '元数据完整性', status: 'pass', detail: '关键元数据 (凭证号, 金额) 齐全' },
                { name: '附件关联性', status: 'pass', detail: '原始凭证与附件关联正确' },
                { name: '数据结构规范性', status: 'pass', detail: '符合 DA/T 94-2022 结构要求' }
            ]
        },
        {
            category: '可用性 (Usability)',
            icon: <Eye className="w-5 h-5 text-purple-600" />,
            items: [
                { name: '文件格式兼容性', status: 'pass', detail: 'OFD/PDF 格式标准，可正常解析' },
                { name: '内容可读性', status: 'pass', detail: '文字/图表清晰，无乱码' }
            ]
        },
        {
            category: '安全性 (Safety)',
            icon: <Lock className="w-5 h-5 text-orange-600" />,
            items: [
                { name: '病毒扫描', status: 'pass', detail: '未发现恶意代码' },
                { name: '访问权限控制', status: 'pass', detail: '权限设置符合安全策略' },
                { name: '敏感词检测', status: 'pass', detail: '未包含违规敏感信息' }
            ]
        }
    ];

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 backdrop-blur-sm">
            <div className="bg-white rounded-xl shadow-2xl w-[800px] max-h-[90vh] overflow-hidden flex flex-col animate-in fade-in zoom-in duration-200">
                {/* Header */}
                <div className="p-6 border-b border-slate-200 flex justify-between items-center bg-slate-50">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-100 rounded-lg">
                            <Shield className="w-6 h-6 text-blue-700" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-slate-900">四性检测报告</h2>
                            <p className="text-sm text-slate-500">依据 GB/T 39677-2020 标准执行</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
                        <X className="w-6 h-6" />
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-50/30">
                    {/* Overall Result */}
                    <div className="bg-emerald-50 border border-emerald-100 rounded-xl p-4 flex items-center justify-between">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center">
                                <CheckCircle className="w-6 h-6 text-emerald-600" />
                            </div>
                            <div>
                                <div className="text-lg font-bold text-emerald-900">检测通过</div>
                                <div className="text-sm text-emerald-700">该批次档案符合电子档案管理规范要求</div>
                            </div>
                        </div>
                        <div className="text-right">
                            <div className="text-sm text-emerald-600">检测时间</div>
                            <div className="font-mono font-medium text-emerald-800">{new Date().toLocaleString()}</div>
                        </div>
                    </div>

                    {/* Detailed Checks */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {checkItems.map((category, idx) => (
                            <div key={idx} className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-100">
                                    {category.icon}
                                    <h3 className="font-bold text-slate-800">{category.category}</h3>
                                </div>
                                <div className="space-y-3">
                                    {category.items.map((item, i) => (
                                        <div key={i} className="flex items-start justify-between group">
                                            <div>
                                                <div className="text-sm font-medium text-slate-700">{item.name}</div>
                                                <div className="text-xs text-slate-500 mt-0.5">{item.detail}</div>
                                            </div>
                                            {item.status === 'pass' ? (
                                                <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0" />
                                            ) : (
                                                <XCircle className="w-4 h-4 text-red-500 shrink-0" />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-slate-200 bg-white flex justify-end gap-3">
                    <button onClick={onClose} className="px-4 py-2 text-slate-700 hover:bg-slate-100 rounded-lg font-medium transition-colors">
                        关闭
                    </button>
                    <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg font-medium shadow-sm transition-colors">
                        <Printer className="w-4 h-4" />
                        打印报告
                    </button>
                </div>
            </div>
        </div>
    );
};
