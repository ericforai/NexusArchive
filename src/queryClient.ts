import { QueryClient } from '@tanstack/react-query';

/**
 * 全局 QueryClient 配置
 * 
 * 用于管理服务端状态缓存、自动重新获取、失败重试等
 */
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            // 数据在 5 分钟内被视为新鲜，不会重新请求
            staleTime: 5 * 60 * 1000,

            // 10 分钟后清理未使用的缓存
            gcTime: 10 * 60 * 1000,

            // 失败后重试 1 次
            retry: 1,

            // 窗口获得焦点时不自动重新获取（避免频繁请求）
            refetchOnWindowFocus: false,

            // 重新连接网络时自动重新获取
            refetchOnReconnect: true,
        },
        mutations: {
            // mutation 失败后重试 0 次
            retry: 0,
        },
    },
});

/**
 * Query Keys 常量
 * 
 * 集中管理所有 query key，避免拼写错误
 */
export const QUERY_KEYS = {
    // 档案
    archives: ['archives'] as const,
    archive: (id: string) => ['archive', id] as const,
    archiveRecent: (limit: number) => ['archives', 'recent', limit] as const,

    // 统计
    stats: ['stats'] as const,
    statsDashboard: ['stats', 'dashboard'] as const,

    // 借阅
    borrowing: ['borrowing'] as const,
    borrowingList: (params: Record<string, unknown>) => ['borrowing', params] as const,

    // 审批
    approvals: ['approvals'] as const,
    approvalList: (params: Record<string, unknown>) => ['approvals', params] as const,

    // 合规
    compliance: ['compliance'] as const,
    complianceReport: (id: string) => ['compliance', 'report', id] as const,

    // 通知
    notifications: ['notifications'] as const,

    // 用户
    user: ['user'] as const,
    userPermissions: ['user', 'permissions'] as const,
} as const;
