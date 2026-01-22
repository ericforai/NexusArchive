/**
 * useArchiveDataLoader - Data Loading Logic Hook
 *
 * Handles data fetching for archives and pool items
 * 修复：使用 ref 存储依赖，避免循环依赖导致无限循环
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
    categoryFilter?: string | null; // 新增
}

export function useArchiveDataLoader(options: UseArchiveDataLoaderOptions) {
    const {
        mode,
        query,
        page,
        isPoolView,
        poolStatusFilter,
        categoryFilter,
        data,
        showToast,
        onSelectionClear,
    } = options;

    const { setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage } = data;
    const isInitialLoadRef = useRef(true);

    // 使用 ref 存储动态依赖，避免 useCallback 依赖变化导致无限循环
    const depsRef = useRef({
        mode,
        query,
        page,
        isPoolView,
        poolStatusFilter,
        categoryFilter,
        data,  // 添加 data 以便访问 pageInfo
        setRows,
        setIsLoading,
        setErrorMessage,
        setPageInfo,
        setCurrentPage,
        showToast,
        onSelectionClear,
    });

    // 同步更新 ref - 包含 data 以确保 pageInfo 是最新的
    useEffect(() => {
        depsRef.current = {
            mode,
            query,
            page,
            isPoolView,
            poolStatusFilter,
            categoryFilter,
            data,  // 同步 data，确保 pageInfo 是最新的引用
            setRows,
            setIsLoading,
            setErrorMessage,
            setPageInfo,
            setCurrentPage,
            showToast,
            onSelectionClear,
        };
    });  // 添加 data 依赖，确保 data 更新时 ref 也更新

    // Load pool data - 不依赖任何外部值，全部从 ref 读取
    const loadPoolData = useCallback(async (pageNum?: number, filterOverride?: PoolStatusFilter) => {
        const { poolStatusFilter, categoryFilter, setRows, setIsLoading, setErrorMessage, setPageInfo, showToast, onSelectionClear, data } = depsRef.current;
        // 使用 data.pageInfo.page 而不是 page.currentPage
        const currentPage = pageNum ?? data.pageInfo.page;
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        try {
            const activeFilter = filterOverride !== undefined ? filterOverride : poolStatusFilter;

            let poolItems = [];
            // 如果指定了状态，调用特定的状态过滤接口（后端过滤）
            if (activeFilter && activeFilter !== 'all') {
                // 状态映射：前端简化状态 -> 后端数据库实际状态值
                const STATUS_MAP: Record<string, string> = {
                    'PENDING_CHECK': 'PENDING_CHECK',
                    'NEEDS_ACTION': 'CHECK_FAILED',
                    'READY_TO_MATCH': 'PENDING_METADATA',
                    'READY_TO_ARCHIVE': 'READY_TO_ARCHIVE',
                    'COMPLETED': 'COMPLETED',
                };
                const queryStatus = STATUS_MAP[activeFilter] || activeFilter;
                poolItems = await poolApi.getListByStatus(queryStatus, categoryFilter || undefined);
            } else {
                poolItems = await poolApi.getList(categoryFilter || undefined);
            }

            const total = poolItems.length;
            const pageSize = data.pageInfo.pageSize;
            const start = (currentPage - 1) * pageSize;
            const paged = poolItems.slice(start, start + pageSize);

            const mappedPaged = paged.map((item: any) => ({
                ...item,
                rawStatus: item.status
            }));

            setRows(mappedPaged);
            setPageInfo({ total, page: currentPage, pageSize });
        } catch (error) {
            console.error('Failed to load pool data:', error);
            setErrorMessage('加载数据失败');
            showToast('加载数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, []); // 空依赖数组

    // Load archive list - 不依赖任何外部值，全部从 ref 读取
    const loadArchiveList = useCallback(async (pageNum?: number) => {
        const { page, query, mode, categoryFilter, setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage, showToast, onSelectionClear } = depsRef.current;
        const currentPage = pageNum ?? page.currentPage;
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        try {
            const shouldSendSubType = query.subTypeFilter && query.subTypeFilter.trim().length > 0 && mode.categoryCode;

            const result = await archivesApi.getArchives({
                page: currentPage,
                limit: page.pageInfo.pageSize,
                search: query.searchTerm || undefined,
                status: query.statusFilter || mode.defaultStatus,
                categoryCode: mode.categoryCode || (categoryFilter === 'VOUCHER' ? 'AC01' : undefined),
                orgId: query.orgFilter || undefined,
                subType: shouldSendSubType ? query.subTypeFilter : undefined
            });

            if (result.code !== 200 || !result.data) {
                throw new Error(result.message || '加载档案数据失败');
            }

            const pageResult: any = result.data;
            const { records = [], total = 0, size = page.pageInfo.pageSize, current = currentPage } = pageResult;
            const mappedItems = (records as any[]).map((item) => mapArchiveToRow(item, mode.subTitle));

            setRows(mappedItems);
            setPageInfo({ total, page: current || currentPage, pageSize: size || page.pageInfo.pageSize });
            setCurrentPage(current || currentPage);
        } catch (error) {
            console.error('Failed to load archives:', error);
            setErrorMessage(error instanceof Error ? error.message : '加载档案数据失败');
            showToast('加载档案数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, []); // 空依赖数组

    // Load current view based on mode - 不依赖任何外部值
    const loadCurrentView = useCallback((pageNum?: number, poolFilter?: PoolStatusFilter) => {
        const { isPoolView } = depsRef.current;
        if (isPoolView) {
            return loadPoolData(pageNum, poolFilter);
        }
        return loadArchiveList(pageNum);
    }, [loadPoolData, loadArchiveList]);

    // 用于存储前一次的依赖值，检测变化
    const prevDepsRef = useRef({
        subTitle: mode.subTitle,
        searchTerm: query.searchTerm,
        statusFilter: query.statusFilter,
        orgFilter: query.orgFilter,
        subTypeFilter: query.subTypeFilter,
        poolStatusFilter,
        categoryFilter, // 新增
        currentPage: page.currentPage,
        isPoolView,
    });

    // Initial load - 只在组件挂载时执行一次
    useEffect(() => {
        if (isInitialLoadRef.current) {
            isInitialLoadRef.current = false;
            const { isPoolView, poolStatusFilter } = depsRef.current;
            const doInitialLoad = async () => {
                if (isPoolView) {
                    await loadPoolData(1, poolStatusFilter);
                } else {
                    await loadArchiveList(1);
                }
            };
            doInitialLoad();
        }
    }, [loadPoolData, loadArchiveList]);

    // Monitor poolStatusFilter changes
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const { poolStatusFilter, isPoolView, setCurrentPage } = depsRef.current;
        const prevFilter = prevDepsRef.current.poolStatusFilter;
        if (prevFilter !== poolStatusFilter) {
            prevDepsRef.current.poolStatusFilter = poolStatusFilter;
            if (isPoolView) {
                setCurrentPage(1);
                // 直接调用 loadCurrentView，确保使用最新的 poolStatusFilter
                loadCurrentView(1, poolStatusFilter);
            }
        }
    }, [poolStatusFilter, loadCurrentView]);

    // Monitor other filter changes
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const { mode, query, isPoolView, poolStatusFilter, categoryFilter, setCurrentPage } = depsRef.current;
        const prevDeps = prevDepsRef.current;
        const depsChanged =
            prevDeps.subTitle !== mode.subTitle ||
            prevDeps.searchTerm !== query.searchTerm ||
            prevDeps.statusFilter !== query.statusFilter ||
            prevDeps.orgFilter !== query.orgFilter ||
            prevDeps.subTypeFilter !== query.subTypeFilter ||
            prevDeps.categoryFilter !== categoryFilter; // 添加门类变化监听

        if (depsChanged) {
            prevDepsRef.current.subTitle = mode.subTitle;
            prevDepsRef.current.searchTerm = query.searchTerm;
            prevDepsRef.current.statusFilter = query.statusFilter;
            prevDepsRef.current.orgFilter = query.orgFilter;
            prevDepsRef.current.subTypeFilter = query.subTypeFilter;
            prevDepsRef.current.categoryFilter = categoryFilter; // 同步门类状态
            // 只更新 page.currentPage 到 1，让 page change effect 来处理数据加载
            setCurrentPage(1);
        }
    }, [mode.subTitle, query.searchTerm, query.statusFilter, query.orgFilter, query.subTypeFilter, categoryFilter, loadPoolData, loadArchiveList]);

    // Monitor page changes - 只监听 currentPage 的值变化
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const { page } = depsRef.current;
        const prevPage = prevDepsRef.current.currentPage;
        if (prevPage !== page.currentPage) {
            prevDepsRef.current.currentPage = page.currentPage;
            // 使用闭包捕获的 poolStatusFilter（依赖数组中的最新值）
            loadCurrentView(page.currentPage, poolStatusFilter);
        }
    }, [page.currentPage, poolStatusFilter, loadCurrentView]);

    return {
        loadCurrentView,
    };
}
