import React, { useState, useEffect } from 'react';
import { Settings, RefreshCw, Eye, Power, CheckCircle2, XCircle, AlertCircle, Play, Loader2 } from 'lucide-react';
import { erpApi, ErpConfig, ErpScenario } from '../../api/erp';
import { toast } from 'react-hot-toast';

/**
 * 将 Cron 表达式转换为友好的中文文本
 */
const cronToHuman = (cron: string | undefined): string => {
    if (!cron) return '定时';

    // 简单匹配常见 cron 模式
    const parts = cron.split(' ');
    if (parts.length < 6) return cron;

    const [second, minute, hour] = parts;

    // 每天某时执行: 0 0 1 * * ?
    if (parts[3] === '*' && parts[4] === '*') {
        const h = parseInt(hour, 10);
        const m = parseInt(minute, 10);
        if (!isNaN(h) && !isNaN(m)) {
            if (m === 0) {
                return `每日 ${h}:00`;
            }
            return `每日 ${h}:${m.toString().padStart(2, '0')}`;
        }
    }

    // 每小时执行: 0 0 * * * ?
    if (hour === '*' && parts[3] === '*') {
        return '每小时';
    }

    // 兜底：返回简化表示
    return `定时 (${hour}:${minute})`;
};

const IntegrationSettings: React.FC = () => {
    const [configs, setConfigs] = useState<ErpConfig[]>([]);
    const [activeConfigId, setActiveConfigId] = useState<number | null>(null);
    const [scenarios, setScenarios] = useState<ErpScenario[]>([]);
    const [loadingConfig, setLoadingConfig] = useState(false);
    const [loadingScenarios, setLoadingScenarios] = useState(false);
    const [syncing, setSyncing] = useState<number | null>(null);
    const [testing, setTesting] = useState(false);

    // Load Configs on Mount
    useEffect(() => {
        loadConfigs();
    }, []);

    // Load Scenarios when Config changes
    useEffect(() => {
        if (activeConfigId) {
            loadScenarios(activeConfigId);
        } else {
            setScenarios([]);
        }
    }, [activeConfigId]);

    const loadConfigs = async () => {
        setLoadingConfig(true);
        try {
            const res = await erpApi.getConfigs();
            if (res.code === 200 && res.data) {
                setConfigs(res.data);
                if (res.data.length > 0) {
                    setActiveConfigId(res.data[0].id);
                }
            }
        } catch (error) {
            toast.error('加载配置失败');
        } finally {
            setLoadingConfig(false);
        }
    };

    const loadScenarios = async (configId: number) => {
        setLoadingScenarios(true);
        try {
            const res = await erpApi.getScenarios(configId);
            if (res.code === 200) {
                setScenarios(res.data || []);
            }
        } catch (error) {
            toast.error('加载业务场景失败');
        } finally {
            setLoadingScenarios(false);
        }
    };

    const handleToggle = async (scenario: ErpScenario) => {
        const newState = !scenario.isActive;
        try {
            // Optimistic update
            setScenarios(prev => prev.map(s => s.id === scenario.id ? { ...s, isActive: newState } : s));

            const res = await erpApi.updateScenario({ id: scenario.id, isActive: newState });
            if (res.code !== 200) {
                throw new Error(res.message);
            }
            toast.success(newState ? '已启用' : '已禁用');
        } catch (error) {
            toast.error('状态更新失败');
            // Revert on error
            setScenarios(prev => prev.map(s => s.id === scenario.id ? { ...s, isActive: !newState } : s));
        }
    };

    const handleManualSync = async (id: number) => {
        setSyncing(id);
        try {
            const res = await erpApi.triggerSync(id);
            if (res.code === 200) {
                toast.success('同步任务已触发');
                // Refresh status after a short delay
                setTimeout(() => {
                    if (activeConfigId) loadScenarios(activeConfigId);
                    setSyncing(null);
                }, 2000);
            } else {
                toast.error(res.message || '触发失败');
                setSyncing(null);
            }
        } catch (error) {
            toast.error('触发异常');
            setSyncing(null);
        }
    };

    const handleTestConnection = async () => {
        if (!activeConfigId) return;
        setTesting(true);
        try {
            const res = await erpApi.testConnection(activeConfigId);
            if (res.code === 200 && res.data) {
                if (res.data.success) {
                    toast.success(res.data.message || '连接测试成功');
                } else {
                    toast.error(res.data.message || '连接测试失败');
                }
            } else {
                toast.error(res.message || '连接测试失败');
            }
        } catch (error) {
            toast.error('连接测试异常');
        } finally {
            setTesting(false);
        }
    };

    // State for Log Modal
    const [logModalOpen, setLogModalOpen] = useState(false);
    const [currentLog, setCurrentLog] = useState<{ title: string, content: string | null }>({ title: '', content: '' });

    const handleViewLog = (scenario: ErpScenario) => {
        setCurrentLog({
            title: `${scenario.name} - 同步日志`,
            content: scenario.lastSyncMsg || '暂无日志记录'
        });
        setLogModalOpen(true);
    };

    const activeConfig = configs.find(c => c.id === activeConfigId);

    return (
        <div className="flex h-full bg-slate-50 gap-4 p-6 relative">
            {/* Log Modal */}
            {logModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
                    <div className="bg-white rounded-lg shadow-xl w-[600px] max-w-[90vw] flex flex-col max-h-[80vh]">
                        <div className="p-4 border-b border-slate-100 flex justify-between items-center">
                            <h3 className="font-medium text-slate-800 flex items-center gap-2">
                                <CheckCircle2 size={16} className="text-blue-500" />
                                {currentLog.title}
                            </h3>
                            <button
                                onClick={() => setLogModalOpen(false)}
                                className="text-slate-400 hover:text-slate-600 transition-colors"
                            >
                                <XCircle size={20} />
                            </button>
                        </div>
                        <div className="p-6 overflow-y-auto bg-slate-50 font-mono text-sm text-slate-600 whitespace-pre-wrap">
                            {currentLog.content}
                        </div>
                        <div className="p-4 border-t border-slate-100 bg-white rounded-b-lg flex justify-end">
                            <button
                                onClick={() => setLogModalOpen(false)}
                                className="px-4 py-2 bg-slate-100 text-slate-600 rounded hover:bg-slate-200 transition-colors text-sm font-medium"
                            >
                                关闭
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Left: Connection List */}
            <div className="w-64 bg-white rounded-lg border border-slate-200 shadow-sm flex flex-col">
                <div className="p-4 border-b border-slate-100 font-medium text-slate-700 flex items-center gap-2">
                    <Settings size={18} />
                    系统连接
                </div>
                <div className="flex-1 overflow-y-auto p-2 space-y-2">
                    {loadingConfig ? (
                        <div className="flex justify-center p-4"><Loader2 className="animate-spin text-slate-400" /></div>
                    ) : configs.map(config => (
                        <div
                            key={config.id}
                            onClick={() => setActiveConfigId(config.id)}
                            className={`p-3 rounded cursor-pointer border transition-all ${activeConfigId === config.id ? 'bg-blue-50 border-blue-200 shadow-sm' : 'bg-white border-transparent hover:bg-slate-50'}`}
                        >
                            <div className="flex items-center justify-between mb-1">
                                <span className={`font-medium ${activeConfigId === config.id ? 'text-blue-700' : 'text-slate-700'}`}>{config.name}</span>
                                <div className="w-2 h-2 rounded-full bg-emerald-500" title="已连接" />
                            </div>
                            <div className="text-xs text-slate-500">{config.erpType}</div>
                        </div>
                    ))}
                    {!loadingConfig && configs.length === 0 && (
                        <div className="text-center text-xs text-slate-400 py-4">暂无连接 配置请联系管理员</div>
                    )}
                </div>
            </div>

            {/* Right: Scenario List */}
            <div className="flex-1 bg-white rounded-lg border border-slate-200 shadow-sm flex flex-col">
                {activeConfig ? (
                    <>
                        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                            <div>
                                <h2 className="text-lg font-medium text-slate-800">{activeConfig.name} - 业务场景配置</h2>
                                <p className="text-sm text-slate-500 mt-0.5">配置与该系统的业务对接场景，启用后系统将自动或按需执行同步任务。</p>
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={handleTestConnection}
                                    disabled={testing}
                                    className="px-3 py-1.5 text-sm bg-white border border-slate-300 rounded text-slate-700 hover:bg-slate-50 disabled:opacity-50 flex items-center gap-1"
                                >
                                    {testing && <Loader2 className="animate-spin" size={14} />}
                                    连接测试
                                </button>
                            </div>
                        </div>

                        <div className="overflow-x-auto relative min-h-[200px]">
                            {loadingScenarios && (
                                <div className="absolute inset-0 bg-white/60 z-10 flex items-center justify-center">
                                    <Loader2 className="animate-spin text-blue-600" size={32} />
                                </div>
                            )}
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-slate-500 border-b border-slate-200">
                                    <tr>
                                        <th className="p-4 font-medium w-64">场景名称 / 描述</th>
                                        <th className="p-4 font-medium">同步策略</th>
                                        <th className="p-4 font-medium">最后同步</th>
                                        <th className="p-4 font-medium">状态</th>
                                        <th className="p-4 font-medium text-right">操作</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {scenarios.map(scenario => (
                                        <tr key={scenario.id} className="hover:bg-slate-50 group">
                                            <td className="p-4">
                                                <div className="font-medium text-slate-800">{scenario.scenarioKey}</div>
                                                <div className="text-xs text-slate-500 mt-0.5">{scenario.name}</div>
                                                <div className="text-xs text-slate-400 mt-0.5">{scenario.description}</div>
                                            </td>
                                            <td className="p-4">
                                                <span className={`inline-flex items-center gap-1.5 px-2 py-1 rounded text-xs border ${scenario.syncStrategy === 'REALTIME' ? 'bg-purple-50 text-purple-700 border-purple-200' :
                                                    scenario.syncStrategy === 'CRON' ? 'bg-blue-50 text-blue-700 border-blue-200' :
                                                        'bg-slate-100 text-slate-600 border-slate-200'
                                                    }`}>
                                                    {scenario.syncStrategy === 'REALTIME' && '实时/回调'}
                                                    {scenario.syncStrategy === 'CRON' && cronToHuman(scenario.cronExpression)}
                                                    {scenario.syncStrategy === 'MANUAL' && '手动触发'}
                                                </span>
                                            </td>
                                            <td className="p-4 text-slate-600 font-mono text-xs">
                                                {scenario.lastSyncTime ? new Date(scenario.lastSyncTime).toLocaleString() : '-'}
                                            </td>
                                            <td className="p-4">
                                                {scenario.lastSyncStatus === 'SUCCESS' && <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-xs font-medium"><CheckCircle2 size={12} /> 正常</span>}
                                                {scenario.lastSyncStatus === 'FAIL' && <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-50 text-rose-600 text-xs font-medium"><AlertCircle size={12} /> 失败</span>}
                                                {scenario.lastSyncStatus === 'NONE' && <span className="text-slate-400 text-xs">未同步</span>}
                                            </td>
                                            <td className="p-4 text-right">
                                                <div className="flex items-center justify-end gap-2">
                                                    {/* Activate Switch */}
                                                    <button
                                                        onClick={() => handleToggle(scenario)}
                                                        className={`w-8 h-4 rounded-full transition-colors relative ${scenario.isActive ? 'bg-blue-600' : 'bg-slate-300'}`}
                                                        title={scenario.isActive ? "点击禁用" : "点击启用"}
                                                    >
                                                        <div className={`w-3 h-3 bg-white rounded-full absolute top-0.5 transition-transform ${scenario.isActive ? 'left-4.5' : 'left-0.5'}`} />
                                                    </button>

                                                    <div className="w-px h-4 bg-slate-200 mx-1" />

                                                    {/* Manual Sync */}
                                                    <button
                                                        onClick={() => handleManualSync(scenario.id)}
                                                        disabled={!scenario.isActive || syncing === scenario.id}
                                                        className={`p-1.5 rounded-full transition-colors ${syncing === scenario.id ? 'bg-blue-50 text-blue-600 animate-spin' : 'hover:bg-slate-100 text-slate-500 hover:text-blue-600 disabled:opacity-50'}`}
                                                        title="立即同步"
                                                    >
                                                        <RefreshCw size={14} />
                                                    </button>

                                                    {/* View Log */}
                                                    <button
                                                        onClick={() => handleViewLog(scenario)}
                                                        className="p-1.5 hover:bg-slate-100 rounded-full text-slate-500 hover:text-slate-700 transition-colors"
                                                        title="查看日志"
                                                    >
                                                        <Eye size={14} />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    {!loadingScenarios && scenarios.length === 0 && (
                                        <tr><td colSpan={5} className="p-8 text-center text-slate-400">暂无场景配置</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </>
                ) : (
                    <div className="flex flex-col items-center justify-center h-full text-slate-400">
                        <AlertCircle size={48} className="mb-4 opacity-50" />
                        <p>请选择左侧连接以配置业务场景</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default IntegrationSettings;
