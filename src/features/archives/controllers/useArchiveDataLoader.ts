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
    const isPageChangeFromFilterRef = useRef(false);

    // 使用 ref 存储动态依赖，避免 useCallback 依赖变化导致无限循环
    const depsRef = useRef({
        mode,
        query,
        page,
        isPoolView,
        poolStatusFilter,
        setRows,
        setIsLoading,
        setErrorMessage,
        setPageInfo,
        setCurrentPage,
        showToast,
        onSelectionClear,
    });

    // 同步更新 ref
    useEffect(() => {
        depsRef.current = {
            mode,
            query,
            page,
            isPoolView,
            poolStatusFilter,
            setRows,
            setIsLoading,
            setErrorMessage,
            setPageInfo,
            setCurrentPage,
            showToast,
            onSelectionClear,
        };
    });

    // Load pool data - 不依赖任何外部值，全部从 ref 读取
    const loadPoolData = useCallback(async (pageNum?: number, filterOverride?: PoolStatusFilter) => {
        const { page, poolStatusFilter, setRows, setIsLoading, setErrorMessage, setPageInfo, showToast, onSelectionClear } = depsRef.current;
        const currentPage = pageNum ?? page.currentPage;
        const startAt = Date.now();
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        let resolvedFilter: PoolStatusFilter | undefined;
        try {
            const activeFilter = filterOverride !== undefined ? filterOverride : poolStatusFilter;
            resolvedFilter = activeFilter;

            let poolItems = [];
            // 如果指定了状态，调用特定的状态过滤接口（后端过滤）
            if (activeFilter && activeFilter !== 'all') {
                // 兼容旧后端：将新状态映射回旧状态
                const LEGACY_STATUS_MAP: Record<string, string> = {
                    'PENDING_CHECK': 'PENDING_CHECK',
                    'NEEDS_ACTION': 'CHECK_FAILED',
                    'READY_TO_MATCH': 'PENDING_METADATA',
                    'READY_TO_ARCHIVE': 'PENDING_ARCHIVE',
                    'COMPLETED': 'ARCHIVED',
                };
                const queryStatus = LEGACY_STATUS_MAP[activeFilter] || activeFilter;

                poolItems = await poolApi.getListByStatus(queryStatus);
            } else {
                poolItems = await poolApi.getList();
            }

            const total = poolItems.length;
            const start = (currentPage - 1) * page.pageInfo.pageSize;
            const paged = poolItems.slice(start, start + page.pageInfo.pageSize);

            const mappedPaged = paged.map((item: any) => ({
                ...item,
                rawStatus: item.status
            }));

            setRows(mappedPaged);
            setPageInfo({ total, page: currentPage, pageSize: page.pageInfo.pageSize });
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
        const { page, query, mode, setRows, setIsLoading, setErrorMessage, setPageInfo, setCurrentPage, showToast, onSelectionClear } = depsRef.current;
        const currentPage = pageNum ?? page.currentPage;
        setIsLoading(true);
        setErrorMessage(null);
        onSelectionClear?.();
        try {
            // Debug: 打印实际参数值，排查 400 错误
            console.log('[ArchiveDataLoader] API params:', {
                page: currentPage,
                limit: page.pageInfo.pageSize,
                search: query.searchTerm || undefined,
                status: query.statusFilter || mode.defaultStatus,
                categoryCode: mode.categoryCode,
                subTitle: mode.subTitle,
                routeKey: mode.routeKey,
                orgId: query.orgFilter || undefined,
                subType: query.subTypeFilter || undefined
            });

            // 当 subType 存在但 categoryCode 为空时，需要确保不会导致后端验证失败
            // 如果 categoryCode 未定义，则不发送 subType 参数以避免验证问题
            const shouldSendSubType = query.subTypeFilter && query.subTypeFilter.trim().length > 0 && mode.categoryCode;

            const result = await archivesApi.getArchives({
                page: currentPage,
                limit: page.pageInfo.pageSize,
                search: query.searchTerm || undefined,
                status: query.statusFilter || mode.defaultStatus,
                categoryCode: mode.categoryCode,
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
                loadPoolData(1, poolStatusFilter);
                isPageChangeFromFilterRef.current = true;
            }
        }
    }, [poolStatusFilter, loadPoolData]);

    // Monitor other filter changes
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const { mode, query, isPoolView, poolStatusFilter, setCurrentPage } = depsRef.current;
        const prevDeps = prevDepsRef.current;
        const depsChanged =
            prevDeps.subTitle !== mode.subTitle ||
            prevDeps.searchTerm !== query.searchTerm ||
            prevDeps.statusFilter !== query.statusFilter ||
            prevDeps.orgFilter !== query.orgFilter ||
            prevDeps.subTypeFilter !== query.subTypeFilter;

        if (depsChanged) {
            prevDepsRef.current.subTitle = mode.subTitle;
            prevDepsRef.current.searchTerm = query.searchTerm;
            prevDepsRef.current.statusFilter = query.statusFilter;
            prevDepsRef.current.orgFilter = query.orgFilter;
            prevDepsRef.current.subTypeFilter = query.subTypeFilter;
            setCurrentPage(1);
            if (isPoolView) {
                loadPoolData(1, poolStatusFilter);
            } else {
                loadArchiveList(1);
            }
            isPageChangeFromFilterRef.current = true;
        }
    }, [mode.subTitle, query.searchTerm, query.statusFilter, query.orgFilter, query.subTypeFilter, loadPoolData, loadArchiveList]);

    // Monitor page changes - 只监听 currentPage 的值变化
    useEffect(() => {
        if (isInitialLoadRef.current) return;
        const { page, poolStatusFilter } = depsRef.current;
        const prevPage = prevDepsRef.current.currentPage;
        if (prevPage !== page.currentPage) {
            prevDepsRef.current.currentPage = page.currentPage;
            if (isPageChangeFromFilterRef.current) {
                isPageChangeFromFilterRef.current = false;
                return;
            }
            loadCurrentView(page.currentPage, poolStatusFilter);
        }
    }, [page.currentPage, poolStatusFilter, loadCurrentView]);

    return {
        loadCurrentView,
        markPageChangeFromFilter: () => {
            isPageChangeFromFilterRef.current = true;
        },
    };
}
