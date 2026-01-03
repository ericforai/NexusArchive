// Input: React、lucide-react、entityApi、fondsApi
// Output: EntityManagementPage 组件
// Pos: 法人管理页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { Plus, Edit3, Trash2, Loader2, Building2, CheckCircle2, XCircle, Users } from 'lucide-react';
import { entityApi, SysEntity } from '../../api/entity';
import { fondsApi, BasFonds } from '../../api/fonds';

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

export const EntityManagementPage: React.FC = () => {
    const [entityList, setEntityList] = useState<SysEntity[]>([]);
    const [fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [selectedEntity, setSelectedEntity] = useState<SysEntity | null>(null);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

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

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const [entityRes, fondsRes] = await Promise.all([
                entityApi.list(),
                fondsApi.list(),
            ]);
            if (entityRes.code === 200 && entityRes.data) {
                setEntityList(entityRes.data);
            }
            if (fondsRes.code === 200 && fondsRes.data) {
                setFondsList(fondsRes.data);
            }
        } catch (error) {
            console.error('加载数据失败', error);
            setMessage({ type: 'error', text: '加载数据失败' });
        } finally {
            setLoading(false);
        }
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
        // 检查是否可以删除
        try {
            const canDeleteRes = await entityApi.canDelete(id);
            if (canDeleteRes.code === 200 && !canDeleteRes.data) {
                setMessage({ type: 'error', text: '该法人下存在关联全宗，无法删除' });
                return;
            }
        } catch (error) {
            console.error('检查删除权限失败', error);
        }

        if (!window.confirm('确定删除该法人吗？删除后关联的全宗将失去法人引用。')) return;
        
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

    const getEntityFondsCount = (entityId: string) => {
        return fondsList.filter(f => f.entityId === entityId).length;
    };

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Building2 className="w-6 h-6 text-primary-600" />
                            <h1 className="text-xl font-semibold text-slate-900">法人管理</h1>
                        </div>
                        <button
                            onClick={() => {
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
                                setShowModal(true);
                            }}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                        >
                            <Plus className="w-4 h-4" />
                            新建法人
                        </button>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">管理法人实体，一个法人可以对应多个全宗</p>
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

                {/* Entity List */}
                <div className="flex-1 overflow-y-auto p-6">
                    {loading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                        </div>
                    ) : entityList.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                            <Building2 className="w-12 h-12 mb-2" />
                            <p>暂无法人数据</p>
                            <button
                                onClick={() => {
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
                                    setShowModal(true);
                                }}
                                className="mt-4 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                            >
                                创建第一个法人
                            </button>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {entityList.map((entity) => {
                                const fondsCount = getEntityFondsCount(entity.id);
                                return (
                                    <div
                                        key={entity.id}
                                        className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                                    >
                                        <div className="flex items-start justify-between mb-3">
                                            <div className="flex-1">
                                                <h3 className="text-lg font-semibold text-slate-900 mb-1">
                                                    {entity.name}
                                                </h3>
                                                {entity.taxId && (
                                                    <p className="text-sm text-slate-500 font-mono">
                                                        税号: {entity.taxId}
                                                    </p>
                                                )}
                                            </div>
                                            <span className={`px-2 py-1 rounded text-xs font-medium ${
                                                entity.status === 'ACTIVE'
                                                    ? 'bg-green-100 text-green-700'
                                                    : 'bg-slate-100 text-slate-700'
                                            }`}>
                                                {entity.status === 'ACTIVE' ? '活跃' : '停用'}
                                            </span>
                                        </div>

                                        <div className="space-y-2 mb-3">
                                            {entity.address && (
                                                <p className="text-sm text-slate-600">
                                                    <span className="text-slate-500">地址:</span> {entity.address}
                                                </p>
                                            )}
                                            {entity.contactPerson && (
                                                <p className="text-sm text-slate-600">
                                                    <span className="text-slate-500">联系人:</span> {entity.contactPerson}
                                                </p>
                                            )}
                                            {entity.contactPhone && (
                                                <p className="text-sm text-slate-600">
                                                    <span className="text-slate-500">电话:</span> {entity.contactPhone}
                                                </p>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-2 mb-3 p-2 bg-slate-50 rounded">
                                            <Users className="w-4 h-4 text-slate-400" />
                                            <span className="text-sm text-slate-600">
                                                关联全宗: <span className="font-semibold">{fondsCount}</span> 个
                                            </span>
                                        </div>

                                        {entity.description && (
                                            <p className="text-sm text-slate-500 mb-3 line-clamp-2">
                                                {entity.description}
                                            </p>
                                        )}

                                        <div className="flex gap-2">
                                            <button
                                                onClick={() => handleEdit(entity)}
                                                className="flex-1 px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50 rounded-lg flex items-center justify-center gap-1"
                                            >
                                                <Edit3 className="w-4 h-4" />
                                                编辑
                                            </button>
                                            <button
                                                onClick={() => handleDelete(entity.id)}
                                                className="flex-1 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded-lg flex items-center justify-center gap-1"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                                删除
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
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


