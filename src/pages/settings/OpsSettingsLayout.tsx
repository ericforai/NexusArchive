// Input: React、react-router-dom 路由、lucide-react 图标
// Output: React 组件 OpsSettingsLayout
// Pos: 系统设置 - 系统运维布局

import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { Link, FileSearch, Upload, Lock } from 'lucide-react';
import { ROUTE_PATHS } from '../../routes/paths';

/**
 * 系统运维标签页配置
 */
const OPS_TABS = [
    {
        key: 'integration',
        label: '集成中心',
        path: ROUTE_PATHS.SETTINGS_INTEGRATION,
        icon: Link,
    },
    {
        key: 'audit',
        label: '审计日志',
        path: ROUTE_PATHS.SETTINGS_AUDIT,
        icon: FileSearch,
    },
    {
        key: 'data-import',
        label: '数据导入',
        path: ROUTE_PATHS.SETTINGS_DATA_IMPORT,
        icon: Upload,
    },
    {
        key: 'security',
        label: '安全合规',
        path: ROUTE_PATHS.SETTINGS_SECURITY,
        icon: Lock,
    },
];

/**
 * 系统运维布局容器
 *
 * 提供标签页导航 + Outlet 渲染子路由
 */
export const OpsSettingsLayout: React.FC = () => {
    const location = useLocation();

    return (
        <div className="min-h-full bg-slate-50">
            {/* 页面标题 */}
            <div className="bg-white border-b border-slate-200 px-8 py-4">
                <h1 className="text-xl font-bold text-slate-900">系统运维</h1>
                <p className="text-sm text-slate-500 mt-1">集成中心、审计日志、数据导入、安全合规</p>
            </div>

            {/* 标签页导航 */}
            <div className="bg-white border-b border-slate-200 px-8">
                <nav className="flex space-x-1 overflow-x-auto" aria-label="系统运维导航">
                    {OPS_TABS.map((tab) => {
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

export default OpsSettingsLayout;
