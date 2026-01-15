// Input: NavItem、路由相关 hooks、权限检查
// Output: NavNode 递归导航节点组件
// Pos: src/components/Sidebar/ 子组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useMemo } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { ChevronDown, FolderOpen } from 'lucide-react';
import { NavItem, ViewState } from '../../types';
import { ROUTE_PATHS, SUBITEM_TO_PATH } from '../../routes/paths';

// Map ViewState to Route Paths
const VIEW_TO_PATH: Record<string, string> = {
  [ViewState.PORTAL]: ROUTE_PATHS.PORTAL,
  [ViewState.PANORAMA]: ROUTE_PATHS.PANORAMA,
  [ViewState.PRE_ARCHIVE]: ROUTE_PATHS.PRE_ARCHIVE,
  [ViewState.COLLECTION]: ROUTE_PATHS.COLLECTION,
  [ViewState.ACCOUNT_ARCHIVES]: ROUTE_PATHS.ARCHIVE,
  [ViewState.ARCHIVE_OPS]: ROUTE_PATHS.ARCHIVE_OPS,
  [ViewState.ARCHIVE_UTILIZATION]: ROUTE_PATHS.ARCHIVE_UTILIZATION,
  [ViewState.STATS]: ROUTE_PATHS.STATS,
  [ViewState.SETTINGS]: ROUTE_PATHS.SETTINGS,
  [ViewState.ADMIN]: ROUTE_PATHS.ADMIN,
  [ViewState.DESTRUCTION]: ROUTE_PATHS.DESTRUCTION,
  [ViewState.LANDING]: '/',
  [ViewState.ABNORMAL]: ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,
  [ViewState.COMPLIANCE_REPORT]: ROUTE_PATHS.ARCHIVE,
  [ViewState.MATCHING]: '/system/matching',
};

interface NavNodeProps {
  item: NavItem;
  level: number;
  siblings: string[];
  collapsed: boolean;
  expandedMenusWithAuto: Set<string>;
  activeTopLevelId: string;
  onToggle: (id: string) => void;
}

/**
 * Check if a nav item is currently active
 */
const useIsActive = () => {
  const location = useLocation();

  return useCallback((item: NavItem) => {
    let targetPath = item.path;
    if (!targetPath && VIEW_TO_PATH[item.id]) {
      targetPath = VIEW_TO_PATH[item.id];
    } else if (item.path && SUBITEM_TO_PATH[item.path]) {
      targetPath = SUBITEM_TO_PATH[item.path];
    }

    if (!targetPath) return false;

    const [pathStr, queryStr] = targetPath.split('?');
    const isPathMatch = location.pathname === pathStr || location.pathname.startsWith(pathStr + '/');
    if (!isPathMatch) return false;

    if (queryStr) {
      return location.search.includes(queryStr);
    }

    return true;
  }, [location.pathname, location.search]);
};

/**
 * Determine the main link path for a nav item
 */
const resolveMainPath = (item: NavItem, hasChildren: boolean): string => {
  if (!hasChildren) {
    if (VIEW_TO_PATH[item.id]) return VIEW_TO_PATH[item.id];
    if (item.path && SUBITEM_TO_PATH[item.path]) return SUBITEM_TO_PATH[item.path];
    if (item.path) return item.path;
  } else if (item.path && SUBITEM_TO_PATH[item.path]) {
    return SUBITEM_TO_PATH[item.path];
  }
  return '#';
};

/**
 * Get styling classes based on level
 */
const getLevelStyles = (level: number) => {
  const padding = level === 0 ? 'px-4' : level === 1 ? 'pl-10 pr-4' : level === 2 ? 'pl-12 pr-4' : 'pl-16 pr-4';
  const fontSize = level === 0 ? 'text-sm font-medium' : 'text-xs';
  const py = level === 0 ? 'py-3' : 'py-2';
  return { padding, fontSize, py };
};

/**
 * Parent button component for items with children
 */
const ParentButton: React.FC<{
  item: NavItem;
  mainPath: string;
  collapsed: boolean;
  isActive: boolean;
  isExpanded: boolean;
  levelStyles: ReturnType<typeof getLevelStyles>;
  onClick: () => void;
}> = ({ item, mainPath, collapsed, isActive, isExpanded, levelStyles, onClick }) => {
  const navigate = useNavigate();
  const Icon = item.icon;
  const commonClasses = `w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} ${levelStyles.padding} ${levelStyles.py} rounded-xl transition-all duration-300 mb-0.5`;
  const activeClasses = 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20';
  const inactiveClasses = 'hover:bg-slate-800/50 hover:text-slate-100 text-slate-400';

  const handleClick = () => {
    onClick();
    if (mainPath !== '#') {
      navigate(mainPath);
    }
  };

  return (
    <button
      onClick={handleClick}
      className={`${commonClasses} ${isActive && collapsed ? activeClasses : (isActive || isExpanded) ? 'text-white' : inactiveClasses}`}
      title={collapsed ? item.label : ''}
    >
      <div className={`flex items-center ${collapsed ? '' : 'space-x-3'} ${collapsed ? 'justify-center' : ''}`}>
        {Icon && <Icon size={20} className={`transition-colors flex-shrink-0 ${(isActive || isExpanded) ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`} />}
        {!collapsed && <span className={`${levelStyles.fontSize} tracking-wide whitespace-normal text-left leading-tight`}>{item.label}</span>}
      </div>
      {!collapsed && (
        <div className="flex items-center">
          {levelStyles.padding !== 'px-4' && <FolderOpen size={12} className="mr-2 text-slate-500" />}
          <ChevronDown
            size={14}
            className={`text-slate-500 transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`}
          />
        </div>
      )}
    </button>
  );
};

