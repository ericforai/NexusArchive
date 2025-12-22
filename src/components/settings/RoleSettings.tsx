// Input: React、lucide-react 图标、本地模块 api/admin
// Output: React 组件 RoleSettings
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { Plus, Edit3, Trash2, Loader2 } from 'lucide-react';
import { adminApi, Role } from '../../api/admin';

interface RoleForm {
    id?: string;
    name: string;
    code: string;
    roleCategory: string;
    type: string;
    description: string;
    permissions: string[];
}

const EXCLUSIVE_ROLE_CODES = ['system_admin', 'security_admin', 'audit_admin'];

/**
 * 角色权限管理页面
 * 
 * 包含角色 CRUD 和权限分配
 */
export const RoleSettings: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>([]);
    const [roleLoading, setRoleLoading] = useState(false);
    const [roleSaving, setRoleSaving] = useState(false);
    const [permissionOptions, setPermissionOptions] = useState<{ key: string; label: string; group: string }[]>([]);

    const [roleForm, setRoleForm] = useState<RoleForm>({
        name: '',
        code: '',
        roleCategory: 'business_user',
        type: 'custom',
        description: '',
        permissions: []
    });

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
        const loadPerms = async () => {
            try {
                const res = await adminApi.getPermissions();
                if (res.code === 200 && res.data) {
                    // 后端返回 { permKey, label, groupName }，需要映射为 { key, label, group }
                    const mapped = (res.data as any[]).map((p: any) => ({
                        key: p.permKey || p.key,
                        label: p.label,
                        group: p.groupName || p.group || '其他'
                    }));
                    setPermissionOptions(mapped);
                }
            } catch (e) {
                // ignore
            }
        };
        loadRoles();
        loadPerms();
    }, []);

    const togglePermissionSelection = (perm: string) => {
        setRoleForm((prev) => {
            const exists = prev.permissions.includes(perm);
            return { ...prev, permissions: exists ? prev.permissions.filter((p) => p !== perm) : [...prev.permissions, perm] };
        });
    };

    const handleRoleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!roleForm.name || !roleForm.code) {
            alert('角色名称和编码必填');
            return;
        }
        if (EXCLUSIVE_ROLE_CODES.includes(roleForm.code) && roleForm.roleCategory !== 'system_admin' && roleForm.roleCategory !== 'security_admin' && roleForm.roleCategory !== 'audit_admin') {
            alert('三员互斥：系统/安全/审计角色的类别应与其编码一致');
            return;
        }
        setRoleSaving(true);
        try {
            const payload = {
                name: roleForm.name,
                code: roleForm.code,
                roleCategory: roleForm.roleCategory,
                type: roleForm.type,
                description: roleForm.description,
                permissions: JSON.stringify(roleForm.permissions)
            };
            const res = roleForm.id
                ? await adminApi.updateRole(roleForm.id, payload)
                : await adminApi.createRole(payload);

            if (res.code === 200) {
                setRoleForm({
                    id: undefined,
                    name: '',
                    code: '',
                    roleCategory: 'business_user',
                    type: 'custom',
                    description: '',
                    permissions: []
                });
                const reload = await adminApi.getRoles({ page: 1, limit: 100 });
                if (reload.code === 200 && reload.data) setRoles(reload.data.records || []);
            } else {
                alert(res.message || '保存失败');
            }
        } catch (e: any) {
            alert(e?.response?.data?.message || '保存失败');
        } finally {
            setRoleSaving(false);
        }
    };

    const handleRoleEdit = (role: Role) => {
        let perms: string[] = [];
        if (role.permissions) {
            try {
                const parsed = JSON.parse(role.permissions);
                if (Array.isArray(parsed)) perms = parsed;
            } catch {
                perms = [];
            }
        }

        if (EXCLUSIVE_ROLE_CODES.includes(role.code)) {
            alert('提示：系统/安全/审计三员角色互斥，请勿同时分配给同一用户。');
        }

        setRoleForm({
            id: role.id,
            name: role.name,
            code: role.code,
            roleCategory: role.roleCategory || 'business_user',
            type: role.type || 'custom',
            description: role.description || '',
            permissions: perms
        });
    };

    const handleRoleDelete = async (id: string) => {
        if (!window.confirm('确认删除该角色？')) return;
        setRoleSaving(true);
        try {
            const res = await adminApi.deleteRole(id);
            if (res.code === 200) {
                const reload = await adminApi.getRoles({ page: 1, limit: 100 });
                if (reload.code === 200 && reload.data) setRoles(reload.data.records || []);
            } else {
                alert(res.message || '删除失败');
            }
        } catch (e: any) {
            alert(e?.response?.data?.message || '删除失败');
        } finally {
            setRoleSaving(false);
        }
    };

    // 按组分类权限
    const groupedPermissions = permissionOptions.reduce((acc, curr) => {
        const g = curr.group || '其他';
        if (!acc[g]) acc[g] = [];
        acc[g].push(curr);
        return acc;
    }, {} as Record<string, typeof permissionOptions>);

    return (
        <div className="space-y-6">
            {/* 角色表单 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 mb-1">
                            {roleForm.id ? '编辑角色' : '创建角色'}
                        </h3>
                        <p className="text-xs text-slate-500">配置角色信息和权限分配</p>
                    </div>
                </div>

                <form className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4" onSubmit={handleRoleSave}>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">角色名称 *</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={roleForm.name}
                            onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">角色编码 *</label>
                        <input
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={roleForm.code}
                            onChange={(e) => setRoleForm({ ...roleForm, code: e.target.value })}
                            required
                        />
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">角色类别</label>
                        <select
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={roleForm.roleCategory}
                            onChange={(e) => setRoleForm({ ...roleForm, roleCategory: e.target.value })}
                        >
                            <option value="business_user">业务操作员</option>
                            <option value="system_admin">系统管理员</option>
                            <option value="security_admin">安全保密员</option>
                            <option value="audit_admin">安全审计员</option>
                        </select>
                    </div>
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-1">类型</label>
                        <select
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={roleForm.type}
                            onChange={(e) => setRoleForm({ ...roleForm, type: e.target.value })}
                        >
                            <option value="custom">自定义</option>
                            <option value="system">系统</option>
                        </select>
                    </div>
                    <div className="flex flex-col md:col-span-2">
                        <label className="text-sm font-medium text-slate-700 mb-1">描述</label>
                        <textarea
                            className="border border-slate-300 rounded-lg p-2 text-sm"
                            value={roleForm.description}
                            onChange={(e) => setRoleForm({ ...roleForm, description: e.target.value })}
                        />
                    </div>
                    <div className="flex flex-col md:col-span-2">
                        <label className="text-sm font-medium text-slate-700 mb-2">分配权限</label>
                        <div className="space-y-3 max-h-60 overflow-y-auto border border-slate-200 rounded p-3">
                            {(Object.entries(groupedPermissions) as [string, { key: string; label: string; group: string }[]][]).map(([group, perms]) => (
                                <div key={group}>
                                    <div className="text-xs font-bold text-slate-500 mb-1 bg-slate-50 px-1">{group}</div>
                                    <div className="flex flex-wrap gap-2">
                                        {perms.map((perm) => (
                                            <label
                                                key={perm.key}
                                                className="inline-flex items-center space-x-1 px-2 py-1 border border-slate-200 rounded text-xs cursor-pointer hover:bg-slate-50"
                                            >
                                                <input
                                                    type="checkbox"
                                                    checked={roleForm.permissions.includes(perm.key)}
                                                    onChange={() => togglePermissionSelection(perm.key)}
                                                />
                                                <span>{perm.label}</span>
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            ))}
                            {permissionOptions.length === 0 && <span className="text-xs text-slate-500">暂无权限定义</span>}
                        </div>
                    </div>
                    <div className="md:col-span-2 flex justify-end space-x-2">
                        {roleForm.id && (
                            <button
                                type="button"
                                className="px-3 py-2 text-xs border rounded"
                                onClick={() =>
                                    setRoleForm({
                                        id: undefined,
                                        name: '',
                                        code: '',
                                        roleCategory: 'business_user',
                                        type: 'custom',
                                        description: '',
                                        permissions: []
                                    })
                                }
                            >
                                取消编辑
                            </button>
                        )}
                        <button
                            type="submit"
                            disabled={roleSaving}
                            className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-60"
                        >
                            <Plus size={16} className="mr-2" />
                            {roleSaving ? '保存中...' : roleForm.id ? '更新角色' : '创建角色'}
                        </button>
                    </div>
                </form>
            </div>

            {/* 角色列表 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <h3 className="text-lg font-bold text-slate-800 mb-4">角色列表</h3>
                <div className="overflow-x-auto">
                    <table className="min-w-full text-sm text-left">
                        <thead>
                            <tr className="text-slate-500 border-b">
                                <th className="py-2 pr-4">角色名称</th>
                                <th className="py-2 pr-4">编码</th>
                                <th className="py-2 pr-4">类别</th>
                                <th className="py-2 pr-4">类型</th>
                                <th className="py-2 pr-4">描述</th>
                                <th className="py-2 pr-4">操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            {roleLoading ? (
                                <tr>
                                    <td colSpan={6} className="py-4 text-center text-slate-500">
                                        <Loader2 className="animate-spin inline-block mr-2" size={16} />
                                        加载中...
                                    </td>
                                </tr>
                            ) : roles.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="py-4 text-center text-slate-500">
                                        暂无角色
                                    </td>
                                </tr>
                            ) : (
                                roles.map((role) => (
                                    <tr key={role.id} className="border-b last:border-0">
                                        <td className="py-2 pr-4">{role.name}</td>
                                        <td className="py-2 pr-4">{role.code}</td>
                                        <td className="py-2 pr-4">{role.roleCategory || '-'}</td>
                                        <td className="py-2 pr-4">{role.type || '-'}</td>
                                        <td className="py-2 pr-4 max-w-xs truncate">{role.description || '-'}</td>
                                        <td className="py-2 pr-4 space-x-2 text-xs">
                                            <button
                                                className="text-blue-600 hover:underline inline-flex items-center"
                                                onClick={() => handleRoleEdit(role)}
                                            >
                                                <Edit3 size={14} className="mr-1" /> 编辑
                                            </button>
                                            {role.type !== 'system' && (
                                                <button
                                                    className="text-red-500 hover:underline inline-flex items-center"
                                                    onClick={() => handleRoleDelete(role.id)}
                                                    disabled={roleSaving}
                                                >
                                                    <Trash2 size={14} className="mr-1" /> 删除
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default RoleSettings;
