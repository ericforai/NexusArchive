import React, { useEffect, useState } from 'react';
import { Plus, Loader2, Upload } from 'lucide-react';
import { adminApi } from '../../api/admin';
import { Tree } from '../org/Tree';

/**
 * 组织架构管理页面
 * 
 * 包含组织创建、树结构展示、列表管理、批量导入
 */
export const OrgSettings: React.FC = () => {
    const [orgs, setOrgs] = useState<any[]>([]);
    const [orgTree, setOrgTree] = useState<any[]>([]);
    const [orgLoading, setOrgLoading] = useState(false);
    const [orgForm, setOrgForm] = useState({ name: '', code: '', parentId: '', type: 'DEPARTMENT', orderNum: 0 });
    const [orgImportText, setOrgImportText] = useState('');
    const [orgImportResult, setOrgImportResult] = useState<{ successCount: number; failCount: number; errors?: string[] } | null>(null);

    useEffect(() => {
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
        loadOrgs();
    }, []);

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
        <div className="space-y-6">
            {/* 创建组织 */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 mb-1">组织/部门</h3>
                        <p className="text-xs text-slate-500">创建、查看组织树</p>
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
            </div>

            {/* 组织树和列表 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h4 className="text-sm font-semibold text-slate-800 mb-3">组织树</h4>
                    {orgLoading ? (
                        <div className="text-xs text-slate-500 flex items-center">
                            <Loader2 size={14} className="animate-spin mr-1" /> 加载中...
                        </div>
                    ) : (
                        <Tree data={orgTree} />
                    )}
                </div>

                <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                    <h4 className="text-sm font-semibold text-slate-800 mb-3">组织列表</h4>
                    <div className="max-h-64 overflow-y-auto text-sm">
                        {orgs.map((org) => (
                            <div key={org.id} className="flex items-center justify-between py-2 border-b last:border-0">
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

                    {/* 排序 */}
                    <div className="mt-4 space-y-2">
                        <div className="text-xs text-slate-600">
                            <span className="font-semibold">排序</span>：点击上/下调整排序号
                        </div>
                        <div className="max-h-32 overflow-y-auto text-xs space-y-1">
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
                    </div>

                    {/* 批量导入 */}
                    <div className="mt-4 pt-4 border-t border-slate-100">
                        <h5 className="text-sm font-semibold text-slate-800 mb-2">批量导入 (JSON 数组)</h5>
                        <textarea
                            className="w-full border border-slate-200 rounded p-2 text-xs"
                            rows={3}
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

                    {/* CSV 导入 */}
                    <div className="mt-4 pt-4 border-t border-slate-100">
                        <h5 className="text-sm font-semibold text-slate-800 mb-2">CSV/Excel 导入</h5>
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
                            <div className="text-xs text-slate-600 mt-2">
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
    );
};

export default OrgSettings;
