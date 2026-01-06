// Input: React、lucide-react、authTicketApi、fondsApi
// Output: AuthTicketApplyPage 组件
// Pos: 授权票据申请页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { FileText, Calendar, Shield, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { authTicketApi, CreateAuthTicketRequest, AuthScope } from '../../api/authTicket';
import { fondsApi, BasFonds } from '../../api/fonds';
import { useFondsStore } from '../../store';

/**
 * 授权票据申请页面
 * 
 * 功能：
 * 1. 申请跨全宗访问授权票据
 * 2. 设置访问范围（全宗/期间/类型/关键词）
 * 3. 设置有效期和申请原因
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
export const AuthTicketApplyPage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    const [form, setForm] = useState<CreateAuthTicketRequest>({
        targetFonds: '',
        scope: {
            accessType: 'READ_ONLY',
        },
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16), // 默认7天后
        reason: '',
    });

    useEffect(() => {
        loadFondsList();
    }, []);

    const loadFondsList = async () => {
        setLoading(true);
        try {
            const res = await fondsApi.list();
            if (res.code === 200 && res.data) {
                // 过滤掉当前全宗
                const filtered = res.data.filter(f => f.fondsCode !== currentFonds?.fondsCode);
                setFondsList(filtered);
            }
        } catch (error) {
            console.error('加载全宗列表失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.targetFonds || !form.reason) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSubmitting(true);
        try {
            const res = await authTicketApi.apply(form);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `授权票据申请成功，票据ID: ${res.data?.ticketId}` });
                setForm({
                    targetFonds: '',
                    scope: { accessType: 'READ_ONLY' },
                    expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
                    reason: '',
                });
            } else {
                setMessage({ type: 'error', text: res.message || '申请失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '申请失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const updateScope = (updates: Partial<AuthScope>) => {
        setForm({
            ...form,
            scope: { ...form.scope, ...updates },
        });
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 max-w-3xl mx-auto w-full">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center gap-3">
                        <Shield className="w-6 h-6 text-primary-600" />
                        <h1 className="text-xl font-semibold text-slate-900">申请跨全宗访问授权票据</h1>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">申请访问其他全宗的授权票据，需要审批通过后才能使用</p>
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

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-6 space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            当前全宗 <span className="text-slate-400">(只读)</span>
                        </label>
                        <input
                            type="text"
                            value={currentFonds ? `${currentFonds.fondsName} (${currentFonds.fondsCode})` : '未选择'}
                            disabled
                            className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            目标全宗 <span className="text-red-500">*</span>
                        </label>
                        <select
                            value={form.targetFonds}
                            onChange={(e) => setForm({ ...form, targetFonds: e.target.value })}
                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            required
                            disabled={loading}
                        >
                            <option value="">请选择目标全宗</option>
                            {fondsList.map((f) => (
                                <option key={f.id} value={f.fondsCode}>
                                    {f.fondsName} ({f.fondsCode})
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 访问范围 */}
                    <div className="border border-slate-200 rounded-lg p-4 space-y-4">
                        <h3 className="text-sm font-medium text-slate-700">访问范围（可选，不填表示访问全部）</h3>
                        
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                归档年度（多个用逗号分隔）
                            </label>
                            <input
                                type="text"
                                value={form.scope.archiveYears?.join(',') || ''}
                                onChange={(e) => {
                                    const years = e.target.value
                                        .split(',')
                                        .map(s => parseInt(s.trim()))
                                        .filter(n => !isNaN(n));
                                    updateScope({ archiveYears: years.length > 0 ? years : undefined });
                                }}
                                placeholder="例如: 2023,2024"
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                档案类型（多个用逗号分隔）
                            </label>
                            <input
                                type="text"
                                value={form.scope.docTypes?.join(',') || ''}
                                onChange={(e) => {
                                    const types = e.target.value
                                        .split(',')
                                        .map(s => s.trim())
                                        .filter(s => s.length > 0);
                                    updateScope({ docTypes: types.length > 0 ? types : undefined });
                                }}
                                placeholder="例如: AC01,AC02"
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                关键词（多个用逗号分隔）
                            </label>
                            <input
                                type="text"
                                value={form.scope.keywords?.join(',') || ''}
                                onChange={(e) => {
                                    const keywords = e.target.value
                                        .split(',')
                                        .map(s => s.trim())
                                        .filter(s => s.length > 0);
                                    updateScope({ keywords: keywords.length > 0 ? keywords : undefined });
                                }}
                                placeholder="例如: 发票,合同"
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                访问类型
                            </label>
                            <select
                                value={form.scope.accessType || 'READ_ONLY'}
                                onChange={(e) => updateScope({ accessType: e.target.value as 'READ_ONLY' | 'READ_WRITE' })}
                                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            >
                                <option value="READ_ONLY">只读</option>
                                <option value="READ_WRITE">读写</option>
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            有效期 <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="datetime-local"
                            value={form.expiresAt}
                            onChange={(e) => setForm({ ...form, expiresAt: e.target.value })}
                            min={new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 16)}
                            max={new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16)}
                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            required
                        />
                        <p className="text-xs text-slate-500 mt-1">有效期必须在1-90天之间</p>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            申请原因 <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={form.reason}
                            onChange={(e) => setForm({ ...form, reason: e.target.value })}
                            rows={4}
                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                            placeholder="请详细说明申请跨全宗访问的原因..."
                            required
                        />
                    </div>

                    <div className="flex gap-3">
                        <button
                            type="submit"
                            disabled={submitting}
                            className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        >
                            {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileText className="w-4 h-4" />}
                            提交申请
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default AuthTicketApplyPage;





