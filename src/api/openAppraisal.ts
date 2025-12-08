import { client as apiClient } from './client';

export interface OpenAppraisal {
    id: string;
    archiveId: string;
    archiveCode?: string;
    archiveTitle?: string;
    retentionPeriod?: string;
    currentSecurityLevel?: string;
    appraiserId?: string;
    appraiserName?: string;
    appraisalDate?: string;
    appraisalResult?: 'OPEN' | 'CONTROLLED' | 'EXTENDED';
    openLevel?: 'PUBLIC' | 'INTERNAL' | 'RESTRICTED';
    reason?: string;
    status: 'PENDING' | 'COMPLETED';
    createdTime?: string;
    lastModifiedTime?: string;
}

export interface AppraisalSubmitRequest {
    id: string;
    appraiserId: string;
    appraiserName: string;
    appraisalResult: 'OPEN' | 'CONTROLLED' | 'EXTENDED';
    openLevel?: string;
    reason: string;
}

export const openAppraisalApi = {
    /**
     * 获取鉴定任务列表
     */
    getAppraisalList: async (page: number = 1, limit: number = 10, status?: string) => {
        const params: any = { page, limit };
        if (status) params.status = status;
        return apiClient.get('/api/open-appraisal/list', { params });
    },

    /**
     * 获取鉴定详情
     */
    getAppraisalById: async (id: string) => {
        return apiClient.get(`/api/open-appraisal/${id}`);
    },

    /**
     * 创建鉴定任务
     */
    createAppraisal: async (appraisal: Partial<OpenAppraisal>) => {
        return apiClient.post('/api/open-appraisal/create', appraisal);
    },

    /**
     * 提交鉴定结果
     */
    submitAppraisal: async (request: AppraisalSubmitRequest) => {
        return apiClient.post('/api/open-appraisal/submit', request);
    },
};
