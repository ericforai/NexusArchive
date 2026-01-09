// Input: React hooks、类型定义
// Output: 文件详情加载 Hook
// Pos: MetadataEditModal 自定义 Hook

import { useState, useCallback, useEffect } from 'react';
import type { MetadataFormData } from '../MetadataForm';
import { getDefaultFormData } from './constants';
import type { FileDetail } from './types';

interface UseFileDetailLoaderOptions {
    isOpen: boolean;
    fileId: string;
    onLoadFileDetail: (fileId: string) => Promise<FileDetail | null>;
}

interface UseFileDetailLoaderReturn {
    loading: boolean;
    formData: MetadataFormData;
    setFormData: (data: MetadataFormData | ((prev: MetadataFormData) => MetadataFormData)) => void;
}

/**
 * 文件详情加载 Hook
 * 当模态框打开时自动加载文件详情并填充表单
 */
export const useFileDetailLoader = ({
    isOpen,
    fileId,
    onLoadFileDetail,
}: UseFileDetailLoaderOptions): UseFileDetailLoaderReturn => {
    const [loading, setLoading] = useState(false);
    const [formData, setFormData] = useState<MetadataFormData>(getDefaultFormData());

    const loadFileDetail = useCallback(async () => {
        setLoading(true);
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
        } catch (_err) {
            // Silently handle load errors
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

    return { loading, formData, setFormData };
};
