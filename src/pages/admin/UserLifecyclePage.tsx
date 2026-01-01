// Input: React、lucide-react、userLifecycleApi
// Output: UserLifecyclePage 组件
// Pos: 用户生命周期管理页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Users, Loader2, CheckCircle2, XCircle, UserPlus, UserMinus, UserCog, AlertCircle } from 'lucide-react';
import { userLifecycleApi, OnboardRequest, OffboardRequest, TransferRequest } from '../../api/userLifecycle';
import { OrgSelector } from '../../components/org/OrgSelector';
import { OrgNode } from '../../types';
import { useAdminSettingsApi } from '../../features/settings';

/**
 * 用户生命周期管理页面
 * 
 * 功能：
 * 1. 入职触发：选择员工、自动创建账号、分配角色
 * 2. 离职触发：选择员工、停用账号、回收权限
 * 3. 调岗触发：选择员工、调整角色和权限
 * 
 * PRD 来源: Section 4.2 - 用户生命周期管理
 */
export const UserLifecyclePage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();
    const [activeTab, setActiveTab] = useState<'onboard' | 'offboard' | 'transfer'>('onboard');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    
    // 组织架构数据
    const [orgTree, setOrgTree] = useState<OrgNode[]>([]);
    const [orgLoading, setOrgLoading] = useState(false);

    // 入职表单
    const [onboardForm, setOnboardForm] = useState<OnboardRequest>({
        employeeId: '',
        roleIds: [],
        organizationId: '', // 新增：组织ID
    });

    // 离职表单
    const [offboardForm, setOffboardForm] = useState<OffboardRequest>({
        employeeId: '',
        reason: '',
    });

    // 调岗表单
    const [transferForm, setTransferForm] = useState<TransferRequest>({
        employeeId: '',
        newRoleIds: [],
        reason: '',
        toOrganizationId: '', // 新增：目标组织ID
    });

    // 加载组织架构数据
    useEffect(() => {
        const loadOrgTree = async () => {
            setOrgLoading(true);
            try {
                const res = await adminApi.getOrgTree();
                if (res.code === 200 && res.data) {
                    setOrgTree(res.data);
                }
            } catch (error) {
                console.error('加载组织架构失败', error);
            } finally {
                setOrgLoading(false);
            }
        };
        loadOrgTree();
    }, [adminApi]);

    // 注意：这里需要集成员工选择器和角色选择器
    // 由于没有现成的API，这里使用模拟数据
    const mockEmployees = [
        { id: '1', name: '张三' },
        { id: '2', name: '李四' },
        { id: '3', name: '王五' },
    ];

    const mockRoles = [
        { id: '1', name: '档案管理员' },
        { id: '2', name: '财务人员' },
        { id: '3', name: '审计人员' },
    ];

    const handleOnboard = async () => {
        if (!onboardForm.employeeId || onboardForm.roleIds.length === 0) {
            setMessage({ type: 'error', text: '请选择员工和至少一个角色' });
            return;
        }
        
        if (!onboardForm.organizationId) {
            setMessage({ type: 'error', text: '请选择目标组织' });
            return;
        }

        setLoading(true);
        try {
            const res = await userLifecycleApi.onboard(onboardForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: `账号创建成功：${res.data?.username || ''}` });
                setOnboardForm({ employeeId: '', roleIds: [], organizationId: '' });
            } else {
                setMessage({ type: 'error', text: res.message || '入职处理失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '入职处理失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleOffboard = async () => {
        if (!offboardForm.employeeId) {
            setMessage({ type: 'error', text: '请选择员工' });
            return;
        }

        if (!window.confirm('确认执行离职处理吗？账号将被停用，权限将被回收。')) {
            return;
        }

        setLoading(true);
        try {
            const res = await userLifecycleApi.offboard(offboardForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: '离职处理成功' });
                setOffboardForm({ employeeId: '', reason: '' });
            } else {
                setMessage({ type: 'error', text: res.message || '离职处理失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '离职处理失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleTransfer = async () => {
        if (!transferForm.employeeId || transferForm.newRoleIds.length === 0) {
            setMessage({ type: 'error', text: '请选择员工和至少一个角色' });
            return;
        }
        
        if (!transferForm.toOrganizationId) {
            setMessage({ type: 'error', text: '请选择目标组织' });
            return;
        }

        setLoading(true);
        try {
            const res = await userLifecycleApi.transfer(transferForm);
            if (res.code === 200) {
                setMessage({ type: 'success', text: '调岗处理成功' });
                setTransferForm({ employeeId: '', newRoleIds: [], reason: '', toOrganizationId: '' });
            } else {
                setMessage({ type: 'error', text: res.message || '调岗处理失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '调岗处理失败' });
        } finally {
            setLoading(false);
        }
    };

    const toggleRole = (roleId: string, roleIds: string[], setRoleIds: (ids: string[]) => void) => {
        if (roleIds.includes(roleId)) {
            setRoleIds(roleIds.filter(id => id !== roleId));
        } else {
            setRoleIds([...roleIds, roleId]);
        }
    };

    return (
        <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Users className="mr-2" size={28} />
                        用户生命周期管理
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">管理用户入职、离职和调岗流程</p>
                </div>
            </div>

            {/* 消息提示 */}
            {message && (
                <div className={`p-4 rounded-lg flex items-center gap-2 ${
                    message.type === 'success' 
                        ? 'bg-green-50 text-green-800 border border-green-200' 
                        : 'bg-red-50 text-red-800 border border-red-200'
                }`}>
                    {message.type === 'success' ? (
                        <CheckCircle2 size={20} />
                    ) : (
                        <XCircle size={20} />
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

            {/* 标签页 */}
            <div className="bg-white border border-slate-200 rounded-lg">
                <div className="flex border-b border-slate-200">
                    <button
                        onClick={() => setActiveTab('onboard')}
                        className={`px-6 py-3 font-medium ${
                            activeTab === 'onboard'
                                ? 'text-primary-600 border-b-2 border-primary-600'
                                : 'text-slate-600 hover:text-slate-900'
                        }`}
                    >
                        <UserPlus className="inline mr-2" size={18} />
                        入职
                    </button>
                    <button
                        onClick={() => setActiveTab('offboard')}
                        className={`px-6 py-3 font-medium ${
                            activeTab === 'offboard'
                                ? 'text-primary-600 border-b-2 border-primary-600'
                                : 'text-slate-600 hover:text-slate-900'
                        }`}
                    >
                        <UserMinus className="inline mr-2" size={18} />
                        离职
                    </button>
                    <button
                        onClick={() => setActiveTab('transfer')}
                        className={`px-6 py-3 font-medium ${
                            activeTab === 'transfer'
                                ? 'text-primary-600 border-b-2 border-primary-600'
                                : 'text-slate-600 hover:text-slate-900'
                        }`}
                    >
                        <UserCog className="inline mr-2" size={18} />
                        调岗
                    </button>
                </div>

                <div className="p-6">
                    {/* 入职表单 */}
                    {activeTab === 'onboard' && (
                        <div className="space-y-6 max-w-2xl">
                            <div>
                                <h3 className="text-lg font-semibold mb-4">员工入职</h3>
                                <p className="text-sm text-slate-600 mb-4">
                                    选择员工后，系统将自动创建用户账号并分配指定角色
                                </p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    选择员工 <span className="text-red-500">*</span>
                                </label>
                                <select
                                    value={onboardForm.employeeId}
                                    onChange={(e) => setOnboardForm({ ...onboardForm, employeeId: e.target.value })}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                >
                                    <option value="">请选择员工</option>
                                    {mockEmployees.map(emp => (
                                        <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    选择组织 <span className="text-red-500">*</span>
                                </label>
                                {orgLoading ? (
                                    <div className="flex items-center justify-center py-8 text-slate-500">
                                        <Loader2 className="animate-spin mr-2" size={16} />
                                        加载组织架构...
                                    </div>
                                ) : (
                                    <OrgSelector
                                        orgTree={orgTree}
                                        value={onboardForm.organizationId}
                                        onChange={(value) => setOnboardForm({ ...onboardForm, organizationId: value as string })}
                                        multiple={false}
                                        placeholder="请选择目标组织"
                                        className="mb-4"
                                    />
                                )}
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    分配角色 <span className="text-red-500">*</span>
                                </label>
                                <div className="space-y-2">
                                    {mockRoles.map(role => (
                                        <label key={role.id} className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={onboardForm.roleIds.includes(role.id)}
                                                onChange={() => toggleRole(role.id, onboardForm.roleIds, (ids) => setOnboardForm({ ...onboardForm, roleIds: ids }))}
                                                className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                                            />
                                            <span>{role.name}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <button
                                onClick={handleOnboard}
                                disabled={loading}
                                className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {loading ? <Loader2 className="animate-spin" size={16} /> : <UserPlus size={16} />}
                                执行入职
                            </button>
                        </div>
                    )}

                    {/* 离职表单 */}
                    {activeTab === 'offboard' && (
                        <div className="space-y-6 max-w-2xl">
                            <div>
                                <h3 className="text-lg font-semibold mb-4">员工离职</h3>
                                <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg flex items-start gap-3 mb-4">
                                    <AlertCircle className="text-yellow-600 flex-shrink-0 mt-0.5" size={20} />
                                    <div className="text-sm text-yellow-800">
                                        <div className="font-semibold mb-1">注意</div>
                                        <div>执行离职后，用户账号将被停用，所有权限将被回收，此操作不可逆。</div>
                                    </div>
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    选择员工 <span className="text-red-500">*</span>
                                </label>
                                <select
                                    value={offboardForm.employeeId}
                                    onChange={(e) => setOffboardForm({ ...offboardForm, employeeId: e.target.value })}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                >
                                    <option value="">请选择员工</option>
                                    {mockEmployees.map(emp => (
                                        <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    离职原因
                                </label>
                                <textarea
                                    value={offboardForm.reason}
                                    onChange={(e) => setOffboardForm({ ...offboardForm, reason: e.target.value })}
                                    rows={4}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                    placeholder="请输入离职原因..."
                                />
                            </div>
                            <button
                                onClick={handleOffboard}
                                disabled={loading}
                                className="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {loading ? <Loader2 className="animate-spin" size={16} /> : <UserMinus size={16} />}
                                执行离职
                            </button>
                        </div>
                    )}

                    {/* 调岗表单 */}
                    {activeTab === 'transfer' && (
                        <div className="space-y-6 max-w-2xl">
                            <div>
                                <h3 className="text-lg font-semibold mb-4">员工调岗</h3>
                                <p className="text-sm text-slate-600 mb-4">
                                    调整员工角色和权限，旧角色将被替换为新角色
                                </p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    选择员工 <span className="text-red-500">*</span>
                                </label>
                                <select
                                    value={transferForm.employeeId}
                                    onChange={(e) => setTransferForm({ ...transferForm, employeeId: e.target.value })}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                >
                                    <option value="">请选择员工</option>
                                    {mockEmployees.map(emp => (
                                        <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    选择目标组织 <span className="text-red-500">*</span>
                                </label>
                                {orgLoading ? (
                                    <div className="flex items-center justify-center py-8 text-slate-500">
                                        <Loader2 className="animate-spin mr-2" size={16} />
                                        加载组织架构...
                                    </div>
                                ) : (
                                    <OrgSelector
                                        orgTree={orgTree}
                                        value={transferForm.toOrganizationId}
                                        onChange={(value) => setTransferForm({ ...transferForm, toOrganizationId: value as string })}
                                        multiple={false}
                                        placeholder="请选择目标组织"
                                        className="mb-4"
                                    />
                                )}
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    新角色 <span className="text-red-500">*</span>
                                </label>
                                <div className="space-y-2">
                                    {mockRoles.map(role => (
                                        <label key={role.id} className="flex items-center gap-2 p-3 border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={transferForm.newRoleIds.includes(role.id)}
                                                onChange={() => toggleRole(role.id, transferForm.newRoleIds, (ids) => setTransferForm({ ...transferForm, newRoleIds: ids }))}
                                                className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                                            />
                                            <span>{role.name}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    调岗原因
                                </label>
                                <textarea
                                    value={transferForm.reason}
                                    onChange={(e) => setTransferForm({ ...transferForm, reason: e.target.value })}
                                    rows={4}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                    placeholder="请输入调岗原因..."
                                />
                            </div>
                            <button
                                onClick={handleTransfer}
                                disabled={loading}
                                className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {loading ? <Loader2 className="animate-spin" size={16} /> : <UserCog size={16} />}
                                执行调岗
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserLifecyclePage;

