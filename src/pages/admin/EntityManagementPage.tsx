// Input: React、lucide-react、entityApi、fondsApi、adminApi
// Output: EntityManagementPage 组件
// Pos: 法人管理页面（树形视图、CRUD 操作、ERP 同步）

import React, { useEffect, useState } from 'react';
import { Plus, Edit3, Trash2, Loader2, Building2, CheckCircle2, XCircle, ChevronRight, ChevronDown, RefreshCw } from 'lucide-react';
import { entityApi, SysEntity, EntityTreeNode } from '../../api/entity';
import { fondsApi, BasFonds } from '../../api/fonds';
import { adminApi } from '../../api/admin';
import { toast } from '../../utils/notificationService';

interface EntityForm {
    id?: string;
    name: string;
    taxId: string;
    address: string;
    contactPerson: string;
    contactPhone: string;
    contactEmail: string;
    status: 'ACTIVE' | 'INACTIVE';
    description: string;
}

/**
 * 递归渲染法人树节点
 */
const EntityTreeNodeComponent: React.FC<{
    node: EntityTreeNode;
    level: number;
    expandedIds: Set<string>;
    onToggle: (id: string) => void;
    onEdit: (entity: SysEntity) => void;
    onDelete: (id: string) => void;
}> = ({ node, level, expandedIds, onToggle, onEdit, onDelete }) => {
    const hasChildren = node.children && node.children.length > 0;
    const paddingLeft = 16 + level * 24;

    return (
        <div>
            {/* 节点行 */}
            <div
                className="flex items-center py-3 px-4 hover:bg-slate-50 transition-colors border-b border-slate-100"
                style={{ paddingLeft: `${paddingLeft}px` }}
            >
                {/* 展开/收起图标 */}
                <span
                    className={`w-5 h-5 flex items-center justify-center cursor-pointer text-slate-400 ${hasChildren ? '' : 'invisible'
                        }`}
                    onClick={() => onToggle(node.id)}
                >
                    {expandedIds.has(node.id) ? (
                        <ChevronDown className="w-4 h-4" />
                    ) : (
                        <ChevronRight className="w-4 h-4" />
                    )}
                </span>

                {/* 法人信息 */}
                <div className="flex-1 flex items-center gap-3">
                    <Building2 className="w-5 h-5 text-primary-600" />
                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <span className="font-semibold text-slate-900">{node.name}</span>
                            {node.taxId && (
                                <span className="text-xs text-slate-500 font-mono">
                                    ({node.taxId})
                                </span>
                            )}
                            <span className={`px-2 py-0.5 rounded text-xs ${node.status === 'ACTIVE'
                                    ? 'bg-green-100 text-green-700'
                                    : 'bg-slate-100 text-slate-700'
                                }`}>
                                {node.status === 'ACTIVE' ? '活跃' : '停用'}
                            </span>
                        </div>
                        <div className="text-xs text-slate-500 mt-1">
                            关联全宗: <span className="font-medium">{node.fondsCount}</span> 个
                        </div>
                    </div>
                </div>

                {/* 操作按钮 */}
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => onEdit({
                            id: node.id,
                            name: node.name,
                            taxId: node.taxId,
                            status: node.status as 'ACTIVE' | 'INACTIVE',
                        })}
                        className="p-2 text-primary-600 hover:bg-primary-50 rounded-lg"
                        title="编辑"
                    >
                        <Edit3 className="w-4 h-4" />
                    </button>
                    <button
                        onClick={() => onDelete(node.id)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                        title="删除"
                    >
                        <Trash2 className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* 子节点 */}
            {expandedIds.has(node.id) && hasChildren && (
                <div>
                    {node.children.map(child => (
                        <EntityTreeNodeComponent
                            key={child.id}
                            node={child}
                            level={level + 1}
                            expandedIds={expandedIds}
                            onToggle={onToggle}
                            onEdit={onEdit}
                            onDelete={onDelete}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export const EntityManagementPage: React.FC = () => {
    const [tree, setTree] = useState<EntityTreeNode[]>([]);
    const [_fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [_selectedEntity, setSelectedEntity] = useState<SysEntity | null>(null);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

    const [form, setForm] = useState<EntityForm>({
        name: '',
        taxId: '',
        address: '',
        contactPerson: '',
        contactPhone: '',
        contactEmail: '',
        status: 'ACTIVE',
        description: '',
    });

    /**
     * 过滤部门数据（前端双重保险）
     * 电子会计档案系统只管理法人实体，不管理部门
     */
    const filterDepartments = (nodes: EntityTreeNode[]): EntityTreeNode[] => {
        return nodes
            .filter(node => {
                // 有税号的，肯定是法人实体
                if (node.taxId) {
                    return true;
                }
                // 名称以"部"结尾且没有税号的，视为部门（过滤）
                if (node.name.endsWith('部')) {
                    return false;
                }
                // 名称包含"部门"的，视为部门（过滤）
                if (node.name.includes('部门')) {
                    return false;
                }
                return true;
            })
            .map(node => ({
                ...node,
                children: node.children ? filterDepartments(node.children) : []
            }));
    };

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const [treeRes, fondsRes] = await Promise.all([
                entityApi.getTree(),
                fondsApi.list(),
            ]);
            if (treeRes.code === 200 && treeRes.data) {
                // 前端过滤：确保不显示部门数据（双重保险）
                const filteredTree = filterDepartments(treeRes.data);
                setTree(filteredTree);
                // 默认展开所有节点
                const allIds = new Set<string>();
                const collectIds = (nodes: EntityTreeNode[]) => {
                    nodes.forEach(node => {
                        allIds.add(node.id);
                        if (node.children?.length) {
                            collectIds(node.children);
                        }
                    });
                };
                collectIds(filteredTree);
                setExpandedIds(allIds);
            }
            if (fondsRes.code === 200 && fondsRes.data) {
                setFondsList(fondsRes.data);
            }
        } catch (error: any) {
            console.error('加载数据失败', error);
            
            // 提取错误信息
            let errorMessage = '加载数据失败';
            
            // 检查是否是 AxiosError 且有响应
            if (error?.response) {
                const status = error.response.status;
                const data = error.response.data;
                
                if (status === 403) {
                    // 403 Forbidden - 权限不足
                    errorMessage = '权限不足：您没有访问法人管理数据的权限。请联系管理员授予 entity:view 或 entity:manage 权限。';
                } else if (status === 401) {
                    // 401 Unauthorized - 未授权
                    errorMessage = '未授权：请重新登录';
                } else if (data?.message) {
                    // 使用后端返回的错误消息
                    errorMessage = `加载数据失败：${data.message}`;
                } else {
                    errorMessage = `加载数据失败：HTTP ${status}`;
                }
            } else if (error?.message) {
                // 网络错误或其他错误
                errorMessage = `加载数据失败：${error.message}`;
            }
            
            setMessage({ type: 'error', text: errorMessage });
        } finally {
            setLoading(false);
        }
    };

    const toggleExpand = (id: string) => {
        const newExpanded = new Set(expandedIds);
        if (newExpanded.has(id)) {
            newExpanded.delete(id);
        } else {
            newExpanded.add(id);
        }
        setExpandedIds(newExpanded);
    };

    const handleEdit = (entity: SysEntity) => {
        setForm({
            id: entity.id,
            name: entity.name,
            taxId: entity.taxId || '',
            address: entity.address || '',
            contactPerson: entity.contactPerson || '',
            contactPhone: entity.contactPhone || '',
            contactEmail: entity.contactEmail || '',
            status: entity.status as 'ACTIVE' | 'INACTIVE',
            description: entity.description || '',
        });
        setSelectedEntity(entity);
        setShowModal(true);
    };

    const handleDelete = async (id: string) => {
        try {
            const canDeleteRes = await entityApi.canDelete(id);
            if (canDeleteRes.code === 200 && !canDeleteRes.data) {
                setMessage({ type: 'error', text: '该法人下存在关联全宗或下级法人，无法删除' });
                return;
            }
        } catch (error) {
            console.error('检查删除权限失败', error);
        }

        if (!window.confirm('确定删除该法人吗？')) return;

        setLoading(true);
        try {
            const res = await entityApi.remove(id);
            if (res.code === 200) {
                setMessage({ type: 'success', text: '删除成功' });
                loadData();
            } else {
                setMessage({ type: 'error', text: res.message || '删除失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '删除失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.name.trim()) {
            setMessage({ type: 'error', text: '法人名称不能为空' });
            return;
        }

        setSaving(true);
        try {
            const res = form.id
                ? await entityApi.update(form)
                : await entityApi.save(form);

            if (res.code === 200) {
                setMessage({ type: 'success', text: form.id ? '更新成功' : '创建成功' });
                setShowModal(false);
                resetForm();
                loadData();
            } else {
                setMessage({ type: 'error', text: res.message || '操作失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '操作失败' });
        } finally {
            setSaving(false);
        }
    };

    const resetForm = () => {
        setForm({
            name: '',
            taxId: '',
            address: '',
            contactPerson: '',
            contactPhone: '',
            contactEmail: '',
            status: 'ACTIVE',
            description: '',
        });
        setSelectedEntity(null);
    };

    const handleSyncFromErp = async () => {
        if (!window.confirm('确认从 YonSuite 同步法人实体吗？此操作将同步法人数据（不包含部门）。')) return;
        setSyncing(true);
        try {
            const res = await adminApi.syncOrgFromErp();
            if (res.code === 200 && res.data) {
                toast.success(res.data.message || '同步成功');
                // 重新加载法人树
                await loadData();
                // 显示详细结果
                if (res.data.errors && res.data.errors.length > 0) {
                    toast.warning(`同步完成，但有 ${res.data.errorCount} 条失败`);
                }
            } else {
                toast.error(res.message || '同步失败');
            }
        } catch (e: any) {
            toast.error(e?.response?.data?.message || '同步失败');
        } finally {
            setSyncing(false);
        }
    };

    const totalCount = tree.length;
    const activeCount = tree.filter(e => e.status === 'ACTIVE').length;

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Building2 className="w-6 h-6 text-primary-600" />
                            <div>
                                <h1 className="text-xl font-semibold text-slate-900">法人管理</h1>
                                <p className="text-sm text-slate-500 mt-1">
                                    管理法人实体及层级关系（{totalCount} 个法人，{activeCount} 个活跃）
                                </p>
                                <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                                    <span>📋</span>
                                    <span>电子会计档案架构：法人 → 全宗 → 档案。法人层级关系仅用于管理维度（母公司-子公司），数据隔离基于全宗号。</span>
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={handleSyncFromErp}
                                disabled={syncing || loading}
                                className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50"
                            >
                                <RefreshCw size={16} className={`mr-2 ${syncing ? 'animate-spin' : ''}`} />
                                {syncing ? '同步中...' : '从 ERP 同步法人'}
                            </button>
                            <button
                                onClick={() => {
                                    resetForm();
                                    setShowModal(true);
                                }}
                                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                            >
                                <Plus className="w-4 h-4" />
                                新建法人
                            </button>
                        </div>
                    </div>
                </div>

                {/* Message */}
                {message && (
                    <div className={`mx-6 mt-4 p-3 rounded-lg flex items-center gap-2 ${message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
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

                {/* Tree */}
                <div className="flex-1 overflow-y-auto">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : tree.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                            <Building2 className="w-12 h-12 mb-2" />
                            <p>暂无法人数据</p>
                            <button
                                onClick={() => {
                                    resetForm();
                                    setShowModal(true);
                                }}
                                className="mt-4 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                            >
                                创建第一个法人
                            </button>
                        </div>
                    ) : (
                        <div className="py-2">
                            {tree.map(node => (
                                <EntityTreeNodeComponent
                                    key={node.id}
                                    node={node}
                                    level={0}
                                    expandedIds={expandedIds}
                                    onToggle={toggleExpand}
                                    onEdit={handleEdit}
                                    onDelete={handleDelete}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">
                                {form.id ? '编辑法人' : '新建法人'}
                            </h2>
                            <button
                                onClick={() => setShowModal(false)}
                                className="text-slate-400 hover:text-slate-600"
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleSubmit} className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    法人名称 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    required
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        统一社会信用代码/税号
                                    </label>
                                    <input
                                        type="text"
                                        value={form.taxId}
                                        onChange={(e) => setForm({ ...form, taxId: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        状态
                                    </label>
                                    <select
                                        value={form.status}
                                        onChange={(e) => setForm({ ...form, status: e.target.value as 'ACTIVE' | 'INACTIVE' })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    >
                                        <option value="ACTIVE">活跃</option>
                                        <option value="INACTIVE">停用</option>
                                    </select>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    注册地址
                                </label>
                                <input
                                    type="text"
                                    value={form.address}
                                    onChange={(e) => setForm({ ...form, address: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                />
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        联系人
                                    </label>
                                    <input
                                        type="text"
                                        value={form.contactPerson}
                                        onChange={(e) => setForm({ ...form, contactPerson: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        联系电话
                                    </label>
                                    <input
                                        type="text"
                                        value={form.contactPhone}
                                        onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">
                                        联系邮箱
                                    </label>
                                    <input
                                        type="email"
                                        value={form.contactEmail}
                                        onChange={(e) => setForm({ ...form, contactEmail: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    描述
                                </label>
                                <textarea
                                    value={form.description}
                                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                                    rows={3}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                />
                            </div>

                            <div className="flex gap-3 pt-4">
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                                    {form.id ? '更新' : '创建'}
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setShowModal(false)}
                                    className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
                                >
                                    取消
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EntityManagementPage;
