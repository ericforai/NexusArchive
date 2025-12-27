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
import React, { Suspense, lazy } from 'react';
import { ArchiveRouteMode, useArchiveActions, useArchiveListController } from '../../features/archives';

// 懒加载 ArchiveListView 以保持路由级代码分割
const ArchiveListView = lazy(() => import('./ArchiveListView'));

interface ArchiveListPageProps {
    routeConfig: ArchiveRouteMode;
}

const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

export const ArchiveListPage: React.FC<ArchiveListPageProps> = ({ routeConfig }) => {
    // 1. 获取核心业务数据与状态
    const controller = useArchiveListController({ routeConfig });

    // 2. 获取操作 Action
    const actions = useArchiveActions(controller);

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
