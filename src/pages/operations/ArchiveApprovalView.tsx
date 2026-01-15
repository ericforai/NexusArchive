// Input: React、lucide-react 图标、本地模块 api/archiveApproval、批量操作组件、Ant Design
// Output: React 组件 ArchiveApprovalView - 集成批量审批功能
// Pos: src/pages/operations/ArchiveApprovalView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { archiveApprovalApi, ArchiveApproval } from '../../api/archiveApproval';
import {
    BatchOperationBar,
    BatchApprovalDialog,
    BatchResultModal,
    useBatchSelection,
    type ApprovalRecord,
    type BatchError
} from '../../components/operations';
import { CheckCircle2, XCircle, FileText, Clock, User, Calendar, MessageSquare, AlertCircle } from 'lucide-react';
import { toast } from '../../utils/notificationService';
import { useAuthStore } from '@/store/useAuthStore';

// Constants
const PAGE_SIZE = 50;

export const ArchiveApprovalView: React.FC = () => {
    // Auth store - get current user
    const user = useAuthStore(state => state.user);

    // 批量选择状态
    const {
        selectedIds,
        rowSelection,
        clearSelection,
        selectAll,
        getSelectedCount
    } = useBatchSelection();

    // 数据状态
    const [approvals, setApprovals] = useState<ArchiveApproval[]>([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState<string>('PENDING');
    const [statusCounts, setStatusCounts] = useState({ PENDING: 0, APPROVED: 0, REJECTED: 0 });

    // 单个审批状态
    const [selectedApproval, setSelectedApproval] = useState<ArchiveApproval | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [comment, setComment] = useState('');
    const [processing, setProcessing] = useState(false);

    // 批量操作状态
    const [batchDialogOpen, setBatchDialogOpen] = useState(false);
    const [batchResultOpen, setBatchResultOpen] = useState(false);
    const [batchAction, setBatchAction] = useState<'approve' | 'reject'>('approve');
    const [batchResult, setBatchResult] = useState({ successCount: 0, failedCount: 0, errors: [] as BatchError[] });
    const [batchProcessing, setBatchProcessing] = useState(false);

    const loadApprovals = useCallback(async () => {
        try {
            setLoading(true);
            // 加载当前筛选的数据
            const response = await archiveApprovalApi.getApprovalList(1, PAGE_SIZE, statusFilter);
            setApprovals(response?.data?.data?.records || []);

            // 加载各状态计数
            const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
                archiveApprovalApi.getApprovalList(1, 1, 'PENDING'),
                archiveApprovalApi.getApprovalList(1, 1, 'APPROVED'),
                archiveApprovalApi.getApprovalList(1, 1, 'REJECTED'),
            ]);
            setStatusCounts({
                PENDING: pendingRes?.data?.data?.total || 0,
                APPROVED: approvedRes?.data?.data?.total || 0,
                REJECTED: rejectedRes?.data?.data?.total || 0,
            });
        } catch (error) {
            console.error('Failed to load approvals:', error);
        } finally {
            setLoading(false);
        }
    }, [statusFilter]);

    useEffect(() => {
        loadApprovals();
    }, [loadApprovals]);

    const handleApprove = async () => {
        if (!selectedApproval) return;

        try {
            setProcessing(true);
            await archiveApprovalApi.approveArchive({
                id: selectedApproval.id,
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: comment || '批准归档'
            });
            setShowModal(false);
            setComment('');
            loadApprovals();
        } catch (error) {
            console.error('Failed to approve:', error);
            toast.error('审批失败，请重试');
        } finally {
            setProcessing(false);
        }
    };

    const handleReject = async () => {
        if (!selectedApproval) return;

        if (!comment.trim()) {
            toast.warning('拒绝归档必须填写审批意见');
            return;
        }

        try {
            setProcessing(true);
            await archiveApprovalApi.rejectArchive({
                id: selectedApproval.id,
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: comment
            });
            setShowModal(false);
            setComment('');
            loadApprovals();
        } catch (error) {
            console.error('Failed to reject:', error);
            toast.error('审批失败，请重试');
        } finally {
            setProcessing(false);
        }
    };

    // 批量操作处理函数
    const handleBatchApprove = () => {
        setBatchAction('approve');
        setBatchDialogOpen(true);
    };

    const handleBatchReject = () => {
        setBatchAction('reject');
        setBatchDialogOpen(true);
    };

    const handleBatchConfirm = async (comment: string, skipIds: string[]) => {
        const selectedIdArray = Array.from(selectedIds).filter(id => !skipIds.includes(id));

        // 添加边界检查
        if (selectedIdArray.length === 0) {
            toast.warning('请选择至少一条记录');
            return;
        }

        if (selectedIdArray.length > 100) {
            toast.warning('单次最多 100 条，请分批操作');
            return;
        }

        try {
            setBatchProcessing(true);

            const request = {
                ids: selectedIdArray.map(String),
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: comment || (batchAction === 'approve' ? '批量批准' : '批量拒绝')
            };

            const result = batchAction === 'approve'
                ? await archiveApprovalApi.batchApprove(request)
                : await archiveApprovalApi.batchReject(request);

            setBatchResult({ successCount: result.successCount, failedCount: result.failed,
                
                
                errors: result.errors?.map(e => ({ id: e.id, reason: e.reason })) || []
            });

            setBatchDialogOpen(false);
            setBatchResultOpen(true);
            clearSelection();
            loadApprovals();

            if (result.failed === 0) {
                toast.success(`批量${batchAction === 'approve' ? '批准' : '拒绝'}成功`);
            } else if (result.successCount === 0) {
                toast.error(`批量${batchAction === 'approve' ? '批准' : '拒绝'}失败`);
            } else {
                toast.warning(`部分成功：${result.successCount}条成功，${result.failed}条失败`);
            }
        } catch (error) {
            console.error("Batch operation failed:", error); console.error("Error response:", (error as any).response?.data); console.error("Error status:", (error as any).response?.status);
            toast.error(`批量${batchAction === 'approve' ? '批准' : '拒绝'}失败，请重试`);
        } finally {
            setBatchProcessing(false);
        }
    };

    const handleBatchRetry = async (failedIds: string[]) => {
        try {
            setBatchProcessing(true);

            const request = {
                ids: failedIds.map(String),
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: batchAction === 'approve' ? '重试批量批准' : '重试批量拒绝'
            };

            const result = batchAction === 'approve'
                ? await archiveApprovalApi.batchApprove(request)
                : await archiveApprovalApi.batchReject(request);

            setBatchResult({ successCount: result.successCount, failedCount: result.failed,
                
                
                errors: result.errors?.map(e => ({ id: e.id, reason: e.reason })) || []
            });

            loadApprovals();

            if (result.failed === 0) {
                toast.success('重试成功');
                setBatchResultOpen(false);
            } else if (result.successCount === 0) {
                toast.error('重试失败');
            } else {
                toast.warning(`部分成功：${result.successCount}条成功，${result.failed}条失败`);
            }
        } catch (error) {
            console.error('Batch retry failed:', error);
            toast.error('重试失败，请重试');
        } finally {
            setBatchProcessing(false);
        }
    };

    const handleSelectAll = () => {
        const allIds = approvals.map(a => a.id);
        const result = selectAll(allIds);
        if (!result.success) {
            toast.warning(result.reason || '全选失败');
        }
    };

    // 获取选中的记录列表
    const selectedRecords = useMemo<ApprovalRecord[]>(() => {
        return approvals
            .filter(a => selectedIds.has(a.id))
            .map(a => ({
                id: a.id,
                title: a.archiveTitle,
                code: a.archiveCode
            }));
    }, [approvals, selectedIds]);

    // 表格列定义 - useMemo to prevent re-creation
    const columns: ColumnsType<ArchiveApproval> = useMemo(() => [
        {
            title: '档号',
            dataIndex: 'archiveCode',
            key: 'archiveCode',
            width: 150,
            render: (text) => <span className="font-mono text-slate-600">{text || '-'}</span>
        },
        {
            title: '档案题名',
            dataIndex: 'archiveTitle',
            key: 'archiveTitle',
            ellipsis: true,
            render: (text) => (
                <span className="font-medium text-slate-800" title={text}>
                    {text && text.length > 50 ? text.substring(0, 50) + '...' : (text || '-')}
                </span>
            )
        },
        {
            title: '申请人',
            dataIndex: 'applicantName',
            key: 'applicantName',
            width: 120,
            render: (text) => <span className="text-slate-600">{text || '-'}</span>
        },
        {
            title: '申请时间',
            dataIndex: 'createdTime',
            key: 'createdTime',
            width: 180,
            render: (text) => (
                <span className="text-slate-500 font-mono text-xs">
                    {text ? new Date(text).toLocaleString('zh-CN') : '-'}
                </span>
            )
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 120,
            render: (status) => getStatusBadge(status)
        },
        {
            title: '操作',
            key: 'action',
            width: 100,
            fixed: 'right',
            render: (_, record) => (
                <button
                    onClick={() => {
                        setSelectedApproval(record);
                        setShowModal(true);
                    }}
                    className="text-indigo-600 hover:text-indigo-700 font-medium text-sm"
                >
                    查看详情
                </button>
            )
        }
    ], []);

    const getStatusBadge = (status: string) => {
        const styles = {
            PENDING: 'bg-amber-100 text-amber-700 border-amber-200',
            APPROVED: 'bg-emerald-100 text-emerald-700 border-emerald-200',
            REJECTED: 'bg-rose-100 text-rose-700 border-rose-200'
        };
        const labels = {
            PENDING: '待审批',
            APPROVED: '已批准',
            REJECTED: '已拒绝'
        };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-bold border ${styles[status as keyof typeof styles]}`}>
                {labels[status as keyof typeof labels]}
            </span>
        );
    };

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Header */}
            <div className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-8 shrink-0">
                <div className="max-w-[1600px] mx-auto">
                    <h2 className="text-2xl font-bold flex items-center gap-3">
                        <FileText className="text-white" /> 档案审批管理
                    </h2>
                    <p className="text-indigo-100 mt-2">
                        对待归档档案进行审批，确保档案质量符合 DA/T 94-2022 标准要求
                    </p>

                    {/* Stats */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-indigo-100 text-xs font-bold uppercase">待审批</span>
                                <Clock size={16} className="text-amber-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {statusCounts.PENDING}
                                <span className="text-xs font-normal text-indigo-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-indigo-100 text-xs font-bold uppercase">已批准</span>
                                <CheckCircle2 size={16} className="text-emerald-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {statusCounts.APPROVED}
                                <span className="text-xs font-normal text-indigo-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-indigo-100 text-xs font-bold uppercase">已拒绝</span>
                                <XCircle size={16} className="text-rose-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {statusCounts.REJECTED}
                                <span className="text-xs font-normal text-indigo-200 ml-1">件</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-hidden max-w-[1600px] mx-auto w-full px-8 pb-8 pt-6 flex flex-col">
                {/* Filter Tabs */}
                <div className="flex gap-2 mb-4">
                    {['PENDING', 'APPROVED', 'REJECTED'].map(status => (
                        <button
                            key={status}
                            onClick={() => {
                                setStatusFilter(status);
                                clearSelection();
                            }}
                            className={`px-4 py-2 rounded-lg font-medium text-sm transition-all ${statusFilter === status
                                ? 'bg-indigo-600 text-white shadow-md'
                                : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
                                }`}
                        >
                            {status === 'PENDING' && '待审批'}
                            {status === 'APPROVED' && '已批准'}
                            {status === 'REJECTED' && '已拒绝'}
                        </button>
                    ))}
                </div>

                {/* Batch Operation Bar */}
                <BatchOperationBar
                    selectedCount={getSelectedCount()}
                    totalCount={approvals.length}
                    onBatchApprove={handleBatchApprove}
                    onBatchReject={handleBatchReject}
                    onSelectAll={handleSelectAll}
                    onClear={clearSelection}
                    loading={batchProcessing}
                />

                {/* Table */}
                <div className="flex-1 bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden flex flex-col">
                    {loading ? (
                        <div className="flex-1 flex items-center justify-center">
                            <div className="text-slate-400">加载中...</div>
                        </div>
                    ) : approvals.length === 0 ? (
                        <div className="flex-1 flex items-center justify-center">
                            <div className="text-center">
                                <AlertCircle size={48} className="text-slate-300 mx-auto mb-3" />
                                <p className="text-slate-400">暂无审批记录</p>
                            </div>
                        </div>
                    ) : (
                        <div className="flex-1 overflow-auto">
                            <Table<ArchiveApproval>
                                columns={columns}
                                dataSource={approvals}
                                rowKey="id"
                                rowSelection={rowSelection}
                                pagination={false}
                                scroll={{ y: 'calc(100vh - 500px)' }}
                                size="middle"
                            />
                        </div>
                    )}
                </div>
            </div>

            {/* Modal */}
            {showModal && selectedApproval && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-auto">
                        <div className="p-6 border-b border-slate-200">
                            <h3 className="text-xl font-bold text-slate-800">审批详情</h3>
                        </div>

                        <div className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="text-xs text-slate-500 font-medium">档号</label>
                                    <p className="font-mono text-slate-800 mt-1">{selectedApproval.archiveCode || '-'}</p>
                                </div>
                                <div>
                                    <label className="text-xs text-slate-500 font-medium">状态</label>
                                    <div className="mt-1">{getStatusBadge(selectedApproval.status)}</div>
                                </div>
                            </div>

                            <div>
                                <label className="text-xs text-slate-500 font-medium">档案题名</label>
                                <p className="text-slate-800 mt-1 break-all">
                                    {selectedApproval.archiveTitle && selectedApproval.archiveTitle.length > 100
                                        ? selectedApproval.archiveTitle.substring(0, 100) + '...'
                                        : (selectedApproval.archiveTitle || '-')}
                                </p>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="text-xs text-slate-500 font-medium flex items-center gap-1">
                                        <User size={12} /> 申请人
                                    </label>
                                    <p className="text-slate-800 mt-1">{selectedApproval.applicantName || '-'}</p>
                                </div>
                                <div>
                                    <label className="text-xs text-slate-500 font-medium flex items-center gap-1">
                                        <Calendar size={12} /> 申请时间
                                    </label>
                                    <p className="text-slate-800 mt-1 font-mono text-xs">
                                        {selectedApproval.createdTime ? new Date(selectedApproval.createdTime).toLocaleString('zh-CN') : '-'}
                                    </p>
                                </div>
                            </div>

                            {selectedApproval.applicationReason && (
                                <div>
                                    <label className="text-xs text-slate-500 font-medium">申请理由</label>
                                    <p className="text-slate-800 mt-1">{selectedApproval.applicationReason}</p>
                                </div>
                            )}

                            {selectedApproval.status !== 'PENDING' && (
                                <>
                                    <div className="border-t border-slate-200 pt-4">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium">审批人</label>
                                                <p className="text-slate-800 mt-1">{selectedApproval.approverName || '-'}</p>
                                            </div>
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium">审批时间</label>
                                                <p className="text-slate-800 mt-1 font-mono text-xs">
                                                    {selectedApproval.approvalTime ? new Date(selectedApproval.approvalTime).toLocaleString('zh-CN') : '-'}
                                                </p>
                                            </div>
                                        </div>
                                        {selectedApproval.approvalComment && (
                                            <div className="mt-4">
                                                <label className="text-xs text-slate-500 font-medium flex items-center gap-1">
                                                    <MessageSquare size={12} /> 审批意见
                                                </label>
                                                <p className="text-slate-800 mt-1 bg-slate-50 p-3 rounded-lg">{selectedApproval.approvalComment}</p>
                                            </div>
                                        )}
                                    </div>
                                </>
                            )}

                            {selectedApproval.status === 'PENDING' && (
                                <div>
                                    <label className="text-xs text-slate-500 font-medium flex items-center gap-1 mb-2">
                                        <MessageSquare size={12} /> 审批意见
                                    </label>
                                    <textarea
                                        value={comment}
                                        onChange={(e) => setComment(e.target.value)}
                                        placeholder="请填写审批意见（拒绝时必填）"
                                        className="w-full border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                                        rows={3}
                                    />
                                </div>
                            )}
                        </div>

                        <div className="p-6 border-t border-slate-200 flex justify-end gap-3">
                            <button
                                onClick={() => {
                                    setShowModal(false);
                                    setComment('');
                                }}
                                className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 font-medium"
                                disabled={processing}
                            >
                                关闭
                            </button>
                            {selectedApproval.status === 'PENDING' && (
                                <>
                                    <button
                                        onClick={handleReject}
                                        className="px-4 py-2 bg-rose-600 text-white rounded-lg hover:bg-rose-700 font-medium flex items-center gap-2 disabled:opacity-50"
                                        disabled={processing}
                                    >
                                        <XCircle size={16} /> 拒绝
                                    </button>
                                    <button
                                        onClick={handleApprove}
                                        className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 font-medium flex items-center gap-2 disabled:opacity-50"
                                        disabled={processing}
                                    >
                                        <CheckCircle2 size={16} /> 批准
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Batch Approval Dialog */}
            <BatchApprovalDialog
                visible={batchDialogOpen}
                selectedCount={getSelectedCount()}
                action={batchAction}
                onConfirm={handleBatchConfirm}
                onCancel={() => setBatchDialogOpen(false)}
                selectedRecords={selectedRecords}
                loading={batchProcessing}
            />

            {/* Batch Result Modal */}
            <BatchResultModal
                visible={batchResultOpen}
                successCount={batchResult.successCount}
                failedCount={batchResult.failedCount}
                errors={batchResult.errors}
                onRetry={handleBatchRetry}
                onClose={() => setBatchResultOpen(false)}
                operationType="approval"
                isRetrying={batchProcessing}
            />
        </div>
    );
};

export default ArchiveApprovalView;
