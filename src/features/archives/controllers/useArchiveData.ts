/**
 * useArchiveData - Data Loading State Management Hook
 *
 * Manages data state and provides setter methods
 * 使用 useMemo 稳定返回值，避免因引用变化导致 useEffect 无限循环
 */
import { useState, useMemo, useCallback } from 'react';
import { ControllerDataInternal } from './types';
import type { GenericRow } from '../../../types';

export function useArchiveData(_initialPageSize = 10): ControllerDataInternal {
    const [rows, setRows] = useState<GenericRow[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // 修复：添加真正的分页信息状态
    const [pageInfo, setPageInfoState] = useState({
        total: 0,
        page: 1,
        pageSize: _initialPageSize
    });

    // 使用 useCallback 稳定函数引用
    const setPageInfo = useCallback((info: { total: number; page: number; pageSize: number }) => {
        setPageInfoState(info);
    }, []);

    const setCurrentPage = useCallback((page: number) => {
        setPageInfoState(prev => ({ ...prev, page }));
    }, []);

    // 使用 useMemo 稳定返回对象引用，只有内部状态变化时才返回新对象
    return useMemo(() => ({
        rows,
        isLoading,
        errorMessage,
        pageInfo,
        setRows,
        setIsLoading,
        setErrorMessage,
        setPageInfo,
        setCurrentPage,
    }), [rows, isLoading, errorMessage, pageInfo, setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage]);
}
