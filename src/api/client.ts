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
        try {
            const token = useAuthStore.getState().token;
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        } catch (e) {
            // Silently fail to add token
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
