// Input: React、lucide-react 图标、AdminSettingsApi
// Output: React 组件 UserSettings
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Plus, Loader2, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { AdminSettingsApi } from './types';
import { User, Role } from '../../types';
import { toast } from '../utils/notificationService';

type UserStatus = 'active' | 'disabled' | 'locked';

interface CreateUserForm {
    username: string;
    password: string;
    fullName: string;
    email: string;
    phone: string;
    roleIds: string[];
}

const EXCLUSIVE_ROLE_CODES = ['system_admin', 'security_admin', 'audit_admin'];

/**
 * 用户管理页面
 * 
 * 包含用户创建和用户列表管理
 */
interface UserSettingsProps {
    adminApi: AdminSettingsApi;
}

export const UserSettings: React.FC<UserSettingsProps> = ({ adminApi }) => {
    const [users, setUsers] = useState<User[]>([]);
    const [page, setPage] = useState(1);
    const [pageSize] = useState(10);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const [roles, setRoles] = useState<Role[]>([]);
    const [roleLoading, setRoleLoading] = useState(false);
    const [createLoading, setCreateLoading] = useState(false);
    const [createSuccess, setCreateSuccess] = useState(false);
    const [createForm, setCreateForm] = useState<CreateUserForm>({
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        roleIds: []
    });

    const loadUsers = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await adminApi.getUsers({ page, limit: pageSize });
            if (res.code === 200 && res.data) {
                setUsers(res.data.records || []);
                setTotal(res.data.total || 0);
            } else {
                setError(res.message || '加载用户失败');
            }
        } catch (e: any) {
            setError(e?.response?.data?.message || '加载用户失败');
        } finally {
            setLoading(false);
        }
    }, [adminApi, page, pageSize]);

    useEffect(() => {
        loadUsers();
    }, [loadUsers]);

    useEffect(() => {
        const loadRoles = async () => {
            setRoleLoading(true);
            try {
                const res = await adminApi.getRoles({ page: 1, limit: 100 });
                if (res.code === 200 && res.data) {
                    setRoles(res.data.records || []);
                }
            } finally {
                setRoleLoading(false);
            }
        };
        loadRoles();
    }, [adminApi]);

    const handleResetPassword = async (id: string) => {
        const hint = '密码要求：至少8位，必须包含大写字母、小写字母、数字和特殊字符';
        const newPwd = window.prompt(`请输入新密码：\n\n${hint}`);
        if (!newPwd) return;

        // 前端验证密码策略
        if (newPwd.length < 8) {
            toast.warning('密码至少需要8位');
            return;
        }
        if (!/[A-Z]/.test(newPwd)) {
            toast.warning('密码需包含大写字母');
            return;
        }
        if (!/[a-z]/.test(newPwd)) {
            toast.warning('密码需包含小写字母');
            return;
        }
        if (!/\d/.test(newPwd)) {
            toast.warning('密码需包含数字');
            return;
        }
        if (!/[^A-Za-z0-9]/.test(newPwd)) {
            toast.warning('密码需包含特殊字符（如 !@#$%^&*）');
            return;
        }

        setActionLoading(id);
        try {
            const res = await adminApi.resetPassword(id, newPwd);
            if (res.code === 200) {
                toast.success('密码已重置');
            } else {
                toast.error(res.message || '重置失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '重置失败');
        } finally {
            setActionLoading(null);
        }
    };

    const handleStatusChange = async (id: string, status: UserStatus) => {
        setActionLoading(id);
        try {
            const res = await adminApi.toggleUserStatus(id, status);
            if (res.code === 200) {
                setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, status } : u)));
            } else {
                toast.error(res.message || '状态更新失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '状态更新失败');
        } finally {
            setActionLoading(null);
        }
    };

    const formatStatus = (status?: string) => {
        if (status === 'active') return '启用';
        if (status === 'disabled') return '禁用';
        if (status === 'locked') return '锁定';
        return status || '-';
    };

    const totalPages = Math.max(1, Math.ceil(total / pageSize));

    const handleCreateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!createForm.username || !createForm.password) {
            toast.warning('用户名和密码必填');
            return;
        }
        const selectedRoles = roles.filter(r => createForm.roleIds.includes(r.id));
        const selectedExclusive = selectedRoles.filter(r => EXCLUSIVE_ROLE_CODES.includes(r.code));
        if (selectedExclusive.length > 1) {
            setError('三员互斥：系统/安全/审计角色不可同时分配给同一用户');
            return;
        }
        setCreateLoading(true);
        setCreateSuccess(false);
        try {
            const res = await adminApi.createUser({
                username: createForm.username,
                password: createForm.password,
                fullName: createForm.fullName,
                email: createForm.email,
                phone: createForm.phone,
                roleIds: createForm.roleIds,
                status: 'active'
            });
            if (res.code === 200) {
                setCreateSuccess(true);
                setCreateForm({ username: '', password: '', fullName: '', email: '', phone: '', roleIds: [] });
                loadUsers();
            } else {
                toast.error(res.message || '创建失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '创建失败');
        } finally {
            setCreateLoading(false);
        }
    };

    const toggleRoleSelection = (roleId: string) => {
        setCreateForm((prev) => {
            const exists = prev.roleIds.includes(roleId);
            const nextRoles = exists ? prev.roleIds.filter((id) => id !== roleId) : [...prev.roleIds, roleId];
            const selectedExclusive = roles.filter(r => nextRoles.includes(r.id) && EXCLUSIVE_ROLE_CODES.includes(r.code));
            if (selectedExclusive.length > 1) {
                toast.warning('三员互斥：系统/安全/审计角色不可同时分配给同一用户');
                return prev;
            }
            return { ...prev, roleIds: nextRoles };
        });
    };

    return (
        <div className="space-y-6">
            {/* 创建用户 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 mb-1">创建用户</h3>
                        <p className="text-xs text-slate-500">填写基础信息并选择角色</p>
                    </div>
                    {createSuccess && (
                        <span className="inline-flex items-center text-xs text-emerald-600 bg-emerald-50 border border-emerald-100 rounded px-2 py-1">
                            <CheckCircle2 size={14} className="mr-1" /> 创建成功
                        </span>
                    )}
                </div>

                <form className="grid grid-cols-1 md:grid-cols-2 gap-4" onSubmit={handleCreateUser}>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">用户名 *</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.username}
                            onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                            required
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">密码 *</label>
                        <input
                            type="password"
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.password}
                            onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                            required
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">姓名</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.fullName}
                            onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">邮箱</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.email}
                            onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">手机号</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.phone}
                            onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })}
                        />
                    </div>
                    <div className="flex flex-col md:col-span-2">
                        <label className="text-sm font-medium text-slate-700 mb-1">分配角色</label>
                        <div className="flex flex-wrap gap-2">
                            {roleLoading && <span className="text-xs text-slate-500">加载角色...</span>}
                            {!roleLoading &&
                                roles.map((role) => (
                                    <label
                                        key={role.id}
                                        className="inline-flex items-center space-x-1 px-2 py-1 border border-slate-200 rounded text-xs cursor-pointer hover:bg-slate-50"
                                    >
                                        <input
                                            type="checkbox"
                                            checked={createForm.roleIds.includes(role.id)}
                                            onChange={() => toggleRoleSelection(role.id)}
                                        />
                                        <span>{role.name}</span>
                                    </label>
                                ))}
                            {!roleLoading && roles.length === 0 && (
                                <span className="text-xs text-slate-500">暂无角色，请先创建角色。</span>
                            )}
                        </div>
                    </div>
                    <div className="md:col-span-2 flex justify-end">
                        <button
                            type="submit"
                            disabled={createLoading}
                            className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-60"
                        >
                            <Plus size={16} className="mr-2" />
                            {createLoading ? '创建中...' : '创建用户'}
                        </button>
                    </div>
                </form>
            </div>

            {/* 用户列表 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 mb-1">用户管理</h3>
                        <p className="text-xs text-slate-500">分页列表、状态切换、重置密码</p>
                    </div>
                    <button
                        onClick={loadUsers}
                        className="inline-flex items-center px-3 py-1.5 text-sm rounded-lg border border-slate-200 hover:bg-slate-50"
                    >
                        <RefreshCw size={14} className="mr-2" /> 刷新
                    </button>
                </div>

                {error && <div className="text-sm text-red-500 mb-3">{error}</div>}

                <div className="overflow-x-auto">
                    <table className="min-w-full text-sm text-left">
                        <thead>
                            <tr className="text-slate-500 border-b">
                                <th className="py-2 pr-4">用户名</th>
                                <th className="py-2 pr-4">姓名</th>
                                <th className="py-2 pr-4">邮箱</th>
                                <th className="py-2 pr-4">手机号</th>
                                <th className="py-2 pr-4">状态</th>
                                <th className="py-2 pr-4">操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan={6} className="py-6 text-center text-slate-500">
                                        <Loader2 className="animate-spin inline-block mr-2" size={16} />
                                        加载中...
                                    </td>
                                </tr>
                            ) : users.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="py-6 text-center text-slate-500">
                                        暂无用户数据
                                    </td>
                                </tr>
                            ) : (
                                users.map((user) => (
                                    <tr key={user.id} className="border-b last:border-0">
                                        <td className="py-2 pr-4">{user.username}</td>
                                        <td className="py-2 pr-4">{user.fullName || '-'}</td>
                                        <td className="py-2 pr-4">{user.email || '-'}</td>
                                        <td className="py-2 pr-4">{user.phone || '-'}</td>
                                        <td className="py-2 pr-4">
                                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs bg-slate-100 text-slate-700">
                                                <ShieldCheck size={12} className="mr-1" />
                                                {formatStatus(user.status)}
                                            </span>
                                        </td>
                                        <td className="py-2 pr-4 space-x-2">
                                            <select
                                                className="border border-slate-200 rounded px-2 py-1 text-xs"
                                                value={user.status || 'active'}
                                                disabled={actionLoading === user.id}
                                                onChange={(e) => handleStatusChange(user.id, e.target.value as UserStatus)}
                                            >
                                                <option value="active">启用</option>
                                                <option value="disabled">禁用</option>
                                                <option value="locked">锁定</option>
                                            </select>
                                            <button
                                                onClick={() => handleResetPassword(user.id)}
                                                disabled={actionLoading === user.id}
                                                className="text-blue-600 hover:underline text-xs disabled:opacity-50"
                                            >
                                                重置密码
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="flex items-center justify-between mt-4 text-xs text-slate-600">
                    <div>
                        第 {page} / {totalPages} 页（共 {total} 条）
                    </div>
                    <div className="space-x-2">
                        <button
                            className="px-3 py-1 border rounded disabled:opacity-40"
                            disabled={page <= 1 || loading}
                            onClick={() => setPage((p) => Math.max(1, p - 1))}
                        >
                            上一页
                        </button>
                        <button
                            className="px-3 py-1 border rounded disabled:opacity-40"
                            disabled={page >= totalPages || loading}
                            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                        >
                            下一页
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserSettings;
