// Input: React, archiveBatchApi, dayjs
// Output: 批次弹窗状态管理 Hook
// Pos: src/pages/operations/archive-batch/hooks/useBatchModals.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback } from 'react';
import { message, Form } from 'antd';
import dayjs from 'dayjs';
import {
    archiveBatchApi,
    ArchiveBatch,
    ArchiveBatchItem,
    IntegrityReport
} from '@/api/archiveBatch';

/**
 * 审批动作类型
 */
export type ApprovalAction = 'approve' | 'reject';

/**
 * 批次弹窗状态 Hook 返回值
 */
export interface UseBatchModalsReturn {
    // 弹窗可见性状态
    createModalVisible: boolean;
    detailModalVisible: boolean;
    approvalModalVisible: boolean;
    addVoucherModalVisible: boolean;

    // 选中批次和条目
    selectedBatch: ArchiveBatch | null;
    batchItems: ArchiveBatchItem[];
    integrityReport: IntegrityReport | null;

    // 审批相关
    approvalAction: ApprovalAction;
    approvalComment: string;
    setApprovalComment: (comment: string) => void;

    // 添加凭证相关
    availableVouchers: any[];
    selectedVoucherIds: number[];
    setSelectedVoucherIds: (ids: number[]) => void;
    loadingVouchers: boolean;

    // 表单
    form: any;

    // 操作方法
    setCreateModalVisible: (visible: boolean) => void;
    setDetailModalVisible: (visible: boolean) => void;
    setApprovalModalVisible: (visible: boolean) => void;
    setAddVoucherModalVisible: (visible: boolean) => void;

    // 业务方法
    handleCreate: (values: { period: [dayjs.Dayjs, dayjs.Dayjs] }, fondsId: number, onSuccess: () => void) => Promise<void>;
    handleViewDetail: (batch: ArchiveBatch) => Promise<void>;
    openApprovalModal: (batch: ArchiveBatch, action: ApprovalAction) => void;
    handleApproval: (onSuccess: () => void) => Promise<void>;
    handleIntegrityCheck: (batchId: number) => Promise<void>;
    handleDelete: (batchId: number, onSuccess: () => void) => Promise<void>;
    loadAvailableVouchers: () => Promise<void>;
    handleAddVouchersToBatch: (onSuccess: () => void) => Promise<void>;
    closeDetailModal: () => void;
    closeAddVoucherModal: () => void;
}

/**
 * 批次弹窗状态管理 Hook
 *
 * 管理创建、详情、审批、添加凭证等弹窗状态和相关操作
 */
