/**
 * useArchivePagination - Pagination Management Hook
 *
 * Handles pagination state
 */
import { useState } from 'react';
import { ControllerPage } from './types';

export function useArchivePagination(initialPageSize = 10): ControllerPage {
    const [currentPage, setCurrentPage] = useState(1);
    const [pageInfo, setPageInfo] = useState({
        total: 0,
        page: 1,
        pageSize: initialPageSize
    });

    return {
        currentPage,
        pageInfo,
        setCurrentPage,
    };
}
