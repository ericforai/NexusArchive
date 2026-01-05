// Input: React hooks, IntegrationDiagnosisResult types, ERP API
// Output: useIntegrationDiagnosis hook (state + actions)
// Pos: src/components/settings/integration/hooks/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { DiagnosisState, DiagnosisActions } from '../types';

interface UseIntegrationDiagnosisOptions {
  erpApi: {
    runDiagnosis: () => Promise<any>;
  };
}

export function useIntegrationDiagnosis(options: UseIntegrationDiagnosisOptions) {
  const { erpApi } = options;

  const [show, setShow] = useState(false);
  const [diagnosing, setDiagnosing] = useState(false);
  const [result, setResult] = useState<DiagnosisState['result']>(null);

  const startDiagnosis = useCallback(async () => {
    setDiagnosing(true);
    try {
      const res = await erpApi.runDiagnosis();
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setShow(true);
        toast.success('诊断完成');
      } else {
        toast.error('诊断失败');
      }
    } catch {
      toast.error('诊断执行失败');
    } finally {
      setDiagnosing(false);
    }
  }, [erpApi]);

  const closeDiagnosis = useCallback(() => {
    setShow(false);
    setResult(null);
  }, []);

  const state: DiagnosisState = {
    show,
    diagnosing,
    result,
  };

  // Use useMemo to stabilize actions reference and prevent infinite loops
  const actions: DiagnosisActions = useMemo(
    () => ({
      startDiagnosis,
      closeDiagnosis,
    }),
    [startDiagnosis, closeDiagnosis]
  );

  return { state, actions };
}
