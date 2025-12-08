import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface Archive {
    id: string;
    fondsNo: string;
    archiveCode: string;
    categoryCode: string;
    title: string;
    amount?: number;
    fiscalYear: string;
    docDate?: string;
    retentionPeriod: string;
    orgName: string;
    status: string;
    securityLevel?: string;
    location?: string;
    createdAt?: string;
    createdTime?: string;
}

export interface ArchiveQuery {
    page?: number;
    limit?: number;
    search?: string;
    status?: string;
    categoryCode?: string;
    orgId?: string;
}

export const archivesApi = {
    getArchives: async (params?: ArchiveQuery) => {
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
    },

    /**
     * 导出 AIP 包为 ZIP 文件
     * @param archivalCode 档号
     */
    exportAipPackage: async (archivalCode: string) => {
        try {
            const response = await client.get(`/export/aip/${archivalCode}`, {
                responseType: 'blob'
            });

            // 创建下载链接
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `${archivalCode}.zip`);
            document.body.appendChild(link);
            link.click();

            // 清理
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            return { success: true };
        } catch (error) {
            console.error('导出 AIP 包失败:', error);
            throw error;
        }
    }
};

export const archivesApiEx = {
    getRecent: async (limit: number = 5) => {
        const response = await client.get<ApiResponse<Archive[]>>('/archives/recent', { params: { limit } });
        return response.data;
    }
};
