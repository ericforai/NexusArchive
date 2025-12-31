// Input: React Router、布局组件与懒加载页面
// Output: routes 路由配置与路径导出
// Pos: 前端路由配置中心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 路由配置中心
 * 
 * 企业级路由架构，采用 React Router v7 嵌套路由 + 懒加载
 */
import React, { lazy, Suspense } from 'react';
import { Navigate, RouteObject } from 'react-router-dom';

import { ErrorBoundary } from '../components/common/ErrorBoundary';

// 布局组件（非懒加载，因为是框架级别）
import { SystemLayout } from '../layouts/SystemLayout';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { ProductWebsite } from '../pages/ProductWebsite';
import { ActivationPage } from '../pages/ActivationPage';

// 页面容器（Page 层）- 封装懒加载和业务组件
import LoginPage from '../pages/Auth/Login';
import { ArchiveListPage } from '../pages/archives/ArchiveListPage';
import { VoucherMatchingPage } from '../pages/matching/VoucherMatchingPage';

// 懒加载各功能模块（Page 层）
const Dashboard = lazy(() => import('../pages/portal/Dashboard'));
const ArchivalPanoramaView = lazy(() => import('../pages/panorama/ArchivalPanoramaView'));
const OCRProcessingView = lazy(() => import('../pages/pre-archive/OCRProcessingView'));
const AbnormalDataView = lazy(() => import('../pages/pre-archive/AbnormalDataView'));
const OriginalVoucherListView = lazy(() => import('../pages/archives/OriginalVoucherListView'));
const OnlineReceptionView = lazy(() => import('../pages/collection/OnlineReceptionView'));
const VolumeManagement = lazy(() => import('../pages/operations/VolumeManagement'));
const ArchiveApprovalView = lazy(() => import('../pages/operations/ArchiveApprovalView'));
const OpenAppraisalView = lazy(() => import('../pages/operations/OpenAppraisalView'));
const DestructionView = lazy(() => import('../pages/operations/DestructionView'));
const RelationshipQueryView = lazy(() => import('../pages/utilization/RelationshipQueryView'));
const BorrowingView = lazy(() => import('../pages/utilization/BorrowingView'));
const StatsView = lazy(() => import('../pages/stats/StatsView'));
const ComplianceReportView = lazy(() => import('../pages/archives/ComplianceReportView'));
const AdminLayout = lazy(() => import('../pages/admin/AdminLayout'));

// 系统设置模块（Tab 拆分子路由）
const SettingsLayout = lazy(() => import('../pages/settings/SettingsLayoutPage'));
const BasicSettings = lazy(() => import('../pages/settings/BasicSettingsPage'));
const UserSettings = lazy(() => import('../pages/settings/UserSettingsPage'));
const RoleSettings = lazy(() => import('../pages/settings/RoleSettingsPage'));
const OrgSettings = lazy(() => import('../pages/settings/OrgSettingsPage'));
const SecuritySettings = lazy(() => import('../pages/settings/SecuritySettingsPage'));
const AuditLogView = lazy(() => import('../pages/settings/AuditLogView'));
const IntegrationSettings = lazy(() => import('../pages/settings/IntegrationSettingsPage'));

const PaymentFileTestView = lazy(() => import('../pages/debug/PaymentFileTestView'));
const PreviewWatermarkTestView = lazy(() => import('../pages/debug/PreviewWatermarkTestView'));

// 匹配向导模块（OnboardingWizard 和 ComplianceReport 仍用 withSuspense）
const OnboardingWizard = lazy(() => import('../pages/matching/OnboardingWizard'));
const ComplianceReport = lazy(() => import('../pages/matching/ComplianceReport'));

// 归档批次模块
const ArchiveBatchView = lazy(() => import('../pages/operations/ArchiveBatchView'));

// 全宗沿革管理模块
const FondsHistoryPage = lazy(() => import('../pages/admin/FondsHistoryPage'));
const FondsHistoryListPage = lazy(() => import('../pages/admin/FondsHistoryListPage'));

// 法人管理模块
const EntityManagementPage = lazy(() => import('../pages/admin/EntityManagementPage'));
const EntityConfigPage = lazy(() => import('../pages/admin/EntityConfigPage'));
const EnterpriseArchitecturePage = lazy(() => import('../pages/admin/EnterpriseArchitecturePage'));

// 历史数据导入模块
const LegacyImportPage = lazy(() => import('../pages/admin/LegacyImportPage'));

