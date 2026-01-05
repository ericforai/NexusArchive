// Input: React hooks, ERP API
// Output: useMonitoring Hook
// Pos: 集成监控数据管理 Hook

import { useState, useCallback } from 'react';
import { erpApi } from '../api/erp';
import type { IntegrationMonitoring } from '../types';

export interface MonitoringState {
  data: IntegrationMonitoring | null;
  loading: boolean;
}

export interface MonitoringActions {
  loadMonitoring: () => Promise<void>;
}

export interface UseMonitoringReturn {
  state: MonitoringState;
  actions: MonitoringActions;
}

/**
 * 集成监控数据管理 Hook
 * <p>
 * 封装集成监控数据的加载逻辑和状态管理
 * </p>
 */
export function useMonitoring(): UseMonitoringReturn {
  const [state, setState] = useState<MonitoringState>({
    data: null,
    loading: false,
  });

  const loadMonitoring = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true }));

    try {
      const response = await erpApi.getIntegrationMonitoring();

      if (response.success && response.data) {
        setState({ data: response.data, loading: false });
      } else {
        setState({ data: null, loading: false });
      }
    } catch (error) {
      console.error('Failed to load monitoring data:', error);
      setState({ data: null, loading: false });
    }
  }, []);

  return {
    state,
    actions: { loadMonitoring },
  };
}

export default useMonitoring;
