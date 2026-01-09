// Input: React, legacyImportApi, toast notification service
// Output: Import history management hook (useImportHistory)
// Pos: src/pages/admin/LegacyImportPage/hooks/useImportHistory.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useEffect } from 'react';
import {
    legacyImportApi,
    LegacyImportTask,
} from '../../../../api/legacyImport';
import { toast } from '../../../../utils/notificationService';

/**
 * 分页参数
 */
export interface PaginationParams {
    page: number;
    pageSize: number;
    statusFilter?: string;
}

/**
 * 历史记录状态
 */
export interface ImportHistoryState {
    tasks: LegacyImportTask[];
    loading: boolean;
    total: number;
    currentPage: number;
    statusFilter: string;
}

/**
 * 状态选项
 */
export const STATUS_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'SUCCESS', label: '成功' },
    { value: 'PARTIAL_SUCCESS', label: '部分成功' },
    { value: 'FAILED', label: '失败' },
    { value: 'PENDING', label: '待处理' },
    { value: 'PROCESSING', label: '处理中' },
];

/**
 * 状态对应的样式类名
 */
export const STATUS_BADGE_STYLES: Record<string, string> = {
    SUCCESS: 'bg-green-100 text-green-800',
    PARTIAL_SUCCESS: 'bg-yellow-100 text-yellow-800',
    FAILED: 'bg-red-100 text-red-800',
    PENDING: 'bg-gray-100 text-gray-800',
    PROCESSING: 'bg-blue-100 text-blue-800',
};

/**
 * 导入历史管理 Hook
 *
 * 负责处理导入历史查询、筛选、分页等功能
 *
 * @param autoLoad - 是否自动加载数据（当依赖项变化时）
 * @param loadOnMount - 是否在挂载时加载数据
 * @returns 历史记录状态和操作方法
 */
export function useImportHistory(autoLoad = true, loadOnMount = false) {
    const [tasks, setTasks] = useState<LegacyImportTask[]>([]);
    const [loading, setLoading] = useState(false);
    const [total, setTotal] = useState(0);
    const [currentPage, setCurrentPage] = useState(1);
    const [statusFilter, setStatusFilter] = useState<string>('');

    /**
     * 加载导入历史
     */
    const loadHistory = useCallback(async () => {
        setLoading(true);
        try {
            const res = await legacyImportApi.getTasks(
                currentPage,
                20,
                statusFilter || undefined
            );

            if (res.code === 200 && res.data) {
                setTasks(res.data.records || []);
                setTotal(res.data.total || 0);
            } else {
                toast.error(res.message || '加载导入历史失败');
            }
        } catch (error) {
            console.error('加载导入历史失败', error);
            toast.error('加载导入历史失败');
        } finally {
            setLoading(false);
        }
    }, [currentPage, statusFilter]);

    /**
     * 刷新历史记录（重置到第一页）
     */
    const refreshHistory = useCallback(() => {
        setCurrentPage(1);
        return loadHistory();
    }, [loadHistory]);

    /**
     * 手动触发加载
     */
    const manualLoad = useCallback(() => {
        return loadHistory();
    }, [loadHistory]);

    /**
     * 设置状态筛选
     */
    const setStatusFilterWithReset = useCallback((newStatusFilter: string) => {
        setStatusFilter(newStatusFilter);
        setCurrentPage(1);
    }, []);

    /**
     * 下载错误报告
     */
    const handleDownloadErrorReport = useCallback(async (importId: string) => {
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
            toast.success('错误报告下载成功');
        } catch (error) {
            console.error('下载错误报告失败', error);
            toast.error('下载错误报告失败');
        }
    }, []);

    /**
     * 获取状态标签样式
     */
    const getStatusBadge = useCallback((status: string): string => {
        return STATUS_BADGE_STYLES[status] || STATUS_BADGE_STYLES.PENDING;
    }, []);

    /**
     * 获取状态显示文本
     */
    const getStatusLabel = useCallback((status: string): string => {
        const option = STATUS_OPTIONS.find(opt => opt.value === status);
        return option?.label || status;
    }, []);

    /**
     * 格式化日期
     */
    const formatDate = useCallback((dateString: string | null | undefined): string => {
        if (!dateString) {
            return '-';
        }

        try {
            const date = new Date(dateString);
            return isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
        } catch {
            return '-';
        }
    }, []);

    /**
     * 计算总页数
     */
    const getTotalPages = useCallback((): number => {
        return Math.ceil(total / 20);
    }, [total]);

    /**
     * 检查是否有下一页
     */
    const hasNextPage = useCallback((): boolean => {
        return currentPage * 20 < total;
    }, [currentPage, total]);

    /**
     * 检查是否有上一页
     */
    const hasPreviousPage = useCallback((): boolean => {
        return currentPage > 1;
    }, [currentPage]);

    /**
     * 跳转到下一页
     */
    const goToNextPage = useCallback(() => {
        if (hasNextPage()) {
            setCurrentPage(p => p + 1);
        }
    }, [hasNextPage]);

    /**
     * 跳转到上一页
     */
    const goToPreviousPage = useCallback(() => {
        if (hasPreviousPage()) {
            setCurrentPage(p => p - 1);
        }
    }, [hasPreviousPage]);

    /**
     * 跳转到指定页
     */
    const goToPage = useCallback((page: number) => {
        const totalPages = getTotalPages();
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    }, [getTotalPages]);

    // 自动加载逻辑
    useEffect(() => {
        if (autoLoad) {
            loadHistory();
        }
    }, [currentPage, statusFilter, autoLoad, loadHistory]);

    // 挂载时加载
    useEffect(() => {
        if (loadOnMount) {
            loadHistory();
        }
    }, [loadOnMount, loadHistory]);

    return {
        // 状态
        tasks,
        loading,
        total,
        currentPage,
        statusFilter,

        // 设置方法
        setCurrentPage,
        setStatusFilter: setStatusFilterWithReset,

        // 操作方法
        loadHistory: manualLoad,
        refreshHistory,
        handleDownloadErrorReport,

        // 分页方法
        goToNextPage,
        goToPreviousPage,
        goToPage,
        hasNextPage,
        hasPreviousPage,
        getTotalPages,

        // 辅助方法
        getStatusBadge,
        getStatusLabel,
        formatDate,

        // 常量
        STATUS_OPTIONS,
        STATUS_BADGE_STYLES,
        PAGE_SIZE: 20,
    };
}
