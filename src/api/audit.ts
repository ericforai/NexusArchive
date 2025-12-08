import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface AuditLog {
  id: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  operationResult?: string;
  riskLevel?: string;
  details?: string;
  clientIp?: string;
  createdAt?: string;
}

export const auditApi = {
  getLogs: async (params?: any) => {
    const res = await client.get<ApiResponse<PageResult<AuditLog>>>('/audit-logs', { params });
    return res.data;
  },
};
