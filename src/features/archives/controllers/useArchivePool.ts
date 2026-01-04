/**
 * useArchivePool - Pool Status Management Hook
 *
 * Handles pool-specific status filtering and statistics
 */
import { useState, useEffect, useCallback } from 'react';
import { client } from '../../../api/client';
import { ControllerPool, PoolStatusFilter } from './types';

interface UseArchivePoolOptions {
    isEnabled: boolean;
}

export function useArchivePool(options: UseArchivePoolOptions): ControllerPool {
    const { isEnabled } = options;

    const [statusFilter, setStatusFilter] = useState<PoolStatusFilter>(null);
    const [statusStats, setStatusStats] = useState<Record<string, number>>({});

    const refreshStats = useCallback(async () => {
        if (!isEnabled) return;
        try {
            const response = await client.get('/pool/stats/status');
            if (response.data.code === 200) {
                setStatusStats(response.data.data || {});
            }
        } catch (error) {
            console.error('Failed to load pool status stats:', error);
        }
    }, [isEnabled]);

    // Load stats on mount and when enabled
    useEffect(() => {
        if (isEnabled) {
            refreshStats();
        }
    }, [isEnabled, refreshStats]);

    return {
        statusFilter,
        setStatusFilter: (v: PoolStatusFilter) => {
            console.log('[useArchivePool] Status filter changed:', v);
            setStatusFilter(v);
        },
        statusStats,
        refreshStats,
    };
}
