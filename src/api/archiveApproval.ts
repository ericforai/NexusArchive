// Input: API client
// Output: archiveApprovalApi
// Pos: 归档审批流程 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client as apiClient } from './client';

export interface ArchiveApproval {
    id: string;
    archiveId: string;
    archiveCode?: string;
    archiveTitle?: string;
    applicantId: string;
    applicantName?: string;
    applicationReason?: string;
    approverId?: string;
    approverName?: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    approvalComment?: string;
    approvalTime?: string;
    createdTime?: string;
    lastModifiedTime?: string;
}

export interface ApprovalRequest {
    id: string;
    approverId: string;
    approverName: string;
    comment: string;
}

export interface BatchApprovalRequest {
    ids: string[];
    approverId: string;
    approverName: string;
    comment: string;
}

export interface BatchApprovalResult {
    success: number;
    failed: number;
    errors?: Array<{ id: string; reason: string }>;
}

export const archiveApprovalApi = {
    /**
     * 获取审批列表
     */
    getApprovalList: async (page: number = 1, limit: number = 10, status?: string) => {
        const params: any = { page, limit };
        if (status) params.status = status;
        return apiClient.get('/archive-approval/list', { params });
    },

    /**
     * 获取审批详情
     */
    getApprovalById: async (id: string) => {
        return apiClient.get(`/archive-approval/${id}`);
    },

    /**
     * 创建审批申请
     */
    createApproval: async (approval: Partial<ArchiveApproval>) => {
        return apiClient.post('/archive-approval/create', approval);
    },

    /**
     * 批准归档
     */
    approveArchive: async (request: ApprovalRequest) => {
        return apiClient.post('/archive-approval/approve', request);
    },

    /**
     * 拒绝归档
     */
    rejectArchive: async (request: ApprovalRequest) => {
        return apiClient.post('/archive-approval/reject', request);
    },

    /**
     * 批量批准归档
     */
    batchApprove: async (request: BatchApprovalRequest): Promise<BatchApprovalResult> => {
        return apiClient.post('/archive-approval/batch-approve', request);
    },

    /**
     * 批量拒绝归档
     */
    batchReject: async (request: BatchApprovalRequest): Promise<BatchApprovalResult> => {
        return apiClient.post('/archive-approval/batch-reject', request);
    },
};
