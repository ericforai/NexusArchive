// Input: React、lucide-react、authTicketApi
// Output: AuthTicketListPage 组件
// Pos: 授权票据列表和审批页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Shield, Search, Filter, CheckCircle2, XCircle, Clock, Ban, Eye, Loader2, MessageSquare } from 'lucide-react';
import { authTicketApi, AuthTicketDetail, ApprovalRequest } from '../../api/authTicket';
import { useFondsStore } from '../../store';

/**
 * 授权票据列表和审批页面
 * 
 * 功能：
 * 1. 查看授权票据列表
 * 2. 审批授权票据（第一审批、第二审批）
 * 3. 撤销授权票据
 * 4. 查看授权票据详情
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
export const AuthTicketListPage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [tickets, setTickets] = useState<AuthTicketDetail[]>([]);
    const [loading, setLoading] = useState(false);
    const [filterStatus, setFilterStatus] = useState<string>('all');
    const [selectedTicket, setSelectedTicket] = useState<AuthTicketDetail | null>(null);
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [showApprovalModal, setShowApprovalModal] = useState(false);
    const [approvalType, setApprovalType] = useState<'first' | 'second'>('first');
    const [approvalForm, setApprovalForm] = useState<ApprovalRequest>({
        comment: '',
        approved: true,
    });
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    // 模拟数据（实际应该从API获取）
    useEffect(() => {
        // TODO: 实现获取授权票据列表的API
        // const loadTickets = async () => {
        //     setLoading(true);
        //     try {
        //         const res = await authTicketApi.list({ status: filterStatus });
        //         if (res.code === 200 && res.data) {
        //             setTickets(res.data);
        //         }
        //     } catch (error) {
        //         console.error('加载授权票据列表失败', error);
        //     } finally {
        //         setLoading(false);
        //     }
        // };
        // loadTickets();
    }, [filterStatus]);

    const handleViewDetail = async (ticketId: string) => {
        setLoading(true);
        try {
            const res = await authTicketApi.getDetail(ticketId);
            if (res.code === 200 && res.data) {
                setSelectedTicket(res.data);
                setShowDetailModal(true);
            }
        } catch (error) {
            console.error('查询授权票据详情失败', error);
            setMessage({ type: 'error', text: '查询详情失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (ticketId: string, type: 'first' | 'second') => {
        setSelectedTicket(tickets.find(t => t.id === ticketId) || null);
        setApprovalType(type);
        setApprovalForm({ comment: '', approved: true });
        setShowApprovalModal(true);
    };

    const handleSubmitApproval = async () => {
        if (!selectedTicket) return;

        setSubmitting(true);
        try {
            const res = approvalType === 'first'
                ? await authTicketApi.firstApproval(selectedTicket.id, approvalForm)
                : await authTicketApi.secondApproval(selectedTicket.id, approvalForm);
            
            if (res.code === 200) {
                setMessage({ type: 'success', text: '审批成功' });
                setShowApprovalModal(false);
                // 重新加载列表
            } else {
                setMessage({ type: 'error', text: res.message || '审批失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '审批失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const handleRevoke = async (ticketId: string) => {
        const reason = prompt('请输入撤销原因:');
        if (!reason) return;

        setSubmitting(true);
        try {
            const res = await authTicketApi.revoke(ticketId, reason);
            if (res.code === 200) {
                setMessage({ type: 'success', text: '撤销成功' });
                // 重新加载列表
            } else {
                setMessage({ type: 'error', text: res.message || '撤销失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '撤销失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const getStatusLabel = (status: string) => {
        const labels: Record<string, string> = {
            'PENDING': '待审批',
            'APPROVED': '已批准',
            'REJECTED': '已拒绝',
            'REVOKED': '已撤销',
            'EXPIRED': '已过期',
        };
        return labels[status] || status;
    };

    const getStatusColor = (status: string) => {
        const colors: Record<string, string> = {
            'PENDING': 'bg-yellow-100 text-yellow-700',
            'APPROVED': 'bg-green-100 text-green-700',
            'REJECTED': 'bg-red-100 text-red-700',
            'REVOKED': 'bg-slate-100 text-slate-700',
            'EXPIRED': 'bg-orange-100 text-orange-700',
        };
        return colors[status] || 'bg-slate-100 text-slate-700';
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'PENDING':
                return <Clock className="w-4 h-4" />;
            case 'APPROVED':
                return <CheckCircle2 className="w-4 h-4" />;
            case 'REJECTED':
                return <XCircle className="w-4 h-4" />;
            case 'REVOKED':
                return <Ban className="w-4 h-4" />;
            case 'EXPIRED':
                return <Clock className="w-4 h-4" />;
            default:
                return null;
        }
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Shield className="w-6 h-6 text-primary-600" />
                            <h1 className="text-xl font-semibold text-slate-900">跨全宗访问授权票据</h1>
                        </div>
                        <a
                            href="/system/security/auth-ticket/apply"
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm"
                        >
                            + 申请授权票据
                        </a>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">管理和审批跨全宗访问授权票据</p>
                </div>

                {/* Message */}
                {message && (
                    <div className={`mx-6 mt-4 p-3 rounded-lg flex items-center gap-2 ${
                        message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                    }`}>
                        {message.type === 'success' ? (
                            <CheckCircle2 className="w-5 h-5" />
                        ) : (
                            <XCircle className="w-5 h-5" />
                        )}
                        <span>{message.text}</span>
                        <button
                            onClick={() => setMessage(null)}
                            className="ml-auto text-slate-400 hover:text-slate-600"
                        >
                            ×
                        </button>
                    </div>
                )}

                {/* Filters */}
                <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
                    <div className="flex gap-4 items-center">
                        <div className="flex items-center gap-2">
                            <Filter className="w-4 h-4 text-slate-400" />
                            <span className="text-sm text-slate-600">状态筛选:</span>
                        </div>
                        <select
                            value={filterStatus}
                            onChange={(e) => setFilterStatus(e.target.value)}
                            className="px-3 py-1.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                        >
                            <option value="all">全部</option>
                            <option value="PENDING">待审批</option>
                            <option value="APPROVED">已批准</option>
                            <option value="REJECTED">已拒绝</option>
                            <option value="REVOKED">已撤销</option>
                            <option value="EXPIRED">已过期</option>
                        </select>
                    </div>
                </div>

                {/* Ticket List */}
                <div className="flex-1 overflow-y-auto p-6">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : tickets.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                            <Shield className="w-12 h-12 mb-2" />
                            <p>暂无授权票据</p>
                            <a
                                href="/system/security/auth-ticket/apply"
                                className="mt-4 text-primary-600 hover:text-primary-700"
                            >
                                申请授权票据
                            </a>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {tickets.map((ticket) => (
                                <div
                                    key={ticket.id}
                                    className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                                >
                                    <div className="flex items-start justify-between mb-3">
                                        <div className="flex items-center gap-3">
                                            <span className={`px-3 py-1 rounded-full text-xs font-medium flex items-center gap-1 ${getStatusColor(ticket.status)}`}>
                                                {getStatusIcon(ticket.status)}
                                                {getStatusLabel(ticket.status)}
                                            </span>
                                            <span className="text-sm text-slate-600">
                                                票据ID: {ticket.id}
                                            </span>
                                        </div>
                                        <span className="text-xs text-slate-400">
                                            {new Date(ticket.createdAt).toLocaleString('zh-CN')}
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 mb-3">
                                        <div className="text-sm">
                                            <span className="text-slate-500">申请人:</span>
                                            <span className="ml-2 font-medium">{ticket.applicantName}</span>
                                        </div>
                                        <div className="text-sm">
                                            <span className="text-slate-500">源全宗:</span>
                                            <span className="ml-2 font-medium">{ticket.sourceFonds}</span>
                                        </div>
                                        <div className="text-sm">
                                            <span className="text-slate-500">目标全宗:</span>
                                            <span className="ml-2 font-medium">{ticket.targetFonds}</span>
                                        </div>
                                        <div className="text-sm">
                                            <span className="text-slate-500">有效期:</span>
                                            <span className="ml-2 font-medium">
                                                {new Date(ticket.expiresAt).toLocaleString('zh-CN')}
                                            </span>
                                        </div>
                                    </div>

                                    {ticket.reason && (
                                        <div className="mb-3">
                                            <p className="text-sm text-slate-500 mb-1">申请原因:</p>
                                            <p className="text-sm text-slate-700 bg-slate-50 p-2 rounded">
                                                {ticket.reason}
                                            </p>
                                        </div>
                                    )}

                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleViewDetail(ticket.id)}
                                            className="px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50 rounded-lg flex items-center gap-1"
                                        >
                                            <Eye className="w-4 h-4" />
                                            查看详情
                                        </button>
                                        {ticket.status === 'PENDING' && (
                                            <>
                                                {!ticket.approvalChain?.firstApprover && (
                                                    <button
                                                        onClick={() => handleApprove(ticket.id, 'first')}
                                                        className="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-1"
                                                    >
                                                        <CheckCircle2 className="w-4 h-4" />
                                                        第一审批
                                                    </button>
                                                )}
                                                {ticket.approvalChain?.firstApprover?.approved && !ticket.approvalChain?.secondApprover && (
                                                    <button
                                                        onClick={() => handleApprove(ticket.id, 'second')}
                                                        className="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-1"
                                                    >
                                                        <CheckCircle2 className="w-4 h-4" />
                                                        第二审批
                                                    </button>
                                                )}
                                            </>
                                        )}
                                        {(ticket.status === 'PENDING' || ticket.status === 'APPROVED') && (
                                            <button
                                                onClick={() => handleRevoke(ticket.id)}
                                                className="px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded-lg flex items-center gap-1"
                                            >
                                                <Ban className="w-4 h-4" />
                                                撤销
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Detail Modal */}
            {showDetailModal && selectedTicket && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">授权票据详情</h2>
                            <button
                                onClick={() => setShowDetailModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                ×
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <span className="text-sm text-slate-500">票据ID:</span>
                                    <p className="font-medium">{selectedTicket.id}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">状态:</span>
                                    <p className="font-medium">{getStatusLabel(selectedTicket.status)}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">申请人:</span>
                                    <p className="font-medium">{selectedTicket.applicantName}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">源全宗:</span>
                                    <p className="font-medium">{selectedTicket.sourceFonds}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">目标全宗:</span>
                                    <p className="font-medium">{selectedTicket.targetFonds}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">有效期:</span>
                                    <p className="font-medium">{new Date(selectedTicket.expiresAt).toLocaleString('zh-CN')}</p>
                                </div>
                            </div>
                            {selectedTicket.scope && (
                                <div>
                                    <span className="text-sm text-slate-500">访问范围:</span>
                                    <pre className="mt-1 p-3 bg-slate-50 rounded text-xs overflow-x-auto">
                                        {JSON.stringify(selectedTicket.scope, null, 2)}
                                    </pre>
                                </div>
                            )}
                            {selectedTicket.approvalChain && (
                                <div>
                                    <span className="text-sm text-slate-500">审批链:</span>
                                    <div className="mt-2 space-y-2">
                                        {selectedTicket.approvalChain.firstApprover && (
                                            <div className="p-3 bg-slate-50 rounded">
                                                <p className="text-sm font-medium">第一审批人: {selectedTicket.approvalChain.firstApprover.approverName}</p>
                                                <p className="text-xs text-slate-500">
                                                    {selectedTicket.approvalChain.firstApprover.approved ? '已批准' : '已拒绝'}
                                                    {selectedTicket.approvalChain.firstApprover.comment && ` - ${selectedTicket.approvalChain.firstApprover.comment}`}
                                                </p>
                                            </div>
                                        )}
                                        {selectedTicket.approvalChain.secondApprover && (
                                            <div className="p-3 bg-slate-50 rounded">
                                                <p className="text-sm font-medium">第二审批人: {selectedTicket.approvalChain.secondApprover.approverName}</p>
                                                <p className="text-xs text-slate-500">
                                                    {selectedTicket.approvalChain.secondApprover.approved ? '已批准' : '已拒绝'}
                                                    {selectedTicket.approvalChain.secondApprover.comment && ` - ${selectedTicket.approvalChain.secondApprover.comment}`}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Approval Modal */}
            {showApprovalModal && selectedTicket && (
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
                                    rows={3}
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
                                    />
                                    <span className="text-sm text-slate-700">批准</span>
                                </label>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    onClick={handleSubmitApproval}
                                    disabled={submitting}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {submitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                                    提交审批
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
        </div>
    );
};

export default AuthTicketListPage;

