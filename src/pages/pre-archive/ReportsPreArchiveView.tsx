// src/pages/pre-archive/ReportsPreArchiveView.tsx
// Input: URL search params
// Output: 财务报告库页面
// Pos: src/pages/pre-archive/ReportsPreArchiveView.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { message, Modal } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { poolApi } from '@/api/pool';
import { usePoolDashboard } from '@/hooks/usePoolDashboard';
import { PoolKanbanView } from '@/components/pool-kanban';
import { PoolDashboard } from '@/components/pool-dashboard';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
import { PRE_ARCHIVE_REPORT_CONFIG } from '@/config/pre-archive-columns.config';
import './PoolPage.css';

const VIEW_MODE_STORAGE_KEY = 'pool.viewMode'; // 复用设置
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
 * 财务报告库页面
 * 对应 DA/T 94 门类: AC03 (财务报告)
 */
export const ReportsPreArchiveView: React.FC = () => {
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

    // 固定门类为 AC03 (财务报告)
    const [categoryFilter] = useState<string | null>('AC03');

    // 获取仪表盘统计数据
    const { stats } = usePoolDashboard({ categoryFilter });

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
        const statusLabel = isArchive ? '批量归档' : '重新检测';

        Modal.confirm({
            title: `确认${statusLabel}？`,
            content: `系统将对所有处于“${isArchive ? '可归档' : '待检测'}”状态的财务报告执行操作。`,
            okText: '确定',
            cancelText: '取消',
            onOk: async () => {
                const hide = message.loading(`正在执行${statusLabel}...`, 0);
                try {
                    // 1. 获取目标 ID (AC03 财务报告)
                    const items = await poolApi.getListByStatus(status, 'AC03');
                    if (items.length === 0) {
                        message.info('当前状态下没有可操作的财务报告');
                        return;
                    }
                    const ids = items.map((item: any) => item.id);

                    if (isArchive) {
                        await poolApi.archiveItems(ids);
                        message.success(`已成功提交 ${ids.length} 份财务报告的归档申请`);
                    } else {
                        await poolApi.checkBatch(ids);
                        message.success(`已开始对 ${ids.length} 份财务报告进行重新检测`);
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
                    <h1 className="pool-page__title">财务报告库</h1>
                </div>
                <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
            </div>

            <PoolDashboard
                stats={stats}
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
                        subTitle="财务报告库"
                        config={PRE_ARCHIVE_REPORT_CONFIG}
                    />
                )}
            </div>
        </div>
    );
};

export default ReportsPreArchiveView;
