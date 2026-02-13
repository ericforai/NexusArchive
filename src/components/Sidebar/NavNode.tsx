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
  [ViewState.WAREHOUSE]: ROUTE_PATHS.WAREHOUSE,
  [ViewState.STATS]: ROUTE_PATHS.STATS,
  [ViewState.QUALITY]: ROUTE_PATHS.QUALITY,
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
 *
 * 优先级：
 * 1. VIEW_TO_PATH 映射（适用于一级菜单，即使有子菜单也可点击）
 * 2. SUBITEM_TO_PATH 映射（适用于子菜单项）
 * 3. item.path 原始值
 * 4. '#' 兜底（仅展开/收起，不导航）
 */
const resolveMainPath = (item: NavItem): string => {
  // 优先级1: VIEW_TO_PATH 映射
  if (VIEW_TO_PATH[item.id]) return VIEW_TO_PATH[item.id];
  // 优先级2: SUBITEM_TO_PATH 映射
  if (item.path && SUBITEM_TO_PATH[item.path]) return SUBITEM_TO_PATH[item.path];
  // 优先级3: 原始 path
  if (item.path) return item.path;
  // 兜底
  return '#';
};

// Level-specific styling constants
const LEVEL_STYLES = [
  { padding: 'px-4', fontSize: 'text-sm font-medium', py: 'py-3' },      // level 0
  { padding: 'pl-10 pr-4', fontSize: 'text-xs', py: 'py-2' },           // level 1
  { padding: 'pl-12 pr-4', fontSize: 'text-xs', py: 'py-2' },           // level 2
  { padding: 'pl-16 pr-4', fontSize: 'text-xs', py: 'py-2' },           // level 3+
] as const;

const getLevelStyles = (level: number) => LEVEL_STYLES[Math.min(level, 3)];

// Shared style class generators
const navStyles = {
  active: 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20',
  inactive: 'hover:bg-slate-800/50 hover:text-slate-100 text-slate-400',
  getCommon: (collapsed: boolean, padding: string, py: string) =>
    `w-full flex items-center ${collapsed ? 'justify-center' : 'justify-between'} ${padding} ${py} rounded-xl transition-all duration-300 mb-0.5`,
};

/**
 * Shared icon + label rendering component
 */
const NavIconLabel: React.FC<{
  icon?: NavItem['icon'];
  label: string;
  collapsed: boolean;
  fontSize: string;
  isActive: boolean;
  level?: number;
}> = ({ icon, label, collapsed, fontSize, isActive, level }) => (
  <div className={`flex items-center ${collapsed ? '' : 'space-x-3'} ${collapsed ? 'justify-center' : ''}`}>
    {icon && React.createElement(icon, {
      size: 20,
      className: `transition-colors flex-shrink-0 ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`
    })}
    {!icon && level !== undefined && level >= 3 && (
      <span className={`w-1.5 h-1.5 rounded-full mr-2 flex-shrink-0 ${isActive ? 'bg-primary-400' : 'bg-slate-600 group-hover:bg-slate-500'}`} />
    )}
    {!collapsed && <span className={`${fontSize} tracking-wide whitespace-normal text-left leading-tight`}>{label}</span>}
  </div>
);

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
  const commonClasses = navStyles.getCommon(collapsed, levelStyles.padding, levelStyles.py);

  const handleClick = () => {
    onClick();
    if (mainPath !== '#') {
      navigate(mainPath);
    }
  };

  return (
    <button
      onClick={handleClick}
      className={`${commonClasses} ${isActive && collapsed ? navStyles.active : (isActive || isExpanded) ? 'text-white' : navStyles.inactive}`}
      title={collapsed ? item.label : ''}
    >
      <NavIconLabel
        icon={item.icon}
        label={item.label}
        collapsed={collapsed}
        fontSize={levelStyles.fontSize}
        isActive={isActive || isExpanded}
      />
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
  const commonClasses = navStyles.getCommon(collapsed, levelStyles.padding, levelStyles.py);

  return (
    <NavLink
      to={mainPath}
      className={({ isActive: linkActive }) => {
        const activeState = isActive || linkActive;
        return `${commonClasses} ${activeState ? navStyles.active : navStyles.inactive}`;
      }}
      title={collapsed ? item.label : ''}
      end={item.id === ViewState.PORTAL}
    >
      <NavIconLabel
        icon={item.icon}
        label={item.label}
        collapsed={collapsed}
        fontSize={levelStyles.fontSize}
        isActive={isActive}
        level={level}
      />
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
  const mainPath = useMemo(() => resolveMainPath(item), [item]);
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
