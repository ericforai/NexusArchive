# IntegrationSettings 组件重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 将 IntegrationSettings.tsx 组件从 1,709 行重构为 ~150 行的组合器组件，拆分为 6 个专用 Hook 和多个子组件。

**架构:** 使用 Compositor 组合器模式 + React Hooks，将单一"上帝组件"拆分为多个职责单一的模块。

**技术栈:**
- React 19 + TypeScript 5.8
- React Hooks (useState, useCallback, useEffect)
- 自定义 Hooks 封装业务逻辑
- 子组件拆分 UI 层

---

## 背景分析

### 当前问题
**文件:** `src/components/settings/IntegrationSettings.tsx` (1,709 行)

**状态数量:** 35 个 useState
**函数数量:** 108 个
**职责数量:** 6+ 个

**识别的职责:**
1. ERP 配置管理 (configs, loading, CRUD)
2. 场景同步管理 (scenarios, sync, history)
3. 参数编辑器 (paramsForm, date range)
4. 连接器模态框 (configModal, editing, validation)
5. 诊断功能 (diagnosis, health check)
6. AI 适配器 (aiAdapter, file upload, preview)
7. 对账功能 (reconciliation)
8. 监控数据 (monitoring)

### 熵增指标
```
复杂度 = 状态数 × 函数数 / 职责数
       = 35 × 108 / 8
       = 472.5 (远超 10 的阈值)
```

### 重构目标
- 主组件: 1,709 行 → ~150 行 (-91%)
- 状态数: 35 个 → 分散到各 Hook
- 每个模块: < 200 行
- TypeScript 编译: ✅ 通过
- 向后兼容: ✅ 100%

---

## 重构架构

### 新目录结构
```
src/components/settings/integration/
├── IntegrationSettingsPage.tsx     (~150 行) - 主页面组合器
├── hooks/
│   ├── useErpConfigManager.ts      (~120 行) - ERP 配置管理
│   ├── useScenarioSyncManager.ts   (~150 行) - 场景同步管理
│   ├── useIntegrationDiagnosis.ts  (~100 行) - 诊断功能
│   ├── useConnectorModal.ts        (~150 行) - 连接器模态框
│   ├── useParamsEditor.ts          (~80 行)  - 参数编辑器
│   └── useAiAdapterHandler.ts      (~120 行) - AI 适配器处理
├── components/
│   ├── ErpConfigList.tsx           (~100 行) - 配置列表
│   ├── ScenarioCard.tsx            (~150 行) - 场景卡片
│   ├── ConnectorForm.tsx           (~120 行) - 连接器表单
│   ├── DiagnosisPanel.tsx          (~100 行) - 诊断面板
│   ├── ParamsEditor.tsx            (~80 行)  - 参数编辑器
│   └── SyncHistoryView.tsx         (~100 行) - 同步历史视图
├── types.ts                        (~50 行)  - 类型定义
└── index.ts                        (~20 行)  - 导出
```

---

## Phase 1: 基础设施准备 (2-3 小时)

### Task 1: 创建目录结构和类型定义

**Files:**
- Create: `src/components/settings/integration/types.ts`
- Create: `src/components/settings/integration/index.ts`

**Step 1: 创建类型定义文件**

