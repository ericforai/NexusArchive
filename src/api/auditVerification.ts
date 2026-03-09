// Input: API client 与 ApiResponse
// Output: auditVerificationApi
// Pos: 审计验真 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 验证结果
 */
export interface VerificationResult {
    valid: boolean;
    logId: string;
    issueType?: 'OK' | 'MISSING_LOG' | 'BROKEN_CHAIN' | 'HASH_MISMATCH';
    expectedHash?: string;
    actualHash?: string;
    reason?: string;
    verifiedAt?: string;
}

/**
 * 哈希链验证结果
 */
export interface ChainVerificationResult {
    chainIntact: boolean;
    totalLogs: number;
    validLogs: number;
    invalidLogs: number;
    missingLogs?: number;
    brokenChainLogs?: number;
    tamperedLogs?: number;
    invalidResults: VerificationResult[];
    verifiedAt?: string;
}

/**
 * 抽检标准
 */
export interface SamplingCriteria {
    userId?: string;
    action?: string;
    resourceType?: string;
    fondsNo?: string;
    startDate?: string;
    endDate?: string;
}

/**
 * 抽检结果
 */
export interface SamplingResult {
    totalLogs: number;
    sampledLogs: number;
    sampledLogIds: string[];
    verificationResult: ChainVerificationResult;
}

export const auditVerificationApi = {
    /**
     * 验证单条审计日志
     */
    verifySingle: async (logId: string): Promise<ApiResponse<VerificationResult>> => {
        const response = await client.post<ApiResponse<VerificationResult>>(
            '/audit-log/verify',
            null,
            { params: { logId } }
        );
        return response.data;
    },

    /**
     * 验证审计日志哈希链（按时间范围）
     */
    verifyChain: async (
        startDate: string,
        endDate: string,
        fondsNo?: string
    ): Promise<ApiResponse<ChainVerificationResult>> => {
        const params: any = { startDate, endDate };
        if (fondsNo) {
            params.fondsNo = fondsNo;
        }
        const response = await client.post<ApiResponse<ChainVerificationResult>>(
            '/audit-log/verify-chain',
            null,
            { params }
        );
        return response.data;
    },

    /**
     * 验证指定日志ID列表的哈希链
     */
    verifyChainByIds: async (logIds: string[]): Promise<ApiResponse<ChainVerificationResult>> => {
        const response = await client.post<ApiResponse<ChainVerificationResult>>(
            '/audit-log/verify-chain-by-ids',
            logIds
        );
        return response.data;
    },

    /**
     * 抽检验真
     */
    sampleVerify: async (
        sampleSize: number,
        startDate?: string,
        endDate?: string,
        criteria?: SamplingCriteria
    ): Promise<ApiResponse<SamplingResult>> => {
        const params: any = { sampleSize };
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
        
        const response = await client.post<ApiResponse<SamplingResult>>(
            '/audit-log/sample-verify',
            criteria || null,
            { params }
        );
        return response.data;
    },

    /**
     * 导出审计证据包
     * 
     * @returns Promise<Blob> - 证据包文件（ZIP格式）
     */
    exportEvidencePackage: async (
        startDate: string,
        endDate: string,
        fondsNo?: string,
        includeVerificationReport: boolean = true
    ): Promise<Blob> => {
        const params: any = { startDate, endDate, includeVerificationReport };
        if (fondsNo) {
            params.fondsNo = fondsNo;
        }
        const response = await client.post(
            '/audit-log/export-evidence',
            null,
            {
                params,
                responseType: 'blob'
            }
        );
        return response.data;
    }
};
