// Input: API client、ApiResponse/PageResult 与 FormData
// Output: adminApi
// Pos: 后台管理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

// 全宗信息
export interface FondsInfo {
    fondsCode: string;
    fondsName: string;
    companyName: string;
}

// 用户全宗权限响应
export interface FondsScopeResponse {
    userId: string;
    assignedFonds: string[];
    availableFonds: FondsInfo[];
}

// 更新用户全宗权限请求
export interface UpdateUserFondsScopeRequest {
    fondsCodes: string[];
}

export const adminApi = {
    // 组织架构
    getOrgTree: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/org/tree');
        return response.data;
    },
    listOrg: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/org');
        return response.data;
    },
    createOrg: async (data: any) => {
        const response = await client.post<ApiResponse<any>>('/admin/org', data);
        return response.data;
    },
    updateOrg: async (id: string, data: any) => {
        const response = await client.put<ApiResponse<any>>(`/admin/org/${id}`, data);
        return response.data;
    },
    updateOrgOrder: async (id: string, orderNum: number) => {
        const response = await client.put<ApiResponse<void>>(`/admin/org/${id}/order`, null, { params: { orderNum } });
        return response.data;
    },
    deleteOrg: async (id: string) => {
        const response = await client.delete<ApiResponse<void>>(`/admin/org/${id}`);
        return response.data;
    },
    bulkOrg: async (items: any[]) => {
        const response = await client.post<ApiResponse<void>>('/admin/org/bulk', items);
        return response.data;
    },
    importOrg: async (file: File) => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await client.post<ApiResponse<any>>('/admin/org/import', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
        return response.data;
    },
    downloadOrgTemplate: async () => {
        const response = await client.get<ApiResponse<any>>('/admin/org/import/template');
        return response.data;
    },
    // 从 ERP 同步组织架构
    syncOrgFromErp: async () => {
        const response = await client.post<ApiResponse<{
            success: boolean;
            message: string;
            successCount: number;
            errorCount: number;
            errors: string[];
        }>>('/admin/org/sync');
        return response.data;
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
    resetPassword: async (id: string, password: string) => {
        const response = await client.post<ApiResponse<void>>(`/admin/users/${id}/reset-password`, { newPassword: password });
        return response.data;
    },
    toggleUserStatus: async (id: string, status: string) => {
        const response = await client.put<ApiResponse<void>>(`/admin/users/${id}/status`, { status });
        return response.data;
    },
    // 获取用户全宗权限
    getUserFondsScope: async (id: string) => {
        const response = await client.get<ApiResponse<FondsScopeResponse>>(`/admin/users/${id}/fonds-scope`);
        return response.data;
    },
    // 更新用户全宗权限
    updateUserFondsScope: async (id: string, fondsCodes: string[]) => {
        const response = await client.put<ApiResponse<void>>(`/admin/users/${id}/fonds-scope`, { fondsCodes });
        return response.data;
    },
    // 获取全宗列表（用于创建用户时选择）
    getFondsList: async () => {
        const response = await client.get<ApiResponse<any[]>>('/bas/fonds/list');
        return response.data;
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
        const response = await client.get<ApiResponse<any[]>>('/admin/permissions');
        return response.data;
    },

    // 岗位管理
    getPositions: async (params?: any) => {
        const response = await client.get<ApiResponse<PageResult<any>>>('/admin/positions', { params });
        return response.data;
    },
    createPosition: async (data: any) => {
        const response = await client.post<ApiResponse<any>>('/admin/positions', data);
        return response.data;
    },
    updatePosition: async (data: any) => {
        const response = await client.put<ApiResponse<any>>('/admin/positions', data);
        return response.data;
    },
    deletePosition: async (id: string) => {
        const response = await client.delete<ApiResponse<void>>(`/admin/positions/${id}`);
        return response.data;
    },

    // 权限管理 (Admin)
    listPermissions: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/permissions');
        return response.data;
    },
    createPermission: async (data: any) => {
        const response = await client.post<ApiResponse<any>>('/admin/permissions', data);
        return response.data;
    },

    // 组织增强
    bulkCreateOrg: async (data: any[]) => {
        const response = await client.post<ApiResponse<void>>('/admin/org/bulk', data);
        return response.data;
    },

    // 系统设置 (暂未实现后端接口)
    getSettings: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/settings');
        return response.data;
    },
    updateSettings: async (settings: any[]) => {
        const response = await client.put<ApiResponse<void>>('/admin/settings', { settings });
        return response.data;
    },

    // Workflow Management (暂未实现后端接口)
    getWorkflows: () => Promise.resolve({ data: [] }), // client.get('/admin/workflows'),
    createWorkflow: (data: any) => {
        void data;
        return Promise.resolve({ data: {} });
    }, // client.post('/admin/workflows', data),
    updateWorkflow: (id: string, data: any) => {
        void id;
        void data;
        return Promise.resolve({ data: {} });
    }, // client.put(`/admin/workflows/${id}`, data),
    deleteWorkflow: (id: string) => {
        void id;
        return Promise.resolve({ data: {} });
    }, // client.delete(`/admin/workflows/${id}`),

    // Certificate Management
    getCertificates: async () => {
        const response = await client.get<ApiResponse<any[]>>('/admin/certificates');
        return response.data;
    },
    getCertificate: async (alias: string) => {
        const response = await client.get<ApiResponse<any>>(`/admin/certificates/${alias}`);
        return response.data;
    },
    verifyCertificate: async (alias: string) => {
        const response = await client.post<ApiResponse<any>>(`/admin/certificates/${alias}/verify`);
        return response.data;
    },
};
