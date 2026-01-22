// src/pages/pre-archive/LedgersPreArchiveView.tsx
// Input: URL search params
// Output: 会计账簿库页面
// Pos: src/pages/pre-archive/LedgersPreArchiveView.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { PoolKanbanView } from '@/components/pool-kanban';
import { PoolDashboard } from '@/components/pool-dashboard';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
import { PRE_ARCHIVE_LEDGER_CONFIG } from '@/config/pre-archive-columns.config';
import './PoolPage.css';

const VIEW_MODE_STORAGE_KEY = 'pool.viewMode';
const DEFAULT_VIEW_MODE: ViewMode = 'list';

type ViewMode = 'list' | 'kanban';

interface ViewSwitcherProps {
    currentMode: ViewMode;
    onModeChange: (mode: ViewMode) => void;
}

function ViewSwitcher({ currentMode, onModeChange }: ViewSwitcherProps) {
    return (
        <div className="pool-page__switcher">
            <button
                type="button"
                className={currentMode === 'list' ? 'active' : ''}
                onClick={() => onModeChange('list')}
                title="列表视图"
            >
                <List size={16} />
                列表
            </button>
            <button
                type="button"
                className={currentMode === 'kanban' ? 'active' : ''}
                onClick={() => onModeChange('kanban')}
                title="看板视图"
            >
                <Columns3 size={16} />
                看板
            </button>
        </div>
    );
}

/**
 * 会计账簿库页面
 * 对应 DA/T 94 门类: AC02 (会计账簿)
 */
export const LedgersPreArchiveView: React.FC = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();

    const getInitialViewMode = useCallback((): ViewMode => {
        const viewParam = searchParams.get('view') as ViewMode | null;
        if (viewParam === 'list' || viewParam === 'kanban') return viewParam;
        return localStorage.getItem(VIEW_MODE_STORAGE_KEY) as ViewMode || DEFAULT_VIEW_MODE;
    }, [searchParams]);

    const [viewMode, setViewMode] = useState<ViewMode>(getInitialViewMode);
    // 默认展示全部状态 (null)，以免过滤掉新上传的 "待检测" (PENDING_CHECK) 文件
    const [dashboardFilter, setDashboardFilter] = useState<SimplifiedPreArchiveStatus | null>(null);

    // 固定门类为 AC02 (会计账簿)
    const [categoryFilter] = useState<string | null>('AC02');

    useEffect(() => {
        const viewParam = searchParams.get('view') as ViewMode | null;
        if (viewParam && (viewParam === 'list' || viewParam === 'kanban') && viewParam !== viewMode) {
            setViewMode(viewParam);
        }
    }, [searchParams, viewMode]);

    const handleViewChange = useCallback((mode: ViewMode) => {
        setViewMode(mode);
        setSearchParams({ view: mode });
        localStorage.setItem(VIEW_MODE_STORAGE_KEY, mode);
    }, [setSearchParams]);

    const handleBatchArchive = useCallback(() => {
        console.log('Batch archive for Ledgers');
    }, []);

    return (
        <div className="pool-page">
            <div className="pool-page__header">
                <div className="pool-page__title-section">
                    <h1 className="pool-page__title">会计账簿库</h1>
                </div>
                <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
            </div>

            <PoolDashboard
                activeFilter={dashboardFilter}
                onFilterChange={setDashboardFilter}
                categoryFilter={categoryFilter}
                onCategoryChange={() => { }}
                showCategoryPicker={false}
                showActions={true}
                onBatchArchive={handleBatchArchive}
            />

            <div className="pool-page__content">
                {viewMode === 'kanban' ? (
                    <PoolKanbanView
                        filter={dashboardFilter}
                        categoryFilter={categoryFilter}
                    />
                ) : (
                    <ArchiveListPage
                        routeConfig="pool"
                        statusFilter={dashboardFilter}
                        categoryFilter={categoryFilter}
                        subTitle="会计账簿库"
                        config={PRE_ARCHIVE_LEDGER_CONFIG}
                    />
                )}
            </div>
        </div>
    );
};

export default LedgersPreArchiveView;
