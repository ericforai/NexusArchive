// Input: React、lucide-react、API types
// Output: ImportTab 组件
// Pos: src/pages/admin/LegacyImportPage/components/ImportTab.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import {
    FileSpreadsheet,
    Download,
    AlertCircle,
    Info,
    ChevronDown,
    ChevronUp,
    CheckCircle2,
    XCircle,
} from 'lucide-react';
import type { ImportPreviewData, ImportResultData } from '../types';
import { FileUploader } from './FileUploader';

/**
 * ImportTab 组件属性
 */
export interface ImportTabProps {
    /** 当前选择的文件 */
    file: File | null;
    /** 是否正在加载 */
    loading: boolean;
    /** 是否正在导入 */
    importing: boolean;
    /** 预览结果 */
    previewResult: ImportPreviewData | null;
    /** 导入结果 */
    importResult: ImportResultData | null;
    /** 文件变化回调 */
    onFileChange: (file: File | null) => void;
    /** 预览点击回调 */
    onPreview: () => void;
    /** 导入点击回调 */
    onImport: () => void;
    /** 下载CSV模板 */
    onDownloadCsvTemplate: () => void;
    /** 下载Excel模板 */
    onDownloadExcelTemplate: () => void;
    /** 下载错误报告 */
    onDownloadErrorReport: (importId: string) => void;
}

/**
 * 导入标签页组件
 *
 * 功能：
 * - 显示合规警告
 * - 显示导入说明
 * - 模板下载
 * - 文件上传
 * - 预览结果显示
 * - 导入结果显示
 */
export const ImportTab: React.FC<ImportTabProps> = ({
    file,
    loading,
    importing,
    previewResult,
    importResult,
    onFileChange,
    onPreview,
    onImport,
    onDownloadCsvTemplate,
    onDownloadExcelTemplate,
    onDownloadErrorReport,
}) => {
    const [showGuideDetail, setShowGuideDetail] = useState(false);

    return (
        <div className="max-w-6xl mx-auto space-y-6">
            {/* 合规警告提示 - P0级别 */}
            <ComplianceWarning />

            {/* 导入指南说明 */}
            <ImportGuide showDetail={showGuideDetail} onToggleDetail={() => setShowGuideDetail(!showGuideDetail)} />

            {/* 模板下载区域 */}
            <TemplateDownloadSection
                onDownloadCsvTemplate={onDownloadCsvTemplate}
                onDownloadExcelTemplate={onDownloadExcelTemplate}
            />

            {/* 文件上传区域 */}
            <FileUploader
                file={file}
                loading={loading}
                importing={importing}
                showPreviewButton={true}
                showImportButton={!!previewResult}
                onFileChange={onFileChange}
                onPreview={onPreview}
                onImport={onImport}
                onClear={() => {
                    onFileChange(null);
                }}
            />

            {/* 预览结果 */}
            {previewResult && <PreviewResultSection previewResult={previewResult} />}

            {/* 导入结果 */}
            {importResult && (
                <ImportResultSection
                    importResult={importResult}
                    onDownloadErrorReport={onDownloadErrorReport}
                />
            )}
        </div>
    );
};

/**
 * 合规警告组件
 */
const ComplianceWarning: React.FC = () => {
    return (
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
    );
};

/**
 * 导入指南组件
 */
interface ImportGuideProps {
    showDetail: boolean;
    onToggleDetail: () => void;
}

const ImportGuide: React.FC<ImportGuideProps> = ({ showDetail, onToggleDetail }) => {
    return (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
                <Info className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="font-medium text-blue-900">导入说明</h3>
                        <button
                            onClick={onToggleDetail}
                            className="text-blue-600 hover:text-blue-800 flex items-center gap-1 text-sm"
                            type="button"
                        >
                            {showDetail ? (
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

                        {showDetail && <DetailedGuideContent />}
                    </div>
                </div>
            </div>
        </div>
    );
};

/**
 * 详细指南内容
 */
const DetailedGuideContent: React.FC = () => {
    return (
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
    );
};

/**
 * 模板下载区域组件
 */
interface TemplateDownloadSectionProps {
    onDownloadCsvTemplate: () => void;
    onDownloadExcelTemplate: () => void;
}

const TemplateDownloadSection: React.FC<TemplateDownloadSectionProps> = ({
    onDownloadCsvTemplate,
    onDownloadExcelTemplate,
}) => {
    return (
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
                    onClick={onDownloadCsvTemplate}
                    className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                    type="button"
                >
                    <Download className="w-4 h-4" />
                    下载 CSV 模板
                </button>
                <button
                    onClick={onDownloadExcelTemplate}
                    className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2 text-slate-700"
                    type="button"
                >
                    <Download className="w-4 h-4" />
                    下载 Excel 模板
                </button>
            </div>
        </div>
    );
};

/**
 * 预览结果组件
 */
interface PreviewResultSectionProps {
    previewResult: ImportPreviewData;
}

const PreviewResultSection: React.FC<PreviewResultSectionProps> = ({ previewResult }) => {
    return (
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

            {/* 统计信息 - 将创建的全宗 */}
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
    );
};

/**
 * 导入结果组件
 */
interface ImportResultSectionProps {
    importResult: ImportResultData;
    onDownloadErrorReport: (importId: string) => void;
}

const ImportResultSection: React.FC<ImportResultSectionProps> = ({ importResult, onDownloadErrorReport }) => {
    return (
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
                            onClick={() => onDownloadErrorReport(importResult.importId)}
                            className="px-4 py-2 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 flex items-center gap-2"
                            type="button"
                        >
                            <Download className="w-4 h-4" />
                            下载错误报告
                        </button>
                    </div>
                )}
            </div>

            {/* 合规提示 - 四性检测提醒 */}
            {importResult.successRows > 0 && <FourPropertyWarning />}
        </div>
    );
};

/**
 * 四性检测警告组件
 */
const FourPropertyWarning: React.FC = () => {
    return (
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
    );
};

export default ImportTab;
