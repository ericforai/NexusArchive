// Input: API client 与 ApiResponse/PageResult
// Output: destructionApi
// Pos: 档案销毁 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

/**
 * 到期档案
 */
export interface ExpiredArchive {
    id: string;
    archiveCode: string;
    title: string;
    fondsNo: string;
    fiscalYear: string;
    retentionPeriod: string;
    expiredDate: string;
    status: string;
}

/**
 * 鉴定清单
 */
export interface AppraisalList {
    id: string;
    name: string;
    fondsNo: string;
    archiveCount: number;
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
    createdAt: string;
    completedAt?: string;
}

/**
 * 销毁申请
 */
export interface Destruction {
    id: string;
    applicantId: string;
    applicantName: string;
    archiveIds: string[];
    archiveCount: number;
    reason: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTING' | 'COMPLETED';
    firstApproverId?: string;
    firstApproverName?: string;
    firstApprovalTime?: string;
    firstApprovalComment?: string;
    secondApproverId?: string;
    secondApproverName?: string;
    secondApprovalTime?: string;
    secondApprovalComment?: string;
    executionTime?: string;
    createdAt: string;
}

/**
 * 销毁审批请求
 */
export interface DestructionApprovalRequest {
    comment?: string;
    approved: boolean;
}

/**
 * 批量审批请求
 */
export interface BatchDestructionApprovalRequest {
    ids: string[];
    comment?: string;
    approvalType: 'first' | 'second';
}

/**
 * 批量审批结果
 */
export interface BatchApprovalResult {
    success: number;
    failed: number;
    errors: Array<{ id: string; reason: string }>;
}

export const destructionApi = {
    /**
     * 获取到期档案列表
     */
    getExpiredArchives: async (params?: {
        page?: number;
        size?: number;
        fondsNo?: string;
        fiscalYear?: string;
        retentionPeriod?: string;
    }): Promise<ApiResponse<PageResult<ExpiredArchive>>> => {
        const response = await client.get<ApiResponse<PageResult<ExpiredArchive>>>(
            '/archive/expired',
            { params }
        );
        return response.data;
    },

    /**
     * 生成鉴定清单
     */
    generateAppraisalList: async (archiveIds: string[]): Promise<ApiResponse<AppraisalList>> => {
        const response = await client.post<ApiResponse<AppraisalList>>(
            '/archive/appraisal/generate',
            { archiveIds }
        );
        return response.data;
    },

    /**
     * 获取鉴定清单列表
     */
    getAppraisalLists: async (params?: {
        page?: number;
        size?: number;
        status?: string;
    }): Promise<ApiResponse<PageResult<AppraisalList>>> => {
        const response = await client.get<ApiResponse<PageResult<AppraisalList>>>(
            '/archive/appraisal/list',
            { params }
        );
        return response.data;
    },

    /**
     * 获取鉴定清单详情
     */
    getAppraisalListDetail: async (id: string): Promise<ApiResponse<AppraisalList>> => {
        const response = await client.get<ApiResponse<AppraisalList>>(
            `/archive/appraisal/${id}`
        );
        return response.data;
    },

    /**
     * 导出鉴定清单
     */
    exportAppraisalList: async (id: string, format: 'excel' | 'pdf'): Promise<Blob> => {
        const response = await client.get(
            `/archive/appraisal/${id}/export`,
            {
                params: { format },
                responseType: 'blob'
            }
        );
        return response.data;
    },

    /**
     * 获取销毁申请列表
     */
    getDestructions: async (params?: {
        page?: number;
        limit?: number;
        status?: string;
    }): Promise<ApiResponse<PageResult<Destruction>>> => {
        const response = await client.get<ApiResponse<PageResult<Destruction>>>(
            '/destruction',
            { params }
        );
        return response.data;
    },

    /**
     * 创建销毁申请
     */
    createDestruction: async (data: {
        reason: string;
        archiveIds: string[];
    }): Promise<ApiResponse<Destruction>> => {
        const response = await client.post<ApiResponse<Destruction>>(
            '/destruction',
            data
        );
        return response.data;
    },

    /**
     * 审批销毁申请
     */
    approveDestruction: async (
        id: string,
        request: DestructionApprovalRequest
    ): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>(
            `/destruction/${id}/approve`,
            request
        );
        return response.data;
    },

    /**
     * 执行销毁
     */
    executeDestruction: async (id: string): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>(
            `/destruction/${id}/execute`
        );
        return response.data;
    },

    /**
     * 获取销毁统计
     */
    getStats: async (): Promise<ApiResponse<{
        pendingAppraisal: number;
        aiSuggested: number;
        activeBatches: number;
        safeDestructionCount: number;
    }>> => {
        const response = await client.get<ApiResponse<{
            pendingAppraisal: number;
            aiSuggested: number;
            activeBatches: number;
            safeDestructionCount: number;
        }>>('/destruction/stats');
        return response.data;
    },

    /**
     * 批量审批销毁申请
     */
    batchApprove: async (request: BatchDestructionApprovalRequest): Promise<ApiResponse<BatchApprovalResult>> => {
        const response = await client.post<ApiResponse<BatchApprovalResult>>(
            '/destruction/batch-approve',
            request
        );
        return response.data;
    },

    /**
     * 批量拒绝销毁申请
     */
    batchReject: async (request: BatchDestructionApprovalRequest): Promise<ApiResponse<BatchApprovalResult>> => {
        const response = await client.post<ApiResponse<BatchApprovalResult>>(
            '/destruction/batch-reject',
            { ...request, comment: request.comment || '' } // 拒绝时意见必填
        );
        return response.data;
    },
};
