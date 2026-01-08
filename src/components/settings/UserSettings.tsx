// Input: React、lucide-react 图标、AdminSettingsApi
// Output: React 组件 UserSettings
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Plus, Loader2, ShieldCheck, CheckCircle2, Shield } from 'lucide-react';
import { AdminSettingsApi } from './types';
import { User, Role } from '../../types';
import { toast } from '../../utils/notificationService';
import { UserFondsScopeDrawer } from './UserFondsScopeDrawer';

type UserStatus = 'active' | 'disabled' | 'locked';

interface CreateUserForm {
    username: string;
    password: string;
    fullName: string;
    email: string;
    phone: string;
    roleIds: string[];
    fondsCodes: string[];
}

const EXCLUSIVE_ROLE_CODES = ['system_admin', 'security_admin', 'audit_admin'];

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
        roleIds: [],
        fondsCodes: []
    });

    const [availableFonds, setAvailableFonds] = useState<Array<{ fondsCode: string; fondsName: string; companyName: string }>>([]);

    const [fondsScopeDrawerOpen, setFondsScopeDrawerOpen] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string>('');
    const [selectedUsername, setSelectedUsername] = useState<string>('');

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

    useEffect(() => {
        const loadAvailableFonds = async () => {
            try {
                const res = await adminApi.getFondsList();
                if (res.code === 200 && res.data) {
                    setAvailableFonds(res.data.map((f: any) => ({
                        fondsCode: f.fondsCode,
                        fondsName: f.fondsName,
                        companyName: f.companyName
                    })));
                }
            } catch (e) {
                console.error('Failed to load fonds list:', e);
            }
        };
        loadAvailableFonds();
    }, [adminApi]);

    const handleResetPassword = async (id: string) => {
        const hint = '密码要求：至少8位，必须包含大写字母、小写字母、数字和特殊字符';
        const newPwd = window.prompt(`请输入新密码：\n\n${hint}`);
        if (!newPwd) return;

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

    const handleOpenFondsScopeDrawer = (userId: string, username: string) => {
        setSelectedUserId(userId);
        setSelectedUsername(username);
        setFondsScopeDrawerOpen(true);
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
        // 前端密码策略校验 - 一次性收集所有不满足的条件
        const pwd = createForm.password;
        const pwdErrors: string[] = [];
        if (pwd.length < 8) pwdErrors.push('至少8位');
        if (!/[A-Z]/.test(pwd)) pwdErrors.push('包含大写字母');
        if (!/[a-z]/.test(pwd)) pwdErrors.push('包含小写字母');
        if (!/\d/.test(pwd)) pwdErrors.push('包含数字');
        if (!/[^A-Za-z0-9]/.test(pwd)) pwdErrors.push('包含特殊字符');

        if (pwdErrors.length > 0) {
            toast.error(`密码不符合策略要求，需满足：${pwdErrors.join('、')}`);
            return;
        }
        if (!createForm.fullName || !createForm.fullName.trim()) {
            toast.warning('姓名必填');
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
            const requestData = {
                username: createForm.username,
                password: createForm.password,
                fullName: createForm.fullName,
                email: createForm.email || undefined,  // 发送 undefined 而不是空字符串
                phone: createForm.phone || undefined,  // 发送 undefined 而不是空字符串
                roleIds: createForm.roleIds,
                fondsCodes: createForm.fondsCodes
                // 移除 status 字段 - 后端 DTO 没有此字段
            };
            console.log('[UserSettings] Creating user with request:', {
                ...requestData,
                password: '***'  // 隐藏密码
            });
            const res = await adminApi.createUser(requestData);
            if (res.code === 200) {
                setCreateSuccess(true);
                setCreateForm({ username: '', password: '', fullName: '', email: '', phone: '', roleIds: [], fondsCodes: [] });
                loadUsers();
            } else {
                toast.error(res.message || '创建失败');
            }
        } catch (e: any) {
            // 详细日志：完整错误响应
            console.error('[UserSettings] Create user error:', {
                status: e?.response?.status,
                statusText: e?.response?.statusText,
                data: e?.response?.data,
                message: e?.response?.data?.message,
                fullError: e
            });
            toast.error(e?.response?.data?.message || e?.message || '创建失败');
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

    const toggleFondsSelection = (fondsCode: string) => {
        setCreateForm((prev) => {
            const exists = prev.fondsCodes.includes(fondsCode);
            const nextFonds = exists ? prev.fondsCodes.filter((c) => c !== fondsCode) : [...prev.fondsCodes, fondsCode];
            return { ...prev, fondsCodes: nextFonds };
        });
    };

    const toggleAllFonds = () => {
        setCreateForm((prev) => {
            if (prev.fondsCodes.length === availableFonds.length) {
                return { ...prev, fondsCodes: [] };
            } else {
                return { ...prev, fondsCodes: availableFonds.map(f => f.fondsCode) };
            }
        });
    };

    return (
        <div className="space-y-6">
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

                <form className="grid grid-cols-1 md:grid-cols-3 gap-4" onSubmit={handleCreateUser}>
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
                            placeholder="至少8位，含大小写字母、数字、特殊字符"
                        />
                        <p className="text-xs text-slate-400 mt-1">如: Admin@123</p>
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">姓名 *</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={createForm.fullName}
                            onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                            required
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">邮箱</label>
                        <input
                            type="email"
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
                        <div className="flex items-center justify-between mb-2">
                            <label className="text-sm font-medium text-slate-700">全宗权限</label>
                            {availableFonds.length > 0 && (
                                <button
                                    type="button"
                                    onClick={toggleAllFonds}
                                    className="text-xs text-blue-600 hover:text-blue-700"
                                >
                                    {createForm.fondsCodes.length === availableFonds.length ? '取消全选' : '全选'}
                                </button>
                            )}
                        </div>
                        <div className="flex flex-wrap gap-2 max-h-24 overflow-y-auto border border-slate-200 rounded-lg p-2">
                            {availableFonds.length === 0 ? (
                                <span className="text-xs text-slate-500">暂无可用全宗</span>
                            ) : (
                                availableFonds.map((fonds) => (
                                    <label
                                        key={fonds.fondsCode}
                                        className="inline-flex items-center space-x-1 px-2 py-1 border border-slate-200 rounded text-xs cursor-pointer hover:bg-blue-50 hover:border-blue-300"
                                    >
                                        <input
                                            type="checkbox"
                                            checked={createForm.fondsCodes.includes(fonds.fondsCode)}
                                            onChange={() => toggleFondsSelection(fonds.fondsCode)}
                                        />
                                        <span>{fonds.fondsName}</span>
                                    </label>
                                ))
                            )}
                        </div>
                        {availableFonds.length > 0 && (
                            <p className="text-xs text-slate-500 mt-1">
                                已选择 {createForm.fondsCodes.length} / {availableFonds.length} 个全宗
                            </p>
                        )}
                    </div>
                    <div className="md:col-span-3">
                        <label className="text-sm font-medium text-slate-700 mb-2 block">角色</label>
                        {roleLoading ? (
                            <div className="flex items-center text-sm text-slate-500">
                                <Loader2 size={16} className="animate-spin mr-2" />
                                加载角色中...
                            </div>
                        ) : (
                            <div className="flex flex-wrap gap-2">
                                {roles.map((role) => (
                                    <label
                                        key={role.id}
                                        className={`inline-flex items-center space-x-2 px-3 py-1.5 border rounded-lg cursor-pointer transition-all ${createForm.roleIds.includes(role.id)
                                            ? 'bg-blue-50 border-blue-500 text-blue-700'
                                            : 'bg-white border-slate-200 hover:border-blue-300'
                                            }`}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={createForm.roleIds.includes(role.id)}
                                            onChange={() => toggleRoleSelection(role.id)}
                                            className="sr-only"
                                        />
                                        <Shield size={14} className={createForm.roleIds.includes(role.id) ? 'text-blue-600' : 'text-slate-400'} />
                                        <span>{role.name}</span>
                                    </label>
                                ))}
                                {roles.length === 0 && (
                                    <span className="text-xs text-slate-500">暂无角色，请先创建角色。</span>
                                )}
                            </div>
                        )}
                    </div>
                    <div className="md:col-span-3 flex justify-end">
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
                                    <tr key={user.id} className="border-b hover:bg-slate-50">
                                        <td className="py-3 pr-4 font-medium">{user.username}</td>
                                        <td className="py-3 pr-4">{user.fullName || '-'}</td>
                                        <td className="py-3 pr-4">{user.email || '-'}</td>
                                        <td className="py-3 pr-4">{user.phone || '-'}</td>
                                        <td className="py-3 pr-4">
                                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs ${user.status === 'active' ? 'bg-green-100 text-green-700' :
                                                user.status === 'disabled' ? 'bg-gray-100 text-gray-700' :
                                                    'bg-red-100 text-red-700'
                                                }`}>
                                                {formatStatus(user.status)}
                                            </span>
                                        </td>
                                        <td className="py-3 pr-4">
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => handleOpenFondsScopeDrawer(user.id, user.username)}
                                                    className="text-blue-600 hover:text-blue-700 text-xs flex items-center"
                                                    title="设置全宗权限"
                                                >
                                                    <ShieldCheck size={14} className="mr-1" />
                                                    全宗权限
                                                </button>
                                                <button
                                                    onClick={() => handleResetPassword(user.id)}
                                                    disabled={actionLoading === user.id}
                                                    className="text-slate-600 hover:text-slate-800 text-xs"
                                                >
                                                    重置密码
                                                </button>
                                                {user.status === 'active' ? (
                                                    <button
                                                        onClick={() => handleStatusChange(user.id, 'disabled')}
                                                        disabled={actionLoading === user.id}
                                                        className="text-orange-600 hover:text-orange-700 text-xs"
                                                    >
                                                        禁用
                                                    </button>
                                                ) : (
                                                    <button
                                                        onClick={() => handleStatusChange(user.id, 'active')}
                                                        disabled={actionLoading === user.id}
                                                        className="text-green-600 hover:text-green-700 text-xs"
                                                    >
                                                        启用
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {totalPages > 1 && (
                    <div className="flex items-center justify-between mt-4 pt-4 border-t">
                        <span className="text-sm text-slate-500">
                            共 {total} 条记录，第 {page} / {totalPages} 页
                        </span>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setPage(p => Math.max(1, p - 1))}
                                disabled={page === 1}
                                className="px-3 py-1 text-sm border rounded hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                上一页
                            </button>
                            <button
                                onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                                disabled={page === totalPages}
                                className="px-3 py-1 text-sm border rounded hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                下一页
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <UserFondsScopeDrawer
                isOpen={fondsScopeDrawerOpen}
                onClose={() => setFondsScopeDrawerOpen(false)}
                userId={selectedUserId}
                username={selectedUsername}
                onSuccess={loadUsers}
            />
        </div>
    );
};

export default UserSettings;
