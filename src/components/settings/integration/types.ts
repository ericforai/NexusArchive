// Input: None (base type definitions)
// Output: Type definitions for integration module
// Pos: src/components/settings/integration//
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/types.ts

import { ErpConfig, ErpScenario, ErpSubInterface, IntegrationDiagnosisResult, IntegrationMonitoring, ReconciliationRecord, SyncHistory } from '../../../types';
import { Cloud, Server, FileText, Settings } from 'lucide-react';

// ============ SAP Interface Types Constants ============
export type SapInterfaceType = 'ODATA' | 'RFC' | 'IDOC' | 'GATEWAY';

export type SapInterfaceStatus = 'implemented' | 'reserved' | 'planned' | 'deprecated';

export const SAP_INTERFACE_STATUS = {
  implemented: 'implemented',
  reserved: 'reserved',
  planned: 'planned',
  deprecated: 'deprecated',
} as const;

export const SAP_INTERFACE_TYPES = [
  {
    key: 'odata',
    name: 'OData 服务',
    description: '现代化 REST 风格集成，基于 HTTP/JSON',
    status: 'implemented' as const,
    icon: Cloud,
  },
  {
    key: 'rfc_bapi',
    name: 'RFC/BAPI',
    description: '传统 SAP 集成方式，需要 SAP Java Connector',
    status: 'reserved' as const,
    icon: Server,
  },
  {
    key: 'idoc',
    name: 'IDoc',
    description: '异步批量数据交换，类似 EDI 格式',
    status: 'reserved' as const,
    icon: FileText,
  },
  {
    key: 'gateway',
    name: 'SAP Gateway',
    description: '自定义 OData 服务构建',
    status: 'reserved' as const,
    icon: Settings,
  },
] as const;

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
  activeScenarioId: number | null;
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

export interface SapInterfaceConfig {
  // OData 配置
  odata?: {
    serverUrl: string;
    authType: 'Basic';
    username: string;
    password: string;
    clientNumber?: string; // on-premise only
    testService?: string;
  };
  // RFC/BAPI 配置（预留）
  rfc?: {
    ashost: string;
    sysnr: string;
    client: string;
    username: string;
    password: string;
    lang?: string;
  };
  // IDoc 配置（预留）
  idoc?: {
    host: string;
    port: number;
    partnerType: string;
  };
  // SAP Gateway 配置（预留）
  gateway?: {
    serviceUrl: string;
    apiKey: string;
  };
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
    accbookMapping: Record<string, string>; // { [accbookCode]: fondsCode } - 账套-全宗映射
    requireClosedPeriod?: boolean; // 关账检查模式：true=强制，false=提醒（仅YonSuite）
    sapInterfaceType?: SapInterfaceType; // SAP 接口类型
    sapConfig?: SapInterfaceConfig; // SAP 接口配置
  };
  newMappingEntry: {
    accbookCode: string;
    fondsCode: string;
  };
  detectedType: string | null;
  testing: boolean;
  sapInterfaceSelectorVisible: boolean; // SAP 接口类型选择器是否可见
}

export interface ConnectorModalActions {
  openModal: (config?: Partial<ErpConfig>) => void;
  closeModal: () => void;
  updateForm: (field: string, value: any) => void;
  updateNewMappingEntry: (field: 'accbookCode' | 'fondsCode', value: string) => void;
  addMappingEntry: (accbookCode: string, fondsCode: string) => void;
  removeMappingEntry: (accbookCode: string) => void;
  detectErpType: (url: string) => Promise<string>;
  testConnection: () => Promise<void>;
  saveConfig: () => Promise<void>;
  // SAP 接口类型相关
  selectSapInterfaceType: (type: SapInterfaceType) => void;
  updateSapConfig: (config: Partial<SapInterfaceConfig>) => void;
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