/**
 * Leaf link component for items without children
 */
const LeafLink: React.FC<{
  item: NavItem;
  mainPath: string;
  collapsed: boolean;
  isActive: boolean;
  level: number;
  levelStyles: ReturnType<typeof getLevelStyles>;
}> = ({ item, mainPath, collapsed, isActive, level, levelStyles }) => {
  const Icon = item.icon;
  const commonClasses = `w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} ${levelStyles.padding} ${levelStyles.py} rounded-xl transition-all duration-300 mb-0.5`;
  const activeClasses = 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20';
  const inactiveClasses = 'hover:bg-slate-800/50 hover:text-slate-100 text-slate-400';

  return (
    <NavLink
      to={mainPath}
      className={({ isActive: linkActive }) => {
        const active = isActive || linkActive;
        return `${commonClasses} ${active ? activeClasses : inactiveClasses}`;
      }}
      title={collapsed ? item.label : ''}
      end={item.id === ViewState.PORTAL}
    >
      <div className={`flex items-center ${collapsed ? '' : 'space-x-3'} ${collapsed ? 'justify-center' : ''}`}>
        {Icon && <Icon size={20} className={`transition-colors flex-shrink-0 ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`} />}
        {!Icon && level >= 3 && (
          <span className={`w-1.5 h-1.5 rounded-full mr-2 flex-shrink-0 ${isActive ? 'bg-primary-400' : 'bg-slate-600 group-hover:bg-slate-500'}`} />
        )}
        {!collapsed && <span className={`${levelStyles.fontSize} tracking-wide whitespace-normal text-left leading-tight`}>{item.label}</span>}
      </div>
      {!collapsed && isActive && level === 0 && (
        <div className="w-1.5 h-1.5 rounded-full bg-primary-400 shadow-[0_0_8px_rgba(56,189,248,0.8)]" />
      )}
    </NavLink>
  );
};

/**
 * Children container with border hierarchy
 */
const ChildrenContainer: React.FC<{
  items: NavItem[];
  level: number;
  renderChild: (child: NavItem, siblings: string[]) => React.ReactNode;
}> = ({ items, level, renderChild }) => {
  const siblings = items.map(c => c.id);
  const ml = level === 0 ? 'ml-7' : 'ml-4';

  return (
    <div className={`overflow-hidden transition-all duration-300 ${level === 0 ? 'mt-1' : ''}`}>
      <div className={`${ml} border-l border-slate-800 space-y-1`}>
        {items.map(child => renderChild(child, siblings.filter(id => id !== child.id)))}
      </div>
    </div>
  );
};

/**
 * Navigation node component (recursive)
 */
export const NavNode: React.FC<NavNodeProps> = ({
  item,
  level,
  siblings: _siblings, // 预留参数，未来可用于同级导航
  collapsed,
  expandedMenusWithAuto,
  activeTopLevelId,
  onToggle,
}) => {
  const hasChildren = Boolean(item.children && item.children.length > 0);
  const isExpanded = expandedMenusWithAuto.has(item.id);
  const isActive = useIsActive()(item);
  const mainPath = useMemo(() => resolveMainPath(item, hasChildren), [item, hasChildren]);
  const levelStyles = useMemo(() => getLevelStyles(level), [level]);
  // Icon 已在 ParentButton/LeafLink 中直接使用 item.icon

  // Recursive render function for children
  const renderChild = useCallback((child: NavItem, childSiblings: string[]) => {
    return (
      <NavNode
        key={child.id}
        item={child}
        level={level + 1}
        siblings={childSiblings}
        collapsed={collapsed}
        expandedMenusWithAuto={expandedMenusWithAuto}
        activeTopLevelId={activeTopLevelId}
        onToggle={onToggle}
      />
    );
  }, [level, collapsed, expandedMenusWithAuto, activeTopLevelId, onToggle]);

  return (
    <div className="group relative">
      {hasChildren ? (
        <>
          <ParentButton
            item={item}
            mainPath={mainPath}
            collapsed={collapsed}
            isActive={isActive}
            isExpanded={isExpanded}
            levelStyles={levelStyles}
            onClick={() => onToggle(item.id)}
          />
          {!collapsed && isExpanded && (
            <ChildrenContainer
              items={item.children!}
              level={level}
              renderChild={renderChild}
            />
          )}
        </>
      ) : (
        <LeafLink
          item={item}
          mainPath={mainPath}
          collapsed={collapsed}
          isActive={isActive}
          level={level}
          levelStyles={levelStyles}
        />
      )}
    </div>
  );
};
