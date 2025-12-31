// Input: React、lucide-react、freezeHoldApi
// Output: FreezeHoldDetailPage 组件
// Pos: 冻结/保全详情页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Lock, Loader2, ArrowLeft, Calendar, User, FileText, AlertCircle, Clock, Unlock } from 'lucide-react';
import { freezeHoldApi, FreezeHoldRecord } from '../../api/freezeHold';
import { useParams, useNavigate } from 'react-router-dom';

/**
 * 冻结/保全详情页面
 * 
 * 功能：
 * 1. 冻结/保全原因展示
 * 2. 冻结/保全期限展示
 * 3. 相关审计日志（可选）
 * 
 * PRD 来源: Section 6.2 - 冻结/保全管理
 */
export const FreezeHoldDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [record, setRecord] = useState<FreezeHoldRecord | null>(null);
    const [loading, setLoading] = useState(false);
    const [showReleaseModal, setShowReleaseModal] = useState(false);
    const [releaseReason, setReleaseReason] = useState('');
    const [releasing, setReleasing] = useState(false);

    useEffect(() => {
        if (id) {
            loadDetail();
        }
    }, [id]);

    const loadDetail = async () => {
        if (!id) return;
        setLoading(true);
        try {
            const res = await freezeHoldApi.getDetail(id);
            if (res.code === 200 && res.data) {
                setRecord(res.data);
            }
        } catch (error) {
            console.error('加载详情失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleRelease = async () => {
        if (!record) return;
        if (!releaseReason.trim()) {
            alert('请填写解除原因');
            return;
        }

        setReleasing(true);
        try {
            const res = await freezeHoldApi.release({ id: record.id, reason: releaseReason });
            if (res.code === 200) {
                alert('解除成功');
                navigate(-1);
            } else {
                alert(res.message || '解除失败');
            }
        } catch (error: any) {
            alert(error?.response?.data?.message || '解除失败');
        } finally {
            setReleasing(false);
        }
    };

    const getTypeLabel = (type: string) => {
        return type === 'FREEZE' ? '冻结' : '保全';
    };

    const getStatusBadge = (status: string) => {
        const configs: Record<string, { label: string; color: string }> = {
            'ACTIVE': { label: '生效中', color: 'bg-blue-100 text-blue-700' },
            'RELEASED': { label: '已解除', color: 'bg-green-100 text-green-700' },
            'EXPIRED': { label: '已过期', color: 'bg-slate-100 text-slate-700' },
        };
        const config = configs[status] || { label: status, color: 'bg-slate-100 text-slate-700' };
        return (
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${config.color}`}>
                {config.label}
            </span>
        );
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <Loader2 className="animate-spin text-slate-400" size={32} />
            </div>
        );
    }

    if (!record) {
        return (
            <div className="p-6">
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
                    <AlertCircle className="text-red-600" size={20} />
                    <div>
                        <div className="font-semibold text-red-800">记录不存在</div>
                        <div className="text-sm text-red-600">未找到指定的冻结/保全记录</div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6 space-y-6">
            {/* 头部 */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 hover:bg-slate-100 rounded-lg"
                    >
                        <ArrowLeft size={20} className="text-slate-600" />
                    </button>
                    <div>
                        <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                            <Lock className="mr-2" size={28} />
                            冻结/保全详情
                        </h2>
                        <p className="text-slate-500 text-sm mt-1">查看冻结/保全记录的详细信息</p>
                    </div>
                </div>
                {record.status === 'ACTIVE' && (
                    <button
                        onClick={() => setShowReleaseModal(true)}
                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 flex items-center gap-2"
                    >
                        <Unlock size={18} />
                        解除
                    </button>
                )}
            </div>

            {/* 基本信息卡片 */}
            <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-6">
                <div>
                    <h3 className="text-lg font-semibold mb-4">基本信息</h3>
                    <div className="grid grid-cols-2 gap-6">
                        <div>
                            <div className="text-sm text-slate-500 mb-1">档案编号</div>
                            <div className="font-mono font-medium text-lg">{record.archiveCode}</div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500 mb-1">档案标题</div>
                            <div className="font-medium text-lg">{record.archiveTitle}</div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500 mb-1">类型</div>
                            <div>
                                <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                                    record.type === 'FREEZE' 
                                        ? 'bg-red-100 text-red-700' 
                                        : 'bg-yellow-100 text-yellow-700'
                                }`}>
                                    {getTypeLabel(record.type)}
                                </span>
                            </div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500 mb-1">状态</div>
                            <div>{getStatusBadge(record.status)}</div>
                        </div>
                    </div>
                </div>

                {/* 申请信息 */}
                <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <FileText size={20} />
                        申请信息
                    </h3>
                    <div className="grid grid-cols-2 gap-6">
                        <div>
                            <div className="text-sm text-slate-500 mb-1 flex items-center gap-1">
                                <User size={14} />
                                申请人
                            </div>
                            <div className="font-medium">{record.applicantName}</div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500 mb-1 flex items-center gap-1">
                                <Calendar size={14} />
                                申请时间
                            </div>
                            <div className="font-medium">
                                {new Date(record.createdAt).toLocaleString('zh-CN')}
                            </div>
                        </div>
                    </div>
                    <div className="mt-4">
                        <div className="text-sm text-slate-500 mb-2">申请原因</div>
                        <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                            <p className="text-slate-800 whitespace-pre-wrap">{record.reason}</p>
                        </div>
                    </div>
                </div>

                {/* 期限信息 */}
                <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <Clock size={20} />
                        期限信息
                    </h3>
                    <div className="grid grid-cols-2 gap-6">
                        <div>
                            <div className="text-sm text-slate-500 mb-1">生效日期</div>
                            <div className="font-medium">
                                {new Date(record.startDate).toLocaleDateString('zh-CN')}
                            </div>
                        </div>
                        <div>
                            <div className="text-sm text-slate-500 mb-1">到期日期</div>
                            <div className="font-medium">
                                {record.endDate 
                                    ? new Date(record.endDate).toLocaleDateString('zh-CN')
                                    : <span className="text-slate-400">永久</span>
                                }
                            </div>
                        </div>
                    </div>
                    {record.endDate && record.status === 'ACTIVE' && (
                        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-2">
                            <AlertCircle className="text-blue-600 flex-shrink-0 mt-0.5" size={16} />
                            <div className="text-sm text-blue-800">
                                距离到期还有 <span className="font-semibold">
                                    {Math.ceil((new Date(record.endDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24))}
                                </span> 天
                            </div>
                        </div>
                    )}
                </div>

                {/* 解除信息（如果有） */}
                {record.status === 'RELEASED' && record.releasedAt && (
                    <div className="border-t pt-6">
                        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                            <Unlock size={20} />
                            解除信息
                        </h3>
                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <div className="text-sm text-slate-500 mb-1">解除人</div>
                                <div className="font-medium">{record.releasedBy || '-'}</div>
                            </div>
                            <div>
                                <div className="text-sm text-slate-500 mb-1">解除时间</div>
                                <div className="font-medium">
                                    {new Date(record.releasedAt).toLocaleString('zh-CN')}
                                </div>
                            </div>
                        </div>
                        {record.releaseReason && (
                            <div className="mt-4">
                                <div className="text-sm text-slate-500 mb-2">解除原因</div>
                                <div className="p-4 bg-green-50 rounded-lg border border-green-200">
                                    <p className="text-green-800 whitespace-pre-wrap">{record.releaseReason}</p>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* 解除模态框 */}
            {showReleaseModal && (
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
                                <div className="font-mono font-medium">{record.archiveCode}</div>
                                <div className="text-sm text-slate-600 mt-2 mb-1">档案标题</div>
                                <div className="font-medium">{record.archiveTitle}</div>
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
                                    disabled={releasing}
                                    className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {releasing ? (
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

export default FreezeHoldDetailPage;

