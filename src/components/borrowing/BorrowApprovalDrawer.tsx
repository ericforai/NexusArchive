// Input: React、lucide-react、borrowingApi
// Output: BorrowApprovalDrawer 组件
// Pos: src/components/borrowing

import React, { useState, useEffect } from 'react';
import {
    X,
    Check,
    RotateCcw,
    Loader2,
    Calendar,
    User,
    FileText,
    Phone,
    AlertCircle
} from 'lucide-react';
import { BorrowStatusTag } from './BorrowStatusTag';
import { borrowingApi, BORROW_PURPOSE_OPTIONS, URGENCY_LEVEL_OPTIONS } from '../../api/borrowing';

export interface BorrowingRecord {
    id: string;
    archiveId: string;
    archiveTitle: string;
    userId: string;
    userName: string;
    reason: string;
    borrowDate: string;
    expectedReturnDate: string;
    actualReturnDate?: string;
    status: string;
    approvalComment?: string;
    borrowPurpose?: string;
    urgencyLevel?: string;
    contactInfo?: string;
    createdTime: string;
}

export interface BorrowApprovalDrawerProps {
    open: boolean;
    borrowingId: string | null;
    onClose: () => void;
    onSuccess: () => void;
    currentUserId?: string; // 当前登录用户ID，用于判断是否显示归还按钮
}

