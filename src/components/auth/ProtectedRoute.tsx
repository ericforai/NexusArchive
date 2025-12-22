// Input: React、react-router-dom 路由、本地模块 store/useAuthStore、api/auth
// Output: React 组件 ProtectedRoute
// Pos: 鉴权/路由守卫组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 路由守卫组件
 * 
 * 保护需要登录的路由，未登录时跳转到登录页
 * 支持登录后跳转回原页面
 */
import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { authApi } from '../../api/auth';

interface ProtectedRouteProps {
    children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    const location = useLocation();
    const [isChecking, setIsChecking] = useState(true);

    // 从 Zustand store 获取认证状态（包括 hydration 状态）
    const { token, user, isAuthenticated, _hasHydrated, login, logout } = useAuthStore();

    useEffect(() => {
        const checkAuth = async () => {
            // 等待 zustand persist hydration 完成
            if (!_hasHydrated) {
                console.log('[ProtectedRoute] Waiting for hydration...');
                return; // 等待 hydration，不做任何决定
            }

            // 如果已经有 token 和 user，直接认证成功
            if (token && user) {
                console.log('[ProtectedRoute] Token and user found in store, authenticated');
                setIsChecking(false);
                return;
            }

            if (!token) {
                console.log('[ProtectedRoute] No token found in store');
                setIsChecking(false);
                return;
            }

            // 只有 token 但没有 user 时才验证
            try {
                console.log('[ProtectedRoute] Verifying token...');
                const res = await authApi.getCurrentUser();
                if (res.code === 200 && res.data) {
                    // 更新 store 中的用户信息
                    login(token, {
                        id: res.data.id,
                        username: res.data.username,
                        realName: res.data.fullName,
                        roles: res.data.roles || [],
                        permissions: res.data.permissions || [],
                    });
                } else {
                    logout();
                }
            } catch (error) {
                console.error('[ProtectedRoute] Token verification failed:', error);
                logout();
            } finally {
                setIsChecking(false);
            }
        };

        checkAuth();
    }, [token, user, _hasHydrated, login, logout]);

    // 等待 hydration 完成或检查中显示加载状态
    if (!_hasHydrated || isChecking) {
        return (
            <div className="flex items-center justify-center h-screen text-slate-600">
                <span className="animate-pulse text-sm">正在校验登录状态...</span>
            </div>
        );
    }

    // 未登录，跳转到登录页，并记录原始路径
    if (!isAuthenticated) {
        return (
            <Navigate
                to="/system/login"
                state={{ from: location }}
                replace
            />
        );
    }

    // 已登录，渲染子组件
    return <>{children}</>;
};

export default ProtectedRoute;
