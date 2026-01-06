// Input: React、react-router-dom 路由、lucide-react 图标、本地模块 constants、types、hooks/usePermissions 等
// Output: React 组件 Sidebar
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo, useState, useCallback } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { NAV_ITEMS } from '../constants';
import { NavItem, ViewState } from '../types';
import { ChevronDown, ChevronsLeft, ChevronsRight, Command, FolderOpen } from 'lucide-react';
import { usePermissions } from '../hooks/usePermissions';
import { ROUTE_PATHS, SUBITEM_TO_PATH } from '../routes/paths';
import { toast } from '../utils/notificationService';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  onVisitLanding: () => void;
}

// Map ViewState to Route Paths
const VIEW_TO_PATH: Record<string, string> = {
  [ViewState.PORTAL]: ROUTE_PATHS.PORTAL,
  [ViewState.PANORAMA]: ROUTE_PATHS.PANORAMA,
  [ViewState.PRE_ARCHIVE]: ROUTE_PATHS.PRE_ARCHIVE,
  [ViewState.COLLECTION]: ROUTE_PATHS.COLLECTION,

  [ViewState.ACCOUNT_ARCHIVES]: ROUTE_PATHS.ARCHIVE, // Repository
  [ViewState.ARCHIVE_OPS]: ROUTE_PATHS.ARCHIVE_OPS, // Operations
  [ViewState.ARCHIVE_UTILIZATION]: ROUTE_PATHS.ARCHIVE_UTILIZATION, // Utilization

  // [ViewState.WAREHOUSE]: ROUTE_PATHS.WAREHOUSE, // Removed
  [ViewState.STATS]: ROUTE_PATHS.STATS,
  [ViewState.SETTINGS]: ROUTE_PATHS.SETTINGS,
  [ViewState.ADMIN]: ROUTE_PATHS.ADMIN,
  [ViewState.DESTRUCTION]: ROUTE_PATHS.DESTRUCTION,
  [ViewState.LANDING]: '/',
  [ViewState.ABNORMAL]: ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,
  [ViewState.COMPLIANCE_REPORT]: ROUTE_PATHS.ARCHIVE,
  [ViewState.MATCHING]: '/system/matching',
};

// Map Path Prefixes to IDs for auto-expansion
const PATH_PREFIX_TO_VIEW: Record<string, string> = {
  '/system/pre-archive': ViewState.PRE_ARCHIVE,
  '/system/collection': ViewState.COLLECTION,

  // Specific prefixes ensure correct menu is highlighted
  '/system/operations': ViewState.ARCHIVE_OPS,
  '/system/archive': ViewState.ACCOUNT_ARCHIVES,
  '/system/utilization': ViewState.ARCHIVE_UTILIZATION,

  // '/system/warehouse': ViewState.WAREHOUSE, // Removed
  '/system/stats': ViewState.STATS,
  '/system/settings': ViewState.SETTINGS,
  '/system/admin': ViewState.ADMIN,
  '/system/destruction': ViewState.DESTRUCTION,
  '/system/panorama': ViewState.PANORAMA,
  '/system/matching': ViewState.MATCHING,
};