// 用户生命周期管理模块
const UserLifecyclePage = lazy(() => import('../pages/admin/UserLifecyclePage'));
const AccessReviewPage = lazy(() => import('../pages/admin/AccessReviewPage'));

// MFA设置模块
const MfaSettingsPage = lazy(() => import('../pages/settings/MfaSettingsPage'));

// 冻结/保全管理模块
const FreezeHoldPage = lazy(() => import('../pages/operations/FreezeHoldPage'));
const FreezeHoldDetailPage = lazy(() => import('../pages/operations/FreezeHoldDetailPage'));

// 跨全宗访问授权票据模块
const AuthTicketApplyPage = lazy(() => import('../pages/security/AuthTicketApplyPage'));
const AuthTicketListPage = lazy(() => import('../pages/security/AuthTicketListPage'));

// 审计验真模块
const AuditVerificationPage = lazy(() => import('../pages/audit/AuditVerificationPage'));
const AuditEvidencePackagePage = lazy(() => import('../pages/audit/AuditEvidencePackagePage'));

// 档案销毁流程模块
const ExpiredArchivesPage = lazy(() => import('../pages/operations/ExpiredArchivesPage'));
const AppraisalListPage = lazy(() => import('../pages/operations/AppraisalListPage'));
const DestructionApprovalPage = lazy(() => import('../pages/operations/DestructionApprovalPage'));
const DestructionExecutionPage = lazy(() => import('../pages/operations/DestructionExecutionPage'));

// 加载占位符
const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

// 包装懒加载组件（泛型版本，提供类型安全）
function withSuspense<P extends object>(
    Component: React.LazyExoticComponent<React.ComponentType<P>>,
    props?: P
): React.ReactElement {
    return (
        <Suspense fallback={<LoadingFallback />}>
            <Component {...(props as P)} />
        </Suspense>
    );
}

/**
 * 路由配置
 */
