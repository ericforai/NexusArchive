// Input: React、lucide-react、destructionApi、批量操作组件
// Output: DestructionApprovalPage 组件（支持批量审批）
// Pos: 销毁审批页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useMemo } from 'react';
import { Shield, Loader2, CheckCircle2, XCircle, Eye, Clock, User, MessageSquare, Check, Square } from 'lucide-react';
import { destructionApi, Destruction, DestructionApprovalRequest } from '../../api/destruction';
import { toast } from '../../utils/notificationService';
import {
  BatchOperationBar,
  BatchApprovalDialog,
  BatchResultModal,
  type ApprovalRecord
} from '@/components/operations';

/**
 * 批量选择限制
 */
const MAX_SELECTION_LIMIT = 100;

/**
 * 销毁审批页面
 *
 * 功能：
 * 1. 待审批销毁申请列表
 * 2. 审批表单：审批意见、批准/拒绝
 * 3. 双人审批流程展示
 * 4. 批量审批功能（支持第一/第二审批批量操作）
 *
 * PRD 来源: Section 13 - 档案销毁
 */
export const DestructionApprovalPage: React.FC = () => {
    const [destructions, setDestructions] = useState<Destruction[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;

    // 单条审批状态
    const [selectedDestruction, setSelectedDestruction] = useState<Destruction | null>(null);
    const [showApprovalModal, setShowApprovalModal] = useState(false);
    const [approvalType, setApprovalType] = useState<'first' | 'second'>('first');
    const [approvalForm, setApprovalForm] = useState<DestructionApprovalRequest>({
        comment: '',
        approved: true,
    });
    const [submitting, setSubmitting] = useState(false);

    // 批量操作状态
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [showBatchDialog, setShowBatchDialog] = useState(false);
    const [batchAction, setBatchAction] = useState<'approve' | 'reject'>('approve');
    const [batchApprovalType, setBatchApprovalType] = useState<'first' | 'second'>('first');
    const [batchProcessing, setBatchProcessing] = useState(false);
    const [batchResult, setBatchResult] = useState<{ success: number; failed: number; errors: Array<{ id: string; reason: string }> } | null>(null);
    const [showResultModal, setShowResultModal] = useState(false);

    const loadDestructions = async () => {
        setLoading(true);
        try {
            const res = await destructionApi.getDestructions({
                page,
                limit: pageSize,
                status: 'PENDING',
            });
            if (res.code === 200 && res.data) {
                setDestructions(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载销毁申请失败', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadDestructions();
    }, [page]);

    // 清空选择
    const clearSelection = () => {
        setSelectedIds(new Set());
    };

    // 切换单条选中
    const toggleSelection = (id: string) => {
        setSelectedIds(prev => {
            const newSet = new Set(prev);
            if (newSet.has(id)) {
                newSet.delete(id);
            } else {
                if (newSet.size >= MAX_SELECTION_LIMIT) {
                    toast.warning(`单次最多选择 ${MAX_SELECTION_LIMIT} 条`);
                    return prev;
                }
                newSet.add(id);
            }
            return newSet;
        });
    };

    // 全选当前页
    const selectAllCurrentPage = () => {
        if (destructions.length > MAX_SELECTION_LIMIT) {
            toast.warning(`当前页超过 ${MAX_SELECTION_LIMIT} 条，请分批选择`);
            return;
        }
        setSelectedIds(new Set(destructions.map(d => d.id)));
    };

    // 批量审批
    const handleBatchAction = (action: 'approve' | 'reject', approvalType: 'first' | 'second') => {
        if (selectedIds.size === 0) return;

        // 检查边界
        if (selectedIds.size > MAX_SELECTION_LIMIT) {
            toast.warning(`单次最多 ${MAX_SELECTION_LIMIT} 条，请分批操作`);
            return;
        }

        setBatchAction(action);
        setBatchApprovalType(approvalType);
        setShowBatchDialog(true);
    };

    // 批量确认
    const handleBatchConfirm = async (comment: string, skipIds: number[]) => {
        setBatchProcessing(true);

        // 转换 skipIds (number) 回 string IDs
        const selectedArray = Array.from(selectedIds);
        const skipIdSet = new Set(skipIds.map(id => selectedArray[id] || ''));
        const finalIds = selectedArray.filter(id => !skipIdSet.has(id));

        try {
            const res = batchAction === 'approve'
                ? await destructionApi.batchApprove({
                    ids: finalIds,
                    comment,
                    approvalType: batchApprovalType,
                })
                : await destructionApi.batchReject({
                    ids: finalIds,
                    comment,
                    approvalType: batchApprovalType,
                });

            if (res.code === 200 && res.data) {
                setBatchResult(res.data);
                setShowResultModal(true);
                setShowBatchDialog(false);
                clearSelection();
                loadDestructions();

                if (res.data.failed === 0) {
                    toast.success(`批量${batchAction === 'approve' ? '批准' : '拒绝'}成功`);
                } else if (res.data.success === 0) {
                    toast.error(`批量${batchAction === 'approve' ? '批准' : '拒绝'}失败`);
                } else {
                    toast.warning(`部分成功：${res.data.success} 条成功，${res.data.failed} 条失败`);
                }
            } else {
                toast.error(res.message || '批量操作失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '批量操作失败');
        } finally {
            setBatchProcessing(false);
        }
    };

    // 重试失败项
    const handleBatchRetry = () => {
        if (!batchResult) return;
        const failedIds = batchResult.errors.map(e => e.id);
        setSelectedIds(new Set(failedIds));
        setShowResultModal(false);
        setBatchResult(null);
        toast.info('已重新选中失败的记录，请重试');
    };

    // 获取批量操作记录（用于弹窗显示）
    const getBatchRecords = useMemo((): ApprovalRecord[] => {
        return Array.from(selectedIds).map((id, index) => {
            const destruction = destructions.find(d => d.id === id);
            return {
                id: index,
                title: destruction ? `申请 #${destruction.id} (${destruction.applicantName})` : `申请 #${id}`,
            };
        });
    }, [selectedIds, destructions]);

    // 单条审批
    const handleApprove = (destruction: Destruction, type: 'first' | 'second') => {
        setSelectedDestruction(destruction);
        setApprovalType(type);
        setApprovalForm({ comment: '', approved: true });
        setShowApprovalModal(true);
    };

    const handleSubmitApproval = async () => {
        if (!selectedDestruction) return;

        setSubmitting(true);
        try {
            const res = await destructionApi.approveDestruction(selectedDestruction.id, approvalForm);
            if (res.code === 200) {
                toast.success('审批成功');
                setShowApprovalModal(false);
                loadDestructions();
            } else {
                toast.error(res.message || '审批失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '审批失败');
        } finally {
            setSubmitting(false);
        }
    };

    const getStatusBadge = (status: string) => {
        const configs: Record<string, { label: string; color: string }> = {
            'PENDING': { label: '待审批', color: 'bg-yellow-100 text-yellow-700' },
            'APPROVED': { label: '已批准', color: 'bg-green-100 text-green-700' },
            'REJECTED': { label: '已拒绝', color: 'bg-red-100 text-red-700' },
        };
        const config = configs[status] || { label: status, color: 'bg-slate-100 text-slate-700' };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${config.color}`}>
                {config.label}
            </span>
        );
    };

    const canFirstApprove = (destruction: Destruction) => {
        return destruction.status === 'PENDING' && !destruction.firstApproverId;
    };

    const canSecondApprove = (destruction: Destruction) => {
        return destruction.status === 'PENDING' &&
               destruction.firstApproverId &&
               !destruction.secondApproverId;
    };

    // 判断批量操作状态
    const hasFirstApprovable = destructions.some(canFirstApprove);
    const hasSecondApprovable = destructions.some(canSecondApprove);

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Shield className="mr-2" size={28} />
                        销毁审批
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">审批档案销毁申请，支持双人审批流程与批量操作</p>
                </div>
            </div>

            {/* 批量操作工具栏 */}
            {selectedIds.size > 0 && (
                <BatchOperationBar
                    selectedCount={selectedIds.size}
                    totalCount={total}
                    onBatchApprove={() => handleBatchAction('approve', hasFirstApprovable ? 'first' : 'second')}
                    onBatchReject={() => handleBatchAction('reject', hasFirstApprovable ? 'first' : 'second')}
                    onSelectAll={selectAllCurrentPage}
                    onClear={clearSelection}
                />
            )}

            {/* 审批列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : destructions.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <Shield size={48} className="mb-4" />
                        <p>暂无待审批的销毁申请</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3 w-10">
                                        <button
                                            onClick={selectAllCurrentPage}
                                            className="text-slate-500 hover:text-slate-700"
                                            title="全选当前页"
                                        >
                                            {selectedIds.size === destructions.length && destructions.length > 0 ? (
                                                <Check className="text-primary-600" size={16} />
                                            ) : (
                                                <Square size={16} />
                                            )}
                                        </button>
                                    </th>
                                    <th className="px-4 py-3">申请ID</th>
                                    <th className="px-4 py-3">申请人</th>
                                    <th className="px-4 py-3">档案数量</th>
                                    <th className="px-4 py-3">状态</th>
                                    <th className="px-4 py-3">第一审批人</th>
                                    <th className="px-4 py-3">第二审批人</th>
                                    <th className="px-4 py-3">创建时间</th>
                                    <th className="px-4 py-3">操作</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {destructions.map(destruction => {
                                    const isSelected = selectedIds.has(destruction.id);
                                    const isRowDisabled = !canFirstApprove(destruction) && !canSecondApprove(destruction);
                                    return (
                                        <tr
                                            key={destruction.id}
                                            className={`hover:bg-slate-50 ${isSelected ? 'bg-primary-50' : ''} ${isRowDisabled ? 'opacity-50' : ''}`}
                                        >
                                            <td className="px-4 py-3">
                                                {!isRowDisabled && (
                                                    <button
                                                        onClick={() => toggleSelection(destruction.id)}
                                                        className="text-slate-500 hover:text-primary-600"
                                                        title={isSelected ? '取消选择' : '选择'}
                                                    >
                                                        {isSelected ? (
                                                            <Check className="text-primary-600" size={16} />
                                                        ) : (
                                                            <Square size={16} />
                                                        )}
                                                    </button>
                                                )}
                                            </td>
                                            <td className="px-4 py-3 font-mono text-xs">{destruction.id}</td>
                                            <td className="px-4 py-3">{destruction.applicantName}</td>
                                            <td className="px-4 py-3">{destruction.archiveCount}</td>
                                            <td className="px-4 py-3">{getStatusBadge(destruction.status)}</td>
                                            <td className="px-4 py-3">
                                                {destruction.firstApproverName ? (
                                                    <div className="flex items-center gap-2">
                                                        <User size={14} className="text-slate-400" />
                                                        <span>{destruction.firstApproverName}</span>
                                                    </div>
                                                ) : (
                                                    <span className="text-slate-400">待审批</span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3">
                                                {destruction.secondApproverName ? (
                                                    <div className="flex items-center gap-2">
                                                        <User size={14} className="text-slate-400" />
                                                        <span>{destruction.secondApproverName}</span>
                                                    </div>
                                                ) : (
                                                    <span className="text-slate-400">待审批</span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3 text-slate-600">
                                                {new Date(destruction.createdAt).toLocaleString('zh-CN')}
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-2">
                                                    {canFirstApprove(destruction) && (
                                                        <button
                                                            onClick={() => handleApprove(destruction, 'first')}
                                                            className="px-3 py-1 bg-primary-600 text-white rounded text-sm hover:bg-primary-700"
                                                        >
                                                            第一审批
                                                        </button>
                                                    )}
                                                    {canSecondApprove(destruction) && (
                                                        <button
                                                            onClick={() => handleApprove(destruction, 'second')}
                                                            className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                                                        >
                                                            第二审批
                                                        </button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                        {/* 分页 */}
                        <div className="px-4 py-3 border-t flex items-center justify-between">
                            <div className="text-sm text-slate-600">
                                共 {total} 条，第 {page} / {Math.ceil(total / pageSize)} 页
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setPage(p => Math.max(1, p - 1))}
                                    disabled={page <= 1}
                                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                                >
                                    上一页
                                </button>
                                <button
                                    onClick={() => setPage(p => Math.min(Math.ceil(total / pageSize), p + 1))}
                                    disabled={page >= Math.ceil(total / pageSize)}
                                    className="px-3 py-1 border rounded text-sm disabled:opacity-50"
                                >
                                    下一页
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {/* 审批模态框 */}
            {showApprovalModal && selectedDestruction && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
                        <div className="px-6 py-4 border-b border-slate-200">
                            <h2 className="text-lg font-semibold">
                                {approvalType === 'first' ? '第一审批' : '第二审批（复核）'}
                            </h2>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    审批意见
                                </label>
                                <textarea
                                    value={approvalForm.comment}
                                    onChange={(e) => setApprovalForm({ ...approvalForm, comment: e.target.value })}
                                    rows={4}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    placeholder="请输入审批意见..."
                                />
                            </div>
                            <div>
                                <label className="flex items-center gap-2">
                                    <input
                                        type="checkbox"
                                        checked={approvalForm.approved}
                                        onChange={(e) => setApprovalForm({ ...approvalForm, approved: e.target.checked })}
                                        className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                                    />
                                    <span className="text-sm text-slate-700">批准</span>
                                </label>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    onClick={handleSubmitApproval}
                                    disabled={submitting}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {submitting ? (
                                        <Loader2 className="animate-spin" size={16} />
                                    ) : (
                                        <>
                                            {approvalForm.approved ? (
                                                <CheckCircle2 size={16} />
                                            ) : (
                                                <XCircle size={16} />
                                            )}
                                            提交审批
                                        </>
                                    )}
                                </button>
                                <button
                                    onClick={() => setShowApprovalModal(false)}
                                    className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
                                >
                                    取消
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* 批量审批对话框 */}
            <BatchApprovalDialog
                visible={showBatchDialog}
                selectedCount={selectedIds.size}
                action={batchAction}
                onConfirm={handleBatchConfirm}
                onCancel={() => setShowBatchDialog(false)}
                selectedRecords={getBatchRecords}
                loading={batchProcessing}
            />

            {/* 批量结果弹窗 */}
            <BatchResultModal
                visible={showResultModal}
                successCount={batchResult?.success ?? 0}
                failedCount={batchResult?.failed ?? 0}
                errors={batchResult?.errors.map(e => ({
                    id: parseInt(e.id) || 0,
                    reason: e.reason,
                })) ?? []}
                onClose={() => setShowResultModal(false)}
                onRetry={handleBatchRetry}
            />
        </div>
    );
};

export default DestructionApprovalPage;