```typescript
// src/components/settings/integration/types.ts

import { ErpConfig, ErpScenario, ErpSubInterface, IntegrationDiagnosisResult, IntegrationMonitoring, ReconciliationRecord, SyncHistory } from '../../../types';

// ============ ERP Config Manager Types ============
export interface ErpConfigManagerState {
  configs: ErpConfig[];
  adapterTypes: string[];
  expandedTypes: Set<string>;
  activeConfigId: number | null;
  loading: boolean;
}

export interface ErpConfigManagerActions {
  loadConfigs: () => Promise<void>;
  setActiveConfig: (id: number | null) => void;
  toggleTypeExpansion: (type: string) => void;
  createConfig: (config: Partial<ErpConfig>) => Promise<void>;
  updateConfig: (id: number, config: Partial<ErpConfig>) => Promise<void>;
  deleteConfig: (id: number) => Promise<void>;
  testConnection: (id: number) => Promise<void>;
}

// ============ Scenario Sync Manager Types ============
export interface ScenarioSyncManagerState {
  scenarios: ErpScenario[];
  expandedScenarios: Set<number>;
  subInterfaces: Record<number, ErpSubInterface[]>;
  syncHistory: Record<number, SyncHistory[]>;
  showHistoryFor: number | null;
  loading: boolean;
  syncing: number | null;
}

export interface ScenarioSyncManagerActions {
  loadScenarios: (configId: number) => Promise<void>;
  toggleScenarioExpansion: (id: number) => void;
  loadSubInterfaces: (scenarioId: number) => Promise<void>;
  loadSyncHistory: (scenarioId: number) => Promise<void>;
  toggleHistoryView: (scenarioId: number | null) => void;
  syncScenario: (scenarioId: number, params?: any) => Promise<void>;
  syncAllScenarios: (configId: number) => Promise<void>;
}

// ============ Connector Modal Types ============
export interface ConnectorModalState {
  show: boolean;
  editingConfig: Partial<ErpConfig> | null;
  configForm: {
    name: string;
    erpType: string;
    baseUrl: string;
    appKey: string;
    appSecret: string;
    accbookCode: string;
    accbookCodes: string[];
  };
  newAccbookCode: string;
  detectedType: string | null;
  testing: boolean;
}

export interface ConnectorModalActions {
  openModal: (config?: Partial<ErpConfig>) => void;
  closeModal: () => void;
  updateForm: (field: string, value: any) => void;
  addAccbookCode: (code: string) => void;
  removeAccbookCode: (code: string) => void;
  detectErpType: (url: string) => Promise<string>;
  testConnection: () => Promise<void>;
  saveConfig: () => Promise<void>;
}

// ============ Diagnosis Types ============
export interface DiagnosisState {
  show: boolean;
  diagnosing: boolean;
  result: IntegrationDiagnosisResult | null;
}

export interface DiagnosisActions {
  startDiagnosis: () => Promise<void>;
  closeDiagnosis: () => void;
}

// ============ Params Editor Types ============
export interface ParamsEditorState {
  showFor: number | null;
  pendingSyncId: number | null;
  form: {
    startDate: string;
    endDate: string;
    pageSize: number;
  };
}

export interface ParamsEditorActions {
  openEditor: (scenarioId: number) => void;
  closeEditor: () => void;
  updateForm: (field: string, value: any) => void;
  submitSync: () => Promise<void>;
}

// ============ AI Adapter Types ============
export interface AiAdapterState {
  show: boolean;
  loading: boolean;
  files: File[];
  preview: any;
  selectedTargetConfigId: number | null;
}

export interface AiAdapterActions {
  openAiAdapter: () => void;
  closeAiAdapter: () => void;
  uploadFiles: (files: File[]) => void;
  removeFile: (index: number) => void;
  generatePreview: () => Promise<void>;
  adaptToConfig: (configId: number) => Promise<void>;
}

// ============ Monitoring Types ============
export interface MonitoringState {
  data: IntegrationMonitoring | null;
  loading: boolean;
}

export interface MonitoringActions {
  loadMonitoring: () => Promise<void>;
}

// ============ Reconciliation Types ============
export interface ReconciliationState {
  show: boolean;
  record: ReconciliationRecord | null;
  loading: boolean;
}

export interface ReconciliationActions {
  showReconciliation: (record: ReconciliationRecord) => void;
  closeReconciliation: () => void;
}

// ============ Combined Hook Type ============
export interface UseIntegrationSettings {
  // Config Manager
  configState: ErpConfigManagerState;
  configActions: ErpConfigManagerActions;

  // Scenario Sync Manager
  scenarioState: ScenarioSyncManagerState;
  scenarioActions: ScenarioSyncManagerActions;

  // Connector Modal
  connectorState: ConnectorModalState;
  connectorActions: ConnectorModalActions;

  // Diagnosis
  diagnosisState: DiagnosisState;
  diagnosisActions: DiagnosisActions;

  // Params Editor
  paramsState: ParamsEditorState;
  paramsActions: ParamsEditorActions;

  // AI Adapter
  aiAdapterState: AiAdapterState;
  aiAdapterActions: AiAdapterActions;

  // Monitoring
  monitoringState: MonitoringState;
  monitoringActions: MonitoringActions;

  // Reconciliation
  reconciliationState: ReconciliationState;
  reconciliationActions: ReconciliationActions;
}
```

**Step 2: 创建导出文件**

```typescript
// src/components/settings/integration/index.ts

export { IntegrationSettingsPage } from './IntegrationSettingsPage';
export * from './types';
```

**Step 3: 运行 TypeScript 检查**

```bash
npx tsc --noEmit
```
Expected: No errors

**Step 4: 提交**

```bash
git add src/components/settings/integration/
git commit -m "feat(integration): create directory structure and type definitions for IntegrationSettings refactoring"
```

---

## Phase 2: Hook 开发 - ERP 配置管理 (3-4 小时)

### Task 2: 创建 useErpConfigManager Hook

