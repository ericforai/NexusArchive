// Input: client 与文档协作/版本请求类型
// Output: 文档协作与版本管理 API
// Pos: 前端 API 模块

import { client } from './client';

export interface DocumentSection {
    id: string;
    projectId: string;
    title: string;
    content?: string;
    sortOrder?: number;
    assignment?: {
        id: string;
        sectionId: string;
        assigneeId: string;
        assigneeName?: string;
        assignedBy?: string;
        note?: string;
        active?: boolean;
        createdAt?: string;
    } | null;
    lock?: {
        id: string;
        sectionId: string;
        lockedBy: string;
        lockedByName?: string;
        reason?: string;
        active?: boolean;
        expiresAt?: string;
        createdAt?: string;
    } | null;
    reminders?: Array<{
        id: string;
        sectionId: string;
        message: string;
        remindAt: string;
        recipientId: string;
        recipientName?: string;
        createdBy?: string;
        delivered?: boolean;
        createdAt?: string;
    }>;
}

export interface DocumentVersion {
    id: string;
    projectId: string;
    versionName: string;
    description?: string;
    createdBy?: string;
    rolledBackBy?: string;
    rolledBackAt?: string;
    createdAt?: string;
}

export const documentsApi = {
    getSection(projectId: string, sectionId: string) {
        return client.get(`/documents/${projectId}/editor/sections/${sectionId}`);
    },

    updateSection(projectId: string, sectionId: string, payload: { title: string; content?: string; sortOrder?: number }) {
        return client.put(`/documents/${projectId}/editor/sections/${sectionId}`, payload);
    },

    createAssignment(projectId: string, payload: { sectionId: string; assigneeId: string; assigneeName?: string; note?: string; active?: boolean }) {
        return client.post(`/documents/${projectId}/editor/assignments`, payload);
    },

    createLock(projectId: string, payload: { sectionId: string; reason?: string; active?: boolean; expiresAt?: string }) {
        return client.post(`/documents/${projectId}/editor/locks`, payload);
    },

    createReminder(projectId: string, payload: { sectionId: string; message: string; remindAt: string; recipientId: string; recipientName?: string }) {
        return client.post(`/documents/${projectId}/editor/reminders`, payload);
    },

    listVersions(projectId: string) {
        return client.get(`/documents/${projectId}/versions`);
    },

    createVersion(projectId: string, payload: { versionName: string; description?: string }) {
        return client.post(`/documents/${projectId}/versions`, payload);
    },

    rollbackVersion(projectId: string, versionId: string) {
        return client.post(`/documents/${projectId}/versions/${versionId}/rollback`);
    },
};
