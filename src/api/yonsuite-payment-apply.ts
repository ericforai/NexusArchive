// Input: API client 与 ApiResponse 类型
// Output: paymentApplyFileApi
// Pos: YonSuite 付款申请单 - 前端 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 付款申请单文件信息
 */
export interface PaymentApplyFileInfo {
  /** 文件 ID */
  id: string;
  /** 文件名称 */
  fileName: string;
  /** 下载地址 (带签名的 OSS URL，有实效性) */
  downLoadUrl: string;
}

/**
 * 付款申请单文件查询参数
 */
export interface PaymentApplyFileQuery {
  configId: number;
  fileIds: string[];
}

/**
 * 付款申请单文件 API
 */
export const paymentApplyFileApi = {
  /**
   * 批量查询文件下载地址
   *
   * @param query 查询参数
   * @returns 文件信息列表
   */
  queryFileUrls: async (query: PaymentApplyFileQuery): Promise<ApiResponse<PaymentApplyFileInfo[]>> => {
    const { configId, fileIds } = query;

    const response = await client.post<ApiResponse<PaymentApplyFileInfo[]>>(
      `/integration/yonsuite/payment-apply/file/url?configId=${configId}`,
      fileIds
    );
    return response.data;
  },

  /**
   * 查询单个文件下载地址
   *
   * @param configId 配置 ID
   * @param fileId 文件 ID
   * @returns 文件信息
   */
  queryFileUrl: async (
    configId: number,
    fileId: string
  ): Promise<ApiResponse<PaymentApplyFileInfo>> => {
    const response = await client.get<ApiResponse<PaymentApplyFileInfo>>(
      `/integration/yonsuite/payment-apply/file/url/${fileId}?configId=${configId}`
    );
    return response.data;
  },

  /**
   * 健康检查
   */
  health: async (): Promise<ApiResponse<string>> => {
    const response = await client.get<ApiResponse<string>>(
      '/integration/yonsuite/payment-apply/file/health'
    );
    return response.data;
  },
};
