// Input: API client 与 ApiResponse/PageResult
// Output: userLifecycleApi
// Pos: 用户生命周期管理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

/**
 * 入职触发请求
 */
export interface OnboardRequest {
    employeeId: string;
    roleIds: string[];
    organizationId?: string; // 组织ID（集团型架构必需）
}

/**
 * 离职触发请求
 */
export interface OffboardRequest {
    employeeId: string;
    reason?: string;
}

/**
 * 调岗触发请求
 */
export interface TransferRequest {
    employeeId: string;
    newRoleIds: string[];
    reason?: string;
    toOrganizationId?: string; // 目标组织ID（集团型架构必需）
}

/**
 * 复核任务
 */
export interface AccessReviewTask {
    id: string;
    userId: string;
    userName: string;
    roleNames: string[];
    reviewDate: string;
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
    reviewerId?: string;
    reviewerName?: string;
    reviewResult?: 'APPROVED' | 'REVOKED';
    reviewComment?: string;
    reviewedAt?: string;
}

/**
 * 执行复核请求
 */
export interface ExecuteReviewRequest {
    taskId: string;
    result: 'APPROVED' | 'REVOKED';
    comment?: string;
    revokeRoleIds?: string[];
}

export const userLifecycleApi = {
    /**
     * 入职触发
     */
    onboard: async (request: OnboardRequest): Promise<ApiResponse<{ userId: string; username: string }>> => {
        const response = await client.post<ApiResponse<{ userId: string; username: string }>>(
            '/user-lifecycle/onboard',
            request
        );
        return response.data;
    },

    /**
     * 离职触发
     */
    offboard: async (request: OffboardRequest): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/user-lifecycle/offboard', request);
        return response.data;
    },

    /**
     * 调岗触发
     */
    transfer: async (request: TransferRequest): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/user-lifecycle/transfer', request);
        return response.data;
    },

    /**
     * 获取复核任务列表
     */
    getReviewTasks: async (params?: {
        page?: number;
        size?: number;
        status?: string;
    }): Promise<ApiResponse<PageResult<AccessReviewTask>>> => {
        const response = await client.get<ApiResponse<PageResult<AccessReviewTask>>>(
            '/access-review/tasks',
            { params }
        );
        return response.data;
    },

    /**
     * 执行复核
     */
    executeReview: async (request: ExecuteReviewRequest): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/access-review/execute', request);
        return response.data;
    },
};

