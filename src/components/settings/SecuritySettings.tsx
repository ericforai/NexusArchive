import React, { useState, useEffect } from 'react';
import { Save, Loader2, ShieldCheck, FileWarning } from 'lucide-react';
import { adminApi } from '../../api/admin';

/**
 * 安全与合规设置页面
 * 
 * 包含四性检测开关、水印设置等安全相关配置
 */
export const SecuritySettings: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [settings, setSettings] = useState({
        fourNatureCheck: true,
        watermarkEnabled: true,
        auditLogRetention: '365',
        encryptionAlgorithm: 'SM4',
    });

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
                    setSettings({
                        fourNatureCheck: map['security.fourNatureCheck'] !== 'false',
                        watermarkEnabled: map['security.watermarkEnabled'] !== 'false',
                        auditLogRetention: map['security.auditLogRetention'] || '365',
                        encryptionAlgorithm: map['security.encryptionAlgorithm'] || 'SM4',
                    });
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
                { configKey: 'security.fourNatureCheck', configValue: String(settings.fourNatureCheck), category: 'security' },
                { configKey: 'security.watermarkEnabled', configValue: String(settings.watermarkEnabled), category: 'security' },
                { configKey: 'security.auditLogRetention', configValue: settings.auditLogRetention, category: 'security' },
                { configKey: 'security.encryptionAlgorithm', configValue: settings.encryptionAlgorithm, category: 'security' },
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
                        <h3 className="text-lg font-bold text-slate-800">安全与合规</h3>
                        <p className="text-sm text-slate-500">配置四性检测、水印、加密等安全策略</p>
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

                <div className="space-y-6">
                    {/* 四性检测 */}
                    <div className="flex items-center justify-between p-4 bg-slate-50 rounded-xl">
                        <div className="flex items-start space-x-3">
                            <ShieldCheck className="text-primary-500 mt-0.5" size={20} />
                            <div>
                                <p className="text-sm font-medium text-slate-800">开启四性检测强控</p>
                                <p className="text-xs text-slate-500 mt-1">
                                    归档前必须通过真实性、完整性、可用性、安全性检测，否则无法入库
                                </p>
                            </div>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                            <input
                                type="checkbox"
                                className="sr-only peer"
                                checked={settings.fourNatureCheck}
                                onChange={(e) => setSettings({ ...settings, fourNatureCheck: e.target.checked })}
                            />
                            <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                        </label>
                    </div>

                    {/* 水印设置 */}
                    <div className="flex items-center justify-between p-4 bg-slate-50 rounded-xl">
                        <div className="flex items-start space-x-3">
                            <FileWarning className="text-amber-500 mt-0.5" size={20} />
                            <div>
                                <p className="text-sm font-medium text-slate-800">水印强制开启</p>
                                <p className="text-xs text-slate-500 mt-1">
                                    所有借阅预览必须强制添加动态水印（用户名+时间+IP）
                                </p>
                            </div>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                            <input
                                type="checkbox"
                                className="sr-only peer"
                                checked={settings.watermarkEnabled}
                                onChange={(e) => setSettings({ ...settings, watermarkEnabled: e.target.checked })}
                            />
                            <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                        </label>
                    </div>

                    {/* 加密算法 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
                        <div className="flex flex-col">
                            <label className="text-sm font-medium text-slate-700 mb-2">加密算法</label>
                            <select
                                className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                                value={settings.encryptionAlgorithm}
                                onChange={(e) => setSettings({ ...settings, encryptionAlgorithm: e.target.value })}
                            >
                                <option value="SM4">SM4 (国密)</option>
                                <option value="AES256">AES-256</option>
                            </select>
                            <p className="text-xs text-slate-500 mt-1">
                                信创环境建议使用国密 SM4 算法
                            </p>
                        </div>

                        <div className="flex flex-col">
                            <label className="text-sm font-medium text-slate-700 mb-2">审计日志保留天数</label>
                            <select
                                className="border border-slate-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
                                value={settings.auditLogRetention}
                                onChange={(e) => setSettings({ ...settings, auditLogRetention: e.target.value })}
                            >
                                <option value="90">90 天</option>
                                <option value="180">180 天</option>
                                <option value="365">365 天 (1年)</option>
                                <option value="730">730 天 (2年)</option>
                                <option value="1825">1825 天 (5年)</option>
                                <option value="3650">3650 天 (10年)</option>
                            </select>
                            <p className="text-xs text-slate-500 mt-1">
                                依据等保 2.0 三级要求，建议至少保留 180 天
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* 合规提示 */}
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                <div className="flex items-start space-x-3">
                    <ShieldCheck className="text-amber-600 mt-0.5" size={20} />
                    <div>
                        <p className="text-sm font-medium text-amber-800">合规提示</p>
                        <ul className="text-xs text-amber-700 mt-2 space-y-1 list-disc list-inside">
                            <li>四性检测依据 DA/T 92-2022《电子档案移交利用管理办法》</li>
                            <li>水印功能符合《会计档案管理办法》（财政部79号令）借阅管理要求</li>
                            <li>加密存储符合等保 2.0 三级数据安全要求</li>
                            <li>审计日志满足 GB/T 39784-2021 审计追溯要求</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SecuritySettings;
