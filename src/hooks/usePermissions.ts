// Input: react-query、auth store、client 与 query keys
// Output: 权限相关 hooks（兼容权限响应格式）
// Pos: 权限数据访问层 hooks
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import { client } from '../api/client';
import { QUERY_KEYS } from '../queryClient';
import { useAuthStore } from '../store';
import { ApiResponse } from '../types';

interface UserPermissions {
    permissions: string[];
    roles: Array<{
        id: string;
        name: string;
        code: string;
    }>;
}

const normalizePermissionsPayload = (
    payload: ApiResponse<UserPermissions> | UserPermissions | null | undefined
): UserPermissions => {
    if (!payload || typeof payload !== 'object') {
        return { permissions: [], roles: [] };
    }

    if ('data' in payload && payload.data) {
        return payload.data;
    }

    if ('permissions' in payload || 'roles' in payload) {
        return payload as UserPermissions;
    }

    return { permissions: [], roles: [] };
};

/**
 * 获取用户权限（使用 React Query 缓存）
 */
export function usePermissionsQuery() {
    const { isAuthenticated, updatePermissions } = useAuthStore();

    const query = useQuery({
        queryKey: QUERY_KEYS.userPermissions,
        queryFn: async () => {
            const response = await client.get<ApiResponse<UserPermissions> | UserPermissions>('/user/permissions');
            return normalizePermissionsPayload(response.data);
        },
        enabled: isAuthenticated, // 只有登录后才请求
        staleTime: 10 * 60 * 1000, // 权限数据 10 分钟内有效
    });

    // 使用 useEffect 同步到 AuthStore（React Query v5 移除了 onSuccess）
    useEffect(() => {
        if (query.data) {
            const rolesArray = query.data.roles?.map((r) => r.code) || [];
            updatePermissions(query.data.permissions || [], rolesArray);
        }
    }, [query.data, updatePermissions]);

    return query;
}

/**
 * 兼容旧的 usePermissions hook
 *
 * 保持与原有 API 兼容，但内部使用 React Query
 * 修复：使用 useMemo 稳定返回值引用
 */
export const usePermissions = () => {
    const { user, hasPermission, hasAnyPermission, hasRole, isSuperAdmin } = useAuthStore();
    const { data, isLoading, refetch } = usePermissionsQuery();

    // 使用 useMemo 稳定返回值引用
    return useMemo(() => ({
        permissions: user?.permissions || [],
        roles: data?.roles || [],
        loading: isLoading,
        hasPermission,
        hasAnyPermission,
        hasRole,
        isSuperAdmin,
        reload: refetch,
    }), [user, data, isLoading, hasPermission, hasAnyPermission, hasRole, isSuperAdmin, refetch]);
};
