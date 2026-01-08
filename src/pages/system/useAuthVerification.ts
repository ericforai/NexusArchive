// Input: AuthStore、authApi
// Output: 认证验证 Hook
// Pos: src/pages/system/ 认证逻辑
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useEffect } from 'react';
import { useAuthStore } from '../../store';
import { authApi } from '../../api/auth';

/**
 * 认证验证 Hook
 * 在组件挂载时验证 token 有效性
 */
export const useAuthVerification = () => {
    const { login, logout: authLogout, setCheckingAuth } = useAuthStore();

    useEffect(() => {
        const verifyAuth = async () => {
            const { token, user } = useAuthStore.getState();

            // 如果有 token 和 user 缓存，直接认证成功
            if (token && user) {
                console.log('[SystemApp] Token and user found, setting authenticated');
                setCheckingAuth(false);
                return;
            }

            if (!token) {
                console.log('[SystemApp] No token found');
                setCheckingAuth(false);
                return;
            }

            // 只有 token 但没有 user 时才验证（边界情况）
            try {
                console.log('[SystemApp] Verifying token...');
                const res = await authApi.getCurrentUser();
                if (res.code === 200 && res.data) {
                    login(token, res.data);
                } else {
                    authLogout();
                }
            } catch (error) {
                console.error('[SystemApp] Token verification failed:', error);
                authLogout();
            } finally {
                setCheckingAuth(false);
            }
        };

        verifyAuth();
    }, [login, authLogout, setCheckingAuth]);
};
