// Input: API client 与 ApiResponse/PageResult
// Output: legacyImportApi
// Pos: 历史数据导入 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

export interface ImportResult {
    importId: string;
    totalRows: number;
    successRows: number;
    failedRows: number;
    errors: ImportError[];
    createdFondsNos: string[];
    createdEntityIds: string[];
    startTime: string;
    endTime: string;
    status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
    errorReportUrl?: string;
}

export interface ImportError {
    rowNumber: number;
    fieldName: string;
    errorCode: string;
    errorMessage: string;
}

export interface ImportPreviewResult {
    totalRows: number;
    validRows: number;
    invalidRows: number;
    previewData: ImportRowPreview[];
    errors: ImportError[];
    statistics: PreviewStatistics;
}

export interface ImportRowPreview {
    rowNumber: number;
    data: Record<string, any>;
    validationErrors: ImportError[];
}

export interface PreviewStatistics {
    fondsCount: number;
    entityCount: number;
    willCreateFonds: string[];
    willCreateEntities: string[];
}

export interface LegacyImportTask {
    id: string;
    operatorId: string;
    operatorName?: string;
    fondsNo: string;
    fileName: string;
    fileSize: number;
    fileHash?: string;
    totalRows: number;
    successRows: number;
    failedRows: number;
    status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'PARTIAL_SUCCESS';
    errorReportPath?: string;
    createdFondsNos?: string;
    createdEntityIds?: string;
    startedAt?: string;
    completedAt?: string;
    createdAt: string;
}

export interface FieldMappingConfig {
    fieldMappings?: Record<string, string>;
}

export const legacyImportApi = {
    /**
     * 预览导入数据
     */
    preview: async (file: File, mappingConfig?: FieldMappingConfig) => {
        const formData = new FormData();
        formData.append('file', file);
        if (mappingConfig) {
            formData.append('mappingConfig', JSON.stringify(mappingConfig));
        }
        const response = await client.post<ApiResponse<ImportPreviewResult>>(
            '/legacy-import/preview',
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        );
        return response.data;
    },

    /**
     * 执行数据导入
     */
    import: async (file: File, mappingConfig?: FieldMappingConfig) => {
        const formData = new FormData();
        formData.append('file', file);
        if (mappingConfig) {
            formData.append('mappingConfig', JSON.stringify(mappingConfig));
        }
        const response = await client.post<ApiResponse<ImportResult>>(
            '/legacy-import/import',
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        );
        return response.data;
    },

    /**
     * 查询导入历史
     */
    getTasks: async (page: number = 1, size: number = 20, status?: string) => {
        const params: any = { page, size };
        if (status) {
            params.status = status;
        }
        const response = await client.get<ApiResponse<PageResult<LegacyImportTask>>>(
            '/legacy-import/tasks',
            { params }
        );
        return response.data;
    },

    /**
     * 获取导入任务详情
     */
    getTaskDetail: async (importId: string) => {
        const response = await client.get<ApiResponse<LegacyImportTask>>(
            `/legacy-import/tasks/${importId}`
        );
        return response.data;
    },

    /**
     * 下载错误报告
     */
    downloadErrorReport: async (importId: string) => {
        const response = await client.get(`/legacy-import/tasks/${importId}/error-report`, {
            responseType: 'blob',
        });
        return response.data;
    },

    /**
     * 下载CSV导入模板
     */
    downloadCsvTemplate: async () => {
        const response = await client.get('/legacy-import/template/csv', {
            responseType: 'blob',
        });
        return response.data;
    },

    /**
     * 下载Excel导入模板
     */
    downloadExcelTemplate: async () => {
        const response = await client.get('/legacy-import/template/excel', {
            responseType: 'blob',
        });
        return response.data;
    },
};

