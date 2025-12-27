// Input: vitest、本地模块 api/client、store
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { client } from '../../api/client';
import { useAuthStore } from '../../store';

// Mock the store
vi.mock('../../store', () => ({
    useAuthStore: {
        getState: vi.fn(),
        subscribe: vi.fn()
    }
}));

// Mock window location
const originalLocation = window.location;

describe('API Client Interceptors', () => {
    let mockLogout: any;
    let originalAdapter: any;

    beforeEach(() => {
        // Reset mocks
        mockLogout = vi.fn();
        (useAuthStore.getState as any).mockReturnValue({
            token: 'test-token',
            logout: mockLogout
        });

        // Mock window.location
        Object.defineProperty(window, 'location', {
            configurable: true,
            value: { href: '', pathname: '/' }
        });

        // Save original adapter
        originalAdapter = client.defaults.adapter;
    });

    afterEach(() => {
        Object.defineProperty(window, 'location', {
            configurable: true,
            value: originalLocation
        });
        // Restore adapter
        client.defaults.adapter = originalAdapter;
        vi.clearAllMocks();
    });

    it('should normalize error message from backend Result format', async () => {
        // Mock adapter to simulate network failure
        client.defaults.adapter = async (config) => {
            const err: any = new Error('Network Error');
            err.config = config;
            err.response = {
                status: 400,
                data: {
                    code: 400,
                    message: 'User friendly error',
                    data: null
                }
            };
            return Promise.reject(err);
        };

        try {
            await client.get('/test');
        } catch (error: any) {
            expect(error.message).toBe('User friendly error');
        }
    });

    it('should redirect to login on 401', async () => {
        // Mock adapter to simulate 401
        client.defaults.adapter = async (config) => {
            const err: any = new Error('Auth Error');
            err.config = config;
            err.response = {
                status: 401,
                data: { message: 'Unauthorized' }
            };
            return Promise.reject(err);
        };

        try {
            await client.get('/test');
        } catch {
            // Error is rethrown, we ignore it
        }

        expect(mockLogout).toHaveBeenCalled();
        expect(window.location.href).toBe('/system/login');
    });
});
