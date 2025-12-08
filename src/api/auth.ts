import { client } from './client';
import { ApiResponse } from '../types';
import { useAuthStore } from '../store';

export interface UserInfo {
    id: string;
    username: string;
    fullName?: string;
    email?: string;
    avatar?: string;
    departmentId?: string;
    status?: string;
    roles?: string[];
    permissions?: string[];
}

export interface LoginResponse {
    token: string;
    user: UserInfo;
}

export const authApi = {
    login: async (credentials: any) => {
        const response = await client.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
        return response.data;
    },
    getCurrentUser: async () => {
        const response = await client.get<ApiResponse<UserInfo>>('/auth/me');
        return response.data;
    },
    refresh: async () => {
        const response = await client.post<ApiResponse<LoginResponse>>('/auth/refresh');
        return response.data;
    },
    logout: async () => {
        try {
            await client.post('/auth/logout');
        } catch (e) {
            // 忽略登出时的异常
        }
        // 使用 AuthStore 清除登录状态
        useAuthStore.getState().logout();
    }
};