export function useBatchModals(): UseBatchModalsReturn {
    const [form] = Form.useForm();

    // 弹窗可见性状态
    const [createModalVisible, setCreateModalVisible] = useState(false);
    const [detailModalVisible, setDetailModalVisible] = useState(false);
    const [approvalModalVisible, setApprovalModalVisible] = useState(false);
    const [addVoucherModalVisible, setAddVoucherModalVisible] = useState(false);

    // 选中批次和条目
    const [selectedBatch, setSelectedBatch] = useState<ArchiveBatch | null>(null);
    const [batchItems, setBatchItems] = useState<ArchiveBatchItem[]>([]);
    const [integrityReport, setIntegrityReport] = useState<IntegrityReport | null>(null);

    // 审批相关
    const [approvalAction, setApprovalAction] = useState<ApprovalAction>('approve');
    const [approvalComment, setApprovalComment] = useState('');

    // 添加凭证相关
    const [availableVouchers, setAvailableVouchers] = useState<any[]>([]);
    const [selectedVoucherIds, setSelectedVoucherIds] = useState<number[]>([]);
    const [loadingVouchers, setLoadingVouchers] = useState(false);

    /**
     * 创建批次
     */
    const handleCreate = useCallback(async (
        values: { period: [dayjs.Dayjs, dayjs.Dayjs] },
        fondsId: number,
        onSuccess: () => void
    ) => {
        try {
            await archiveBatchApi.createBatch({
                fondsId,
                periodStart: values.period[0].format('YYYY-MM-DD'),
                periodEnd: values.period[1].format('YYYY-MM-DD'),
            });
            message.success('创建成功');
            setCreateModalVisible(false);
            form.resetFields();
            onSuccess();
        } catch (err: any) {
            message.error(err.message || '创建失败');
        }
    }, [form]);

    /**
     * 查看详情
     */
    const handleViewDetail = useCallback(async (batch: ArchiveBatch) => {
        setSelectedBatch(batch);
        setDetailModalVisible(true);

        try {
            const items = await archiveBatchApi.getItems(batch.id);
            setBatchItems(items);

            if (batch.integrityReport) {
                setIntegrityReport(batch.integrityReport);
            }
        } catch {
            message.error('加载批次详情失败');
        }
    }, []);

    /**
     * 打开审批弹窗
     */
    const openApprovalModal = useCallback((batch: ArchiveBatch, action: ApprovalAction) => {
        setSelectedBatch(batch);
        setApprovalAction(action);
        setApprovalComment('');
        setApprovalModalVisible(true);
    }, []);

    /**
     * 执行审批
     */
    const handleApproval = useCallback(async (onSuccess: () => void) => {
        if (!selectedBatch) return;

        try {
            if (approvalAction === 'approve') {
                await archiveBatchApi.approveBatch(selectedBatch.id, approvalComment);
                message.success('审批通过');
            } else {
                if (!approvalComment.trim()) {
                    message.error('请填写驳回原因');
                    return;
                }
                await archiveBatchApi.rejectBatch(selectedBatch.id, approvalComment);
                message.success('已驳回');
            }
            setApprovalModalVisible(false);
            onSuccess();
        } catch (err: any) {
            message.error(err.message || '操作失败');
        }
    }, [selectedBatch, approvalAction, approvalComment]);

    /**
     * 执行四性检测
     */
    const handleIntegrityCheck = useCallback(async (batchId: number) => {
        try {
            const report = await archiveBatchApi.runIntegrityCheck(batchId);
            setIntegrityReport(report);
            message.success('四性检测完成');
        } catch (err: any) {
            message.error(err.message || '检测失败');
        }
    }, []);

    /**
     * 删除批次
     */
    const handleDelete = useCallback(async (batchId: number, onSuccess: () => void) => {
        try {
            await archiveBatchApi.deleteBatch(batchId);
            message.success('删除成功');
            onSuccess();
        } catch (err: any) {
            message.error(err.message || '删除失败');
        }
    }, []);

    /**
     * 加载可选凭证列表
     */
    const loadAvailableVouchers = useCallback(async () => {
        if (!selectedBatch) return;
        setLoadingVouchers(true);
        try {
            const response = await fetch('/api/pool?page=1&size=100&status=PENDING_ARCHIVE');
            const data = await response.json();
            if (data.code === 200) {
                setAvailableVouchers(data.data?.records || data.data || []);
            }
        } catch {
            message.error('加载凭证列表失败');
        } finally {
            setLoadingVouchers(false);
        }
    }, [selectedBatch]);

    /**
     * 添加选中的凭证到批次
     */
    const handleAddVouchersToBatch = useCallback(async (onSuccess: () => void) => {
        if (!selectedBatch || selectedVoucherIds.length === 0) return;
        try {
            const added = await archiveBatchApi.addVouchers(selectedBatch.id, selectedVoucherIds);
            message.success(`已添加 ${added} 条凭证`);
            setAddVoucherModalVisible(false);
            setSelectedVoucherIds([]);

            // 刷新条目列表
            const items = await archiveBatchApi.getItems(selectedBatch.id);
            setBatchItems(items);

            // 刷新批次信息
            const updatedBatch = await archiveBatchApi.getBatch(selectedBatch.id);
            setSelectedBatch(updatedBatch);

            onSuccess();
        } catch (err: any) {
            message.error(err.message || '添加失败');
        }
    }, [selectedBatch, selectedVoucherIds]);

    /**
     * 关闭详情弹窗
     */
    const closeDetailModal = useCallback(() => {
        setDetailModalVisible(false);
        setSelectedBatch(null);
        setBatchItems([]);
        setIntegrityReport(null);
    }, []);

    /**
     * 关闭添加凭证弹窗
     */
    const closeAddVoucherModal = useCallback(() => {
        setAddVoucherModalVisible(false);
        setSelectedVoucherIds([]);
    }, []);

    return {
        // 弹窗可见性状态
        createModalVisible,
        detailModalVisible,
        approvalModalVisible,
        addVoucherModalVisible,

        // 选中批次和条目
        selectedBatch,
        batchItems,
        integrityReport,

        // 审批相关
        approvalAction,
        approvalComment,
        setApprovalComment,

        // 添加凭证相关
        availableVouchers,
        selectedVoucherIds,
        setSelectedVoucherIds,
        loadingVouchers,

        // 表单
        form,

        // 操作方法
        setCreateModalVisible,
        setDetailModalVisible,
        setApprovalModalVisible,
        setAddVoucherModalVisible,

        // 业务方法
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
    };
}

export default useBatchModals;
