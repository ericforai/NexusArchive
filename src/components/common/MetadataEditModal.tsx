// Input: React、lucide-react 图标、react-dom
// Output: React 组件 MetadataEditModal（纯 UI 层）
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import { X, Save, FileText } from 'lucide-react';
import { FormModal } from '../modals/FormModal';
import { MetadataForm, MetadataFormData, MetadataFormConfig } from './MetadataForm';

// ✅ 已移除对 api/client 的依赖（遵循架构边界规则 A）
// API 调用现在通过 props 传入


// 单据类型选项 (《会计档案管理办法》财政部79号令 第6条)
const VOUCHER_TYPE_OPTIONS = [
    { code: 'AC01', label: '会计凭证', desc: '原始凭证（发票、收据、银行回单等）、记账凭证' },
    { code: 'AC02', label: '会计账簿', desc: '总账、明细账、日记账、固定资产卡片' },
    { code: 'AC03', label: '财务会计报告', desc: '月度/季度/半年度/年度报告' },
    { code: 'AC04', label: '其他会计资料', desc: '银行对账单、纳税申报表、会计档案鉴定意见书等' },
];

// 默认字段配置
const DEFAULT_FIELDS: MetadataFormConfig[] = [
    {
        name: 'fiscalYear',
        label: '会计年度',
        required: true,
        type: 'text',
        placeholder: '例：2025',
        pattern: '\\d{4}',
    },
    {
        name: 'voucherType',
        label: '单据类型',
        required: true,
        type: 'select',
        options: VOUCHER_TYPE_OPTIONS,
    },
    {
        name: 'creator',
        label: '责任者',
        required: true,
        type: 'text',
        placeholder: '例：财务部 张三',
    },
    {
        name: 'fondsCode',
        label: '全宗号',
        required: false,
        type: 'text',
        placeholder: '例：COMP001',
        helperText: '可选',
    },
    {
        name: 'modifyReason',
        label: '修改原因',
        required: true,
        type: 'textarea',
        placeholder: '例：补充上传发票的分类信息',
        rows: 2,
        helperText: '合规要求',
    },
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
    /** 自定义字段配置 */
    fieldConfig?: MetadataFormConfig[];
}

export const MetadataEditModal: React.FC<MetadataEditModalProps> = ({
    isOpen,
    onClose,
    fileId,
    fileName,
    onSuccess,
    onLoadFileDetail,
    onUpdateMetadata,
    fieldConfig = DEFAULT_FIELDS,
}) => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Form state using unified MetadataFormData
    const [formData, setFormData] = useState<MetadataFormData>({
        fiscalYear: new Date().getFullYear().toString(),
        voucherType: 'AC01',
        creator: '',
        fondsCode: '',
        modifyReason: '',
    });

    const loadFileDetail = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const detail = await onLoadFileDetail(fileId);
            if (detail) {
                setFormData({
                    fiscalYear: detail.fiscalYear || new Date().getFullYear().toString(),
                    voucherType: detail.voucherType || 'AC01',
                    creator: detail.creator || '',
                    fondsCode: detail.fondsCode || '',
                    modifyReason: '',
                });
            }
        } catch (err) {
            console.error('Failed to load file detail:', err);
        } finally {
            setLoading(false);
        }
    }, [fileId, onLoadFileDetail]);

    // Load existing metadata when modal opens
    useEffect(() => {
        if (isOpen && fileId) {
            loadFileDetail();
        }
    }, [isOpen, fileId, loadFileDetail]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validation
        const requiredFields = fieldConfig.filter(f => f.required);
        for (const field of requiredFields) {
            if (!formData[field.name as keyof MetadataFormData]) {
                setError(`请填写${field.label}`);
                return;
            }
        }

        setSaving(true);
        setError(null);

        try {
            const result = await onUpdateMetadata({
                id: fileId,
                fiscalYear: formData.fiscalYear,
                voucherType: formData.voucherType,
                creator: formData.creator,
                fondsCode: formData.fondsCode || undefined,
                modifyReason: formData.modifyReason,
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

    const header = (
        <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
                <h3 className="text-lg font-semibold text-slate-800 dark:text-white">元数据补录</h3>
                <p className="text-sm text-slate-500 dark:text-slate-400 truncate max-w-[280px]">{fileName}</p>
            </div>
        </div>
    );

    return (
        <FormModal
            isOpen={isOpen}
            onClose={onClose}
            onSubmit={handleSubmit}
            isSubmitting={saving}
            error={error}
            title="元数据补录"
            header={header}
            submitText="保存并重新检测"
        >
            <MetadataForm
                data={formData}
                onChange={setFormData}
                fields={fieldConfig}
                loading={loading}
                error={error}
            />
        </FormModal>
    );
};

export default MetadataEditModal;
