// Input: API client
// Output: destructionApi
// Pos: 档案销毁 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';

export const destructionApi = {
    // 创建销毁申请
    createDestruction: async (data: {
        reason: string;
        archiveIds: string[];
    }) => {
        const response = await client.post('/destruction', {
            ...data,
            archiveIds: JSON.stringify(data.archiveIds) // Backend expects JSON string
        });
        return response.data;
    },

    // 获取销毁列表
    getDestructions: async (params?: { status?: string }) => {
        const response = await client.get('/destruction', { params });
        return response.data;
    },

    // 审批销毁
    approveDestruction: async (id: string, comment: string) => {
        const response = await client.post(`/destruction/${id}/approve`, { comment });
        return response.data;
    },

    // 执行销毁
    executeDestruction: async (id: string) => {
        const response = await client.post(`/destruction/${id}/execute`);
        return response.data;
    }
};
