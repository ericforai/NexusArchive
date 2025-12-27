// Input: API Client
// Output: OriginalVoucher API Methods & Types
// Pos: 原始凭证数据交互层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client as apiClient } from './client';

// 类型定义
export interface OriginalVoucher {
    id: string;
    voucherNo: string;
    voucherCategory: string;
    voucherType: string;
    businessDate: string;
    amount?: number;
    currency: string;
    counterparty?: string;
    summary?: string;
    creator?: string;
    auditor?: string;
    bookkeeper?: string;
    approver?: string;
    sourceSystem?: string;
    sourceDocId?: string;
    fondsCode: string;
    fiscalYear: string;
    retentionPeriod: string;
    archiveStatus: string;
    archivedTime?: string;
    version: number;
    isLatest: boolean;
    createdTime: string;
}

export interface OriginalVoucherFile {
    id: string;
    voucherId: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    storagePath: string;
    fileHash: string;
    hashAlgorithm: string;
    fileRole: string;
    sequenceNo: number;
    createdTime: string;
}

export interface VoucherRelation {
    id: string;
    originalVoucherId: string;
    accountingVoucherId: string;
    relationType: string;
    relationDesc?: string;
    createdTime: string;
}

export interface OriginalVoucherType {
    id: string;
    categoryCode: string;
    categoryName: string;
    typeCode: string;
    typeName: string;
    defaultRetention: string;
    sortOrder: number;
    enabled: boolean;
}

export interface OriginalVoucherStats {
    total: number;
    archived: number;
    pending: number;
    draft: number;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

// API 函数

/**
 * 分页查询原始凭证
 */
export async function getOriginalVouchers(params: {
    page?: number;
    limit?: number;
    search?: string;
    category?: string;
    type?: string;
    status?: string;
    fondsCode?: string;
    fiscalYear?: string;
    /** 单据池状态过滤 (ENTRY,PARSED,PARSE_FAILED,MATCHED,ARCHIVED) */
    poolStatus?: string;
}): Promise<PageResult<OriginalVoucher>> {
    const { data } = await apiClient.get('/original-vouchers', { params });
    return data?.data || data;
}

/**
 * 获取原始凭证详情
 */
export async function getOriginalVoucher(id: string): Promise<OriginalVoucher> {
    const { data } = await apiClient.get(`/original-vouchers/${id}`);
    return data?.data || data;
}

/**
 * 获取原始凭证文件列表
 */
export async function getOriginalVoucherFiles(id: string): Promise<OriginalVoucherFile[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/files`);
    return data?.data || data || [];
}

/**
 * 获取版本历史
 */
export async function getVersionHistory(id: string): Promise<OriginalVoucher[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/versions`);
    return data?.data || data || [];
}

/**
 * 获取关联的记账凭证
 */
export async function getVoucherRelations(id: string): Promise<VoucherRelation[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/relations`);
    return data?.data || data || [];
}

/**
 * 创建原始凭证
 */
export async function createOriginalVoucher(voucher: Partial<OriginalVoucher>): Promise<OriginalVoucher> {
    const { data } = await apiClient.post('/original-vouchers', voucher);
    return data?.data || data;
}

/**
 * 更新原始凭证
 */
export async function updateOriginalVoucher(
    id: string,
    voucher: Partial<OriginalVoucher>,
    reason?: string
): Promise<OriginalVoucher> {
    const { data } = await apiClient.put(`/original-vouchers/${id}`, { voucher, reason });
    return data?.data || data;
}

/**
 * 删除原始凭证
 */
export async function deleteOriginalVoucher(id: string): Promise<void> {
    await apiClient.delete(`/original-vouchers/${id}`);
}

/**
 * 建立与记账凭证的关联
 */
export async function createVoucherRelation(
    originalVoucherId: string,
    accountingVoucherId: string,
    description?: string
): Promise<VoucherRelation> {
    const { data } = await apiClient.post(`/original-vouchers/${originalVoucherId}/relations`, {
        accountingVoucherId,
        description
    });
    return data?.data || data;
}

/**
 * 删除关联关系
 */
export async function deleteVoucherRelation(relationId: string): Promise<void> {
    await apiClient.delete(`/original-vouchers/relations/${relationId}`);
}

/**
 * 提交归档
 */
export async function submitForArchive(id: string): Promise<void> {
    await apiClient.post(`/original-vouchers/${id}/submit`);
}

/**
 * 确认归档
 */
export async function confirmArchive(id: string): Promise<void> {
    await apiClient.post(`/original-vouchers/${id}/confirm`);
}

/**
 * 获取所有原始凭证类型
 */
export async function getOriginalVoucherTypes(): Promise<OriginalVoucherType[]> {
    const { data } = await apiClient.get('/original-vouchers/types');
    // API 返回 Result<List<T>> 格式 { code, data, msg }，需要解包
    return data?.data || data || [];
}

/**
 * 获取原始凭证统计
 */
export async function getOriginalVoucherStats(params?: {
    fondsCode?: string;
    fiscalYear?: string;
}): Promise<OriginalVoucherStats> {
    const { data } = await apiClient.get('/original-vouchers/stats', { params });
    return data?.data || data;
}

/**
 * 添加原始凭证文件
 */
export async function addOriginalVoucherFile(
    id: string,
    file: File,
    fileRole?: string
): Promise<OriginalVoucherFile> {
    const formData = new FormData();
    formData.append('file', file);
    if (fileRole) {
        formData.append('fileRole', fileRole);
    }
    const { data } = await apiClient.post(`/original-vouchers/${id}/files`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
    return data?.data || data;
}

// 类型常量
export const VOUCHER_CATEGORIES = [
    { code: 'INVOICE', name: '发票类' },
    { code: 'BANK', name: '银行类' },
    { code: 'DOCUMENT', name: '单据类' },
    { code: 'CONTRACT', name: '合同类' },
    { code: 'OTHER', name: '其他类' }
];

export const ARCHIVE_STATUS = [
    { code: 'DRAFT', name: '草稿', color: 'gray' },
    { code: 'PENDING', name: '待归档', color: 'yellow' },
    { code: 'ARCHIVED', name: '已归档', color: 'green' },
    { code: 'FROZEN', name: '已冻结', color: 'red' }
];

// 统一导出 API 对象以支持全景视图的双路获取逻辑
export const originalVoucherApi = {
    getOriginalVouchers,
    getOriginalVoucher,
    getOriginalVoucherFiles,
    getVersionHistory,
    getVoucherRelations,
    createOriginalVoucher,
    updateOriginalVoucher,
    deleteOriginalVoucher,
    createVoucherRelation,
    deleteVoucherRelation,
    submitForArchive,
    confirmArchive,
    getOriginalVoucherTypes,
    getOriginalVoucherStats,
    addOriginalVoucherFile
};
