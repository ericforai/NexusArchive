import React, { useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { Dashboard } from './components/Dashboard';
import { LoginView } from './components/LoginView';
import { AdminLayout } from './components/admin/AdminLayout';
import { ArchiveListView } from './components/ArchiveListView';
import { StatsView } from './components/StatsView';
import { RelationshipQueryView } from './components/RelationshipQueryView';
import { OCRProcessingView } from './components/OCRProcessingView';
import { WarehouseView } from './components/WarehouseView';
import { DestructionView } from './components/DestructionView';
import { ArchiveApprovalView } from './components/ArchiveApprovalView';
import { RelationshipView } from './components/RelationshipView';
import { OpenAppraisalView } from './components/OpenAppraisalView';
import { OpenInventoryView } from './components/OpenInventoryView';
import { DestructionRepositoryView } from './components/DestructionRepositoryView';
import { BorrowingView } from './components/BorrowingView';
import { ArchivalPanoramaView } from './components/ArchivalPanoramaView';
import { OnlineReceptionView } from './components/OnlineReceptionView';
import AbnormalDataView from './components/AbnormalDataView';
import VolumeManagement from './components/VolumeManagement';
import { ComplianceReportView } from './components/ComplianceReportView';
import { ViewState, ModuleConfig } from './types';
import { useNavigate } from 'react-router-dom';
import { authApi } from './api/auth';
import {
    PRE_ARCHIVE_POOL_CONFIG,
    PRE_ARCHIVE_LINK_CONFIG,
    COLLECTION_ONLINE_CONFIG,
    COLLECTION_SCAN_CONFIG,
    COLLECTION_CONFIG,
    ARCHIVE_VIEW_CONFIG,
    ARCHIVE_BOX_CONFIG,
    ACCOUNTING_VOUCHER_CONFIG,
    ACCOUNTING_LEDGER_CONFIG,
    FINANCIAL_REPORT_CONFIG,
    OTHER_ACCOUNTING_MATERIALS_CONFIG,
    BORROWING_CONFIG,
    QUERY_CONFIG,
    GENERIC_CONFIG,
    WAREHOUSE_RACK_CONFIG,
    WAREHOUSE_ENV_CONFIG
} from './constants';
import { triggerAuditRefresh } from './utils/audit';

// 使用 Zustand Store
import { useAuthStore, useAppStore } from './store';

export const SystemApp: React.FC = () => {
    const navigate = useNavigate();

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
        setActiveSubItem,
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

    const getModuleConfig = (): ModuleConfig => {
        // Pre-Archive
        if (activeView === ViewState.PRE_ARCHIVE) {
            if (activeSubItem === '凭证关联') return PRE_ARCHIVE_LINK_CONFIG;
            return PRE_ARCHIVE_POOL_CONFIG;
        }

        // Collection
        if (activeView === ViewState.COLLECTION) {
            if (activeSubItem === '在线接收') return COLLECTION_ONLINE_CONFIG;
            if (activeSubItem === '扫描集成') return COLLECTION_SCAN_CONFIG;
            return COLLECTION_CONFIG;
        }

        // Archive Management
        if (activeView === ViewState.ARCHIVE_MGMT) {
            if (activeSubItem === '档案装盒') return ARCHIVE_BOX_CONFIG;
            if (activeSubItem === '会计凭证') return ACCOUNTING_VOUCHER_CONFIG;
            if (activeSubItem === '会计账簿') return ACCOUNTING_LEDGER_CONFIG;
            if (activeSubItem === '财务报告') return FINANCIAL_REPORT_CONFIG;
            if (activeSubItem === '其他会计资料') return OTHER_ACCOUNTING_MATERIALS_CONFIG;
            return ARCHIVE_VIEW_CONFIG;
        }

        // Query
        if (activeView === ViewState.QUERY) {
            return QUERY_CONFIG;
        }

        // Borrowing
        if (activeView === ViewState.BORROWING) {
            return BORROWING_CONFIG;
        }

        // Warehouse fallback
        if (activeView === ViewState.WAREHOUSE) {
            if (activeSubItem === '温湿度监控') return WAREHOUSE_ENV_CONFIG;
            return WAREHOUSE_RACK_CONFIG;
        }

        return GENERIC_CONFIG;
    };

    const renderContent = () => {
        switch (activeView) {
            case ViewState.PORTAL:
                return <Dashboard onNavigate={handleNavigate} />;

            case ViewState.PANORAMA:
                return <ArchivalPanoramaView initialVoucherId={activeResourceId} />;

            case ViewState.PRE_ARCHIVE:
                if (activeSubItem === 'OCR识别') return <OCRProcessingView />;
                if (activeSubItem === '异常数据') return <AbnormalDataView />;
                return <ArchiveListView title="预归档库" subTitle={activeSubItem || '电子凭证池'} config={getModuleConfig()} onNavigate={handleNavigate} />;

            case ViewState.COLLECTION:
                if (activeSubItem === '在线接收') return <OnlineReceptionView />;
                return <ArchiveListView title="资料收集" subTitle={activeSubItem || '概览'} config={getModuleConfig()} onNavigate={handleNavigate} />;

            case ViewState.ARCHIVE_MGMT:
                if (activeSubItem === '归档审批') return <ArchiveApprovalView />;
                if (activeSubItem === '关联查看') return <RelationshipView />;
                if (activeSubItem === '开放鉴定') return <OpenAppraisalView />;
                if (activeSubItem === '开放清册') return <OpenInventoryView />;
                if (activeSubItem === '销毁鉴定') return <DestructionView />;
                if (activeSubItem === '销毁库') return <DestructionRepositoryView />;
                if (activeSubItem === '档案组卷') return <VolumeManagement />;
                return <ArchiveListView title="档案管理" subTitle={activeSubItem || '归档查看'} config={getModuleConfig()} onNavigate={handleNavigate} />;

            case ViewState.QUERY:
                if (activeSubItem === '穿透联查') return <RelationshipQueryView />;
                return <ArchiveListView title="档案查询" subTitle={activeSubItem || '全文检索'} config={getModuleConfig()} onNavigate={handleNavigate} />;

            case ViewState.BORROWING:
                return <BorrowingView />;

            case ViewState.WAREHOUSE:
                return <WarehouseView />;

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
