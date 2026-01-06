// Input: React、lucide-react 图标、adminApi
// Output: React 组件 UserFondsScopeDrawer - 右侧抽屉
// Pos: 系统设置组件 - 用户全宗权限管理抽屉

import React, { useState, useEffect } from 'react';
import { Loader2, ShieldCheck } from 'lucide-react';
import { Drawer } from 'antd';
import { adminApi } from '../../api/admin';
import { toast } from '../../utils/notificationService';

interface UserFondsScopeDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    userId: string;
    username: string;
    onSuccess?: () => void;
}

export const UserFondsScopeDrawer: React.FC<UserFondsScopeDrawerProps> = ({
    isOpen,
    onClose,
    userId,
    username,
    onSuccess,
}) => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [assignedFonds, setAssignedFonds] = useState<string[]>([]);
    const [availableFonds, setAvailableFonds] = useState<Array<{fondsCode: string; fondsName: string; companyName: string}>>([]);

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

    return (
        <Drawer
            title={
                <div className="flex items-center gap-2">
                    <ShieldCheck className="text-blue-600" size={20} />
                    <span>设置全宗权限</span>
                </div>
            }
            placement="right"
            open={isOpen}
            onClose={onClose}
            width={420}
            styles={{
                body: { padding: '20px' }
            }}
            footer={
                <div className="flex items-center justify-end gap-3 p-4 border-t">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={saving}
                        className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
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
                </div>
            }
        >
            {loading ? (
                <div className="flex items-center justify-center py-8">
                    <Loader2 size={24} className="animate-spin text-slate-400" />
                </div>
            ) : (
                <div className="space-y-4">
                    <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                        <div>
                            <p className="text-xs text-slate-500">用户</p>
                            <p className="font-medium text-slate-800">{username}</p>
                        </div>
                    </div>

                    {availableFonds.length > 0 && (
                        <button
                            type="button"
                            onClick={toggleAll}
                            className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 transition-colors"
                        >
                            <div className={`w-4 h-4 border rounded flex items-center justify-center transition-colors ${
                                isAllSelected
                                    ? 'bg-blue-600 border-blue-600'
                                    : 'border-slate-300'
                            }`}>
                                {isAllSelected && (
                                    <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                    </svg>
                                )}
                            </div>
                            {isAllSelected ? '取消全选' : '全选'}
                        </button>
                    )}

                    <div className="space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto">
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
                                                ? 'bg-blue-50 border-blue-200'
                                                : 'bg-white border-slate-200 hover:border-slate-300'
                                        }`}
                                    >
                                        <div className={`w-5 h-5 border rounded flex items-center justify-center transition-colors ${
                                            isSelected
                                                ? 'bg-blue-600 border-blue-600'
                                                : 'border-slate-300'
                                        }`}>
                                            {isSelected && (
                                                <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                                </svg>
                                            )}
                                        </div>
                                        <div className="flex-1 text-left">
                                            <p className="font-medium text-slate-800">{fonds.fondsName}</p>
                                            <p className="text-sm text-slate-500">
                                                {fonds.companyName} · {fonds.fondsCode}
                                            </p>
                                        </div>
                                    </button>
                                );
                            })
                        )}
                    </div>

                    {availableFonds.length > 0 && (
                        <div className="pt-3 border-t">
                            <p className="text-sm text-slate-500 text-center">
                                已选择 {assignedFonds.length} / {availableFonds.length} 个全宗
                            </p>
                        </div>
                    )}
                </div>
            )}
        </Drawer>
    );
};

export default UserFondsScopeDrawer;
