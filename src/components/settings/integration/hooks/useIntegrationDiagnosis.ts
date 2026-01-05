import { useState, useCallback } from 'react';
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

  const actions: DiagnosisActions = {
    startDiagnosis,
    closeDiagnosis,
  };

  return { state, actions };
}