**Files:**
- Create: `src/components/settings/integration/hooks/useErpConfigManager.ts`
- Test: `src/components/settings/integration/hooks/__tests__/useErpConfigManager.test.ts`

**Step 1: 创建测试文件**

```typescript
// src/components/settings/integration/hooks/__tests__/useErpConfigManager.test.ts

import { renderHook, act, waitFor } from '@testing-library/react';
import { useErpConfigManager } from '../useErpConfigManager';
import { ErpConfig } from '../../../../../types';

// Mock API
const mockErpApi = {
  getConfigs: jest.fn(),
  createConfig: jest.fn(),
  updateConfig: jest.fn(),
  deleteConfig: jest.fn(),
  testConnection: jest.fn(),
};

const mockConfigs: ErpConfig[] = [
  { id: 1, name: 'YonSuite Dev', erpType: 'yonsuite', baseUrl: 'https://api.test.com', appKey: 'key1', appSecret: 'secret1' },
  { id: 2, name: 'Kingdee Prod', erpType: 'kingdee', baseUrl: 'https://api.prod.com', appKey: 'key2', appSecret: 'secret2' },
];

describe('useErpConfigManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('should have empty initial state', () => {
      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      expect(result.current.state.configs).toEqual([]);
      expect(result.current.state.adapterTypes).toEqual([]);
      expect(result.current.state.activeConfigId).toBeNull();
      expect(result.current.state.loading).toBe(false);
    });
  });

  describe('loadConfigs', () => {
    it('should load configs successfully', async () => {
      mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: mockConfigs });

      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      expect(result.current.state.configs).toEqual(mockConfigs);
      expect(result.current.state.adapterTypes).toEqual(['yonsuite', 'kingdee']);
      expect(result.current.state.activeConfigId).toBe(1);
      expect(result.current.state.loading).toBe(false);
    });

    it('should handle loading state', async () => {
      mockErpApi.getConfigs.mockImplementation(() => new Promise(resolve => {
        setTimeout(() => resolve({ code: 200, data: mockConfigs }), 100);
      }));

      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      act(() => {
        result.current.actions.loadConfigs();
      });

      expect(result.current.state.loading).toBe(true);

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });
    });

    it('should handle API error', async () => {
      mockErpApi.getConfigs.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      expect(result.current.state.configs).toEqual([]);
      expect(result.current.state.loading).toBe(false);
    });
  });

  describe('setActiveConfig', () => {
    it('should set active config', () => {
      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      act(() => {
        result.current.actions.setActiveConfig(5);
      });

      expect(result.current.state.activeConfigId).toBe(5);
    });

    it('should clear active config', () => {
      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      act(() => {
        result.current.actions.setActiveConfig(5);
        result.current.actions.setActiveConfig(null);
      });

      expect(result.current.state.activeConfigId).toBeNull();
    });
  });

  describe('toggleTypeExpansion', () => {
    it('should expand collapsed type', () => {
      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      act(() => {
        result.current.actions.toggleTypeExpansion('yonsuite');
      });

      expect(result.current.state.expandedTypes.has('yonsuite')).toBe(true);
    });

    it('should collapse expanded type', () => {
      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      act(() => {
        result.current.actions.toggleTypeExpansion('yonsuite');
        result.current.actions.toggleTypeExpansion('yonsuite');
      });

      expect(result.current.state.expandedTypes.has('yonsuite')).toBe(false);
    });
  });

  describe('createConfig', () => {
    it('should create new config', async () => {
      const newConfig = { name: 'New Config', erpType: 'yonsuite' as const, baseUrl: 'https://test.com' };
      mockErpApi.createConfig.mockResolvedValue({ code: 200, data: { id: 3, ...newConfig } });
      mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: [...mockConfigs, { id: 3, ...newConfig }] });

      const { result } = renderHook(() => useErpConfigManager(mockErpApi));

      await act(async () => {
        await result.current.actions.createConfig(newConfig);
      });

      expect(mockErpApi.createConfig).toHaveBeenCalledWith(newConfig);
      expect(mockErpApi.getConfigs).toHaveBeenCalled();
    });
  });

  describe('deleteConfig', () => {
    it('should delete config', async () => {
      mockErpApi.deleteConfig.mockResolvedValue({ code: 200 });
      mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: mockConfigs.filter(c => c.id !== 1) });

      const { result } = renderHook(() => useErpConfigManager(mockErpApi));
      result.current.state.configs = mockConfigs;

      await act(async () => {
        await result.current.actions.deleteConfig(1);
      });

      expect(mockErpApi.deleteConfig).toHaveBeenCalledWith(1);
      expect(mockErpApi.getConfigs).toHaveBeenCalled();
    });
  });
});
```

