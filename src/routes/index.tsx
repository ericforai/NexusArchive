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
import { RouteErrorBoundary } from '../components/common/RouteErrorBoundary';
import { Warehouse } from 'lucide-react';


// 布局组件（非懒加载，因为是框架级别）
import { SystemLayout } from '../layouts/SystemLayout';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { ActivationPage } from '../pages/ActivationPage';

// ProductWebsite 使用懒加载避免 React 实例问题
const ProductWebsite = lazy(() => import('../pages/product-website'));

// 页面容器（Page 层）- 封装懒加载和业务组件
import LoginPage from '../pages/Auth/Login';
const SsoLaunchPage = lazy(() => import('../pages/Auth/SsoLaunchPage'));
import { ArchiveListPage } from '../pages/archives/ArchiveListPage';
import { VoucherMatchingPage } from '../pages/matching/VoucherMatchingPage';
import { PoolPage } from '@/pages/pre-archive/PoolPage';
import { Dashboard } from '../pages/portal/Dashboard';

// 懒加载各功能模块（Page 层）
const ArchivalPanoramaView = lazy(() => import('../pages/panorama/ArchivalPanoramaView'));
const OCRProcessingView = lazy(() => import('../pages/pre-archive/OCRProcessingView'));
const AbnormalDataView = lazy(() => import('../pages/pre-archive/AbnormalDataView'));
const OriginalVoucherListView = lazy(() => import('../pages/archives/OriginalVoucherListView'));
const OnlineReceptionView = lazy(() => import('../pages/collection/OnlineReceptionView'));
const BatchUploadView = lazy(() => import('../pages/collection/BatchUploadView'));
const VolumeManagement = lazy(() => import('../pages/operations/VolumeManagement'));
const ArchiveApprovalView = lazy(() => import('../pages/operations/ArchiveApprovalView'));
const OpenAppraisalView = lazy(() => import('../pages/operations/OpenAppraisalView'));
const DestructionView = lazy(() => import('../pages/operations/DestructionView'));
const RelationshipQueryView = lazy(() => import('../pages/utilization/RelationshipQueryView'));
const BorrowingView = lazy(() => import('../pages/utilization/BorrowingView'));
const StatsView = lazy(() => import('../pages/stats/StatsView'));
const ComplianceReportView = lazy(() => import('../pages/archives/ComplianceReportView'));
const ArchiveDetailPage = lazy(() => import('../pages/archives/ArchiveDetailPage'));
const OtherAccountingMaterialsView = lazy(() => import('../pages/pre-archive/OtherAccountingMaterialsView'));
const LedgersPreArchiveView = lazy(() => import('../pages/pre-archive/LedgersPreArchiveView'));
const ReportsPreArchiveView = lazy(() => import('../pages/pre-archive/ReportsPreArchiveView'));
const AdminLayout = lazy(() => import('../pages/admin/AdminLayout'));

// 系统设置模块（4 个整合版布局）
const BasicSettingsLayout = lazy(() => import('../pages/settings/BasicSettingsLayout'));
const OrgSettingsLayout = lazy(() => import('../pages/settings/OrgSettingsLayout'));
const UserSettingsLayout = lazy(() => import('../pages/settings/UserSettingsLayout'));
const OpsSettingsLayout = lazy(() => import('../pages/settings/OpsSettingsLayout'));

// 系统设置子页面组件
const BasicSettings = lazy(() => import('../pages/settings/BasicSettingsPage'));
const UserSettings = lazy(() => import('../pages/settings/UserSettingsPage'));
const RoleSettings = lazy(() => import('../pages/settings/RoleSettingsPage'));
const SecuritySettings = lazy(() => import('../pages/settings/SecuritySettingsPage'));
const AuditLogView = lazy(() => import('../pages/settings/AuditLogView'));
const IntegrationSettings = lazy(() => import('../pages/settings/IntegrationSettingsPage'));
const DataImportPage = lazy(() => import('../pages/settings/DataImportPage'));

const PaymentFileTestView = lazy(() => import('../pages/debug/PaymentFileTestView'));
const PreviewWatermarkTestView = lazy(() => import('../pages/debug/PreviewWatermarkTestView'));

// 凭证预览组件 Demo
const VoucherPreviewDemo = lazy(() => import('../pages/demo/VoucherPreviewDemo'));

// [已移除] PoolKanbanPage - 看板功能已集成到 PoolPage 组件中

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

// 全宗管理模块
const FondsManagement = lazy(() => import('../pages/admin/FondsManagement'));

// 岗位管理模块
const PositionManagement = lazy(() => import('../pages/admin/PositionManagement').then(m => ({ default: m.PositionManagement })));

// [预留] 历史数据导入模块 - 路由待集成
// const LegacyImportPage = lazy(() => import('../pages/admin/LegacyImportPage'));

