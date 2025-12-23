// 原始凭证 API 模块
// 用于与后端 /original-vouchers 端点交互

import { apiClient } from './client';

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
}): Promise<PageResult<OriginalVoucher>> {
    const { data } = await apiClient.get('/original-vouchers', { params });
    return data;
}

/**
 * 获取原始凭证详情
 */
export async function getOriginalVoucher(id: string): Promise<OriginalVoucher> {
    const { data } = await apiClient.get(`/original-vouchers/${id}`);
    return data;
}

/**
 * 获取原始凭证文件列表
 */
export async function getOriginalVoucherFiles(id: string): Promise<OriginalVoucherFile[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/files`);
    return data;
}

/**
 * 获取版本历史
 */
export async function getVersionHistory(id: string): Promise<OriginalVoucher[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/versions`);
    return data;
}

/**
 * 获取关联的记账凭证
 */
export async function getVoucherRelations(id: string): Promise<VoucherRelation[]> {
    const { data } = await apiClient.get(`/original-vouchers/${id}/relations`);
    return data;
}

/**
 * 创建原始凭证
 */
export async function createOriginalVoucher(voucher: Partial<OriginalVoucher>): Promise<OriginalVoucher> {
    const { data } = await apiClient.post('/original-vouchers', voucher);
    return data;
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
    return data;
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
    return data;
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
    return data;
}

/**
 * 获取原始凭证统计
 */
export async function getOriginalVoucherStats(params?: {
    fondsCode?: string;
    fiscalYear?: string;
}): Promise<OriginalVoucherStats> {
    const { data } = await apiClient.get('/original-vouchers/stats', { params });
    return data;
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
