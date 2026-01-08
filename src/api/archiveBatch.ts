// Input: API client、ApiResponse 类型
// Output: archiveBatchApi
// Pos: 归档批次 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

// ========== 类型定义 ==========

export interface ArchiveBatch {
    id: number;
    batchNo: string;
    fondsId: number;
    periodStart: string;
    periodEnd: string;
    scopeType: 'PERIOD' | 'CUSTOM';
    status: BatchStatus;
    voucherCount: number;
    docCount: number;
    fileCount: number;
    totalSizeBytes: number;
    validationReport?: ValidationReport;
    integrityReport?: IntegrityReport;
    errorMessage?: string;
    submittedBy?: number;
    submittedAt?: string;
    approvedBy?: number;
    approvedAt?: string;
    approvalComment?: string;
    archivedAt?: string;
    archivedBy?: number;
    createdBy?: number;
    createdTime: string;
    lastModifiedTime: string;
}

export type BatchStatus =
    | 'PENDING'      // 待提交
    | 'VALIDATING'   // 校验中
    | 'APPROVED'     // 已审批
    | 'ARCHIVED'     // 已归档
    | 'REJECTED'     // 已驳回
    | 'FAILED';      // 失败

export interface ArchiveBatchItem {
    id: number;
    batchId: number;
    itemType: 'VOUCHER' | 'SOURCE_DOC';
    refId: number;
    refNo?: string;
    status: 'PENDING' | 'VALIDATED' | 'ARCHIVED' | 'FAILED';
    validationResult?: Record<string, unknown>;
    hashSm3?: string;
    createdTime: string;
}

