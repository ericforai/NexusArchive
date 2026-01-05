// Input: React hooks
// Output: useReconciliation Hook
// Pos: 对账记录对话框管理 Hook

import { useState, useCallback } from 'react';
import type { ReconciliationRecord } from '../types';

export interface ReconciliationState {
  show: boolean;
  record: ReconciliationRecord | null;
  loading: boolean;
}

export interface ReconciliationActions {
  showReconciliation: (record: ReconciliationRecord) => void;
  closeReconciliation: () => void;
}

export interface UseReconciliationReturn {
  state: ReconciliationState;
  actions: ReconciliationActions;
}

/**
 * 对账记录对话框管理 Hook
 * <p>
 * 封装对账记录对话框的显示/隐藏逻辑和状态管理
 * </p>
 */
export function useReconciliation(): UseReconciliationReturn {
  const [state, setState] = useState<ReconciliationState>({
    show: false,
    record: null,
    loading: false,
  });

  const showReconciliation = useCallback((record: ReconciliationRecord) => {
    setState({
      show: true,
      record,
      loading: false,
    });
  }, []);

  const closeReconciliation = useCallback(() => {
    setState({
      show: false,
      record: null,
      loading: false,
    });
  }, []);

  return {
    state,
    actions: {
      showReconciliation,
      closeReconciliation,
    },
  };
}

export default useReconciliation;
