// Input: axios 与浏览器 location
// Output: 带拦截器的 HTTP client 实例
// Pos: 前端 API 请求基础封装

import axios from 'axios';
import { getHttpClientState, performLogout } from './client.types';

const API_URL = '/api';

export const client = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add token and fonds code
client.interceptors.request.use(
    (config) => {
        // Fix double /api prefix if it exists
        if (config.url?.startsWith('/api/api/')) {
            config.url = config.url.replace('/api/api/', '/api/');
        } else if (config.url?.startsWith('api/api/')) {
            config.url = config.url.replace('api/api/', 'api/');
        }

        try {
            // 通过延迟绑定的接口获取状态，避免循环依赖
            const { token, fondsCode } = getHttpClientState();

            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }

            if (fondsCode) {
                config.headers['X-Fonds-No'] = fondsCode;
            }
        } catch {
            // Silently fail to add headers
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor to handle errors
client.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        handleResponseError(error);
        return Promise.reject(error);
    }
);

/**
 * Handle response error - extracted to reduce complexity
 */
function handleResponseError(error: unknown): void {
    if (!error || typeof error !== 'object') return;
    const axiosError = error as { response?: { status: number; data?: { message?: string } }; config?: { url?: string }; message?: string };
    if (!axiosError.response) return;

    const { status, data } = axiosError.response;
    const isAuthError = status === 401;

    if (isAuthError) {
        const { token } = getHttpClientState();
        if (token) {
            performLogout();
        }
        redirectToLoginIfNeeded(data?.message);
    }

    // Normalize error message for UI consumption
    if (data?.message && axiosError.message !== undefined) {
        (error as { message: string }).message = data.message;
    }
}

/**
 * Redirect to login or activation page if needed
 */
function redirectToLoginIfNeeded(message?: string): void {
    if (typeof window === 'undefined') return;
    const { pathname } = window.location;
    if (pathname.includes('/login') || pathname.includes('/activation')) return;

    const isLicenseError = message?.includes('License') || message?.includes('许可');
    window.location.href = isLicenseError ? '/system/activation' : '/system/login';
}
