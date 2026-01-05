// Input: API client 与 ApiResponse
// Output: yonsuiteApi
// Pos: YonSuite 集成 - 前端 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 凭证附件
 */
export interface VoucherAttachment {
  fileId: string;
  filePath: string;
  ctime: number;
  utime: number;
  fileExtension: string;
  fileSize: number;
  fileName: string;
  name: string;
  yhtUserId: string;
  tenantId: string;
  objectId: string;
  objectName: string;
}

/**
 * 销售出库单记录
 */
export interface SalesOutRecord {
  id: string;
  code: string;
  vouchdate: string;
  custName: string;
  warehouseName: string;
  totalQuantity: string;
}

/**
 * 同步请求参数
 */
export interface SyncRequest {
  configId: number;
  startDate?: string;  // yyyy-MM-dd
  endDate?: string;    // yyyy-MM-dd
}

/**
 * 同步响应结果
 */
export interface SyncResult {
  code: number;
  message: string;
  data: string[];     // 同步的 ID 列表
  timestamp: number;
}

export const yonsuiteApi = {
  /**
   * 同步销售出库单列表（指定日期范围）
   */
  syncSalesOut: async (params: SyncRequest) => {
    const { configId, startDate, endDate } = params;
    const queryParams = new URLSearchParams({
      configId: String(configId),
      startDate: startDate || '',
      endDate: endDate || ''
    });

    const response = await client.post<ApiResponse<string[]>>(
      `/yonsuite/generic/salesout/sync?${queryParams.toString()}`
    );
    return response.data;
  },

  /**
   * 快速同步（最近7天）
   */
  syncRecentSalesOut: async (configId: number) => {
    const response = await client.post<ApiResponse<string[]>>(
      `/yonsuite/generic/salesout/sync/recent?configId=${configId}`
    );
    return response.data;
  },

  /**
   * 同步单个销售出库单详情
   */
  syncSalesOutDetail: async (configId: number, salesOutId: string) => {
    const response = await client.post<ApiResponse<string>>(
      `/yonsuite/generic/salesout/detail?configId=${configId}&salesOutId=${salesOutId}`
    );
    return response.data;
  },

  /**
   * 查询凭证附件
   */
  queryVoucherAttachments: async (configId: number, businessIds: string[]) => {
    const response = await client.post<ApiResponse<Record<string, VoucherAttachment[]>>>(
      `/yonsuite/generic/voucher/attachments?configId=${configId}`,
      businessIds
    );
    return response.data;
  },
};
