// Input: React, legacyImportApi, toast notification service
// Output: Import preview hook (useImportPreview)
// Pos: src/pages/admin/LegacyImportPage/hooks/useImportPreview.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback } from 'react';
import {
    legacyImportApi,
    ImportPreviewResult,
    ImportResult,
} from '../../../../api/legacyImport';
import { toast } from '../../../../utils/notificationService';

/**
 * 预览状态
 */
export interface PreviewState {
    previewResult: ImportPreviewResult | null;
    importResult: ImportResult | null;
    loading: boolean;
    importing: boolean;
}

/**
 * 导入预览和执行 Hook
 *
 * 负责处理数据预览、执行导入等核心业务逻辑
 *
 * @param file - 要导入的文件
 * @param onImportSuccess - 导入成功后的回调
 * @returns 预览状态和操作方法
 */
export function useImportPreview(
    file: File | null,
    onImportSuccess?: () => void
) {
    const [previewResult, setPreviewResult] = useState<ImportPreviewResult | null>(null);
    const [importResult, setImportResult] = useState<ImportResult | null>(null);
    const [loading, setLoading] = useState(false);
    const [importing, setImporting] = useState(false);

    /**
     * 预览导入数据
     */
    const handlePreview = useCallback(async () => {
        if (!file) {
            toast.warning('请先选择文件');
            return;
        }

        setLoading(true);
        try {
            const res = await legacyImportApi.preview(file);
            if (res.code === 200 && res.data) {
                setPreviewResult(res.data);
                toast.success('预览成功');
            } else {
                toast.error(res.message || '预览失败');
            }
        } catch (error: any) {
            const errorMessage = error?.response?.data?.message || '预览失败';
            toast.error(errorMessage);
        } finally {
            setLoading(false);
        }
    }, [file]);

    /**
     * 执行导入
     */
    const handleImport = useCallback(async () => {
        if (!file) {
            toast.warning('请先选择文件');
            return false;
        }

        if (!previewResult) {
            toast.warning('请先预览数据');
            return false;
        }

        if (!window.confirm('确认导入数据吗？导入过程可能需要较长时间。')) {
            return false;
        }

        setImporting(true);
        try {
            const res = await legacyImportApi.import(file);
            if (res.code === 200 && res.data) {
                setImportResult(res.data);
                toast.success('导入成功');
                onImportSuccess?.();
                return true;
            } else {
                toast.error(res.message || '导入失败');
                return false;
            }
        } catch (error: any) {
            const errorMessage = error?.response?.data?.message || '导入失败';
            toast.error(errorMessage);
            return false;
        } finally {
            setImporting(false);
        }
    }, [file, previewResult, onImportSuccess]);

    /**
     * 重置预览状态
     */
    const resetPreview = useCallback(() => {
        setPreviewResult(null);
        setImportResult(null);
        setLoading(false);
        setImporting(false);
    }, []);

    /**
     * 检查是否可以导入
     */
    const canImport = useCallback((): boolean => {
        return !!file && !!previewResult;
    }, [file, previewResult]);

    /**
     * 检查是否可以预览
     */
    const canPreview = useCallback((): boolean => {
        return !!file && !loading;
    }, [file, loading]);

    /**
     * 获取预览统计信息
     */
    const getPreviewStats = useCallback(() => {
        if (!previewResult) {
            return null;
        }

        return {
            totalRows: previewResult.totalRows ?? 0,
            validRows: previewResult.validRows ?? 0,
            invalidRows: previewResult.invalidRows ?? 0,
            fondsCount: previewResult.statistics?.fondsCount ?? 0,
            willCreateFonds: previewResult.statistics?.willCreateFonds ?? [],
            errors: previewResult.errors ?? [],
        };
    }, [previewResult]);

    /**
     * 获取导入统计信息
     */
    const getImportStats = useCallback(() => {
        if (!importResult) {
            return null;
        }

        return {
            status: importResult.status,
            totalRows: importResult.totalRows ?? 0,
            successRows: importResult.successRows ?? 0,
            failedRows: importResult.failedRows ?? 0,
            importId: importResult.importId,
            errorReportUrl: importResult.errorReportUrl,
            errors: importResult.errors ?? [],
        };
    }, [importResult]);

    return {
        // 状态
        previewResult,
        importResult,
        loading,
        importing,

        // 设置方法
        setPreviewResult,
        setImportResult,

        // 操作方法
        handlePreview,
        handleImport,
        resetPreview,
        canImport,
        canPreview,

        // 辅助方法
        getPreviewStats,
        getImportStats,
    };
}