export const Sidebar: React.FC<SidebarProps> = ({
  collapsed,
  onToggle,
  onVisitLanding,
}) => {
  const { hasPermission } = usePermissions();
  const location = useLocation();
  const [expandedMenus, setExpandedMenus] = useState<Set<string>>(new Set());
  const [collapsedTopLevel, setCollapsedTopLevel] = useState<Set<string>>(new Set());

  // --- Auto-Expand Logic ---
  // Find which top-level ID matches the current path
  const activeTopLevelId = useMemo(() => {
    const path = location.pathname;
    if (path === '/system' || path === '/system/') return ViewState.PORTAL;

    for (const [prefix, viewId] of Object.entries(PATH_PREFIX_TO_VIEW)) {
      if (path.startsWith(prefix)) return viewId;
    }
    return ViewState.PORTAL;
  }, [location.pathname]);

  const expandedMenusWithAuto = useMemo(() => {
    const next = new Set(expandedMenus);
    if (activeTopLevelId && !collapsedTopLevel.has(activeTopLevelId)) {
      next.add(activeTopLevelId);
    }
    return next;
  }, [expandedMenus, activeTopLevelId, collapsedTopLevel]);

  // Filter Nav Items by Permission
  const filteredNav = useMemo(() => {
    return NAV_ITEMS.filter(item => {
      if (!item.permission) return true;
      return hasPermission(item.permission) || hasPermission('nav:all') || hasPermission('system_admin');
    });
  }, [hasPermission]);

  // --- Accordion Logic ---
  // Toggles an ID while closing its siblings
  const handleToggle = useCallback((id: string, siblings: string[], isTopLevel: boolean) => {
    if (collapsed) {
      onToggle();
      return;
    }

    const isCurrentlyExpanded = expandedMenusWithAuto.has(id);

    setExpandedMenus(prev => {
      const next = new Set(prev);

      if (isCurrentlyExpanded) {
        // Just collapse this one
        next.delete(id);
      } else {
        // Expand this one, collapse siblings
        siblings.forEach(siblingId => next.delete(siblingId));
        next.add(id);
      }
      return next;
    });

    if (isTopLevel && id === activeTopLevelId) {
      setCollapsedTopLevel(prev => {
        const next = new Set(prev);
        if (isCurrentlyExpanded) {
          next.add(id);
        } else {
          next.delete(id);
        }
        return next;
      });
    }
  }, [collapsed, onToggle, expandedMenusWithAuto, activeTopLevelId]);

  // --- Active Check Logic ---
  const isItemActive = useCallback((item: NavItem) => {
    // 1. If it's a top-level item, check against activeTopLevelId
    // But this is only for 'highlight'. 
    // For leaf nodes, we want precise match.

    // Determine target path
    let targetPath = item.path;
    if (!targetPath && VIEW_TO_PATH[item.id]) {
      targetPath = VIEW_TO_PATH[item.id];
    } else if (item.path && SUBITEM_TO_PATH[item.path]) {
      targetPath = SUBITEM_TO_PATH[item.path];
    }

    if (!targetPath) return false;

    // Split path and query
    const [pathStr, queryStr] = targetPath.split('?');

    // Check path match
    const isPathMatch = location.pathname === pathStr || location.pathname.startsWith(pathStr + '/');
    if (!isPathMatch) return false;

    // Check query param (e.g. ?type=MONTHLY)
    if (queryStr) {
      return location.search.includes(queryStr);
    }

    return true;
  }, [location.pathname, location.search]);


  // --- Recursive Render Function ---
  const renderNavNode = (item: NavItem, level: number, siblings: string[]) => {
    const hasChildren = item.children && item.children.length > 0;
    const isExpanded = expandedMenusWithAuto.has(item.id);
    const isActive = isItemActive(item);

    // Determine the main link path (if it's a leaf or clickable parent)
    let mainPath = '#';
    if (!hasChildren) {
      if (VIEW_TO_PATH[item.id]) mainPath = VIEW_TO_PATH[item.id];
      else if (item.path && SUBITEM_TO_PATH[item.path]) mainPath = SUBITEM_TO_PATH[item.path];
      else if (item.path) mainPath = item.path;
    }

    // Indentation based on level - 不同层级需要不同的缩进以区分层次
    const paddingLeft = level === 0 ? 'px-4' : level === 1 ? 'pl-10 pr-4' : level === 2 ? 'pl-12 pr-4' : 'pl-16 pr-4';
    // Font size adjustments
    const fontSize = level === 0 ? 'text-sm font-medium' : 'text-xs';

    const Icon = item.icon;

    // Wrapper for Leaf vs Parent
    const commonClasses = `w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} ${paddingLeft} py-${level === 0 ? '3' : '2'} rounded-xl transition-all duration-300 mb-0.5`;
    const activeClasses = 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20';
    const inactiveClasses = 'hover:bg-slate-800/50 hover:text-slate-100 text-slate-400';

    // If it's a Top Level Item and Collapsed, show tooltip? (Handled by standard HTML title usually)

    return (
      <div key={item.id} className="group relative">
        {hasChildren ? (
          // Parent Node: Button to toggle
          <button
            onClick={() => handleToggle(item.id, siblings, level === 0)}
            className={`${commonClasses} ${isItemActive(item) && collapsed ? activeClasses : filterActiveParent(isActive, isExpanded) ? 'text-white' : inactiveClasses}`}
            title={collapsed ? item.label : ''}
          >
            <div className={`flex items-center ${collapsed ? '' : 'space-x-3'} ${collapsed ? 'justify-center' : ''}`}>
              {Icon && <Icon size={20} className={`transition-colors flex-shrink-0 ${(isActive || isExpanded) ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`} />}
              {!collapsed && <span className={`${fontSize} tracking-wide whitespace-normal text-left leading-tight`}>{item.label}</span>}
            </div>
            {!collapsed && (
              <div className="flex items-center">
                {/* Folder Icon Decoration for Level > 0 */}
                {level > 0 && <FolderOpen size={12} className="mr-2 text-slate-500" />}
                <ChevronDown
                  size={14}
                  className={`text-slate-500 transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`}
                />
              </div>
            )}
          </button>
        ) : (
          // Leaf Node: NavLink
          <NavLink
            to={mainPath}
            className={({ isActive: linkActive }) => {
              // Custom active check because of query params
              const active = isActive || linkActive;
              return `${commonClasses} ${active ? activeClasses : inactiveClasses}`;
            }}
            title={collapsed ? item.label : ''}
            end={item.id === ViewState.PORTAL}
          >
            <div className={`flex items-center ${collapsed ? '' : 'space-x-3'} ${collapsed ? 'justify-center' : ''}`}>
              {Icon && <Icon size={20} className={`transition-colors flex-shrink-0 ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`} />}
              {/* 只有 level >= 3 的叶子节点才显示点（如销售订单、出库单等） */}
              {/* level 2 的叶子节点（如记账凭证）不显示任何装饰，与原始凭证等保持一致 */}
              {!Icon && level >= 3 && (
                <span className={`w-1.5 h-1.5 rounded-full mr-2 flex-shrink-0 ${isActive ? 'bg-primary-400' : 'bg-slate-600 group-hover:bg-slate-500'}`}></span>
              )}
              {!collapsed && <span className={`${fontSize} tracking-wide whitespace-normal text-left leading-tight`}>{item.label}</span>}
            </div>
            {!collapsed && isActive && level === 0 && (
              <div className="w-1.5 h-1.5 rounded-full bg-primary-400 shadow-[0_0_8px_rgba(56,189,248,0.8)]" />
            )}
          </NavLink>
        )}

        {/* Children Container (Accordion Body) */}
        {!collapsed && hasChildren && isExpanded && (
          <div className={`overflow-hidden transition-all duration-300 ${level === 0 ? 'mt-1' : ''}`}>
            {/* Border line for hierarchy */}
            {level === 0 && <div className="ml-7 border-l border-slate-800 space-y-1">
              {item.children!.map((child) => renderNavNode(
                child,
                level + 1,
                item.children!.map(s => s.id).filter(id => id !== child.id) // Pass siblings logic
              ))}
            </div>}
            {level > 0 && <div className="ml-4 border-l border-slate-800 space-y-1">
              {item.children!.map((child) => renderNavNode(
                child,
                level + 1,
                item.children!.map(s => s.id).filter(id => id !== child.id)
              ))}
            </div>}
          </div>
        )}
      </div>
    );
  };

  // Helper to determine if a parent text should be highlighted (when expanded)
  const filterActiveParent = (isActive: boolean, isExpanded: boolean) => {
    // Logic: If expanded, maybe highlight text slightly?
    return isExpanded;
  };


  return (
    <aside className={`${collapsed ? 'w-20' : 'w-72'} h-screen bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shadow-2xl z-20 relative transition-all duration-300`}>
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
          // Top level siblings
          const siblings = filteredNav.map(n => n.id).filter(id => id !== item.id);
          return renderNavNode(item, 0, siblings);
        })}
      </nav>

      {/* Footer User Info */}
      <div className="p-4 border-t border-slate-800 bg-slate-900/80 z-10 space-y-2">

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