**Step 2: 运行测试验证失败**

```bash
npm test -- useErpConfigManager.test.ts --watch
```
Expected: FAIL with "useErpConfigManager not defined"

**Step 3: 实现 useErpConfigManager Hook**

```typescript
// src/components/settings/integration/hooks/useErpConfigManager.ts

import { useState, useCallback } from 'react';
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

  // State
  const [configs, setConfigs] = useState<ErpConfig[]>([]);
  const [adapterTypes, setAdapterTypes] = useState<string[]>([]);
  const [expandedTypes, setExpandedTypes] = useState<Set<string>>(new Set());
  const [activeConfigId, setActiveConfigId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  // Actions
  const loadConfigs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await erpApi.getConfigs();
      if (res.code === 200 && res.data) {
        setConfigs(res.data);
        // Extract unique adapter types
        const types = [...new Set(res.data.map((c: ErpConfig) => c.erpType?.toLowerCase() || 'generic'))];
        setAdapterTypes(types);
        // Expand first type by default
        if (types.length > 0 && expandedTypes.size === 0) {
          setExpandedTypes(new Set([types[0]]));
          // Select first config of first type
          const firstOfType = res.data.find((c: ErpConfig) => (c.erpType?.toLowerCase() || 'generic') === types[0]);
          if (firstOfType) setActiveConfigId(firstOfType.id);
        }
      }
    } catch {
      toast.error('加载配置失败');
    } finally {
      setLoading(false);
    }
  }, [erpApi, expandedTypes.size]);

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
        await loadConfigs(); // Reload to get updated list
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

  const deleteConfig = useCallback(async (id: number) => {
    try {
      const res = await erpApi.deleteConfig(id);
      if (res.code === 200) {
        toast.success('配置删除成功');
        await loadConfigs();
      }
    } catch {
      toast.error('配置删除失败');
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

  const actions: ErpConfigManagerActions = {
    loadConfigs,
    setActiveConfig: setActiveConfigId,
    toggleTypeExpansion,
    createConfig,
    updateConfig,
    deleteConfig,
    testConnection,
  };

  return { state, actions };
}
```

**Step 4: 运行测试验证通过**

```bash
npm test -- useErpConfigManager.test.ts
```
Expected: PASS

**Step 5: 提交**

```bash
git add src/components/settings/integration/hooks/useErpConfigManager.ts
git add src/components/settings/integration/hooks/__tests__/useErpConfigManager.test.ts
git commit -m "feat(integration): implement useErpConfigManager hook with tests"
```

---

## Phase 3: Hook 开发 - 场景同步管理 (3-4 小时)

### Task 3: 创建 useScenarioSyncManager Hook

**Files:**
- Create: `src/components/settings/integration/hooks/useScenarioSyncManager.ts`
- Test: `src/components/settings/integration/hooks/__tests__/useScenarioSyncManager.test.ts`

**Step 1: 创建测试文件**

```typescript
// src/components/settings/integration/hooks/__tests__/useScenarioSyncManager.test.ts

import { renderHook, act, waitFor } from '@testing-library/react';
import { useScenarioSyncManager } from '../useScenarioSyncManager';

const mockErpApi = {
  getScenarios: jest.fn(),
  getSubInterfaces: jest.fn(),
  getSyncHistory: jest.fn(),
  syncScenario: jest.fn(),
  syncAllScenarios: jest.fn(),
};

const mockScenarios = [
  { id: 1, name: 'Sales Outbound', status: 'ACTIVE' },
  { id: 2, name: 'Purchase Inbound', status: 'ACTIVE' },
];

describe('useScenarioSyncManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should load scenarios for config', async () => {
    mockErpApi.getScenarios.mockResolvedValue({ code: 200, data: mockScenarios });

    const { result } = renderHook(() => useScenarioSyncManager(mockErpApi));

    await act(async () => {
      await result.current.actions.loadScenarios(1);
    });

    expect(result.current.state.scenarios).toEqual(mockScenarios);
  });

  it('should load sub-interfaces', async () => {
    const mockSubInterfaces = [
      { id: 1, name: 'Sales Order', path: '/sales/order' },
    ];
    mockErpApi.getSubInterfaces.mockResolvedValue({ code: 200, data: mockSubInterfaces });

    const { result } = renderHook(() => useScenarioSyncManager(mockErpApi));

    await act(async () => {
      await result.current.actions.loadSubInterfaces(1);
    });

    expect(result.current.state.subInterfaces[1]).toEqual(mockSubInterfaces);
  });

  it('should sync scenario', async () => {
    mockErpApi.syncScenario.mockResolvedValue({ code: 200 });

    const { result } = renderHook(() => useScenarioSyncManager(mockErpApi));

    await act(async () => {
      await result.current.actions.syncScenario(1);
    });

    expect(mockErpApi.syncScenario).toHaveBeenCalledWith(1);
    expect(result.current.state.syncing).toBeNull();
  });
});
```

