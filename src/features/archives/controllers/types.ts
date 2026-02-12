/**
 * Archive Controllers - Types
 *
 * Shared types for archive list controller hooks
 */
import { ModuleConfig, GenericRow } from '../../../types';
import { ArchiveRouteMode, RouteConfigMeta as _RouteConfigMeta } from '../routeConfigs';

export type PoolStatusFilter = 'all' | 'PENDING_CHECK' | 'NEEDS_ACTION' | 'READY_TO_MATCH' | 'READY_TO_ARCHIVE' | 'SUBMITTED' | 'COMPLETED' | 'PENDING_APPROVAL' | null;

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

// Public interface (exposed to components)
export interface ControllerData {
    rows: GenericRow[];
    isLoading: boolean;
    errorMessage: string | null;
}

// Internal interface (for hooks only)
export interface ControllerDataInternal extends ControllerData {
    pageInfo: { total: number; page: number; pageSize: number };
    setRows: (rows: GenericRow[]) => void;
    setIsLoading: (loading: boolean) => void;
    setErrorMessage: (error: string | null) => void;
    setPageInfo: (info: { total: number; page: number; pageSize: number }) => void;
    setCurrentPage: (page: number) => void;
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
    data: ControllerDataInternal;  // 使用 Internal 类型以访问 pageInfo
    selection: ControllerSelection;
    pool: ControllerPool;
    ui: ControllerUI;
    actions: ControllerActions;
}

// Hook options types
export interface UseArchiveModeOptions {
    routeConfig?: string;
    title?: string;
    subTitle?: string;
    config?: any;
}

export interface UseArchiveDataLoadOptions {
    mode: ControllerMode;
    query: ControllerQuery;
    page: ControllerPage;
    isPoolView: boolean;
    poolStatusFilter?: PoolStatusFilter;
    categoryFilter?: string | null; // 新增
}

export interface UseArchiveActionsOptions {
    mode: ControllerMode;
    query: ControllerQuery;
    page: ControllerPage;
    data: Omit<ControllerData, 'setRows' | 'setIsLoading' | 'setErrorMessage' | 'setPageInfo' | 'setCurrentPage'>;
    pool?: ControllerPool;
    reload: (page?: number) => Promise<void>;
    showToast: (message: string, type?: 'success' | 'error') => void;
}
