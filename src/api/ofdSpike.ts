// Input: axios client、FormData、ArrayBuffer
// Output: OFD spike 调试 API 封装（liteofd / ofdrw 验证）
// Pos: src/api/ofdSpike.ts

import { client } from './client';

export interface OfdSpikeSample {
    voucherId: string;
    fileId: string;
    fileName: string;
    sourceType: 'ORIGINAL';
}

export const DEFAULT_OFD_SPIKE_SAMPLE: OfdSpikeSample = {
    voucherId: '1970e13f-7653-4740-9aaf-d0e873b91b7e',
    fileId: '5fe0368e-7f42-42da-9aa6-97009bd889dc',
    fileName: 'OV-2026-INV-000002.ofd',
    sourceType: 'ORIGINAL',
};

export const ofdSpikeApi = {
    async fetchOriginalVoucherFile(fileId: string): Promise<ArrayBuffer> {
        const response = await client.get<ArrayBuffer>(`/original-vouchers/files/download/${fileId}`, {
            responseType: 'arraybuffer',
        });
        return response.data;
    },

    async convertPdfToOfd(file: File): Promise<Blob> {
        const formData = new FormData();
        formData.append('file', file);

        const response = await client.post<Blob>('/debug/ofd-spike/convert-pdf', formData, {
            responseType: 'blob',
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        return response.data;
    },
};
