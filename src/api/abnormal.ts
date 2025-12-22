// Input: API client 与 ApiResponse 类型
// Output: 异常数据操作函数
// Pos: 异常数据处理 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
