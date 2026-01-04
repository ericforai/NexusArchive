// Input: React、lucide-react、destructionApi、useFondsStore
// Output: AppraisalListPage 组件
// Pos: 鉴定清单生成页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { FileText, Loader2, Download, Eye, Calendar, CheckCircle2, Clock, XCircle } from 'lucide-react';
import { destructionApi, AppraisalList } from '../../api/destruction';
import { useFondsStore } from '../../store';
import { toast } from '../../utils/notificationService';

/**
 * 鉴定清单生成页面
 * 
 * 功能：
 * 1. 展示鉴定清单列表
 * 2. 支持导出（Excel/PDF）
 * 3. 查看鉴定清单详情
 * 
 * PRD 来源: Section 13 - 档案销毁
 */
export const AppraisalListPage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [lists, setLists] = useState<AppraisalList[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [selectedList, setSelectedList] = useState<AppraisalList | null>(null);
    const [showDetailModal, setShowDetailModal] = useState(false);

    const loadLists = async () => {
        setLoading(true);
        try {
            const params: any = {
                page,
                size: pageSize,
            };
            if (statusFilter) params.status = statusFilter;
            
            const res = await destructionApi.getAppraisalLists(params);
            if (res.code === 200 && res.data) {
                setLists(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载鉴定清单失败', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadLists();
    }, [page, statusFilter]);

    const handleViewDetail = async (id: string) => {
        try {
            const res = await destructionApi.getAppraisalListDetail(id);
            if (res.code === 200 && res.data) {
                setSelectedList(res.data);
                setShowDetailModal(true);
            }
        } catch (error) {
            console.error('加载详情失败', error);
            toast.error('加载详情失败');
        }
    };

    const handleExport = async (id: string, format: 'excel' | 'pdf') => {
        try {
            const blob = await destructionApi.exportAppraisalList(id, format);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `appraisal-list-${id}.${format === 'excel' ? 'xlsx' : 'pdf'}`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('导出失败', error);
            toast.error('导出失败');
        }
    };

    const getStatusBadge = (status: string) => {
        const configs: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
            'PENDING': {
                label: '待处理',
                color: 'bg-yellow-100 text-yellow-700',
                icon: <Clock className="w-4 h-4" />
            },
            'IN_PROGRESS': {
                label: '进行中',
                color: 'bg-blue-100 text-blue-700',
                icon: <Clock className="w-4 h-4" />
            },
            'COMPLETED': {
                label: '已完成',
                color: 'bg-green-100 text-green-700',
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
                        <FileText className="mr-2" size={28} />
                        鉴定清单管理
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">管理档案销毁鉴定清单，支持导出和查看详情</p>
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
                        <option value="PENDING">待处理</option>
                        <option value="IN_PROGRESS">进行中</option>
                        <option value="COMPLETED">已完成</option>
                    </select>
                    <button
                        onClick={loadLists}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm"
                    >
                        刷新
                    </button>
                </div>
            </div>

            {/* 清单列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : lists.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <FileText size={48} className="mb-4" />
                        <p>暂无鉴定清单</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3">清单名称</th>
                                    <th className="px-4 py-3">全宗号</th>
                                    <th className="px-4 py-3">档案数量</th>
                                    <th className="px-4 py-3">状态</th>
                                    <th className="px-4 py-3">创建时间</th>
                                    <th className="px-4 py-3">完成时间</th>
                                    <th className="px-4 py-3">操作</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {lists.map(list => (
                                    <tr key={list.id} className="hover:bg-slate-50">
                                        <td className="px-4 py-3 font-medium">{list.name}</td>
                                        <td className="px-4 py-3">{list.fondsNo}</td>
                                        <td className="px-4 py-3">{list.archiveCount}</td>
                                        <td className="px-4 py-3">{getStatusBadge(list.status)}</td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {new Date(list.createdAt).toLocaleString('zh-CN')}
                                        </td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {list.completedAt ? new Date(list.completedAt).toLocaleString('zh-CN') : '-'}
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => handleViewDetail(list.id)}
                                                    className="px-3 py-1 text-primary-600 hover:bg-primary-50 rounded text-sm flex items-center gap-1"
                                                >
                                                    <Eye size={14} />
                                                    详情
                                                </button>
                                                <button
                                                    onClick={() => handleExport(list.id, 'excel')}
                                                    className="px-3 py-1 text-green-600 hover:bg-green-50 rounded text-sm flex items-center gap-1"
                                                >
                                                    <Download size={14} />
                                                    Excel
                                                </button>
                                                <button
                                                    onClick={() => handleExport(list.id, 'pdf')}
                                                    className="px-3 py-1 text-red-600 hover:bg-red-50 rounded text-sm flex items-center gap-1"
                                                >
                                                    <Download size={14} />
                                                    PDF
                                                </button>
                                            </div>
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

            {/* 详情模态框 */}
            {showDetailModal && selectedList && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">鉴定清单详情</h2>
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
                                    <span className="text-sm text-slate-500">清单名称:</span>
                                    <p className="font-medium">{selectedList.name}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">全宗号:</span>
                                    <p className="font-medium">{selectedList.fondsNo}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">档案数量:</span>
                                    <p className="font-medium">{selectedList.archiveCount}</p>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">状态:</span>
                                    <div className="mt-1">{getStatusBadge(selectedList.status)}</div>
                                </div>
                                <div>
                                    <span className="text-sm text-slate-500">创建时间:</span>
                                    <p className="font-medium">{new Date(selectedList.createdAt).toLocaleString('zh-CN')}</p>
                                </div>
                                {selectedList.completedAt && (
                                    <div>
                                        <span className="text-sm text-slate-500">完成时间:</span>
                                        <p className="font-medium">{new Date(selectedList.completedAt).toLocaleString('zh-CN')}</p>
                                    </div>
                                )}
                            </div>
                            <div className="pt-4 border-t flex gap-3">
                                <button
                                    onClick={() => handleExport(selectedList.id, 'excel')}
                                    className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 flex items-center gap-2"
                                >
                                    <Download size={16} />
                                    导出Excel
                                </button>
                                <button
                                    onClick={() => handleExport(selectedList.id, 'pdf')}
                                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 flex items-center gap-2"
                                >
                                    <Download size={16} />
                                    导出PDF
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AppraisalListPage;



