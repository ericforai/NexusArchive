// Input: React hooks、类型定义
// Output: 元数据提交 Hook
// Pos: MetadataEditModal 自定义 Hook

import { useState, useCallback } from 'react';
import type { MetadataFormData, MetadataFormConfig } from '../MetadataForm';
import type { MetadataUpdatePayload } from './types';

interface UseMetadataSubmitOptions {
    fileId: string;
    formData: MetadataFormData;
    fieldConfig: MetadataFormConfig[];
    onUpdateMetadata: (payload: MetadataUpdatePayload) => Promise<{ success: boolean; message?: string }>;
    onSuccess?: () => void;
    onClose: () => void;
}

interface UseMetadataSubmitReturn {
    saving: boolean;
    error: string | null;
    handleSubmit: (e: React.FormEvent) => Promise<void>;
    setError: (error: string | null) => void;
}

/**
 * 元数据提交 Hook
 * 处理表单验证和提交逻辑
 */
export const useMetadataSubmit = ({
    fileId,
    formData,
    fieldConfig,
    onUpdateMetadata,
    onSuccess,
    onClose,
}: UseMetadataSubmitOptions): UseMetadataSubmitReturn => {
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    /**
     * 验证表单必填字段
     */
    const validateForm = useCallback((): string | null => {
        const requiredFields = fieldConfig.filter((f) => f.required);
        for (const field of requiredFields) {
            if (!formData[field.name as keyof MetadataFormData]) {
                return `请填写${field.label}`;
            }
        }
        return null;
    }, [formData, fieldConfig]);

    /**
     * 处理表单提交
     */
    const handleSubmit = useCallback(
        async (e: React.FormEvent) => {
            e.preventDefault();

            // Validation
            const validationError = validateForm();
            if (validationError) {
                setError(validationError);
                return;
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
            } catch (err: unknown) {
                const message = err instanceof Error ? err.message : '更新失败，请重试';
                setError(message);
            } finally {
                setSaving(false);
            }
        },
        [fileId, formData, onUpdateMetadata, onSuccess, onClose, validateForm]
    );

    return { saving, error, handleSubmit, setError };
};
