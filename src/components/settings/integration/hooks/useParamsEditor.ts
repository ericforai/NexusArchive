import { useState, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { ParamsEditorState, ParamsEditorActions } from '../types';

interface UseParamsEditorOptions {
  erpApi: {
    syncScenario: (scenarioId: number, params: any) => Promise<any>;
  };
  onSyncComplete?: () => void;
}

const DEFAULT_FORM = {
  startDate: '',
  endDate: '',
  pageSize: 100,
};

export function useParamsEditor(options: UseParamsEditorOptions) {
  const { erpApi, onSyncComplete } = options;

  const [showFor, setShowFor] = useState<number | null>(null);
  const [pendingSyncId, setPendingSyncId] = useState<number | null>(null);
  const [form, setForm] = useState(DEFAULT_FORM);

  const openEditor = useCallback((scenarioId: number) => {
    setShowFor(scenarioId);
    setPendingSyncId(scenarioId);
    setForm(DEFAULT_FORM);
  }, []);

  const closeEditor = useCallback(() => {
    setShowFor(null);
    setPendingSyncId(null);
  }, []);

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

  const actions: ParamsEditorActions = {
    openEditor,
    closeEditor,
    updateForm,
    submitSync,
  };

  return { state, actions };
}
