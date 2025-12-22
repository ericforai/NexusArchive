// Input: React、react-router-dom 路由
// Output: React 组件 SettingsLayout
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
    Settings,
    Users,
    Shield,
    Building2,
    FolderTree,
    Lock,
    FileSearch,
    Link
} from 'lucide-react';

const SETTINGS_TABS = [
    { key: 'basic', label: '基础设置', path: '/system/settings/basic', icon: Settings },
    { key: 'users', label: '用户管理', path: '/system/settings/users', icon: Users },
    { key: 'roles', label: '角色权限', path: '/system/settings/roles', icon: Shield },
    { key: 'org', label: '组织架构', path: '/system/settings/org', icon: Building2 },
    { key: 'fonds', label: '全宗管理', path: '/system/settings/fonds', icon: FolderTree },
    { key: 'security', label: '安全合规', path: '/system/settings/security', icon: Lock },
    { key: 'integration', label: '集成中心', path: '/system/settings/integration', icon: Link },
    { key: 'audit', label: '审计日志', path: '/system/settings/audit', icon: FileSearch },
];

/**
 * 系统设置布局容器
 * 
 * 提供 Tab 导航 + Outlet 渲染子路由
 */
export const SettingsLayout: React.FC = () => {
    const location = useLocation();

    return (
        <div className="min-h-full bg-slate-50">
            {/* 页面标题 */}
            <div className="bg-white border-b border-slate-200 px-8 py-6">
                <h1 className="text-2xl font-bold text-slate-800">系统设置</h1>
                <p className="text-slate-500 mt-1">配置全局参数、用户权限及安全策略</p>
            </div>

            {/* Tab 导航 */}
            <div className="bg-white border-b border-slate-200 px-8">
                <nav className="flex space-x-1 overflow-x-auto" aria-label="设置导航">
                    {SETTINGS_TABS.map((tab) => {
                        const isActive = location.pathname === tab.path ||
                            (tab.key === 'basic' && location.pathname === '/system/settings');
                        const Icon = tab.icon;

                        return (
                            <NavLink
                                key={tab.key}
                                to={tab.path}
                                className={`
                  flex items-center px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap
                  transition-colors duration-200
                  ${isActive
                                        ? 'border-primary-500 text-primary-600'
                                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                                    }
                `}
                            >
                                <Icon size={16} className="mr-2" />
                                {tab.label}
                            </NavLink>
                        );
                    })}
                </nav>
            </div>

            {/* 子路由内容 */}
            <div className="p-8 max-w-5xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-300">
                <Outlet />
            </div>
        </div>
    );
};

export default SettingsLayout;
