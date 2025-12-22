// Input: React、lucide-react 图标、本地模块 api/fonds
// Output: React 组件 FondsManagement
// Pos: 管理后台页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { Plus, Edit3, Trash2, Loader2, Save } from 'lucide-react';
import { fondsApi, BasFonds } from '../../api/fonds';

interface FondsForm {
    id?: string;
    fondsCode: string;
    fondsName: string;
    companyName: string;
    description: string;
}

export const FondsManagement: React.FC = () => {
    const [fondsList, setFondsList] = useState<BasFonds[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [showModal, setShowModal] = useState(false);

    // Form State
    const [form, setForm] = useState<FondsForm>({
        fondsCode: '',
        fondsName: '',
        companyName: '',
        description: ''
    });

    const loadData = async () => {
        setLoading(true);
        try {
            const res = await fondsApi.list();
            if (res.code === 200 && res.data) {
                setFondsList(res.data);
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleEdit = (item: BasFonds) => {
        setForm({
            id: item.id,
            fondsCode: item.fondsCode,
            fondsName: item.fondsName,
            companyName: item.companyName || '',
            description: item.description || ''
        });
        setShowModal(true);
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('确定删除该全宗吗？删除后关联的档案将失去全宗引用。')) return;
        setLoading(true);
        try {
            await fondsApi.remove(id);
            loadData();
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (form.id) {
                await fondsApi.update(form);
            } else {
                await fondsApi.save(form);
            }
            setShowModal(false);
            loadData();
        } catch (error) {
            alert('保存失败，请检查全宗号是否重复');
        } finally {
            setSaving(false);
        }
    };

    const openCreate = () => {
        setForm({ fondsCode: '', fondsName: '', companyName: '', description: '' });
        setShowModal(true);
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h3 className="text-lg font-bold text-slate-800 mb-1">全宗管理 (Fonds)</h3>
                    <p className="text-xs text-slate-500">管理档案系统的顶层全宗实体，符合 DA/T 94 标准。</p>
                </div>
                <button
                    onClick={openCreate}
                    className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700"
                >
                    <Plus size={16} className="mr-2" />
                    新建全宗
                </button>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-500 font-medium">
                        <tr>
                            <th className="px-4 py-3 rounded-l-lg">全宗号</th>
                            <th className="px-4 py-3">全宗名称</th>
                            <th className="px-4 py-3">立档单位</th>
                            <th className="px-4 py-3">描述</th>
                            <th className="px-4 py-3 rounded-r-lg text-right">操作</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {loading ? (
                            <tr>
                                <td colSpan={5} className="py-8 text-center text-slate-500">
                                    <div className="flex items-center justify-center">
                                        <Loader2 size={20} className="animate-spin mr-2" /> 加载中...
                                    </div>
                                </td>
                            </tr>
                        ) : fondsList.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="py-8 text-center text-slate-400">暂无全宗数据，请新建。</td>
                            </tr>
                        ) : (
                            fondsList.map((item) => (
                                <tr key={item.id} className="hover:bg-slate-50 group transition-colors">
                                    <td className="px-4 py-3 font-mono text-primary-600 font-medium">{item.fondsCode}</td>
                                    <td className="px-4 py-3 font-medium text-slate-800">{item.fondsName}</td>
                                    <td className="px-4 py-3 text-slate-600">{item.companyName || '-'}</td>
                                    <td className="px-4 py-3 text-slate-500 truncate max-w-xs">{item.description || '-'}</td>
                                    <td className="px-4 py-3 text-right space-x-2">
                                        <button
                                            onClick={() => handleEdit(item)}
                                            className="p-1 text-slate-400 hover:text-blue-600 transition-colors"
                                            title="编辑"
                                        >
                                            <Edit3 size={16} />
                                        </button>
                                        <button
                                            onClick={() => handleDelete(item.id)}
                                            className="p-1 text-slate-400 hover:text-red-600 transition-colors"
                                            title="删除"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/20 backdrop-blur-sm p-4 animate-in fade-in duration-200">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
                        <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                            <h3 className="font-bold text-slate-800">{form.id ? '编辑全宗' : '新建全宗'}</h3>
                            <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-slate-600">×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">全宗号 *</label>
                                <input
                                    className="w-full border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
                                    placeholder="例如: Z001"
                                    value={form.fondsCode}
                                    onChange={e => setForm({ ...form, fondsCode: e.target.value })}
                                    required
                                />
                                <p className="text-xs text-slate-400 mt-1">全局唯一，用于生成档号前缀。</p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">全宗名称 *</label>
                                <input
                                    className="w-full border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
                                    placeholder="例如: 集团总公司全宗"
                                    value={form.fondsName}
                                    onChange={e => setForm({ ...form, fondsName: e.target.value })}
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">立档单位名称</label>
                                <input
                                    className="w-full border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
                                    placeholder="例如: XX集团有限公司"
                                    value={form.companyName}
                                    onChange={e => setForm({ ...form, companyName: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">描述</label>
                                <textarea
                                    className="w-full border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
                                    rows={3}
                                    value={form.description}
                                    onChange={e => setForm({ ...form, description: e.target.value })}
                                />
                            </div>

                            <div className="pt-2 flex justify-end space-x-3">
                                <button
                                    type="button"
                                    onClick={() => setShowModal(false)}
                                    className="px-4 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50"
                                >
                                    取消
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-70 flex items-center"
                                >
                                    {saving && <Loader2 size={16} className="animate-spin mr-2" />}
                                    保存
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default FondsManagement;
