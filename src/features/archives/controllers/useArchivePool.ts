/**
 * useArchivePool - Pool Status Management Hook
 *
 * Handles pool-specific status filtering and statistics
 * 修复：使用 ref 存储 isEnabled，使用 useMemo 稳定返回值，避免无限循环
 */
import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { client } from '../../../api/client';
import { ControllerPool, PoolStatusFilter } from './types';

interface UseArchivePoolOptions {
    isEnabled: boolean;
    initialStatusFilter?: PoolStatusFilter;
    categoryFilter?: string | null; // 新增
}

export function useArchivePool(options: UseArchivePoolOptions): ControllerPool {
    const { isEnabled, initialStatusFilter, categoryFilter } = options;

    const [statusFilter, setStatusFilter] = useState<PoolStatusFilter>(initialStatusFilter || null);
    const [statusStats, setStatusStats] = useState<Record<string, number>>({});

    // 使用 ref 存储 isEnabled，避免 refreshStats 依赖变化
    const isEnabledRef = useRef(isEnabled);
    useEffect(() => {
        isEnabledRef.current = isEnabled;
    }, [isEnabled]);

    const refreshStats = useCallback(async () => {
        if (!isEnabledRef.current) return;
        try {
            const url = categoryFilter
                ? `/pool/stats/status?category=${categoryFilter}`
                : '/pool/stats/status';
            const response = await client.get(url);
            if (response.data.code === 200) {
                setStatusStats(response.data.data || {});
            }
        } catch (error) {
            console.error('Failed to load pool status stats:', error);
        }
    }, []); // 移除 isEnabled 依赖

    // Load stats on mount and when enabled
    useEffect(() => {
        if (isEnabled) {
            refreshStats();
        }
    }, [isEnabled, categoryFilter, refreshStats]); // 添加 categoryFilter 和 refreshStats 依赖

    // 使用 useMemo 稳定返回值
    return useMemo(() => ({
        statusFilter,
        setStatusFilter: (v: PoolStatusFilter) => setStatusFilter(v),
        statusStats,
        refreshStats,
    }), [statusFilter, statusStats, refreshStats]);
}
