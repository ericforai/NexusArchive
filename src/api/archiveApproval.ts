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

export const archiveApprovalApi = {
    /**
     * 获取审批列表
     */
    getApprovalList: async (page: number = 1, limit: number = 10, status?: string) => {
        const params: any = { page, limit };
        if (status) params.status = status;
        return apiClient.get('/api/archive-approval/list', { params });
    },

    /**
     * 获取审批详情
     */
    getApprovalById: async (id: string) => {
        return apiClient.get(`/api/archive-approval/${id}`);
    },

    /**
     * 创建审批申请
     */
    createApproval: async (approval: Partial<ArchiveApproval>) => {
        return apiClient.post('/api/archive-approval/create', approval);
    },

    /**
     * 批准归档
     */
    approveArchive: async (request: ApprovalRequest) => {
        return apiClient.post('/api/archive-approval/approve', request);
    },

    /**
     * 拒绝归档
     */
    rejectArchive: async (request: ApprovalRequest) => {
        return apiClient.post('/api/archive-approval/reject', request);
    },
};
