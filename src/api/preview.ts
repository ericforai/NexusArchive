import { client } from './client';

export type PreviewResourceType = 'archiveMain' | 'file';

export interface PreviewRequest {
    resourceType?: PreviewResourceType;
    archiveId?: string;
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
    normalizeRequest: (params: PreviewRequest): Required<Pick<PreviewRequest, 'resourceType' | 'mode'>> & Pick<PreviewRequest, 'archiveId' | 'fileId'> => {
        const mode = params.mode || 'stream';
        if (params.resourceType) {
            return {
                resourceType: params.resourceType,
                archiveId: params.archiveId,
                fileId: params.fileId,
                mode,
            };
        }

        if (params.fileId && !params.archiveId) {
            return {
                resourceType: 'file',
                fileId: params.fileId,
                archiveId: undefined,
                mode,
            };
        }

        return {
            resourceType: 'archiveMain',
            archiveId: params.archiveId,
            fileId: params.fileId,
            mode,
        };
    },

    /**
     * 预览已归档档案
     */
    getPreview: async (params: PreviewRequest): Promise<PreviewResult> => {
        const normalized = previewApi.normalizeRequest(params);
        const mode = normalized.mode;
        if (mode === 'presigned') {
            const response = await client.post('/preview/presigned', null, {
                params: {
                    resourceType: normalized.resourceType,
                    archiveId: normalized.archiveId,
                    fileId: normalized.fileId,
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

        const response = await client.post('/preview', null, {
            params: {
                resourceType: normalized.resourceType,
                archiveId: normalized.archiveId,
                fileId: normalized.fileId,
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
