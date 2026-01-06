// Input: React、lucide-react、userLifecycleApi
// Output: AccessReviewPage 组件
// Pos: 定期复核（Access Review）页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { ShieldCheck, Loader2, CheckCircle2, XCircle, Eye, Clock, User, MessageSquare } from 'lucide-react';
import { userLifecycleApi, AccessReviewTask, ExecuteReviewRequest } from '../../api/userLifecycle';
import { toast } from '../../utils/notificationService';

/**
 * 定期复核（Access Review）页面
 * 
 * 功能：
 * 1. 复核任务列表
 * 2. 复核执行：查看用户权限、确认/撤销权限
 * 3. 复核历史记录
 * 
 * PRD 来源: Section 4.2 - 用户生命周期管理
 */
export const AccessReviewPage: React.FC = () => {
    const [tasks, setTasks] = useState<AccessReviewTask[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [selectedTask, setSelectedTask] = useState<AccessReviewTask | null>(null);
    const [showReviewModal, setShowReviewModal] = useState(false);
    const [reviewForm, setReviewForm] = useState<ExecuteReviewRequest>({
        taskId: '',
        result: 'APPROVED',
        comment: '',
        revokeRoleIds: [],
    });
    const [submitting, setSubmitting] = useState(false);

    const loadTasks = async () => {
        setLoading(true);
        try {
            const params: any = { page, size: pageSize };
            if (statusFilter) params.status = statusFilter;
            
            const res = await userLifecycleApi.getReviewTasks(params);
            if (res.code === 200 && res.data) {
                setTasks(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载复核任务失败', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTasks();
    }, [page, statusFilter]);

    const handleReview = (task: AccessReviewTask) => {
        setSelectedTask(task);
        setReviewForm({
            taskId: task.id,
            result: 'APPROVED',
            comment: '',
            revokeRoleIds: [],
        });
        setShowReviewModal(true);
    };

    const handleSubmitReview = async () => {
        if (!selectedTask) return;

        setSubmitting(true);
        try {
            const res = await userLifecycleApi.executeReview(reviewForm);
            if (res.code === 200) {
                toast.success('复核完成');
                setShowReviewModal(false);
                loadTasks();
            } else {
                toast.error(res.message || '复核失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '复核失败');
        } finally {
            setSubmitting(false);
        }
    };

    const getStatusBadge = (status: string) => {
        const configs: Record<string, { label: string; color: string }> = {
            'PENDING': { label: '待复核', color: 'bg-yellow-100 text-yellow-700' },
            'IN_PROGRESS': { label: '复核中', color: 'bg-blue-100 text-blue-700' },
            'COMPLETED': { label: '已完成', color: 'bg-green-100 text-green-700' },
        };
        const config = configs[status] || { label: status, color: 'bg-slate-100 text-slate-700' };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${config.color}`}>
                {config.label}
            </span>
        );
    };

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <ShieldCheck className="mr-2" size={28} />
                        权限定期复核
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">定期复核用户权限，确保权限的合理性和安全性</p>
                </div>
            </div>

            {/* 筛选条件 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <div className="flex items-center gap-4">
                    <label className="text-sm font-medium text-slate-700">状态筛选:</label>
                    <select
                        value={statusFilter}
                        onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
                        className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                    >
                        <option value="">全部</option>
                        <option value="PENDING">待复核</option>
                        <option value="IN_PROGRESS">复核中</option>
                        <option value="COMPLETED">已完成</option>
                    </select>
                    <button
                        onClick={loadTasks}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm"
                    >
                        刷新
                    </button>
                </div>
            </div>

            {/* 任务列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : tasks.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <ShieldCheck size={48} className="mb-4" />
                        <p>暂无复核任务</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3">用户</th>
                                    <th className="px-4 py-3">角色</th>
                                    <th className="px-4 py-3">复核日期</th>
                                    <th className="px-4 py-3">状态</th>
                                    <th className="px-4 py-3">复核人</th>
                                    <th className="px-4 py-3">复核结果</th>
                                    <th className="px-4 py-3">操作</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {tasks.map(task => (
                                    <tr key={task.id} className="hover:bg-slate-50">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <User size={16} className="text-slate-400" />
                                                <span>{task.userName}</span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex flex-wrap gap-1">
                                                {task.roleNames.map((role, idx) => (
                                                    <span key={idx} className="px-2 py-1 bg-slate-100 text-slate-700 rounded text-xs">
                                                        {role}
                                                    </span>
                                                ))}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {new Date(task.reviewDate).toLocaleDateString('zh-CN')}
                                        </td>
                                        <td className="px-4 py-3">{getStatusBadge(task.status)}</td>
                                        <td className="px-4 py-3">{task.reviewerName || '-'}</td>
                                        <td className="px-4 py-3">
                                            {task.reviewResult ? (
                                                task.reviewResult === 'APPROVED' ? (
                                                    <span className="text-green-600">已批准</span>
                                                ) : (
                                                    <span className="text-red-600">已撤销</span>
                                                )
                                            ) : (
                                                <span className="text-slate-400">-</span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3">
                                            {task.status === 'PENDING' && (
                                                <button
                                                    onClick={() => handleReview(task)}
                                                    className="px-3 py-1 bg-primary-600 text-white rounded text-sm hover:bg-primary-700 flex items-center gap-1"
                                                >
                                                    <ShieldCheck size={14} />
                                                    复核
                                                </button>
                                            )}
                                            {task.status === 'COMPLETED' && (
                                                <button
                                                    onClick={() => handleReview(task)}
                                                    className="px-3 py-1 text-slate-600 hover:bg-slate-100 rounded text-sm flex items-center gap-1"
                                                >
                                                    <Eye size={14} />
                                                    查看
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
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

            {/* 复核模态框 */}
            {showReviewModal && selectedTask && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">权限复核</h2>
                            <button
                                onClick={() => setShowReviewModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                ×
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <span className="text-sm text-slate-500">用户:</span>
                                    <p className="font-medium">{selectedTask.userName}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">复核日期:</span>
                                    <p className="font-medium">{new Date(selectedTask.reviewDate).toLocaleDateString('zh-CN')}</p>
                                </div>
                            </div>
                            <div>
                                <span className="text-sm text-slate-500">当前角色:</span>
                                <div className="mt-2 flex flex-wrap gap-2">
                                    {selectedTask.roleNames.map((role, idx) => (
                                        <span key={idx} className="px-3 py-1 bg-slate-100 text-slate-700 rounded-full text-sm">
                                            {role}
                                        </span>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    复核结果
                                </label>
                                <div className="space-y-2">
                                    <label className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50">
                                        <input
                                            type="radio"
                                            name="reviewResult"
                                            checked={reviewForm.result === 'APPROVED'}
                                            onChange={() => setReviewForm({ ...reviewForm, result: 'APPROVED' })}
                                            className="w-4 h-4 text-primary-600 border-slate-300 focus:ring-primary-500"
                                        />
                                        <CheckCircle2 className="text-green-600" size={20} />
                                        <span>批准（保持权限）</span>
                                    </label>
                                    <label className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50">
                                        <input
                                            type="radio"
                                            name="reviewResult"
                                            checked={reviewForm.result === 'REVOKED'}
                                            onChange={() => setReviewForm({ ...reviewForm, result: 'REVOKED' })}
                                            className="w-4 h-4 text-primary-600 border-slate-300 focus:ring-primary-500"
                                        />
                                        <XCircle className="text-red-600" size={20} />
                                        <span>撤销权限</span>
                                    </label>
                                </div>
                            </div>
                            {reviewForm.result === 'REVOKED' && (
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-2">
                                        撤销角色（可选，留空则撤销所有角色）
                                    </label>
                                    <div className="space-y-2">
                                        {selectedTask.roleNames.map((roleName, idx) => {
                                            const roleId = `role-${idx}`; // 模拟角色ID
                                            return (
                                                <label key={idx} className="flex items-center gap-2 p-2 border border-slate-200 rounded cursor-pointer hover:bg-slate-50">
                                                    <input
                                                        type="checkbox"
                                                        checked={reviewForm.revokeRoleIds?.includes(roleId) || false}
                                                        onChange={(e) => {
                                                            const ids = reviewForm.revokeRoleIds || [];
                                                            if (e.target.checked) {
                                                                setReviewForm({ ...reviewForm, revokeRoleIds: [...ids, roleId] });
                                                            } else {
                                                                setReviewForm({ ...reviewForm, revokeRoleIds: ids.filter(id => id !== roleId) });
                                                            }
                                                        }}
                                                        className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                                                    />
                                                    <span>{roleName}</span>
                                                </label>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    复核意见
                                </label>
                                <textarea
                                    value={reviewForm.comment}
                                    onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                                    rows={4}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                    placeholder="请输入复核意见..."
                                />
                            </div>
                            <div className="flex gap-3 pt-4 border-t">
                                <button
                                    onClick={handleSubmitReview}
                                    disabled={submitting}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {submitting ? (
                                        <Loader2 className="animate-spin" size={16} />
                                    ) : (
                                        <ShieldCheck size={16} />
                                    )}
                                    提交复核
                                </button>
                                <button
                                    onClick={() => setShowReviewModal(false)}
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

export default AccessReviewPage;





