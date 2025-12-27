// Input: React、路由导航、鉴权 API、Zustand 状态、界面组件与常量
// Output: SystemApp 业务主入口组件
// Pos: 认证后系统工作台入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { Dashboard } from './pages/portal/Dashboard';
import { LoginView } from './pages/Auth/LoginView';
import { AdminLayout } from './pages/admin/AdminLayout';
import { ArchiveListPage } from './pages/archives/ArchiveListPage';
import { StatsView } from './pages/stats/StatsView';
import { RelationshipQueryView } from './pages/utilization/RelationshipQueryView';
import { OCRProcessingView } from './pages/pre-archive/OCRProcessingView';
import { DestructionView } from './pages/operations/DestructionView';
import { ArchiveApprovalView } from './pages/operations/ArchiveApprovalView';
import { OpenAppraisalView } from './pages/operations/OpenAppraisalView';
import { BorrowingView } from './pages/utilization/BorrowingView';
import { ArchivalPanoramaView } from './pages/panorama/ArchivalPanoramaView';
import { OnlineReceptionView } from './pages/collection/OnlineReceptionView';
import AbnormalDataView from './pages/pre-archive/AbnormalDataView';
import { OriginalVoucherListView } from './pages/archives/OriginalVoucherListView';
import VolumeManagement from './pages/operations/VolumeManagement';
import { ComplianceReportView } from './pages/archives/ComplianceReportView';
import VoucherMatchingView from './pages/matching/VoucherMatchingView';
import { ViewState } from './types';
import type { ArchiveRouteMode } from './features/archives';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from './api/auth';
import { triggerAuditRefresh } from './utils/audit';

// 使用 Zustand Store
import { useAuthStore, useAppStore } from './store';

// URL 到 ViewState 的映射
const PATH_TO_VIEW: Record<string, ViewState> = {

    '/system/pre-archive': ViewState.PRE_ARCHIVE,
    '/system/collection': ViewState.COLLECTION,
    '/system/archive': ViewState.ACCOUNT_ARCHIVES,
    '/system/operations': ViewState.ARCHIVE_OPS,
    '/system/utilization': ViewState.ARCHIVE_UTILIZATION,
    '/system/stats': ViewState.STATS,
    '/system/settings': ViewState.SETTINGS,
    '/system/panorama': ViewState.PANORAMA,
};

// 子菜单路径到 subItem 的映射
const PATH_TO_SUBITEM: Record<string, string> = {};

