// Input: React hooks, ErpConfig types, ERP API
// Output: useErpConfigManager hook (state + actions)
// Pos: src/components/settings/integration/hooks/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { ErpConfig } from '../../../../types';
import { ErpConfigManagerState, ErpConfigManagerActions } from '../types';

interface UseErpConfigManagerOptions {
  erpApi: {
    getConfigs: () => Promise<any>;
    createConfig: (config: Partial<ErpConfig>) => Promise<any>;
    updateConfig: (id: number, config: Partial<ErpConfig>) => Promise<any>;
    deleteConfig: (id: number) => Promise<any>;
    testConnection: (id: number) => Promise<any>;
  };
}

export function useErpConfigManager(options: UseErpConfigManagerOptions) {
  const { erpApi } = options;

  const [configs, setConfigs] = useState<ErpConfig[]>([]);
  const [adapterTypes, setAdapterTypes] = useState<string[]>([]);
  const [expandedTypes, setExpandedTypes] = useState<Set<string>>(new Set());
  const [activeConfigId, setActiveConfigId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  const loadConfigs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await erpApi.getConfigs();
      if (res.code === 200 && res.data) {
        setConfigs(res.data);
        const uniqueTypes = new Set<string>(res.data.map((c: ErpConfig) => c.erpType?.toLowerCase() || 'generic'));
        const types = Array.from(uniqueTypes) as string[];
        setAdapterTypes(types);

        // Only set expanded types and active config if not already set
        setExpandedTypes((prev: Set<string>) => {
          if (prev.size === 0 && types.length > 0) {
            return new Set([types[0]]);
          }
          return prev;
        });

        setActiveConfigId(prev => {
          if (prev === null && types.length > 0) {
            const firstOfType = res.data.find((c: ErpConfig) => (c.erpType?.toLowerCase() || 'generic') === types[0]);
            return firstOfType ? firstOfType.id : null;
          }
          return prev;
        });
      }
    } catch {
      toast.error('加载配置失败');
    } finally {
      setLoading(false);
    }
  }, [erpApi]);

  const toggleTypeExpansion = useCallback((type: string) => {
    setExpandedTypes(prev => {
      const newSet = new Set(prev);
      if (newSet.has(type)) {
        newSet.delete(type);
      } else {
        newSet.add(type);
      }
      return newSet;
    });
  }, []);

  const createConfig = useCallback(async (config: Partial<ErpConfig>) => {
    try {
      const res = await erpApi.createConfig(config);
      if (res.code === 200) {
        toast.success('配置创建成功');
        await loadConfigs();
      }
    } catch {
      toast.error('配置创建失败');
    }
  }, [erpApi, loadConfigs]);

  const updateConfig = useCallback(async (id: number, config: Partial<ErpConfig>) => {
    try {
      const res = await erpApi.updateConfig(id, config);
      if (res.code === 200) {
        toast.success('配置更新成功');
        await loadConfigs();
      }
    } catch {
      toast.error('配置更新失败');
    }
  }, [erpApi, loadConfigs]);

  const deleteConfig = useCallback(async (configId: number) => {
    try {
      // Check for active syncs first (if API supports it)
      try {
        const activeSyncsRes = await (erpApi as any).getActiveSyncs?.();
        if (activeSyncsRes?.code === 200) {
          const hasActiveSync = activeSyncsRes.data.some(
            (s: any) => s.configId === configId && s.status === 'running'
          );
          if (hasActiveSync) {
            toast.error('该连接器有同步任务正在进行,无法删除');
            return;
          }
        }
      } catch {
        // Ignore if getActiveSyncs doesn't exist or fails
      }

      const res = await erpApi.deleteConfig(configId);
      if (res.code === 200) {
        toast.success('已删除连接器');
        // Refresh the list
        loadConfigs();
      } else {
        toast.error(res.message || '删除失败');
      }
    } catch (error: any) {
      if (error.response?.status === 404) {
        toast.error('连接器不存在');
      } else if (error.response?.status === 403) {
        toast.error('没有权限删除此连接器');
      } else {
        toast.error('删除异常,请稍后重试');
      }
    }
  }, [erpApi, loadConfigs]);

  const testConnection = useCallback(async (id: number) => {
    try {
      const res = await erpApi.testConnection(id);
      if (res.code === 200) {
        toast.success('连接测试成功');
      } else {
        toast.error('连接测试失败');
      }
    } catch {
      toast.error('连接测试失败');
    }
  }, [erpApi]);

  const state: ErpConfigManagerState = {
    configs,
    adapterTypes,
    expandedTypes,
    activeConfigId,
    loading,
  };

  // Use useMemo to stabilize actions reference and prevent infinite loops
  const actions: ErpConfigManagerActions = useMemo(
    () => ({
      loadConfigs,
      setActiveConfig: setActiveConfigId,
      toggleTypeExpansion,
      createConfig,
      updateConfig,
      deleteConfig,
      testConnection,
    }),
    [
      loadConfigs,
      toggleTypeExpansion,
      createConfig,
      updateConfig,
      deleteConfig,
      testConnection,
    ]
  );

  return { state, actions };
}
