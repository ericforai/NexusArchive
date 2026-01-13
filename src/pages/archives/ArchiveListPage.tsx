// Input: ArchiveListView 组件、路由配置类型、Controller Hook
// Output: ArchiveListPage 页面容器
// Pos: src/pages/archives/ArchiveListPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive List Page Container
 * 
 * 职责：接收路由 routeConfig，通过 Controller 获取业务数据，传递给 ArchiveListView。
 * View 只负责渲染，所有业务逻辑由 Controller 提供。
 */
import React, { Suspense, lazy, useEffect } from 'react';
import { ArchiveRouteMode, useArchiveActions, useArchiveListController, PoolStatusFilter } from '../../features/archives';
import { SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';

// 简化状态到旧状态的映射（用于筛选同步）
const SIMPLIFIED_TO_OLD_STATUS: Record<SimplifiedPreArchiveStatus, PoolStatusFilter> = {
    'PENDING_CHECK': 'PENDING_CHECK',
    'NEEDS_ACTION': 'CHECK_FAILED',
    'READY_TO_MATCH': 'PENDING_METADATA',
    'READY_TO_ARCHIVE': 'PENDING_ARCHIVE',
    'COMPLETED': 'ARCHIVED',
};

// 诊断：模块加载日志
console.log('%c[ArchiveListPage] MODULE LOADED', 'color: #ef4444; font-weight: bold; font-size: 18px;');

// 懒加载 ArchiveListView 以保持路由级代码分割
const ArchiveListView = lazy(() => {
    console.log('%c[ArchiveListPage] LAZY LOADING ArchiveListView...', 'color: #f59e0b; font-weight: bold;');
    return import('./ArchiveListView');
});

interface ArchiveListPageProps {
    routeConfig: ArchiveRouteMode;
    /** 仪表盘筛选状态 (仅用于 pool 视图) */
    statusFilter?: SimplifiedPreArchiveStatus | null;
}

const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

export const ArchiveListPage: React.FC<ArchiveListPageProps> = ({ routeConfig, statusFilter }) => {
    // 1. 获取核心业务数据与状态
    const controller = useArchiveListController({ routeConfig });

    // 2. 获取操作 Action
    const actions = useArchiveActions(controller);

    // 3. 同步仪表盘筛选到列表状态
    useEffect(() => {
        if (routeConfig === 'pool' && statusFilter !== undefined) {
            // 将 SimplifiedPreArchiveStatus 映射到旧状态
            const targetStatus = statusFilter ? SIMPLIFIED_TO_OLD_STATUS[statusFilter] : null;
            controller.pool.setStatusFilter(targetStatus);
        }
    }, [statusFilter, routeConfig, controller.pool]);

    // TODO: 后续 View 重构时，将 controller 和 actions 传递给 View 组件
    // 例如：<ArchiveListView controller={controller} actions={actions} />

    return (
        <Suspense fallback={<LoadingFallback />}>
            {/* 传递 Controller 和 Actions 给 View */}
            <ArchiveListView
                routeConfig={routeConfig}
                controller={controller}
                actions={actions}
            />
        </Suspense>
    );
};

export default ArchiveListPage;
