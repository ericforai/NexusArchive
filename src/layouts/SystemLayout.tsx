// Input: React、路由 Outlet、布局组件与 authApi
// Output: SystemLayout 布局组件
// Pos: 系统级页面框架
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 系统布局组件
 * 
 * 作为所有业务页面的父容器，包含 Sidebar 和 TopBar
 */
import React, { useState, Suspense } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { TopBar } from '../components/TopBar';
import { authApi } from '../api/auth';
import { safeStorage } from '../utils/storage';
import { Toaster } from 'react-hot-toast';
import { useFondsStore } from '../store/useFondsStore';

// 加载占位符
const LoadingSpinner = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

export const SystemLayout: React.FC = () => {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const navigate = useNavigate();
    const hasHydrated = useFondsStore(state => state._hasHydrated);

    const handleLogout = () => {
        // Fire and forget logout request - don't let network hang stop the user
        authApi.logout().catch(e => console.warn('Logout signal failed', e));

        // Immediate local cleanup
        safeStorage.clear();
        // Force hard reload to login page
        window.location.href = '/system/login';
    };

    const handleVisitLanding = () => {
        navigate('/');
    };

    const handleNavigateToPanorama = (item: { id: string }) => {
        navigate(`/system/panorama/${item.id}`);
    };

    return (
        <div className="flex h-screen bg-slate-50 font-sans text-slate-900">
            <Toaster position="top-center" reverseOrder={false} />
            <Sidebar
                collapsed={sidebarCollapsed}
                onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
                onVisitLanding={handleVisitLanding}
            />

            <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
                <TopBar
                    onLogout={handleLogout}
                    onNavigate={handleNavigateToPanorama}
                />
                <main className="flex-1 overflow-y-auto scroll-smooth p-0">
                    {!hasHydrated ? (
                        <LoadingSpinner />
                    ) : (
                        <Suspense fallback={<LoadingSpinner />}>
                            <Outlet />
                        </Suspense>
                    )}
                </main>
            </div>
        </div>
    );
};

export default SystemLayout;
