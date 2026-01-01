// Input: React
// Output: useOrg Hook
// Pos: 通用复用组件 - 组织数据管理 Hook

import { useState, useCallback, useEffect } from 'react';
import { OrgNode } from '../../types';

export interface UseOrgOptions {
  fetchOrgTree?: () => Promise<OrgNode[]>;
  defaultOrgId?: string;
  autoFetch?: boolean;
}

export interface UseOrgReturn {
  orgTree: OrgNode[];
  selectedOrgId: string | null;
  selectedOrg: OrgNode | null;
  selectedOrgPath: OrgNode[];
  loading: boolean;
  error: string | null;
  selectOrg: (orgId: string) => void;
  clearSelection: () => void;
  reload: () => Promise<void>;
  findOrgById: (id: string) => OrgNode | null;
  findOrgByName: (name: string) => OrgNode | null;
  getChildren: (orgId: string) => OrgNode[];
  getParent: (orgId: string) => OrgNode | null;
}

/**
 * 组织数据管理 Hook
 * <p>
 * 封装组织树数据的加载、选择、查找等操作
 * </p>
 */
export function useOrg({
  fetchOrgTree,
  defaultOrgId,
  autoFetch = true,
}: UseOrgOptions = {}): UseOrgReturn {
  const [orgTree, setOrgTree] = useState<OrgNode[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(defaultOrgId || null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 加载组织树
  const loadOrgTree = useCallback(async () => {
    if (!fetchOrgTree) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const tree = await fetchOrgTree();
      setOrgTree(tree);
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载组织树失败';
      setError(message);
      console.error('Failed to load org tree:', err);
    } finally {
      setLoading(false);
    }
  }, [fetchOrgTree]);

  // 自动加载
  useEffect(() => {
    if (autoFetch && fetchOrgTree) {
      loadOrgTree();
    }
  }, [autoFetch, fetchOrgTree, loadOrgTree]);

  // 查找组织
  const findOrgById = useCallback((id: string): OrgNode | null => {
    const search = (nodes: OrgNode[]): OrgNode | null => {
      for (const node of nodes) {
        if (node.id === id) return node;
        if (node.children) {
          const found = search(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    return search(orgTree);
  }, [orgTree]);

  const findOrgByName = useCallback((name: string): OrgNode | null => {
    const search = (nodes: OrgNode[]): OrgNode | null => {
      for (const node of nodes) {
        if (node.name === name) return node;
        if (node.children) {
          const found = search(node.children);
          if (found) return found;
        }
      }
      return null;
    };
    return search(orgTree);
  }, [orgTree]);

  // 查找路径
  const findPath = useCallback((targetId: string): OrgNode[] => {
    const search = (nodes: OrgNode[], id: string, path: OrgNode[] = []): OrgNode[] | null => {
      for (const node of nodes) {
        const currentPath = [...path, node];
        if (node.id === id) {
          return currentPath;
        }
        if (node.children) {
          const result = search(node.children, id, currentPath);
          if (result) return result;
        }
      }
      return null;
    };
    return search(orgTree, targetId) || [];
  }, [orgTree]);

  // 获取选中的组织
  const selectedOrg = selectedOrgId ? findOrgById(selectedOrgId) : null;

  // 获取选中组织的路径
  const selectedOrgPath = selectedOrgId ? findPath(selectedOrgId) : [];

  // 选择组织
  const selectOrg = useCallback((orgId: string) => {
    setSelectedOrgId(orgId);
  }, []);

  // 清除选择
  const clearSelection = useCallback(() => {
    setSelectedOrgId(null);
  }, []);

  // 获取子组织
  const getChildren = useCallback((orgId: string): OrgNode[] => {
    const org = findOrgById(orgId);
    return org?.children || [];
  }, [findOrgById]);

  // 获取父组织
  const getParent = useCallback((orgId: string): OrgNode | null => {
    const search = (nodes: OrgNode[], id: string, parent: OrgNode | null = null): OrgNode | null => {
      for (const node of nodes) {
        if (node.id === id) {
          return parent;
        }
        if (node.children) {
          const found = search(node.children, id, node);
          if (found) return found;
        }
      }
      return null;
    };
    return search(orgTree, orgId);
  }, [orgTree]);

  return {
    orgTree,
    selectedOrgId,
    selectedOrg,
    selectedOrgPath,
    loading,
    error,
    selectOrg,
    clearSelection,
    reload: loadOrgTree,
    findOrgById,
    findOrgByName,
    getChildren,
    getParent,
  };
}

export default useOrg;
