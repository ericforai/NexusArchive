// Input: API client 与 ApiResponse 类型
// Output: statsApi
// Pos: 统计报表 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

export interface ArchivalTrendPoint {
  date: string;
  count: number;
}

export interface DashboardStats {
  totalArchives: number;
  storageUsed: string;
  pendingTasks: number;
  todayIngest: number;
  recentTrend: ArchivalTrendPoint[];
}

export interface StorageStats {
  total: string;
  used: string;
  usagePercent: number;
}

export interface TaskStatusStats {
  total: number;
  completed: number;
  failed: number;
  running: number;
  pending: number;
  byStatus: Record<string, number>;
}

export const statsApi = {
  getErpStats: async () => {
    const response = await client.get<ApiResponse<ErpStats>>('/erp/config/stats');
    return response.data;
  },
  getDestructionStats: async () => {
    const response = await client.get<ApiResponse<DestructionStats>>('/destruction/stats');
    return response.data;
  },
  getDashboard: async () => {
    const response = await client.get<ApiResponse<DashboardStats>>('/stats/dashboard');
    return response.data;
  },
  getTrend: async () => {
    const response = await client.get<ApiResponse<ArchivalTrendPoint[]>>('/stats/archival-trend');
    return response.data;
  },
  getStorage: async () => {
    const response = await client.get<ApiResponse<StorageStats>>('/stats/storage');
    return response.data;
  },
  getTaskStatus: async () => {
    const response = await client.get<ApiResponse<TaskStatusStats>>('/stats/ingest-status');
    return response.data;
  }
};
