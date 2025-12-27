// Input: API client 与 FormData
// Output: attachmentsApi
// Pos: 附件上传/下载 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 附件关联 API
 * 用于全景视图中凭证与附件（发票/合同/回单）的关联管理
 */

import { client } from './client';

export interface AttachmentFile {
    id: string;
    archivalCode: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    fileHash: string;
    storagePath: string;
    docType?: string; // invoice, contract, bank_slip, other
    createdTime?: string;
    highlightMeta?: string; // JSON string containing coordinate data
}

export interface AttachmentLink {
    id: string;
    archiveId: string;
    fileId: string;
    attachmentType: string;
    relationDesc?: string;
    createdBy?: string;
    createdAt?: string;
}

interface ApiResponse<T> {
    code: number;
    message?: string;
    data: T;
}

export const attachmentsApi = {
    /**
     * 获取档案关联的所有附件文件（包含类型信息）
     */
    getByArchive: async (archiveId: string): Promise<AttachmentFile[]> => {
        try {
            // 同时获取关联记录和文件列表
            const [linksRes, filesRes] = await Promise.all([
                client.get<ApiResponse<AttachmentLink[]>>(`/attachments/links/${archiveId}`),
                client.get<ApiResponse<AttachmentFile[]>>(`/attachments/by-archive/${archiveId}`)
            ]);

            const links = linksRes.data.data || [];
            const files = filesRes.data.data || [];

            // 合并附件类型信息
            return files.map(file => {
                const link = links.find(l => l.fileId === file.id);
                return {
                    ...file,
                    docType: link?.attachmentType || 'other'
                };
            });
        } catch (error) {
            console.error('Failed to fetch attachments', error);
            return [];
        }
    },

    /**
     * 获取附件关联记录
     */
    getLinks: async (archiveId: string): Promise<AttachmentLink[]> => {
        const res = await client.get<ApiResponse<AttachmentLink[]>>(`/attachments/links/${archiveId}`);
        return res.data.data || [];
    },

    /**
     * 关联已有文件到档案
     */
    link: async (archiveId: string, fileId: string, attachmentType: string = 'other'): Promise<AttachmentLink> => {
        const res = await client.post<ApiResponse<AttachmentLink>>('/attachments/link', {
            archiveId,
            fileId,
            attachmentType
        });
        return res.data.data;
    },

    /**
     * 上传附件并关联到档案
     */
    uploadAndLink: async (
        archiveId: string,
        file: File,
        attachmentType: string = 'other'
    ): Promise<AttachmentFile> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('archiveId', archiveId);
        formData.append('attachmentType', attachmentType);

        const res = await client.post<ApiResponse<AttachmentFile>>('/attachments/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return res.data.data;
    },

    /**
     * 删除附件关联
     */
    unlink: async (attachmentId: string): Promise<void> => {
        await client.delete(`/attachments/${attachmentId}`);
    },

    /**
     * 构建文件下载 URL
     */
    getDownloadUrl: (fileId: string): string => {
        return `/api/archive/files/download/${fileId}`;
    }
};
