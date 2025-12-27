// Input: Controller Hook
// Output: useArchiveActions Hook
// Pos: src/features/archives/useArchiveActions.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive Actions Hook
 * 
 * 封装档案管理相关的具体操作逻辑，包括增删改查、归档、导出等。
 * 进一步为 ArchiveListView 瘦身。
 */
import { useState, useCallback } from 'react';
import { GenericRow } from '../../types';
import { poolApi } from '../../api/pool';
import { archivesApi } from '../../api/archives';
import { client } from '../../api/client';
import { ArchiveListController } from './useArchiveListController';

interface UseArchiveActionsResult {
    // States
    isUploading: boolean;
    isArchiving: boolean;
    isExporting: string | null; // ID of item being exported

    // Handlers
    handleDelete: (id: string) => Promise<void>;
    handleBatchDelete: () => Promise<void>;
    handleAipExport: (row: GenericRow) => Promise<void>;
    handleUpload: (file: File) => Promise<void>;
    handleMetadataUpdate: (fileId: string, payload: any) => Promise<{ success: boolean; message?: string }>;
    handlePoolCheck: (mode: 'all' | 'failed') => Promise<void>;

    // Archiving Flow (Confirmation handled by View/Page for now, or exposed here)
    executeArchiving: () => Promise<void>;
}