// [预留] 用户生命周期管理模块 - 路由待集成
// const UserLifecyclePage = lazy(() => import('../pages/admin/UserLifecyclePage'));
// const AccessReviewPage = lazy(() => import('../pages/admin/AccessReviewPage'));

// MFA设置模块
const MfaSettingsPage = lazy(() => import('../pages/settings/MfaSettingsPage'));

// ERP AI 适配器预览模块
const ErpPreviewPage = lazy(() => import('../pages/settings/ErpPreviewPage'));

// 冻结/保全管理模块
const FreezeHoldPage = lazy(() => import('../pages/operations/FreezeHoldPage'));
const FreezeHoldDetailPage = lazy(() => import('../pages/operations/FreezeHoldDetailPage'));

// 扫描模块
const MobileUploadPage = lazy(() => import('../pages/scan/MobileUploadPage').then(m => ({ default: m.MobileUploadPage })));

// 库房管理模块
const WarehouseView = lazy(() => import('../pages/utilization/WarehouseView'));

// 跨全宗访问授权票据模块
const AuthTicketApplyPage = lazy(() => import('../pages/security/AuthTicketApplyPage'));
const AuthTicketListPage = lazy(() => import('../pages/security/AuthTicketListPage'));

// [预留] 审计验真模块 - 路由待集成
// const AuditVerificationPage = lazy(() => import('../pages/audit/AuditVerificationPage'));
// const AuditEvidencePackagePage = lazy(() => import('../pages/audit/AuditEvidencePackagePage'));

// 代码质量监控模块
const QualityView = lazy(() => import('../pages/quality/QualityView').then(m => ({ default: m.QualityView })));

// 档案销毁流程模块
const ExpiredArchivesPage = lazy(() => import('../pages/operations/ExpiredArchivesPage'));
const AppraisalListPage = lazy(() => import('../pages/operations/AppraisalListPage'));
const DestructionApprovalPage = lazy(() => import('../pages/operations/DestructionApprovalPage'));
const DestructionExecutionPage = lazy(() => import('../pages/operations/DestructionExecutionPage'));

/**
 * 加载占位符
 *
 * 注意：常用页面（Dashboard, PoolPage, ArchiveListPage）已改为直接导入，无需 LoadingFallback
 * 大型页面（全景视图、统计图表、报表）仍使用懒加载，需要此占位符
 */
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
            <Component {...(props as any)} />
        </Suspense>
    );
}

/**
 * 路由配置
 */
