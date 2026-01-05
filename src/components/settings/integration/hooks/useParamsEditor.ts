// Input: React hooks, ERP API
// Output: useParamsEditor hook (state + actions)
// Pos: src/components/settings/integration/hooks/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { ParamsEditorState, ParamsEditorActions } from '../types';

interface UseParamsEditorOptions {
  erpApi: {
    syncScenario: (scenarioId: number, params: any) => Promise<any>;
  };
  onSyncComplete?: () => void;
  setSyncing?: (scenarioId: number | null) => void;
}

const DEFAULT_FORM = {
  startDate: '',
  endDate: '',
  pageSize: 100,
};

export function useParamsEditor(options: UseParamsEditorOptions) {
  const { erpApi, onSyncComplete, setSyncing } = options;

  const [showFor, setShowFor] = useState<number | null>(null);
  const [pendingSyncId, setPendingSyncId] = useState<number | null>(null);
  const [form, setForm] = useState(DEFAULT_FORM);

  const openEditor = useCallback((scenarioId: number) => {
    setShowFor(scenarioId);
    setPendingSyncId(scenarioId);
    setForm(DEFAULT_FORM);
    // Set syncing state when opening editor to show "syncing" indicator
    setSyncing?.(scenarioId);
  }, [setSyncing]);

  const closeEditor = useCallback(() => {
    setShowFor(null);
    setPendingSyncId(null);
    // Reset syncing state when closing editor
    setSyncing?.(null);
  }, [setSyncing]);

  const updateForm = useCallback((field: string, value: any) => {
    setForm(prev => ({ ...prev, [field]: value }));
  }, []);

  const submitSync = useCallback(async () => {
    if (pendingSyncId === null) return;

    try {
      await erpApi.syncScenario(pendingSyncId, form);
      toast.success('同步已发起');
      closeEditor();
      onSyncComplete?.();
    } catch {
      toast.error('同步失败');
      closeEditor();
    }
  }, [erpApi, pendingSyncId, form, closeEditor, onSyncComplete]);

  const state: ParamsEditorState = {
    showFor,
    pendingSyncId,
    form,
  };

  // Use useMemo to stabilize actions reference and prevent infinite loops
  const actions: ParamsEditorActions = useMemo(
    () => ({
      openEditor,
      closeEditor,
      updateForm,
      submitSync,
    }),
    [openEditor, closeEditor, updateForm, submitSync]
  );

  return { state, actions };
}