export const SystemApp: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    // 从 AuthStore 获取认证状态
    const {
        isAuthenticated,
        isCheckingAuth,
        login,
        logout: authLogout,
        setCheckingAuth,
    } = useAuthStore();

    // 从 AppStore 获取应用状态
    const {
        activeView,
        activeSubItem,
        activeResourceId,
        sidebarCollapsed,
        navigate: appNavigate,
        setActiveView,
        toggleSidebar,
    } = useAppStore();

    // 初始化时验证 token
    useEffect(() => {
        const verifyAuth = async () => {
            const { token, user } = useAuthStore.getState();

            // 如果有 token 和 user 缓存，直接认证成功
            if (token && user) {
                console.log('[SystemApp] Token and user found, setting authenticated');
                setCheckingAuth(false);
                return;
            }

            if (!token) {
                console.log('[SystemApp] No token found');
                setCheckingAuth(false);
                return;
            }

            // 只有 token 但没有 user 时才验证（边界情况）
            try {
                console.log('[SystemApp] Verifying token...');
                const res = await authApi.getCurrentUser();
                if (res.code === 200 && res.data) {
                    login(token, {
                        id: res.data.id,
                        username: res.data.username,
                        realName: res.data.fullName,
                        roles: res.data.roles || [],
                        permissions: res.data.permissions || [],
                    });
                } else {
                    authLogout();
                }
            } catch (error) {
                console.error('[SystemApp] Token verification failed:', error);
                authLogout();
            } finally {
                setCheckingAuth(false);
            }
        };

        verifyAuth();
    }, [login, authLogout, setCheckingAuth]);

    // 监听 URL 变化，同步到 AppStore
    useEffect(() => {
        const path = location.pathname;

        // 精确匹配子菜单
        if (PATH_TO_SUBITEM[path]) {
            const subItem = PATH_TO_SUBITEM[path];
            // 找到父级 ViewState
            for (const [prefix, view] of Object.entries(PATH_TO_VIEW)) {
                if (path.startsWith(prefix)) {
                    if (activeView !== view || activeSubItem !== subItem) {
                        appNavigate(view, subItem, '');
                    }
                    return;
                }
            }
        }

        // 匹配顶级路由
        for (const [prefix, view] of Object.entries(PATH_TO_VIEW)) {
            if (path.startsWith(prefix)) {
                if (activeView !== view) {
                    appNavigate(view, '', '');
                }
                return;
            }
        }
    }, [location.pathname, activeView, activeSubItem, appNavigate]);

    const handleLoginSuccess = (user: any) => {
        console.log('[SystemApp] Login success, setting authenticated');
        const token = useAuthStore.getState().token;
        login(token || '', {
            id: user.id,
            username: user.username,
            realName: user.fullName || user.realName,
            roles: user.roles || [],
            permissions: user.permissions || [],
        });
        appNavigate(ViewState.PORTAL);
    };

    const handleNavigate = (view: ViewState, subItem: string = '', resourceId: string = '') => {
        appNavigate(view, subItem, resourceId);
    };

    const handleLogout = async () => {
        await authApi.logout();
        triggerAuditRefresh();
        authLogout();
        navigate('/system');
    };

    if (isCheckingAuth) {
        return (
            <div className="flex items-center justify-center h-screen text-slate-600">
                <span className="animate-pulse text-sm">正在校验登录状态...</span>
            </div>
        );
    }

    if (!isAuthenticated) {
        return (
            <LoginView
                onLoginSuccess={handleLoginSuccess}
                onVisitLanding={() => navigate('/')}
            />
        );
    }

    if (activeView === ViewState.ADMIN) {
        return <AdminLayout onExit={() => setActiveView(ViewState.PORTAL)} />;
    }

    const resolveArchiveRouteMode = (): ArchiveRouteMode => {
        if (activeView === ViewState.PRE_ARCHIVE) {
            return activeSubItem === '凭证关联' ? 'link' : 'pool';
        }
        if (activeView === ViewState.COLLECTION) {
            return activeSubItem === '扫描集成' ? 'scan' : 'collection';
        }
        if (activeView === ViewState.ACCOUNT_ARCHIVES) {
            if (activeSubItem === '会计凭证') return 'voucher';
            if (activeSubItem === '会计账簿') return 'ledger';
            if (activeSubItem === '财务报告') return 'report';
            if (activeSubItem === '其他会计资料') return 'other';
            return 'view';
        }
        if (activeView === ViewState.ARCHIVE_OPS) {
            return activeSubItem === '档案装盒' ? 'box' : 'view';
        }
        if (activeView === ViewState.ARCHIVE_UTILIZATION || activeView === ViewState.QUERY || activeView === ViewState.BORROWING) {
            return 'query';
        }
        return 'view';
    };

    const renderContent = () => {
        switch (activeView) {
            case ViewState.PORTAL:
                return <Dashboard onNavigate={handleNavigate} />;

            case ViewState.PANORAMA:
                return <ArchivalPanoramaView initialVoucherId={activeResourceId} />;

            case ViewState.PRE_ARCHIVE: {
                if (activeSubItem === 'OCR识别') return <OCRProcessingView />;
                if (activeSubItem === '异常数据') return <AbnormalDataView />;
                // 单据池及其子分类使用 OriginalVoucherListView (Pool Mode)
                // 与数据库 sys_original_voucher_type 保持同步
                const DOC_POOL_TYPES = [
                    '单据池',
                    // 发票类
                    '单据池:纸质发票', '单据池:增值税电子发票', '单据池:数电发票',
                    '单据池:数电票（铁路）', '单据池:数电票（航空）', '单据池:数电票（财政）',
                    // 银行类
                    '单据池:银行回单', '单据池:银行对账单',
                    // 单据类
                    '单据池:付款单', '单据池:收款单', '单据池:收款单据（收据）', '单据池:工资单',
                    // 合同类
                    '单据池:合同', '单据池:协议',
                    // 其他类
                    '单据池:其他'
                ];
                if (DOC_POOL_TYPES.includes(activeSubItem || '')) {
                    // 提取分类类型（如果有）
                    const voucherType = activeSubItem?.includes(':') ? activeSubItem.split(':')[1] : undefined;
                    return <OriginalVoucherListView
                        title="单据池"
                        subTitle={voucherType || '全部单据'}
                        poolMode={true}
                    />;
                }
                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;
            }

            case ViewState.COLLECTION:
                if (activeSubItem === '在线接收') return <OnlineReceptionView />;
                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;

            // --- Accounting Archives (Repository) ---
            case ViewState.ACCOUNT_ARCHIVES: {
                // 原始凭证及其子分类使用独立组件
                const ORIGINAL_VOUCHER_TYPES = [
                    '原始凭证',
                    '销售订单', '出库单', '采购订单', '入库单',
                    '付款申请单', '报销单',
                    '普通发票', '增值税专票',
                    '银行回单', '银行对账单',
                    '合同协议'
                ];
                if (ORIGINAL_VOUCHER_TYPES.includes(activeSubItem || '')) {
                    return <OriginalVoucherListView
                        title="原始凭证"
                        subTitle={activeSubItem === '原始凭证' ? '原始凭证管理' : activeSubItem}
                    />;
                }
                // 记账凭证和其他子菜单使用通用组件
                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;
            }

            // --- Archive Operations ---
            case ViewState.ARCHIVE_OPS:
                if (activeSubItem === '归档审批') return <ArchiveApprovalView />;
                if (activeSubItem === '档案组卷') return <VolumeManagement />;
                if (activeSubItem === '开放鉴定') return <OpenAppraisalView />;
                if (activeSubItem === '销毁鉴定') return <DestructionView />;
                if (activeSubItem === '档案装盒') return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;

                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;

            case ViewState.ARCHIVE_MGMT: // Backward compatibility fallback
                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;

            case ViewState.ARCHIVE_UTILIZATION:
                if (activeSubItem === '穿透联查') return <RelationshipQueryView />;
                if (activeSubItem === '借阅申请') return <BorrowingView />;
                return <ArchiveListPage routeConfig={resolveArchiveRouteMode()} />;

            // case ViewState.WAREHOUSE: return <WarehouseView />; // Removed

            case ViewState.DESTRUCTION:
                return <DestructionView />;

            case ViewState.STATS:
                return <StatsView drillDown={activeSubItem} onNavigate={handleNavigate} />;

            case ViewState.SETTINGS:
                // 使用新的路由架构，Settings 页面通过 routes/index.tsx 渲染
                navigate('/system/settings');
                return null;

            case ViewState.COMPLIANCE_REPORT:
                return (
                    <ComplianceReportView
                        archiveId={activeResourceId}
                        onBack={() => handleNavigate(ViewState.ARCHIVE_MGMT, activeSubItem)}
                    />
                );

            case ViewState.MATCHING:
                return <VoucherMatchingView voucherId={activeResourceId || ''} voucherNo={activeSubItem} />;



            default:
                return <Dashboard onNavigate={handleNavigate} />;
        }
    };

    return (
        <div className="flex h-screen bg-slate-50 font-sans text-slate-900">
            <Sidebar
                onVisitLanding={() => navigate('/')}
                collapsed={sidebarCollapsed}
                onToggle={toggleSidebar}
            />

            <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
                <TopBar
                    onLogout={handleLogout}
                    onNavigate={(item) => handleNavigate(ViewState.PANORAMA, '', item.id)}
                />
                <main className="flex-1 overflow-y-auto scroll-smooth p-0">
                    {renderContent()}
                </main>
            </div>
        </div>
    );
};
