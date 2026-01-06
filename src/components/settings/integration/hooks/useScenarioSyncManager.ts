// Input: React hooks, ErpScenario types, ERP API
// Output: useScenarioSyncManager hook (state + actions)
// Pos: src/components/settings/integration/hooks/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { ErpScenario, ErpSubInterface, SyncHistory } from '../../../../types';
import { ScenarioSyncManagerState, ScenarioSyncManagerActions } from '../types';

interface UseScenarioSyncManagerOptions {
  erpApi: {
    getScenarios: (configId: number) => Promise<any>;
    getSubInterfaces: (scenarioId: number) => Promise<any>;
    getSyncHistory: (scenarioId: number) => Promise<any>;
    syncScenario: (scenarioId: number, params?: any) => Promise<any>;
    syncAllScenarios: (configId: number) => Promise<any>;
  };
}

export function useScenarioSyncManager(options: UseScenarioSyncManagerOptions) {
  const { erpApi } = options;

  const [scenarios, setScenarios] = useState<ErpScenario[]>([]);
  const [expandedScenarios, setExpandedScenarios] = useState<Set<number>>(new Set());
  const [subInterfaces, setSubInterfaces] = useState<Record<number, ErpSubInterface[]>>({});
  const [syncHistory, setSyncHistory] = useState<Record<number, SyncHistory[]>>({});
  const [showHistoryFor, setShowHistoryFor] = useState<number | null>(null);
  const [activeScenarioId, setActiveScenarioId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState<number | null>(null);

  const loadScenarios = useCallback(async (configId: number) => {
    setLoading(true);
    try {
      const res = await erpApi.getScenarios(configId);
      if (res.code === 200) {
        setScenarios(res.data || []);
      } else {
        setScenarios([]);
      }
    } catch {
      toast.error('加载业务场景失败');
      setScenarios([]);
    } finally {
      setLoading(false);
    }
  }, [erpApi]);

  const toggleScenarioExpansion = useCallback((id: number) => {
    setExpandedScenarios(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }, []);

  const loadSubInterfaces = useCallback(async (scenarioId: number) => {
    try {
      const res = await erpApi.getSubInterfaces(scenarioId);
      if (res.code === 200) {
        setSubInterfaces(prev => ({ ...prev, [scenarioId]: res.data || [] }));
      } else {
        setSubInterfaces(prev => ({ ...prev, [scenarioId]: [] }));
      }
    } catch {
      console.error('加载子接口失败');
      setSubInterfaces(prev => ({ ...prev, [scenarioId]: [] }));
    }
  }, [erpApi]);

  const loadSyncHistory = useCallback(async (scenarioId: number) => {
    try {
      const res = await erpApi.getSyncHistory(scenarioId);
      if (res.code === 200) {
        setSyncHistory(prev => ({ ...prev, [scenarioId]: res.data || [] }));
      } else {
        setSyncHistory(prev => ({ ...prev, [scenarioId]: [] }));
      }
    } catch {
      console.error('加载同步历史失败');
      setSyncHistory(prev => ({ ...prev, [scenarioId]: [] }));
    }
  }, [erpApi]);

  const toggleHistoryView = useCallback((scenarioId: number | null) => {
    setShowHistoryFor(scenarioId);
    if (scenarioId && !syncHistory[scenarioId]) {
      loadSyncHistory(scenarioId);
    }
  }, [syncHistory, loadSyncHistory]);

  const syncScenario = useCallback(async (scenarioId: number, params?: any) => {
    setSyncing(scenarioId);
    try {
      const res = await erpApi.syncScenario(scenarioId, params);
      if (res.code === 200) {
        toast.success('同步成功');
        await loadSyncHistory(scenarioId);
      } else {
        toast.error(res.message || '同步失败');
      }
    } catch {
      toast.error('同步失败');
    } finally {
      setSyncing(null);
    }
  }, [erpApi, loadSyncHistory]);

  const syncAllScenarios = useCallback(async (configId: number) => {
    try {
      const res = await erpApi.syncAllScenarios(configId);
      if (res.code === 200) {
        toast.success('批量同步成功');
        await loadScenarios(configId);
      }
    } catch {
      toast.error('批量同步失败');
    }
  }, [erpApi, loadScenarios]);

  const state: ScenarioSyncManagerState = {
    scenarios,
    expandedScenarios,
    subInterfaces,
    syncHistory,
    showHistoryFor,
    activeScenarioId,
    loading,
    syncing,
  };

  // Use useMemo to stabilize actions reference and prevent infinite loops
  const actions: ScenarioSyncManagerActions = useMemo(
    () => ({
      loadScenarios,
      toggleScenarioExpansion,
      loadSubInterfaces,
      loadSyncHistory,
      toggleHistoryView,
      syncScenario,
      syncAllScenarios,
      setSyncing,
    }),
    [
      loadScenarios,
      toggleScenarioExpansion,
      loadSubInterfaces,
      loadSyncHistory,
      toggleHistoryView,
      syncScenario,
      syncAllScenarios,
      setSyncing,
    ]
  );

  return { state, actions };
}
