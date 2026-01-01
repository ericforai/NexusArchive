// Input: React
// Output: useDataTable Hook
// Pos: 通用复用组件 - 表格数据管理 Hook

import { useState, useCallback, useEffect } from 'react';

export interface PaginationConfig {
  current: number;
  pageSize: number;
  total: number;
}

export interface UseDataTableOptions<T> {
  fetchFn: (page: number, pageSize: number, filters?: Record<string, any>) => Promise<{
    data: T[];
    total: number;
  }>;
  defaultPageSize?: number;
  defaultFilters?: Record<string, any>;
  autoFetch?: boolean;
}

export interface UseDataTableReturn<T> {
  data: T[];
  loading: boolean;
  pagination: PaginationConfig;
  filters: Record<string, any>;
  fetchData: () => Promise<void>;
  handlePageChange: (page: number, pageSize: number) => void;
  handleFilterChange: (filters: Record<string, any>) => void;
  handleReset: () => void;
  refresh: () => Promise<void>;
}

/**
 * 统一的表格数据管理 Hook
 * <p>
 * 封装分页、筛选、数据获取逻辑
 * </p>
 */
export function useDataTable<T extends Record<string, any>>({
  fetchFn,
  defaultPageSize = 10,
  defaultFilters = {},
  autoFetch = true,
}: UseDataTableOptions<T>): UseDataTableReturn<T> {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState<PaginationConfig>({
    current: 1,
    pageSize: defaultPageSize,
    total: 0,
  });
  const [filters, setFilters] = useState<Record<string, any>>(defaultFilters);

  // Fetch data
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchFn(pagination.current, pagination.pageSize, filters);
      setData(result.data);
      setPagination(prev => ({
        ...prev,
        total: result.total,
      }));
    } catch (error) {
      console.error('Failed to fetch data:', error);
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [fetchFn, pagination.current, pagination.pageSize, filters]);

  // Handle page change
  const handlePageChange = useCallback((page: number, pageSize: number) => {
    setPagination(prev => ({
      ...prev,
      current: page,
      pageSize,
    }));
  }, []);

  // Handle filter change
  const handleFilterChange = useCallback((newFilters: Record<string, any>) => {
    setFilters(newFilters);
    setPagination(prev => ({
      ...prev,
      current: 1, // Reset to first page when filters change
    }));
  }, []);

  // Reset filters and pagination
  const handleReset = useCallback(() => {
    setFilters(defaultFilters);
    setPagination({
      current: 1,
      pageSize: defaultPageSize,
      total: 0,
    });
  }, [defaultFilters, defaultPageSize]);

  // Refresh current page
  const refresh = useCallback(async () => {
    await fetchData();
  }, [fetchData]);

  // Auto fetch on mount or when filters/pagination change
  useEffect(() => {
    if (autoFetch) {
      fetchData();
    }
  }, [fetchData, autoFetch]);

  return {
    data,
    loading,
    pagination,
    filters,
    fetchData,
    handlePageChange,
    handleFilterChange,
    handleReset,
    refresh,
  };
}

export default useDataTable;
