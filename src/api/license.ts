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
