// src/pages/pre-archive/LedgersPreArchiveView.tsx
// Input: URL search params
// Output: 会计账簿库页面
// Pos: src/pages/pre-archive/LedgersPreArchiveView.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { message, Modal } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
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
    // 默认显示"待检测"状态
    const [dashboardFilter, setDashboardFilter] = useState<SimplifiedPreArchiveStatus | null>(
        SimplifiedPreArchiveStatus.PENDING_CHECK
    );

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

    const queryClient = useQueryClient();

    // 批量操作回调：接收状态参数
    const handleBatchArchive = useCallback(async (status: SimplifiedPreArchiveStatus) => {
        const isArchive = status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE;
        const statusLabel = isArchive ? '批量归档' : '批量检测';

        Modal.confirm({
            title: `确认${statusLabel}？`,
            content: `系统将对所有处于“${isArchive ? '可归档' : '待检测'}”状态的会计账簿执行操作。`,
            okText: '确定',
            cancelText: '取消',
            onOk: async () => {
                const hide = message.loading(`正在执行${statusLabel}...`, 0);
                try {
                    // 1. 获取目标 ID (AC02 账簿)
                    const items = await poolApi.getListByStatus(status, 'AC02');
                    if (items.length === 0) {
                        message.info('当前状态下没有可操作的会计账簿');
                        return;
                    }
                    const ids = items.map((item: any) => item.id);

                    if (isArchive) {
                        await poolApi.archiveItems(ids);
                        message.success(`已成功提交 ${ids.length} 份账簿的归档申请`);
                    } else {
                        await poolApi.checkBatch(ids);
                        message.success(`已开始对 ${ids.length} 份账簿进行重新检测`);
                    }

                    // 2. 刷新数据
                    queryClient.invalidateQueries({ queryKey: ['pool'] });
                } catch (error: any) {
                    console.error(`${statusLabel}失败:`, error);
                    message.error(`${statusLabel}执行失败: ${error.message || '系统错误'}`);
                } finally {
                    hide();
                }
            }
        });
    }, [queryClient]);

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
