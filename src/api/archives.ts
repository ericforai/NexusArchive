// Input: API client、ApiResponse/PageResult 与浏览器 Blob/URL
// Output: archivesApi 与 archivesApiEx
// Pos: 档案 CRUD 与导出 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
    creator?: string;
    customMetadata?: string; // JSON 格式的会计分录数据
    fileName?: string;
    erpVoucherNo?: string;
    voucherWord?: string;
    summary?: string;
}

export interface ArchiveQuery {
    page?: number;
    limit?: number;
    search?: string;
    status?: string;
    categoryCode?: string;
    orgId?: string;
    subType?: string; // Generic sub-type filter (e.g. bookType, reportType)
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
    getArchiveFiles: async (id: string) => {
        const response = await client.get<ApiResponse<any[]>>(`/archives/${id}/files`);
        return response.data; // Returns specific file list
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
    },

    /**
     * 获取档案关联的凭证分录数据
     * 用于凭证预览标签页
     * @param archiveId 档案ID
     */
    getVoucherData: async (archiveId: string): Promise<VoucherDataResponse | null> => {
        try {
            const response = await client.get<ApiResponse<VoucherDataResponse>>(`/archive/${archiveId}/voucher-data`);
            if (response.data.code === 200 && response.data.data) {
                return response.data.data;
            }
            return null;
        } catch (error) {
            console.error('获取凭证分录数据失败:', error);
            return null;
        }
    }
};

/**
 * 凭证分录数据响应
 */
export interface VoucherDataResponse {
    fileId: string;
    sourceData: string | null;
    voucherWord: string | null;
    summary: string | null;
    docDate: string | null;
    creator: string | null;
}

export const archivesApiEx = {
    getRecent: async (limit: number = 5) => {
        const response = await client.get<ApiResponse<Archive[]>>('/archives/recent', { params: { limit } });
        return response.data;
    }
};
