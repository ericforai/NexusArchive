// Input: vitest、@/store/useAuthStore、@testing-library/react
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAuthStore, User } from '@/store/useAuthStore';
import { act } from '@testing-library/react';

/**
 * useAuthStore 单元测试
 * 
 * 测试覆盖:
 * - 登录状态管理
 * - 权限检查（包括通配符匹配）
 * - 角色检查
 * - 用户信息更新
 * 
 * @author Agent E - 质量保障工程师
 */
describe('useAuthStore', () => {
    const mockUser: User = {
        id: 'user-001',
        username: 'admin',
        realName: '管理员',
        email: 'admin@example.com',
        roles: ['ADMIN', 'SUPER_ADMIN'],
        permissions: ['archive:read', 'archive:write', 'borrowing:*', 'nav:all'],
    };

    beforeEach(() => {
        // 重置 store 状态
        const store = useAuthStore.getState();
        store.logout();
    });

    describe('登录管理', () => {
        it('登录后应正确设置用户状态', () => {
            const store = useAuthStore.getState();

            act(() => {
                store.login('jwt-token-123', mockUser);
            });

            const state = useAuthStore.getState();
            expect(state.token).toBe('jwt-token-123');
            expect(state.isAuthenticated).toBe(true);
            expect(state.user?.username).toBe('admin');
            expect(state.isCheckingAuth).toBe(false);
        });

        it('登出后应清除所有状态', () => {
            const store = useAuthStore.getState();

            act(() => {
                store.login('jwt-token-123', mockUser);
                store.logout();
            });

            const state = useAuthStore.getState();
            expect(state.token).toBeNull();
            expect(state.user).toBeNull();
            expect(state.isAuthenticated).toBe(false);
        });

        it('初始状态应为未认证', () => {
            const state = useAuthStore.getState();
            expect(state.isAuthenticated).toBe(false);
            expect(state.user).toBeNull();
        });
    });

    describe('权限检查', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('token', mockUser);
            });
        });

        it('完全匹配权限应返回 true', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('archive:read')).toBe(true);
            expect(state.hasPermission('archive:write')).toBe(true);
        });

        it('通配符权限匹配应返回 true', () => {
            const state = useAuthStore.getState();
            // borrowing:* 应该匹配 borrowing:view, borrowing:create 等
            expect(state.hasPermission('borrowing:view')).toBe(true);
            expect(state.hasPermission('borrowing:create')).toBe(true);
            expect(state.hasPermission('borrowing:approve')).toBe(true);
        });

        it('没有的权限应返回 false', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('destruction:execute')).toBe(false);
            expect(state.hasPermission('admin:users')).toBe(false);
        });

        it('hasAnyPermission 应正确工作', () => {
            const state = useAuthStore.getState();
            expect(state.hasAnyPermission(['archive:read', 'unknown:perm'])).toBe(true);
            expect(state.hasAnyPermission(['unknown:perm1', 'unknown:perm2'])).toBe(false);
        });

        it('未登录时权限检查应返回 false', () => {
            const store = useAuthStore.getState();
            act(() => {
                store.logout();
            });

            const state = useAuthStore.getState();
            expect(state.hasPermission('archive:read')).toBe(false);
        });
    });

    describe('角色检查', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('token', mockUser);
            });
        });

        it('hasRole 应正确匹配角色', () => {
            const state = useAuthStore.getState();
            expect(state.hasRole('ADMIN')).toBe(true);
            expect(state.hasRole('SUPER_ADMIN')).toBe(true);
            expect(state.hasRole('USER')).toBe(false);
        });

        it('isSuperAdmin 应正确识别超级管理员', () => {
            const state = useAuthStore.getState();
            expect(state.isSuperAdmin()).toBe(true);
        });

        it('非超级管理员 isSuperAdmin 应返回 false', () => {
            const store = useAuthStore.getState();
            const normalUser: User = {
                ...mockUser,
                roles: ['USER'],
            };
            act(() => {
                store.login('token', normalUser);
            });

            const state = useAuthStore.getState();
            expect(state.isSuperAdmin()).toBe(false);
        });
    });

    describe('用户信息更新', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('token', mockUser);
            });
        });

        it('updateUser 应部分更新用户信息', () => {
            const store = useAuthStore.getState();

            act(() => {
                store.updateUser({ email: 'new-email@example.com' });
            });

            const state = useAuthStore.getState();
            expect(state.user?.email).toBe('new-email@example.com');
            expect(state.user?.username).toBe('admin'); // 其他字段不变
        });

        it('updatePermissions 应更新权限列表', () => {
            const store = useAuthStore.getState();

            act(() => {
                store.updatePermissions(['new:perm1', 'new:perm2'], ['NEW_ROLE']);
            });

            const state = useAuthStore.getState();
            expect(state.user?.permissions).toEqual(['new:perm1', 'new:perm2']);
            expect(state.user?.roles).toEqual(['NEW_ROLE']);
        });

        it('updatePermissions 不传 roles 时应保留原有角色', () => {
            const store = useAuthStore.getState();
            const originalRoles = store.user?.roles;

            act(() => {
                store.updatePermissions(['new:perm1']);
            });

            const state = useAuthStore.getState();
            expect(state.user?.roles).toEqual(originalRoles);
        });

        it('未登录时 updateUser 应不执行任何操作', () => {
            const store = useAuthStore.getState();
            act(() => {
                store.logout();
                store.updateUser({ email: 'test@test.com' });
            });

            const state = useAuthStore.getState();
            expect(state.user).toBeNull();
        });
    });

    describe('setCheckingAuth', () => {
        it('应正确设置检查状态', () => {
            const store = useAuthStore.getState();

            act(() => {
                store.setCheckingAuth(true);
            });
            expect(useAuthStore.getState().isCheckingAuth).toBe(true);

            act(() => {
                store.setCheckingAuth(false);
            });
            expect(useAuthStore.getState().isCheckingAuth).toBe(false);
        });
    });
});
