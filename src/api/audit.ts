// Input: API client 与 ApiResponse/PageResult
// Output: auditApi
// Pos: 审计日志 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
