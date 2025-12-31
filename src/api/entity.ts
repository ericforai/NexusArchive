// Input: API client 与 ApiResponse
// Output: entityApi
// Pos: 法人管理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

/**
 * 法人实体
 */
export interface SysEntity {
    id: string;
    name: string;
    taxId?: string;
    address?: string;
    contactPerson?: string;
    contactPhone?: string;
    contactEmail?: string;
    status: 'ACTIVE' | 'INACTIVE';
    description?: string;
    createdBy?: string;
    createdTime?: string;
    updatedTime?: string;
}

export const entityApi = {
    /**
     * 分页查询法人列表
     */
    getPage: async (page: number = 1, limit: number = 10) => {
        const response = await client.get<ApiResponse<PageResult<SysEntity>>>(`/entity/page`, {
            params: { page, limit },
        });
        return response.data;
    },

    /**
     * 查询法人列表
     */
    list: async () => {
        const response = await client.get<ApiResponse<SysEntity[]>>(`/entity/list`);
        return response.data;
    },

    /**
     * 查询活跃法人列表
     */
    listActive: async () => {
        const response = await client.get<ApiResponse<SysEntity[]>>(`/entity/list/active`);
        return response.data;
    },

    /**
     * 查询法人详情
     */
    getById: async (id: string) => {
        const response = await client.get<ApiResponse<SysEntity>>(`/entity/${id}`);
        return response.data;
    },

    /**
     * 查询法人下的全宗列表
     */
    getFondsIds: async (id: string) => {
        const response = await client.get<ApiResponse<string[]>>(`/entity/${id}/fonds`);
        return response.data;
    },

    /**
     * 检查法人是否可以删除
     */
    canDelete: async (id: string) => {
        const response = await client.get<ApiResponse<boolean>>(`/entity/${id}/can-delete`);
        return response.data;
    },

    /**
     * 创建法人
     */
    save: async (data: Partial<SysEntity>) => {
        const response = await client.post<ApiResponse<boolean>>('/entity', data);
        return response.data;
    },

    /**
     * 更新法人
     */
    update: async (data: Partial<SysEntity>) => {
        const response = await client.put<ApiResponse<boolean>>('/entity', data);
        return response.data;
    },

    /**
     * 删除法人
     */
    remove: async (id: string) => {
        const response = await client.delete<ApiResponse<boolean>>(`/entity/${id}`);
        return response.data;
    },
};