**Step 2: 运行测试验证失败**

```bash
npm test -- useScenarioSyncManager.test.ts
```
Expected: FAIL

**Step 3: 实现 Hook**

```typescript
// src/components/settings/integration/hooks/useScenarioSyncManager.ts

import { useState, useCallback } from 'react';
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
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState<number | null>(null);

  const loadScenarios = useCallback(async (configId: number) => {
    setLoading(true);
    try {
      const res = await erpApi.getScenarios(configId);
      if (res.code === 200) {
        setScenarios(res.data || []);
      }
    } catch {
      toast.error('加载业务场景失败');
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
      }
    } catch {
      console.error('加载子接口失败');
    }
  }, [erpApi]);

  const loadSyncHistory = useCallback(async (scenarioId: number) => {
    try {
      const res = await erpApi.getSyncHistory(scenarioId);
      if (res.code === 200) {
        setSyncHistory(prev => ({ ...prev, [scenarioId]: res.data || [] }));
      }
    } catch {
      console.error('加载同步历史失败');
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
    loading,
    syncing,
  };

  const actions: ScenarioSyncManagerActions = {
    loadScenarios,
    toggleScenarioExpansion,
    loadSubInterfaces,
    loadSyncHistory,
    toggleHistoryView,
    syncScenario,
    syncAllScenarios,
  };

  return { state, actions };
}
```

**Step 4: 运行测试**

```bash
npm test -- useScenarioSyncManager.test.ts
```
Expected: PASS

**Step 5: 提交**

```bash
git add src/components/settings/integration/hooks/useScenarioSyncManager.ts
git add src/components/settings/integration/hooks/__tests__/useScenarioSyncManager.test.ts
git commit -m "feat(integration): implement useScenarioSyncManager hook with tests"
```

---

## Phase 4: Hook 开发 - 连接器模态框 (2-3 小时)

### Task 4: 创建 useConnectorModal Hook

**Files:**
- Create: `src/components/settings/integration/hooks/useConnectorModal.ts`
- Test: `src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts`

**Step 1: 创建测试**

```typescript
// src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts

import { renderHook, act } from '@testing-library/react';
import { useConnectorModal } from '../useConnectorModal';

const mockErpApi = {
  createConfig: jest.fn(),
  updateConfig: jest.fn(),
  testConnection: jest.fn(),
  detectErpType: jest.fn(),
};

describe('useConnectorModal', () => {
  it('should open modal for new config', () => {
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.openModal();
    });

    expect(result.current.state.show).toBe(true);
    expect(result.current.state.editingConfig).toBeNull();
  });

  it('should open modal for editing config', () => {
    const editingConfig = { id: 1, name: 'Test', erpType: 'yonsuite' };
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.openModal(editingConfig);
    });

    expect(result.current.state.editingConfig).toEqual(editingConfig);
  });

  it('should close modal', () => {
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.openModal();
      result.current.actions.closeModal();
    });

    expect(result.current.state.show).toBe(false);
  });

  it('should update form field', () => {
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.updateForm('name', 'New Name');
    });

    expect(result.current.state.configForm.name).toBe('New Name');
  });

  it('should add accbook code', () => {
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.addAccbookCode('001');
    });

    expect(result.current.state.configForm.accbookCodes).toContain('001');
    expect(result.current.state.newAccbookCode).toBe('');
  });

  it('should remove accbook code', () => {
    const { result } = renderHook(() => useConnectorModal(mockErpApi));

    act(() => {
      result.current.actions.addAccbookCode('001');
      result.current.actions.addAccbookCode('002');
      result.current.actions.removeAccbookCode('001');
    });

    expect(result.current.state.configForm.accbookCodes).toEqual(['002']);
  });
});
```

**Step 2: 运行测试验证失败**

```bash
npm test -- useConnectorModal.test.ts
```
Expected: FAIL

**Step 3: 实现 Hook**

