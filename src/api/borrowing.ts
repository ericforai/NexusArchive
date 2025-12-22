// Input: API client 与 ApiResponse/PageResult
// Output: borrowingApi
// Pos: 借阅流程 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface BorrowingRecord {
    id: string;
    archiveId: string;
    archiveTitle?: string;
    userName?: string;
    borrowDate?: string;
    expectedReturnDate?: string;
    actualReturnDate?: string;
    status: string;
    reason?: string;
    approvalComment?: string;
}

export interface BorrowingListParams {
    status?: string;
    my?: boolean;
    page?: number;
    limit?: number;
}

export const borrowingApi = {
    // 创建借阅申请
    createBorrowing: async (data: {
        archiveId: string;
        reason?: string;
        borrowDate?: string;
        expectedReturnDate?: string;
    }): Promise<ApiResponse<BorrowingRecord>> => {
        const response = await client.post('/borrowing', data);
        return response.data;
    },

    // 获取借阅列表
    getBorrowings: async (params?: BorrowingListParams): Promise<ApiResponse<PageResult<BorrowingRecord>>> => {
        const response = await client.get('/borrowing', { params });
        return response.data;
    },

    // 审批借阅
    approveBorrowing: async (id: string, payload: { approved: boolean; comment?: string }): Promise<ApiResponse<BorrowingRecord>> => {
        const response = await client.post(`/borrowing/${id}/approve`, payload);
        return response.data;
    },

    // 归还档案
    returnArchive: async (id: string): Promise<ApiResponse<void>> => {
        const response = await client.post(`/borrowing/${id}/return`);
        return response.data;
    },

    // 取消借阅
    cancelBorrowing: async (id: string): Promise<ApiResponse<void>> => {
        const response = await client.post(`/borrowing/${id}/cancel`);
        return response.data;
    },
};
