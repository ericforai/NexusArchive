// Input: 路由配置、API 客户端、状态管理
// Output: useArchiveListController Hook
// Pos: src/features/archives/useArchiveListController.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive List Controller Hook - Refactored
 *
 * 核心业务逻辑层，将数据获取、状态管理、分页、筛选等逻辑从 View 中抽离。
 * View 组件只需消费 Controller 输出并渲染 UI。
 *
 * === REFACTORED ===
 * Previously 650 lines with mixed responsibilities.
 * Now ~150 lines as a compositor of specialized hooks.
 */
import React from 'react';
import { useArchiveMode } from './controllers/useArchiveMode';
import { useArchiveQuery } from './controllers/useArchiveQuery';
import { useArchivePagination } from './controllers/useArchivePagination';
import { useArchiveData } from './controllers/useArchiveData';
import { useArchiveDataLoader } from './controllers/useArchiveDataLoader';
import { useArchivePool } from './controllers/useArchivePool';
import { useArchiveToast } from './controllers/useArchiveToast';
import { useArchiveCsvActions } from './controllers/useArchiveControllerActions';
import { ArchiveListController } from './controllers/types';

// Re-export types for backward compatibility
export * from './controllers/types';

// Re-export individual hooks for advanced usage
export { useArchiveMode } from './controllers/useArchiveMode';
export { useArchiveQuery } from './controllers/useArchiveQuery';
export { useArchivePagination } from './controllers/useArchivePagination';
export { useArchiveData } from './controllers/useArchiveData';
export { useArchivePool } from './controllers/useArchivePool';
export { useArchiveToast } from './controllers/useArchiveToast';
export { useArchiveCsvActions } from './controllers/useArchiveControllerActions';

// Re-export utility functions
export { mapArchiveToRow, resolveCategoryLabel, formatStatus } from './controllers/utils';

/**
 * Main Archive List Controller Hook
 *
 * Now acts as a compositor, combining specialized hooks.
 * Each hook has a single, well-defined responsibility.
 */
export function useArchiveListController(options: any): ArchiveListController {
    const mode = useArchiveMode(options);
    const query = useArchiveQuery();
    const page = useArchivePagination();
    const data = useArchiveData(page.pageInfo.pageSize);
    const pool = useArchivePool({ isEnabled: mode.isPoolView });
    const ui = useArchiveToast();
    const { loadCurrentView } = useArchiveDataLoader({
        mode, query, page,
        isPoolView: mode.isPoolView,
        poolStatusFilter: pool.statusFilter,
        data, showToast: ui.showToast,
    });
    const selection = useArchiveSelectionInline(data.rows);
    const actions = useArchiveCsvActions({
        mode, query, page,
        data: { rows: data.rows, isLoading: data.isLoading, errorMessage: data.errorMessage },
        pool: mode.isPoolView ? pool : undefined,
        reload: loadCurrentView,
        showToast: ui.showToast,
    });

    return {
        mode, query, page,
        data: { rows: data.rows, isLoading: data.isLoading, errorMessage: data.errorMessage },
        selection, pool, ui, actions,
    };
}

function useArchiveSelectionInline(rows: any[]) {
    const [selectedIds, setSelectedIds] = React.useState<string[]>([]);
    const toggle = React.useCallback((id: string) => {
        setSelectedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
    }, []);
    const toggleAll = React.useCallback(() => {
        setSelectedIds(prev => (prev.length === rows.length ? [] : rows.map(r => r.id)));
    }, [selectedIds.length, rows]);
    const clear = React.useCallback(() => setSelectedIds([]), []);
    return {
        selectedIds, toggle, toggleAll, clear,
        allSelected: rows.length > 0 && selectedIds.length === rows.length,
    };
}
