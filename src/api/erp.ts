import { client } from './client';
import { ApiResponse } from '../types';

export interface ErpConfig {
    id: number;
    name: string;
    erpType: string;
    configJson: string;
    isActive: number;
    status?: string;
    createdTime?: string;
    lastModifiedTime?: string;
}

export interface ErpScenario {
    id: number;
    configId: number;
    scenarioKey: string;
    name: string;
    description: string;
    isActive: boolean;
    syncStrategy: 'REALTIME' | 'CRON' | 'MANUAL';
    cronExpression?: string;
    lastSyncTime?: string;
    lastSyncStatus: string;
    lastSyncMsg?: string;
    paramsJson?: string;
}

export interface ErpSubInterface {
    id: number;
    scenarioId: number;
    interfaceKey: string;
    interfaceName: string;
    description?: string;
    isActive: boolean;
    sortOrder: number;
    configJson?: string;
}

export interface SyncHistory {
    id: number;
    scenarioId: number;
    syncStartTime: string;
    syncEndTime?: string;
    status: 'RUNNING' | 'SUCCESS' | 'FAIL';
    totalCount: number;
    successCount: number;
    failCount: number;
    errorMessage?: string;
    syncParams?: string;
    fourNatureSummary?: string;
}

export interface ConnectionTestResult {
    success: boolean;
    adapterName?: string;
    message: string;
}

export const erpApi = {
    // Configs
    getConfigs: async (): Promise<ApiResponse<ErpConfig[]>> => {
        const response = await client.get<ApiResponse<ErpConfig[]>>('/erp/config');
        return response.data;
    },

    // Test Connection
    testConnection: async (configId: number): Promise<ApiResponse<ConnectionTestResult>> => {
        const response = await client.post<ApiResponse<ConnectionTestResult>>(`/erp/config/${configId}/test`, {});
        return response.data;
    },

    saveConfig: async (config: Partial<ErpConfig>): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/erp/config', config);
        return response.data;
    },

    deleteConfig: async (id: number): Promise<ApiResponse<void>> => {
        const response = await client.delete<ApiResponse<void>>(`/erp/config/${id}`);
        return response.data;
    },

    // Scenarios
    getScenarios: async (configId: number): Promise<ApiResponse<ErpScenario[]>> => {
        const response = await client.get<ApiResponse<ErpScenario[]>>(`/erp/scenario/list/${configId}`);
        return response.data;
    },
    updateScenario: async (data: Partial<ErpScenario>): Promise<ApiResponse<void>> => {
        const response = await client.put<ApiResponse<void>>('/erp/scenario', data);
        return response.data;
    },
    triggerSync: async (id: number): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>(`/erp/scenario/${id}/sync`, {});
        return response.data;
    },

    // Sub-Interfaces
    getSubInterfaces: async (scenarioId: number): Promise<ApiResponse<ErpSubInterface[]>> => {
        const response = await client.get<ApiResponse<ErpSubInterface[]>>(`/erp/scenario/${scenarioId}/interfaces`);
        return response.data;
    },
    toggleSubInterface: async (id: number): Promise<ApiResponse<void>> => {
        const response = await client.put<ApiResponse<void>>(`/erp/scenario/interface/toggle/${id}`);
        return response.data;
    },

    // Sync History
    getSyncHistory: async (scenarioId: number): Promise<ApiResponse<SyncHistory[]>> => {
        const response = await client.get<ApiResponse<SyncHistory[]>>(`/erp/scenario/${scenarioId}/history`);
        return response.data;
    },

    // Scenario Params
    updateScenarioParams: async (scenarioId: number, params: Record<string, unknown>): Promise<ApiResponse<void>> => {
        const response = await client.put<ApiResponse<void>>(`/erp/scenario/${scenarioId}/params`, params);
        return response.data;
    },

    // Adapter Types
    getAdapterTypes: async (): Promise<ApiResponse<{ identifier: string; name: string; description: string }[]>> => {
        const response = await client.get<ApiResponse<{ identifier: string; name: string; description: string }[]>>('/erp/config/types');
        return response.data;
    },

    diagnoseConfig: async (id: number): Promise<ApiResponse<any>> => {
        const response = await client.get<ApiResponse<any>>(`/erp/config/${id}/diagnose`);
        return response.data;
    },

    // Phase 4: Reconciliation
    triggerReconciliation: async (params: any): Promise<ApiResponse<any>> => {
        const response = await client.post<ApiResponse<any>>('/reconciliation/trigger', params);
        return response.data;
    },

    getReconHistory: async (configId: number): Promise<ApiResponse<any[]>> => {
        const response = await client.get<ApiResponse<any[]>>('/reconciliation/history', { params: { configId } });
        return response.data;
    },

    // Phase 4: Monitoring
    getIntegrationMonitoring: async (): Promise<ApiResponse<any>> => {
        const response = await client.get<ApiResponse<any>>('/monitoring/integration');
        return response.data;
    },
};

// Integration Channel for Online Reception page
export interface IntegrationChannel {
    id: number;
    name: string;
    displayName: string;
    configName: string;
    erpType: string;
    frequency: string;
    lastSync: string | null;
    receivedCount: number;
    status: 'normal' | 'error' | 'syncing';
    description: string;
    apiEndpoint: string | null;
    accbookCode: string | null;
    lastSyncMsg: string | null;
}

export const integrationApi = {
    getChannels: async (): Promise<ApiResponse<IntegrationChannel[]>> => {
        const response = await client.get<ApiResponse<IntegrationChannel[]>>('/erp/scenario/channels');
        return response.data;
    },
};
