// Input: React、本地模块 api/borrowing、hooks/usePermissions
// Output: React 组件 BorrowingView
// Pos: src/pages/utilization/BorrowingView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { borrowingApi, BorrowingRecord } from '../../api/borrowing';
import { usePermissions } from '../../hooks/usePermissions';
import {
    Plus,
    Search,
    BookOpen,
    CheckCircle,
    XCircle,
    RotateCcw,
    Loader2,
    ShieldCheck,
    Ban,
    Download,
    FileSpreadsheet
} from 'lucide-react';
import { Clock } from 'lucide-react';
import { triggerAuditRefresh } from '../../utils/audit';

const STATUS_OPTIONS = [
    { key: 'ALL', label: '全部' },
    { key: 'PENDING', label: '待审批' },
    { key: 'APPROVED', label: '已通过' },
    { key: 'REJECTED', label: '已拒绝' },
    { key: 'RETURNED', label: '已归还' },
    { key: 'CANCELLED', label: '已取消' }
];

const STATUS_META: Record<
    string,
    { label: string; color: string; icon: React.ReactNode }
> = {
    APPROVED: {
        label: '已通过',
        color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        icon: <CheckCircle size={12} />
    },
    PENDING: {
        label: '待审批',
        color: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: <Clock size={12} />
    },
    RETURNED: {
        label: '已归还',
        color: 'bg-slate-100 text-slate-700 border-slate-200',
        icon: <RotateCcw size={12} />
    },
    REJECTED: {
        label: '已拒绝',
        color: 'bg-rose-50 text-rose-700 border-rose-200',
        icon: <XCircle size={12} />
    },
    CANCELLED: {
        label: '已取消',
        color: 'bg-slate-100 text-slate-500 border-slate-200',
        icon: <Ban size={12} />
    }
};

