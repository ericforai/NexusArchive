// Input: React、路由导航、鉴权 API、Zustand 状态、界面组件与常量
// Output: SystemApp 业务主入口组件
// Pos: 认证后系统工作台入口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { useNavigate } from 'react-router-dom';

// 组件导入
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { LoginView } from './pages/Auth/LoginView';
import { AdminLayout } from './pages/admin/AdminLayout';

// 状态管理
import { useAuthStore, useAppStore } from './store';

// 类型
import { ViewState } from './types';

// 视图常量、渲染器和 Hooks
import { renderContent, useAuthVerification, useUrlSync, useSystemHandlers } from './pages/system';

export const SystemApp: React.FC = () => {
    const navigate = useNavigate();

    // 使用自定义 Hooks
    useAuthVerification();
    useUrlSync();

    // 获取事件处理器
    const { handleLoginSuccess, handleNavigate, handleLogout, handleNavigateToPanorama } = useSystemHandlers();

    // 从 AuthStore 获取认证状态
    const { isAuthenticated, isCheckingAuth } = useAuthStore();

    // 加载中状态
    if (isCheckingAuth) {
        return (
            <div className="flex items-center justify-center h-screen text-slate-600">
                <span className="animate-pulse text-sm">正在校验登录状态...</span>
            </div>
        );
    }

    // 未登录状态
    if (!isAuthenticated) {
        return (
            <LoginView
                onLoginSuccess={handleLoginSuccess}
                onVisitLanding={() => navigate('/')}
            />
        );
    }

    // 已登录状态
    return (
        <AuthenticatedApp
            navigate={navigate}
            handleNavigate={handleNavigate}
            handleLogout={handleLogout}
            handleNavigateToPanorama={handleNavigateToPanorama}
        />
    );
};

/**
 * 已认证用户的主应用布局 - 提取以减少主函数行数
 */
const AuthenticatedApp: React.FC<{
    navigate: ReturnType<typeof useNavigate>;
    handleNavigate: ReturnType<typeof useSystemHandlers>['handleNavigate'];
    handleLogout: ReturnType<typeof useSystemHandlers>['handleLogout'];
    handleNavigateToPanorama: ReturnType<typeof useSystemHandlers>['handleNavigateToPanorama'];
}> = ({ navigate, handleNavigate, handleLogout, handleNavigateToPanorama }) => {
    const {
        activeView,
        activeSubItem,
        activeResourceId,
        sidebarCollapsed,
        setActiveView,
        toggleSidebar,
    } = useAppStore();

    // 管理员模式
    if (activeView === ViewState.ADMIN) {
        return <AdminLayout onExit={() => setActiveView(ViewState.PORTAL)} />;
    }

    // 渲染主内容
    const content = renderContent({
        activeView,
        activeSubItem,
        activeResourceId,
        onNavigate: handleNavigate,
        navigateSettings: () => navigate('/system/settings'),
    });

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
                    onNavigate={handleNavigateToPanorama}
                />
                <main className="flex-1 overflow-y-auto scroll-smooth p-0">
                    {content}
                </main>
            </div>
        </div>
    );
};
