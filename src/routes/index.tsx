/**
 * 路由配置中心
 * 
 * 企业级路由架构，采用 React Router v7 嵌套路由 + 懒加载
 */
import React, { lazy, Suspense } from 'react';
import { Navigate, RouteObject } from 'react-router-dom';
import { ProductWebsite } from '../components/ProductWebsite';

// 布局组件（非懒加载，因为是框架级别）
import { SystemLayout } from '../layouts/SystemLayout';
import { ProtectedRoute } from '../components/auth/ProtectedRoute';
import { LoginView } from '../components/LoginView';

// 懒加载各功能模块
const Dashboard = lazy(() => import('../components/Dashboard'));
const ArchivalPanoramaView = lazy(() => import('../components/ArchivalPanoramaView'));
const ArchiveListView = lazy(() => import('../components/ArchiveListView'));
const OCRProcessingView = lazy(() => import('../components/OCRProcessingView'));
const AbnormalDataView = lazy(() => import('../components/AbnormalDataView'));
const OnlineReceptionView = lazy(() => import('../components/OnlineReceptionView'));
const VolumeManagement = lazy(() => import('../components/VolumeManagement'));
const ArchiveApprovalView = lazy(() => import('../components/ArchiveApprovalView'));
const OpenAppraisalView = lazy(() => import('../components/OpenAppraisalView'));
const DestructionView = lazy(() => import('../components/DestructionView'));
const RelationshipQueryView = lazy(() => import('../components/RelationshipQueryView'));
const BorrowingView = lazy(() => import('../components/BorrowingView'));
const WarehouseView = lazy(() => import('../components/WarehouseView'));
const StatsView = lazy(() => import('../components/StatsView'));
const ComplianceReportView = lazy(() => import('../components/ComplianceReportView'));
const AdminLayout = lazy(() => import('../components/admin/AdminLayout'));

// 系统设置模块（Tab 拆分子路由）
const SettingsLayout = lazy(() => import('../components/settings/SettingsLayout'));
const BasicSettings = lazy(() => import('../components/settings/BasicSettings'));
const UserSettings = lazy(() => import('../components/settings/UserSettings'));
const RoleSettings = lazy(() => import('../components/settings/RoleSettings'));
const OrgSettings = lazy(() => import('../components/settings/OrgSettings'));
const SecuritySettings = lazy(() => import('../components/settings/SecuritySettings'));
const FondsManagement = lazy(() => import('../components/admin/FondsManagement'));
const AuditLogView = lazy(() => import('../components/AuditLogView'));

// 加载占位符
const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

// 包装懒加载组件
const withSuspense = (Component: React.LazyExoticComponent<any>, props?: any) => (
    <Suspense fallback={<LoadingFallback />}>
        <Component {...props} />
    </Suspense>
);

/**
 * 路由配置
 */
export const routes: RouteObject[] = [
    // 产品官网
    { path: '/', element: <ProductWebsite /> },

    // 登录页（独立于 SystemLayout）
    { path: '/system/login', element: <LoginView /> },

    // 业务系统（需登录）
    {
        path: '/system',
        element: (
            <ProtectedRoute>
                <SystemLayout />
            </ProtectedRoute>
        ),
        children: [
            // 门户首页
            { index: true, element: withSuspense(Dashboard) },

            // 全景视图（支持可选参数）
            { path: 'panorama/:id?', element: withSuspense(ArchivalPanoramaView) },

            // ========== 预归档库 ==========
            { path: 'pre-archive', element: withSuspense(ArchiveListView, { routeConfig: 'pool' }) },
            { path: 'pre-archive/pool', element: withSuspense(ArchiveListView, { routeConfig: 'pool' }) },
            { path: 'pre-archive/ocr', element: withSuspense(OCRProcessingView) },
            { path: 'pre-archive/link', element: withSuspense(ArchiveListView, { routeConfig: 'link' }) },
            { path: 'pre-archive/abnormal', element: withSuspense(AbnormalDataView) },

            // ========== 资料收集 ==========
            { path: 'collection', element: withSuspense(ArchiveListView, { routeConfig: 'collection' }) },
            { path: 'collection/online', element: withSuspense(OnlineReceptionView) },
            { path: 'collection/scan', element: withSuspense(ArchiveListView, { routeConfig: 'scan' }) },
            { path: 'collection/upload', element: withSuspense(ArchiveListView, { routeConfig: 'collection' }) },

            // ========== 档案管理 ==========
            { path: 'archive', element: withSuspense(ArchiveListView, { routeConfig: 'view' }) },
            { path: 'archive/vouchers', element: withSuspense(ArchiveListView, { routeConfig: 'voucher' }) },
            { path: 'archive/ledgers', element: withSuspense(ArchiveListView, { routeConfig: 'ledger' }) },
            { path: 'archive/reports', element: withSuspense(ArchiveListView, { routeConfig: 'report' }) },
            { path: 'archive/other', element: withSuspense(ArchiveListView, { routeConfig: 'other' }) },
            { path: 'archive/boxing', element: withSuspense(ArchiveListView, { routeConfig: 'box' }) },
            { path: 'archive/volume', element: withSuspense(VolumeManagement) },
            { path: 'archive/approval', element: withSuspense(ArchiveApprovalView) },
            { path: 'archive/open-appraisal', element: withSuspense(OpenAppraisalView) },
            { path: 'archive/destruction', element: withSuspense(DestructionView) },
            { path: 'archive/compliance/:id', element: withSuspense(ComplianceReportView) },

            // ========== 档案查询 ==========
            { path: 'query', element: withSuspense(ArchiveListView, { routeConfig: 'query' }) },
            { path: 'query/relationship', element: withSuspense(RelationshipQueryView) },

            // ========== 档案借阅 ==========
            { path: 'borrowing', element: withSuspense(BorrowingView) },

            // ========== 档案销毁 ==========
            { path: 'destruction', element: withSuspense(DestructionView) },

            // ========== 库房管理 ==========
            { path: 'warehouse', element: withSuspense(WarehouseView) },
            { path: 'warehouse/env', element: withSuspense(WarehouseView, { tab: 'env' }) },

            // ========== 数据统计 ==========
            { path: 'stats', element: withSuspense(StatsView) },
            { path: 'stats/:drillDown', element: withSuspense(StatsView) },

            // ========== 系统设置（Tab 子路由）==========
            {
                path: 'settings',
                element: withSuspense(SettingsLayout),
                children: [
                    { index: true, element: <Navigate to="basic" replace /> },
                    { path: 'basic', element: withSuspense(BasicSettings) },
                    { path: 'users', element: withSuspense(UserSettings) },
                    { path: 'roles', element: withSuspense(RoleSettings) },
                    { path: 'org', element: withSuspense(OrgSettings) },
                    { path: 'fonds', element: withSuspense(FondsManagement) },
                    { path: 'security', element: withSuspense(SecuritySettings) },
                    { path: 'audit', element: withSuspense(AuditLogView) },
                ],
            },

            // ========== 后台管理 ==========
            { path: 'admin/*', element: withSuspense(AdminLayout) },
        ],
    },

    // 404 兜底
    { path: '*', element: <Navigate to="/" replace /> },
];

// 从 paths.ts 重新导出路由常量（向后兼容）
export { ROUTE_PATHS, SUBITEM_TO_PATH } from './paths';
