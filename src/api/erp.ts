import { client } from './client';
import { ApiResponse } from '../types';

export interface ErpConfig {
    id: number;
    name: string;
    erpType: string;
    status?: string;
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
