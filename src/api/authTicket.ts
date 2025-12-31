// Input: API client 与 ApiResponse
// Output: authTicketApi
// Pos: 授权票据 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 授权范围
 */
export interface AuthScope {
    archiveYears?: number[];
    docTypes?: string[];
    keywords?: string[];
    accessType?: 'READ_ONLY' | 'READ_WRITE';
}

/**
 * 审批链
 */
export interface ApprovalChain {
    firstApprover?: {
        approverId: string;
        approverName: string;
        comment?: string;
        approved: boolean;
        approvedAt?: string;
    };
    secondApprover?: {
        approverId: string;
        approverName: string;
        comment?: string;
        approved: boolean;
        approvedAt?: string;
    };
}

/**
 * 授权票据详情
 */
export interface AuthTicketDetail {
    id: string;
    applicantId: string;
    applicantName: string;
    sourceFonds: string;
    targetFonds: string;
    scope: AuthScope;
    expiresAt: string; // ISO datetime string
    status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED' | 'EXPIRED';
    approvalChain?: ApprovalChain;
    reason: string;
    createdAt: string; // ISO datetime string
    lastModifiedTime?: string; // ISO datetime string
}

/**
 * 创建授权票据申请请求
 */
export interface CreateAuthTicketRequest {
    targetFonds: string;
    scope: AuthScope;
    expiresAt: string; // ISO datetime string
    reason: string;
}

/**
 * 审批请求
 */
export interface ApprovalRequest {
    comment: string;
    approved: boolean;
}

/**
 * 授权票据列表查询参数
 */
export interface AuthTicketListParams {
    page?: number;
    size?: number;
    status?: string;
    sourceFonds?: string;
    targetFonds?: string;
    applicantId?: string;
}

export const authTicketApi = {
    /**
     * 查询授权票据列表
     */
    list: async (params?: AuthTicketListParams): Promise<ApiResponse<{
        records: AuthTicketDetail[];
        total: number;
        page: number;
        size: number;
    }>> => {
        const response = await client.get<ApiResponse<{
            records: AuthTicketDetail[];
            total: number;
            page: number;
            size: number;
        }>>(
            '/auth-ticket/list',
            { params }
        );
        return response.data;
    },

    /**
     * 创建授权票据申请
     */
    apply: async (request: CreateAuthTicketRequest) => {
        const response = await client.post<ApiResponse<{
            ticketId: string;
            status: string;
            createdAt: string;
        }>>(
            '/auth-ticket/apply',
            request.scope,
            {
                params: {
                    targetFonds: request.targetFonds,
                    expiresAt: request.expiresAt,
                    reason: request.reason,
                },
            }
        );
        return response.data;
    },

    /**
     * 查询授权票据详情
     */
    getDetail: async (ticketId: string) => {
        const response = await client.get<ApiResponse<AuthTicketDetail>>(
            `/auth-ticket/${ticketId}`
        );
        return response.data;
    },

    /**
     * 撤销授权票据
     */
    revoke: async (ticketId: string, reason: string) => {
        const response = await client.post<ApiResponse<void>>(
            `/auth-ticket/${ticketId}/revoke?reason=${encodeURIComponent(reason)}`,
            null
        );
        return response.data;
    },

    /**
     * 第一审批人审批
     */
    firstApproval: async (ticketId: string, request: ApprovalRequest) => {
        const params = new URLSearchParams();
        params.append('comment', request.comment);
        params.append('approved', request.approved.toString());
        const response = await client.post<ApiResponse<void>>(
            `/auth-ticket/${ticketId}/first-approval?${params.toString()}`,
            null
        );
        return response.data;
    },

    /**
     * 第二审批人审批（复核）
     */
    secondApproval: async (ticketId: string, request: ApprovalRequest) => {
        const params = new URLSearchParams();
        params.append('comment', request.comment);
        params.append('approved', request.approved.toString());
        const response = await client.post<ApiResponse<void>>(
            `/auth-ticket/${ticketId}/second-approval?${params.toString()}`,
            null
        );
        return response.data;
    },
};

