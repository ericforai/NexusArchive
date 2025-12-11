import { client } from './client';
import { ApiResponse } from '../types';

export interface AbnormalVoucher {
    id: string;
    requestId: string;
    sourceSystem: string;
    voucherNumber: string;
    sipData: string;
    failReason: string;
    status: 'PENDING' | 'RETRYING' | 'IGNORED' | 'RESOLVED';
    createTime: string;
    updateTime: string;
}

export const getPendingAbnormals = async () => {
    const response = await client.get<ApiResponse<AbnormalVoucher[]>>('/v1/abnormal');
    // client (axios) returns AxiosResponse. The data is in response.data.
    return response.data;
};

export const retryAbnormal = async (id: string) => {
    const response = await client.post<void>(`/v1/abnormal/${id}/retry`);
    return response.data;
};

export const updateAbnormalSip = async (id: string, sipData: any) => {
    const response = await client.put<void>(`/v1/abnormal/${id}`, sipData);
    return response.data;
};
