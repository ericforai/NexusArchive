import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { archivesApi, archivesApiEx, Archive, ArchiveQuery } from '../api/archives';
import { QUERY_KEYS } from '../queryClient';

/**
 * 获取档案列表
 */
export function useArchives(params?: ArchiveQuery) {
    return useQuery({
        queryKey: [...QUERY_KEYS.archives, params],
        queryFn: () => archivesApi.getArchives(params),
        select: (data) => data.data, // 解包 ApiResponse
    });
}

/**
 * 获取单个档案详情
 */
export function useArchive(id: string) {
    return useQuery({
        queryKey: QUERY_KEYS.archive(id),
        queryFn: () => archivesApi.getArchiveById(id),
        select: (data) => data.data,
        enabled: !!id, // id 为空时不发起请求
    });
}

/**
 * 获取最近档案
 */
export function useRecentArchives(limit: number = 5) {
    return useQuery({
        queryKey: QUERY_KEYS.archiveRecent(limit),
        queryFn: () => archivesApiEx.getRecent(limit),
        select: (data) => data.data,
    });
}

/**
 * 创建档案
 */
export function useCreateArchive() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: Partial<Archive>) => archivesApi.createArchive(data),
        onSuccess: () => {
            // 创建成功后，使列表缓存失效
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.archives });
        },
    });
}

/**
 * 更新档案
 */
export function useUpdateArchive() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ id, data }: { id: string; data: Partial<Archive> }) =>
            archivesApi.updateArchive(id, data),
        onSuccess: (_, variables) => {
            // 更新成功后，使列表和单条缓存失效
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.archives });
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.archive(variables.id) });
        },
    });
}

/**
 * 删除档案
 */
export function useDeleteArchive() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (id: string) => archivesApi.deleteArchive(id),
        onSuccess: () => {
            // 删除成功后，使列表缓存失效
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.archives });
        },
    });
}

/**
 * 导出 AIP 包
 */
export function useExportAip() {
    return useMutation({
        mutationFn: (archivalCode: string) => archivesApi.exportAipPackage(archivalCode),
    });
}
