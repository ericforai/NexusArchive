// Input: React、react-router-dom 路由、lucide-react 图标
// Output: React 组件 BasicSettingsLayout
// Pos: 系统设置 - 基础配置布局

import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { Settings, Shield } from 'lucide-react';
import { ROUTE_PATHS } from '../../routes/paths';

/**
 * 基础配置标签页配置
 */
const BASIC_TABS = [
    {
        key: 'basic',
        label: '基础设置',
        path: ROUTE_PATHS.SETTINGS_BASIC,
        icon: Settings,
    },
    {
        key: 'security',
        label: '安全合规',
        path: ROUTE_PATHS.SETTINGS_BASIC_SECURITY,
        icon: Shield,
    },
];

/**
 * 基础配置布局容器
 *
 * 提供标签页导航 + Outlet 渲染子路由
 */
export const BasicSettingsLayout: React.FC = () => {
    const location = useLocation();

    return (
        <div className="min-h-full bg-slate-50">
            {/* 页面标题 */}
            <div className="bg-white border-b border-slate-200 px-8 py-4">
                <h1 className="text-xl font-bold text-slate-900">基础配置</h1>
                <p className="text-sm text-slate-500 mt-1">系统基础参数、安全策略</p>
            </div>

            {/* 标签页导航 */}
            <div className="bg-white border-b border-slate-200 px-8">
                <nav className="flex space-x-1 overflow-x-auto" aria-label="基础配置导航">
                    {BASIC_TABS.map((tab) => {
                        const isActive = location.pathname === tab.path;
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
            <div className="px-6 py-6 max-w-7xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-300">
                <Outlet />
            </div>
        </div>
    );
};

export default BasicSettingsLayout;
