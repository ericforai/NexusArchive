/**
 * 侧边栏导航组件（路由版本）
 * 
 * 使用 React Router NavLink 实现真正的路由导航
 */
import React, { useMemo, useEffect } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { NAV_ITEMS } from '../constants';
import { ViewState } from '../types';
import { ChevronRight, ChevronDown, ChevronsLeft, ChevronsRight, Command } from 'lucide-react';
import { usePermissions } from '../hooks/usePermissions';
import { ROUTE_PATHS, SUBITEM_TO_PATH } from '../routes/paths';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  onVisitLanding: () => void;
}

// ViewState 到路由路径的映射
const VIEW_TO_PATH: Record<ViewState, string> = {
  [ViewState.PORTAL]: ROUTE_PATHS.PORTAL,
  [ViewState.PANORAMA]: ROUTE_PATHS.PANORAMA,
  [ViewState.PRE_ARCHIVE]: ROUTE_PATHS.PRE_ARCHIVE,
  [ViewState.COLLECTION]: ROUTE_PATHS.COLLECTION,
  [ViewState.ARCHIVE_MGMT]: ROUTE_PATHS.ARCHIVE,
  [ViewState.QUERY]: ROUTE_PATHS.QUERY,
  [ViewState.BORROWING]: ROUTE_PATHS.BORROWING,
  [ViewState.WAREHOUSE]: ROUTE_PATHS.WAREHOUSE,
  [ViewState.STATS]: ROUTE_PATHS.STATS,
  [ViewState.SETTINGS]: ROUTE_PATHS.SETTINGS,
  [ViewState.ADMIN]: ROUTE_PATHS.ADMIN,
  [ViewState.DESTRUCTION]: ROUTE_PATHS.DESTRUCTION,
  [ViewState.LANDING]: '/',
  [ViewState.ABNORMAL]: ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,
  [ViewState.COMPLIANCE_REPORT]: ROUTE_PATHS.ARCHIVE,
};

// 路径前缀到 ViewState 的映射（用于判断当前激活的菜单组）
const PATH_PREFIX_TO_VIEW: Record<string, ViewState> = {
  '/system/pre-archive': ViewState.PRE_ARCHIVE,
  '/system/collection': ViewState.COLLECTION,
  '/system/archive': ViewState.ARCHIVE_MGMT,
  '/system/query': ViewState.QUERY,
  '/system/borrowing': ViewState.BORROWING,
  '/system/warehouse': ViewState.WAREHOUSE,
  '/system/stats': ViewState.STATS,
  '/system/settings': ViewState.SETTINGS,
  '/system/admin': ViewState.ADMIN,
  '/system/destruction': ViewState.DESTRUCTION,
  '/system/panorama': ViewState.PANORAMA,
};

