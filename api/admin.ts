import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface User {
    id: string;
    username: string;
    fullName: string;
    email?: string;
    phone?: string;
    departmentId?: string;
    status: string;
    roleIds?: string[];
    createdAt?: string;
}

export interface Role {
    id: string;
    name: string;
    code: string;
    roleCategory: string;
    isExclusive: boolean;
    description?: string;
    permissions?: string; // JSON string
    dataScope?: string;
    type: string;
    createdAt?: string;
}

export const adminApi = {
    // 组织架构 (暂未实现后端接口，保留占位)
    getOrgTree: async () => {
        // const response = await client.get('/admin/org/tree');
        // return response.data;
        return [];
    },
    createDepartment: async (data: any) => {
        // const response = await client.post('/admin/departments', data);
        // return response.data;
        return {};
    },
    updateDepartment: async (id: string, data: any) => {
        // const response = await client.put(`/admin/departments/${id}`, data);
        // return response.data;
        return {};
    },
    deleteDepartment: async (id: string) => {
        // const response = await client.delete(`/admin/departments/${id}`);
        // return response.data;
        return {};
    },

    // 人员管理
    getUsers: async (params?: any) => {
        const response = await client.get<ApiResponse<PageResult<User>>>('/admin/users', { params });
        return response.data;
    },
    createUser: async (data: Partial<User>) => {
        const response = await client.post<ApiResponse<User>>('/admin/users', data);
        return response.data;
    },
    updateUser: async (id: string, data: Partial<User>) => {
        const response = await client.put<ApiResponse<void>>(`/admin/users/${id}`, data);
        return response.data;
    },
    resetPassword: async (id: string, password?: string) => {
        // 后端暂未实现重置密码接口，暂时注释
        // const response = await client.post(`/admin/users/${id}/reset-password`, { newPassword: password });
        // return response.data;
        return {};
    },
    toggleUserStatus: async (id: string, status: string) => {
        // 后端暂未实现状态切换接口，暂时注释
        // const response = await client.put(`/admin/users/${id}/status`, { status });
        // return response.data;
        return {};
    },

    // 角色管理
    getRoles: async (params?: any) => {
        const response = await client.get<ApiResponse<PageResult<Role>>>('/admin/roles', { params });
        return response.data;
    },
    createRole: async (data: Partial<Role>) => {
        const response = await client.post<ApiResponse<Role>>('/admin/roles', data);
        return response.data;
    },
    updateRole: async (id: string, data: Partial<Role>) => {
        const response = await client.put<ApiResponse<void>>(`/admin/roles/${id}`, data);
        return response.data;
    },
    deleteRole: async (id: string) => {
        const response = await client.delete<ApiResponse<void>>(`/admin/roles/${id}`);
        return response.data;
    },
    getPermissions: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/roles/permissions');
        return response.data;
    },

    // 系统设置 (暂未实现后端接口)
    getSettings: async () => {
        // const response = await client.get('/admin/settings');
        // return response.data;
        return [];
    },
    updateSettings: async (settings: any[]) => {
        // const response = await client.put('/admin/settings', { settings });
        // return response.data;
        return {};
    },

    // Workflow Management (暂未实现后端接口)
    getWorkflows: () => Promise.resolve({ data: [] }), // client.get('/admin/workflows'),
    createWorkflow: (data: any) => Promise.resolve({ data: {} }), // client.post('/admin/workflows', data),
    updateWorkflow: (id: string, data: any) => Promise.resolve({ data: {} }), // client.put(`/admin/workflows/${id}`, data),
    deleteWorkflow: (id: string) => Promise.resolve({ data: {} }), // client.delete(`/admin/workflows/${id}`),
};
