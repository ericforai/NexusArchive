// Input: React、react-router-dom 路由、lucide-react 图标
// Output: React 组件 OrgSettingsLayout
// Pos: 系统设置 - 组织管理布局

import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { Building2, Network, Shield, Users, History } from 'lucide-react';
import { ROUTE_PATHS } from '../../routes/paths';

/**
 * 组织管理标签页配置
 */
const ORG_TABS = [
  {
    key: 'entity',
    label: '法人管理',
    path: ROUTE_PATHS.SETTINGS_ORG_ENTITY,
    icon: Building2,
    description: '管理法人实体（法律主体，含统一社会信用代码）',
  },
  {
    key: 'architecture',
    label: '集团架构',
    path: ROUTE_PATHS.SETTINGS_ORG_ARCHITECTURE,
    icon: Network,
    description: '查看"法人 → 全宗 → 档案"层级关系',
  },
  {
    key: 'fonds',
    label: '全宗管理',
    path: ROUTE_PATHS.SETTINGS_ORG_FONDS,
    icon: Shield,
    description: '管理全宗（档案归集单元）',
  },
  {
    key: 'positions',
    label: '岗位管理',
    path: ROUTE_PATHS.SETTINGS_ORG_POSITIONS,
    icon: Users,
    description: '管理岗位',
  },
  {
    key: 'fonds-history',
    label: '全宗沿革',
    path: ROUTE_PATHS.SETTINGS_ORG_FONDS_HISTORY,
    icon: History,
    description: '全宗历史沿革记录',
  },
];

/**
 * 组织管理布局容器
 *
 * 提供标签页导航 + Outlet 渲染子路由
 */
export const OrgSettingsLayout: React.FC = () => {
  const location = useLocation();

  return (
    <div className="min-h-full bg-slate-50">
      {/* 页面标题 */}
      <div className="bg-white border-b border-slate-200 px-8 py-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-900">组织管理</h1>
            <p className="text-sm text-slate-500 mt-1">管理法人、全宗、岗位等组织基础数据</p>
          </div>
        </div>
      </div>

      {/* Tab 导航 */}
      <div className="bg-white border-b border-slate-200 px-8">
        <nav className="flex space-x-1 overflow-x-auto" aria-label="组织管理导航">
          {ORG_TABS.map((tab) => {
            const isActive = location.pathname === tab.path ||
                              (tab.key === 'entity' && location.pathname === ROUTE_PATHS.SETTINGS_ORG);
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

export default OrgSettingsLayout;
