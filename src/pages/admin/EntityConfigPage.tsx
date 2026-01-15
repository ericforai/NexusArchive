// Input: React、lucide-react、entityConfigApi、entityApi
// Output: EntityConfigPage 组件
// Pos: 法人配置管理页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Settings, Save, Trash2, Plus, Loader2, CheckCircle2, XCircle, Building2 } from 'lucide-react';
import { entityConfigApi, EntityConfig } from '../../api/entityConfig';
import { entityApi, SysEntity } from '../../api/entity';

type ConfigType = 'ERP_INTEGRATION' | 'BUSINESS_RULE' | 'COMPLIANCE_POLICY';

const CONFIG_TYPE_LABELS: Record<ConfigType, string> = {
    ERP_INTEGRATION: 'ERP 集成配置',
    BUSINESS_RULE: '业务规则配置',
    COMPLIANCE_POLICY: '合规策略配置',
};

export const EntityConfigPage: React.FC = () => {
    const [entityList, setEntityList] = useState<SysEntity[]>([]);
    const [selectedEntityId, setSelectedEntityId] = useState<string>('');
    const [configs, setConfigs] = useState<Record<string, EntityConfig[]>>({});
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [editingConfig, setEditingConfig] = useState<EntityConfig | null>(null);
    const [activeTab, setActiveTab] = useState<ConfigType>('ERP_INTEGRATION');
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    const [form, setForm] = useState<EntityConfig>({
        entityId: '',
        configType: 'ERP_INTEGRATION',
        configKey: '',
        configValue: '',
        description: '',
    });

    useEffect(() => {
        loadEntityList();
    }, []);

    useEffect(() => {
        if (selectedEntityId) {
            loadConfigs(selectedEntityId);
        }
    }, [selectedEntityId]);

    const loadEntityList = async () => {
        try {
            const res = await entityApi.listActive();
            if (res.code === 200 && res.data) {
                setEntityList(res.data);
                if (res.data.length > 0 && !selectedEntityId) {
                    setSelectedEntityId(res.data[0].id);
                }
            }
        } catch (error) {
            console.error('加载法人列表失败', error);
        }
    };

    const loadConfigs = async (entityId: string) => {
        setLoading(true);
        try {
            const res = await entityConfigApi.getConfigsGroupedByType(entityId);
            if (res.code === 200 && res.data) {
                setConfigs(res.data);
            } else {
                setConfigs({});
            }
        } catch (error) {
            console.error('加载配置失败', error);
            setConfigs({});
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = (config: EntityConfig) => {
        setEditingConfig(config);
        setForm({
            entityId: config.entityId,
            configType: config.configType,
            configKey: config.configKey,
            configValue: config.configValue || '',
            description: config.description || '',
        });
        setShowModal(true);
    };

    const handleDelete = async (config: EntityConfig) => {
        if (!window.confirm('确定删除该配置吗？')) return;

        setLoading(true);
        try {
            const res = await entityConfigApi.deleteByEntityIdAndType(
                config.entityId,
                config.configType
            );
            if (res.code === 200) {
                setMessage({ type: 'success', text: '删除成功' });
                loadConfigs(selectedEntityId);
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
        if (!form.entityId || !form.configType || !form.configKey) {
            setMessage({ type: 'error', text: '请填写完整信息' });
            return;
        }

        setSaving(true);
        try {
            const res = await entityConfigApi.saveOrUpdate(form);
            if (res.code === 200) {
                setMessage({ type: 'success', text: '保存成功' });
                setShowModal(false);
                setForm({
                    entityId: selectedEntityId,
                    configType: activeTab,
                    configKey: '',
                    configValue: '',
                    description: '',
                });
                setEditingConfig(null);
                loadConfigs(selectedEntityId);
            } else {
                setMessage({ type: 'error', text: res.message || '保存失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '保存失败' });
        } finally {
            setSaving(false);
        }
    };

    const currentConfigs = configs[activeTab] || [];

    return (
        <div className="h-full flex flex-col bg-slate-50 p-6">
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-200">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Settings className="w-6 h-6 text-primary-600" />
                            <h1 className="text-xl font-semibold text-slate-900">法人配置管理</h1>
                        </div>
                        <div className="flex items-center gap-4">
                            <select
                                value={selectedEntityId}
                                onChange={(e) => {
                                    setSelectedEntityId(e.target.value);
                                    setForm({ ...form, entityId: e.target.value });
                                }}
                                className="px-4 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                            >
                                <option value="">-- 选择法人 --</option>
                                {entityList.map((entity) => (
                                    <option key={entity.id} value={entity.id}>
                                        {entity.name}
                                    </option>
                                ))}
                            </select>
                            {selectedEntityId && (
                                <button
                                    onClick={() => {
                                        setForm({
                                            entityId: selectedEntityId,
                                            configType: activeTab,
                                            configKey: '',
                                            configValue: '',
                                            description: '',
                                        });
                                        setEditingConfig(null);
                                        setShowModal(true);
                                    }}
                                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 flex items-center gap-2"
                                >
                                    <Plus className="w-4 h-4" />
                                    新建配置
                                </button>
                            )}
                        </div>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">为每个法人设置独立的配置（ERP接口、业务规则、合规策略等）</p>
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

                {!selectedEntityId ? (
                    <div className="flex-1 flex items-center justify-center">
                        <div className="text-center text-slate-400">
                            <Building2 className="w-12 h-12 mx-auto mb-2" />
                            <p>请先选择法人</p>
                        </div>
                    </div>
                ) : (
                    <>
                        {/* Tabs */}
                        <div className="px-6 pt-4 border-b border-slate-200">
                            <div className="flex gap-2">
                                {(Object.keys(CONFIG_TYPE_LABELS) as ConfigType[]).map((type) => (
                                    <button
                                        key={type}
                                        onClick={() => {
                                            setActiveTab(type);
                                            setForm({ ...form, configType: type });
                                        }}
                                        className={`px-4 py-2 rounded-t-lg text-sm font-medium transition-colors ${activeTab === type
                                            ? 'bg-primary-50 text-primary-700 border-b-2 border-primary-600'
                                            : 'text-slate-600 hover:text-slate-900'
                                            }`}
                                    >
                                        {CONFIG_TYPE_LABELS[type]}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Config List */}
                        <div className="flex-1 overflow-y-auto p-6">
                            {loading ? (
                                <div className="flex items-center justify-center h-64">
                                    <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                                </div>
                            ) : currentConfigs.length === 0 ? (
                                <div className="flex flex-col items-center justify-center h-64 text-slate-400">
                                    <Settings className="w-12 h-12 mb-2" />
                                    <p>暂无配置，点击&quot;新建配置&quot;添加</p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {currentConfigs.map((config) => (
                                        <div
                                            key={config.id}
                                            className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                                        >
                                            <div className="flex items-start justify-between mb-3">
                                                <div className="flex-1">
                                                    <h3 className="text-lg font-semibold text-slate-900 mb-1">
                                                        {config.configKey}
                                                    </h3>
                                                    {config.description && (
                                                        <p className="text-sm text-slate-500">
                                                            {config.description}
                                                        </p>
                                                    )}
                                                </div>
                                                <div className="flex gap-2">
                                                    <button
                                                        onClick={() => handleEdit(config)}
                                                        className="px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50 rounded-lg"
                                                    >
                                                        编辑
                                                    </button>
                                                    <button
                                                        onClick={() => handleDelete(config)}
                                                        className="px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded-lg"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </div>
                                            {config.configValue && (
                                                <div className="mt-3">
                                                    <p className="text-xs text-slate-500 mb-1">配置值:</p>
                                                    <pre className="p-3 bg-slate-50 rounded text-xs overflow-x-auto">
                                                        {config.configValue}
                                                    </pre>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
                            <h2 className="text-lg font-semibold">
                                {editingConfig ? '编辑配置' : '新建配置'}
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
                                    法人 <span className="text-slate-400">(只读)</span>
                                </label>
                                <input
                                    type="text"
                                    value={entityList.find(e => e.id === form.entityId)?.name || ''}
                                    disabled
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    配置类型 <span className="text-red-500">*</span>
                                </label>
                                <select
                                    value={form.configType}
                                    onChange={(e) => setForm({ ...form, configType: e.target.value as ConfigType })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    required
                                >
                                    {Object.entries(CONFIG_TYPE_LABELS).map(([value, label]) => (
                                        <option key={value} value={value}>
                                            {label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    配置键 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    value={form.configKey}
                                    onChange={(e) => setForm({ ...form, configKey: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    placeholder="例如: ERP_API_URL, ERP_API_KEY"
                                    required
                                    disabled={!!editingConfig}
                                />
                                <p className="text-xs text-slate-400 mt-1">配置键用于标识配置项，创建后不可修改</p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    配置值 <span className="text-slate-400">(JSON格式)</span>
                                </label>
                                <textarea
                                    value={form.configValue}
                                    onChange={(e) => setForm({ ...form, configValue: e.target.value })}
                                    rows={8}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 font-mono text-sm"
                                    placeholder='例如: {"apiUrl": "https://erp.example.com/api", "apiKey": "xxx"}'
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    描述
                                </label>
                                <textarea
                                    value={form.description}
                                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                                    rows={2}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                                />
                            </div>

                            <div className="flex gap-3 pt-4">
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                                    保存
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

export default EntityConfigPage;






