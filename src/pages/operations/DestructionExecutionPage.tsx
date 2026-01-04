// Input: React、lucide-react、destructionApi
// Output: DestructionExecutionPage 组件
// Pos: 销毁执行页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Flame, Loader2, CheckCircle2, AlertTriangle, Play, RefreshCw } from 'lucide-react';
import { destructionApi, Destruction } from '../../api/destruction';
import { toast } from '../../utils/notificationService';

/**
 * 销毁执行页面
 * 
 * 功能：
 * 1. 已审批的销毁任务列表
 * 2. 执行销毁操作
 * 3. 销毁进度显示
 * 
 * PRD 来源: Section 13 - 档案销毁
 */
export const DestructionExecutionPage: React.FC = () => {
    const [destructions, setDestructions] = useState<Destruction[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;
    const [executing, setExecuting] = useState<string | null>(null);

    const loadDestructions = async () => {
        setLoading(true);
        try {
            const res = await destructionApi.getDestructions({
                page,
                limit: pageSize,
                status: 'APPROVED',
            });
            if (res.code === 200 && res.data) {
                setDestructions(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载销毁任务失败', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadDestructions();
    }, [page]);

    const handleExecute = async (id: string) => {
        if (!window.confirm('确认执行销毁操作吗？此操作不可逆！')) {
            return;
        }

        setExecuting(id);
        try {
            const res = await destructionApi.executeDestruction(id);
            if (res.code === 200) {
                toast.success('销毁执行成功');
                loadDestructions();
            } else {
                toast.error(res.message || '执行失败');
            }
        } catch (error: any) {
            toast.error(error?.response?.data?.message || '执行失败');
        } finally {
            setExecuting(null);
        }
    };

    const getStatusBadge = (status: string) => {
        const configs: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
            'APPROVED': {
                label: '已批准',
                color: 'bg-green-100 text-green-700',
                icon: <CheckCircle2 className="w-4 h-4" />
            },
            'EXECUTING': {
                label: '执行中',
                color: 'bg-blue-100 text-blue-700',
                icon: <Loader2 className="w-4 h-4 animate-spin" />
            },
            'COMPLETED': {
                label: '已完成',
                color: 'bg-slate-100 text-slate-700',
                icon: <CheckCircle2 className="w-4 h-4" />
            },
        };
        const config = configs[status] || { label: status, color: 'bg-slate-100 text-slate-700', icon: null };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-medium flex items-center gap-1 w-fit ${config.color}`}>
                {config.icon}
                {config.label}
            </span>
        );
    };

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Flame className="mr-2" size={28} />
                        销毁执行
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">执行已审批的档案销毁任务</p>
                </div>
                <button
                    onClick={loadDestructions}
                    className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                >
                    <RefreshCw size={16} />
                    刷新
                </button>
            </div>

            {/* 警告提示 */}
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
                <AlertTriangle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
                <div className="text-sm text-red-800">
                    <div className="font-semibold mb-1">警告：销毁操作不可逆</div>
                    <div>执行销毁后，相关档案将被永久删除，无法恢复。请确保已做好备份和确认工作。</div>
                </div>
            </div>

            {/* 销毁任务列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : destructions.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <Flame size={48} className="mb-4" />
                        <p>暂无待执行的销毁任务</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3">申请ID</th>
                                    <th className="px-4 py-3">申请人</th>
                                    <th className="px-4 py-3">档案数量</th>
                                    <th className="px-4 py-3">状态</th>
                                    <th className="px-4 py-3">第一审批人</th>
                                    <th className="px-4 py-3">第二审批人</th>
                                    <th className="px-4 py-3">审批时间</th>
                                    <th className="px-4 py-3">操作</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {destructions.map(destruction => (
                                    <tr key={destruction.id} className="hover:bg-slate-50">
                                        <td className="px-4 py-3 font-mono text-xs">{destruction.id}</td>
                                        <td className="px-4 py-3">{destruction.applicantName}</td>
                                        <td className="px-4 py-3">{destruction.archiveCount}</td>
                                        <td className="px-4 py-3">{getStatusBadge(destruction.status)}</td>
                                        <td className="px-4 py-3">{destruction.firstApproverName || '-'}</td>
                                        <td className="px-4 py-3">{destruction.secondApproverName || '-'}</td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {destruction.secondApprovalTime 
                                                ? new Date(destruction.secondApprovalTime).toLocaleString('zh-CN')
                                                : '-'}
                                        </td>
                                        <td className="px-4 py-3">
                                            {destruction.status === 'APPROVED' && (
                                                <button
                                                    onClick={() => handleExecute(destruction.id)}
                                                    disabled={executing === destruction.id}
                                                    className="px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                                                >
                                                    {executing === destruction.id ? (
                                                        <Loader2 className="animate-spin" size={14} />
                                                    ) : (
                                                        <Play size={14} />
                                                    )}
                                                    执行销毁
                                                </button>
                                            )}
                                            {destruction.status === 'EXECUTING' && (
                                                <span className="text-sm text-blue-600">执行中...</span>
                                            )}
                                            {destruction.status === 'COMPLETED' && (
                                                <span className="text-sm text-slate-400">已完成</span>
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
        </div>
    );
};

export default DestructionExecutionPage;



