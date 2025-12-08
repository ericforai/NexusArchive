import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 用户信息接口
 */
export interface User {
    id: string;
    username: string;
    realName?: string;
    fullName?: string;
    email?: string;
    avatar?: string;
    departmentId?: string;
    status?: string;
    roles: string[];
    permissions: string[];
}

/**
 * 认证状态接口
 */
interface AuthState {
    // 状态
    token: string | null;
    user: User | null;
    isAuthenticated: boolean;
    isCheckingAuth: boolean;

    // Actions
    login: (token: string, user: User) => void;
    logout: () => void;
    setCheckingAuth: (checking: boolean) => void;
    updateUser: (user: Partial<User>) => void;
    updatePermissions: (permissions: string[], roles?: string[]) => void;

    // 权限检查
    hasPermission: (perm: string) => boolean;
    hasAnyPermission: (perms: string[]) => boolean;
    hasRole: (role: string) => boolean;
    isSuperAdmin: () => boolean;
}

/**
 * 认证状态 Store
 * 
 * 使用 persist 中间件将状态持久化到 localStorage
 * 替代原有的 safeStorage 手动管理
 */
export const useAuthStore = create<AuthState>()(
    persist(
        (set, get) => ({
            // 初始状态
            token: null,
            user: null,
            isAuthenticated: false,
            isCheckingAuth: true,

            // 登录
            login: (token, user) => {
                console.log('[AuthStore] Login:', user.username);
                set({
                    token,
                    user: {
                        ...user,
                        roles: user.roles || [],
                        permissions: user.permissions || [],
                    },
                    isAuthenticated: true,
                    isCheckingAuth: false,
                });
            },

            // 登出
            logout: () => {
                console.log('[AuthStore] Logout');
                set({
                    token: null,
                    user: null,
                    isAuthenticated: false,
                    isCheckingAuth: false,
                });
            },

            // 设置检查中状态
            setCheckingAuth: (checking) => set({ isCheckingAuth: checking }),

            // 更新用户信息
            updateUser: (userData) => {
                const { user } = get();
                if (user) {
                    set({ user: { ...user, ...userData } });
                }
            },

            // 更新权限（从 /user/permissions 接口获取后调用）
            updatePermissions: (permissions, roles) => {
                const { user } = get();
                if (user) {
                    set({
                        user: {
                            ...user,
                            permissions,
                            roles: roles || user.roles,
                        },
                    });
                }
            },

            // 检查是否有指定权限（支持通配符匹配）
            hasPermission: (perm) => {
                const { user } = get();
                if (!user) return false;

                const permissions = user.permissions || [];
                return permissions.some((p) => {
                    // 完全匹配
                    if (p === perm) return true;
                    // 超级权限
                    if (p === '*') return true;
                    // 通配符匹配: "archive:*" 匹配 "archive:view"
                    if (p.endsWith('*')) {
                        const prefix = p.slice(0, -1);
                        return perm.startsWith(prefix);
                    }
                    return false;
                });
            },

            // 检查是否有任一权限
            hasAnyPermission: (perms) => {
                const { hasPermission } = get();
                return perms.some((p) => hasPermission(p));
            },

            // 检查是否有指定角色
            hasRole: (role) => {
                const { user } = get();
                return user?.roles?.includes(role) ?? false;
            },

            // 是否为超级管理员
            isSuperAdmin: () => {
                const { hasRole } = get();
                return hasRole('super_admin') || hasRole('SUPER_ADMIN');
            },
        }),
        {
            name: 'nexus-auth', // localStorage key
            partialize: (state) => ({
                // 只持久化这些字段
                token: state.token,
                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
            // 使用自定义 storage 适配器，避免 "Access to storage is not allowed" 错误
            storage: {
                getItem: (name) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            const value = window.localStorage.getItem(name);
                            return value ? JSON.parse(value) : null;
                        }
                    } catch (e) {
                        console.warn('[AuthStore] Storage getItem failed:', e);
                    }
                    return null;
                },
                setItem: (name, value) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            window.localStorage.setItem(name, JSON.stringify(value));
                        }
                    } catch (e) {
                        console.warn('[AuthStore] Storage setItem failed:', e);
                    }
                },
                removeItem: (name) => {
                    try {
                        if (typeof window !== 'undefined' && window.localStorage) {
                            window.localStorage.removeItem(name);
                        }
                    } catch (e) {
                        console.warn('[AuthStore] Storage removeItem failed:', e);
                    }
                },
            },
        }
    )
);
