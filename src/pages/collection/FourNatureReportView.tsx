// Input: React、lucide-react 图标、API
// Output: React 组件 FourNatureReportView
// Pos: src/pages/collection/FourNatureReportView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

'use client';

import React, { useEffect, useState } from 'react';
import { CheckCircle, XCircle, AlertTriangle, Shield, FileCheck, Lock, Eye, Printer, X, Loader2 } from 'lucide-react';

interface CheckItem {
    name: string;
    status: 'PASS' | 'FAIL' | 'WARNING';
    message: string;
    errors?: string[];
}

interface FourNatureReport {
    checkId: string;
    checkTime: string;
    archivalCode?: string;
    status: 'PASS' | 'FAIL' | 'WARNING';
    authenticity?: CheckItem;
    integrity?: CheckItem;
    usability?: CheckItem;
    safety?: CheckItem;
}

interface FourNatureReportViewProps {
    fileId: string;
    onClose: () => void;
}

// API 基础路径
const API_BASE = process.env.NEXT_PUBLIC_API_BASE || '/api';

export const FourNatureReportView: React.FC<FourNatureReportViewProps> = ({ fileId, onClose }) => {
    const [report, setReport] = useState<FourNatureReport | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchReport = async () => {
            try {
                setLoading(true);
                setError(null);
                
                const token = localStorage.getItem('token');
                const response = await fetch(`${API_BASE}/pool/check/${fileId}`, {
                    headers: {
                        'Authorization': token ? `Bearer ${token}` : '',
                        'Content-Type': 'application/json'
                    }
                });
                
                if (!response.ok) {
                    throw new Error(`检测请求失败: ${response.status}`);
                }
                
                const result = await response.json();
                if (result.code === 200 && result.data) {
                    setReport(result.data);
                } else {
                    throw new Error(result.message || '未知错误');
                }
            } catch (err) {
                setError(err instanceof Error ? err.message : '网络请求失败');
            } finally {
                setLoading(false);
            }
        };

        if (fileId) {
            fetchReport();
        }
    }, [fileId]);

    const getStatusIcon = (status?: string) => {
        switch (status) {
            case 'PASS':
                return <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0" />;
            case 'FAIL':
                return <XCircle className="w-4 h-4 text-red-500 shrink-0" />;
            case 'WARNING':
                return <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />;
            default:
                return <CheckCircle className="w-4 h-4 text-slate-400 shrink-0" />;
        }
    };

    const getOverallStatusConfig = (status?: string) => {
        switch (status) {
            case 'PASS':
                return { bg: 'bg-emerald-50', border: 'border-emerald-100', text: 'text-emerald-900', icon: CheckCircle, iconBg: 'bg-emerald-100', iconColor: 'text-emerald-600', label: '检测通过' };
            case 'FAIL':
                return { bg: 'bg-red-50', border: 'border-red-100', text: 'text-red-900', icon: XCircle, iconBg: 'bg-red-100', iconColor: 'text-red-600', label: '检测失败' };
            case 'WARNING':
                return { bg: 'bg-amber-50', border: 'border-amber-100', text: 'text-amber-900', icon: AlertTriangle, iconBg: 'bg-amber-100', iconColor: 'text-amber-600', label: '检测警告' };
            default:
                return { bg: 'bg-slate-50', border: 'border-slate-100', text: 'text-slate-900', icon: Shield, iconBg: 'bg-slate-100', iconColor: 'text-slate-600', label: '检测中' };
        }
    };

    const renderCheckCategory = (title: string, icon: React.ReactNode, item?: CheckItem) => {
        if (!item) return null;
        
        return (
            <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow">
                <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-100">
                    {icon}
                    <h3 className="font-bold text-slate-800">{title}</h3>
                    {getStatusIcon(item.status)}
                </div>
                <div className="space-y-3">
                    <div className="flex items-start justify-between group">
                        <div className="flex-1">
                            <div className="text-sm font-medium text-slate-700">{item.name || title}</div>
                            <div className="text-xs text-slate-500 mt-0.5">{item.message}</div>
                            {item.errors && item.errors.length > 0 && (
                                <ul className="mt-2 text-xs text-red-600 list-disc list-inside">
                                    {item.errors.map((err, i) => (
                                        <li key={i}>{err}</li>
                                    ))}
                                </ul>
                            )}
                        </div>
                        {getStatusIcon(item.status)}
                    </div>
                </div>
            </div>
        );
    };

    const statusConfig = getOverallStatusConfig(report?.status);
    const StatusIcon = statusConfig.icon;

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
                            <p className="text-sm text-slate-500">依据 DA/T 92-2022 标准执行</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
                        <X className="w-6 h-6" />
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-50/30">
                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-12">
                            <Loader2 className="w-10 h-10 text-blue-600 animate-spin" />
                            <p className="mt-4 text-slate-600">正在执行四性检测...</p>
                        </div>
                    ) : error ? (
                        <div className="bg-red-50 border border-red-100 rounded-xl p-4 flex items-center gap-4">
                            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
                                <XCircle className="w-6 h-6 text-red-600" />
                            </div>
                            <div>
                                <div className="text-lg font-bold text-red-900">检测失败</div>
                                <div className="text-sm text-red-700">{error}</div>
                            </div>
                        </div>
                    ) : report ? (
                        <>
                            {/* Overall Result */}
                            <div className={`${statusConfig.bg} border ${statusConfig.border} rounded-xl p-4 flex items-center justify-between`}>
                                <div className="flex items-center gap-4">
                                    <div className={`w-12 h-12 rounded-full ${statusConfig.iconBg} flex items-center justify-center`}>
                                        <StatusIcon className={`w-6 h-6 ${statusConfig.iconColor}`} />
                                    </div>
                                    <div>
                                        <div className={`text-lg font-bold ${statusConfig.text}`}>{statusConfig.label}</div>
                                        <div className={`text-sm ${statusConfig.iconColor}`}>
                                            {report.archivalCode || '待归档文件'}
                                        </div>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <div className={`text-sm ${statusConfig.iconColor}`}>检测时间</div>
                                    <div className={`font-mono font-medium ${statusConfig.text}`}>
                                        {report.checkTime ? new Date(report.checkTime).toLocaleString() : '-'}
                                    </div>
                                </div>
                            </div>

                            {/* Detailed Checks */}
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {renderCheckCategory(
                                    '真实性 (Authenticity)',
                                    <Shield className="w-5 h-5 text-blue-600" />,
                                    report.authenticity
                                )}
                                {renderCheckCategory(
                                    '完整性 (Integrity)',
                                    <FileCheck className="w-5 h-5 text-emerald-600" />,
                                    report.integrity
                                )}
                                {renderCheckCategory(
                                    '可用性 (Usability)',
                                    <Eye className="w-5 h-5 text-purple-600" />,
                                    report.usability
                                )}
                                {renderCheckCategory(
                                    '安全性 (Safety)',
                                    <Lock className="w-5 h-5 text-orange-600" />,
                                    report.safety
                                )}
                            </div>
                        </>
                    ) : null}
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-slate-200 bg-white flex justify-end gap-3">
                    <button onClick={onClose} className="px-4 py-2 text-slate-700 hover:bg-slate-100 rounded-lg font-medium transition-colors">
                        关闭
                    </button>
                    <button 
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg font-medium shadow-sm transition-colors disabled:opacity-50"
                        disabled={loading || !!error}
                        onClick={() => window.print()}
                    >
                        <Printer className="w-4 h-4" />
                        打印报告
                    </button>
                </div>
            </div>
        </div>
    );
};
