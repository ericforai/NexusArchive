// Input: axios、auth store、fonds store 与浏览器 location
// Output: 带拦截器的 HTTP client 实例
// Pos: 前端 API 请求基础封装
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import axios from 'axios';
import { useAuthStore, useFondsStore } from '../store';

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
        try {
            // 添加认证 Token
            const token = useAuthStore.getState().token;
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }

            // 添加当前全宗号（用于数据过滤）
            const fondsCode = useFondsStore.getState().getCurrentFondsCode();
            if (fondsCode) {
                config.headers['X-Fonds-Code'] = fondsCode;
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
        if (error.response) {
            const { status, data } = error.response;
            const url = error.config?.url || '';
            const isAuthError = status === 401;
            const isForbidden = status === 403 && !url.includes('/auth/login');

            if (isAuthError || isForbidden) {
                const { token, logout } = useAuthStore.getState();
                if (token) {
                    logout();
                }

                if (typeof window !== 'undefined' && !window.location.pathname.includes('/login') && !window.location.pathname.includes('/activation')) {
                    const msg = data?.message || '';
                    if (msg.includes('License') || msg.includes('许可')) {
                        window.location.href = '/system/activation';
                    } else {
                        window.location.href = '/system/login';
                    }
                }
            }

            // Normalize error message for UI consumption
            // Backend now consistently returns { code, message, data }
            if (data && data.message) {
                error.message = data.message;
            }
        }
        return Promise.reject(error);
    }
);
