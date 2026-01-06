// Input: React、lucide-react 图标、adminApi
// Output: React 组件 UserFondsScopeDialog
// Pos: 系统设置组件 - 用户全宗权限管理对话框

import React, { useState, useEffect } from 'react';
import { Loader2, ShieldCheck } from 'lucide-react';
import { BaseModal } from '../modals/BaseModal';
import { adminApi, FondsInfo } from '../../api/admin';
import { toast } from '../../utils/notificationService';

interface UserFondsScopeDialogProps {
    isOpen: boolean;
    onClose: () => void;
    userId: string;
    username: string;
    onSuccess?: () => void;
}

export const UserFondsScopeDialog: React.FC<UserFondsScopeDialogProps> = ({
    isOpen,
    onClose,
    userId,
    username,
    onSuccess,
}) => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [assignedFonds, setAssignedFonds] = useState<string[]>([]);
    const [availableFonds, setAvailableFonds] = useState<FondsInfo[]>([]);

    useEffect(() => {
        if (!isOpen || !userId) return;

        const loadData = async () => {
            setLoading(true);
            try {
                const res = await adminApi.getUserFondsScope(userId);
                if (res.code === 200 && res.data) {
                    setAssignedFonds(res.data.assignedFonds || []);
                    setAvailableFonds(res.data.availableFonds || []);
                } else {
                    toast.error(res.message || '加载全宗权限失败');
                }
            } catch (e: any) {
                toast.error(e?.response?.data?.message || '加载全宗权限失败');
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [isOpen, userId]);

    const toggleFonds = (fondsCode: string) => {
        setAssignedFonds((prev) =>
            prev.includes(fondsCode)
                ? prev.filter((c) => c !== fondsCode)
                : [...prev, fondsCode]
        );
    };

    const toggleAll = () => {
        if (assignedFonds.length === availableFonds.length) {
            setAssignedFonds([]);
        } else {
            setAssignedFonds(availableFonds.map((f) => f.fondsCode));
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const res = await adminApi.updateUserFondsScope(userId, assignedFonds);
            if (res.code === 200) {
                toast.success('全宗权限已更新');
                onSuccess?.();
                onClose();
            } else {
                toast.error(res.message || '更新失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '更新失败');
        } finally {
            setSaving(false);
        }
    };

    const isAllSelected = availableFonds.length > 0 && assignedFonds.length === availableFonds.length;
    const isIndeterminate = assignedFonds.length > 0 && assignedFonds.length < availableFonds.length;

    return (
        <BaseModal
            isOpen={isOpen}
            onClose={onClose}
            title="设置全宗权限"
            maxWidth="md"
            footer={
                <>
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                        disabled={saving}
                    >
                        取消
                    </button>
                    <button
                        type="button"
                        onClick={handleSave}
                        disabled={saving || loading}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                        {saving && <Loader2 size={16} className="animate-spin" />}
                        保存
                    </button>
                </>
            }
        >
            {loading ? (
                <div className="flex items-center justify-center py-8">
                    <Loader2 size={24} className="animate-spin text-slate-400" />
                </div>
            ) : (
                <div className="space-y-4">
                    <div className="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
                        <ShieldCheck className="text-blue-600" size={20} />
                        <div>
                            <p className="text-sm text-slate-500 dark:text-slate-400">用户</p>
                            <p className="font-medium text-slate-800 dark:text-white">{username}</p>
                        </div>
                    </div>

                    {availableFonds.length > 0 && (
                        <button
                            type="button"
                            onClick={toggleAll}
                            className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        >
                            <div className={`w-4 h-4 border rounded flex items-center justify-center transition-colors ${
                                isAllSelected
                                    ? 'bg-blue-600 border-blue-600'
                                    : 'border-slate-300 dark:border-slate-600'
                            }`}>
                                {isAllSelected && (
                                    <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                    </svg>
                                )}
                                {isIndeterminate && (
                                    <div className="w-2 h-0.5 bg-white" />
                                )}
                            </div>
                            {isAllSelected ? '取消全选' : '全选'}
                        </button>
                    )}

                    <div className="space-y-2 max-h-64 overflow-y-auto">
                        {availableFonds.length === 0 ? (
                            <p className="text-center text-slate-400 py-4">暂无可用的全宗</p>
                        ) : (
                            availableFonds.map((fonds) => {
                                const isSelected = assignedFonds.includes(fonds.fondsCode);
                                return (
                                    <button
                                        key={fonds.fondsCode}
                                        type="button"
                                        onClick={() => toggleFonds(fonds.fondsCode)}
                                        className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all ${
                                            isSelected
                                                ? 'bg-blue-50 border-blue-200 dark:bg-blue-900/20 dark:border-blue-800'
                                                : 'bg-white border-slate-200 hover:border-slate-300 dark:bg-slate-800 dark:border-slate-700 dark:hover:border-slate-600'
                                        }`}
                                    >
                                        <div className={`w-5 h-5 border rounded flex items-center justify-center transition-colors ${
                                            isSelected
                                                ? 'bg-blue-600 border-blue-600'
                                                : 'border-slate-300 dark:border-slate-600'
                                        }`}>
                                            {isSelected && (
                                                <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                                </svg>
                                            )}
                                        </div>
                                        <div className="flex-1 text-left">
                                            <p className="font-medium text-slate-800 dark:text-white">{fonds.fondsName}</p>
                                            <p className="text-sm text-slate-500 dark:text-slate-400">
                                                {fonds.companyName} · {fonds.fondsCode}
                                            </p>
                                        </div>
                                    </button>
                                );
                            })
                        )}
                    </div>

                    {availableFonds.length > 0 && (
                        <p className="text-sm text-slate-500 dark:text-slate-400 text-center">
                            已选择 {assignedFonds.length} / {availableFonds.length} 个全宗
                        </p>
                    )}
                </div>
            )}
        </BaseModal>
    );
};

export default UserFondsScopeDialog;