export const routes: RouteObject[] = [
    // 根路径显示产品首页（公开访问）
    { path: '/', element: withSuspense(ProductWebsite) },

    // 登录页（独立于 SystemLayout，使用 Page 层）
    { path: '/system/login', element: <LoginPage /> },
    { path: '/system/sso/launch', element: withSuspense(SsoLaunchPage) },

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
        errorElement: <RouteErrorBoundary />,
        children: [
            // 门户首页
            { index: true, element: <Dashboard /> },

            // 全景视图（支持可选参数）
            { path: 'panorama/:id?', element: withSuspense(ArchivalPanoramaView) },

            // ========== 预归档库 ==========
            // PoolPage 作为容器，支持列表/看板双视图
            { path: 'pre-archive', element: <PoolPage /> },
            { path: 'pre-archive/pool', element: <PoolPage /> },
            { path: 'pre-archive/pool/kanban', element: <PoolPage /> },  // 兼容旧路由
            { path: 'pre-archive/doc-pool', element: withSuspense(OriginalVoucherListView, { title: '单据池', subTitle: '原始单据管理', poolMode: true }) },
            { path: 'pre-archive/ledgers', element: withSuspense(LedgersPreArchiveView) },
            { path: 'pre-archive/reports', element: withSuspense(ReportsPreArchiveView) },
            { path: 'pre-archive/other', element: withSuspense(OtherAccountingMaterialsView) },
            { path: 'pre-archive/ocr', element: withSuspense(OCRProcessingView) },
            { path: 'pre-archive/link', element: <ArchiveListPage routeConfig="link" /> },
            { path: 'pre-archive/abnormal', element: withSuspense(AbnormalDataView) },

            // ========== 资料收集 ==========
            { path: 'collection', element: <ArchiveListPage routeConfig="collection" /> },
            { path: 'collection/online', element: withSuspense(OnlineReceptionView) },
            { path: 'collection/scan', element: withSuspense(OCRProcessingView) },
            { path: 'collection/upload', element: withSuspense(BatchUploadView) },
            { path: 'scan/mobile/:sessionId', element: withSuspense(MobileUploadPage) },

            // ========== 档案管理 (Repository) ==========
            { path: 'archive', element: <ArchiveListPage routeConfig="view" /> },
            { path: 'archive/vouchers', element: <ArchiveListPage routeConfig="voucher" /> },
            { path: 'archive/accounting-vouchers', element: <ArchiveListPage routeConfig="voucher" /> },
            { path: 'archive/ledgers', element: <ArchiveListPage routeConfig="ledger" /> },
            { path: 'archive/original-vouchers', element: withSuspense(OriginalVoucherListView) },
            { path: 'archive/reports', element: <ArchiveListPage routeConfig="report" /> },
            { path: 'archive/other', element: <ArchiveListPage routeConfig="other" /> },
            { path: 'archive/compliance/:id', element: withSuspense(ComplianceReportView) },
            { path: 'archives/:id', element: withSuspense(ArchiveDetailPage) },

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

            // ========== 库房管理 ==========
            { path: 'warehouse', element: withSuspense(WarehouseView) },

            // ========== 数据统计 ==========
            { path: 'stats', element: withSuspense(StatsView) },
            { path: 'stats/:drillDown', element: withSuspense(StatsView) },

            // ========== 代码质量监控 ==========
            { path: 'quality', element: withSuspense(QualityView) },

            // ========== 系统设置（4 个整合版布局）==========
            { path: 'settings', element: <Navigate to="/system/settings/basic" replace /> },

            // 基础配置（基础设置 + 安全合规）
            {
                path: 'settings/basic',
                element: withSuspense(BasicSettingsLayout),
                children: [
                    { index: true, element: withSuspense(BasicSettings) },
                    { path: 'security', element: withSuspense(SecuritySettings) },
                ],
            },

            // 组织管理（法人、架构、全宗、岗位、沿革）
            {
                path: 'settings/org',
                element: withSuspense(OrgSettingsLayout),
                children: [
                    { index: true, element: withSuspense(EntityManagementPage) },
                    { path: 'entity', element: withSuspense(EntityManagementPage) },
                    { path: 'architecture', element: withSuspense(EnterpriseArchitecturePage) },
                    { path: 'fonds', element: withSuspense(FondsManagement) },
                    { path: 'positions', element: withSuspense(PositionManagement) },
                    { path: 'fonds-history', element: withSuspense(FondsHistoryPage) },
                ],
            },

            // 用户权限（用户管理 + 角色权限）
            {
                path: 'settings/users',
                element: withSuspense(UserSettingsLayout),
                children: [
                    { index: true, element: withSuspense(UserSettings) },
                    { path: 'roles', element: withSuspense(RoleSettings) },
                ],
            },
            { path: 'settings/roles', element: <Navigate to="/system/settings/users/roles" replace /> },

            // 系统运维（集成中心 + 审计日志 + 数据导入 + 安全合规）
            {
                path: 'settings/integration',
                element: withSuspense(OpsSettingsLayout),
                children: [
                    { index: true, element: withSuspense(IntegrationSettings) },
                    { path: 'audit', element: withSuspense(AuditLogView) },
                    { path: 'data-import', element: withSuspense(DataImportPage) },
                    { path: 'security', element: withSuspense(SecuritySettings) },
                ],
            },
            { path: 'settings/audit', element: <Navigate to="/system/settings/integration/audit" replace /> },
            { path: 'settings/data-import', element: <Navigate to="/system/settings/integration/data-import" replace /> },
            { path: 'settings/security', element: <Navigate to="/system/settings/basic/security" replace /> },
            { path: 'settings/mfa', element: withSuspense(MfaSettingsPage) },

            // ========== ERP AI 适配器预览 ==========
            { path: 'settings/erp-ai/preview', element: withSuspense(ErpPreviewPage) },

            // ========== 法人管理 ==========
            { path: 'admin/entity', element: withSuspense(EntityManagementPage) },
            { path: 'admin/entity/config', element: withSuspense(EntityConfigPage) },
            { path: 'admin/enterprise-architecture', element: withSuspense(EnterpriseArchitecturePage) },

            // ========== 全宗沿革管理 ==========
            { path: 'admin/fonds-history', element: withSuspense(FondsHistoryPage) },
            { path: 'admin/fonds-history/list', element: withSuspense(FondsHistoryListPage) },

            // ========== 跨全宗访问授权票据 ==========
            { path: 'security/auth-ticket/apply', element: withSuspense(AuthTicketApplyPage) },
            { path: 'security/auth-ticket', element: withSuspense(AuthTicketListPage) },
            { path: 'security/auth-ticket/list', element: withSuspense(AuthTicketListPage) },

            // ========== 冻结/保全管理 ==========
            { path: 'operations/freeze-hold', element: withSuspense(FreezeHoldPage) },
            { path: 'operations/freeze-hold/:id', element: withSuspense(FreezeHoldDetailPage) },

            // ========== 后台管理 ==========
            { path: 'admin/*', element: withSuspense(AdminLayout) },

            // ========== Debug ==========
            { path: 'debug/payment-file', element: withSuspense(PaymentFileTestView) },
            { path: 'debug/preview-watermark', element: withSuspense(PreviewWatermarkTestView) },
            { path: 'debug/voucher-preview', element: withSuspense(VoucherPreviewDemo) },

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
