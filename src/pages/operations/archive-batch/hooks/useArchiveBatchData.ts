// Input: archiveBatchApi, useFondsStore
// Output: ArchiveBatch 数据获取和分页 Hook
// Pos: src/pages/operations/archive-batch/hooks/useArchiveBatchData.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import {
    archiveBatchApi,
    ArchiveBatch,
    BatchStats,
    BatchStatus
} from '@/api/archiveBatch';
import { useFondsStore } from '@/store/useFondsStore';

/**
 * 归档批次数据 Hook 返回值
 */
export interface UseArchiveBatchDataReturn {
    // 数据状态
    batches: ArchiveBatch[];
    total: number;
    stats: BatchStats;
    loading: boolean;

    // 分页状态
    page: number;
    pageSize: number;
    statusFilter?: BatchStatus;

    // 操作方法
    setPage: (page: number) => void;
    setPageSize: (pageSize: number) => void;
    setStatusFilter: (status?: BatchStatus) => void;
    loadBatches: () => Promise<void>;
    loadStats: () => Promise<void>;
    refresh: () => Promise<void>;
}

/**
 * 归档批次数据获取 Hook
 *
 * 管理批次列表数据、统计信息和分页状态
 */
export function useArchiveBatchData(): UseArchiveBatchDataReturn {
    const currentFonds = useFondsStore((state: any) => state.currentFonds);

    // 数据状态
    const [batches, setBatches] = useState<ArchiveBatch[]>([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState<BatchStats>({
        total: 0,
        byStatus: {} as Record<BatchStatus, number>
    });

    // 分页状态
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [statusFilter, setStatusFilter] = useState<BatchStatus | undefined>();

    /**
     * 加载批次列表
     */
    const loadBatches = useCallback(async () => {
        setLoading(true);
        try {
            const result = await archiveBatchApi.listBatches(page, pageSize, undefined, statusFilter);
            setBatches(result.records);
            setTotal(result.total);
        } catch {
            message.error('加载批次列表失败');
        } finally {
            setLoading(false);
        }
    }, [page, pageSize, statusFilter]);

    /**
     * 加载统计
     */
    const loadStats = useCallback(async () => {
        try {
            const data = await archiveBatchApi.getStats();
            setStats(data);
        } catch {
            // 忽略统计加载失败
        }
    }, []);

    /**
     * 刷新数据
     */
    const refresh = useCallback(async () => {
        await Promise.all([loadBatches(), loadStats()]);
    }, [loadBatches, loadStats]);

    // 初始加载
    useEffect(() => {
        loadBatches();
        loadStats();
    }, [loadBatches, loadStats]);

    return {
        // 数据状态
        batches,
        total,
        stats,
        loading,

        // 分页状态
        page,
        pageSize,
        statusFilter,

        // 操作方法
        setPage,
        setPageSize,
        setStatusFilter,
        loadBatches,
        loadStats,
        refresh
    };
}

export default useArchiveBatchData;