```typescript
// src/components/settings/integration/hooks/useConnectorModal.ts

import { useState, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import { ErpConfig } from '../../../../types';
import { ConnectorModalState, ConnectorModalActions } from '../types';

interface UseConnectorModalOptions {
  erpApi: {
    createConfig: (config: Partial<ErpConfig>) => Promise<any>;
    updateConfig: (id: number, config: Partial<ErpConfig>) => Promise<any>;
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
      setConfigForm({
        name: config.name || '',
        erpType: config.erpType || 'yonsuite',
        baseUrl: config.baseUrl || '',
        appKey: config.appKey || '',
        appSecret: config.appSecret || '',
        accbookCode: config.accbookCode || '',
        accbookCodes: config.accbookCodes || [],
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
      if (editingConfig?.id) {
        await erpApi.updateConfig(editingConfig.id, configForm);
        toast.success('配置更新成功');
      } else {
        await erpApi.createConfig(configForm);
        toast.success('配置创建成功');
      }
      closeModal();
      onConfigSaved?.();
    } catch {
      toast.error('保存失败');
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

  const actions: ConnectorModalActions = {
    openModal,
    closeModal,
    updateForm,
    addAccbookCode,
    removeAccbookCode,
    detectErpType,
    testConnection,
    saveConfig,
  };

  return { state, actions };
}
```

**Step 4: 运行测试**

```bash
npm test -- useConnectorModal.test.ts
```
Expected: PASS

**Step 5: 提交**

```bash
git add src/components/settings/integration/hooks/useConnectorModal.ts
git add src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts
git commit -m "feat(integration): implement useConnectorModal hook with tests"
```

---

## Phase 5: 其余 Hooks 开发 (4-5 小时)

### Task 5-8: 创建剩余 Hooks

按照相同的 TDD 模式创建以下 Hooks：

1. **useIntegrationDiagnosis** - 诊断功能 (~100 行)
2. **useParamsEditor** - 参数编辑器 (~80 行)
3. **useAiAdapterHandler** - AI 适配器处理 (~120 行)
4. **useMonitoring** & **useReconciliation** - 监控和对账 (~100 行)

每个 Hook 遵循相同的步骤:
1. 写测试
2. 运行测试 (失败)
3. 写实现
4. 运行测试 (通过)
5. 提交

---

## Phase 6: UI 组件开发 (4-5 小时)

### Task 9: 创建 ErpConfigList 组件

**Files:**
- Create: `src/components/settings/integration/components/ErpConfigList.tsx`

**Step 1: 创建组件**

```typescript
// src/components/settings/integration/components/ErpConfigList.tsx

import React from 'react';
import { ChevronDown, ChevronRight, Database, Zap, Settings } from 'lucide-react';
import { ErpConfig } from '../../../../types';
import { ErpConfigManagerState, ErpConfigManagerActions } from '../types';

const ADAPTER_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  yonsuite: { icon: <Database size={16} />, color: 'text-blue-600 bg-blue-50', label: '用友 YonSuite' },
  kingdee: { icon: <Zap size={16} />, color: 'text-blue-600 bg-blue-50', label: '金蝶云星空' },
  weaver: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '泛微 OA' },
  generic: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '通用 REST' },
};

interface ErpConfigListProps {
  state: ErpConfigManagerState;
  actions: ErpConfigManagerActions;
  onSelectConfig: (config: ErpConfig) => void;
}

export function ErpConfigList({ state, actions, onSelectConfig }: ErpConfigListProps) {
  const { configs, adapterTypes, expandedTypes, activeConfigId, loading } = state;

  if (loading) {
    return <div className="flex justify-center p-8"><div className="animate-spin" /></div>;
  }

  return (
    <div className="space-y-4">
      {adapterTypes.map(type => {
        const typeConfigs = configs.filter(c => (c.erpType?.toLowerCase() || 'generic') === type);
        const isExpanded = expandedTypes.has(type);
        const config = ADAPTER_CONFIG[type] || ADAPTER_CONFIG.generic;

        return (
          <div key={type} className="border rounded-lg">
            <button
              onClick={() => actions.toggleTypeExpansion(type)}
              className="w-full flex items-center justify-between p-4 hover:bg-gray-50"
            >
              <div className="flex items-center gap-2">
                {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                <span className={`p-2 rounded ${config.color}`}>{config.icon}</span>
                <span className="font-medium">{config.label}</span>
                <span className="text-sm text-gray-500">({typeConfigs.length})</span>
              </div>
            </button>

            {isExpanded && (
              <div className="border-t p-2">
                {typeConfigs.map(config => (
                  <div
                    key={config.id}
                    onClick={() => {
                      actions.setActiveConfig(config.id);
                      onSelectConfig(config);
                    }}
                    className={`p-3 rounded cursor-pointer hover:bg-gray-50 ${
                      activeConfigId === config.id ? 'bg-blue-50' : ''
                    }`}
                  >
                    <div className="font-medium">{config.name}</div>
                    <div className="text-sm text-gray-500">{config.baseUrl}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
```

