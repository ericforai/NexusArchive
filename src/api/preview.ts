import { client } from './client';

export interface PreviewRequest {
    archiveId: string;
    fileId?: string;
    mode?: 'stream' | 'presigned' | 'rendered';
}

export interface WatermarkMetadata {
    text?: string;
    subText?: string;
    opacity?: number;
    rotate?: number;
}

export interface PreviewStreamResult {
    mode: 'stream' | 'rendered';
    blob: Blob;
    traceId?: string;
    watermark?: WatermarkMetadata;
}

export interface PreviewPresignedResult {
    mode: 'presigned';
    presignedUrl?: string;
    expiresAt?: string;
    traceId?: string;
    watermark?: WatermarkMetadata;
}

export type PreviewResult = PreviewStreamResult | PreviewPresignedResult;

export const previewApi = {
    /**
     * 预览已归档档案
     */
    getPreview: async (params: PreviewRequest): Promise<PreviewResult> => {
        const mode = params.mode || 'stream';
        if (mode === 'presigned') {
            const response = await client.post('/archive/preview/presigned', null, {
                params: {
                    archiveId: params.archiveId,
                    fileId: params.fileId,
                },
            });
            const data = response.data?.data || response.data;
            return {
                mode: 'presigned',
                presignedUrl: data?.presignedUrl || data?.presigned_url,
                expiresAt: data?.expiresAt || data?.expires_at,
                traceId: data?.traceId,
                watermark: data?.watermark,
            };
        }

        const response = await client.post('/archive/preview', null, {
            params: {
                archiveId: params.archiveId,
                fileId: params.fileId,
                mode,
            },
            responseType: 'blob',
        });

        const headers = response.headers || {};
        const traceId = headers['x-trace-id'];
        const opacity = headers['x-watermark-opacity'];
        const rotate = headers['x-watermark-rotate'];
        const watermark: WatermarkMetadata = {
            text: headers['x-watermark-text'],
            subText: headers['x-watermark-subtext'],
            opacity: opacity ? Number(opacity) : undefined,
            rotate: rotate ? Number(rotate) : undefined,
        };

        return {
            mode: mode === 'rendered' ? 'rendered' : 'stream',
            blob: response.data,
            traceId,
            watermark,
        };
    },

    /**
     * 预览记账凭证库文件（未归档）
     * @param fileId 文件ID
     */
    getPoolPreview: async (fileId: string): Promise<PreviewResult> => {
        const response = await client.get(`/pool/preview/${fileId}`, {
            responseType: 'blob',
        });

        const headers = response.headers || {};
        const traceId = headers['x-trace-id'];
        const opacity = headers['x-watermark-opacity'];
        const rotate = headers['x-watermark-rotate'];
        const watermark: WatermarkMetadata = {
            text: headers['x-watermark-text'],
            subText: headers['x-watermark-subtext'],
            opacity: opacity ? Number(opacity) : undefined,
            rotate: rotate ? Number(rotate) : undefined,
        };

        return {
            mode: 'stream',
            blob: response.data,
            traceId,
            watermark,
        };
    },
};
