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
        applicantId: string;
        applicantName: string;
        deptId?: string;
        deptName?: string;
        purpose: string;
        borrowType: 'READING' | 'COPY' | 'LOAN';
        archiveIds: string[];
        expectedStartDate: string;
        expectedEndDate: string;
    }): Promise<ApiResponse<BorrowingRecord>> => {
        const response = await client.post('/borrow/requests', data);
        return response.data;
    },

    // 获取借阅列表
    getBorrowings: async (params?: BorrowingListParams): Promise<ApiResponse<PageResult<BorrowingRecord>>> => {
        const response = await client.get('/borrow/requests', { params });
        return response.data;
    },

    // 审批借阅 (修正: 后端 Command 需要 requestId)
    approveBorrowing: async (id: string, payload: { approverId: string; approverName: string; approved: boolean; comment: string }): Promise<ApiResponse<void>> => {
        const response = await client.post(`/borrow/requests/${id}/approve`, { ...payload, requestId: id });
        return response.data;
    },

    // 确认借出
    confirmOut: async (id: string): Promise<ApiResponse<void>> => {
        const response = await client.post(`/borrow/requests/${id}/confirm-out`);
        return response.data;
    },

    // 归还档案
    returnArchive: async (id: string, operatorId: string): Promise<ApiResponse<void>> => {
        const response = await client.post(`/borrow/requests/${id}/return`, null, {
            params: { operatorId }
        });
        return response.data;
    },

    // 取消借阅 (Mock)
    cancelBorrowing: async (_id: string): Promise<ApiResponse<void>> => {
        // const response = await client.post(`/borrow/requests/${id}/cancel`);
        console.warn('Cancel not implemented in backend yet');
        return { code: 200, message: 'Simulated Cancel Success', data: undefined };
    },
};