export const BorrowingView: React.FC = () => {
    const [borrowings, setBorrowings] = useState<BorrowingRecord[]>([]);
    const [loading, setLoading] = useState(false);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [onlyMine, setOnlyMine] = useState(false);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [search, setSearch] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [toast, setToast] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [creating, setCreating] = useState(false);
    const [rowLoading, setRowLoading] = useState<string | null>(null);
    const [createForm, setCreateForm] = useState({
        archiveId: '',
        reason: '',
        expectedReturnDate: ''
    });
    const { hasPermission } = usePermissions();
    const canApprove = hasPermission('borrowing:approve');
    const canReturn = hasPermission('borrowing:return');
    const canCancel = hasPermission('borrowing:cancel');

    const displayedBorrowings = useMemo(() => {
        if (!search.trim()) return borrowings;
        const keyword = search.toLowerCase();
        return borrowings.filter(
            (item) =>
                (item.archiveTitle || '').toLowerCase().includes(keyword) ||
                (item.userName || '').toLowerCase().includes(keyword) ||
                (item.reason || '').toLowerCase().includes(keyword)
        );
    }, [borrowings, search]);

    const totalPages = Math.max(1, Math.ceil(total / pageSize));

    const fetchBorrowings = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await borrowingApi.getBorrowings({
                status: statusFilter === 'ALL' ? undefined : statusFilter,
                my: onlyMine,
                page,
                limit: pageSize
            });
            if (res.code === 200 && res.data) {
                setBorrowings(res.data.records || []);
                setTotal(res.data.total || 0);
            } else {
                setBorrowings([]);
                setTotal(0);
                setError(res.message || '加载借阅列表失败');
            }
        } catch (err) {
            console.error('Failed to fetch borrowings', err);
            setBorrowings([]);
            setTotal(0);
            setError('加载借阅列表失败，请稍后重试');
        } finally {
            setLoading(false);
        }
    }, [statusFilter, onlyMine, page, pageSize]);

    useEffect(() => {
        fetchBorrowings();
    }, [fetchBorrowings]);

    const showToast = (text: string, type: 'success' | 'error' = 'success') => {
        setToast({ text, type });
        setTimeout(() => setToast(null), 2600);
    };

    const handleCreateSubmit = async () => {
        if (!createForm.archiveId.trim()) {
            showToast('请输入档案ID', 'error');
            return;
        }
        setCreating(true);
        try {
            const res = await borrowingApi.createBorrowing({
                applicantId: 'user-1', // Mock user
                applicantName: '测试用户', // Mock user
                deptId: 'dept-1', // Mock dept
                deptName: '研发部', // Mock dept
                archiveIds: [createForm.archiveId.trim()], // Single to Array
                purpose: createForm.reason.trim(),
                borrowType: 'READING', // Default type
                expectedStartDate: new Date().toISOString().split('T')[0], // Today
                expectedEndDate: createForm.expectedReturnDate || new Date(Date.now() + 86400000 * 30).toISOString().split('T')[0] // Default 30 days
            });
            if (res.code === 200) {
                showToast('借阅申请已提交');
                setShowCreateModal(false);
                setCreateForm({ archiveId: '', reason: '', expectedReturnDate: '' });
                setPage(1);
                fetchBorrowings();
                triggerAuditRefresh();
            } else {
                showToast(res.message || '提交失败', 'error');
            }
        } catch (err) {
            console.error('Create borrowing failed', err);
            showToast('提交失败，请稍后重试', 'error');
        } finally {
            setCreating(false);
        }
    };

    const handleApprove = async (id: string, approved: boolean) => {
        if (!canApprove) {
            showToast('无审批权限', 'error');
            return;
        }
        const tip = approved ? '通过该借阅申请？' : '拒绝该借阅申请？';
        if (!confirm(tip)) return;
        const comment = prompt('审批意见（可选）', '') || '';
        setRowLoading(id);
        try {
            const res = await borrowingApi.approveBorrowing(id, {
                approverId: 'admin-1', // Mock admin
                approverName: '管理员',
                approved,
                comment: comment || (approved ? '同意' : '拒绝')
            });
            if (res.code === 200) {
                showToast(approved ? '已审批通过' : '已拒绝申请');
                fetchBorrowings();
                triggerAuditRefresh();
            } else {
                showToast(res.message || '操作失败', 'error');
            }
        } catch (err) {
            console.error('Approve borrowing failed', err);
            showToast('操作失败，请稍后再试', 'error');
        } finally {
            setRowLoading(null);
        }
    };

    const handleReturn = async (id: string) => {
        if (!canReturn) {
            showToast('无归还操作权限', 'error');
            return;
        }
        if (!confirm('确认归还此档案吗？')) return;
        setRowLoading(id);
        try {
            const res = await borrowingApi.returnArchive(id, 'admin-1');
            if (res.code === 200) {
                showToast('已归还');
                fetchBorrowings();
                triggerAuditRefresh();
            } else {
                showToast(res.message || '归还失败', 'error');
            }
        } catch (err) {
            console.error('Return borrowing failed', err);
            showToast('归还失败，请稍后重试', 'error');
        } finally {
            setRowLoading(null);
        }
    };

    const handleCancel = async (id: string) => {
        if (!canCancel) {
            showToast('无取消权限', 'error');
            return;
        }
        if (!confirm('确认取消此借阅申请？')) return;
        setRowLoading(id);
        try {
            const res = await borrowingApi.cancelBorrowing(id);
            if (res.code === 200) {
                showToast('已取消申请');
                fetchBorrowings();
                triggerAuditRefresh();
            } else {
                showToast(res.message || '取消失败', 'error');
            }
        } catch (err) {
            console.error('Cancel borrowing failed', err);
            showToast('取消失败，请稍后再试', 'error');
        } finally {
            setRowLoading(null);
        }
    };

    const handleExport = (format: 'csv' | 'excel') => {
        if (displayedBorrowings.length === 0) {
            showToast('当前没有可导出的数据', 'error');
            return;
        }
        const headers = ['申请单号', '档案题名', '申请人', '借阅日期', '预计归还', '实际归还', '状态', '审批意见'];
        const separator = format === 'csv' ? ',' : '\t';
        const rows = displayedBorrowings.map((item) =>
            [
                item.id,
                item.archiveTitle || '-',
                item.userName || '-',
                item.borrowDate || '-',
                item.expectedReturnDate || '-',
                item.actualReturnDate || '-',
                STATUS_META[item.status]?.label || item.status || '-',
                item.approvalComment || '-'
            ].map((cell) => `"${(cell || '').toString().replace(/"/g, '""')}"`).join(separator)
        );
        const content = [headers.join(separator), ...rows].join('\n');
        const blob = new Blob([`\uFEFF${content}`], {
            type: format === 'csv' ? 'text/csv;charset=utf-8;' : 'application/vnd.ms-excel'
        });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = format === 'csv' ? 'borrowing.csv' : 'borrowing.xls';
        link.click();
        URL.revokeObjectURL(link.href);
        showToast('导出开始下载');
    };

    return (
        <div className="p-6 h-full flex flex-col bg-slate-50">
            <div className="flex justify-between items-start mb-6">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
                        <BookOpen className="text-blue-600" /> 档案借阅管理
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">
                        管理借阅申请的发起、审批、归还与取消，支持状态筛选与导出
                    </p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => handleExport('csv')}
                        className="px-3 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 flex items-center gap-2"
                    >
                        <Download size={16} /> CSV
                    </button>
                    <button
                        onClick={() => handleExport('excel')}
                        className="px-3 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 flex items-center gap-2"
                    >
                        <FileSpreadsheet size={16} /> Excel
                    </button>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors shadow-sm"
                    >
                        <Plus size={18} /> 发起借阅
                    </button>
                </div>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm mb-4 flex flex-wrap gap-3 items-center">
                <div className="relative flex-1 min-w-[260px] max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="搜索申请人、档案题名或原因..."
                        className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    />
                </div>
                <div className="flex gap-2 flex-wrap">
                    {STATUS_OPTIONS.map((status) => (
                        <button
                            key={status.key}
                            onClick={() => {
                                setStatusFilter(status.key);
                                setPage(1);
                            }}
                            className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${statusFilter === status.key
                                ? 'bg-slate-900 text-white'
                                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                                }`}
                        >
                            {status.label}
                        </button>
                    ))}
                </div>
                <button
                    onClick={() => {
                        setOnlyMine((prev) => !prev);
                        setPage(1);
                    }}
                    className={`px-3 py-2 rounded-lg text-sm font-medium flex items-center gap-2 transition-colors ${onlyMine ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                        }`}
                >
                    <ShieldCheck size={16} /> 只看我的
                </button>
            </div>

            {error && (
                <div className="bg-rose-50 border border-rose-200 text-rose-700 rounded-lg p-3 mb-3 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <XCircle size={16} />
                        <span>{error}</span>
                    </div>
                    <button onClick={fetchBorrowings} className="text-sm text-rose-700 underline">
                        重试
                    </button>
                </div>
            )}

            <div className="bg-white rounded-xl border border-slate-200 shadow-sm flex-1 overflow-hidden flex flex-col">
                <div className="overflow-x-auto flex-1">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-medium">
                            <tr>
                                <th className="p-4 w-12">#</th>
                                <th className="p-4">档案题名</th>
                                <th className="p-4">申请人</th>
                                <th className="p-4">借阅日期</th>
                                <th className="p-4">预计归还</th>
                                <th className="p-4">实际归还</th>
                                <th className="p-4">状态</th>
                                <th className="p-4 text-right">操作</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr>
                                    <td colSpan={8} className="p-8 text-center text-slate-400">
                                        <div className="flex items-center justify-center gap-2">
                                            <Loader2 className="animate-spin" size={16} />
                                            正在加载借阅数据...
                                        </div>
                                    </td>
                                </tr>
                            ) : displayedBorrowings.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="p-10 text-center text-slate-400">
                                        <div className="flex flex-col items-center gap-3">
                                            <BookOpen size={28} className="text-slate-300" />
                                            <div>暂无借阅记录</div>
                                            <button
                                                onClick={() => setShowCreateModal(true)}
                                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                                            >
                                                发起首个借阅
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ) : (
                                displayedBorrowings.map((item, idx) => (
                                    <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                                        <td className="p-4 text-slate-400">{(page - 1) * pageSize + idx + 1}</td>
                                        <td className="p-4">
                                            <div className="font-medium text-slate-800">{item.archiveTitle || '未知档案'}</div>
                                            <div className="text-xs text-slate-500 mt-1 line-clamp-1">{item.reason || '—'}</div>
                                        </td>
                                        <td className="p-4 text-slate-600">{item.userName || '-'}</td>
                                        <td className="p-4 text-slate-600">{item.borrowDate || '-'}</td>
                                        <td className="p-4 text-slate-600">{item.expectedReturnDate || '-'}</td>
                                        <td className="p-4 text-slate-600">{item.actualReturnDate || '-'}</td>
                                        <td className="p-4">
                                            <span
                                                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${STATUS_META[item.status]?.color || 'bg-slate-100 text-slate-600 border-slate-200'}`}
                                            >
                                                {STATUS_META[item.status]?.icon}
                                                {STATUS_META[item.status]?.label || item.status || '未知状态'}
                                            </span>
                                            {item.approvalComment && (
                                                <div className="text-xs text-slate-400 mt-1 line-clamp-1">
                                                    {item.approvalComment}
                                                </div>
                                            )}
                                        </td>
                                        <td className="p-4 text-right">
                                            {item.status === 'PENDING' && (
                                                <div className="flex justify-end gap-2">
                                                    <button
                                                        onClick={() => handleApprove(item.id, true)}
                                                        disabled={rowLoading === item.id}
                                                        className="text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg px-3 py-1.5 text-xs font-medium disabled:opacity-50"
                                                    >
                                                        {rowLoading === item.id ? '处理中...' : '通过'}
                                                    </button>
                                                    <button
                                                        onClick={() => handleApprove(item.id, false)}
                                                        disabled={rowLoading === item.id}
                                                        className="text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg px-3 py-1.5 text-xs font-medium disabled:opacity-50"
                                                    >
                                                        拒绝
                                                    </button>
                                                    <button
                                                        onClick={() => handleCancel(item.id)}
                                                        disabled={rowLoading === item.id}
                                                        className="text-slate-600 bg-slate-100 hover:bg-slate-200 border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-medium disabled:opacity-50"
                                                    >
                                                        取消
                                                    </button>
                                                </div>
                                            )}
                                            {item.status === 'APPROVED' && (
                                                <button
                                                    onClick={() => handleReturn(item.id)}
                                                    disabled={rowLoading === item.id}
                                                    className="text-blue-700 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-lg px-3 py-1.5 text-xs font-medium disabled:opacity-50"
                                                >
                                                    {rowLoading === item.id ? '处理中...' : '归还'}
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="p-4 border-t border-slate-200 flex items-center justify-between text-sm text-slate-500">
                    <div className="flex items-center gap-3">
                        <span>
                            当前显示 {displayedBorrowings.length} / {total} 条
                        </span>
                        <select
                            value={pageSize}
                            onChange={(e) => {
                                setPageSize(Number(e.target.value));
                                setPage(1);
                            }}
                            className="border border-slate-200 rounded-lg px-2 py-1 text-sm"
                        >
                            {[10, 20, 50].map((size) => (
                                <option key={size} value={size}>
                                    每页 {size}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            disabled={page <= 1}
                            onClick={() => setPage((p) => Math.max(1, p - 1))}
                            className="px-3 py-1.5 border border-slate-200 rounded-lg disabled:opacity-40"
                        >
                            上一页
                        </button>
                        <span>
                            第 {page} / {totalPages} 页
                        </span>
                        <button
                            disabled={page >= totalPages}
                            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                            className="px-3 py-1.5 border border-slate-200 rounded-lg disabled:opacity-40"
                        >
                            下一页
                        </button>
                    </div>
                </div>
            </div>

            {showCreateModal && (
                <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-40">
                    <div className="bg-white rounded-xl shadow-xl border border-slate-200 w-full max-w-lg p-6">
                        <div className="flex justify-between items-center mb-4">
                            <div>
                                <h3 className="text-lg font-bold text-slate-800">发起借阅申请</h3>
                                <p className="text-sm text-slate-500">填写档案ID和借阅原因</p>
                            </div>
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                <XCircle size={18} />
                            </button>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm text-slate-600 mb-1">档案ID</label>
                                <input
                                    value={createForm.archiveId}
                                    onChange={(e) => setCreateForm((prev) => ({ ...prev, archiveId: e.target.value }))}
                                    placeholder="请输入档案ID"
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                                />
                            </div>
                            <div>
                                <label className="block text-sm text-slate-600 mb-1">借阅原因</label>
                                <textarea
                                    value={createForm.reason}
                                    onChange={(e) => setCreateForm((prev) => ({ ...prev, reason: e.target.value }))}
                                    rows={3}
                                    placeholder="说明借阅用途，便于审批"
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                                />
                            </div>
                            <div>
                                <label className="block text-sm text-slate-600 mb-1">预计归还日期</label>
                                <input
                                    type="date"
                                    value={createForm.expectedReturnDate}
                                    onChange={(e) =>
                                        setCreateForm((prev) => ({ ...prev, expectedReturnDate: e.target.value }))
                                    }
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                                />
                            </div>
                        </div>
                        <div className="flex justify-end gap-2 mt-6">
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="px-4 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50"
                            >
                                取消
                            </button>
                            <button
                                onClick={handleCreateSubmit}
                                disabled={creating}
                                className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-60"
                            >
                                {creating ? '提交中...' : '提交申请'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {toast && (
                <div
                    className={`fixed bottom-6 right-6 px-4 py-2 rounded-lg shadow-lg text-white ${toast.type === 'success' ? 'bg-emerald-600' : 'bg-rose-600'
                        }`}
                >
                    {toast.text}
                </div>
            )}
        </div>
    );
};

export default BorrowingView;
