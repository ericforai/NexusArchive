// Input: React、lucide-react、legacyImportApi、Tailwind CSS
// Output: LegacyImportPage 组件
// Pos: src/pages/admin/LegacyImportPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import {
    Upload,
    FileText,
    CheckCircle2,
    XCircle,
    AlertCircle,
    Loader2,
    Download,
    History,
    Eye,
    RefreshCw,
    X,
    HelpCircle,
    Info,
    FileSpreadsheet,
    ChevronDown,
    ChevronUp,
} from 'lucide-react';
import {
    legacyImportApi,
    ImportPreviewResult,
    ImportResult,
    LegacyImportTask,
    ImportError,
} from '../../api/legacyImport';

/**
 * 历史数据导入页面
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 * 
 * 功能：
 * 1. 文件上传（支持拖拽）
 * 2. 导入预览（显示解析结果和验证错误）
 * 3. 执行导入（显示进度）
 * 4. 导入历史查询
 * 5. 错误报告下载
 */
export const LegacyImportPage: React.FC = () => {
    // 状态管理
    const [activeTab, setActiveTab] = useState<'import' | 'history'>('import');
    const [file, setFile] = useState<File | null>(null);
    const [previewResult, setPreviewResult] = useState<ImportPreviewResult | null>(null);
    const [importResult, setImportResult] = useState<ImportResult | null>(null);
    const [loading, setLoading] = useState(false);
    const [importing, setImporting] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [tasks, setTasks] = useState<LegacyImportTask[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [showGuideDetail, setShowGuideDetail] = useState(false);

    useEffect(() => {
        if (activeTab === 'history') {
            loadHistory();
        }
    }, [activeTab, currentPage, statusFilter]);

    // 加载导入历史
    const loadHistory = async () => {
        setHistoryLoading(true);
        try {
            const res = await legacyImportApi.getTasks(currentPage, 20, statusFilter || undefined);
            if (res.code === 200 && res.data) {
                setTasks(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载导入历史失败', error);
        } finally {
            setHistoryLoading(false);
        }
    };

    // 处理文件选择
    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0];
        if (selectedFile) {
            // 文件大小检查（100MB = 100 * 1024 * 1024 bytes）
            const maxSize = 100 * 1024 * 1024;
            if (selectedFile.size > maxSize) {
                alert(`文件大小超过限制！最大允许 100MB，当前文件：${(selectedFile.size / 1024 / 1024).toFixed(2)} MB`);
                e.target.value = '';
                return;
            }
            setFile(selectedFile);
            setPreviewResult(null);
            setImportResult(null);
        }
    };

    // 处理文件拖拽
    const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile) {
            // 文件大小检查（100MB = 100 * 1024 * 1024 bytes）
            const maxSize = 100 * 1024 * 1024;
            if (droppedFile.size > maxSize) {
                alert(`文件大小超过限制！最大允许 100MB，当前文件：${(droppedFile.size / 1024 / 1024).toFixed(2)} MB`);
                return;
            }
            setFile(droppedFile);
            setPreviewResult(null);
            setImportResult(null);
        }
    };

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
    };

    // 预览导入数据
    const handlePreview = async () => {
        if (!file) {
            alert('请先选择文件');
            return;
        }

        setLoading(true);
        try {
            const res = await legacyImportApi.preview(file);
            if (res.code === 200 && res.data) {
                setPreviewResult(res.data);
            } else {
                alert(res.message || '预览失败');
            }
        } catch (error: any) {
            alert(error?.response?.data?.message || '预览失败');
        } finally {
            setLoading(false);
        }
    };

    // 执行导入
    const handleImport = async () => {
        if (!file) {
            alert('请先选择文件');
            return;
        }

        if (!previewResult) {
            alert('请先预览数据');
            return;
        }

        if (!window.confirm('确认导入数据吗？导入过程可能需要较长时间。')) {
            return;
        }

        setImporting(true);
        try {
            const res = await legacyImportApi.import(file);
            if (res.code === 200 && res.data) {
                setImportResult(res.data);
                // 导入成功后，刷新历史列表
                if (activeTab === 'history') {
                    loadHistory();
                }
            } else {
                alert(res.message || '导入失败');
            }
        } catch (error: any) {
            alert(error?.response?.data?.message || '导入失败');
        } finally {
            setImporting(false);
        }
    };

    // 下载错误报告
    const handleDownloadErrorReport = async (importId: string) => {
        try {
            const blob = await legacyImportApi.downloadErrorReport(importId);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `error_report_${importId}.xlsx`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            alert('下载错误报告失败');
        }
    };

    // 下载CSV模板
    const handleDownloadCsvTemplate = async () => {
        try {
            const blob = await legacyImportApi.downloadCsvTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.csv';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            alert('下载CSV模板失败');
        }
    };

    // 下载Excel模板
    const handleDownloadExcelTemplate = async () => {
        try {
            const blob = await legacyImportApi.downloadExcelTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.xlsx';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            alert('下载Excel模板失败');
        }
    };

    // 获取状态标签样式
    const getStatusBadge = (status: string) => {
        const styles = {
            SUCCESS: 'bg-green-100 text-green-800',
            PARTIAL_SUCCESS: 'bg-yellow-100 text-yellow-800',
            FAILED: 'bg-red-100 text-red-800',
            PENDING: 'bg-gray-100 text-gray-800',
            PROCESSING: 'bg-blue-100 text-blue-800',
        };
        return styles[status as keyof typeof styles] || styles.PENDING;
    };

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Header */}
            <div className="bg-white border-b border-slate-200 px-6 py-4">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <FileText className="w-6 h-6 text-primary-600" />
                        <h1 className="text-xl font-semibold text-slate-900">历史数据导入</h1>
                    </div>
                </div>

                {/* Tabs */}
                <div className="mt-4 flex gap-4 border-b border-slate-200">
                    <button
                        onClick={() => setActiveTab('import')}
                        className={`px-4 py-2 font-medium ${
                            activeTab === 'import'
                                ? 'text-primary-600 border-b-2 border-primary-600'
                                : 'text-slate-600 hover:text-slate-900'
                        }`}
                    >
                        数据导入
                    </button>
                    <button
                        onClick={() => setActiveTab('history')}
                        className={`px-4 py-2 font-medium ${
                            activeTab === 'history'
                                ? 'text-primary-600 border-b-2 border-primary-600'
                                : 'text-slate-600 hover:text-slate-900'
                        }`}
                    >
                        <History className="w-4 h-4 inline-block mr-2" />
                        导入历史
                    </button>
                </div>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-auto p-6">
                {activeTab === 'import' ? (
                    <div className="max-w-6xl mx-auto space-y-6">
                        {/* 合规警告提示 - P0级别 */}
                        <div className="bg-amber-50 border-2 border-amber-300 rounded-lg p-4">
                            <div className="flex items-start gap-3">
                                <AlertCircle className="w-6 h-6 text-amber-600 mt-0.5 flex-shrink-0" />
                                <div className="flex-1">
                                    <h3 className="font-semibold text-amber-900 mb-2">
                                        ⚠️ 合规说明（重要）
                                    </h3>
                                    <div className="text-sm text-amber-800 space-y-2">
                                        <p className="font-medium">
                                            本功能<strong>仅适用于历史数据迁移场景</strong>，不适用于新档案的正式归档。
                                        </p>
                                        <div className="bg-white rounded p-3 border border-amber-200">
                                            <p className="mb-2"><strong>适用范围：</strong></p>
                                            <ul className="list-disc list-inside space-y-1 ml-2 text-xs">
                                                <li>✅ 系统迁移：从旧系统迁移历史数据到新系统</li>
                                                <li>✅ 历史元数据补录：电子文件已通过其他方式完成归档和四性检测</li>
                                                <li>❌ 新档案归档：请使用标准归档流程（包含四性检测）</li>
                                            </ul>
                                        </div>
                                        <p className="text-xs text-amber-700 mt-2">
                                            <strong>合规要求：</strong>根据GB/T 39362-2020，电子会计档案归档时必须进行四性检测（完整性、真实性、可用性、安全性）。
                                            通过本功能导入的数据<strong>仅包含元数据</strong>，如需归档电子文件，请使用标准归档流程或后续补充文件关联。
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 导入指南说明 */}
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                            <div className="flex items-start gap-3">
                                <Info className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
                                <div className="flex-1">
                                    <div className="flex items-center justify-between mb-2">
                                        <h3 className="font-medium text-blue-900">导入说明</h3>
                                        <button
                                            onClick={() => setShowGuideDetail(!showGuideDetail)}
                                            className="text-blue-600 hover:text-blue-800 flex items-center gap-1 text-sm"
                                        >
                                            {showGuideDetail ? (
                                                <>
                                                    <ChevronUp className="w-4 h-4" />
                                                    收起详细说明
                                                </>
                                            ) : (
                                                <>
                                                    <ChevronDown className="w-4 h-4" />
                                                    展开详细说明
                                                </>
                                            )}
                                        </button>
                                    </div>
                                    <div className="text-sm text-blue-800 space-y-1">
                                        <p>
                                            <strong>文件格式：</strong>支持 CSV、Excel (.xlsx, .xls) 格式
                                        </p>
                                        <p>
                                            <strong>文件大小限制：</strong>最大 100MB，建议单次导入不超过 10,000 行（大数据量建议分批导入）
                                        </p>
                                        <p>
                                            <strong>必需字段：</strong>fonds_no（全宗号）、fonds_name（全宗名称）、archive_year（归档年度）、doc_type（档案类型）、title（档案标题）、retention_policy_name（保管期限名称）
                                        </p>
                                        <p>
                                            <strong>模板下载：</strong>请先下载模板文件，按照模板格式填写数据后再导入
                                        </p>
                                        
                                        {showGuideDetail && (
                                            <div className="mt-4 pt-4 border-t border-blue-300 space-y-3">
                                                <div>
                                                    <h4 className="font-semibold mb-2">必需字段说明：</h4>
                                                    <ul className="list-disc list-inside space-y-1 ml-2">
                                                        <li><strong>fonds_no</strong>：全宗号，字母数字下划线，长度1-50，如：JD-001</li>
                                                        <li><strong>fonds_name</strong>：全宗名称，如：京东集团</li>
                                                        <li><strong>archive_year</strong>：归档年度，有效年份1900-2100，如：2024</li>
                                                        <li><strong>doc_type</strong>：档案类型，如：凭证、报表、账簿等</li>
                                                        <li><strong>title</strong>：档案标题，长度1-255，如：2024年1月记账凭证</li>
                                                        <li><strong>retention_policy_name</strong>：保管期限名称，如：永久、30年、10年等</li>
                                                    </ul>
                                                </div>
                                                <div>
                                                    <h4 className="font-semibold mb-2">可选字段：</h4>
                                                    <ul className="list-disc list-inside space-y-1 ml-2">
                                                        <li>entity_name（法人实体名称）</li>
                                                        <li>entity_tax_code（统一社会信用代码）</li>
                                                        <li>doc_date（形成日期，格式：YYYY-MM-DD）</li>
                                                        <li>amount（金额，数字格式）</li>
                                                        <li>counterparty（对方单位）</li>
                                                        <li>voucher_no（凭证号）</li>
                                                        <li>invoice_no（发票号）</li>
                                                    </ul>
                                                </div>
                                                <div>
                                                    <h4 className="font-semibold mb-2">注意事项：</h4>
                                                    <ul className="list-disc list-inside space-y-1 ml-2">
                                                        <li>CSV文件需使用UTF-8编码（建议使用模板文件）</li>
                                                        <li>Excel文件使用第一个工作表（Sheet）</li>
                                                        <li>第一行必须为表头（字段名），请勿删除或修改</li>
                                                        <li>系统会自动创建不存在的全宗和法人实体</li>
                                                        <li>导入前请先预览数据，确认无误后再执行导入</li>
                                                    </ul>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 模板下载区域 */}
                        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                            <h2 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
                                <FileSpreadsheet className="w-5 h-5 text-primary-600" />
                                下载导入模板
                            </h2>
                            <p className="text-sm text-slate-600 mb-4">
                                请先下载模板文件，按照模板格式填写数据后再导入。模板包含标准字段名和示例数据。
                            </p>
                            <div className="flex gap-3">
                                <button
                                    onClick={handleDownloadCsvTemplate}
                                    className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                >
                                    <Download className="w-4 h-4" />
                                    下载 CSV 模板
                                </button>
                                <button
                                    onClick={handleDownloadExcelTemplate}
                                    className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                                >
                                    <Download className="w-4 h-4" />
                                    下载 Excel 模板
                                </button>
                            </div>
                        </div>

                        {/* 文件上传区域 */}
                        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                            <h2 className="text-lg font-semibold text-slate-900 mb-4">选择文件</h2>
                            <div
                                onDrop={handleDrop}
                                onDragOver={handleDragOver}
                                className="border-2 border-dashed border-slate-300 rounded-lg p-8 text-center hover:border-primary-400 transition-colors"
                            >
                                <Upload className="w-12 h-12 mx-auto text-slate-400 mb-4" />
                                <p className="text-slate-600 mb-2">
                                    拖拽文件到此处，或
                                    <label className="text-primary-600 cursor-pointer hover:underline ml-1">
                                        点击选择文件
                                        <input
                                            type="file"
                                            accept=".csv,.xlsx,.xls"
                                            onChange={handleFileSelect}
                                            className="hidden"
                                        />
                                    </label>
                                </p>
                                <p className="text-sm text-slate-500">
                                    支持 CSV、Excel (.xlsx, .xls) 格式
                                </p>
                                <p className="text-xs text-slate-400 mt-1">
                                    <strong>文件大小限制：</strong>最大 100MB | <strong>建议行数：</strong>不超过 10,000 行
                                </p>
                                {file && (
                                    <div className="mt-4 p-3 bg-slate-50 rounded-lg">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <FileText className="w-5 h-5 text-slate-600" />
                                                <span className="text-slate-900">{file.name}</span>
                                                <span className="text-sm text-slate-500">
                                                    ({(file.size / 1024 / 1024).toFixed(2)} MB)
                                                </span>
                                            </div>
                                            <button
                                                onClick={() => {
                                                    setFile(null);
                                                    setPreviewResult(null);
                                                    setImportResult(null);
                                                }}
                                                className="text-slate-400 hover:text-slate-600"
                                            >
                                                <X className="w-5 h-5" />
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>

                            <div className="mt-4 flex gap-3">
                                <button
                                    onClick={handlePreview}
                                    disabled={!file || loading}
                                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                                >
                                    {loading ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <Eye className="w-4 h-4" />
                                    )}
                                    预览数据
                                </button>
                                {previewResult && (
                                    <button
                                        onClick={handleImport}
                                        disabled={importing}
                                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                                    >
                                        {importing ? (
                                            <Loader2 className="w-4 h-4 animate-spin" />
                                        ) : (
                                            <CheckCircle2 className="w-4 h-4" />
                                        )}
                                        确认导入
                                    </button>
                                )}
                            </div>
                        </div>

                        {/* 预览结果 */}
                        {previewResult && (
                            <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                                <h2 className="text-lg font-semibold text-slate-900 mb-4">预览结果</h2>
                                <div className="grid grid-cols-4 gap-4 mb-6">
                                    <div className="p-4 bg-slate-50 rounded-lg">
                                        <div className="text-sm text-slate-600">总行数</div>
                                        <div className="text-2xl font-semibold text-slate-900">
                                            {previewResult.totalRows ?? 0}
                                        </div>
                                    </div>
                                    <div className="p-4 bg-green-50 rounded-lg">
                                        <div className="text-sm text-green-600">有效行数</div>
                                        <div className="text-2xl font-semibold text-green-900">
                                            {previewResult.validRows ?? 0}
                                        </div>
                                    </div>
                                    <div className="p-4 bg-red-50 rounded-lg">
                                        <div className="text-sm text-red-600">无效行数</div>
                                        <div className="text-2xl font-semibold text-red-900">
                                            {previewResult.invalidRows ?? 0}
                                        </div>
                                    </div>
                                    <div className="p-4 bg-blue-50 rounded-lg">
                                        <div className="text-sm text-blue-600">全宗数量</div>
                                        <div className="text-2xl font-semibold text-blue-900">
                                            {previewResult.statistics?.fondsCount ?? 0}
                                        </div>
                                    </div>
                                </div>

                                {/* 统计信息 */}
                                {previewResult.statistics?.willCreateFonds && previewResult.statistics.willCreateFonds.length > 0 && (
                                    <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                                        <div className="flex items-center gap-2 mb-2">
                                            <AlertCircle className="w-5 h-5 text-yellow-600" />
                                            <span className="font-medium text-yellow-900">
                                                将自动创建全宗：
                                            </span>
                                        </div>
                                        <div className="text-sm text-yellow-800">
                                            {Array.isArray(previewResult.statistics.willCreateFonds) 
                                                ? previewResult.statistics.willCreateFonds.join(', ')
                                                : String(previewResult.statistics.willCreateFonds || '')}
                                        </div>
                                    </div>
                                )}

                                {/* 错误列表 */}
                                {previewResult.errors && Array.isArray(previewResult.errors) && previewResult.errors.length > 0 && (
                                    <div className="mt-4">
                                        <h3 className="text-md font-medium text-slate-900 mb-3">
                                            验证错误 ({previewResult.errors.length} 条)
                                        </h3>
                                        <div className="max-h-64 overflow-auto border border-slate-200 rounded-lg">
                                            <table className="w-full text-sm">
                                                <thead className="bg-slate-50">
                                                    <tr>
                                                        <th className="px-4 py-2 text-left">行号</th>
                                                        <th className="px-4 py-2 text-left">字段名</th>
                                                        <th className="px-4 py-2 text-left">错误消息</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-200">
                                                    {previewResult.errors.slice(0, 50).map((error, index) => (
                                                        <tr key={index}>
                                                            <td className="px-4 py-2">{error?.rowNumber ?? '-'}</td>
                                                            <td className="px-4 py-2">{error?.fieldName ?? '-'}</td>
                                                            <td className="px-4 py-2 text-red-600">
                                                                {error?.errorMessage ?? '-'}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                            {previewResult.errors.length > 50 && (
                                                <div className="px-4 py-2 text-sm text-slate-500 text-center bg-slate-50">
                                                    仅显示前 50 条错误，完整错误列表将在导入报告中提供
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* 导入结果 */}
                        {importResult && (
                            <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                                <h2 className="text-lg font-semibold text-slate-900 mb-4">导入结果</h2>
                                <div
                                    className={`p-4 rounded-lg mb-4 ${
                                        importResult.status === 'SUCCESS'
                                            ? 'bg-green-50 border border-green-200'
                                            : importResult.status === 'PARTIAL_SUCCESS'
                                            ? 'bg-yellow-50 border border-yellow-200'
                                            : 'bg-red-50 border border-red-200'
                                    }`}
                                >
                                    <div className="flex items-center gap-3 mb-2">
                                        {importResult.status === 'SUCCESS' ? (
                                            <CheckCircle2 className="w-6 h-6 text-green-600" />
                                        ) : importResult.status === 'PARTIAL_SUCCESS' ? (
                                            <AlertCircle className="w-6 h-6 text-yellow-600" />
                                        ) : (
                                            <XCircle className="w-6 h-6 text-red-600" />
                                        )}
                                        <span
                                            className={`text-lg font-semibold ${
                                                importResult.status === 'SUCCESS'
                                                    ? 'text-green-900'
                                                    : importResult.status === 'PARTIAL_SUCCESS'
                                                    ? 'text-yellow-900'
                                                    : 'text-red-900'
                                            }`}
                                        >
                                            {importResult.status === 'SUCCESS'
                                                ? '导入成功'
                                                : importResult.status === 'PARTIAL_SUCCESS'
                                                ? '部分成功'
                                                : '导入失败'}
                                        </span>
                                    </div>
                                    <div className="grid grid-cols-3 gap-4 mt-4">
                                        <div>
                                            <div className="text-sm text-slate-600">总行数</div>
                                            <div className="text-xl font-semibold text-slate-900">
                                                {importResult.totalRows ?? 0}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="text-sm text-green-600">成功</div>
                                            <div className="text-xl font-semibold text-green-900">
                                                {importResult.successRows ?? 0}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="text-sm text-red-600">失败</div>
                                            <div className="text-xl font-semibold text-red-900">
                                                {importResult.failedRows ?? 0}
                                            </div>
                                        </div>
                                    </div>
                                    {importResult.errors && Array.isArray(importResult.errors) && importResult.errors.length > 0 && importResult.errorReportUrl && (
                                        <div className="mt-4">
                                            <button
                                                onClick={() => handleDownloadErrorReport(importResult.importId)}
                                                className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2"
                                            >
                                                <Download className="w-4 h-4" />
                                                下载错误报告
                                            </button>
                                        </div>
                                    )}
                                </div>
                                
                                {/* 合规提示 - 四性检测提醒 */}
                                {importResult.successRows > 0 && (
                                    <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                                        <div className="flex items-start gap-2">
                                            <AlertCircle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
                                            <div className="flex-1 text-sm text-amber-800">
                                                <p className="font-medium mb-2">⚠️ 重要提醒：四性检测要求</p>
                                                <p className="mb-2">
                                                    本次导入的数据<strong>仅包含元数据</strong>，未包含电子文件。根据GB/T 39362-2020要求，电子会计档案归档时必须进行四性检测（完整性、真实性、可用性、安全性）。
                                                </p>
                                                <div className="bg-white rounded p-2 border border-amber-200 text-xs">
                                                    <p className="font-medium mb-1">后续操作建议：</p>
                                                    <ul className="list-disc list-inside space-y-1 ml-2">
                                                        <li>如需归档电子文件，请使用<strong>标准归档流程</strong>（包含四性检测）</li>
                                                        <li>或通过<strong>文件关联功能</strong>关联电子文件后，进行四性检测</li>
                                                        <li>历史数据迁移场景下，如电子文件已在其他系统完成四性检测，请保留检测记录作为审计证据</li>
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="max-w-6xl mx-auto">
                        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
                            {/* 筛选器 */}
                            <div className="mb-4 flex items-center gap-4">
                                <select
                                    value={statusFilter}
                                    onChange={(e) => {
                                        setStatusFilter(e.target.value);
                                        setCurrentPage(1);
                                    }}
                                    className="px-3 py-2 border border-slate-300 rounded-lg"
                                >
                                    <option value="">全部状态</option>
                                    <option value="SUCCESS">成功</option>
                                    <option value="PARTIAL_SUCCESS">部分成功</option>
                                    <option value="FAILED">失败</option>
                                    <option value="PENDING">待处理</option>
                                    <option value="PROCESSING">处理中</option>
                                </select>
                                <button
                                    onClick={loadHistory}
                                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                                >
                                    <RefreshCw className="w-4 h-4" />
                                    刷新
                                </button>
                            </div>

                            {/* 任务列表 */}
                            {historyLoading ? (
                                <div className="text-center py-8">
                                    <Loader2 className="w-8 h-8 animate-spin mx-auto text-slate-400" />
                                </div>
                            ) : tasks.length === 0 ? (
                                <div className="text-center py-8 text-slate-500">暂无导入记录</div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead className="bg-slate-50">
                                            <tr>
                                                <th className="px-4 py-3 text-left">文件名</th>
                                                <th className="px-4 py-3 text-left">操作人</th>
                                                <th className="px-4 py-3 text-left">全宗号</th>
                                                <th className="px-4 py-3 text-left">总行数</th>
                                                <th className="px-4 py-3 text-left">成功/失败</th>
                                                <th className="px-4 py-3 text-left">状态</th>
                                                <th className="px-4 py-3 text-left">创建时间</th>
                                                <th className="px-4 py-3 text-left">操作</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-200">
                                            {tasks.map((task) => (
                                                <tr key={task.id} className="hover:bg-slate-50">
                                                    <td className="px-4 py-3">{task.fileName || '-'}</td>
                                                    <td className="px-4 py-3">
                                                        {task.operatorName || (typeof task.operatorId === 'string' ? task.operatorId : '-')}
                                                    </td>
                                                    <td className="px-4 py-3">{task.fondsNo || '-'}</td>
                                                    <td className="px-4 py-3">{task.totalRows ?? 0}</td>
                                                    <td className="px-4 py-3">
                                                        <span className="text-green-600">{task.successRows ?? 0}</span> /{' '}
                                                        <span className="text-red-600">{task.failedRows ?? 0}</span>
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        <span
                                                            className={`px-2 py-1 rounded text-xs ${getStatusBadge(
                                                                task.status || 'PENDING'
                                                            )}`}
                                                        >
                                                            {task.status || 'PENDING'}
                                                        </span>
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        {task.createdAt 
                                                            ? (() => {
                                                                try {
                                                                    const date = new Date(task.createdAt);
                                                                    return isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
                                                                } catch {
                                                                    return '-';
                                                                }
                                                            })()
                                                            : '-'}
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        {task.errorReportPath && (
                                                            <button
                                                                onClick={() => handleDownloadErrorReport(task.id)}
                                                                className="text-primary-600 hover:text-primary-800 flex items-center gap-1"
                                                            >
                                                                <Download className="w-4 h-4" />
                                                                错误报告
                                                            </button>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            {/* 分页 */}
                            {total > 20 && (
                                <div className="mt-4 flex items-center justify-between">
                                    <div className="text-sm text-slate-600">
                                        共 {total} 条记录，第 {currentPage} 页
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                                            disabled={currentPage === 1}
                                            className="px-3 py-1 border border-slate-300 rounded disabled:opacity-50"
                                        >
                                            上一页
                                        </button>
                                        <button
                                            onClick={() => setCurrentPage((p) => p + 1)}
                                            disabled={currentPage * 20 >= total}
                                            className="px-3 py-1 border border-slate-300 rounded disabled:opacity-50"
                                        >
                                            下一页
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

// 默认导出以支持懒加载
export default LegacyImportPage;
