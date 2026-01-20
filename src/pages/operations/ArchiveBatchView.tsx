// Input: React、归档批次 API、批量操作组件
// Output: ArchiveBatchView 组件（集成批量审批功能）
// Pos: src/pages/operations/ArchiveBatchView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback } from 'react';
import { message } from 'antd';
import { useFondsStore } from '@/store/useFondsStore';
import { archiveBatchApi } from '@/api/archiveBatch';
// 批量操作组件已集成到 BatchTable 中
// import {
//     BatchOperationBar,
//     BatchApprovalDialog,
//     BatchResultModal
// } from '@/components/operations';
// Hooks
import {
    useArchiveBatchData,
    useBatchModals,
    useBatchOperations
} from './archive-batch/hooks';
// Components
import { BatchStats } from './archive-batch/components';
import { BatchTable } from './archive-batch/components';

const ArchiveBatchView: React.FC = () => {
    const currentFonds = useFondsStore((state: any) => state.currentFonds);

    // 数据和分页状态
    const {
        batches,
        total,
        stats,
        loading,
        page,
        pageSize,
        statusFilter,
        setPage,
        setPageSize: _setPageSize, // 预留接口，由 BatchTable 内部管理
        setStatusFilter,
        refresh
    } = useArchiveBatchData();

    // 弹窗状态管理
    const {
        createModalVisible,
        detailModalVisible,
        approvalModalVisible,
        addVoucherModalVisible,
        selectedBatch,
        batchItems,
        integrityReport,
        approvalAction,
        approvalComment,
        availableVouchers,
        selectedVoucherIds,
        loadingVouchers,
        form,
        setCreateModalVisible,
        setDetailModalVisible: _setDetailModalVisible, // 由 handleViewDetail 管理
        setApprovalModalVisible,
        setAddVoucherModalVisible,
        setApprovalComment,
        setSelectedVoucherIds,
        handleCreate,
        handleViewDetail,
        openApprovalModal,
        handleApproval,
        handleIntegrityCheck,
        handleDelete,
        loadAvailableVouchers,
        handleAddVouchersToBatch,
        closeDetailModal,
        closeAddVoucherModal
    } = useBatchModals();

    // 批量操作状态管理
    const {
        batchDialogOpen,
        batchResultOpen,
        batchAction,
        batchResult,
        batchProcessing,
        rowSelection,
        clearSelection,
        getSelectedCount,
        selectedRecords,
        handleBatchApprove,
        handleBatchReject,
        handleBatchConfirm,
        handleBatchRetry,
        handleSelectAll
    } = useBatchOperations(batches);

    // 提交校验
    const handleSubmit = useCallback(async (batchId: number) => {
        try {
            await archiveBatchApi.submitBatch(batchId);
            message.success('提交成功，正在校验');
            refresh();
        } catch (err: any) {
            message.error(err.message || '提交失败');
        }
    }, [refresh]);

    // 执行归档
    const handleArchive = useCallback(async (batchId: number) => {
        try {
            await archiveBatchApi.executeBatchArchive(batchId);
            message.success('归档完成');
            refresh();
        } catch (err: any) {
            message.error(err.message || '归档失败');
        }
    }, [refresh]);

    // 创建批次
    const handleCreateBatch = useCallback(async (values: { period: [any, any] }) => {
        await handleCreate(values, currentFonds?.id ? parseInt(currentFonds.id) : 1, refresh);
    }, [handleCreate, currentFonds?.id, refresh]);

    // 审批确认
    const handleApprovalConfirm = useCallback(async (comment: string, skipIds: string[]) => {
        await handleBatchConfirm(comment, skipIds);
        refresh();
    }, [handleBatchConfirm, refresh]);

    // 批量操作重试
    const handleBatchRetryConfirm = useCallback(async (failedIds: string[]) => {
        await handleBatchRetry(failedIds);
        refresh();
    }, [handleBatchRetry, refresh]);

    // 关闭审批弹窗
    const closeApprovalDialog = useCallback(() => {
        setApprovalModalVisible(false);
    }, [setApprovalModalVisible]);

    // 关闭结果弹窗
    const closeResultDialog = useCallback(() => {
        // Will be handled by BatchResultModal's onClose
    }, []);

    return (
        <div className="p-6">
            {/* 统计卡片 */}
            <BatchStats stats={stats} />

            {/* 批次列表 */}
            <BatchTable
                batches={batches}
                total={total}
                loading={loading}
                page={page}
                pageSize={pageSize}
                statusFilter={statusFilter}
                rowSelection={rowSelection}
                selectedCount={getSelectedCount()}
                onPageChange={setPage}
                onRefresh={refresh}
                onOpenCreateModal={() => setCreateModalVisible(true)}
                onStatusFilterChange={setStatusFilter}
                onViewDetail={handleViewDetail}
                onSubmit={handleSubmit}
                onDelete={(id) => handleDelete(id, refresh)}
                onApprove={(batch) => openApprovalModal(batch, 'approve')}
                onReject={(batch) => openApprovalModal(batch, 'reject')}
                onArchive={handleArchive}
                onBatchApprove={handleBatchApprove}
                onBatchReject={handleBatchReject}
                onSelectAll={() => handleSelectAll(batches.map(b => String(b.id)))}
                onClearSelection={clearSelection}
                batchProcessing={batchProcessing}
                batchDialogOpen={batchDialogOpen}
                batchResultOpen={batchResultOpen}
                batchAction={batchAction}
                batchResult={batchResult}
                selectedRecords={selectedRecords}
                onBatchConfirm={handleApprovalConfirm}
                onBatchRetry={handleBatchRetryConfirm}
                onCloseResultDialog={closeResultDialog}
                detailModalVisible={detailModalVisible}
                selectedBatch={selectedBatch}
                batchItems={batchItems}
                integrityReport={integrityReport}
                onCloseDetail={closeDetailModal}
                onIntegrityCheck={handleIntegrityCheck}
                onLoadAvailableVouchers={loadAvailableVouchers}
                onOpenAddVoucherModal={() => setAddVoucherModalVisible(true)}
                addVoucherModalVisible={addVoucherModalVisible}
                availableVouchers={availableVouchers}
                selectedVoucherIds={selectedVoucherIds}
                loadingVouchers={loadingVouchers}
                onCloseAddVoucherModal={closeAddVoucherModal}
                onVoucherSelectionChange={(ids) => setSelectedVoucherIds(ids)}
                onAddVouchers={() => handleAddVouchersToBatch(refresh)}
                approvalModalVisible={approvalModalVisible}
                approvalAction={approvalAction}
                approvalComment={approvalComment}
                onApprovalCommentChange={setApprovalComment}
                onApproval={() => handleApproval(refresh)}
                onCloseApproval={closeApprovalDialog}
                createModalVisible={createModalVisible}
                form={form}
                onCloseCreate={() => setCreateModalVisible(false)}
                onFormSubmit={handleCreateBatch}
            />
        </div>
    );
};

export default ArchiveBatchView;
