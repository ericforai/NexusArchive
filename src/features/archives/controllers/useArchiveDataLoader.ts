/**
 * useArchiveDataLoader - Data Loading Logic Hook
 *
 * Handles data fetching for archives and pool items
 */
import { useCallback, useRef, useEffect } from 'react';
import { poolApi } from '../../../api/pool';
import { archivesApi } from '../../../api/archives';
import { UseArchiveDataLoadOptions, ControllerDataInternal, PoolStatusFilter } from './types';
import { mapArchiveToRow } from './utils';

interface UseArchiveDataLoaderOptions extends UseArchiveDataLoadOptions {
    data: ControllerDataInternal;
    showToast: (message: string, type?: 'success' | 'error') => void;
    onSelectionClear?: () => void;
}

export function useArchiveDataLoader(options: UseArchiveDataLoaderOptions) {
    const {
        mode,
        query,
        page,
        isPoolView,
        poolStatusFilter,
        data,
        showToast,
        onSelectionClear,
    } = options;

    const { setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage } = data;
    const isInitialLoadRef = useRef(true);
    const prevDepsRef = useRef({
        subTitle: mode.subTitle,
        searchTerm: query.searchTerm,
        statusFilter: query.statusFilter,
        orgFilter: query.orgFilter,
        subTypeFilter: query.subTypeFilter,
        poolStatusFilter,
    });

    // Load pool data
    const loadPoolData = useCallback(async (pageNum = page.currentPage, filterOverride?: PoolStatusFilter) => {
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        try {
            const poolItems = await poolApi.getList();
            const activeFilter = filterOverride !== undefined ? filterOverride : poolStatusFilter;

            const filtered = poolItems.filter((item) => {
                const itemStatus = (item as any).status || 'PENDING_CHECK';
                if (activeFilter && activeFilter !== 'all') {
                    if (itemStatus !== activeFilter) return false;
                } else if (query.statusFilter) {
                    if (itemStatus !== query.statusFilter) return false;
                }
                if (!query.searchTerm) return true;
                return Object.values(item).some((val) =>
                    String(val || '').toLowerCase().includes(query.searchTerm.toLowerCase())
                );
            });

            const total = filtered.length;
            const start = (pageNum - 1) * page.pageInfo.pageSize;
            const paged = filtered.slice(start, start + page.pageInfo.pageSize);
            const mappedPaged = paged.map((item: any) => ({
                ...item,
                rawStatus: item.status
            }));

            setRows(mappedPaged);
            setPageInfo({ total, page: pageNum, pageSize: page.pageInfo.pageSize });
        } catch (error) {
            console.error('Failed to load pool data:', error);
            setErrorMessage('加载数据失败');
            showToast('加载数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, [page.currentPage, page.pageInfo.pageSize, query.searchTerm, query.statusFilter, poolStatusFilter, setRows, setIsLoading, setErrorMessage, setPageInfo, showToast, onSelectionClear]);

    // Load archive list
    const loadArchiveList = useCallback(async (pageNum = page.currentPage) => {
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        try {
            const result = await archivesApi.getArchives({
                page: pageNum,
                limit: page.pageInfo.pageSize,
                search: query.searchTerm || undefined,
                status: query.statusFilter || mode.defaultStatus,
                categoryCode: mode.categoryCode,
                orgId: query.orgFilter || undefined,
                subType: query.subTypeFilter || undefined
            });

            if (result.code !== 200 || !result.data) {
                throw new Error(result.message || '加载档案数据失败');
            }

            const pageResult: any = result.data;
            const { records = [], total = 0, size = page.pageInfo.pageSize, current = pageNum } = pageResult;
            const mappedItems = (records as any[]).map((item) => mapArchiveToRow(item, mode.subTitle));

            setRows(mappedItems);
            setPageInfo({ total, page: current || pageNum, pageSize: size || page.pageInfo.pageSize });
            setCurrentPage(current || pageNum);
        } catch (error) {
            console.error('Failed to load archives:', error);
            setErrorMessage(error instanceof Error ? error.message : '加载档案数据失败');
            showToast('加载档案数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, [page.currentPage, page.pageInfo.pageSize, query.searchTerm, query.statusFilter, mode.defaultStatus, mode.categoryCode, mode.subTitle, query.orgFilter, query.subTypeFilter, setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage, showToast, onSelectionClear]);

    // Load current view based on mode
    const loadCurrentView = useCallback((pageNum = page.currentPage, poolFilter?: PoolStatusFilter) => {
        if (isPoolView) {
            return loadPoolData(pageNum, poolFilter);
        }
        return loadArchiveList(pageNum);
    }, [isPoolView, loadPoolData, loadArchiveList, page.currentPage]);

    // Initial load
    useEffect(() => {
        if (isInitialLoadRef.current) {
            isInitialLoadRef.current = false;
            if (isPoolView) {
                loadPoolData(1, poolStatusFilter);
            } else {
                loadArchiveList(1);
            }
        }
    }, [isPoolView, poolStatusFilter, loadPoolData, loadArchiveList]);

    // Monitor poolStatusFilter changes
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const prevFilter = prevDepsRef.current.poolStatusFilter;
        if (prevFilter !== poolStatusFilter) {
            prevDepsRef.current = { ...prevDepsRef.current, poolStatusFilter };
            if (isPoolView) {
                setCurrentPage(1);
                loadPoolData(1, poolStatusFilter);
            }
        }
    }, [poolStatusFilter, isPoolView, loadPoolData, setCurrentPage]);

    // Monitor other filter changes
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const prevDeps = prevDepsRef.current;
        const depsChanged =
            prevDeps.subTitle !== mode.subTitle ||
            prevDeps.searchTerm !== query.searchTerm ||
            prevDeps.statusFilter !== query.statusFilter ||
            prevDeps.orgFilter !== query.orgFilter ||
            prevDeps.subTypeFilter !== query.subTypeFilter;

        if (depsChanged) {
            prevDepsRef.current = {
                ...prevDepsRef.current,
                subTitle: mode.subTitle,
                searchTerm: query.searchTerm,
                statusFilter: query.statusFilter,
                orgFilter: query.orgFilter,
                subTypeFilter: query.subTypeFilter,
            };
            setCurrentPage(1);
            if (isPoolView) {
                loadPoolData(1, poolStatusFilter);
            } else {
                loadArchiveList(1);
            }
        }
    }, [mode.subTitle, query.searchTerm, query.statusFilter, query.orgFilter, query.subTypeFilter, isPoolView, poolStatusFilter, loadPoolData, loadArchiveList, setCurrentPage]);

    // Monitor page changes
    const isPageChangeFromFilterRef = useRef(false);
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        if (isPageChangeFromFilterRef.current) {
            isPageChangeFromFilterRef.current = false;
            return;
        }
        loadCurrentView(page.currentPage, poolStatusFilter);
    }, [page.currentPage, poolStatusFilter, loadCurrentView]);

    return {
        loadCurrentView,
        markPageChangeFromFilter: () => {
            isPageChangeFromFilterRef.current = true;
        },
    };
}
