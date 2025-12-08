import { client } from './client';
import { ApiResponse } from '../types';

export interface ArchivalTrendPoint {
  date: string;
  count: number;
}

export interface DashboardStats {
  totalArchives: number;
  storageUsed: string;
  pendingTasks: number;
  todayIngest: number;
  recentTrend?: ArchivalTrendPoint[];
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
  getDashboard: async () => {
    const res = await client.get<ApiResponse<DashboardStats>>('/stats/dashboard');
    return res.data;
  },
  getStorage: async () => {
    const res = await client.get<ApiResponse<StorageStats>>('/stats/storage');
    return res.data;
  },
  getTrend: async () => {
    const res = await client.get<ApiResponse<ArchivalTrendPoint[]>>('/stats/archival-trend');
    return res.data;
  },
  getTaskStatus: async () => {
    const res = await client.get<ApiResponse<TaskStatusStats>>('/stats/ingest-status');
    return res.data;
  }
};
