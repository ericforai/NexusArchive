// Input: API client 与 ApiResponse
// Output: mfaApi
// Pos: MFA 多因素认证 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * MFA设置响应
 */
export interface MfaSetupResponse {
    qrCodeUrl: string;
    secret: string;
    backupCodes: string[];
}

/**
 * MFA验证请求
 */
export interface MfaVerifyRequest {
    code: string; // TOTP码或备用码
}

/**
 * MFA状态
 */
export interface MfaStatus {
    enabled: boolean;
    setupRequired: boolean;
}

export const mfaApi = {
    /**
     * 获取MFA状态
     */
    getStatus: async (): Promise<ApiResponse<MfaStatus>> => {
        const response = await client.get<ApiResponse<MfaStatus>>('/mfa/status');
        return response.data;
    },

    /**
     * 设置MFA（生成二维码和备用码）
     */
    setup: async (): Promise<ApiResponse<MfaSetupResponse>> => {
        const response = await client.post<ApiResponse<MfaSetupResponse>>('/mfa/setup');
        return response.data;
    },

    /**
     * 验证TOTP码（完成设置）
     */
    verify: async (code: string): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/mfa/verify', { code });
        return response.data;
    },

    /**
     * 验证备用码
     */
    verifyBackup: async (code: string): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/mfa/verify-backup', { code });
        return response.data;
    },

    /**
     * 获取备用码
     */
    getBackupCodes: async (): Promise<ApiResponse<string[]>> => {
        const response = await client.get<ApiResponse<string[]>>('/mfa/backup-codes');
        return response.data;
    },

    /**
     * 禁用MFA
     */
    disable: async (): Promise<ApiResponse<void>> => {
        const response = await client.post<ApiResponse<void>>('/mfa/disable');
        return response.data;
    },
};


