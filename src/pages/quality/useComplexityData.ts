// Input: complexity-history.json
// Output: 复杂度数据 Hook
// Pos: src/pages/quality/ 数据读取
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useEffect } from 'react';
import type { ComplexityHistory, ComplexitySnapshot, FileViolation } from './types';

const HISTORY_URL = '/docs/metrics/complexity-history.json';

/**
 * 复杂度数据 Hook
 * 读取并处理历史快照数据
 */
export const useComplexityData = () => {
    const [data, setData] = useState<ComplexityHistory | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const response = await fetch(HISTORY_URL);
                if (!response.ok) {
                    throw new Error('Failed to load complexity history');
                }
                const history: ComplexityHistory = await response.json();
                setData(history);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Unknown error');
                console.error('[useComplexityData] Failed to load data:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    /**
     * 获取最新快照
     */
    const latestSnapshot = data?.snapshots[data.snapshots.length - 1] || null;

    /**
     * 获取最近 N 条快照
     */
    const getRecentSnapshots = (count: number): ComplexitySnapshot[] => {
        if (!data) return [];
        return data.snapshots.slice(-count);
    };

    /**
     * 获取所有违规文件
     */
    const getAllViolations = (): FileViolation[] => {
        if (!latestSnapshot) return [];
        return [...latestSnapshot.files].sort((a, b) => {
            // 按严重程度排序
            const severityOrder = { high: 0, medium: 1, low: 2 };
            const aSeverity = getSeverityLevel(a);
            const bSeverity = getSeverityLevel(b);
            if (severityOrder[aSeverity] !== severityOrder[bSeverity]) {
                return severityOrder[aSeverity] - severityOrder[bSeverity];
            }
            // 相同严重程度按复杂度排序
            return b.complexity - a.complexity;
        });
    };

    /**
     * 按严重程度筛选
     */
    const getBySeverity = (severity: 'high' | 'medium' | 'low'): FileViolation[] => {
        return getAllViolations().filter(file => getSeverityLevel(file) === severity);
    };

    return {
        data,
        loading,
        error,
        latestSnapshot,
        getRecentSnapshots,
        getAllViolations,
        getBySeverity
    };
};

/**
 * 获取文件严重程度
 */
function getSeverityLevel(file: FileViolation): 'high' | 'medium' | 'low' {
    if (file.complexity > 15 || file.maxFunctionLines > 100) return 'high';
    if (file.complexity > 10 || file.maxFunctionLines > 50) return 'medium';
    return 'low';
}
