// Input: archiveBatchApi, useAuthStore, useBatchSelection
// Output: 批次批量操作 Hook
// Pos: src/pages/operations/archive-batch/hooks/useBatchOperations.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';
import { message } from 'antd';
import {
    archiveBatchApi,
    ArchiveBatch
} from '@/api/archiveBatch';
import { useAuthStore } from '@/store/useAuthStore';
import { useBatchSelection } from '@/components/operations';
import type { ApprovalRecord, BatchError } from '@/components/operations';

/**
 * 批量操作 Hook 返回值
 */
export interface UseBatchOperationsReturn {
    // 批量操作状态
    batchDialogOpen: boolean;
    batchResultOpen: boolean;
    batchAction: 'approve' | 'reject';
    batchResult: { success: number; failed: number; errors: BatchError[] };
    batchProcessing: boolean;

    // 批量选择
    selectedIds: Set<string>;
    rowSelection: any;
    clearSelection: () => void;
    selectAll: (allIds: string[]) => { success: boolean; reason?: string };
    getSelectedCount: () => number;

    // 选中记录列表
    selectedRecords: ApprovalRecord[];

    // 操作方法
    setBatchDialogOpen: (open: boolean) => void;
    setBatchResultOpen: (open: boolean) => void;
    handleBatchApprove: () => void;
    handleBatchReject: () => void;
    handleBatchConfirm: (comment: string, skipIds: string[]) => Promise<void>;
    handleBatchRetry: (failedIds: string[]) => Promise<void>;
    handleSelectAll: (allIds: string[]) => void;
}

/**
 * 批次批量操作 Hook
 *
 * 管理批量审批/拒绝的流程和状态
 */
export function useBatchOperations(batches: ArchiveBatch[]): UseBatchOperationsReturn {
    const user = useAuthStore((state: any) => state.user);

    // 批量选择状态
    const {
        selectedIds,
        rowSelection,
        clearSelection,
        selectAll,
        getSelectedCount
    } = useBatchSelection();

    // 批量操作状态
    const [batchDialogOpen, setBatchDialogOpen] = useState(false);
    const [batchResultOpen, setBatchResultOpen] = useState(false);
    const [batchAction, setBatchAction] = useState<'approve' | 'reject'>('approve');
    const [batchResult, setBatchResult] = useState<{ success: number; failed: number; errors: BatchError[] }>({
        success: 0,
        failed: 0,
        errors: []
    });
    const [batchProcessing, setBatchProcessing] = useState(false);

    /**
     * 批量批准
     */
    const handleBatchApprove = useCallback(() => {
        setBatchAction('approve');
        setBatchDialogOpen(true);
    }, []);

    /**
     * 批量拒绝
     */
    const handleBatchReject = useCallback(() => {
        setBatchAction('reject');
        setBatchDialogOpen(true);
    }, []);

    /**
     * 批量操作确认
     */
    const handleBatchConfirm = useCallback(async (comment: string, skipIds: string[]) => {
        const selectedIdArray = Array.from(selectedIds).filter(id => !skipIds.includes(id));

        // 添加边界检查
        if (selectedIdArray.length === 0) {
            message.warning('请选择至少一条记录');
            return;
        }

        if (selectedIdArray.length > 100) {
            message.warning('单次最多 100 条，请分批操作');
            return;
        }

        try {
            setBatchProcessing(true);

            // API expects number[], so we convert string[] back to number[] if necessary,
            // or we need to check if API supports string[].
            // Assuming API expects numbers based on previous code.
            const batchIds = selectedIdArray.map(id => Number(id));

            const request = {
                batchIds: batchIds,
                operatorId: user?.id || '',
                operatorName: user?.realName || user?.username || '',
                comment: comment || (batchAction === 'approve' ? '批量批准' : '批量拒绝')
            };

            const result = batchAction === 'approve'
                ? await archiveBatchApi.batchApprove(request)
                : await archiveBatchApi.batchReject(request);

            setBatchResult({
                success: result.success,
                failed: result.failed,
                errors: (result.errors || []).map(e => ({...e, id: String(e.id)}))
            });

            setBatchDialogOpen(false);
            setBatchResultOpen(true);
            clearSelection();

            if (result.failed === 0) {
                message.success(`批量${batchAction === 'approve' ? '批准' : '拒绝'}成功`);
            } else if (result.success === 0) {
                message.error(`批量${batchAction === 'approve' ? '批准' : '拒绝'}失败`);
            } else {
                message.warning(`部分成功：${result.success}条成功，${result.failed}条失败`);
            }
        } catch (err: any) {
            message.error(err.message || `批量${batchAction === 'approve' ? '批准' : '拒绝'}失败，请重试`);
        } finally {
            setBatchProcessing(false);
        }
    }, [selectedIds, user, batchAction, clearSelection]);

    /**
     * 批量操作重试
     */
    const handleBatchRetry = useCallback(async (failedIds: string[]) => {
        try {
            setBatchProcessing(true);

            const request = {
                batchIds: failedIds.map(id => Number(id)),
                operatorId: user?.id || '',
                operatorName: user?.realName || user?.username || '',
                comment: batchAction === 'approve' ? '重试批量批准' : '重试批量拒绝'
            };

            const result = batchAction === 'approve'
                ? await archiveBatchApi.batchApprove(request)
                : await archiveBatchApi.batchReject(request);

            setBatchResult({
                success: result.success,
                failed: result.failed,
                errors: (result.errors || []).map(e => ({...e, id: String(e.id)}))
            });

            if (result.failed === 0) {
                message.success('重试成功');
                setBatchResultOpen(false);
            } else if (result.success === 0) {
                message.error('重试失败');
            } else {
                message.warning(`部分成功：${result.success}条成功，${result.failed}条失败`);
            }
        } catch (err: any) {
            message.error(err.message || '重试失败，请重试');
        } finally {
            setBatchProcessing(false);
        }
    }, [user, batchAction]);

    /**
     * 全选处理
     */
    const handleSelectAll = useCallback((allIds: string[]) => {
        const result = selectAll(allIds);
        if (!result.success) {
            message.warning(result.reason || '全选失败');
        }
    }, [selectAll]);

    /**
     * 获取选中的记录列表
     */
    const selectedRecords = useMemo<ApprovalRecord[]>(() => {
        return batches
            .filter(b => selectedIds.has(String(b.id)))
            .map(b => ({
                id: String(b.id),
                title: b.batchNo,
                code: b.batchNo
            }));
    }, [batches, selectedIds]);

    return {
        // 批量操作状态
        batchDialogOpen,
        batchResultOpen,
        batchAction,
        batchResult,
        batchProcessing,

        // 批量选择
        selectedIds,
        rowSelection,
        clearSelection,
        selectAll,
        getSelectedCount,

        // 选中记录列表
        selectedRecords,

        // 操作方法
        setBatchDialogOpen,
        setBatchResultOpen,
        handleBatchApprove,
        handleBatchReject,
        handleBatchConfirm,
        handleBatchRetry,
        handleSelectAll
    };
}

export default useBatchOperations;
