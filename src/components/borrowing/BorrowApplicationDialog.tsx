// Input: React、lucide-react、borrowingApi
// Output: BorrowApplicationDialog 组件
// Pos: src/components/borrowing
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { XCircle, Loader2 } from 'lucide-react';
import { borrowingApi, BORROW_PURPOSE_OPTIONS, URGENCY_LEVEL_OPTIONS } from '../../api/borrowing';

export interface BorrowApplicationDialogProps {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void;
    defaultArchiveId?: string;
}

const DEFAULT_EXPIRE_DAYS = 30;
const MIN_EXPIRE_DAYS = 7;
const MAX_EXPIRE_DAYS = 90;

/**
 * 借阅申请弹窗组件
 *
 * 功能：
 * - 支持输入档案ID
 * - 借阅原因必填，长度≥10字符
 * - 预计归还日期必填（默认30天，范围7-90天）
 * - 借阅用途必填
 * - 联系方式必填
 */
export const BorrowApplicationDialog: React.FC<BorrowApplicationDialogProps> = ({
    open,
    onClose,
    onSuccess,
    defaultArchiveId = ''
}) => {
    const [form, setForm] = useState({
        archiveId: defaultArchiveId,
        reason: '',
        expectedReturnDate: '',
        borrowPurpose: '',
        urgencyLevel: 'NORMAL',
        contactInfo: ''
    });
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // 计算默认归还日期（30天后）
    const defaultReturnDate = React.useMemo(() => {
        const date = new Date();
        date.setDate(date.getDate() + DEFAULT_EXPIRE_DAYS);
        return date.toISOString().split('T')[0];
    }, []);

    // 计算最小和最大归还日期
    const minReturnDate = React.useMemo(() => {
        const date = new Date();
        date.setDate(date.getDate() + MIN_EXPIRE_DAYS);
        return date.toISOString().split('T')[0];
    }, []);

    const maxReturnDate = React.useMemo(() => {
        const date = new Date();
        date.setDate(date.getDate() + MAX_EXPIRE_DAYS);
        return date.toISOString().split('T')[0];
    }, []);

    const handleSubmit = async () => {
        setError(null);

        // 校验
        if (!form.archiveId.trim()) {
            setError('请输入档案ID');
            return;
        }
        if (form.reason.trim().length < 10) {
            setError('借阅原因至少需要10个字符');
            return;
        }
        if (!form.expectedReturnDate) {
            setError('请选择预计归还日期');
            return;
        }
        if (!form.borrowPurpose) {
            setError('请选择借阅用途');
            return;
        }
        if (!form.contactInfo.trim()) {
            setError('请填写联系方式');
            return;
        }

        // 验证归还日期范围
        const selectedDate = new Date(form.expectedReturnDate);
        const minDate = new Date(minReturnDate);
        const maxDate = new Date(maxReturnDate);
        if (selectedDate < minDate || selectedDate > maxDate) {
            setError(`预计归还日期必须在${MIN_EXPIRE_DAYS}-${MAX_EXPIRE_DAYS}天之间`);
            return;
        }

        setSubmitting(true);
        try {
            const res = await borrowingApi.createBorrowing({
                archiveId: form.archiveId.trim(),
                reason: form.reason.trim(),
                expectedReturnDate: form.expectedReturnDate,
                borrowPurpose: form.borrowPurpose,
                urgencyLevel: form.urgencyLevel,
                contactInfo: form.contactInfo.trim()
            });

            if (res.code === 200) {
                // 重置表单
                setForm({
                    archiveId: '',
                    reason: '',
                    expectedReturnDate: '',
                    borrowPurpose: '',
                    urgencyLevel: 'NORMAL',
                    contactInfo: ''
                });
                onSuccess();
            } else {
                setError(res.message || '提交失败');
            }
        } catch (err) {
            console.error('Create borrowing failed', err);
            setError('提交失败，请稍后重试');
        } finally {
            setSubmitting(false);
        }
    };

    const handleClose = () => {
        if (!submitting) {
            setError(null);
            setForm({
                archiveId: '',
                reason: '',
                expectedReturnDate: '',
                borrowPurpose: '',
                urgencyLevel: 'NORMAL',
                contactInfo: ''
            });
            onClose();
        }
    };

    if (!open) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center z-40">
            <div className="bg-white rounded-xl shadow-xl border border-slate-200 w-full max-w-lg p-6 m-4">
                <div className="flex justify-between items-start mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800">发起借阅申请</h3>
                        <p className="text-sm text-slate-500">填写档案信息和借阅原因</p>
                    </div>
                    <button
                        onClick={handleClose}
                        disabled={submitting}
                        className="text-slate-400 hover:text-slate-600 disabled:opacity-50"
                    >
                        <XCircle size={18} />
                    </button>
                </div>

                {error && (
                    <div className="bg-rose-50 border border-rose-200 text-rose-700 rounded-lg p-3 mb-4 text-sm">
                        {error}
                    </div>
                )}

                <div className="space-y-4">
                    {/* 档案ID */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            档案ID <span className="text-rose-500">*</span>
                        </label>
                        <input
                            value={form.archiveId}
                            onChange={(e) => setForm((prev) => ({ ...prev, archiveId: e.target.value }))}
                            placeholder="请输入档案ID"
                            disabled={submitting}
                            className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50"
                        />
                    </div>

                    {/* 借阅原因 */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            借阅原因 <span className="text-rose-500">*</span>
                        </label>
                        <textarea
                            value={form.reason}
                            onChange={(e) => setForm((prev) => ({ ...prev, reason: e.target.value }))}
                            rows={3}
                            placeholder="请详细说明借阅原因（至少10个字符）"
                            disabled={submitting}
                            className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50 resize-none"
                        />
                        <div className="text-xs text-slate-400 mt-1">
                            {form.reason.length}/10 字符
                        </div>
                    </div>

                    {/* 借阅用途 */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            借阅用途 <span className="text-rose-500">*</span>
                        </label>
                        <select
                            value={form.borrowPurpose}
                            onChange={(e) => setForm((prev) => ({ ...prev, borrowPurpose: e.target.value }))}
                            disabled={submitting}
                            className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50"
                        >
                            <option value="">请选择借阅用途</option>
                            {BORROW_PURPOSE_OPTIONS.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 紧急程度 */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            紧急程度
                        </label>
                        <div className="flex gap-4">
                            {URGENCY_LEVEL_OPTIONS.map((option) => (
                                <label key={option.value} className="flex items-center gap-2 cursor-pointer">
                                    <input
                                        type="radio"
                                        name="urgencyLevel"
                                        value={option.value}
                                        checked={form.urgencyLevel === option.value}
                                        onChange={(e) => setForm((prev) => ({ ...prev, urgencyLevel: e.target.value }))}
                                        disabled={submitting}
                                        className="w-4 h-4 text-blue-600"
                                    />
                                    <span className="text-sm text-slate-700">{option.label}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* 预计归还日期 */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            预计归还日期 <span className="text-rose-500">*</span>
                        </label>
                        <input
                            type="date"
                            value={form.expectedReturnDate}
                            onChange={(e) =>
                                setForm((prev) => ({ ...prev, expectedReturnDate: e.target.value }))
                            }
                            min={minReturnDate}
                            max={maxReturnDate}
                            disabled={submitting}
                            className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50"
                        />
                        <div className="text-xs text-slate-400 mt-1">
                            默认 {DEFAULT_EXPIRE_DAYS} 天后，可选范围 {MIN_EXPIRE_DAYS}-{MAX_EXPIRE_DAYS} 天
                        </div>
                    </div>

                    {/* 联系方式 */}
                    <div>
                        <label className="block text-sm text-slate-600 mb-1">
                            联系方式 <span className="text-rose-500">*</span>
                        </label>
                        <input
                            value={form.contactInfo}
                            onChange={(e) => setForm((prev) => ({ ...prev, contactInfo: e.target.value }))}
                            placeholder="请输入手机号或邮箱"
                            disabled={submitting}
                            className="w-full border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-50"
                        />
                    </div>
                </div>

                <div className="flex justify-end gap-2 mt-6">
                    <button
                        onClick={handleClose}
                        disabled={submitting}
                        className="px-4 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-50"
                    >
                        取消
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={submitting}
                        className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
                    >
                        {submitting ? (
                            <>
                                <Loader2 size={16} className="animate-spin" />
                                提交中...
                            </>
                        ) : (
                            '提交申请'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default BorrowApplicationDialog;
