// Input: API client 与 ApiResponse 类型
// Output: licenseApi
// Pos: 授权许可 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

export interface LicenseInfo {
    expireAt: string;
    maxUsers: number;
    nodeLimit: number;
    raw?: string;
}

export const licenseApi = {
    load: async (licenseText: string) => {
        const response = await client.post<ApiResponse<LicenseInfo>>('/license/load', licenseText, {
            headers: { 'Content-Type': 'text/plain' }
        });
        return response.data;
    },
    getCurrent: async () => {
        const response = await client.get<ApiResponse<LicenseInfo>>('/license');
        return response.data;
    }
};
