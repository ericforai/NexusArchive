/**
 * useArchiveData - Data Loading State Management Hook
 *
 * Manages data state and provides setter methods
 */
import { useState } from 'react';
import { ControllerDataInternal } from './types';
import type { GenericRow } from '../../../types';

export function useArchiveData(initialPageSize = 10): ControllerDataInternal {
    const [rows, setRows] = useState<GenericRow[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const [pageInfo, setPageInfo] = useState({
        total: 0,
        page: 1,
        pageSize: initialPageSize
    });

    const [currentPage, setCurrentPage] = useState(1);

    return {
        rows,
        isLoading,
        errorMessage,
        setRows,
        setIsLoading,
        setErrorMessage,
        setPageInfo,
        setCurrentPage,
    };
}
