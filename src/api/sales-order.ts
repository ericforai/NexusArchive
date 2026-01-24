// Input: API client
// Output: salesOrderApi
// Pos: 销售订单 - 前端 API 层

import { client } from './client';

export interface SalesOrderSyncRequest {
  dateBegin?: string;
  dateEnd?: string;
  statusCodes?: string[];
  agentId?: string;
  salesOrgId?: string;
}

export interface SalesOrderSyncResult {
  total: number;
  success: number;
  failed: number;
  skipped: number;
  relatedSalesOut?: number;
  relatedVoucher?: number;
  errors: Array<{ orderCode: string; reason: string }>;
}

export interface SalesOrder {
  id: number;
  orderCode: string;
  agentName: string;
  vouchdate: string;
  payMoney: number;
  nextStatusName: string;
}

export const salesOrderApi = {
  // 同步销售订单
  sync: (request: SalesOrderSyncRequest) =>
    client.post<SalesOrderSyncResult>('/sales-order/sync', request),

  // 查询订单详情
  get: (id: number) =>
    client.get<SalesOrder>(`/sales-order/${id}`),

  // 查询关联链路
  getRelations: (id: number) =>
    client.get(`/sales-order/${id}/relations`),
};
