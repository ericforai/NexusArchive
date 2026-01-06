// Input: React hooks, ErpConfig types, ERP API
// Output: useConnectorModal hook (state + actions)
// Pos: src/components/settings/integration/hooks/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { ErpConfig } from '../../../../types';
import { ConnectorModalState, ConnectorModalActions } from '../types';

interface UseConnectorModalOptions {
  erpApi: {
    saveConfig: (config: Partial<ErpConfig>) => Promise<any>;
    testConnection: (id: number) => Promise<any>;
  };
  onConfigSaved?: () => void;
}

const ERP_TEMPLATES = [
  { name: 'YonSuite', pattern: /yonyoucloud|yonbip|yonsuite/i, type: 'yonsuite', defaultUrl: 'https://api.yonyoucloud.com/iuap-api-gateway' },
  { name: '金蝶云星空', pattern: /kingdee|k3cloud/i, type: 'kingdee', defaultUrl: '/k3cloud/' },
  { name: '泛微 OA (e-9)', pattern: /weaver|ecology/i, type: 'weaver', defaultUrl: '/weaver/' },
];

export function useConnectorModal(options: UseConnectorModalOptions) {
  const { erpApi, onConfigSaved } = options;

  const [show, setShow] = useState(false);
  const [editingConfig, setEditingConfig] = useState<Partial<ErpConfig> | null>(null);
  const [testing, setTesting] = useState(false);
  const [detectedType, setDetectedType] = useState<string | null>(null);
  const [newAccbookCode, setNewAccbookCode] = useState('');

  const [configForm, setConfigForm] = useState({
    name: '',
    erpType: 'yonsuite',
    baseUrl: '',
    appKey: '',
    appSecret: '',
    accbookCode: '',
    accbookCodes: [] as string[],
  });

  const openModal = useCallback((config?: Partial<ErpConfig>) => {
    if (config) {
      setEditingConfig(config);

      // Parse configJson to get the actual configuration fields
      let configData = {};
      try {
        if (config.configJson) {
          configData = JSON.parse(config.configJson);
        }
      } catch (error) {
        console.error('Failed to parse configJson:', error);
      }

      setConfigForm({
        name: config.name || '',
        erpType: config.erpType || 'yonsuite',
        baseUrl: (configData as any).baseUrl || '',
        appKey: (configData as any).appKey || '',
        appSecret: (configData as any).appSecret || '',
        accbookCode: (configData as any).accbookCode || '',
        accbookCodes: (configData as any).accbookCodes || [],
      });
    } else {
      setEditingConfig(null);
      setConfigForm({
        name: '',
        erpType: 'yonsuite',
        baseUrl: '',
        appKey: '',
        appSecret: '',
        accbookCode: '',
        accbookCodes: [],
      });
    }
    setShow(true);
    setDetectedType(null);
  }, []);

  const closeModal = useCallback(() => {
    setShow(false);
    setEditingConfig(null);
    setTesting(false);
  }, []);

  const updateForm = useCallback((field: string, value: any) => {
    setConfigForm(prev => ({ ...prev, [field]: value }));
  }, []);

  const addAccbookCode = useCallback((code: string) => {
    if (!code.trim()) return;
    setConfigForm(prev => ({
      ...prev,
      accbookCodes: [...prev.accbookCodes, code.trim()],
    }));
    setNewAccbookCode('');
  }, []);

  const removeAccbookCode = useCallback((code: string) => {
    setConfigForm(prev => ({
      ...prev,
      accbookCodes: prev.accbookCodes.filter(c => c !== code),
    }));
  }, []);

  const detectErpType = useCallback(async (url: string): Promise<string> => {
    const template = ERP_TEMPLATES.find(t => t.pattern.test(url));
    const detected = template?.type || 'generic';
    setDetectedType(detected);
    if (detected !== 'generic') {
      updateForm('erpType', detected);
    }
    return detected;
  }, [updateForm]);

  const testConnection = useCallback(async () => {
    setTesting(true);
    try {
      if (editingConfig?.id) {
        const res = await erpApi.testConnection(editingConfig.id);
        if (res.code === 200) {
          toast.success('连接测试成功');
        } else {
          toast.error('连接测试失败');
        }
      }
    } catch {
      toast.error('连接测试失败');
    } finally {
      setTesting(false);
    }
  }, [erpApi, editingConfig]);

  const saveConfig = useCallback(async () => {
    try {
      // Build the config object with configJson as a serialized JSON string
      const configToSave = {
        id: editingConfig?.id,
        name: configForm.name,
        erpType: configForm.erpType,
        configJson: JSON.stringify({
          baseUrl: configForm.baseUrl,
          appKey: configForm.appKey,
          appSecret: configForm.appSecret,
          accbookCode: configForm.accbookCode,
          accbookCodes: configForm.accbookCodes,
        }),
        isActive: 1,
      };

      const res = await erpApi.saveConfig(configToSave);
      if (res.code === 200) {
        toast.success(editingConfig?.id ? '配置更新成功' : '配置创建成功');
        closeModal();
        onConfigSaved?.();
      } else {
        toast.error(res.message || '保存失败');
      }
    } catch (e: any) {
      const errorMsg = e?.response?.data?.message || e?.message || '保存失败，请稍后重试';
      toast.error(errorMsg);
      console.error('Save config error:', e);
    }
  }, [erpApi, editingConfig, configForm, closeModal, onConfigSaved]);

  const state: ConnectorModalState = {
    show,
    editingConfig,
    configForm,
    newAccbookCode,
    detectedType,
    testing,
  };

  // Use useMemo to stabilize actions reference and prevent infinite loops
  const actions: ConnectorModalActions = useMemo(
    () => ({
      openModal,
      closeModal,
      updateForm,
      addAccbookCode,
      removeAccbookCode,
      detectErpType,
      testConnection,
      saveConfig,
    }),
    [
      openModal,
      closeModal,
      updateForm,
      addAccbookCode,
      removeAccbookCode,
      detectErpType,
      testConnection,
      saveConfig,
    ]
  );

  return { state, actions };
}
