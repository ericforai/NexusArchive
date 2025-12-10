import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';

import { safeStorage } from './storage';

const instance = axios.create({
    baseURL: '/api', // Proxy is configured in vite.config.ts usually, or we use relative path
    timeout: 10000,
});

instance.interceptors.request.use(
    (config) => {
        const token = safeStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

instance.interceptors.response.use(
    (response) => response.data,
    (error) => {
        // Handle global errors
        if (error.response) {
            const status = error.response.status;
            const msg = error.response.data?.message || '';

            // 401 Unauthorized -> Logout
            if (status === 401) {
                safeStorage.removeItem('token');
                if (!window.location.pathname.includes('/system/login')) {
                    window.location.href = '/system/login';
                }
            }

            // 403 Forbidden (License Issues) -> Redirect to Activation
            // LicenseFilter returns BusinessException which is mapped to 403 with specific messages
            // 403 Forbidden
            if (status === 403) {
                // If it's a License issue, redirect to activation
                if (msg.includes('License') || msg.includes('许可')) {
                    if (!window.location.pathname.includes('/system/activation')) {
                        window.location.href = '/system/activation';
                    }
                    return Promise.reject(error);
                }

                // If we have no token (or storage failed/expired), and got 403, it's likely an "Anonymous Access Denied"
                // Spring Security defaults to 403 for anonymous access to protected resources in some configs.
                const token = safeStorage.getItem('token');
                if (!token) {
                    safeStorage.removeItem('token'); // cleanup just in case
                    if (!window.location.pathname.includes('/system/login')) {
                        window.location.href = '/system/login';
                    }
                    return Promise.reject(error);
                }

                // Otherwise, it's a real Permission Denied (Logged in but no rights)
                // We might want to show a toast here? For now, just reject.
            }
        }
        return Promise.reject(error);
    }
);

export interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

export const request = <T = any>(config: AxiosRequestConfig): Promise<ApiResponse<T>> => {
    return instance.request<any, ApiResponse<T>>(config);
};
