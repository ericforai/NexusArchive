// Input: React、react-router-dom 路由、lucide-react 图标、本地模块 constants、types、hooks/usePermissions 等
// Output: React 组件 Sidebar
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo, useState, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { NAV_ITEMS } from '../constants.tsx';
import { ViewState } from '../types';
import { ChevronsLeft, ChevronsRight, Command } from 'lucide-react';
import { usePermissions } from '../hooks/usePermissions';
// ROUTE_PATHS 暂不使用，需要时可恢复
// import { ROUTE_PATHS } from '../routes/paths';
import { NavNode } from './Sidebar/NavNode';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  onVisitLanding: () => void;
}

// Map Path Prefixes to IDs for auto-expansion
const PATH_PREFIX_TO_VIEW: Record<string, string> = {
  '/system/pre-archive': ViewState.PRE_ARCHIVE,
  '/system/collection': ViewState.COLLECTION,
  '/system/operations': ViewState.ARCHIVE_OPS,
  '/system/archive': ViewState.ACCOUNT_ARCHIVES,
  '/system/utilization': ViewState.ARCHIVE_UTILIZATION,
  '/system/warehouse': ViewState.WAREHOUSE,
  '/system/stats': ViewState.STATS,
  '/system/settings': ViewState.SETTINGS,
  '/system/admin': ViewState.ADMIN,
  '/system/destruction': ViewState.DESTRUCTION,
  '/system/panorama': ViewState.PANORAMA,
  '/system/matching': ViewState.MATCHING,
  '/system/quality': ViewState.QUALITY,
  '/system/audit': ViewState.AUDIT,
};

export const Sidebar: React.FC<SidebarProps> = ({
  collapsed,
  onToggle,
  onVisitLanding: _onVisitLanding, // 预留接口，暂未使用
}) => {
  const { hasPermission } = usePermissions();
  const location = useLocation();
  const [expandedMenus, setExpandedMenus] = useState<Set<string>>(new Set());
  const [collapsedTopLevel, setCollapsedTopLevel] = useState<Set<string>>(new Set());

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

  // Toggle handler
  const handleToggle = useCallback((id: string) => {
    if (collapsed) {
      onToggle();
      return;
    }

    const isCurrentlyExpanded = expandedMenusWithAuto.has(id);

    setExpandedMenus(prev => {
      const next = new Set(prev);
      if (isCurrentlyExpanded) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });

    if (id === activeTopLevelId) {
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

  return (
    <aside className={`${collapsed ? 'w-20' : 'w-72'} h-screen bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shadow-2xl z-20 relative transition-all duration-300`}>
      {/* Background Decorative Elements */}
      <div className="absolute top-0 left-0 w-full h-64 bg-gradient-to-b from-blue-900/20 to-transparent pointer-events-none" />

      {/* Logo Area */}
      <div className={`p-6 flex items-center ${collapsed ? 'justify-center' : 'space-x-3'} border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm z-10 transition-all duration-300`}>
        <a href="/system" className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 text-white flex-shrink-0">
          <Command size={20} />
        </a>
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
          const siblings = filteredNav.map(n => n.id).filter(id => id !== item.id);
          return (
            <NavNode
              key={item.id}
              item={item}
              level={0}
              siblings={siblings}
              collapsed={collapsed}
              expandedMenusWithAuto={expandedMenusWithAuto}
              activeTopLevelId={activeTopLevelId}
              onToggle={handleToggle}
            />
          );
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
