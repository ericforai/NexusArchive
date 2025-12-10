import React, { useState, useEffect } from 'react';
import { ShieldCheck, Upload, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { licenseApi, LicenseInfo } from '../../api/license';

export const LicenseSettings: React.FC = () => {
    const [license, setLicense] = useState<LicenseInfo | null>(null);
    const [loading, setLoading] = useState(false);
    const [importText, setImportText] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    useEffect(() => {
        loadLicense();
    }, []);

    const loadLicense = async () => {
        try {
            const res = await licenseApi.getCurrent();
            if (res.code === 200 && res.data) {
                setLicense(res.data);
            }
        } catch (e) {
            console.error(e);
        }
    };

    const handleImport = async () => {
        if (!importText.trim()) return;
        setLoading(true);
        setError(null);
        try {
            const res = await licenseApi.load(importText.trim());
            if (res.code === 200) {
                setSuccess(true);
                setLicense(res.data);
                setImportText('');
                setTimeout(() => setSuccess(false), 3000);
            } else {
                setError(res.message || '导入失败');
            }
        } catch (e: any) {
            setError(e?.message || '导入异常');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
            <div className="flex items-center justify-between mb-4">
                <div>
                    <h3 className="text-lg font-bold text-slate-800 mb-1">License 授权管理</h3>
                    <p className="text-xs text-slate-500">查看系统授权状态或导入新的 License 证书。</p>
                </div>
                {success && (
                    <span className="inline-flex items-center text-xs text-emerald-600 bg-emerald-50 border border-emerald-100 rounded px-2 py-1 animate-in fade-in">
                        <CheckCircle2 size={14} className="mr-1" /> 授权更新成功
                    </span>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* 左侧：当前状态 */}
                <div className="border border-slate-100 bg-slate-50 rounded-xl p-4">
                    <h4 className="flex items-center gap-2 font-semibold text-slate-700 mb-4">
                        <ShieldCheck className={`w-5 h-5 ${license ? 'text-emerald-500' : 'text-slate-400'}`} />
                        当前状态
                    </h4>

                    {!license ? (
                        <div className="flex flex-col items-center justify-center py-8 text-slate-400">
                            <AlertTriangle className="w-8 h-8 mb-2 opacity-50" />
                            <span className="text-sm">暂无有效授权</span>
                        </div>
                    ) : (
                        <div className="space-y-3 text-sm">
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">到期时间</span>
                                <span className="font-mono font-medium text-slate-900">{license.expireAt}</span>
                            </div>
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">最大用户数</span>
                                <span className="font-mono font-medium text-slate-900">{license.maxUsers} 人</span>
                            </div>
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">节点限制</span>
                                <span className="font-mono font-medium text-slate-900">{license.nodeLimit} 节点</span>
                            </div>
                            <div className="pt-2 text-xs text-slate-400 break-all">
                                证书指纹: {license.raw?.substring(0, 20)}...
                            </div>
                        </div>
                    )}
                </div>

                {/* 右侧：导入区域 */}
                <div>
                    <h4 className="flex items-center gap-2 font-semibold text-slate-700 mb-2">
                        <Upload className="w-4 h-4 text-slate-500" />
                        导入证书
                    </h4>
                    <textarea
                        className="w-full h-32 border border-slate-300 rounded-lg p-3 text-xs font-mono focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none resize-none"
                        placeholder="在此粘贴 License JSON 字符串..."
                        value={importText}
                        onChange={(e) => setImportText(e.target.value)}
                    />

                    {error && (
                        <div className="mt-2 text-xs text-red-500 flex items-center gap-1">
                            <AlertTriangle size={12} />
                            {error}
                        </div>
                    )}

                    <div className="mt-3 flex justify-end">
                        <button
                            onClick={handleImport}
                            disabled={loading || !importText.trim()}
                            className="px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors flex items-center gap-2"
                        >
                            {loading ? '验证中...' : '验证并导入'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};
