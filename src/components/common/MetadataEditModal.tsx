// Input: React、lucide-react 图标、react-dom
// Output: React 组件 MetadataEditModal（纯 UI 层）
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { X, Save, RefreshCw, FileText, AlertCircle, AlertTriangle } from 'lucide-react';
import { createPortal } from 'react-dom';

// ✅ 已移除对 api/client 的依赖（遵循架构边界规则 A）
// API 调用现在通过 props 传入


// 单据类型选项 (《会计档案管理办法》财政部79号令 第6条)
const VOUCHER_TYPE_OPTIONS = [
    { code: 'AC01', label: '会计凭证', desc: '原始凭证（发票、收据、银行回单等）、记账凭证' },
    { code: 'AC02', label: '会计账簿', desc: '总账、明细账、日记账、固定资产卡片' },
    { code: 'AC03', label: '财务会计报告', desc: '月度/季度/半年度/年度报告' },
    { code: 'AC04', label: '其他会计资料', desc: '银行对账单、纳税申报表、会计档案鉴定意见书等' },
];

interface FileDetail {
    id: string;
    fileName: string;
    fileType: string;
    status: string;
    fiscalYear?: string;
    voucherType?: string;
    creator?: string;
    fondsCode?: string;
}

interface MetadataUpdatePayload {
    id: string;
    fiscalYear: string;
    voucherType: string;
    creator: string;
    fondsCode?: string;
    modifyReason: string;
}

interface MetadataEditModalProps {
    isOpen: boolean;
    onClose: () => void;
    fileId: string;
    fileName: string;
    onSuccess?: () => void;
    // ✅ 新增：通过 props 传入 API 操作（遵循架构边界规则 A）
    onLoadFileDetail: (fileId: string) => Promise<FileDetail | null>;
    onUpdateMetadata: (payload: MetadataUpdatePayload) => Promise<{ success: boolean; message?: string }>;
}

export const MetadataEditModal: React.FC<MetadataEditModalProps> = ({
    isOpen,
    onClose,
    fileId,
    fileName,
    onSuccess,
    onLoadFileDetail,
    onUpdateMetadata,
}) => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Form state
    const [fiscalYear, setFiscalYear] = useState(new Date().getFullYear().toString());
    const [voucherType, setVoucherType] = useState('AC01');
    const [creator, setCreator] = useState('');
    const [fondsCode, setFondsCode] = useState('');
    const [modifyReason, setModifyReason] = useState('');

    // Load existing metadata when modal opens
    useEffect(() => {
        if (isOpen && fileId) {
            loadFileDetail();
        }
    }, [isOpen, fileId]);

    const loadFileDetail = async () => {
        setLoading(true);
        setError(null);
        try {
            // ✅ 使用 props 传入的回调，而非直接调用 API
            const detail = await onLoadFileDetail(fileId);
            if (detail) {
                if (detail.fiscalYear) setFiscalYear(detail.fiscalYear);
                if (detail.voucherType) setVoucherType(detail.voucherType);
                if (detail.creator) setCreator(detail.creator);
                if (detail.fondsCode) setFondsCode(detail.fondsCode);
            }
        } catch (err) {
            console.error('Failed to load file detail:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validation
        if (!fiscalYear || !voucherType || !creator || !modifyReason) {
            setError('请填写所有必填字段');
            return;
        }

        setSaving(true);
        setError(null);

        try {
            // ✅ 使用 props 传入的回调，而非直接调用 API
            const result = await onUpdateMetadata({
                id: fileId,
                fiscalYear,
                voucherType,
                creator,
                fondsCode: fondsCode || undefined,
                modifyReason
            });

            if (result.success) {
                onSuccess?.();
                onClose();
            } else {
                setError(result.message || '更新失败');
            }
        } catch (err: any) {
            console.error('Failed to update metadata:', err);
            setError(err.message || '更新失败，请重试');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/50 backdrop-blur-sm"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="relative bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-lg mx-4 animate-in fade-in zoom-in-95 duration-200">
                {/* Header */}
                <div className="flex items-center justify-between p-5 border-b border-slate-200 dark:border-slate-700">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                            <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold text-slate-800 dark:text-white">元数据补录</h3>
                            <p className="text-sm text-slate-500 dark:text-slate-400 truncate max-w-[280px]">{fileName}</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    {loading ? (
                        <div className="flex items-center justify-center py-8">
                            <RefreshCw className="w-6 h-6 text-blue-500 animate-spin" />
                        </div>
                    ) : (
                        <>
                            {/* Fiscal Year */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                                    会计年度 <span className="text-rose-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={fiscalYear}
                                    onChange={(e) => setFiscalYear(e.target.value)}
                                    placeholder="例：2025"
                                    pattern="\d{4}"
                                    className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                />
                            </div>

                            {/* Voucher Type */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                                    单据类型 <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    value={voucherType}
                                    onChange={(e) => setVoucherType(e.target.value)}
                                    className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                >
                                    {VOUCHER_TYPE_OPTIONS.map((opt) => (
                                        <option key={opt.code} value={opt.code}>
                                            {opt.code} - {opt.label}
                                        </option>
                                    ))}
                                </select>
                                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                                    {VOUCHER_TYPE_OPTIONS.find(o => o.code === voucherType)?.desc}
                                </p>
                            </div>

                            {/* Creator */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                                    责任者 <span className="text-rose-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={creator}
                                    onChange={(e) => setCreator(e.target.value)}
                                    placeholder="例：财务部 张三"
                                    className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                />
                            </div>

                            {/* Fonds Code */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                                    全宗号 <span className="text-slate-400">(可选)</span>
                                </label>
                                <input
                                    type="text"
                                    value={fondsCode}
                                    onChange={(e) => setFondsCode(e.target.value)}
                                    placeholder="例：COMP001"
                                    className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                />
                            </div>

                            {/* Modify Reason */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                                    修改原因 <span className="text-rose-500">*</span>
                                    <span className="ml-2 text-xs text-slate-400">(合规要求)</span>
                                </label>
                                <textarea
                                    value={modifyReason}
                                    onChange={(e) => setModifyReason(e.target.value)}
                                    placeholder="例：补充上传发票的分类信息"
                                    rows={2}
                                    className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all resize-none"
                                />
                            </div>

                            {/* Error Message */}
                            {error && (
                                <div className="flex items-center gap-2 p-3 bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400 rounded-xl text-sm">
                                    <AlertTriangle size={16} />
                                    {error}
                                </div>
                            )}
                        </>
                    )}
                </form>

                {/* Footer */}
                <div className="flex items-center justify-end gap-3 p-5 border-t border-slate-200 dark:border-slate-700">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors"
                    >
                        取消
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={saving || loading}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {saving ? (
                            <>
                                <RefreshCw size={16} className="animate-spin" />
                                保存中...
                            </>
                        ) : (
                            <>
                                <Save size={16} />
                                保存并重新检测
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};

export default MetadataEditModal;