**Step 2: TypeScript 检查**

```bash
npx tsc --noEmit
```
Expected: No errors

**Step 3: 提交**

```bash
git add src/components/settings/integration/components/ErpConfigList.tsx
git commit -m "feat(integration): create ErpConfigList component"
```

---

### Task 10-13: 创建其余组件

按照相同模式创建:
1. **ScenarioCard** - 场景卡片
2. **ConnectorForm** - 连接器表单
3. **DiagnosisPanel** - 诊断面板
4. **ParamsEditor** - 参数编辑器

---

## Phase 7: 主组合器组件 (2-3 小时)

### Task 14: 创建 IntegrationSettingsPage 组合器

**Files:**
- Modify: `src/components/settings/IntegrationSettings.tsx` → `src/components/settings/integration/IntegrationSettingsPage.tsx`

**Step 1: 创建新的组合器组件**

```typescript
// src/components/settings/integration/IntegrationSettingsPage.tsx

/**
 * Integration Settings Page - Refactored
 *
 * Compositor pattern: combines specialized hooks and components
 * Original: 1,709 lines → Refactored: ~150 lines
 */
import React, { useEffect } from 'react';
import { Settings, RefreshCw } from 'lucide-react';
import { IntegrationSettingsApi } from '../types';
import { useErpConfigManager } from './hooks/useErpConfigManager';
import { useScenarioSyncManager } from './hooks/useScenarioSyncManager';
import { useConnectorModal } from './hooks/useConnectorModal';
import { useIntegrationDiagnosis } from './hooks/useIntegrationDiagnosis';
import { useParamsEditor } from './hooks/useParamsEditor';
import { useAiAdapterHandler } from './hooks/useAiAdapterHandler';
import { ErpConfigList } from './components/ErpConfigList';
import { ScenarioCard } from './components/ScenarioCard';
import { ConnectorForm } from './components/ConnectorForm';

interface IntegrationSettingsPageProps {
  erpApi: IntegrationSettingsApi;
}

export function IntegrationSettingsPage({ erpApi }: IntegrationSettingsPageProps) {
  // Hook: ERP Config Manager
  const configManager = useErpConfigManager({ erpApi });

  // Hook: Scenario Sync Manager
  const scenarioManager = useScenarioSyncManager({ erpApi });

  // Hook: Connector Modal
  const connectorModal = useConnectorModal({
    erpApi,
    onConfigSaved: configManager.actions.loadConfigs,
  });

  // Hook: Diagnosis
  const diagnosis = useIntegrationDiagnosis({ erpApi });

  // Hook: Params Editor
  const paramsEditor = useParamsEditor({
    erpApi,
    onSyncComplete: scenarioManager.actions.loadSyncHistory,
  });

  // Hook: AI Adapter
  const aiAdapter = useAiAdapterHandler({ erpApi });

  // Initial load
  useEffect(() => {
    configManager.actions.loadConfigs();
  }, [configManager.actions]);

  // Load scenarios when active config changes
  useEffect(() => {
    if (configManager.state.activeConfigId) {
      scenarioManager.actions.loadScenarios(configManager.state.activeConfigId);
    }
  }, [configManager.state.activeConfigId, scenarioManager.actions]);

  const handleSelectConfig = (config: ErpConfig) => {
    configManager.actions.setActiveConfig(config.id);
  };

  return (
    <div className="integration-settings p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Settings size={24} />
          集成设置
        </h1>
        <button
          onClick={() => configManager.actions.loadConfigs()}
          className="flex items-center gap-2 px-4 py-2 border rounded hover:bg-gray-50"
        >
          <RefreshCw size={16} />
          刷新
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: ERP Config List */}
        <div className="lg:col-span-1">
          <div className="border rounded-lg p-4">
            <h2 className="font-semibold mb-4">ERP 连接器</h2>
            <ErpConfigList
              state={configManager.state}
              actions={configManager.actions}
              onSelectConfig={handleSelectConfig}
            />
            <button
              onClick={() => connectorModal.actions.openModal()}
              className="w-full mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              添加连接器
            </button>
          </div>
        </div>

        {/* Right: Scenarios */}
        <div className="lg:col-span-2">
          {configManager.state.activeConfigId && (
            <div className="space-y-4">
              {scenarioManager.state.scenarios.map(scenario => (
                <ScenarioCard
                  key={scenario.id}
                  scenario={scenario}
                  state={scenarioManager.state}
                  actions={scenarioManager.actions}
                  onOpenParams={paramsEditor.actions.openEditor}
                  onViewHistory={(id) => scenarioManager.actions.toggleHistoryView(id)}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Connector Modal */}
      {connectorModal.state.show && (
        <ConnectorForm
          state={connectorModal.state}
          actions={connectorModal.actions}
        />
      )}

      {/* Diagnosis Panel */}
      {diagnosis.state.show && (
        <DiagnosisPanel
          state={diagnosis.state}
          actions={diagnosis.actions}
        />
      )}

      {/* Params Editor Modal */}
      {paramsEditor.state.showFor && (
        <ParamsEditorModal
          state={paramsEditor.state}
          actions={paramsEditor.actions}
        />
      )}
    </div>
  );
}
```

