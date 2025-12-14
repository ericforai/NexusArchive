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

// 加载占位符
const LoadingSpinner = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

export const SystemLayout: React.FC = () => {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const navigate = useNavigate();

    const handleLogout = async () => {
        await authApi.logout();
        safeStorage.removeItem('token');
        safeStorage.removeItem('user');
        navigate('/system/login');
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
                    <Suspense fallback={<LoadingSpinner />}>
                        <Outlet />
                    </Suspense>
                </main>
            </div>
        </div>
    );
};

export default SystemLayout;