export interface CreateBatchRequest {
    fondsId: number;
    periodStart: string;
    periodEnd: string;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

export interface BatchStats {
    total: number;
    byStatus: Record<BatchStatus, number>;
}

export interface ValidationReport {
    batchId: number;
    batchNo: string;
    validatedAt: string;
    totalItems: number;
    validatedItems: number;
    errorCount: number;
    warningCount: number;
    errors: Array<Record<string, unknown>>;
    warnings: Array<Record<string, unknown>>;
}

export interface IntegrityReport {
    batchId: number;
    batchNo: string;
    checkedAt: string;
    checks: Array<{
        checkType: string;
        name: string;
        result: 'PASS' | 'FAIL' | 'WARNING';
        details?: unknown;
    }>;
    overallResult: 'PASS' | 'FAIL';
}

// ========== API 接口 ==========

export const archiveBatchApi = {
    // ========== 批次管理 ==========

    // 创建归档批次
    createBatch: async (request: CreateBatchRequest): Promise<ArchiveBatch> => {
        const response = await client.post<ApiResponse<ArchiveBatch>>('/archive-batch', request);
        return response.data.data;
    },

    // 获取批次详情
    getBatch: async (batchId: number): Promise<ArchiveBatch> => {
        const response = await client.get<ApiResponse<ArchiveBatch>>(`/archive-batch/${batchId}`);
        return response.data.data;
    },

    // 分页查询批次
    listBatches: async (
        page: number = 1,
        size: number = 20,
        fondsId?: number,
        status?: BatchStatus
    ): Promise<PageResult<ArchiveBatch>> => {
        const params = new URLSearchParams();
        params.append('page', String(page));
        params.append('size', String(size));
        if (fondsId) params.append('fondsId', String(fondsId));
        if (status) params.append('status', status);

        const response = await client.get<ApiResponse<PageResult<ArchiveBatch>>>(
            `/archive-batch?${params.toString()}`
        );
        return response.data.data;
    },

    // 删除批次
    deleteBatch: async (batchId: number): Promise<void> => {
        await client.delete(`/archive-batch/${batchId}`);
    },

    // ========== 批次条目管理 ==========

    // 添加凭证到批次
    addVouchers: async (batchId: number, voucherIds: number[]): Promise<number> => {
        const response = await client.post<ApiResponse<number>>(
            `/archive-batch/${batchId}/vouchers`,
            voucherIds
        );
        return response.data.data;
    },

    // 添加单据到批次
    addDocs: async (batchId: number, docIds: number[]): Promise<number> => {
        const response = await client.post<ApiResponse<number>>(
            `/archive-batch/${batchId}/docs`,
            docIds
        );
        return response.data.data;
    },

    // 从批次移除条目
    removeItem: async (batchId: number, itemId: number): Promise<void> => {
        await client.delete(`/archive-batch/${batchId}/items/${itemId}`);
    },

    // 获取批次条目列表
    getItems: async (batchId: number, itemType?: string): Promise<ArchiveBatchItem[]> => {
        const params = itemType ? `?itemType=${itemType}` : '';
        const response = await client.get<ApiResponse<ArchiveBatchItem[]>>(
            `/archive-batch/${batchId}/items${params}`
        );
        return response.data.data;
    },

    // ========== 归档流程 ==========

    // 提交批次进行校验
    submitBatch: async (batchId: number): Promise<ArchiveBatch> => {
        const response = await client.post<ApiResponse<ArchiveBatch>>(
            `/archive-batch/${batchId}/submit`
        );
        return response.data.data;
    },

    // 执行批次校验
    validateBatch: async (batchId: number): Promise<ValidationReport> => {
        const response = await client.post<ApiResponse<ValidationReport>>(
            `/archive-batch/${batchId}/validate`
        );
        return response.data.data;
    },

    // 审批通过
    approveBatch: async (batchId: number, comment?: string): Promise<ArchiveBatch> => {
        const response = await client.post<ApiResponse<ArchiveBatch>>(
            `/archive-batch/${batchId}/approve`,
            { comment }
        );
        return response.data.data;
    },

    // 审批驳回
    rejectBatch: async (batchId: number, comment: string): Promise<ArchiveBatch> => {
        const response = await client.post<ApiResponse<ArchiveBatch>>(
            `/archive-batch/${batchId}/reject`,
            { comment }
        );
        return response.data.data;
    },

    // 执行归档
    executeBatchArchive: async (batchId: number): Promise<ArchiveBatch> => {
        const response = await client.post<ApiResponse<ArchiveBatch>>(
            `/archive-batch/${batchId}/archive`
        );
        return response.data.data;
    },

    // ========== 四性检测 ==========

    // 执行四性检测
    runIntegrityCheck: async (batchId: number): Promise<IntegrityReport> => {
        const response = await client.post<ApiResponse<IntegrityReport>>(
            `/archive-batch/${batchId}/integrity-check`
        );
        return response.data.data;
    },

    // ========== 统计 ==========

    // 获取批次统计
    getStats: async (fondsId?: number): Promise<BatchStats> => {
        const params = fondsId ? `?fondsId=${fondsId}` : '';
        const response = await client.get<ApiResponse<BatchStats>>(
            `/archive-batch/stats${params}`
        );
        return response.data.data;
    },

    // ========== 批量审批 ==========

    // 批量审批通过
    batchApprove: async (request: {
        batchIds: number[];
        operatorId?: string;
        operatorName?: string;
        comment?: string;
    }): Promise<{ success: number; failed: number; errors?: Array<{ id: number; reason: string }> }> => {
        const response = await client.post<
            ApiResponse<{ success: number; failed: number; errors?: Array<{ id: number; reason: string }> }>
        >('/collection/batch/batch-approve', request);
        return response.data.data;
    },

    // 批量审批驳回
    batchReject: async (request: {
        batchIds: number[];
        operatorId?: string;
        operatorName?: string;
        comment: string;
    }): Promise<{ success: number; failed: number; errors?: Array<{ id: number; reason: string }> }> => {
        const response = await client.post<
            ApiResponse<{ success: number; failed: number; errors?: Array<{ id: number; reason: string }> }>
        >('/collection/batch/batch-reject', request);
        return response.data.data;
    },
};

export default archiveBatchApi;
