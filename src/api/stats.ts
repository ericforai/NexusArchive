import { client } from './client';
import { ApiResponse } from '../types';

export interface ErpStats {
  connectedSystems: number;
  todayReceived: number;
  activeInterfaces: number;
  abnormalCount: number;
}

export interface DestructionStats {
  pendingAppraisal: number;
  aiSuggested: number;
  activeBatches: number;
  safeDestructionCount: number;
}

export const statsApi = {
  getErpStats: async () => {
    const response = await client.get<ApiResponse<ErpStats>>('/erp/config/stats');
    return response.data;
  },
  getDestructionStats: async () => {
    const response = await client.get<ApiResponse<DestructionStats>>('/destruction/stats');
    return response.data;
  }
};
