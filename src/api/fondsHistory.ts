// Input: API client 与 ApiResponse
// Output: fondsHistoryApi
// Pos: 全宗沿革 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 全宗沿革详情
 */
export interface FondsHistoryDetail {
    id: string;
    fondsNo: string;
    eventType: 'MIGRATE' | 'MERGE' | 'SPLIT' | 'RENAME';
    fromFondsNo?: string;
    toFondsNo?: string;
    effectiveDate: string; // ISO date string
    reason: string;
    approvalTicketId?: string;
    snapshot?: Record<string, any>;
    createdBy?: string;
    createdAt: string; // ISO datetime string
}

/**
 * 全宗迁移请求
 */
export interface MigrateFondsRequest {
    fromFondsNo: string;
    toFondsNo: string;
    effectiveDate: string; // ISO date string
    reason: string;
    approvalTicketId?: string;
}

/**
 * 全宗合并请求
 */
export interface MergeFondsRequest {
    sourceFondsNos: string[];
    targetFondsNo: string;
    effectiveDate: string; // ISO date string
    reason: string;
    approvalTicketId?: string;
}

/**
 * 全宗分立请求
 */
export interface SplitFondsRequest {
    sourceFondsNo: string;
    newFondsNos: string[];
    effectiveDate: string; // ISO date string
    reason: string;
    approvalTicketId?: string;
}

/**
 * 全宗重命名请求
 */
export interface RenameFondsRequest {
    oldFondsNo: string;
    newFondsNo: string;
    effectiveDate: string; // ISO date string
    reason: string;
    approvalTicketId?: string;
}

export const fondsHistoryApi = {
    /**
     * 全宗迁移
     */
    migrate: async (request: MigrateFondsRequest) => {
        const response = await client.post<ApiResponse<{ historyId: string }>>(
            '/fonds-history/migrate',
            null,
            {
                params: {
                    fromFondsNo: request.fromFondsNo,
                    toFondsNo: request.toFondsNo,
                    effectiveDate: request.effectiveDate,
                    reason: request.reason,
                    ...(request.approvalTicketId && { approvalTicketId: request.approvalTicketId }),
                },
            }
        );
        return response.data;
    },

    /**
     * 全宗合并
     */
    merge: async (request: MergeFondsRequest) => {
        const params = new URLSearchParams();
        request.sourceFondsNos.forEach(no => params.append('sourceFondsNos', no));
        params.append('targetFondsNo', request.targetFondsNo);
        params.append('effectiveDate', request.effectiveDate);
        params.append('reason', request.reason);
        if (request.approvalTicketId) {
            params.append('approvalTicketId', request.approvalTicketId);
        }
        const response = await client.post<ApiResponse<{ historyIds: string[] }>>(
            `/fonds-history/merge?${params.toString()}`,
            null
        );
        return response.data;
    },

    /**
     * 全宗分立
     */
    split: async (request: SplitFondsRequest) => {
        const params = new URLSearchParams();
        params.append('sourceFondsNo', request.sourceFondsNo);
        request.newFondsNos.forEach(no => params.append('newFondsNos', no));
        params.append('effectiveDate', request.effectiveDate);
        params.append('reason', request.reason);
        if (request.approvalTicketId) {
            params.append('approvalTicketId', request.approvalTicketId);
        }
        const response = await client.post<ApiResponse<{ historyIds: string[] }>>(
            `/fonds-history/split?${params.toString()}`,
            null
        );
        return response.data;
    },

    /**
     * 全宗重命名
     */
    rename: async (request: RenameFondsRequest) => {
        const params = new URLSearchParams();
        params.append('oldFondsNo', request.oldFondsNo);
        params.append('newFondsNo', request.newFondsNo);
        params.append('effectiveDate', request.effectiveDate);
        params.append('reason', request.reason);
        if (request.approvalTicketId) {
            params.append('approvalTicketId', request.approvalTicketId);
        }
        const response = await client.post<ApiResponse<{ historyId: string }>>(
            `/fonds-history/rename?${params.toString()}`,
            null
        );
        return response.data;
    },

    /**
     * 查询全宗沿革历史
     */
    getHistory: async (fondsNo: string) => {
        const response = await client.get<ApiResponse<FondsHistoryDetail[]>>(
            `/fonds-history/${fondsNo}`
        );
        return response.data;
    },
};

