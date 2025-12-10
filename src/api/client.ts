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
            const url = error.config?.url || '';
            // 排除登录接口本身的 401（密码错误等情况）
            if (!url.includes('/auth/login')) {
                console.warn('[Axios] 401 - Token invalid or expired, auto logout');
                const { logout } = useAuthStore.getState();
                logout();
                // 重定向到登录页（如果不在登录页）
                if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
                    window.location.href = '/system/login';
                }
            }
        }
        return Promise.reject(error);
    }
);
