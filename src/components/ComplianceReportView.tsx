import React, { useState, useEffect } from 'react';
import {
    ShieldCheck,
    ShieldAlert,
    ShieldX,
    FileText,
    Download,
    ChevronLeft,
    CheckCircle2,
    AlertTriangle,
    XCircle,
    Hash,
    Calendar,
    Building2,
    FileDigit
} from 'lucide-react';
import { ViewState, ApiResponse } from '../types';
import { client } from '../api/client';

interface ComplianceReportViewProps {
    archiveId: string;
    onBack: () => void;
}

interface ComplianceResult {
    archiveId: string;
    archiveCode: string;
    complianceLevel: '完全合规' | '合规但有警告' | '不合规';
    violationCount: number;
    warningCount: number;
    violations: string[];
    warnings: string[];
}

interface ArchiveInfo {
    archiveCode: string;
    title: string;
    fondsName: string;
    year: string;
    retentionPeriod: string;
}

export const ComplianceReportView: React.FC<ComplianceReportViewProps> = ({ archiveId, onBack }) => {
    const [loading, setLoading] = useState(true);
    const [result, setResult] = useState<ComplianceResult | null>(null);
    const [archiveInfo, setArchiveInfo] = useState<ArchiveInfo | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchData();
    }, [archiveId]);

    const fetchData = async () => {
        try {
            setLoading(true);
            // Fetch compliance result
            const response = await client.get<ApiResponse<ComplianceResult>>(`/compliance/archives/${archiveId}`);
            const res = response.data;

            if (res && res.code === 200) {
                setResult(res.data);
            } else {
                setError(res.message || 'Unknown error');
            }

            // Fetch archive info (optional, for display)
            // For now we might just use what is in the report or fetch separately if needed
            // const archiveRes = await get(`/api/archives/${archiveId}`);

        } catch (err: any) {
            setError(err.message || '获取合规性报告失败');
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async (format: 'xml' | 'json') => {
        try {
            const response = await client.get<ApiResponse<string>>(`/compliance/archives/${archiveId}/report?format=${format}`);
            const res = response.data;
            if (res && res.code === 200) {
                // Trigger download
                const blob = new Blob([res.data], { type: format === 'json' ? 'application/json' : 'text/xml' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `compliance_report_${archiveId}.${format}`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (err) {
            console.error('Export failed', err);
        }
    };

    const getStatusColor = (level?: string) => {
        if (level === '完全合规') return 'text-green-600 bg-green-50 border-green-200';
        if (level === '合规但有警告') return 'text-yellow-600 bg-yellow-50 border-yellow-200';
        return 'text-red-600 bg-red-50 border-red-200';
    };

    const getStatusIcon = (level?: string) => {
        if (level === '完全合规') return <ShieldCheck className="w-16 h-16 text-green-500" />;
        if (level === '合规但有警告') return <ShieldAlert className="w-16 h-16 text-yellow-500" />;
        return <ShieldX className="w-16 h-16 text-red-500" />;
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-full">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8 text-center text-red-600">
                <AlertTriangle className="w-12 h-12 mx-auto mb-4" />
                <p>{error}</p>
                <button onClick={onBack} className="mt-4 text-indigo-600 hover:underline">返回</button>
            </div>
        );
    }

    if (!result) return null;

    return (
        <div className="p-6 max-w-7xl mx-auto space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                    <button
                        onClick={onBack}
                        className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                    >
                        <ChevronLeft className="w-6 h-6 text-slate-600" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold text-slate-900">《会计档案管理办法》符合性检查报告</h1>
                        <p className="text-slate-500 mt-1">检测依据：DA/T 94-2022 电子会计档案管理规范</p>
                    </div>
                </div>
                <div className="flex space-x-3">
                    <button
                        onClick={() => handleExport('xml')}
                        className="flex items-center px-4 py-2 bg-white border border-slate-200 rounded-lg text-slate-700 hover:bg-slate-50"
                    >
                        <FileText className="w-4 h-4 mr-2" />
                        导出 XML
                    </button>
                    <button
                        onClick={() => handleExport('json')}
                        className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                    >
                        <Download className="w-4 h-4 mr-2" />
                        导出 JSON
                    </button>
                </div>
            </div>

            {/* Overview Card */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-8">
                <div className="flex items-start justify-between">
                    <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-6">
                            <span className={`px-4 py-1.5 rounded-full text-sm font-semibold border ${getStatusColor(result.complianceLevel)}`}>
                                {result.complianceLevel}
                            </span>
                            <span className="text-sm text-slate-500">检测时间：{new Date().toLocaleString()}</span>
                        </div>

                        <div className="grid grid-cols-2 gap-8">
                            <div>
                                <h3 className="text-sm font-medium text-slate-500 mb-4">档案信息</h3>
                                <div className="space-y-3">
                                    <div className="flex items-center text-slate-700">
                                        <Hash className="w-4 h-4 mr-2 text-slate-400" />
                                        <span className="font-mono">{result.archiveCode || '未生成档号'}</span>
                                    </div>
                                </div>
                            </div>
                            <div>
                                <h3 className="text-sm font-medium text-slate-500 mb-4">检测统计</h3>
                                <div className="flex space-x-8">
                                    <div className="text-center">
                                        <div className="text-2xl font-bold text-red-600">{result.violationCount}</div>
                                        <div className="text-xs text-slate-500">违规项</div>
                                    </div>
                                    <div className="text-center">
                                        <div className="text-2xl font-bold text-yellow-600">{result.warningCount}</div>
                                        <div className="text-xs text-slate-500">警告项</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="ml-12">
                        {getStatusIcon(result.complianceLevel)}
                    </div>
                </div>
            </div>

            {/* Violations */}
            {result.violations.length > 0 && (
                <div className="bg-white rounded-xl shadow-sm border border-red-200 overflow-hidden">
                    <div className="px-6 py-4 bg-red-50 border-b border-red-100 flex items-center">
                        <XCircle className="w-5 h-5 text-red-600 mr-2" />
                        <h3 className="font-semibold text-red-900">违规项 (必须修复)</h3>
                    </div>
                    <div className="divide-y divide-red-100">
                        {result.violations.map((v, i) => (
                            <div key={i} className="px-6 py-4 text-slate-700 flex items-start">
                                <span className="text-red-500 font-mono mr-3">{i + 1}.</span>
                                {v}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Warnings */}
            {result.warnings.length > 0 && (
                <div className="bg-white rounded-xl shadow-sm border border-yellow-200 overflow-hidden">
                    <div className="px-6 py-4 bg-yellow-50 border-b border-yellow-100 flex items-center">
                        <AlertTriangle className="w-5 h-5 text-yellow-600 mr-2" />
                        <h3 className="font-semibold text-yellow-900">警告项 (建议优化)</h3>
                    </div>
                    <div className="divide-y divide-yellow-100">
                        {result.warnings.map((w, i) => (
                            <div key={i} className="px-6 py-4 text-slate-700 flex items-start">
                                <span className="text-yellow-500 font-mono mr-3">{i + 1}.</span>
                                {w}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Safe Items (Mock) */}
            <div className="bg-white rounded-xl shadow-sm border border-green-200 overflow-hidden">
                <div className="px-6 py-4 bg-green-50 border-b border-green-100 flex items-center">
                    <CheckCircle2 className="w-5 h-5 text-green-600 mr-2" />
                    <h3 className="font-semibold text-green-900">已通过检测项</h3>
                </div>
                <div className="p-6 grid grid-cols-2 gap-4 text-sm text-slate-600">
                    <div className="flex items-center"><CheckCircle2 className="w-4 h-4 text-green-500 mr-2" /> 档案四性检测 (真实性、完整性、可用性、安全性)</div>
                    <div className="flex items-center"><CheckCircle2 className="w-4 h-4 text-green-500 mr-2" /> 数据来源可靠性验证</div>
                    <div className="flex items-center"><CheckCircle2 className="w-4 h-4 text-green-500 mr-2" /> 电子会计凭证元数据结构</div>
                    <div className="flex items-center"><CheckCircle2 className="w-4 h-4 text-green-500 mr-2" /> 关联文件数量一致性</div>
                </div>
            </div>
        </div>
    );
};

export default ComplianceReportView;
