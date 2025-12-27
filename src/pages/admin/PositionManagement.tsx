// Input: React、lucide-react 图标、本地模块 api/admin
// Output: React 组件 PositionManagement
// Pos: src/pages/admin/PositionManagement.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin';
import { AlertTriangle, Loader2, Plus, Trash2, Edit3 } from 'lucide-react';

interface Position {
  id: string;
  name: string;
  code: string;
  departmentId?: string;
  description?: string;
  status?: string;
  createdAt?: string;
}

export const PositionManagement: React.FC = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Position | null>(null);
  const [form, setForm] = useState<Partial<Position>>({
    name: '',
    code: '',
    departmentId: '',
    description: '',
    status: 'active'
  });

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminApi.getPositions({ page: 1, limit: 50 });
      if (res.code === 200 && res.data) {
        setPositions((res.data as any).records || []);
      } else {
        setError(res.message || '加载岗位数据失败');
      }
    } catch {
      setError('加载岗位数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ name: '', code: '', departmentId: '', description: '', status: 'active' });
    setModalOpen(true);
  };

  const openEdit = (p: Position) => {
    setEditing(p);
    setForm(p);
    setModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.code) {
      setError('名称和编码必填');
      return;
    }
    try {
      if (editing) {
        await adminApi.updatePosition({ ...form, id: editing.id });
      } else {
        await adminApi.createPosition(form);
      }
      setModalOpen(false);
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('确定删除该岗位？')) return;
    try {
      await adminApi.deletePosition(id);
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.message || '删除失败');
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-xl font-bold text-slate-800">岗位管理</h3>
          <p className="text-sm text-slate-500">维护部门岗位，辅助角色与人员管理</p>
        </div>
        <button
          onClick={openCreate}
          className="px-3 py-2 bg-primary-600 text-white rounded-lg text-sm font-semibold hover:bg-primary-700 flex items-center gap-2"
        >
          <Plus size={16} /> 新建岗位
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-2 rounded-lg text-sm flex items-center gap-2">
          <AlertTriangle size={16} /> {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center gap-2 text-slate-500 text-sm">
          <Loader2 className="animate-spin" size={16} /> 加载中...
        </div>
      ) : positions.length === 0 ? (
        <div className="text-slate-400 text-sm">暂无岗位，请新建。</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm text-slate-700">
            <thead className="bg-slate-50 text-xs text-slate-500 uppercase">
              <tr>
                <th className="px-3 py-2 text-left">名称</th>
                <th className="px-3 py-2 text-left">编码</th>
                <th className="px-3 py-2 text-left">所属部门</th>
                <th className="px-3 py-2 text-left">状态</th>
                <th className="px-3 py-2 text-left">描述</th>
                <th className="px-3 py-2 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {positions.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50">
                  <td className="px-3 py-2 font-medium text-slate-800">{p.name}</td>
                  <td className="px-3 py-2 font-mono text-slate-600">{p.code}</td>
                  <td className="px-3 py-2 text-slate-600">{p.departmentId || '-'}</td>
                  <td className="px-3 py-2">
                    <span className={`px-2 py-1 text-xs rounded-full border ${p.status === 'disabled'
                      ? 'bg-slate-100 text-slate-500 border-slate-200'
                      : 'bg-emerald-50 text-emerald-700 border-emerald-100'}`}>
                      {p.status === 'disabled' ? '禁用' : '启用'}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-slate-500">{p.description || '-'}</td>
                  <td className="px-3 py-2 text-right space-x-2">
                    <button onClick={() => openEdit(p)} className="text-primary-600 hover:text-primary-700 text-xs inline-flex items-center gap-1">
                      <Edit3 size={14} /> 编辑
                    </button>
                    <button onClick={() => handleDelete(p.id)} className="text-rose-600 hover:text-rose-700 text-xs inline-flex items-center gap-1">
                      <Trash2 size={14} /> 删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-100">
            <div className="p-4 border-b border-slate-100 flex justify-between items-center">
              <h4 className="text-lg font-bold text-slate-800">{editing ? '编辑岗位' : '新建岗位'}</h4>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-slate-600">✕</button>
            </div>
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700">名称</label>
                <input
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  value={form.name || ''}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">编码</label>
                <input
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  value={form.code || ''}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  required
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">所属部门 ID</label>
                <input
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  value={form.departmentId || ''}
                  onChange={(e) => setForm({ ...form, departmentId: e.target.value })}
                  placeholder="可选"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">描述</label>
                <textarea
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  value={form.description || ''}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={2}
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">状态</label>
                <select
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  value={form.status || 'active'}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="active">启用</option>
                  <option value="disabled">禁用</option>
                </select>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg">{editing ? '保存' : '创建'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
