// Input: API client 与 ApiResponse 类型
// Output: paymentApplySyncApi
// Pos: YonSuite 付款申请单同步 - 前端 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 付款申请单记录
 */
export interface PaymentApplyRecord {
  /** 单据 ID */
  id: string;
  /** 单据编码 */
  code: string;
  /** 申请日期 */
  billDate: string;
  /** 申请金额 */
  applyAmount: string;
  /** 币种 */
  pkCurrency: string;
  /** 币种名称 */
  currencyName: string;
  /** 申请人 ID */
  creatorId: string;
  /** 申请人名称 */
  creatorName: string;
  /** 审核状态 */
  verifyState: string;
  /** 审核状态名称 */
  verifyStateName: string;
  /** 付款状态 */
  payStatus: string;
  /** 付款状态名称 */
  payStatusName: string;
  /** 收款单位名称 */
  receiveUnitName: string;
  /** 收款账号 */
  receiveAccount: string;
  /** 收款开户行 */
  receiveBankName: string;
  /** 用途/摘要 */
  purpose: string;
  /** 来源系统 */
  srcSystem: string;
  /** 创建时间 */
  createTime: string;
  /** 修改时间 */
  modifyTime: string;
  /** 附件数量 */
  attachmentQty: number;
}

/**
 * 付款申请单同步请求参数
 */
export interface PaymentApplySyncRequest {
  configId: number;
  startDate?: string;
  endDate?: string;
}

/**
 * 付款申请单列表响应
 */
export interface PaymentApplyListResponse {
  code: string;
  message: string;
  data?: {
    recordList: PaymentApplyRecord[];
    pageCount: number;
    totalCount: number;
  };
}

/**
 * 付款申请单同步 API
 */
export const paymentApplySyncApi = {
  syncPaymentApply: async (params: PaymentApplySyncRequest): Promise<ApiResponse<string[]>> => {
    const { configId, startDate, endDate } = params;
    const queryParams = new URLSearchParams();
    queryParams.set('configId', String(configId));
    if (startDate) queryParams.set('startDate', startDate);
    if (endDate) queryParams.set('endDate', endDate);

    const response = await client.post<ApiResponse<string[]>>(
      '/yonsuite/generic/paymentapply/sync?' + queryParams.toString()
    );
    return response.data;
  },

  syncRecentPaymentApply: async (configId: number): Promise<ApiResponse<string[]>> => {
    const response = await client.post<ApiResponse<string[]>>(
      '/yonsuite/generic/paymentapply/sync/recent?configId=' + configId
    );
    return response.data;
  },

  queryPaymentApplyList: async (params: PaymentApplySyncRequest): Promise<ApiResponse<PaymentApplyListResponse>> => {
    const { configId, startDate, endDate } = params;
    const queryParams = new URLSearchParams();
    queryParams.set('configId', String(configId));
    if (startDate) queryParams.set('startDate', startDate);
    if (endDate) queryParams.set('endDate', endDate);

    const response = await client.get<ApiResponse<PaymentApplyListResponse>>(
      '/yonsuite/generic/paymentapply/list?' + queryParams.toString()
    );
    return response.data;
  },
};