export const Sidebar: React.FC<SidebarProps> = ({
  collapsed,
  onToggle,
  onVisitLanding,
}) => {
  const { hasPermission } = usePermissions();
  const location = useLocation();
  const [expandedMenus, setExpandedMenus] = React.useState<Set<string>>(new Set());

  // 根据当前路径确定激活的菜单组
  const activeViewFromPath = useMemo(() => {
    const path = location.pathname;

    // 精确匹配 /system
    if (path === '/system' || path === '/system/') {
      return ViewState.PORTAL;
    }

    // 前缀匹配
    for (const [prefix, view] of Object.entries(PATH_PREFIX_TO_VIEW)) {
      if (path.startsWith(prefix)) {
        return view;
      }
    }

    return ViewState.PORTAL;
  }, [location.pathname]);

  // 自动展开当前激活的菜单组
  useEffect(() => {
    setExpandedMenus(prev => {
      const next = new Set(prev);
      next.add(activeViewFromPath);
      return next;
    });
  }, [activeViewFromPath]);

  const filteredNav = useMemo(() => {
    return NAV_ITEMS.filter(item => {
      if (!item.permission) return true;
      return hasPermission(item.permission) || hasPermission('nav:all') || hasPermission('system_admin');
    });
  }, [hasPermission]);

  const handleMainClick = (id: ViewState, hasSubItems: boolean) => {
    if (collapsed) {
      onToggle();
      return;
    }

    // 切换子菜单展开状态
    if (hasSubItems) {
      setExpandedMenus(prev => {
        const next = new Set(prev);
        if (next.has(id)) {
          next.delete(id);
        } else {
          next.add(id);
        }
        return next;
      });
    }
  };

  // 检查当前路径是否匹配某个子菜单项
  const isSubItemActive = (subItem: string): boolean => {
    const path = SUBITEM_TO_PATH[subItem];
    if (!path) return false;
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  return (
    <aside className={`${collapsed ? 'w-20' : 'w-64'} h-screen bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shadow-2xl z-20 relative transition-all duration-300`}>
      {/* Background Decorative Elements */}
      <div className="absolute top-0 left-0 w-full h-64 bg-gradient-to-b from-blue-900/20 to-transparent pointer-events-none" />

      {/* Logo Area */}
      <div className={`p-6 flex items-center ${collapsed ? 'justify-center' : 'space-x-3'} border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm z-10 transition-all duration-300`}>
        <NavLink to="/system" className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 text-white flex-shrink-0">
          <Command size={20} />
        </NavLink>
        {!collapsed && (
          <div className="overflow-hidden whitespace-nowrap animate-in fade-in duration-300">
            <h1 className="font-bold text-white text-lg leading-tight tracking-tight">DigiVoucher</h1>
            <p className="text-xs text-slate-500 font-medium">电子会计档案</p>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-6 px-3 space-y-1 scrollbar-thin scrollbar-thumb-slate-700">
        {filteredNav.map((item) => {
          const isActive = activeViewFromPath === item.id;
          const isExpanded = expandedMenus.has(item.id);
          const mainPath = VIEW_TO_PATH[item.id] || '/system';

          return (
            <div key={item.id} className="group relative">
              {/* 主导航项 */}
              {item.subItems ? (
                // 有子菜单：使用 button 控制展开
                <button
                  onClick={() => handleMainClick(item.id, true)}
                  className={`w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} px-4 py-3 rounded-xl transition-all duration-300 ${isActive
                    ? 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20'
                    : 'hover:bg-slate-800/50 hover:text-slate-100'
                    }`}
                  title={collapsed ? item.label : ''}
                >
                  <div className={`flex items-center ${collapsed ? '' : 'space-x-3'}`}>
                    <item.icon
                      size={20}
                      className={`transition-colors ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`}
                    />
                    {!collapsed && <span className="font-medium text-sm tracking-wide">{item.label}</span>}
                  </div>

                  {!collapsed && (
                    <div className="flex items-center">
                      <ChevronDown
                        size={14}
                        className={`mr-2 text-slate-500 transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`}
                      />
                      {isActive && (
                        <div className="w-1.5 h-1.5 rounded-full bg-primary-400 shadow-[0_0_8px_rgba(56,189,248,0.8)]" />
                      )}
                    </div>
                  )}
                </button>
              ) : (
                // 无子菜单：直接使用 NavLink
                <NavLink
                  to={mainPath}
                  className={({ isActive: linkActive }) => `w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} px-4 py-3 rounded-xl transition-all duration-300 ${linkActive || isActive
                    ? 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20'
                    : 'hover:bg-slate-800/50 hover:text-slate-100'
                    }`}
                  title={collapsed ? item.label : ''}
                  end={item.id === ViewState.PORTAL}
                >
                  <div className={`flex items-center ${collapsed ? '' : 'space-x-3'}`}>
                    <item.icon
                      size={20}
                      className={`transition-colors ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`}
                    />
                    {!collapsed && <span className="font-medium text-sm tracking-wide">{item.label}</span>}
                  </div>

                  {!collapsed && isActive && (
                    <div className="w-1.5 h-1.5 rounded-full bg-primary-400 shadow-[0_0_8px_rgba(56,189,248,0.8)]" />
                  )}
                </NavLink>
              )}

              {/* 子菜单项 */}
              {!collapsed && item.subItems && isExpanded && (
                <div className="mt-1 ml-10 space-y-0.5 border-l border-slate-800 pl-2">
                  {item.subItems.map((sub) => {
                    const subPath = SUBITEM_TO_PATH[sub] || mainPath;
                    const isSubActive = isSubItemActive(sub);

                    return (
                      <NavLink
                        key={sub}
                        to={subPath}
                        className={`block w-full text-left px-3 py-2.5 text-xs rounded-lg transition-colors ${isSubActive
                          ? 'text-primary-300 bg-slate-800/60 font-medium'
                          : 'text-slate-400 hover:text-primary-300 hover:bg-slate-800/30'
                          }`}
                      >
                        <span className="flex items-center">
                          <ChevronRight size={10} className={`mr-2 transition-opacity ${isSubActive ? 'opacity-100' : 'opacity-0'}`} />
                          {sub}
                        </span>
                      </NavLink>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* Footer User Info */}
      <div className="p-4 border-t border-slate-800 bg-slate-900/80 z-10 space-y-2">
        {!collapsed && (
          <button
            onClick={onVisitLanding}
            className="w-full flex items-center justify-center space-x-2 px-2 py-2 rounded-lg bg-primary-600/20 border border-primary-500/30 text-primary-400 hover:bg-primary-600/30 transition-colors text-xs font-bold"
          >
            <span>访问产品官网</span>
          </button>
        )}

        <button
          onClick={() => alert('打开用户个人中心')}
          className={`w-full flex items-center ${collapsed ? 'justify-center' : 'space-x-3'} px-2 py-2 rounded-lg bg-slate-800/50 border border-slate-700/50 hover:bg-slate-800 transition-colors`}
        >
          <img src="https://picsum.photos/32/32" alt="User" className="w-8 h-8 rounded-full ring-2 ring-slate-700 flex-shrink-0" />
          {!collapsed && (
            <div className="flex-1 overflow-hidden text-left">
              <p className="text-sm font-medium text-white truncate">管理员</p>
              <p className="text-xs text-slate-400 truncate">系统维护部</p>
            </div>
          )}
        </button>

        {/* Toggle Button */}
        <button
          onClick={onToggle}
          className="w-full h-8 flex items-center justify-center text-slate-500 hover:text-slate-300 hover:bg-slate-800/50 rounded-lg transition-colors mt-2"
        >
          {collapsed ? <ChevronsRight size={16} /> : <ChevronsLeft size={16} />}
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
