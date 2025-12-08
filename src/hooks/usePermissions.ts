import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
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

/**
 * 获取用户权限（使用 React Query 缓存）
 */
export function usePermissionsQuery() {
    const { isAuthenticated, updatePermissions } = useAuthStore();

    const query = useQuery({
        queryKey: QUERY_KEYS.userPermissions,
        queryFn: async () => {
            const response = await client.get<ApiResponse<UserPermissions>>('/user/permissions');
            return response.data.data;
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
 */
export const usePermissions = () => {
    const { user, hasPermission, hasAnyPermission, hasRole, isSuperAdmin } = useAuthStore();
    const { data, isLoading, refetch } = usePermissionsQuery();

    return {
        permissions: user?.permissions || [],
        roles: data?.roles || [],
        loading: isLoading,
        hasPermission,
        hasAnyPermission,
        hasRole,
        isSuperAdmin,
        reload: refetch,
    };
};