export const routes: RouteObject[] = [
    // 根路径显示产品首页（公开访问）
    { path: '/', element: <ProductWebsite /> },

    // 登录页（独立于 SystemLayout，使用 Page 层）
    { path: '/system/login', element: <LoginPage /> },

    // 激活页（独立于 SystemLayout，但需要基础环境）
    { path: '/system/activation', element: <ActivationPage /> },


    // 业务系统（需登录）
    {
        path: '/system',
        element: (
            <ProtectedRoute>
                <ErrorBoundary>
                    <SystemLayout />
                </ErrorBoundary>
            </ProtectedRoute>
        ),
        children: [
            // 门户首页
            { index: true, element: withSuspense(Dashboard) },

            // 全景视图（支持可选参数）
            { path: 'panorama/:id?', element: withSuspense(ArchivalPanoramaView) },

            // ========== 预归档库 ==========
            { path: 'pre-archive', element: <ArchiveListPage routeConfig="pool" /> },
            { path: 'pre-archive/pool', element: <ArchiveListPage routeConfig="pool" /> },
            { path: 'pre-archive/doc-pool', element: withSuspense(OriginalVoucherListView, { title: '单据池', subTitle: '原始单据管理', poolMode: true }) },
            { path: 'pre-archive/ocr', element: withSuspense(OCRProcessingView) },
            { path: 'pre-archive/link', element: <ArchiveListPage routeConfig="link" /> },
            { path: 'pre-archive/abnormal', element: withSuspense(AbnormalDataView) },

            // ========== 资料收集 ==========
            { path: 'collection', element: <ArchiveListPage routeConfig="collection" /> },
            { path: 'collection/online', element: withSuspense(OnlineReceptionView) },
            { path: 'collection/scan', element: <ArchiveListPage routeConfig="scan" /> },
            { path: 'collection/upload', element: <ArchiveListPage routeConfig="collection" /> },

            // ========== 档案管理 (Repository) ==========
            { path: 'archive', element: <ArchiveListPage routeConfig="view" /> },
            { path: 'archive/vouchers', element: <ArchiveListPage routeConfig="voucher" /> },
            { path: 'archive/ledgers', element: <ArchiveListPage routeConfig="ledger" /> },
            { path: 'archive/original-vouchers', element: withSuspense(OriginalVoucherListView) },
            { path: 'archive/reports', element: <ArchiveListPage routeConfig="report" /> },
            { path: 'archive/other', element: <ArchiveListPage routeConfig="other" /> },
            { path: 'archive/compliance/:id', element: withSuspense(ComplianceReportView) },

            // ========== 档案作业 (Operations) ==========
            { path: 'operations', element: <ArchiveListPage routeConfig="view" /> }, // Fallback/Default
            { path: 'operations/boxing', element: <ArchiveListPage routeConfig="box" /> },
            { path: 'operations/volume', element: withSuspense(VolumeManagement) },
            { path: 'operations/approval', element: withSuspense(ArchiveApprovalView) },
            { path: 'operations/batch', element: withSuspense(ArchiveBatchView) },  // 归档批次
            { path: 'operations/open-appraisal', element: withSuspense(OpenAppraisalView) },
            { path: 'operations/destruction', element: withSuspense(DestructionView) },
            { path: 'operations/expired-archives', element: withSuspense(ExpiredArchivesPage) },
            { path: 'operations/appraisal-list', element: withSuspense(AppraisalListPage) },
            { path: 'operations/destruction-approval', element: withSuspense(DestructionApprovalPage) },
            { path: 'operations/destruction-execution', element: withSuspense(DestructionExecutionPage) },

            // ========== 档案利用 (Utilization) ==========
            { path: 'utilization', element: <ArchiveListPage routeConfig="query" /> }, // Fallback
            { path: 'utilization/query', element: <ArchiveListPage routeConfig="query" /> },
            { path: 'utilization/relationship', element: withSuspense(RelationshipQueryView) },
            { path: 'utilization/borrowing', element: withSuspense(BorrowingView) },

            // ========== 档案销毁 ==========
            { path: 'destruction', element: withSuspense(DestructionView) },

            // ========== 库房管理 - Removed ==========

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
                    { path: 'security', element: withSuspense(SecuritySettings) },
                    { path: 'integration', element: withSuspense(IntegrationSettings) },
                    { path: 'audit', element: withSuspense(AuditLogView) },
                    { path: 'mfa', element: withSuspense(MfaSettingsPage) },
                ],
            },

            // ========== 法人管理 ==========
            { path: 'admin/entity', element: withSuspense(EntityManagementPage) },
            { path: 'admin/entity/config', element: withSuspense(EntityConfigPage) },
            { path: 'admin/enterprise-architecture', element: withSuspense(EnterpriseArchitecturePage) },
            
            // ========== 全宗沿革管理 ==========
            { path: 'admin/fonds-history', element: withSuspense(FondsHistoryPage) },
            { path: 'admin/fonds-history/list', element: withSuspense(FondsHistoryListPage) },
            
            // ========== 历史数据导入 ==========
            { path: 'admin/legacy-import', element: withSuspense(LegacyImportPage) },
            
            // ========== 跨全宗访问授权票据 ==========
            { path: 'security/auth-ticket/apply', element: withSuspense(AuthTicketApplyPage) },
            { path: 'security/auth-ticket', element: withSuspense(AuthTicketListPage) },
            { path: 'security/auth-ticket/list', element: withSuspense(AuthTicketListPage) },
            
            // ========== 用户生命周期管理 ==========
            { path: 'admin/user-lifecycle', element: withSuspense(UserLifecyclePage) },
            { path: 'admin/access-review', element: withSuspense(AccessReviewPage) },
            
            // ========== 冻结/保全管理 ==========
            { path: 'operations/freeze-hold', element: withSuspense(FreezeHoldPage) },
            { path: 'operations/freeze-hold/:id', element: withSuspense(FreezeHoldDetailPage) },
            
            // ========== 审计验真 ==========
            { path: 'audit/verification', element: withSuspense(AuditVerificationPage) },
            { path: 'audit/evidence-package', element: withSuspense(AuditEvidencePackagePage) },
            
            // ========== 后台管理 ==========
            { path: 'admin/*', element: withSuspense(AdminLayout) },

            // ========== Debug ==========
            { path: 'debug/payment-file', element: withSuspense(PaymentFileTestView) },
            { path: 'debug/preview-watermark', element: withSuspense(PreviewWatermarkTestView) },

            // ========== 匹配引擎 ==========
            { path: 'matching', element: <VoucherMatchingPage /> },
            { path: 'matching/auto', element: <VoucherMatchingPage /> },
            { path: 'matching/wizard', element: withSuspense(OnboardingWizard, { companyId: 1 }) },
            { path: 'matching/report', element: withSuspense(ComplianceReport) },
        ],
    },

    // 404 兜底
    { path: '*', element: <Navigate to="/" replace /> },
];

// 从 paths.ts 重新导出路由常量（向后兼容）
export { ROUTE_PATHS, SUBITEM_TO_PATH } from './paths';
