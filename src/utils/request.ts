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
        // Handle global errors (e.g., 401 logout)
        if (error.response && error.response.status === 401) {
            safeStorage.removeItem('token');
            // Avoid infinite reload loop if 401 persists
            if (!window.location.pathname.includes('/system/login')) {
                window.location.href = '/system/login';
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
