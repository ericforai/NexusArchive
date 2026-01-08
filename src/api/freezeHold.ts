// Input: API client 与 ApiResponse/PageResult
// Output: freezeHoldApi
// Pos: 冻结/保全管理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

/**
 * 冻结/保全类型
 */
export type FreezeHoldType = 'FREEZE' | 'HOLD';

/**
 * 冻结/保全状态
 */
export type FreezeHoldStatus = 'ACTIVE' | 'RELEASED' | 'EXPIRED';

/**
 * 冻结/保全记录
 */
export interface FreezeHoldRecord {
    id: string;
    archiveId: string;
    archiveCode: string;
    archiveTitle: string;
    type: FreezeHoldType;
    status: FreezeHoldStatus;
    reason: string;
    startDate: string;
    endDate?: string;
    applicantId: string;
    applicantName: string;
    createdAt: string;
    releasedAt?: string;
    releasedBy?: string;
    releaseReason?: string;
}

/**
 * 申请冻结/保全请求
 */
export interface ApplyFreezeHoldRequest {
    archiveIds: string[];
    type: FreezeHoldType;
    reason: string;
    endDate?: string; // 可选，不填则永久冻结
}

/**
 * 解除冻结/保全请求
 */
export interface ReleaseFreezeHoldRequest {
    id: string;
    reason: string;
}

export const freezeHoldApi = {
    /**
     * 申请冻结/保全
     */
    apply: async (request: ApplyFreezeHoldRequest): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/archive/freeze/apply', request);
        return response.data;
    },

    /**
     * 获取冻结/保全列表
     */
    list: async (params?: {
        page?: number;
        size?: number;
        type?: FreezeHoldType;
        status?: FreezeHoldStatus;
        archiveCode?: string;
    }): Promise<ApiResponse<PageResult<FreezeHoldRecord>>> => {
        const response = await client.get<ApiResponse<PageResult<FreezeHoldRecord>>>(
            '/archive/freeze/list',
            { params }
        );
        return response.data;
    },

    /**
     * 获取冻结/保全详情
     */
    getDetail: async (id: string): Promise<ApiResponse<FreezeHoldRecord>> => {
        const response = await client.get<ApiResponse<FreezeHoldRecord>>(
            `/archive/freeze/${id}`
        );
        return response.data;
    },

    /**
     * 解除冻结/保全
     */
    release: async (request: ReleaseFreezeHoldRequest): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>(
            `/archive/freeze/${request.id}/release`,
            { reason: request.reason }
        );
        return response.data;
    },
};






