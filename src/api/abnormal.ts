import { request } from '../utils/request';

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

export const getPendingAbnormals = () => {
    return request<AbnormalVoucher[]>({
        url: '/api/v1/abnormal',
        method: 'GET',
    });
};

export const retryAbnormal = (id: string) => {
    return request<void>({
        url: `/api/v1/abnormal/${id}/retry`,
        method: 'POST',
    });
};

export const updateAbnormalSip = (id: string, sipData: any) => {
    return request<void>({
        url: `/api/v1/abnormal/${id}`,
        method: 'PUT',
        data: sipData,
    });
};
