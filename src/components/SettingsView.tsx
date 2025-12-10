import React, { useEffect, useState } from 'react';
import { Save, RefreshCw, ShieldCheck, Loader2, Plus, CheckCircle2, Trash2, Edit3, Upload, AlertTriangle } from 'lucide-react';
import { adminApi, User, Role } from '../api/admin';
import { Tree } from './org/Tree';
import { AuditLogView } from './AuditLogView';
import { FondsManagement } from './admin/FondsManagement';
import { LicenseSettings } from './settings/LicenseSettings';

type UserStatus = 'active' | 'disabled' | 'locked';

interface CreateUserForm {
  username: string;
  password: string;
  fullName: string;
  email: string;
  phone: string;
  roleIds: string[];
}

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

export const SettingsView: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [orgs, setOrgs] = useState<any[]>([]);
  const [orgTree, setOrgTree] = useState<any[]>([]);
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

  const [orgForm, setOrgForm] = useState({ name: '', code: '', parentId: '', type: 'DEPARTMENT', orderNum: 0 });
  const [orgLoading, setOrgLoading] = useState(false);
  const [orgImportText, setOrgImportText] = useState('');
  const [orgImportResult, setOrgImportResult] = useState<{ successCount: number; failCount: number; errors?: string[] } | null>(null);

  const [permissionOptions, setPermissionOptions] = useState<{ key: string; label: string; group: string }[]>([]);
  const [roleForm, setRoleForm] = useState<RoleForm>({
    name: '',
    code: '',
    roleCategory: 'business_user',
    type: 'custom',
    description: '',
    permissions: []
  });
  const [roleSaving, setRoleSaving] = useState(false);
  const [settings, setSettings] = useState<any>({});
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaving, setSettingsSaving] = useState(false);

  const loadUsers = async () => {
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
  };

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

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
          setPermissionOptions(res.data as any);
        }
      } catch (e) {
        // ignore
      }
    };
    loadRoles();
    loadPerms();
  }, []);

  useEffect(() => {
    const loadSettings = async () => {
      setSettingsLoading(true);
      try {
        const res = await adminApi.getSettings();
        if (res.code === 200 && res.data) {
          const map: any = {};
          (res.data as any[]).forEach((item: any) => {
            map[item.configKey] = item.configValue;
          });
          setSettings(map);
        }
      } finally {
        setSettingsLoading(false);
      }
    };
    const loadOrgs = async () => {
      setOrgLoading(true);
      try {
        const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
        if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
        if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
      } finally {
        setOrgLoading(false);
      }
    };
    loadSettings();
    loadOrgs();
  }, []);

  const handleResetPassword = async (id: string) => {
    const newPwd = window.prompt('请输入新密码：');
    if (!newPwd) return;
    setActionLoading(id);
    try {
      const res = await adminApi.resetPassword(id, newPwd);
      if (res.code === 200) {
        alert('密码已重置');
      } else {
        alert(res.message || '重置失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '重置失败');
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
        alert(res.message || '状态更新失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '状态更新失败');
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
      alert('用户名和密码必填');
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
      } as any);
      if (res.code === 200) {
        setCreateSuccess(true);
        setCreateForm({ username: '', password: '', fullName: '', email: '', phone: '', roleIds: [] });
        loadUsers();
      } else {
        alert(res.message || '创建失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '创建失败');
    } finally {
      setCreateLoading(false);
    }
  };

  const toggleRoleSelection = (roleId: string) => {
    setCreateForm((prev) => {
      const exists = prev.roleIds.includes(roleId);
      const nextRoles = exists ? prev.roleIds.filter((id) => id !== roleId) : [...prev.roleIds, roleId];
      // 三员互斥：阻止同时选择多个 system/security/audit
      const selectedExclusive = roles.filter(r => nextRoles.includes(r.id) && EXCLUSIVE_ROLE_CODES.includes(r.code));
      if (selectedExclusive.length > 1) {
        alert('三员互斥：系统/安全/审计角色不可同时分配给同一用户');
        return prev;
      }
      return { ...prev, roleIds: nextRoles };
    });
  };

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

    // 三员互斥提示：编辑时如为系统角色，提示不可与其他互斥角色并存（仅前端提示，不覆盖后端校验）
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

  const handleSettingsSave = async () => {
    setSettingsSaving(true);
    try {
      const payload = [
        { configKey: 'system.name', configValue: settings['system.name'], category: 'system' },
        { configKey: 'archive.prefix', configValue: settings['archive.prefix'], category: 'archive' },
        { configKey: 'storage.type', configValue: settings['storage.type'], category: 'storage' },
        { configKey: 'storage.path', configValue: settings['storage.path'], category: 'storage' },
        { configKey: 'retention.default', configValue: settings['retention.default'], category: 'archive' },
      ];
      const res = await adminApi.updateSettings(payload);
      if (res.code === 200) {
        alert('保存成功');
      } else {
        alert(res.message || '保存失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '保存失败');
    } finally {
      setSettingsSaving(false);
    }
  };

  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orgForm.name) {
      alert('组织名称必填');
      return;
    }
    setOrgLoading(true);
    try {
      const res = await adminApi.createOrg({
        name: orgForm.name,
        code: orgForm.code,
        parentId: orgForm.parentId || undefined,
        type: orgForm.type,
        orderNum: orgForm.orderNum
      });
      if (res.code === 200) {
        setOrgForm({ name: '', code: '', parentId: '', type: 'DEPARTMENT', orderNum: 0 });
        const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
        if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
        if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
      } else {
        alert(res.message || '创建失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '创建失败');
    } finally {
      setOrgLoading(false);
    }
  };

  const handleDeleteOrg = async (id: string) => {
    if (!window.confirm('确认删除该组织/部门吗？')) return;
    setOrgLoading(true);
    try {
      await adminApi.deleteOrg(id);
      const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
      if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
      if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
    } finally {
      setOrgLoading(false);
    }
  };

  const handleOrgReorder = async (id: string, direction: 'up' | 'down') => {
    const target = orgs.find((o) => o.id === id);
    if (!target) return;
    const delta = direction === 'up' ? -1 : 1;
    const newOrder = (target.orderNum || 0) + delta;
    setOrgLoading(true);
    try {
      await adminApi.updateOrgOrder(id, newOrder);
      const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
      if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
      if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
    } finally {
      setOrgLoading(false);
    }
  };

  const handleOrgBulkImport = async () => {
    if (!orgImportText.trim()) return;
    let payload: any[] = [];
    try {
      payload = JSON.parse(orgImportText);
      if (!Array.isArray(payload)) throw new Error();
    } catch {
      alert('请输入合法的 JSON 数组，例如 [{"name":"财务部","code":"FIN"}]');
      return;
    }
    setOrgLoading(true);
    try {
      const res = await adminApi.bulkOrg(payload);
      if (res.code === 200) {
        setOrgImportText('');
        setOrgImportResult(null);
        const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
        if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
        if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
      } else {
        alert(res.message || '导入失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '导入失败');
    } finally {
      setOrgLoading(false);
    }
  };

  const handleOrgFileImport = async (file?: File) => {
    if (!file) return;
    setOrgLoading(true);
    setOrgImportResult(null);
    try {
      const res = await adminApi.importOrg(file);
      if (res.code === 200 && res.data) {
        setOrgImportResult(res.data as any);
        const [listRes, treeRes] = await Promise.all([adminApi.listOrg(), adminApi.getOrgTree()]);
        if (listRes.code === 200 && listRes.data) setOrgs(listRes.data);
        if (treeRes.code === 200 && treeRes.data) setOrgTree(treeRes.data);
      } else {
        alert(res.message || '导入失败');
      }
    } catch (e: any) {
      alert(e?.response?.data?.message || '导入失败');
    } finally {
      setOrgLoading(false);
    }
  };

  return (
    <div className="p-8 space-y-6 max-w-5xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">系统设置</h2>
          <p className="text-slate-500">配置全局参数、用户权限及安全策略。</p>
        </div>
        <button
          onClick={handleSettingsSave}
          disabled={settingsSaving}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all disabled:opacity-60"
        >
          <Save size={16} className="mr-2" /> {settingsSaving ? '保存中...' : '保存更改'}
        </button>
      </div>

      <div className="space-y-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">基础设置</h3>
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4">
            {/* ... basic settings ... */}
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">系统名称</label>
              <input
                type="text"
                value={settings['system.name'] || ''}
                onChange={(e) => setSettings({ ...settings, ['system.name']: e.target.value })}
                className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">归档全宗号前缀</label>
              <input
                type="text"
                value={settings['archive.prefix'] || ''}
                onChange={(e) => setSettings({ ...settings, ['archive.prefix']: e.target.value })}
                className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">存储类型</label>
              <select
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={settings['storage.type'] || 'local'}
                onChange={(e) => setSettings({ ...settings, ['storage.type']: e.target.value })}
              >
                <option value="local">本地/NAS</option>
                <option value="oss">对象存储</option>
              </select>
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">存储路径/桶名</label>
              <input
                type="text"
                value={settings['storage.path'] || ''}
                onChange={(e) => setSettings({ ...settings, ['storage.path']: e.target.value })}
                className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">默认保管期限</label>
              <input
                type="text"
                value={settings['retention.default'] || ''}
                onChange={(e) => setSettings({ ...settings, ['retention.default']: e.target.value })}
                className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </div>
          </div>
        </div>

        {/* Fonds Management Section */}
        <FondsManagement />

        {/* License Management Section */}
        <LicenseSettings />

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">组织/部门</h3>
              <p className="text-xs text-slate-500">创建、查看组织树。</p>
            </div>
          </div>
          <form className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4" onSubmit={handleCreateOrg}>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">名称 *</label>
              <input
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={orgForm.name}
                onChange={(e) => setOrgForm({ ...orgForm, name: e.target.value })}
                required
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">编码</label>
              <input
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={orgForm.code}
                onChange={(e) => setOrgForm({ ...orgForm, code: e.target.value })}
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">上级 ID</label>
              <input
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={orgForm.parentId}
                onChange={(e) => setOrgForm({ ...orgForm, parentId: e.target.value })}
                placeholder="留空为顶级"
              />
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">类型</label>
              <select
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={orgForm.type}
                onChange={(e) => setOrgForm({ ...orgForm, type: e.target.value })}
              >
                <option value="COMPANY">公司</option>
                <option value="DEPARTMENT">部门</option>
              </select>
            </div>
            <div className="flex flex-col">
              <label className="text-sm font-medium text-slate-700 mb-1">排序</label>
              <input
                type="number"
                className="border border-slate-300 rounded-lg p-2 text-sm"
                value={orgForm.orderNum}
                onChange={(e) => setOrgForm({ ...orgForm, orderNum: Number(e.target.value) })}
              />
            </div>
            <div className="md:col-span-3 flex justify-end">
              <button
                type="submit"
                disabled={orgLoading}
                className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-60"
              >
                <Plus size={16} className="mr-2" />
                {orgLoading ? '保存中...' : '创建组织/部门'}
              </button>
            </div>
          </form>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="border border-slate-200 rounded-lg p-3">
              <h4 className="text-sm font-semibold text-slate-800 mb-2">组织树</h4>
              {orgLoading ? (
                <div className="text-xs text-slate-500 flex items-center">
                  <Loader2 size={14} className="animate-spin mr-1" /> 加载中...
                </div>
              ) : (
                <Tree data={orgTree} />
              )}
            </div>
            <div className="border border-slate-200 rounded-lg p-3">
              <h4 className="text-sm font-semibold text-slate-800 mb-2">组织列表</h4>
              <div className="max-h-64 overflow-y-auto text-sm">
                {orgs.map((org) => (
                  <div key={org.id} className="flex items-center justify-between py-1 border-b last:border-0">
                    <div>
                      <div className="text-slate-800">{org.name}</div>
                      <div className="text-xs text-slate-500">
                        {org.type} · {org.code || '-'} · parent: {org.parentId || '根'}
                      </div>
                    </div>
                    <button
                      className="text-red-500 text-xs hover:underline"
                      onClick={() => handleDeleteOrg(org.id)}
                      disabled={orgLoading}
                    >
                      删除
                    </button>
                  </div>
                ))}
                {orgs.length === 0 && <div className="text-xs text-slate-500">暂无数据</div>}
              </div>
              <div className="mt-3 space-x-2 text-xs text-slate-600">
                <span className="font-semibold">排序</span>
                <span>点击上/下调整排序号并刷新树：</span>
              </div>
              <div className="max-h-32 overflow-y-auto text-xs mt-2 space-y-1">
                {orgs.map((org) => (
                  <div key={org.id} className="flex items-center justify-between">
                    <span>{org.name}（{org.orderNum || 0}）</span>
                    <span className="space-x-1">
                      <button
                        className="px-2 py-0.5 border rounded disabled:opacity-40"
                        disabled={orgLoading}
                        onClick={() => handleOrgReorder(org.id, 'up')}
                      >
                        上
                      </button>
                      <button
                        className="px-2 py-0.5 border rounded disabled:opacity-40"
                        disabled={orgLoading}
                        onClick={() => handleOrgReorder(org.id, 'down')}
                      >
                        下
                      </button>
                    </span>
                  </div>
                ))}
              </div>
              <div className="mt-4">
                <h5 className="text-sm font-semibold text-slate-800 mb-1">批量导入 (JSON 数组)</h5>
                <textarea
                  className="w-full border border-slate-200 rounded p-2 text-xs"
                  rows={4}
                  placeholder='例如: [{"name":"财务部","code":"FIN"},{"name":"人力资源部","code":"HR","parentId":"<上级ID>"}]'
                  value={orgImportText}
                  onChange={(e) => setOrgImportText(e.target.value)}
                />
                <div className="flex justify-end mt-2">
                  <button
                    className="px-3 py-1.5 text-xs bg-primary-600 text-white rounded hover:bg-primary-700 disabled:opacity-50"
                    disabled={orgLoading}
                    onClick={handleOrgBulkImport}
                  >
                    批量导入
                  </button>
                </div>
              </div>
              <div className="mt-4 space-y-2">
                <h5 className="text-sm font-semibold text-slate-800">CSV/Excel 导入</h5>
                <div className="flex items-center space-x-2">
                  <label className="px-3 py-1.5 text-xs border rounded cursor-pointer hover:bg-slate-50 inline-flex items-center space-x-2">
                    <Upload size={14} />
                    <span>选择文件</span>
                    <input
                      type="file"
                      accept=".csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                      className="hidden"
                      onChange={(e) => handleOrgFileImport(e.target.files?.[0])}
                    />
                  </label>
                  <button
                    className="text-xs text-blue-600 hover:underline"
                    onClick={async () => {
                      const tpl = await adminApi.downloadOrgTemplate();
                      alert(`模板示例：\n${tpl.data?.csvHeader}\n${tpl.data?.example}`);
                    }}
                    type="button"
                  >
                    查看模板说明
                  </button>
                </div>
                {orgImportResult && (
                  <div className="text-xs text-slate-600">
                    <div>成功 {orgImportResult.successCount} 条，失败 {orgImportResult.failCount} 条</div>
                    {orgImportResult.errors && orgImportResult.errors.length > 0 && (
                      <div className="mt-1 text-red-500">
                        {orgImportResult.errors.slice(0, 5).map((err, idx) => (
                          <div key={idx}>{err}</div>
                        ))}
                        {orgImportResult.errors.length > 5 && <div>...更多错误请查看日志</div>}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">创建用户</h3>
              <p className="text-xs text-slate-500">填写基础信息并选择角色。</p>
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

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">用户管理</h3>
              <p className="text-xs text-slate-500">分页列表、状态切换、重置密码。</p>
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

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">角色管理</h3>
              <p className="text-xs text-slate-500">新建/编辑角色，分配权限。</p>
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
              <div className="space-y-3 max-h-60 overflow-y-auto border border-slate-200 rounded p-2">
                {(Object.entries(
                  permissionOptions.reduce((acc, curr) => {
                    const g = curr.group || '其他';
                    if (!acc[g]) acc[g] = [];
                    acc[g].push(curr);
                    return acc;
                  }, {} as Record<string, typeof permissionOptions>)
                ) as any[]).map(([group, perms]) => (
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

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">用户管理</h3>
              <p className="text-xs text-slate-500">分页列表、状态切换、重置密码。</p>
            </div>
            <button
              onClick={loadUsers}
              className="inline-flex items-center px-3 py-1.5 text-sm rounded-lg border border-slate-200 hover:bg-slate-50"
            >
              <RefreshCw size={14} className="mr-2" /> 刷新
            </button>
          </div>
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
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <h3 className="text-lg font-bold text-slate-800 mb-4 border-b border-slate-100 pb-2">安全与合规</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-800">开启四性检测强控</p>
                <p className="text-xs text-slate-500">归档前必须通过四性检测，否则无法入库</p>
              </div>
              <div className="relative inline-block w-10 mr-2 align-middle select-none transition duration-200 ease-in">
                <input
                  type="checkbox"
                  name="toggle"
                  id="toggle"
                  className="toggle-checkbox absolute block w-5 h-5 rounded-full bg-white border-4 appearance-none cursor-pointer checked:right-0 checked:border-primary-500"
                />
                <label
                  htmlFor="toggle"
                  className="toggle-label block overflow-hidden h-5 rounded-full bg-gray-300 cursor-pointer checked:bg-primary-500"
                ></label>
              </div>
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-800">水印强制开启</p>
                <p className="text-xs text-slate-500">所有借阅预览必须强制添加动态水印</p>
              </div>
              <div className="relative inline-block w-10 mr-2 align-middle select-none transition duration-200 ease-in">
                <input
                  type="checkbox"
                  checked
                  readOnly
                  className="toggle-checkbox absolute block w-5 h-5 rounded-full bg-white border-4 appearance-none cursor-pointer right-0 border-primary-500"
                />
                <label className="toggle-label block overflow-hidden h-5 rounded-full bg-primary-500 cursor-pointer"></label>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <AuditLogView />
        </div>
      </div>
    </div>
  );
};

export default SettingsView;