export function useArchiveActions(controller: ArchiveListController): UseArchiveActionsResult {
    const { ui, data, selection, actions, pool, mode } = controller;

    const [isUploading, setIsUploading] = useState(false);
    const [isArchiving, setIsArchiving] = useState(false);
    const [isExporting, setIsExporting] = useState<string | null>(null);

    // 删除单条
    const handleDelete = useCallback(async (id: string) => {
        if (!window.confirm('确定要删除这条记录吗？')) return;

        try {
            if (mode.isPoolView) {
                await poolApi.delete(id);
            } else {
                await archivesApi.deleteArchive(id);
            }
            ui.showToast('删除成功', 'success');
            actions.reload();
        } catch (error: any) {
            ui.showToast('删除失败: ' + (error.message || '未知错误'), 'error');
        }
    }, [mode.isPoolView, ui, actions]);

    // 批量删除
    const handleBatchDelete = useCallback(async () => {
        if (selection.selectedIds.length === 0) return;
        if (!window.confirm(`确定要删除选中的 ${selection.selectedIds.length} 条记录吗？`)) return;

        try {
            if (mode.isPoolView) {
                // Pool API might implement batch delete in future, loop for now or specific endpoint
                // Assuming loop for safety if no batch endpoint confirmed
                await Promise.all(selection.selectedIds.map(id => poolApi.delete(id)));
            } else {
                // Archives API might have batch delete
                await Promise.all(selection.selectedIds.map(id => archivesApi.deleteArchive(id)));
            }
            ui.showToast('批量删除成功', 'success');
            selection.clear();
            actions.reload();
        } catch (error: any) {
            ui.showToast('批量删除失败: ' + (error.message || '部分删除失败'), 'error');
        }
    }, [selection, mode.isPoolView, ui, actions]);

    // AIP 导出
    const handleAipExport = useCallback(async (row: GenericRow) => {
        const exportCode = row.archivalCode || row.code;
        if (!exportCode) {
            ui.showToast('无法导出: 缺少归档号', 'error');
            return;
        }

        setIsExporting(exportCode);
        try {
            const response = await client.get(`/archives/export/aip/${row.id}`, {
                responseType: 'blob'
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `${exportCode}.aip`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            ui.showToast('AIP 导出成功', 'success');
        } catch (error) {
            console.error('Export failed:', error);
            ui.showToast('AIP 导出失败', 'error');
        } finally {
            setIsExporting(null);
        }
    }, [ui]);

    // 上传 (Pool)
    const handleUpload = useCallback(async (file: File) => {
        if (!mode.isPoolView) return;

        const formData = new FormData();
        formData.append('file', file);

        setIsUploading(true);
        try {
            ui.showToast('正在上传...', 'success');
            const response = await client.post('/ingest/upload', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });

            if (response.status === 200 && response.data.code === 200) {
                ui.showToast(`✅ 上传成功: ${response.data.data.fileName}`, 'success');
                actions.reload();
            } else {
                throw new Error(response.data.message || '服务器错误');
            }
        } catch (error: any) {
            console.error('Upload error:', error);
            ui.showToast('上传出错: ' + error.message, 'error');
        } finally {
            setIsUploading(false);
        }
    }, [mode.isPoolView, ui, actions]);

    // 元数据更新
    const handleMetadataUpdate = useCallback(async (fileId: string, payload: any) => {
        try {
            const response = await client.post('/pool/metadata/update', payload);
            if (response.data?.code === 200) {
                actions.reload();
                // 如果是 Pool View，也刷新统计
                if (mode.isPoolView) pool.refreshStats();
                return { success: true, message: response.data.message };
            } else {
                return { success: false, message: response.data?.message || '更新失败' };
            }
        } catch (err: any) {
            return { success: false, message: err.response?.data?.message || '更新失败' };
        }
    }, [actions, pool, mode.isPoolView]);

    // Pool 检测
    const handlePoolCheck = useCallback(async (checkMode: 'all' | 'failed') => {
        const endpoint = checkMode === 'all' ? '/pool/check/all-pending' : '/pool/check/failed';

        // 使用 controller.data.isLoading 可能会冲突，这里可以用 toast 提示
        ui.showToast(checkMode === 'all' ? '正在执行全量检测...' : '正在重试失败项...', 'success');

        try {
            const response = await client.get(endpoint);
            if (response.data.code === 200) {
                const reports = response.data.data || [];
                const passCount = reports.filter((r: any) => r.status === 'PASS').length;
                const failCount = reports.filter((r: any) => r.status === 'FAIL').length;
                const warnCount = reports.filter((r: any) => r.status === 'WARNING').length;

                ui.showToast(`检测完成: 通过 ${passCount}, 失败 ${failCount}, 警告 ${warnCount}`, passCount > 0 ? 'success' : 'error');
                actions.reload();
                pool.refreshStats();
            }
        } catch (error: any) {
            ui.showToast('检测失败: ' + (error.message || '未知错误'), 'error');
        }
    }, [ui, actions, pool]);

    // 归档提交
    const executeArchiving = useCallback(async () => {
        if (selection.selectedIds.length === 0) return;

        setIsArchiving(true);
        try {
            const response = await client.post('/archives/batch-archive', {
                ids: selection.selectedIds
            });

            if (response.data.code === 200) {
                const result = response.data.data;
                const successCount = result.successItems ? result.successItems.length : (Array.isArray(result) ? result.length : 0);
                const failureCount = result.failures ? Object.keys(result.failures).length : 0;

                if (failureCount === 0 && successCount > 0) {
                    ui.showToast(`已提交 ${successCount} 个归档申请`, 'success');
                } else if (successCount > 0) {
                    ui.showToast(`部分成功: ${successCount}条, 失败 ${failureCount}条`, 'error');
                } else {
                    ui.showToast(`提交失败 (${failureCount})`, 'error');
                }

                if (successCount > 0) {
                    selection.clear();
                    actions.reload();
                    pool.refreshStats();
                }
            }
        } catch (error: any) {
            ui.showToast('提交归档失败: ' + (error.response?.data?.message || error.message), 'error');
        } finally {
            setIsArchiving(false);
        }
    }, [selection, ui, actions, pool]);

    return {
        isUploading,
        isArchiving,
        isExporting,
        handleDelete,
        handleBatchDelete,
        handleAipExport,
        handleUpload,
        handleMetadataUpdate,
        handlePoolCheck,
        executeArchiving
    };
}
