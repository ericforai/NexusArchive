import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface Archive {
    id: string;
    fondsNo: string;
    archiveCode: string;
    categoryCode: string;
    title: string;
    fiscalYear: string;
    retentionPeriod: string;
    orgName: string;
    status: string;
    securityLevel?: string;
    location?: string;
    createdAt?: string;
}

export const archivesApi = {
    getArchives: async (params?: any) => {
        const response = await client.get<ApiResponse<PageResult<Archive>>>('/archives', { params });
        return response.data;
    },
    createArchive: async (data: Partial<Archive>) => {
        const response = await client.post<ApiResponse<Archive>>('/archives', data);
        return response.data;
    },
    getArchiveById: async (id: string) => {
        const response = await client.get<ApiResponse<Archive>>(`/archives/${id}`);
        return response.data;
    },
    updateArchive: async (id: string, data: Partial<Archive>) => {
        const response = await client.put<ApiResponse<void>>(`/archives/${id}`, data);
        return response.data;
    },
    deleteArchive: async (id: string) => {
        const response = await client.delete<ApiResponse<void>>(`/archives/${id}`);
        return response.data;
    }
};
