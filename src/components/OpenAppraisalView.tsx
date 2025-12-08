import React, { useState, useEffect } from 'react';
import { openAppraisalApi, OpenAppraisal } from '../api/openAppraisal';
import { Shield, ShieldCheck, ShieldAlert, Clock, Calendar, FileText, AlertCircle, CheckCircle2, RefreshCw } from 'lucide-react';

export const OpenAppraisalView: React.FC = () => {
    const [appraisals, setAppraisals] = useState<OpenAppraisal[]>([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState<string>('PENDING');
    const [selectedAppraisal, setSelectedAppraisal] = useState<OpenAppraisal | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({
        appraisalResult: 'OPEN' as 'OPEN' | 'CONTROLLED' | 'EXTENDED',
        openLevel: 'PUBLIC' as 'PUBLIC' | 'INTERNAL' | 'RESTRICTED',
        reason: ''
    });
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        loadAppraisals();
    }, [statusFilter]);

    const loadAppraisals = async () => {
        try {
            setLoading(true);
            const response = await openAppraisalApi.getAppraisalList(1, 50, statusFilter);
            setAppraisals(response.data.data.records || []);
        } catch (error) {
            console.error('Failed to load appraisals:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async () => {
        if (!selectedAppraisal) return;

        if (!formData.reason.trim()) {
            alert('请填写鉴定理由');
            return;
        }

        try {
            setProcessing(true);
            await openAppraisalApi.submitAppraisal({
                id: selectedAppraisal.id,
                appraiserId: 'admin',
                appraiserName: '管理员',
                appraisalResult: formData.appraisalResult,
                openLevel: formData.appraisalResult === 'OPEN' ? formData.openLevel : undefined,
                reason: formData.reason
            });
            setShowModal(false);
            setFormData({ appraisalResult: 'OPEN', openLevel: 'PUBLIC', reason: '' });
            loadAppraisals();
        } catch (error) {
            console.error('Failed to submit appraisal:', error);
            alert('提交失败，请重试');
        } finally {
            setProcessing(false);
        }
    };

    const getStatusBadge = (status: string) => {
        const styles = {
            PENDING: 'bg-amber-100 text-amber-700 border-amber-200',
            COMPLETED: 'bg-emerald-100 text-emerald-700 border-emerald-200'
        };
        const labels = {
            PENDING: '待鉴定',
            COMPLETED: '已完成'
        };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-bold border ${styles[status as keyof typeof styles]}`}>
                {labels[status as keyof typeof labels]}
            </span>
        );
    };

    const getResultBadge = (result?: string) => {
        if (!result) return null;
        const styles = {
            OPEN: 'bg-emerald-100 text-emerald-700 border-emerald-200',
            CONTROLLED: 'bg-rose-100 text-rose-700 border-rose-200',
            EXTENDED: 'bg-blue-100 text-blue-700 border-blue-200'
        };
        const labels = {
            OPEN: '开放',
            CONTROLLED: '控制',
            EXTENDED: '延期'
        };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-bold border ${styles[result as keyof typeof styles]}`}>
                {labels[result as keyof typeof labels]}
            </span>
        );
    };

    return (
        <div className="h-full flex flex-col bg-slate-50">
            {/* Header */}
            <div className="bg-gradient-to-r from-teal-600 to-cyan-600 text-white p-8 shrink-0">
                <div className="max-w-[1600px] mx-auto">
                    <h2 className="text-2xl font-bold flex items-center gap-3">
                        <Shield className="text-white" /> 开放鉴定管理
                    </h2>
                    <p className="text-teal-100 mt-2">
                        对达到保管期限的档案进行开放性鉴定，决定是否可以对外开放查阅
                    </p>

                    {/* Stats */}
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6">
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-teal-100 text-xs font-bold uppercase">待鉴定</span>
                                <Clock size={16} className="text-amber-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {appraisals.filter(a => a.status === 'PENDING').length}
                                <span className="text-xs font-normal text-teal-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-teal-100 text-xs font-bold uppercase">已开放</span>
                                <ShieldCheck size={16} className="text-emerald-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {appraisals.filter(a => a.appraisalResult === 'OPEN').length}
                                <span className="text-xs font-normal text-teal-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-teal-100 text-xs font-bold uppercase">控制访问</span>
                                <ShieldAlert size={16} className="text-rose-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {appraisals.filter(a => a.appraisalResult === 'CONTROLLED').length}
                                <span className="text-xs font-normal text-teal-200 ml-1">件</span>
                            </div>
                        </div>
                        <div className="bg-white/10 backdrop-blur-sm rounded-lg p-4 border border-white/20">
                            <div className="flex justify-between items-start">
                                <span className="text-teal-100 text-xs font-bold uppercase">延长保管</span>
                                <RefreshCw size={16} className="text-blue-300" />
                            </div>
                            <div className="text-3xl font-bold mt-2">
                                {appraisals.filter(a => a.appraisalResult === 'EXTENDED').length}
                                <span className="text-xs font-normal text-teal-200 ml-1">件</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-hidden max-w-[1600px] mx-auto w-full px-8 pb-8 pt-6 flex flex-col">
                {/* Filter Tabs */}
                <div className="flex gap-2 mb-4">
                    {['PENDING', 'COMPLETED'].map(status => (
                        <button
                            key={status}
                            onClick={() => setStatusFilter(status)}
                            className={`px-4 py-2 rounded-lg font-medium text-sm transition-all ${statusFilter === status
                                    ? 'bg-teal-600 text-white shadow-md'
                                    : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
                                }`}
                        >
                            {status === 'PENDING' && '待鉴定'}
                            {status === 'COMPLETED' && '已完成'}
                        </button>
                    ))}
                </div>

                {/* Table */}
                <div className="flex-1 bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden flex flex-col">
                    {loading ? (
                        <div className="flex-1 flex items-center justify-center">
                            <div className="text-slate-400">加载中...</div>
                        </div>
                    ) : appraisals.length === 0 ? (
                        <div className="flex-1 flex items-center justify-center">
                            <div className="text-center">
                                <AlertCircle size={48} className="text-slate-300 mx-auto mb-3" />
                                <p className="text-slate-400">暂无鉴定任务</p>
                            </div>
                        </div>
                    ) : (
                        <div className="flex-1 overflow-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-slate-600 font-medium border-b border-slate-200 sticky top-0">
                                    <tr>
                                        <th className="p-4">档号</th>
                                        <th className="p-4">档案题名</th>
                                        <th className="p-4">保管期限</th>
                                        <th className="p-4">当前密级</th>
                                        <th className="p-4">鉴定结果</th>
                                        <th className="p-4">状态</th>
                                        <th className="p-4">操作</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {appraisals.map(appraisal => (
                                        <tr key={appraisal.id} className="hover:bg-slate-50">
                                            <td className="p-4 font-mono text-slate-600">{appraisal.archiveCode || '-'}</td>
                                            <td className="p-4 font-medium text-slate-800">{appraisal.archiveTitle || '-'}</td>
                                            <td className="p-4 text-slate-600">{appraisal.retentionPeriod || '-'}</td>
                                            <td className="p-4">
                                                <span className="px-2 py-1 bg-slate-100 text-slate-700 rounded text-xs font-medium">
                                                    {appraisal.currentSecurityLevel || '-'}
                                                </span>
                                            </td>
                                            <td className="p-4">{getResultBadge(appraisal.appraisalResult)}</td>
                                            <td className="p-4">{getStatusBadge(appraisal.status)}</td>
                                            <td className="p-4">
                                                <button
                                                    onClick={() => {
                                                        setSelectedAppraisal(appraisal);
                                                        setShowModal(true);
                                                    }}
                                                    className="text-teal-600 hover:text-teal-700 font-medium text-sm"
                                                >
                                                    {appraisal.status === 'PENDING' ? '开始鉴定' : '查看详情'}
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
            {showModal && selectedAppraisal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-auto">
                        <div className="p-6 border-b border-slate-200">
                            <h3 className="text-xl font-bold text-slate-800">开放鉴定</h3>
                        </div>

                        <div className="p-6 space-y-6">
                            {/* Archive Info */}
                            <div className="bg-slate-50 rounded-lg p-4 space-y-3">
                                <h4 className="font-bold text-slate-700 flex items-center gap-2">
                                    <FileText size={16} /> 档案信息
                                </h4>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-xs text-slate-500 font-medium">档号</label>
                                        <p className="font-mono text-slate-800 mt-1">{selectedAppraisal.archiveCode || '-'}</p>
                                    </div>
                                    <div>
                                        <label className="text-xs text-slate-500 font-medium">保管期限</label>
                                        <p className="text-slate-800 mt-1">{selectedAppraisal.retentionPeriod || '-'}</p>
                                    </div>
                                </div>
                                <div>
                                    <label className="text-xs text-slate-500 font-medium">档案题名</label>
                                    <p className="text-slate-800 mt-1">{selectedAppraisal.archiveTitle || '-'}</p>
                                </div>
                                <div>
                                    <label className="text-xs text-slate-500 font-medium">当前密级</label>
                                    <p className="text-slate-800 mt-1">
                                        <span className="px-2 py-1 bg-white border border-slate-200 rounded text-sm">
                                            {selectedAppraisal.currentSecurityLevel || '-'}
                                        </span>
                                    </p>
                                </div>
                            </div>

                            {selectedAppraisal.status === 'PENDING' ? (
                                <>
                                    {/* Appraisal Form */}
                                    <div className="space-y-4">
                                        <div>
                                            <label className="text-sm font-medium text-slate-700 mb-2 block">鉴定结果</label>
                                            <div className="grid grid-cols-3 gap-3">
                                                {[
                                                    { value: 'OPEN', label: '开放', icon: ShieldCheck, color: 'emerald' },
                                                    { value: 'CONTROLLED', label: '控制', icon: ShieldAlert, color: 'rose' },
                                                    { value: 'EXTENDED', label: '延期', icon: RefreshCw, color: 'blue' }
                                                ].map(option => (
                                                    <button
                                                        key={option.value}
                                                        onClick={() => setFormData({ ...formData, appraisalResult: option.value as any })}
                                                        className={`p-4 rounded-lg border-2 transition-all ${formData.appraisalResult === option.value
                                                                ? `border-${option.color}-500 bg-${option.color}-50`
                                                                : 'border-slate-200 hover:border-slate-300'
                                                            }`}
                                                    >
                                                        <option.icon size={24} className={`mx-auto mb-2 ${formData.appraisalResult === option.value ? `text-${option.color}-600` : 'text-slate-400'
                                                            }`} />
                                                        <div className="text-sm font-medium text-slate-800">{option.label}</div>
                                                    </button>
                                                ))}
                                            </div>
                                        </div>

                                        {formData.appraisalResult === 'OPEN' && (
                                            <div>
                                                <label className="text-sm font-medium text-slate-700 mb-2 block">开放等级</label>
                                                <select
                                                    value={formData.openLevel}
                                                    onChange={(e) => setFormData({ ...formData, openLevel: e.target.value as any })}
                                                    className="w-full border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                                                >
                                                    <option value="PUBLIC">公开 (PUBLIC)</option>
                                                    <option value="INTERNAL">内部 (INTERNAL)</option>
                                                    <option value="RESTRICTED">限制 (RESTRICTED)</option>
                                                </select>
                                            </div>
                                        )}

                                        <div>
                                            <label className="text-sm font-medium text-slate-700 mb-2 block">鉴定理由 *</label>
                                            <textarea
                                                value={formData.reason}
                                                onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                                                placeholder="请详细说明鉴定理由..."
                                                className="w-full border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                                                rows={4}
                                            />
                                        </div>
                                    </div>
                                </>
                            ) : (
                                <>
                                    {/* Completed Appraisal Info */}
                                    <div className="space-y-4">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium">鉴定结果</label>
                                                <div className="mt-1">{getResultBadge(selectedAppraisal.appraisalResult)}</div>
                                            </div>
                                            {selectedAppraisal.openLevel && (
                                                <div>
                                                    <label className="text-xs text-slate-500 font-medium">开放等级</label>
                                                    <p className="text-slate-800 mt-1">{selectedAppraisal.openLevel}</p>
                                                </div>
                                            )}
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium">鉴定人</label>
                                                <p className="text-slate-800 mt-1">{selectedAppraisal.appraiserName || '-'}</p>
                                            </div>
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium flex items-center gap-1">
                                                    <Calendar size={12} /> 鉴定日期
                                                </label>
                                                <p className="text-slate-800 mt-1 font-mono text-xs">
                                                    {selectedAppraisal.appraisalDate || '-'}
                                                </p>
                                            </div>
                                        </div>
                                        {selectedAppraisal.reason && (
                                            <div>
                                                <label className="text-xs text-slate-500 font-medium">鉴定理由</label>
                                                <p className="text-slate-800 mt-1 bg-slate-50 p-3 rounded-lg">{selectedAppraisal.reason}</p>
                                            </div>
                                        )}
                                    </div>
                                </>
                            )}
                        </div>

                        <div className="p-6 border-t border-slate-200 flex justify-end gap-3">
                            <button
                                onClick={() => {
                                    setShowModal(false);
                                    setFormData({ appraisalResult: 'OPEN', openLevel: 'PUBLIC', reason: '' });
                                }}
                                className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 font-medium"
                                disabled={processing}
                            >
                                关闭
                            </button>
                            {selectedAppraisal.status === 'PENDING' && (
                                <button
                                    onClick={handleSubmit}
                                    className="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 font-medium flex items-center gap-2 disabled:opacity-50"
                                    disabled={processing}
                                >
                                    <CheckCircle2 size={16} /> 提交鉴定
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OpenAppraisalView;
