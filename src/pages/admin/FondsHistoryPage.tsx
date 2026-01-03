// Input: React、lucide-react、fondsHistoryApi、fondsApi
// Output: FondsHistoryPage 组件
// Pos: 全宗沿革管理页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { ArrowRight, Merge, Split, Edit3, History, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { fondsHistoryApi, MigrateFondsRequest, MergeFondsRequest, SplitFondsRequest, RenameFondsRequest } from '../../api/fondsHistory';
import { fondsApi, BasFonds } from '../../api/fonds';

/**
 * 全宗沿革管理页面
 * 
 * 功能：
 * 1. 全宗迁移
 * 2. 全宗合并
 * 3. 全宗分立
 * 4. 全宗重命名
 * 
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
export const FondsHistoryPage: React.FC = () => {
    const [activeTab, setActiveTab] = useState<'migrate' | 'merge' | 'split' | 'rename'>('migrate');
    const [fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    // 迁移表单
    const [migrateForm, setMigrateForm] = useState<MigrateFondsRequest>({
        fromFondsNo: '',
        toFondsNo: '',
        effectiveDate: new Date().toISOString().split('T')[0],
        reason: '',
    });

    // 合并表单
    const [mergeForm, setMergeForm] = useState<MergeFondsRequest>({
        sourceFondsNos: [],
        targetFondsNo: '',
        effectiveDate: new Date().toISOString().split('T')[0],
        reason: '',
    });

    // 分立表单
    const [splitForm, setSplitForm] = useState<SplitFondsRequest>({
        sourceFondsNo: '',
        newFondsNos: [''],
        effectiveDate: new Date().toISOString().split('T')[0],
        reason: '',
    });

    // 重命名表单
    const [renameForm, setRenameForm] = useState<RenameFondsRequest>({
        oldFondsNo: '',
        newFondsNo: '',
        effectiveDate: new Date().toISOString().split('T')[0],
        reason: '',
    });

    React.useEffect(() => {
        loadFondsList();
    }, []);

    const loadFondsList = async () => {
        setLoading(true);
        try {
            const res = await fondsApi.list();
            if (res.code === 200 && res.data) {
                setFondsList(res.data);
            }
        } catch (error) {
            console.error('加载全宗列表失败', error);
            setMessage({ type: 'error', text: '加载全宗列表失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleMigrate = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!migrateForm.fromFondsNo || !migrateForm.toFondsNo || !migrateForm.reason) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSubmitting(true);
        try {
            const res = await fondsHistoryApi.migrate(migrateForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `全宗迁移成功，历史记录ID: ${res.data?.historyId}` });
                setMigrateForm({
                    fromFondsNo: '',
                    toFondsNo: '',
                    effectiveDate: new Date().toISOString().split('T')[0],
                    reason: '',
                });
                loadFondsList();
            } else {
                setMessage({ type: 'error', text: res.message || '全宗迁移失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '全宗迁移失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const handleMerge = async (e: React.FormEvent) => {
        e.preventDefault();
        if (mergeForm.sourceFondsNos.length === 0 || !mergeForm.targetFondsNo || !mergeForm.reason) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSubmitting(true);
        try {
            const res = await fondsHistoryApi.merge(mergeForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `全宗合并成功，历史记录ID: ${res.data?.historyIds?.join(', ')}` });
                setMergeForm({
                    sourceFondsNos: [],
                    targetFondsNo: '',
                    effectiveDate: new Date().toISOString().split('T')[0],
                    reason: '',
                });
                loadFondsList();
            } else {
                setMessage({ type: 'error', text: res.message || '全宗合并失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '全宗合并失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const handleSplit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!splitForm.sourceFondsNo || splitForm.newFondsNos.length === 0 || !splitForm.reason) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSubmitting(true);
        try {
            const res = await fondsHistoryApi.split(splitForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `全宗分立成功，历史记录ID: ${res.data?.historyIds?.join(', ')}` });
                setSplitForm({
                    sourceFondsNo: '',
                    newFondsNos: [''],
                    effectiveDate: new Date().toISOString().split('T')[0],
                    reason: '',
                });
                loadFondsList();
            } else {
                setMessage({ type: 'error', text: res.message || '全宗分立失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '全宗分立失败' });
        } finally {
            setSubmitting(false);
        }
    };

    const handleRename = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!renameForm.oldFondsNo || !renameForm.newFondsNo || !renameForm.reason) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSubmitting(true);
        try {
            const res = await fondsHistoryApi.rename(renameForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `全宗重命名成功，历史记录ID: ${res.data?.historyId}` });
                setRenameForm({
                    oldFondsNo: '',
                    newFondsNo: '',
                    effectiveDate: new Date().toISOString().split('T')[0],
                    reason: '',
                });
                loadFondsList();
            } else {
                setMessage({ type: 'error', text: res.message || '全宗重命名失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '全宗重命名失败' });
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center gap-3">
                        <History className="w-6 h-6 text-primary-600" />
                        <h1 className="text-xl font-semibold text-slate-900">全宗沿革管理</h1>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">管理全宗的迁移、合并、分立和重命名操作</p>
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

                {/* Tabs */}
                <div className="px-6 pt-4 border-b border-slate-200">
                    <div className="flex gap-2">
                        <button
                            onClick={() => setActiveTab('migrate')}
                            className={`px-4 py-2 rounded-t-lg text-sm font-medium transition-colors ${
                                activeTab === 'migrate'
                                    ? 'bg-primary-50 text-primary-700 border-b-2 border-primary-600'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <ArrowRight className="w-4 h-4 inline mr-1" />
                            全宗迁移
                        </button>
                        <button
                            onClick={() => setActiveTab('merge')}
                            className={`px-4 py-2 rounded-t-lg text-sm font-medium transition-colors ${
                                activeTab === 'merge'
                                    ? 'bg-primary-50 text-primary-700 border-b-2 border-primary-600'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Merge className="w-4 h-4 inline mr-1" />
                            全宗合并
                        </button>
                        <button
                            onClick={() => setActiveTab('split')}
                            className={`px-4 py-2 rounded-t-lg text-sm font-medium transition-colors ${
                                activeTab === 'split'
                                    ? 'bg-primary-50 text-primary-700 border-b-2 border-primary-600'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Split className="w-4 h-4 inline mr-1" />
                            全宗分立
                        </button>
                        <button
                            onClick={() => setActiveTab('rename')}
                            className={`px-4 py-2 rounded-t-lg text-sm font-medium transition-colors ${
                                activeTab === 'rename'
                                    ? 'bg-primary-50 text-primary-700 border-b-2 border-primary-600'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Edit3 className="w-4 h-4 inline mr-1" />
                            全宗重命名
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : (
                        <>
                            {/* 全宗迁移 */}
                            {activeTab === 'migrate' && (
                                <form onSubmit={handleMigrate} className="max-w-2xl space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            源全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            value={migrateForm.fromFondsNo}
                                            onChange={(e) => setMigrateForm({ ...migrateForm, fromFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        >
                                            <option value="">请选择源全宗</option>
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            目标全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            value={migrateForm.toFondsNo}
                                            onChange={(e) => setMigrateForm({ ...migrateForm, toFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        >
                                            <option value="">请选择目标全宗</option>
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            生效日期 <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            value={migrateForm.effectiveDate}
                                            onChange={(e) => setMigrateForm({ ...migrateForm, effectiveDate: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            迁移原因 <span className="text-red-500">*</span>
                                        </label>
                                        <textarea
                                            value={migrateForm.reason}
                                            onChange={(e) => setMigrateForm({ ...migrateForm, reason: e.target.value })}
                                            rows={4}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            placeholder="请说明迁移原因..."
                                            required
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        disabled={submitting}
                                        className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {submitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                                        执行迁移
                                    </button>
                                </form>
                            )}

                            {/* 全宗合并 */}
                            {activeTab === 'merge' && (
                                <form onSubmit={handleMerge} className="max-w-2xl space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            源全宗号（可多选）<span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            multiple
                                            value={mergeForm.sourceFondsNos}
                                            onChange={(e) => {
                                                const selected = Array.from(e.target.selectedOptions, option => option.value);
                                                setMergeForm({ ...mergeForm, sourceFondsNos: selected });
                                            }}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            size={5}
                                            required
                                        >
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                        <p className="text-xs text-slate-500 mt-1">按住 Ctrl/Cmd 键可多选</p>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            目标全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            value={mergeForm.targetFondsNo}
                                            onChange={(e) => setMergeForm({ ...mergeForm, targetFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        >
                                            <option value="">请选择目标全宗</option>
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            生效日期 <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            value={mergeForm.effectiveDate}
                                            onChange={(e) => setMergeForm({ ...mergeForm, effectiveDate: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            合并原因 <span className="text-red-500">*</span>
                                        </label>
                                        <textarea
                                            value={mergeForm.reason}
                                            onChange={(e) => setMergeForm({ ...mergeForm, reason: e.target.value })}
                                            rows={4}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            placeholder="请说明合并原因..."
                                            required
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        disabled={submitting}
                                        className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {submitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                                        执行合并
                                    </button>
                                </form>
                            )}

                            {/* 全宗分立 */}
                            {activeTab === 'split' && (
                                <form onSubmit={handleSplit} className="max-w-2xl space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            源全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            value={splitForm.sourceFondsNo}
                                            onChange={(e) => setSplitForm({ ...splitForm, sourceFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        >
                                            <option value="">请选择源全宗</option>
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            新全宗号列表 <span className="text-red-500">*</span>
                                        </label>
                                        {splitForm.newFondsNos.map((fondsNo, index) => (
                                            <div key={index} className="flex gap-2 mb-2">
                                                <input
                                                    type="text"
                                                    value={fondsNo}
                                                    onChange={(e) => {
                                                        const newFondsNos = [...splitForm.newFondsNos];
                                                        newFondsNos[index] = e.target.value;
                                                        setSplitForm({ ...splitForm, newFondsNos });
                                                    }}
                                                    placeholder={`新全宗号 ${index + 1}`}
                                                    className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                                    required
                                                />
                                                {splitForm.newFondsNos.length > 1 && (
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            const newFondsNos = splitForm.newFondsNos.filter((_, i) => i !== index);
                                                            setSplitForm({ ...splitForm, newFondsNos });
                                                        }}
                                                        className="px-3 py-2 text-red-600 hover:bg-red-50 rounded-lg"
                                                    >
                                                        删除
                                                    </button>
                                                )}
                                            </div>
                                        ))}
                                        <button
                                            type="button"
                                            onClick={() => setSplitForm({ ...splitForm, newFondsNos: [...splitForm.newFondsNos, ''] })}
                                            className="text-sm text-primary-600 hover:text-primary-700"
                                        >
                                            + 添加新全宗号
                                        </button>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            生效日期 <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            value={splitForm.effectiveDate}
                                            onChange={(e) => setSplitForm({ ...splitForm, effectiveDate: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            分立原因 <span className="text-red-500">*</span>
                                        </label>
                                        <textarea
                                            value={splitForm.reason}
                                            onChange={(e) => setSplitForm({ ...splitForm, reason: e.target.value })}
                                            rows={4}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            placeholder="请说明分立原因..."
                                            required
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        disabled={submitting}
                                        className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {submitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                                        执行分立
                                    </button>
                                </form>
                            )}

                            {/* 全宗重命名 */}
                            {activeTab === 'rename' && (
                                <form onSubmit={handleRename} className="max-w-2xl space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            旧全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <select
                                            value={renameForm.oldFondsNo}
                                            onChange={(e) => setRenameForm({ ...renameForm, oldFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        >
                                            <option value="">请选择全宗</option>
                                            {fondsList.map((f) => (
                                                <option key={f.id} value={f.fondsCode}>
                                                    {f.fondsName} ({f.fondsCode})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            新全宗号 <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="text"
                                            value={renameForm.newFondsNo}
                                            onChange={(e) => setRenameForm({ ...renameForm, newFondsNo: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            placeholder="请输入新全宗号"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            生效日期 <span className="text-red-500">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            value={renameForm.effectiveDate}
                                            onChange={(e) => setRenameForm({ ...renameForm, effectiveDate: e.target.value })}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700 mb-1">
                                            重命名原因 <span className="text-red-500">*</span>
                                        </label>
                                        <textarea
                                            value={renameForm.reason}
                                            onChange={(e) => setRenameForm({ ...renameForm, reason: e.target.value })}
                                            rows={4}
                                            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                            placeholder="请说明重命名原因..."
                                            required
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        disabled={submitting}
                                        className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {submitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                                        执行重命名
                                    </button>
                                </form>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default FondsHistoryPage;


