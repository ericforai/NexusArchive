// Input: React、lucide-react、freezeHoldApi、archivesApi
// Output: FreezeHoldPage 组件
// Pos: 冻结/保全管理页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Lock, Loader2, CheckCircle2, XCircle, Plus, Eye, Unlock, AlertCircle, Filter, Search } from 'lucide-react';
import { freezeHoldApi, FreezeHoldRecord, ApplyFreezeHoldRequest, FreezeHoldType, FreezeHoldStatus } from '../../api/freezeHold';
import { useNavigate } from 'react-router-dom';
import { ROUTE_PATHS } from '../../routes/paths';

/**
 * 冻结/保全管理页面
 * 
 * 功能：
 * 1. 冻结/保全申请：选择档案、填写原因、设置期限
 * 2. 冻结/保全列表：展示所有冻结/保全的档案
 * 3. 解除冻结/保全：审批解除申请
 * 
 * PRD 来源: Section 6.2 - 冻结/保全管理
 */
export const FreezeHoldPage: React.FC = () => {
    const navigate = useNavigate();
    const [records, setRecords] = useState<FreezeHoldRecord[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const pageSize = 20;
    const [typeFilter, setTypeFilter] = useState<FreezeHoldType | ''>('');
    const [statusFilter, setStatusFilter] = useState<FreezeHoldStatus | ''>('');
    const [archiveCodeFilter, setArchiveCodeFilter] = useState('');
    const [showApplyModal, setShowApplyModal] = useState(false);
    const [applyForm, setApplyForm] = useState<ApplyFreezeHoldRequest>({
        archiveIds: [],
        type: 'FREEZE',
        reason: '',
        endDate: '',
    });
    const [selectedArchiveCodes, setSelectedArchiveCodes] = useState<string[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [showReleaseModal, setShowReleaseModal] = useState(false);
    const [releaseRecord, setReleaseRecord] = useState<FreezeHoldRecord | null>(null);
    const [releaseReason, setReleaseReason] = useState('');

    useEffect(() => {
        loadRecords();
    }, [page, typeFilter, statusFilter, archiveCodeFilter]);

    const loadRecords = async () => {
        setLoading(true);
        try {
            const params: any = { page, size: pageSize };
            if (typeFilter) params.type = typeFilter;
            if (statusFilter) params.status = statusFilter;
            if (archiveCodeFilter) params.archiveCode = archiveCodeFilter;

            const res = await freezeHoldApi.list(params);
            if (res.code === 200 && res.data) {
                setRecords(res.data.records || []);
                setTotal(res.data.total || 0);
            }
        } catch (error) {
            console.error('加载冻结/保全记录失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleApply = async () => {
        if (applyForm.archiveIds.length === 0) {
            alert('请选择至少一个档案');
            return;
        }
        if (!applyForm.reason.trim()) {
            alert('请填写申请原因');
            return;
        }

        setSubmitting(true);
        try {
            const res = await freezeHoldApi.apply(applyForm);
            if (res.code === 200) {
                alert('申请成功');
                setShowApplyModal(false);
                setApplyForm({ archiveIds: [], type: 'FREEZE', reason: '', endDate: '' });
                setSelectedArchiveCodes([]);
                loadRecords();
            } else {
                alert(res.message || '申请失败');
            }
        } catch (error: any) {
            alert(error?.response?.data?.message || '申请失败');
        } finally {
            setSubmitting(false);
        }
    };

    const handleRelease = async () => {
        if (!releaseRecord) return;
        if (!releaseReason.trim()) {
            alert('请填写解除原因');
            return;
        }

        setSubmitting(true);
        try {
            const res = await freezeHoldApi.release({ id: releaseRecord.id, reason: releaseReason });
            if (res.code === 200) {
                alert('解除成功');
                setShowReleaseModal(false);
                setReleaseRecord(null);
                setReleaseReason('');
                loadRecords();
            } else {
                alert(res.message || '解除失败');
            }
        } catch (error: any) {
            alert(error?.response?.data?.message || '解除失败');
        } finally {
            setSubmitting(false);
        }
    };

    const getTypeLabel = (type: FreezeHoldType) => {
        return type === 'FREEZE' ? '冻结' : '保全';
    };

    const getStatusBadge = (status: FreezeHoldStatus) => {
        const configs: Record<FreezeHoldStatus, { label: string; color: string }> = {
            'ACTIVE': { label: '生效中', color: 'bg-blue-100 text-blue-700' },
            'RELEASED': { label: '已解除', color: 'bg-green-100 text-green-700' },
            'EXPIRED': { label: '已过期', color: 'bg-slate-100 text-slate-700' },
        };
        const config = configs[status];
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${config.color}`}>
                {config.label}
            </span>
        );
    };

    // 注意：这里需要集成档案选择器
    // 由于没有现成的组件，这里使用简单的文本输入来输入档案编号
    const handleArchiveCodeInput = (value: string) => {
        const codes = value.split(/[,\n\s]+/).filter(code => code.trim());
        setSelectedArchiveCodes(codes);
        // 假设档案ID和编号相同，实际应该通过API查询
        setApplyForm({ ...applyForm, archiveIds: codes });
    };

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Lock className="mr-2" size={28} />
                        冻结/保全管理
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">管理档案的冻结和保全状态，防止档案被误操作</p>
                </div>
                <button
                    onClick={() => setShowApplyModal(true)}
                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                >
                    <Plus size={18} />
                    申请冻结/保全
                </button>
            </div>

            {/* 筛选条件 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <div className="flex items-center gap-4 flex-wrap">
                    <div className="flex items-center gap-2">
                        <Filter size={18} className="text-slate-400" />
                        <label className="text-sm font-medium text-slate-700">类型:</label>
                        <select
                            value={typeFilter}
                            onChange={(e) => { setTypeFilter(e.target.value as FreezeHoldType | ''); setPage(1); }}
                            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                        >
                            <option value="">全部</option>
                            <option value="FREEZE">冻结</option>
                            <option value="HOLD">保全</option>
                        </select>
                    </div>
                    <div className="flex items-center gap-2">
                        <label className="text-sm font-medium text-slate-700">状态:</label>
                        <select
                            value={statusFilter}
                            onChange={(e) => { setStatusFilter(e.target.value as FreezeHoldStatus | ''); setPage(1); }}
                            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                        >
                            <option value="">全部</option>
                            <option value="ACTIVE">生效中</option>
                            <option value="RELEASED">已解除</option>
                            <option value="EXPIRED">已过期</option>
                        </select>
                    </div>
                    <div className="flex items-center gap-2 flex-1 max-w-xs">
                        <Search size={18} className="text-slate-400" />
                        <input
                            type="text"
                            value={archiveCodeFilter}
                            onChange={(e) => setArchiveCodeFilter(e.target.value)}
                            placeholder="档案编号"
                            className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                    </div>
                    <button
                        onClick={loadRecords}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm"
                    >
                        查询
                    </button>
                </div>
            </div>

            {/* 记录列表 */}
            <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center h-64">
                        <Loader2 className="animate-spin text-slate-400" size={32} />
                    </div>
                ) : records.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                        <Lock size={48} className="mb-4" />
                        <p>暂无冻结/保全记录</p>
                    </div>
                ) : (
                    <>
                        <table className="w-full text-left text-sm">
                            <thead className="bg-slate-50 border-b">
                                <tr>
                                    <th className="px-4 py-3">档案编号</th>
                                    <th className="px-4 py-3">档案标题</th>
                                    <th className="px-4 py-3">类型</th>
                                    <th className="px-4 py-3">状态</th>
                                    <th className="px-4 py-3">申请原因</th>
                                    <th className="px-4 py-3">申请人</th>
                                    <th className="px-4 py-3">生效日期</th>
                                    <th className="px-4 py-3">到期日期</th>
                                    <th className="px-4 py-3">操作</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {records.map(record => (
                                    <tr key={record.id} className="hover:bg-slate-50">
                                        <td className="px-4 py-3 font-mono text-xs">{record.archiveCode}</td>
                                        <td className="px-4 py-3 max-w-xs truncate" title={record.archiveTitle}>
                                            {record.archiveTitle}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-1 rounded text-xs ${
                                                record.type === 'FREEZE' 
                                                    ? 'bg-red-100 text-red-700' 
                                                    : 'bg-yellow-100 text-yellow-700'
                                            }`}>
                                                {getTypeLabel(record.type)}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">{getStatusBadge(record.status)}</td>
                                        <td className="px-4 py-3 max-w-xs truncate text-slate-600" title={record.reason}>
                                            {record.reason}
                                        </td>
                                        <td className="px-4 py-3 text-slate-600">{record.applicantName}</td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {new Date(record.startDate).toLocaleDateString('zh-CN')}
                                        </td>
                                        <td className="px-4 py-3 text-slate-600">
                                            {record.endDate ? new Date(record.endDate).toLocaleDateString('zh-CN') : '永久'}
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => navigate(`${ROUTE_PATHS.FREEZE_HOLD_DETAIL}/${record.id}`)}
                                                    className="px-3 py-1 text-slate-600 hover:bg-slate-100 rounded text-sm flex items-center gap-1"
                                                >
                                                    <Eye size={14} />
                                                    详情
                                                </button>
                                                {record.status === 'ACTIVE' && (
                                                    <button
                                                        onClick={() => {
                                                            setReleaseRecord(record);
                                                            setShowReleaseModal(true);
                                                        }}
                                                        className="px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700 flex items-center gap-1"
                                                    >
                                                        <Unlock size={14} />
                                                        解除
                                                    </button>
                                                )}
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

            {/* 申请模态框 */}
            {showApplyModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">申请冻结/保全</h2>
                            <button
                                onClick={() => setShowApplyModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                ×
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    类型 <span className="text-red-500">*</span>
                                </label>
                                <div className="space-y-2">
                                    <label className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50">
                                        <input
                                            type="radio"
                                            name="type"
                                            checked={applyForm.type === 'FREEZE'}
                                            onChange={() => setApplyForm({ ...applyForm, type: 'FREEZE' })}
                                            className="w-4 h-4 text-primary-600 border-slate-300 focus:ring-primary-500"
                                        />
                                        <div>
                                            <div className="font-medium">冻结</div>
                                            <div className="text-xs text-slate-500">临时阻止档案操作</div>
                                        </div>
                                    </label>
                                    <label className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50">
                                        <input
                                            type="radio"
                                            name="type"
                                            checked={applyForm.type === 'HOLD'}
                                            onChange={() => setApplyForm({ ...applyForm, type: 'HOLD' })}
                                            className="w-4 h-4 text-primary-600 border-slate-300 focus:ring-primary-500"
                                        />
                                        <div>
                                            <div className="font-medium">保全</div>
                                            <div className="text-xs text-slate-500">长期保护档案</div>
                                        </div>
                                    </label>
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    档案编号 <span className="text-red-500">*</span>
                                </label>
                                <textarea
                                    value={selectedArchiveCodes.join('\n')}
                                    onChange={(e) => handleArchiveCodeInput(e.target.value)}
                                    rows={6}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 font-mono text-sm"
                                    placeholder="请输入档案编号，每行一个或多个，用逗号、空格分隔"
                                />
                                <p className="text-xs text-slate-500 mt-1">
                                    已选择 {selectedArchiveCodes.length} 个档案
                                </p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    申请原因 <span className="text-red-500">*</span>
                                </label>
                                <textarea
                                    value={applyForm.reason}
                                    onChange={(e) => setApplyForm({ ...applyForm, reason: e.target.value })}
                                    rows={4}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                    placeholder="请详细说明申请冻结/保全的原因..."
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    到期日期（可选，留空则永久）
                                </label>
                                <input
                                    type="date"
                                    value={applyForm.endDate}
                                    onChange={(e) => setApplyForm({ ...applyForm, endDate: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-2">
                                <AlertCircle className="text-blue-600 flex-shrink-0 mt-0.5" size={16} />
                                <p className="text-xs text-blue-800">
                                    冻结/保全后，相关档案将被禁止修改和删除操作，直到解除冻结/保全。
                                </p>
                            </div>
                            <div className="flex gap-3 pt-4 border-t">
                                <button
                                    onClick={handleApply}
                                    disabled={submitting}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {submitting ? (
                                        <Loader2 className="animate-spin" size={16} />
                                    ) : (
                                        <CheckCircle2 size={16} />
                                    )}
                                    提交申请
                                </button>
                                <button
                                    onClick={() => setShowApplyModal(false)}
                                    className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
                                >
                                    取消
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* 解除模态框 */}
            {showReleaseModal && releaseRecord && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-xl w-full mx-4">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">解除冻结/保全</h2>
                            <button
                                onClick={() => setShowReleaseModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                ×
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div className="p-4 bg-slate-50 rounded-lg">
                                <div className="text-sm text-slate-600 mb-1">档案编号</div>
                                <div className="font-mono font-medium">{releaseRecord.archiveCode}</div>
                                <div className="text-sm text-slate-600 mt-2 mb-1">档案标题</div>
                                <div className="font-medium">{releaseRecord.archiveTitle}</div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    解除原因 <span className="text-red-500">*</span>
                                </label>
                                <textarea
                                    value={releaseReason}
                                    onChange={(e) => setReleaseReason(e.target.value)}
                                    rows={4}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                    placeholder="请说明解除冻结/保全的原因..."
                                />
                            </div>
                            <div className="flex gap-3 pt-4 border-t">
                                <button
                                    onClick={handleRelease}
                                    disabled={submitting}
                                    className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {submitting ? (
                                        <Loader2 className="animate-spin" size={16} />
                                    ) : (
                                        <Unlock size={16} />
                                    )}
                                    确认解除
                                </button>
                                <button
                                    onClick={() => setShowReleaseModal(false)}
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

export default FreezeHoldPage;

