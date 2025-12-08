import axios from 'axios';
import { useAuthStore } from '../store';

const API_URL = '/api';

export const client = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add token
client.interceptors.request.use(
    (config) => {
        console.log('[Axios] Requesting:', config.url);
        try {
            // 从 AuthStore 获取 token
            const token = useAuthStore.getState().token;
            console.log('[Axios] Token found:', !!token);
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        } catch (e) {
            console.error('[Axios] Error reading token:', e);
        }
        return config;
    },
    (error) => {
        console.error('[Axios] Request Error:', error);
        return Promise.reject(error);
    }
);

// Response interceptor to handle errors
client.interceptors.response.use(
    (response) => {
        console.log('[Axios] Response:', response.config.url, response.status);
        return response;
    },
    (error) => {
        console.error('[Axios] Response Error:', error.message);
        if (error.response && error.response.status === 401) {
            console.warn('[Axios] 401 Unauthorized for:', error.config?.url);
            // 注意：不再自动删除 token 和重定向
            // 让 SystemApp 或 usePermissions 自己处理认证状态
            // 这避免了登录后立即被踢出的循环问题
        }
        return Promise.reject(error);
    }
);
