// Input: 路由配置、API 客户端、状态管理
// Output: useArchiveListController Hook
// Pos: src/features/archives/useArchiveListController.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive List Controller Hook
 * 
 * 核心业务逻辑层，将数据获取、状态管理、分页、筛选等逻辑从 View 中抽离。
 * View 组件只需消费 Controller 输出并渲染 UI。
 */
import { useState, useCallback, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import {
    ArchiveRouteMode,
    ROUTE_CONFIG_MAP,
    DEFAULT_ROUTE_CONFIG,
    RouteConfigMeta
} from './routeConfigs';
import { ModuleConfig, GenericRow } from '../../types';
import { poolApi, PoolItem } from '../../api/pool';
import { archivesApi } from '../../api/archives';
import { adminApi } from '../../api/admin';
import { client } from '../../api/client';
import { GENERIC_CONFIG } from '../../constants';

// ============ 类型定义 ============

export type PoolStatusFilter = 'all' | 'PENDING_CHECK' | 'CHECK_FAILED' | 'PENDING_METADATA' | 'PENDING_ARCHIVE' | 'PENDING_APPROVAL' | 'ARCHIVED' | null;

export interface ControllerMode {
    routeKey: ArchiveRouteMode | undefined;
    title: string;
    subTitle: string;
    config: ModuleConfig;
    isPoolView: boolean;
    isLinkingView: boolean;
    categoryCode?: 'AC01' | 'AC02' | 'AC03' | 'AC04';
    defaultStatus?: string;
}

export interface ControllerQuery {
    searchTerm: string;
    setSearchTerm: (v: string) => void;
    statusFilter: string;
    setStatusFilter: (v: string) => void;
    orgFilter: string;
    setOrgFilter: (v: string) => void;
    orgOptions: { label: string; value: string }[];
    subTypeFilter: string;
    setSubTypeFilter: (v: string) => void;
}

export interface ControllerPage {
    currentPage: number;
    pageInfo: { total: number; page: number; pageSize: number };
    setCurrentPage: (p: number) => void;
}

export interface ControllerData {
    rows: GenericRow[];
    isLoading: boolean;
    errorMessage: string | null;
}

export interface ControllerSelection {
    selectedIds: string[];
    toggle: (id: string) => void;
    toggleAll: () => void;
    clear: () => void;
    allSelected: boolean;
}

export interface ControllerPool {
    statusFilter: PoolStatusFilter;
    setStatusFilter: (v: PoolStatusFilter) => void;
    statusStats: Record<string, number>;
    refreshStats: () => Promise<void>;
}

export interface ControllerToast {
    visible: boolean;
    message: string;
    type: 'success' | 'error';
}

export interface ControllerUI {
    toast: ControllerToast;
    showToast: (m: string, t?: 'success' | 'error') => void;
}

export interface ControllerActions {
    reload: (page?: number) => Promise<void>;
    exportCsv: () => void;
}

export interface ArchiveListController {
    mode: ControllerMode;
    query: ControllerQuery;
    page: ControllerPage;
    data: ControllerData;
    selection: ControllerSelection;
    pool: ControllerPool;
    ui: ControllerUI;
    actions: ControllerActions;
}

// ============ 辅助函数 ============

const CATEGORY_LABELS: Record<string, string> = {
    AC01: '会计凭证',
    AC02: '会计账簿',
    AC03: '财务报告',
    AC04: '其他会计资料'
};

const STATUS_LABELS: Record<string, string> = {
    draft: '草稿',
    MATCH_PENDING: '待匹配',
    MATCHED: '匹配成功',
    pending: '准备归档',
    archived: '已归档'
};

const PRE_ARCHIVE_STATUS_LABELS: Record<string, { label: string }> = {
    PENDING_CHECK: { label: '待检测' },
    CHECK_FAILED: { label: '检测失败' },
    PENDING_METADATA: { label: '待补录' },
    MATCH_PENDING: { label: '待匹配' },
    MATCHED: { label: '匹配成功' },
    PENDING_ARCHIVE: { label: '准备归档' },
    PENDING_APPROVAL: { label: '归档审批中' },
    ARCHIVING: { label: '归档中' },
    ARCHIVED: { label: '已归档' },
};

const resolveCategoryLabel = (code?: string) => {
    if (!code) return '档案';
    return CATEGORY_LABELS[code] || code;
};

const formatStatus = (status?: string) => {
    if (!status) return '-';
    return STATUS_LABELS[status] || PRE_ARCHIVE_STATUS_LABELS[status]?.label || status;
};

const getSafeDisplayValue = (text: string | undefined | null) => {
    if (!text) return '-';
    const isHash = text.length > 30 && !text.includes(' ') && /^[a-fA-F0-9]+$/.test(text);
    if (isHash) return '密级文档(标题加密)';
    return text;
};

const mapArchiveToRow = (archive: any, subTitle: string): GenericRow => {
    const categoryLabel = resolveCategoryLabel(archive?.categoryCode);
    const baseDate = archive?.docDate || archive?.createdAt || archive?.createdTime || '';
    const date = baseDate ? String(baseDate).split('T')[0] : '';
    const statusText = formatStatus(archive?.status);

    if (subTitle === '会计凭证' || subTitle === '凭证关联') {
        const amountValue = archive?.amount;
        const amount = typeof amountValue === 'number'
            ? `¥ ${amountValue.toFixed(2)}`
            : amountValue || '-';

        let subjectName = '-';
        try {
            if (archive?.customMetadata) {
                const meta = typeof archive.customMetadata === 'string'
                    ? JSON.parse(archive.customMetadata)
                    : archive.customMetadata;
                if (Array.isArray(meta) && meta.length > 0) {
                    const entry = meta.find((m: any) => m.accsubject && (m.debit_org > 0 || m.debitLocal > 0)) || meta[0];
                    if (entry?.accsubject?.name) {
                        subjectName = entry.accsubject.name;
                    } else if (entry?.description) {
                        subjectName = entry.description;
                    }
                }
            }
            if (subjectName === '-' && archive?.title) {
                subjectName = archive.title;
            }
            subjectName = getSafeDisplayValue(subjectName);
        } catch (e) {
            console.warn('Failed to parse custom metadata for subject', e);
        }

        return {
            id: archive?.id,
            code: archive?.archiveCode,
            voucherNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            entity: archive?.orgName,
            period: archive?.fiscalPeriod || archive?.fiscalYear || '',
            subject: subjectName,
            type: categoryLabel,
            amount,
            date,
            status: statusText,
            matchScore: archive?.matchScore || 0,
            autoLink: archive?.matchMethod || '-',
            rawStatus: archive?.status
        };
    }

    if (subTitle === '会计账簿') {
        return {
            id: archive?.id,
            code: archive?.archiveCode,
            ledgerNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            type: categoryLabel,
            entity: archive?.orgName,
            year: archive?.fiscalYear,
            period: archive?.fiscalPeriod || '全年',
            subject: getSafeDisplayValue(archive?.title),
            pageCount: archive?.pageCount || '-',
            status: statusText,
            rawStatus: archive?.preArchiveStatus || archive?.status
        };
    }

    if (subTitle === '财务报告') {
        return {
            id: archive?.id,
            code: archive?.archiveCode,
            reportNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            type: categoryLabel,
            year: archive?.fiscalYear,
            unit: archive?.orgName,
            title: getSafeDisplayValue(archive?.title),
            period: archive?.fiscalPeriod || '',
            date,
            status: statusText,
            rawStatus: archive?.preArchiveStatus || archive?.status
        };
    }

    return {
        id: archive?.id,
        code: archive?.archiveCode,
        archiveNo: archive?.fondsNo,
        archivalCode: archive?.archiveCode,
        category: categoryLabel,
        year: archive?.fiscalYear,
        period: archive?.retentionPeriod || archive?.fiscalPeriod,
        title: getSafeDisplayValue(archive?.title),
        security: archive?.securityLevel || '-',
        status: statusText,
        orgName: archive?.orgName,
        date,
        rawStatus: archive?.preArchiveStatus || archive?.status
    };
};

// ============ Hook 实现 ============

interface ControllerOptions {
    routeConfig?: ArchiveRouteMode;
    // 向后兼容的传统 Props
    title?: string;
    subTitle?: string;
    config?: ModuleConfig;
}

export function useArchiveListController(options: ControllerOptions): ArchiveListController {
    const { routeConfig, title: propTitle, subTitle: propSubTitle, config: propConfig } = options;

    // ===== Mode 解析 =====
    const resolvedConfig: RouteConfigMeta | undefined = routeConfig
        ? ROUTE_CONFIG_MAP[routeConfig]
        : undefined;

    const title = resolvedConfig?.title || propTitle || DEFAULT_ROUTE_CONFIG.title;
    const subTitle = resolvedConfig?.subTitle || propSubTitle || DEFAULT_ROUTE_CONFIG.subTitle;
    const config = resolvedConfig?.config || propConfig || GENERIC_CONFIG;

    const isPoolView = subTitle === '电子凭证池';
    const isLinkingView = subTitle === '凭证关联';

    const resolveCategoryCode = useCallback((): 'AC01' | 'AC02' | 'AC03' | 'AC04' | undefined => {
        switch (subTitle) {
            case '会计凭证':
            case '凭证关联':
                return 'AC01';
            case '会计账簿':
                return 'AC02';
            case '财务报告':
                return 'AC03';
            case '其他会计资料':
                return 'AC04';
            default:
                return undefined;
        }
    }, [subTitle]);

    const resolveDefaultStatus = useCallback(() => {
        if (subTitle === '凭证关联') return 'draft,MATCH_PENDING,MATCHED';
        if (['会计凭证', '会计账簿', '财务报告', '其他会计资料'].includes(subTitle || '')) {
            return 'archived';
        }
        return undefined;
    }, [subTitle]);

    const mode: ControllerMode = {
        routeKey: routeConfig,
        title,
        subTitle,
        config,
        isPoolView,
        isLinkingView,
        categoryCode: resolveCategoryCode(),
        defaultStatus: resolveDefaultStatus(),
    };

    // ===== Query 状态 =====
    const location = useLocation();
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [orgFilter, setOrgFilter] = useState('');
    const [subTypeFilter, setSubTypeFilter] = useState(new URLSearchParams(location.search).get('type') || '');
    const [orgOptions, setOrgOptions] = useState<{ label: string; value: string }[]>([]);

    // 同步 URL 查询参数
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        setSubTypeFilter(params.get('type') || '');
    }, [location.search]);

    // 加载组织列表
    useEffect(() => {
        const loadOrgs = async () => {
            try {
                const res = await adminApi.listOrg();
                if (res.code === 200 && res.data) {
                    setOrgOptions(
                        (res.data as any[]).map((o) => ({
                            label: o.name,
                            value: o.id
                        }))
                    );
                }
            } catch (e) {
                // ignore
            }
        };
        loadOrgs();
    }, []);

    const query: ControllerQuery = {
        searchTerm,
        setSearchTerm,
        statusFilter,
        setStatusFilter,
        orgFilter,
        setOrgFilter,
        orgOptions,
        subTypeFilter,
        setSubTypeFilter,
    };

    // ===== Page 状态 =====
    const [currentPage, setCurrentPage] = useState(1);
    const [pageInfo, setPageInfo] = useState({ total: 0, page: 1, pageSize: 10 });

    const page: ControllerPage = {
        currentPage,
        pageInfo,
        setCurrentPage,
    };

    // ===== Data 状态 =====
    const [rows, setRows] = useState<GenericRow[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const data: ControllerData = {
        rows,
        isLoading,
        errorMessage,
    };

    // ===== Selection 状态 =====
    const [selectedIds, setSelectedIds] = useState<string[]>([]);

    const toggleSelection = useCallback((id: string) => {
        setSelectedIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
        );
    }, []);

    const toggleAll = useCallback(() => {
        if (selectedIds.length === rows.length) {
            setSelectedIds([]);
        } else {
            setSelectedIds(rows.map(r => r.id));
        }
    }, [selectedIds.length, rows]);

    const clearSelection = useCallback(() => {
        setSelectedIds([]);
    }, []);

    const selection: ControllerSelection = {
        selectedIds,
        toggle: toggleSelection,
        toggleAll,
        clear: clearSelection,
        allSelected: rows.length > 0 && selectedIds.length === rows.length,
    };

    // ===== Pool 专用状态 =====
    const [poolStatusFilter, setPoolStatusFilter] = useState<PoolStatusFilter>(null);
    const [poolStatusStats, setPoolStatusStats] = useState<Record<string, number>>({});

    const refreshPoolStats = useCallback(async () => {
        try {
            const response = await client.get('/pool/stats/status');
            if (response.data.code === 200) {
                setPoolStatusStats(response.data.data || {});
            }
        } catch (error) {
            console.error('Failed to load pool status stats:', error);
        }
    }, []);

    useEffect(() => {
        if (isPoolView) {
            refreshPoolStats();
        }
    }, [isPoolView, refreshPoolStats]);

    const pool: ControllerPool = {
        statusFilter: poolStatusFilter,
        setStatusFilter: setPoolStatusFilter,
        statusStats: poolStatusStats,
        refreshStats: refreshPoolStats,
    };

    // ===== Toast UI =====
    const [toast, setToast] = useState<ControllerToast>({ visible: false, message: '', type: 'success' });

    const showToast = useCallback((message: string, type: 'success' | 'error' = 'success') => {
        setToast({ visible: true, message, type });
        setTimeout(() => {
            setToast(prev => ({ ...prev, visible: false }));
        }, 3000);
    }, []);

    const ui: ControllerUI = {
        toast,
        showToast,
    };

    // ===== 数据加载 =====
    const loadPoolData = useCallback(async (pageNum = currentPage) => {
        setIsLoading(true);
        setErrorMessage(null);
        setSelectedIds([]);
        try {
            const poolItems = await poolApi.getList();
            let filtered = poolItems.filter((item) => {
                const itemStatus = (item as any).status || 'PENDING_CHECK';
                if (poolStatusFilter && poolStatusFilter !== 'all') {
                    if (itemStatus !== poolStatusFilter) return false;
                } else if (statusFilter) {
                    if (itemStatus !== statusFilter) return false;
                }
                if (!searchTerm) return true;
                return Object.values(item).some((val) =>
                    String(val || '').toLowerCase().includes(searchTerm.toLowerCase())
                );
            });
            const total = filtered.length;
            const start = (pageNum - 1) * pageInfo.pageSize;
            const paged = filtered.slice(start, start + pageInfo.pageSize);
            const mappedPaged = paged.map((item: any) => ({
                ...item,
                rawStatus: item.status
            }));
            setRows(mappedPaged as GenericRow[]);
            setPageInfo(prev => ({ ...prev, total, page: pageNum }));
            refreshPoolStats();
        } catch (error) {
            console.error('Failed to load pool data:', error);
            setErrorMessage('加载数据失败');
            showToast('加载数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, [currentPage, pageInfo.pageSize, searchTerm, poolStatusFilter, statusFilter, refreshPoolStats, showToast]);

    const loadArchiveList = useCallback(async (pageNum = currentPage) => {
        setIsLoading(true);
        setErrorMessage(null);
        setSelectedIds([]);
        try {
            const result = await archivesApi.getArchives({
                page: pageNum,
                limit: pageInfo.pageSize,
                search: searchTerm || undefined,
                status: statusFilter || mode.defaultStatus,
                categoryCode: mode.categoryCode,
                orgId: orgFilter || undefined,
                subType: subTypeFilter || undefined
            });

            if (result.code !== 200 || !result.data) {
                throw new Error(result.message || '加载档案数据失败');
            }

            const pageResult: any = result.data;
            const { records = [], total = 0, size = pageInfo.pageSize, current = pageNum } = pageResult;
            const mappedItems = (records as any[]).map((item) => mapArchiveToRow(item, subTitle));
            setRows(mappedItems);
            setPageInfo({ total, page: current || pageNum, pageSize: size || pageInfo.pageSize });
            setCurrentPage(current || pageNum);
        } catch (error) {
            console.error('Failed to load archives:', error);
            setErrorMessage(error instanceof Error ? error.message : '加载档案数据失败');
            showToast('加载档案数据失败', 'error');
        } finally {
            setIsLoading(false);
        }
    }, [currentPage, pageInfo.pageSize, searchTerm, statusFilter, mode.defaultStatus, mode.categoryCode, orgFilter, subTitle, subTypeFilter, showToast]);

    const loadCurrentView = useCallback((pageNum = currentPage) => {
        if (isPoolView) {
            return loadPoolData(pageNum);
        }
        return loadArchiveList(pageNum);
    }, [isPoolView, loadPoolData, loadArchiveList, currentPage]);

    // 依赖变化时重置分页并加载
    const prevDepsRef = useRef({ subTitle, searchTerm, statusFilter, orgFilter, subTypeFilter, poolStatusFilter });

    useEffect(() => {
        const prevDeps = prevDepsRef.current;
        const depsChanged =
            prevDeps.subTitle !== subTitle ||
            prevDeps.searchTerm !== searchTerm ||
            prevDeps.statusFilter !== statusFilter ||
            prevDeps.orgFilter !== orgFilter ||
            prevDeps.subTypeFilter !== subTypeFilter ||
            prevDeps.poolStatusFilter !== poolStatusFilter;

        if (depsChanged) {
            prevDepsRef.current = { subTitle, searchTerm, statusFilter, orgFilter, subTypeFilter, poolStatusFilter };
            setCurrentPage(1);
            loadCurrentView(1);
        } else {
            loadCurrentView(currentPage);
        }
    }, [subTitle, searchTerm, statusFilter, orgFilter, subTypeFilter, poolStatusFilter, currentPage, loadCurrentView]);

    // ===== Actions =====
    const exportCsv = useCallback(() => {
        if (rows.length === 0) {
            showToast('没有数据可导出', 'error');
            return;
        }
        const headers = config.columns.map((c: any) => c.header).join(',');
        const csvRows = rows.map(row => config.columns.map((c: any) => row[c.key]).join(',')).join('\n');
        const csvContent = `data:text/csv;charset=utf-8,\uFEFF${headers}\n${csvRows}`;
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `${title}_${subTitle || 'data'}_export.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast('导出成功，正在下载文件');
    }, [rows, config, title, subTitle, showToast]);

    const actions: ControllerActions = {
        reload: loadCurrentView,
        exportCsv,
    };

    return {
        mode,
        query,
        page,
        data,
        selection,
        pool,
        ui,
        actions,
    };
}