export const BorrowApprovalDrawer: React.FC<BorrowApprovalDrawerProps> = ({
    open,
    borrowingId,
    onClose,
    onSuccess,
    currentUserId
}) => {
    const [borrowing, setBorrowing] = useState<BorrowingRecord | null>(null);
    const [loading, setLoading] = useState(false);
    const [approving, setApproving] = useState(false);
    const [returning, setReturning] = useState(false);
    const [comment, setComment] = useState('');
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (open && borrowingId) {
            fetchBorrowing();
        } else {
            setBorrowing(null);
            setComment('');
            setError(null);
        }
    }, [open, borrowingId]);

    const fetchBorrowing = async () => {
        if (!borrowingId) return;

        setLoading(true);
        setError(null);
        try {
            const res = await borrowingApi.getBorrowings({ page: 1, limit: 100 });
            if (res.code === 200 && res.data) {
                const found = res.data.records.find((b) => b.id === borrowingId);
                if (found) {
                    setBorrowing(found);
                } else {
                    setError('未找到借阅记录');
                }
            }
        } catch (err) {
            console.error('Fetch borrowing failed', err);
            setError('加载失败，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (approved: boolean) => {
        if (!borrowingId) return;

        setApproving(true);
        setError(null);
        try {
            const res = await borrowingApi.approveBorrowing(borrowingId, {
                approved,
                comment: comment.trim() || undefined
            });

            if (res.code === 200) {
                onSuccess();
                onClose();
            } else {
                setError(res.message || '操作失败');
            }
        } catch (err) {
            console.error('Approve borrowing failed', err);
            setError('操作失败，请稍后重试');
        } finally {
            setApproving(false);
        }
    };

    const handleUserReturn = async () => {
        if (!borrowingId) return;

        setReturning(true);
        setError(null);
        try {
            const res = await borrowingApi.userReturnArchive(borrowingId);

            if (res.code === 200) {
                onSuccess();
                onClose();
            } else {
                setError(res.message || '归还失败');
            }
        } catch (err) {
            console.error('Return borrowing failed', err);
            setError('归还失败，请稍后重试');
        } finally {
            setReturning(false);
        }
    };

    // 判断是否显示归还按钮（用户只能归还自己的借阅）
    const canUserReturn = borrowing &&
        borrowing.userId === currentUserId &&
        ['APPROVED', 'BORROWED', 'OVERDUE'].includes(borrowing.status);

    // 判断是否显示审批按钮（管理员）
    const needsApproval = borrowing && borrowing.status === 'PENDING';

    // 获取借阅用途显示文本
    const getPurposeLabel = (purpose?: string) => {
        if (!purpose) return '-';
        const found = BORROW_PURPOSE_OPTIONS.find((p) => p.value === purpose);
        return found?.label || purpose;
    };

    // 获取紧急程度显示文本
    const getUrgencyLabel = (urgency?: string) => {
        if (!urgency) return '普通';
        const found = URGENCY_LEVEL_OPTIONS.find((u) => u.value === urgency);
        return found?.label || '普通';
    };

    // 判断是否紧急
    const isUrgent = borrowing?.urgencyLevel === 'URGENT';

    if (!open) return null;

    return (
        <div className="fixed inset-0 z-50">
            <div
                className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
                onClick={onClose}
            />

            <div className="absolute right-0 top-0 h-full w-full max-w-md bg-white shadow-xl flex flex-col">
                <div className="flex items-center justify-between p-4 border-b border-slate-200">
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        借阅申请详情
                        {isUrgent && (
                            <span className="px-2 py-0.5 bg-rose-100 text-rose-700 text-xs rounded-full">
                                紧急
                            </span>
                        )}
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
                        <X size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-4">
                    {loading ? (
                        <div className="flex items-center justify-center py-12 text-slate-400">
                            <Loader2 size={24} className="animate-spin mr-2" />
                            加载中...
                        </div>
                    ) : borrowing ? (
                        <div className="space-y-4">
                            <div className="bg-slate-50 rounded-lg p-3">
                                <div className="text-xs text-slate-500 mb-1">当前状态</div>
                                <BorrowStatusTag
                                    status={borrowing.status}
                                    showComment={!!borrowing.approvalComment}
                                    comment={borrowing.approvalComment}
                                />
                            </div>

                            <div>
                                <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                    <FileText size={14} />
                                    档案信息
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <div className="font-medium text-slate-800">
                                        {borrowing.archiveTitle || '未知档案'}
                                    </div>
                                    <div className="text-xs text-slate-500 mt-1">
                                        ID: {borrowing.archiveId}
                                    </div>
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                    <User size={14} />
                                    申请人
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <div className="font-medium text-slate-800">
                                        {borrowing.userName || '-'}
                                    </div>
                                    <div className="text-xs text-slate-500 mt-1">
                                        ID: {borrowing.userId}
                                    </div>
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                    <FileText size={14} />
                                    借阅原因
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3 text-sm text-slate-700">
                                    {borrowing.reason || '—'}
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                        <FileText size={14} />
                                        借阅用途
                                    </div>
                                    <div className="bg-slate-50 rounded-lg p-3 text-sm text-slate-700">
                                        {getPurposeLabel(borrowing.borrowPurpose)}
                                    </div>
                                </div>
                                <div>
                                    <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                        <AlertCircle size={14} />
                                        紧急程度
                                    </div>
                                    <div className="bg-slate-50 rounded-lg p-3 text-sm text-slate-700">
                                        {getUrgencyLabel(borrowing.urgencyLevel)}
                                    </div>
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                    <Phone size={14} />
                                    联系方式
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3 text-sm text-slate-700">
                                    {borrowing.contactInfo || '—'}
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center gap-2 text-sm text-slate-500 mb-1">
                                    <Calendar size={14} />
                                    时间信息
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3 space-y-2 text-sm">
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">申请时间</span>
                                        <span className="text-slate-700">
                                            {borrowing.createdTime?.split('T')[0] || '-'}
                                        </span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">借阅日期</span>
                                        <span className="text-slate-700">
                                            {borrowing.borrowDate || '-'}
                                        </span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">预计归还</span>
                                        <span className="text-slate-700">
                                            {borrowing.expectedReturnDate || '-'}
                                        </span>
                                    </div>
                                    {borrowing.actualReturnDate && (
                                        <div className="flex justify-between">
                                            <span className="text-slate-500">实际归还</span>
                                            <span className="text-slate-700">
                                                {borrowing.actualReturnDate}
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="text-center py-12 text-slate-400">
                            未找到借阅记录
                        </div>
                    )}
                </div>

                {/* 用户归还按钮 */}
                {canUserReturn && (
                    <div className="border-t border-slate-200 p-4">
                        {error && (
                            <div className="bg-rose-50 border border-rose-200 text-rose-700 rounded-lg p-2 text-sm mb-3">
                                {error}
                            </div>
                        )}
                        <button
                            onClick={handleUserReturn}
                            disabled={returning}
                            className="w-full px-4 py-2 rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {returning ? (
                                <>
                                    <Loader2 size={16} className="animate-spin" />
                                    归还中...
                                </>
                            ) : (
                                <>
                                    <RotateCcw size={16} />
                                    提前归还
                                </>
                            )}
                        </button>
                    </div>
                )}

                {/* 管理员审批按钮 */}
                {needsApproval && (
                    <div className="border-t border-slate-200 p-4 space-y-3">
                        {error && (
                            <div className="bg-rose-50 border border-rose-200 text-rose-700 rounded-lg p-2 text-sm">
                                {error}
                            </div>
                        )}

                        <div>
                            <label className="block text-sm text-slate-600 mb-1">审批意见</label>
                            <textarea
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                                rows={2}
                                placeholder="填写审批意见（可选）"
                                disabled={approving}
                                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50 resize-none"
                            />
                        </div>

                        <div className="flex gap-2">
                            <button
                                onClick={() => handleApprove(false)}
                                disabled={approving}
                                className="flex-1 px-4 py-2 rounded-lg border border-rose-200 text-rose-700 hover:bg-rose-50 disabled:opacity-50 flex items-center justify-center gap-2"
                            >
                                {approving ? (
                                    <Loader2 size={16} className="animate-spin" />
                                ) : (
                                    <X size={16} />
                                )}
                                拒绝
                            </button>
                            <button
                                onClick={() => handleApprove(true)}
                                disabled={approving}
                                className="flex-1 px-4 py-2 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 flex items-center justify-center gap-2"
                            >
                                {approving ? (
                                    <Loader2 size={16} className="animate-spin" />
                                ) : (
                                    <Check size={16} />
                                )}
                                批准
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default BorrowApprovalDrawer;
