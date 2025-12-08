import React, { useEffect, useState } from 'react';
import { Save, Loader2 } from 'lucide-react';
import { adminApi } from '../../api/admin';

/**
 * 基础设置页面
 * 
 * 包含系统名称、存储配置、默认保管期限等基础参数
 */
export const BasicSettings: React.FC = () => {
    const [settings, setSettings] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const loadSettings = async () => {
            setLoading(true);
            try {
                const res = await adminApi.getSettings();
                if (res.code === 200 && res.data) {
                    const map: Record<string, string> = {};
                    (res.data as any[]).forEach((item: any) => {
                        map[item.configKey] = item.configValue;
                    });
                    setSettings(map);
                }
            } finally {
                setLoading(false);
            }
        };
        loadSettings();
    }, []);

    const handleSave = async () => {
        setSaving(true);
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
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center py-12 text-slate-500">
                <Loader2 className="animate-spin mr-2" size={20} />
                加载设置中...
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800">基础配置</h3>
                        <p className="text-sm text-slate-500">系统名称、归档参数等基础设置</p>
                    </div>
                    <button
                        onClick={handleSave}
                        disabled={saving}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all disabled:opacity-60"
                    >
                        <Save size={16} className="mr-2" />
                        {saving ? '保存中...' : '保存更改'}
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-2">系统名称</label>
                        <input
                            type="text"
                            value={settings['system.name'] || ''}
                            onChange={(e) => setSettings({ ...settings, 'system.name': e.target.value })}
                            placeholder="例如：XX公司电子会计档案系统"
                            className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                        />
                    </div>

                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-2">归档全宗号前缀</label>
                        <input
                            type="text"
                            value={settings['archive.prefix'] || ''}
                            onChange={(e) => setSettings({ ...settings, 'archive.prefix': e.target.value })}
                            placeholder="例如：QZ-2024"
                            className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                        />
                    </div>

                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-2">存储类型</label>
                        <select
                            className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                            value={settings['storage.type'] || 'local'}
                            onChange={(e) => setSettings({ ...settings, 'storage.type': e.target.value })}
                        >
                            <option value="local">本地/NAS</option>
                            <option value="oss">对象存储 (OSS/S3)</option>
                            <option value="minio">MinIO</option>
                        </select>
                    </div>

                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-2">存储路径/桶名</label>
                        <input
                            type="text"
                            value={settings['storage.path'] || ''}
                            onChange={(e) => setSettings({ ...settings, 'storage.path': e.target.value })}
                            placeholder="例如：/data/archives 或 nexusarchive-bucket"
                            className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                        />
                    </div>

                    <div className="flex flex-col">
                        <label className="text-sm font-medium text-slate-700 mb-2">默认保管期限</label>
                        <select
                            className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                            value={settings['retention.default'] || '30Y'}
                            onChange={(e) => setSettings({ ...settings, 'retention.default': e.target.value })}
                        >
                            <option value="10Y">10年</option>
                            <option value="30Y">30年</option>
                            <option value="PERMANENT">永久</option>
                        </select>
                        <p className="text-xs text-slate-500 mt-1">
                            依据《会计档案管理办法》（财政部79号令）设置
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BasicSettings;
