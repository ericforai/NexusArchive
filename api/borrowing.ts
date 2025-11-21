import { client } from './client';

export const borrowingApi = {
    // 创建借阅申请
    createBorrowing: async (data: {
        archiveId: string;
        reason: string;
        borrowDate: string;
        expectedReturnDate: string;
    }) => {
        const response = await client.post('/borrowing', data);
        return response.data;
    },

    // 获取借阅列表
    getBorrowings: async (params?: { status?: string; my?: boolean }) => {
        const response = await client.get('/borrowing', { params });
        return response.data;
    },

    // 归还档案
    returnArchive: async (id: string) => {
        const response = await client.post(`/borrowing/${id}/return`);
        return response.data;
    },

    // 取消借阅
    cancelBorrowing: async (id: string) => {
        const response = await client.post(`/borrowing/${id}/cancel`);
        return response.data;
    },
};
