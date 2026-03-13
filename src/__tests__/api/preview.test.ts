// Input: vitest、本地模块 api/preview、api/client、store
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../store', () => ({
    useAuthStore: {
        getState: vi.fn(),
        subscribe: vi.fn(),
    },
    useFondsStore: {
        getState: vi.fn(),
        subscribe: vi.fn(),
    },
}));

import { client } from '../../api/client';
import { previewApi } from '../../api/preview';
import { useAuthStore, useFondsStore } from '../../store';

describe('previewApi', () => {
    let originalAdapter: any;

    beforeEach(() => {
        originalAdapter = client.defaults.adapter;
        (useAuthStore.getState as any).mockReturnValue({
            token: 'test-token',
            logout: vi.fn(),
        });
        (useFondsStore.getState as any).mockReturnValue({
            getCurrentFondsCode: () => 'F001',
        });
    });

    afterEach(() => {
        client.defaults.adapter = originalAdapter;
        vi.clearAllMocks();
    });

    it('should parse watermark headers in stream preview', async () => {
        client.defaults.adapter = async (config) => {
            return {
                data: new Blob(['preview']),
                status: 200,
                statusText: 'OK',
                headers: {
                    'x-trace-id': 'trace-123',
                    'x-watermark-text': 'user-1 2025-01-01 trace-123',
                    'x-watermark-subtext': 'trace-123 | Fonds:F001',
                    'x-watermark-opacity': '0.2',
                    'x-watermark-rotate': '-40',
                },
                config,
            };
        };

        const result = await previewApi.getPreview({
            resourceType: 'archiveMain',
            archiveId: 'A1',
            mode: 'stream',
        });

        expect(result.mode).toBe('stream');
        expect(result.traceId).toBe('trace-123');
        expect(result.watermark?.text).toBe('user-1 2025-01-01 trace-123');
        expect(result.watermark?.subText).toBe('trace-123 | Fonds:F001');
        expect(result.watermark?.opacity).toBe(0.2);
        expect(result.watermark?.rotate).toBe(-40);
    });

    it('should return presigned metadata', async () => {
        client.defaults.adapter = async (config) => {
            return {
                data: {
                    data: {
                        presignedUrl: 'https://example.com/presigned',
                        expiresAt: '2025-01-02T00:00:00Z',
                        traceId: 'trace-456',
                        watermark: {
                            text: 'user-2 2025-01-02 trace-456',
                            subText: 'trace-456 | Fonds:F002',
                            opacity: 0.3,
                            rotate: -45,
                        },
                    },
                },
                status: 200,
                statusText: 'OK',
                headers: {},
                config,
            };
        };

        const result = await previewApi.getPreview({
            resourceType: 'archiveMain',
            archiveId: 'A2',
            mode: 'presigned',
        });

        expect(result.mode).toBe('presigned');
        if (result.mode === 'presigned') {
            expect(result.presignedUrl).toBe('https://example.com/presigned');
        }
        expect(result.traceId).toBe('trace-456');
        expect(result.watermark?.text).toBe('user-2 2025-01-02 trace-456');
    });

    it('should use unified file preview contract when previewing attachments', async () => {
        let requestConfig: any;

        client.defaults.adapter = async (config) => {
            requestConfig = config;
            return {
                data: new Blob(['preview']),
                status: 200,
                statusText: 'OK',
                headers: {},
                config,
            };
        };

        await previewApi.getPreview({
            fileId: 'F3',
            resourceType: 'file',
            mode: 'stream',
        });

        expect(requestConfig?.params).toMatchObject({
            resourceType: 'file',
            fileId: 'F3',
            mode: 'stream',
        });
    });
});
