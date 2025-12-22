// Input: API client
// Output: workflowApi
// Pos: 流程引擎 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';

export const workflowApi = {
    // 启动流程
    startWorkflow: async (workflowCode: string, businessId: string) => {
        const response = await client.post('/workflow/start', { workflowCode, businessId });
        return response.data;
    },

    // 获取待办任务
    getTasks: async () => {
        const response = await client.get('/workflow/tasks');
        return response.data;
    },

    // 审批任务
    approveTask: async (instanceId: string, comment?: string) => {
        const response = await client.post(`/workflow/instances/${instanceId}/approve`, { comment });
        return response.data;
    },

    // 拒绝任务
    rejectTask: async (instanceId: string, comment?: string) => {
        const response = await client.post(`/workflow/instances/${instanceId}/reject`, { comment });
        return response.data;
    },
};
