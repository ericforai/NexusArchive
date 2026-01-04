// Input: React、lucide-react、auditVerificationApi、useFondsStore
// Output: AuditEvidencePackagePage 组件
// Pos: 审计证据包导出页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { Download, Calendar, Loader2, CheckCircle2, XCircle, FileArchive, AlertCircle } from 'lucide-react';
import { auditVerificationApi } from '../../api/auditVerification';
import { useFondsStore } from '../../store';

/**
 * 审计证据包导出页面
 * 
 * 功能：
 * 1. 选择导出条件（全宗、时间范围）
 * 2. 选择是否包含验真报告
 * 3. 导出进度显示
 * 4. 下载证据包
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
export const AuditEvidencePackagePage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [fondsNo, setFondsNo] = useState('');
    const [includeVerificationReport, setIncludeVerificationReport] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);
    const [exportHistory, setExportHistory] = useState<Array<{
        id: string;
        startDate: string;
        endDate: string;
        fondsNo?: string;
        exportedAt: string;
        fileSize?: string;
    }>>([]);

    // 获取当前日期作为默认结束日期
    React.useEffect(() => {
        const today = new Date().toISOString().split('T')[0];
        if (!endDate) {
            setEndDate(today);
        }
        // 默认开始日期设为30天前
        if (!startDate) {
            const thirtyDaysAgo = new Date();
            thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
            setStartDate(thirtyDaysAgo.toISOString().split('T')[0]);
        }
    }, []);

    // 使用当前全宗号
    React.useEffect(() => {
        if (currentFonds?.fondsNo && !fondsNo) {
            setFondsNo(currentFonds.fondsNo);
        }
    }, [currentFonds, fondsNo]);

    const handleExport = async () => {
        if (!startDate || !endDate) {
            setError('请选择开始日期和结束日期');
            return;
        }

        if (new Date(startDate) > new Date(endDate)) {
            setError('开始日期不能晚于结束日期');
            return;
        }

        setLoading(true);
        setError(null);
        setSuccess(false);

        try {
            const blob = await auditVerificationApi.exportEvidencePackage(
                startDate,
                endDate,
                fondsNo || undefined,
                includeVerificationReport
            );

            // 生成文件名
            const fondsPart = fondsNo ? `_${fondsNo}` : '';
            const filename = `evidence-package_${startDate}_${endDate}${fondsPart}.zip`;

            // 创建下载链接
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            // 记录导出历史
            const fileSize = formatFileSize(blob.size);
            const newHistory = {
                id: Date.now().toString(),
                startDate,
                endDate,
                fondsNo: fondsNo || undefined,
                exportedAt: new Date().toISOString(),
                fileSize
            };
            setExportHistory(prev => [newHistory, ...prev].slice(0, 10)); // 保留最近10条
            setSuccess(true);

            // 3秒后清除成功提示
            setTimeout(() => setSuccess(false), 3000);
        } catch (err: any) {
            setError(err.message || '导出失败，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    };

    const formatDate = (dateString: string): string => {
        return new Date(dateString).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <FileArchive className="mr-2" size={28} />
                        审计证据包导出
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">导出审计日志证据包，用于外部审计或合规检查</p>
                </div>
            </div>

            {/* 导出表单 */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold mb-4 flex items-center">
                    <Calendar className="mr-2" size={20} />
                    导出条件
                </h3>
                <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                开始日期 <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                结束日期 <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                            全宗号（可选）
                        </label>
                        <input
                            type="text"
                            value={fondsNo}
                            onChange={(e) => setFondsNo(e.target.value)}
                            placeholder="留空则导出所有全宗"
                            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                        />
                        {currentFonds?.fondsNo && (
                            <p className="text-xs text-slate-500 mt-1">
                                当前全宗：{currentFonds.fondsNo}
                            </p>
                        )}
                    </div>

                    <div className="flex items-center">
                        <input
                            type="checkbox"
                            id="includeVerificationReport"
                            checked={includeVerificationReport}
                            onChange={(e) => setIncludeVerificationReport(e.target.checked)}
                            className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                        />
                        <label htmlFor="includeVerificationReport" className="ml-2 text-sm text-slate-700">
                            包含验真报告（推荐）
                        </label>
                        <span className="ml-2 text-xs text-slate-500">
                            （包含哈希链验真结果，增强证据效力）
                        </span>
                    </div>

                    <div className="pt-4 border-t border-slate-200">
                        <button
                            onClick={handleExport}
                            disabled={loading || !startDate || !endDate}
                            className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? (
                                <>
                                    <Loader2 className="animate-spin mr-2" size={16} />
                                    导出中...
                                </>
                            ) : (
                                <>
                                    <Download className="mr-2" size={16} />
                                    导出证据包
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </div>

            {/* 错误提示 */}
            {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                    <div className="flex items-center text-red-800">
                        <XCircle className="mr-2" size={20} />
                        <span>{error}</span>
                    </div>
                </div>
            )}

            {/* 成功提示 */}
            {success && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                    <div className="flex items-center text-green-800">
                        <CheckCircle2 className="mr-2" size={20} />
                        <span>证据包导出成功！文件已开始下载</span>
                    </div>
                </div>
            )}

            {/* 导出说明 */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-start">
                    <AlertCircle className="text-blue-600 mr-2 mt-0.5 flex-shrink-0" size={20} />
                    <div className="text-sm text-blue-800">
                        <div className="font-semibold mb-2">导出说明：</div>
                        <ul className="list-disc list-inside space-y-1">
                            <li>证据包为ZIP格式，包含指定时间范围内的所有审计日志</li>
                            <li>若选择包含验真报告，将同时包含哈希链验真结果</li>
                            <li>证据包可用于外部审计、合规检查或数据备份</li>
                            <li>建议定期导出证据包，作为审计留痕的备份</li>
                        </ul>
                    </div>
                </div>
            </div>

            {/* 导出历史 */}
            {exportHistory.length > 0 && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">最近导出记录</h3>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 text-slate-500 border-b">
                                <tr>
                                    <th className="px-4 py-2">导出时间</th>
                                    <th className="px-4 py-2">时间范围</th>
                                    <th className="px-4 py-2">全宗号</th>
                                    <th className="px-4 py-2">文件大小</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {exportHistory.map((record) => (
                                    <tr key={record.id}>
                                        <td className="px-4 py-2 text-slate-600">
                                            {formatDate(record.exportedAt)}
                                        </td>
                                        <td className="px-4 py-2 text-slate-800">
                                            {record.startDate} 至 {record.endDate}
                                        </td>
                                        <td className="px-4 py-2 text-slate-600">
                                            {record.fondsNo || '全部'}
                                        </td>
                                        <td className="px-4 py-2 text-slate-600">
                                            {record.fileSize || '-'}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AuditEvidencePackagePage;



