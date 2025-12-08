import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface BasFonds {
    id: string;
    fondsCode: string;
    fondsName: string;
    companyName?: string;
    description?: string;
    createdBy?: string;
    createdTime?: string;
    updatedTime?: string;
}

export const fondsApi = {
    getPage: async (page: number = 1, limit: number = 10) => {
        const response = await client.get<ApiResponse<PageResult<BasFonds>>>(`/bas/fonds/page`, { params: { page, limit } });
        return response.data;
    },

    list: async () => {
        const response = await client.get<ApiResponse<BasFonds[]>>(`/bas/fonds/list`);
        return response.data;
    },

    save: async (data: Partial<BasFonds>) => {
        const response = await client.post<ApiResponse<boolean>>('/bas/fonds', data);
        return response.data;
    },

    update: async (data: Partial<BasFonds>) => {
        const response = await client.put<ApiResponse<boolean>>('/bas/fonds', data);
        return response.data;
    },

    remove: async (id: string) => {
        const response = await client.delete<ApiResponse<boolean>>(`/bas/fonds/${id}`);
        return response.data;
    }
};
