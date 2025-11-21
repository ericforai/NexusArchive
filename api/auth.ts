import { client } from './client';
import { ApiResponse } from '../types';

export interface LoginResponse {
    token: string;
    user: {
        id: string;
        username: string;
        role: string;
    };
}

export const authApi = {
    login: async (credentials: any) => {
        const response = await client.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
        return response.data;
    },
    register: async (data: any) => {
        // 后端暂未实现注册接口
        // const response = await client.post('/auth/register', data);
        // return response.data;
        return {};
    },
    getCurrentUser: async () => {
        const response = await client.get<ApiResponse<any>>('/auth/me');
        return response.data;
    },
    logout: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
};
