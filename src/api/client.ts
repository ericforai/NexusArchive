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
        if (error.response) {
            const { status } = error.response;
            const url = error.config?.url || '';
            const isAuthError = status === 401;
            // Handle 403 (Forbidden) - often implies missing/invalid token or license issue
            const isForbidden = status === 403 && !url.includes('/auth/login');

            if (isAuthError || isForbidden) {
                console.warn(`[Axios] ${status} - Token invalid, expired, or forbidden. Auto logout.`);

                // Avoid infinite loops if we are already logging out
                const { token, logout } = useAuthStore.getState();
                if (token) {
                    logout();
                }

                // Redirect to login if not already there
                if (typeof window !== 'undefined' && !window.location.pathname.includes('/login') && !window.location.pathname.includes('/activation')) {
                    // Check if it's a license 403 (special case)
                    const msg = error.response.data?.message || '';
                    if (msg.includes('License') || msg.includes('许可')) {
                        window.location.href = '/system/activation';
                    } else {
                        window.location.href = '/system/login';
                    }
                }
            }
        }
        return Promise.reject(error);
    }
);