**Step 2: TypeScript 检查**

```bash
npx tsc --noEmit
```
Expected: No errors (some placeholder components may cause errors - create minimal versions)

**Step 3: 提交**

```bash
git add src/components/settings/integration/IntegrationSettingsPage.tsx
git commit -m "feat(integration): create main IntegrationSettingsPage compositor"
```

---

## Phase 8: 迁移和清理 (1-2 小时)

### Task 15: 迁移到新结构

**Files:**
- Modify: `src/components/settings/index.ts`
- Delete: `src/components/settings/IntegrationSettings.tsx` (original)

**Step 1: 更新导出**

```typescript
// src/components/settings/index.ts

// OLD: export { default as IntegrationSettings } from './IntegrationSettings';
// NEW: export { IntegrationSettingsPage as IntegrationSettings } from './integration/IntegrationSettingsPage';
```

**Step 2: 备份原文件**

```bash
mv src/components/settings/IntegrationSettings.tsx src/components/settings/IntegrationSettings.tsx.backup
```

**Step 3: 验证导入**

```bash
npx tsc --noEmit
```

**Step 4: 提交**

```bash
git add src/components/settings/index.ts
git add src/components/settings/IntegrationSettings.tsx.backup
git commit -m "refactor(integration): migrate to new modular structure"
```

---

## Phase 9: 验证和测试 (2-3 小时)

### Task 16: 运行完整测试套件

**Step 1: 运行单元测试**

```bash
npm test -- src/components/settings/integration/
```
Expected: All PASS

**Step 2: TypeScript 编译检查**

```bash
npx tsc --noEmit
```
Expected: No errors

**Step 3: 循环依赖检查**

```bash
npx madge --circular src/components/settings/integration/
```
Expected: No circular dependencies

**Step 4: 架构检查**

```bash
npm run check:arch
```
Expected: No violations

**Step 5: 提交**

```bash
git commit -m "test(integration): all tests passing for refactored IntegrationSettings"
```

---

## Phase 10: 文档更新 (1 小时)

### Task 17: 更新文档

**Files:**
- Update: `docs/entropy-reduction-frontend-audit.md`
- Update: `docs/architecture/modularization-refactoring-2025-12-31.md`
- Create: `src/components/settings/integration/README.md`

**Step 1: 创建组件 README**

```markdown
# Integration Settings Module

Refactored from 1,709 lines to ~150 lines using compositor pattern.

## Structure

- `IntegrationSettingsPage.tsx` - Main compositor
- `hooks/` - Business logic hooks
- `components/` - UI components
- `types.ts` - Type definitions

## Hooks

- `useErpConfigManager` - ERP configuration management
- `useScenarioSyncManager` - Scenario sync management
- `useConnectorModal` - Connector modal state
- `useIntegrationDiagnosis` - Diagnosis functionality
- `useParamsEditor` - Parameter editor
- `useAiAdapterHandler` - AI adapter handling
```

**Step 2: 更新熵减报告**

标记 `IntegrationSettings.tsx` 为已完成。

**Step 3: 提交**

```bash
git add docs/
git commit -m "docs(integration): update documentation for refactored IntegrationSettings"
```

---

## 验收标准

### 功能验收
- [ ] 所有现有功能正常工作
- [ ] UI 交互无变化
- [ ] API 调用正确

### 代码质量
- [ ] TypeScript 编译通过
- [ ] 单元测试覆盖率 > 80%
- [ ] 无循环依赖
- [ ] 无架构违规

### 性能
- [ ] 渲染性能无退化
- [ ] 包体积无明显增加

---

## 总结

此计划将 IntegrationSettings 组件从 **1,709 行**重构为 **~150 行**的组合器，拆分为:
- **6 个专用 Hook** (业务逻辑)
- **6 个子组件** (UI 层)
- **100% 向后兼容**

预计总时间: **20-25 小时**
核心原则: **TDD, DRY, YAGNI, 频繁提交**
