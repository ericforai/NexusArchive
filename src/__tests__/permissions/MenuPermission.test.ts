import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAuthStore, User } from '@/store/useAuthStore';
import { act } from '@testing-library/react';
import { NAV_ITEMS } from '@/constants';

/**
 * 菜单可见性权限测试
 * 
 * 验证不同权限的用户看到的菜单是否正确
 * 
 * @author 权限系统测试
 */
describe('菜单可见性权限测试', () => {

    // 管理员用户 - 拥有所有权限
    const adminUser: User = {
        id: 'user-admin',
        username: 'admin',
        realName: '系统管理员',
        roles: ['SYSTEM_ADMIN'],
        permissions: [
            'nav:all',
            'nav:settings',
            'nav:archive_mgmt',
            'manage_users',
            'manage_roles',
            'manage_settings',
            'audit:view'
        ],
    };

    // 受限用户 - 只有审计日志权限
    const limitedUser: User = {
        id: 'user-limited',
        username: 'auditonly',
        realName: '审计员',
        roles: ['AUDIT_VIEWER'],
        permissions: ['audit:view'],
    };

    // 普通档案管理员 - 只有档案相关权限
    const archiveUser: User = {
        id: 'user-archive',
        username: 'archivist',
        realName: '档案管理员',
        roles: ['ARCHIVIST'],
        permissions: [
            'nav:archive_mgmt',
            'nav:collection',
            'nav:query',
            'archive:read',
            'archive:write'
        ],
    };

    beforeEach(() => {
        const store = useAuthStore.getState();
        store.logout();
    });

    describe('管理员菜单可见性', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('admin-token', adminUser);
            });
        });

        it('管理员应该有 nav:all 权限', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:all')).toBe(true);
        });

        it('管理员应该能看到设置菜单', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:settings')).toBe(true);
        });

        it('管理员应该能看到用户管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasAnyPermission(['manage_users', 'nav:all'])).toBe(true);
        });

        it('管理员应该能看到角色管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasAnyPermission(['manage_roles', 'nav:all'])).toBe(true);
        });
    });

    describe('受限用户菜单可见性', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('limited-token', limitedUser);
            });
        });

        it('受限用户应该能看到审计日志相关功能', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('audit:view')).toBe(true);
        });

        it('受限用户不应该有 nav:all 权限', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:all')).toBe(false);
        });

        it('受限用户不应该能看到设置菜单', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:settings')).toBe(false);
        });

        it('受限用户不应该能看到用户管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('manage_users')).toBe(false);
        });

        it('受限用户不应该能看到角色管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('manage_roles')).toBe(false);
        });

        it('受限用户不应该能看到档案管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:archive_mgmt')).toBe(false);
        });
    });

    describe('档案管理员菜单可见性', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('archive-token', archiveUser);
            });
        });

        it('档案管理员应该能看到档案管理菜单', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:archive_mgmt')).toBe(true);
        });

        it('档案管理员应该能看到收集菜单', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:collection')).toBe(true);
        });

        it('档案管理员不应该能看到设置菜单', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('nav:settings')).toBe(false);
        });

        it('档案管理员不应该能看到用户管理', () => {
            const state = useAuthStore.getState();
            expect(state.hasPermission('manage_users')).toBe(false);
        });
    });

    describe('未登录用户', () => {
        it('未登录用户不应该有任何权限', () => {
            const state = useAuthStore.getState();
            expect(state.isAuthenticated).toBe(false);
            expect(state.hasPermission('nav:all')).toBe(false);
            expect(state.hasPermission('audit:view')).toBe(false);
        });
    });
});

/**
 * 路由守卫权限测试
 * 
 * 验证路由权限保护是否正确工作
 */
describe('路由守卫权限测试', () => {

    const limitedUser: User = {
        id: 'user-limited',
        username: 'auditonly',
        realName: '审计员',
        roles: ['AUDIT_VIEWER'],
        permissions: ['audit:view'],
    };

    beforeEach(() => {
        const store = useAuthStore.getState();
        store.logout();
    });

    describe('受限用户路由访问', () => {
        beforeEach(() => {
            const store = useAuthStore.getState();
            act(() => {
                store.login('limited-token', limitedUser);
            });
        });

        it('受限用户访问审计日志应该被允许', () => {
            const state = useAuthStore.getState();
            // 审计日志页面需要 audit:view 或 audit_logs 权限
            const canAccess = state.hasAnyPermission(['audit:view', 'audit_logs']);
            expect(canAccess).toBe(true);
        });

        it('受限用户访问用户管理应该被拒绝', () => {
            const state = useAuthStore.getState();
            const canAccess = state.hasPermission('manage_users');
            expect(canAccess).toBe(false);
        });

        it('受限用户访问角色管理应该被拒绝', () => {
            const state = useAuthStore.getState();
            const canAccess = state.hasPermission('manage_roles');
            expect(canAccess).toBe(false);
        });

        it('受限用户访问系统设置应该被拒绝', () => {
            const state = useAuthStore.getState();
            const canAccess = state.hasAnyPermission(['manage_settings', 'nav:settings']);
            expect(canAccess).toBe(false);
        });
    });

    describe('未认证用户路由访问', () => {
        it('未认证用户访问任何受保护路由都应该被拒绝', () => {
            const state = useAuthStore.getState();
            expect(state.isAuthenticated).toBe(false);

            // 所有权限检查都应该返回 false
            expect(state.hasPermission('audit:view')).toBe(false);
            expect(state.hasPermission('manage_users')).toBe(false);
            expect(state.hasPermission('nav:all')).toBe(false);
        });
    });
});
