import { useState, useEffect } from 'react';
import { client } from '../api/client';

interface UserPermissions {
    permissions: string[];
    roles: Array<{
        id: string;
        name: string;
        code: string;
    }>;
}

export const usePermissions = () => {
    const [permissions, setPermissions] = useState<string[]>([]);
    const [roles, setRoles] = useState<UserPermissions['roles']>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadPermissions();
    }, []);

    const loadPermissions = async () => {
        try {
            setLoading(true);
            const response = await client.get('/user/permissions');
            const data = response.data.data;
            setPermissions(data.permissions || []);
            setRoles(data.roles || []);

            // 缓存到 localStorage
            localStorage.setItem('user_permissions', JSON.stringify(data.permissions || []));
            localStorage.setItem('user_roles', JSON.stringify(data.roles || []));
        } catch (error) {
            console.error('Failed to load permissions:', error);
            // 尝试从缓存加载
            const cachedPermissions = localStorage.getItem('user_permissions');
            const cachedRoles = localStorage.getItem('user_roles');
            if (cachedPermissions) setPermissions(JSON.parse(cachedPermissions));
            if (cachedRoles) setRoles(JSON.parse(cachedRoles));
        } finally {
            setLoading(false);
        }
    };

    const hasPermission = (permission: string): boolean => {
        return permissions.includes(permission);
    };

    const hasAnyPermission = (requiredPermissions: string[]): boolean => {
        return requiredPermissions.some(p => permissions.includes(p));
    };

    const hasRole = (roleCode: string): boolean => {
        return roles.some(r => r.code === roleCode);
    };

    const isSuperAdmin = (): boolean => {
        return hasRole('super_admin');
    };

    return {
        permissions,
        roles,
        loading,
        hasPermission,
        hasAnyPermission,
        hasRole,
        isSuperAdmin,
        reload: loadPermissions
    };
};
