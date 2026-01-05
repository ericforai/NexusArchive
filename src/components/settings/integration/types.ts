// Input: None (base type definitions)
// Output: Type definitions for integration module
// Pos: src/components/settings/integration//
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
  setSyncing: (scenarioId: number | null) => void;
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
