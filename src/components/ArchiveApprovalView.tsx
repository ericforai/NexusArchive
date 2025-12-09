import React, { useState, useEffect } from 'react';
import { archiveApprovalApi, ArchiveApproval } from '../api/archiveApproval';
import { CheckCircle2, XCircle, FileText, Clock, User, Calendar, MessageSquare, AlertCircle } from 'lucide-react';

export const ArchiveApprovalView: React.FC = () => {
    const [approvals, setApprovals] = useState<ArchiveApproval[]>([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState<string>('PENDING');
    const [selectedApproval, setSelectedApproval] = useState<ArchiveApproval | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [comment, setComment] = useState('');
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        loadApprovals();
    }, [statusFilter]);

    const loadApprovals = async () => {
        try {
            setLoading(true);
            const response = await archiveApprovalApi.getApprovalList(1, 50, statusFilter);
            setApprovals(response.data.data.records || []);
        } catch (error) {
            console.error('Failed to load approvals:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async () => {
        if (!selectedApproval) return;

        try {
            setProcessing(true);
            await archiveApprovalApi.approveArchive({
                id: selectedApproval.id,
                approverId: 'admin',
                approverName: '管理员',
                comment: comment || '批准归档'
            });
            setShowModal(false);
            setComment('');
            loadApprovals();
        } catch (error) {
            console.error('Failed to approve:', error);
            alert('审批失败，请重试');
        } finally {
            setProcessing(false);
        }
    };

    const handleReject = async () => {
        if (!selectedApproval) return;

        if (!comment.trim()) {
            alert('拒绝归档必须填写审批意见');
            return;
        }

        try {
            setProcessing(true);
            await archiveApprovalApi.rejectArchive({
                id: selectedApproval.id,
                approverId: 'admin',
                approverName: '管理员',
                comment: comment
            });
            setShowModal(false);
            setComment('');
            loadApprovals();
        } catch (error) {
            console.error('Failed to reject:', error);
            alert('审批失败，请重试');
        } finally {
            setProcessing(false);
        }
    };

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
                                {approvals.filter(a => a.status === 'PENDING').length}
                                <span className="text-xs font-normal text-indigo-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-indigo-100 text-xs font-bold uppercase">已批准</span>
                                <CheckCircle2 size={16} className="text-emerald-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {approvals.filter(a => a.status === 'APPROVED').length}
                                <span className="text-xs font-normal text-indigo-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-indigo-100 text-xs font-bold uppercase">已拒绝</span>
                                <XCircle size={16} className="text-rose-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {approvals.filter(a => a.status === 'REJECTED').length}
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
                            onClick={() => setStatusFilter(status)}
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
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-slate-600 font-medium border-b border-slate-200 sticky top-0">
                                    <tr>
                                        <th className="p-4">档号</th>
                                        <th className="p-4">档案题名</th>
                                        <th className="p-4">申请人</th>
                                        <th className="p-4">申请时间</th>
                                        <th className="p-4">状态</th>
                                        <th className="p-4">操作</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {approvals.map(approval => (
                                        <tr key={approval.id} className="hover:bg-slate-50">
                                            <td className="p-4 font-mono text-slate-600">{approval.archiveCode || '-'}</td>
                                            <td className="p-4 font-medium text-slate-800" title={approval.archiveTitle}>
                                                {approval.archiveTitle && approval.archiveTitle.length > 50
                                                    ? approval.archiveTitle.substring(0, 50) + '...'
                                                    : (approval.archiveTitle || '-')}
                                            </td>
                                            <td className="p-4 text-slate-600">{approval.applicantName || '-'}</td>
                                            <td className="p-4 text-slate-500 font-mono text-xs">
                                                {approval.createdTime ? new Date(approval.createdTime).toLocaleString('zh-CN') : '-'}
                                            </td>
                                            <td className="p-4">{getStatusBadge(approval.status)}</td>
                                            <td className="p-4">
                                                <button
                                                    onClick={() => {
                                                        setSelectedApproval(approval);
                                                        setShowModal(true);
                                                    }}
                                                    className="text-indigo-600 hover:text-indigo-700 font-medium text-sm"
                                                >
                                                    查看详情
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
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
        </div>
    );
};

export default ArchiveApprovalView;
