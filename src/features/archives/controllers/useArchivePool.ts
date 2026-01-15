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
}

export function useArchivePool(options: UseArchivePoolOptions): ControllerPool {
    const { isEnabled, initialStatusFilter } = options;

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
            const response = await client.get('/pool/stats/status');
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
    }, [isEnabled]); // 移除 refreshStats 依赖

    // 使用 useMemo 稳定返回值
    return useMemo(() => ({
        statusFilter,
        setStatusFilter: (v: PoolStatusFilter) => setStatusFilter(v),
        statusStats,
        refreshStats,
    }), [statusFilter, statusStats, refreshStats]);
}
