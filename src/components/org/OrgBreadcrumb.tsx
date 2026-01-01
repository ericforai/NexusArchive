// Input: React, lucide-react
// Output: OrgBreadcrumb 组件
// Pos: 通用复用组件 - 组织面包屑导航

import React from 'react';
import { ChevronRight, Building2, Home } from 'lucide-react';
import { OrgNode } from '../../types';

export interface OrgBreadcrumbProps {
  orgTree: OrgNode[];
  selectedOrgId?: string;
  onNavigate?: (orgId: string) => void;
  className?: string;
  showHome?: boolean;
  homeLabel?: string;
}

/**
 * 组织面包屑导航
 * <p>
 * 显示当前选中组织在组织树中的路径
 * </p>
 */
export function OrgBreadcrumb({
  orgTree,
  selectedOrgId,
  onNavigate,
  className = '',
  showHome = true,
  homeLabel = '首页',
}: OrgBreadcrumbProps) {
  // 查找组织路径
  const findPath = (nodes: OrgNode[], targetId: string, path: OrgNode[] = []): OrgNode[] | null => {
    for (const node of nodes) {
      const currentPath = [...path, node];
      if (node.id === targetId) {
        return currentPath;
      }
      if (node.children) {
        const result = findPath(node.children, targetId, currentPath);
        if (result) return result;
      }
    }
    return null;
  };

  const path = selectedOrgId ? findPath(orgTree, selectedOrgId) : null;

  if (!path) {
    return null;
  }

  return (
    <nav className={`org-breadcrumb flex items-center gap-1 text-sm ${className}`}>
      {/* Home */}
      {showHome && (
        <>
          <button
            onClick={() => onNavigate?.('')}
            className="flex items-center gap-1 text-slate-500 hover:text-slate-700 transition-colors"
          >
            <Home size={14} />
            <span>{homeLabel}</span>
          </button>
          <ChevronRight size={14} className="text-slate-300" />
        </>
      )}

      {/* Path */}
      {path.map((org, index) => (
        <React.Fragment key={org.id}>
          {index > 0 && <ChevronRight size={14} className="text-slate-300" />}
          <button
            onClick={() => onNavigate?.(org.id)}
            className={`flex items-center gap-1 transition-colors ${
              index === path.length - 1
                ? 'text-blue-600 font-medium'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {index === 0 && <Building2 size={14} />}
            <span className="max-w-[150px] truncate">{org.name}</span>
          </button>
        </React.Fragment>
      ))}
    </nav>
  );
}

/**
 * 简化版面包屑（仅显示名称）
 */
export interface OrgBreadcrumbSimpleProps {
  items: Array<{ id: string; name: string }>;
  onNavigate?: (id: string) => void;
  className?: string;
  separator?: React.ReactNode;
}

export function OrgBreadcrumbSimple({
  items,
  onNavigate,
  className = '',
  separator = <ChevronRight size={14} className="text-slate-300" />,
}: OrgBreadcrumbSimpleProps) {
  if (items.length === 0) return null;

  return (
    <nav className={`org-breadcrumb-simple flex items-center gap-1 text-sm ${className}`}>
      {items.map((item, index) => (
        <React.Fragment key={item.id}>
          {index > 0 && separator}
          <button
            onClick={() => onNavigate?.(item.id)}
            className={`transition-colors ${
              index === items.length - 1
                ? 'text-blue-600 font-medium'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {item.name}
          </button>
        </React.Fragment>
      ))}
    </nav>
  );
}

export default OrgBreadcrumb;
