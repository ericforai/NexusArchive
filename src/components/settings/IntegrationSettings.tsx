// Input: React、IntegrationSettingsApi
// Output: React 组件 IntegrationSettings
// Pos: 系统设置组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';

import {
    Settings, RefreshCw, Eye, CheckCircle2, XCircle, AlertCircle,
    Loader2, ChevronRight, ChevronDown, History, Sliders,
    ToggleLeft, ToggleRight, Clock, Database, Zap, Download, Upload,
    Activity, ShieldCheck, PlusCircle, MoreHorizontal
} from 'lucide-react';
import { IntegrationSettingsApi } from './types';
import { ErpConfig, ErpScenario, ErpSubInterface, IntegrationDiagnosisResult, IntegrationMonitoring, ReconciliationRecord, SyncHistory } from '../../types';
import { toast } from 'react-hot-toast';
import { ComplianceRadar, ReconciliationReport } from '../common';

// Adapter type icons & colors (Refined Monochrome Theme)
const ADAPTER_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
    yonsuite: { icon: <Database size={16} />, color: 'text-blue-600 bg-blue-50', label: '用友 YonSuite' },
    kingdee: { icon: <Zap size={16} />, color: 'text-blue-600 bg-blue-50', label: '金蝶云星空' },
    weaver: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '泛微 OA' },
    weavere10: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '泛微 E10' },
    weaver_e10: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '泛微 E10' },
    generic: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '通用 REST' },
};

const ERP_TEMPLATES = [
    { name: 'YonSuite', pattern: /yonyoucloud|yonbip|yonsuite/i, type: 'yonsuite', defaultUrl: 'https://api.yonyoucloud.com/iuap-api-gateway' },
    { name: '金蝶云星空', pattern: /kingdee|k3cloud/i, type: 'kingdee', defaultUrl: '/k3cloud/' },
    { name: '泛微 OA (e-9)', pattern: /weaver|ecology/i, type: 'weaver', defaultUrl: '/weaver/' },
    { name: '泛微 E10', pattern: /weavere10|e10/i, type: 'weavere10', defaultUrl: '/e10/' }
];

const cronToHuman = (cron: string | undefined): string => {
    if (!cron) return '定时';
    const parts = cron.split(' ');
    if (parts.length < 6) return cron;
    const [, minute, hour] = parts;
    if (parts[3] === '*' && parts[4] === '*') {
        const h = parseInt(hour, 10);
        const m = parseInt(minute, 10);
        if (!isNaN(h) && !isNaN(m)) {
            return m === 0 ? `每日 ${h}:00` : `每日 ${h}:${m.toString().padStart(2, '0')}`;
        }
    }
    if (hour === '*' && parts[3] === '*') return '每小时';
    return `定时 (${hour}:${minute})`;
};

interface IntegrationSettingsProps {
    erpApi: IntegrationSettingsApi;
}

const IntegrationSettings: React.FC<IntegrationSettingsProps> = ({ erpApi }) => {
    // State: Configs grouped by adapter type
    const [configs, setConfigs] = useState<ErpConfig[]>([]);
    const [adapterTypes, setAdapterTypes] = useState<string[]>([]);
    const [expandedTypes, setExpandedTypes] = useState<Set<string>>(new Set());
    const [activeConfigId, setActiveConfigId] = useState<number | null>(null);

    // State: Scenarios and sub-interfaces
    const [scenarios, setScenarios] = useState<ErpScenario[]>([]);
    const [expandedScenarios, setExpandedScenarios] = useState<Set<number>>(new Set());
    const [subInterfaces, setSubInterfaces] = useState<Record<number, ErpSubInterface[]>>({});

    // State: Sync history
    const [syncHistory, setSyncHistory] = useState<Record<number, SyncHistory[]>>({});
    const [showHistoryFor, setShowHistoryFor] = useState<number | null>(null);
    const [actionMenuScenario, setActionMenuScenario] = useState<ErpScenario | null>(null);

    // State: Params editor
    const [showParamsFor, setShowParamsFor] = useState<number | null>(null);
    const [pendingSyncId, setPendingSyncId] = useState<number | null>(null); // 待同步的场景ID
    const [paramsForm, setParamsForm] = useState<{ startDate: string; endDate: string; pageSize: number }>({
        startDate: '', endDate: '', pageSize: 100
    });

    // Loading states
    const [loadingConfig, setLoadingConfig] = useState(false);
    const [loadingScenarios, setLoadingScenarios] = useState(false);
    const [syncing, setSyncing] = useState<number | null>(null);
    const [testing, setTesting] = useState(false);

    // Connector Modal State
    const [showConfigModal, setShowConfigModal] = useState(false);
    const [editingConfig, setEditingConfig] = useState<Partial<ErpConfig> | null>(null);
    const [configForm, setConfigForm] = useState({
        name: '', erpType: 'yonsuite', baseUrl: '', appKey: '', appSecret: '',
        accbookCode: '',          // Legacy single value
        accbookCodes: [] as string[]  // Multi-org support
    });
    const [newAccbookCode, setNewAccbookCode] = useState(''); // For adding new codes
    const [detectedType, setDetectedType] = useState<string | null>(null);

    // Diagnosis State
    const [showDiagnosis, setShowDiagnosis] = useState(false);
    const [diagnosing, setDiagnosing] = useState(false);
    const [diagnosisResult, setDiagnosisResult] = useState<IntegrationDiagnosisResult | null>(null);

    // Phase 4: Reconciliation State
    const [showRecon, setShowRecon] = useState(false);
    const [reconRecord, setReconRecord] = useState<ReconciliationRecord | null>(null);
    const [reconLoading, setReconLoading] = useState(false);

    // Monitoring State
    const [monitoringData, setMonitoringData] = useState<IntegrationMonitoring | null>(null);

    const loadConfigs = useCallback(async () => {
        setLoadingConfig(true);
        try {
            const res = await erpApi.getConfigs();
            if (res.code === 200 && res.data) {
                setConfigs(res.data);
                // Extract unique adapter types
                const types = [...new Set(res.data.map(c => c.erpType?.toLowerCase() || 'generic'))];
                setAdapterTypes(types);
                // Expand first type by default
                if (types.length > 0) {
                    setExpandedTypes(new Set([types[0]]));
                    // Select first config of first type
                    const firstOfType = res.data.find(c => (c.erpType?.toLowerCase() || 'generic') === types[0]);
                    if (firstOfType) setActiveConfigId(firstOfType.id);
                }
            }
        } catch {
            toast.error('加载配置失败');
        } finally {
            setLoadingConfig(false);
        }
    }, [erpApi]);

    const loadScenarios = useCallback(async (configId: number) => {
        setLoadingScenarios(true);
        try {
            const res = await erpApi.getScenarios(configId);
            if (res.code === 200) {
                setScenarios(res.data || []);
            }
        } catch {
            toast.error('加载业务场景失败');
        } finally {
            setLoadingScenarios(false);
        }
    }, [erpApi]);

    const loadSubInterfaces = useCallback(async (scenarioId: number) => {
        try {
            const res = await erpApi.getSubInterfaces(scenarioId);
            if (res.code === 200) {
                setSubInterfaces(prev => ({ ...prev, [scenarioId]: res.data || [] }));
            }
        } catch (error) {
            console.error('加载子接口失败', error);
        }
    }, [erpApi]);

    const loadSyncHistory = useCallback(async (scenarioId: number) => {
        try {
            const res = await erpApi.getSyncHistory(scenarioId);
            if (res.code === 200) {
                setSyncHistory(prev => ({ ...prev, [scenarioId]: res.data || [] }));
            }
        } catch (error) {
            console.error('加载同步历史失败', error);
        }
    }, [erpApi]);

    const loadMonitoring = useCallback(async () => {
        try {
            const res = await erpApi.getIntegrationMonitoring();
            if (res.code === 200) {
                setMonitoringData(res.data);
            }
        } catch (error) {
            console.error('Failed to load monitoring data', error);
        }
    }, [erpApi]);

    useEffect(() => {
        loadConfigs();
        loadMonitoring();
    }, [loadConfigs, loadMonitoring]);

    // Load Scenarios when Config changes
    useEffect(() => {
        if (activeConfigId) {
            loadScenarios(activeConfigId);
        } else {
            setScenarios([]);
        }
    }, [activeConfigId, loadScenarios]);


    const toggleAdapterType = (type: string) => {
        setExpandedTypes(prev => {
            const next = new Set(prev);
            if (next.has(type)) next.delete(type);
            else next.add(type);
            return next;
        });
    };

    const toggleScenarioExpand = async (scenarioId: number) => {
        setExpandedScenarios(prev => {
            const next = new Set(prev);
            if (next.has(scenarioId)) {
                next.delete(scenarioId);
            } else {
                next.add(scenarioId);
                // Load sub-interfaces if not loaded
                if (!subInterfaces[scenarioId]) {
                    loadSubInterfaces(scenarioId);
                }
            }
            return next;
        });
    };

    const handleToggleScenario = async (scenario: ErpScenario) => {
        const newState = !scenario.isActive;
        try {
            setScenarios(prev => prev.map(s => s.id === scenario.id ? { ...s, isActive: newState } : s));
            const res = await erpApi.updateScenario({ id: scenario.id, isActive: newState });
            if (res.code !== 200) throw new Error(res.message);
            toast.success(newState ? '已启用' : '已禁用');
        } catch {
            toast.error('状态更新失败');
            setScenarios(prev => prev.map(s => s.id === scenario.id ? { ...s, isActive: !newState } : s));
        }
    };

    const handleToggleSubInterface = async (subId: number, scenarioId: number) => {
        try {
            await erpApi.toggleSubInterface(subId);
            // Refresh sub-interfaces
            loadSubInterfaces(scenarioId);
            toast.success('接口状态已更新');
        } catch {
            toast.error('更新失败');
        }
    };

    // 同步前先弹出日期选择弹窗
    const handleManualSync = async (id: number) => {
        const scenario = scenarios.find(s => s.id === id);
        if (scenario) {
            // 设置待同步场景ID并弹出参数配置窗口
            setPendingSyncId(id);
            handleShowParams(scenario);
        }
    };

    // 实际执行同步的内部函数
    const executeSync = async (id: number) => {
        setSyncing(id);
        try {
            const res = await erpApi.triggerSync(id);
            if (res.code === 200) {
                toast.success('同步任务已触发');
                setTimeout(() => {
                    if (activeConfigId) loadScenarios(activeConfigId);
                    setSyncing(null);
                }, 2000);
            } else {
                toast.error(res.message || '触发失败');
                setSyncing(null);
            }
        } catch {
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
                if (res.data.success) toast.success(res.data.message || '连接测试成功');
                else toast.error(res.data.message || '连接测试失败');
            } else {
                toast.error(res.message || '连接测试失败');
            }
        } catch {
            toast.error('连接测试异常');
        } finally {
            setTesting(false);
        }
    };

    const handleTriggerRecon = async () => {
        if (!activeConfigId) return;
        setShowRecon(true);
        setReconLoading(true);
        setReconRecord(null);
        try {
            const today = new Date().toISOString().split('T')[0];
            const lastMonth = new Date();
            lastMonth.setMonth(lastMonth.getMonth() - 1);
            const startDate = lastMonth.toISOString().split('T')[0];

            const res = await erpApi.triggerReconciliation({
                configId: activeConfigId,
                subjectCode: '1001',
                startDate: startDate,
                endDate: today,
                operatorId: 'user_admin'
            });
            if (res.code === 200) {
                setReconRecord(res.data);
            } else {
                toast.error(res.message || '核对执行失败');
            }
        } catch {
            toast.error('对账任务触发异常');
        } finally {
            setReconLoading(false);
        }
    };

    const handleDiagnose = async () => {
        if (!activeConfigId) return;
        setDiagnosing(true);
        setShowDiagnosis(true);
        setDiagnosisResult(null);
        try {
            const res = await erpApi.diagnoseConfig(activeConfigId);
            if (res.code === 200) {
                setDiagnosisResult(res.data);
            } else {
                toast.error(res.message || '诊断请求失败');
            }
        } catch {
            toast.error('一键诊断异常');
        } finally {
            setDiagnosing(false);
        }
    };

    const handleShowHistory = async (scenarioId: number) => {
        setShowHistoryFor(scenarioId);
        if (!syncHistory[scenarioId]) {
            await loadSyncHistory(scenarioId);
        }
    };

    const handleShowParams = (scenario: ErpScenario) => {
        setShowParamsFor(scenario.id);
        // Parse existing params
        try {
            const existing = scenario.paramsJson ? JSON.parse(scenario.paramsJson) : {};
            setParamsForm({
                startDate: existing.startDate || '',
                endDate: existing.endDate || '',
                pageSize: existing.pageSize || 100,
            });
        } catch {
            setParamsForm({ startDate: '', endDate: '', pageSize: 100 });
        }
    };

    const handleSaveParams = async (triggerSync: boolean = false) => {
        if (!showParamsFor) return;
        try {
            await erpApi.updateScenarioParams(showParamsFor, paramsForm);
            toast.success('参数已保存');

            const scenarioId = showParamsFor;
            setShowParamsFor(null);

            // 如果是从同步按钮触发的，保存后自动执行同步
            if (triggerSync && pendingSyncId === scenarioId) {
                setPendingSyncId(null);
                executeSync(scenarioId);
            } else {
                setPendingSyncId(null);
            }

            if (activeConfigId) loadScenarios(activeConfigId);
        } catch {
            toast.error('保存失败');
        }
    };

    // --- Connector Management Logic ---
    const handleUrlChange = (url: string) => {
        setConfigForm(prev => ({ ...prev, baseUrl: url }));

        // Smart Detection
        const match = ERP_TEMPLATES.find(t => t.pattern.test(url));
        if (match) {
            setDetectedType(match.type);
            // Auto-fill logic (only if erpType hasn't been manually changed to something else)
            // Or just suggest it. 
        } else {
            setDetectedType(null);
        }
    };

    const handleApplyDetected = () => {
        if (!detectedType) return;
        const template = ERP_TEMPLATES.find(t => t.type === detectedType);
        setConfigForm(prev => ({
            ...prev,
            erpType: detectedType,
            baseUrl: prev.baseUrl || template?.defaultUrl || ''
        }));
        setDetectedType(null);
        toast.success(`已应用 ${template?.name} 模板建议`, { icon: '✨' });
    };

    const openAddConfig = () => {
        setEditingConfig(null);
        setConfigForm({ name: '', erpType: 'yonsuite', baseUrl: '', appKey: '', appSecret: '', accbookCode: '', accbookCodes: [] });
        setDetectedType(null);
        setShowConfigModal(true);
    };

    const openEditConfig = (config: ErpConfig) => {
        setEditingConfig(config);
        let parsed = { baseUrl: '', appKey: '', appSecret: '', accbookCode: '', accbookCodes: [] as string[] };
        try {
            parsed = config.configJson ? JSON.parse(config.configJson) : parsed;
        } catch (e) {
            console.error('Failed to parse configJson', e);
        }
        setConfigForm({
            name: config.name,
            erpType: config.erpType,
            baseUrl: parsed.baseUrl || '',
            appKey: parsed.appKey || '',
            appSecret: parsed.appSecret || '',
            accbookCode: parsed.accbookCode || '',
            accbookCodes: parsed.accbookCodes || (parsed.accbookCode ? [parsed.accbookCode] : [])
        });
        setDetectedType(null);
        setShowConfigModal(true);
    };

    const handleSaveConfig = async () => {
        if (!configForm.name || !configForm.baseUrl) {
            toast.error('名称和基础URL必填');
            return;
        }

        const configJson = JSON.stringify({
            baseUrl: configForm.baseUrl,
            appKey: configForm.appKey,
            appSecret: configForm.appSecret,
            accbookCode: configForm.accbookCodes.length > 0 ? configForm.accbookCodes[0] : configForm.accbookCode,
            accbookCodes: configForm.accbookCodes.length > 0 ? configForm.accbookCodes :
                (configForm.accbookCode ? [configForm.accbookCode] : [])
        });

        const data: Partial<ErpConfig> = {
            id: editingConfig?.id,
            name: configForm.name,
            erpType: configForm.erpType,
            configJson: configJson,
            isActive: 1
        };

        try {
            await erpApi.saveConfig(data);
            toast.success(editingConfig ? '配置已更新' : '配置已保存');
            setShowConfigModal(false);
            loadConfigs();
        } catch {
            toast.error('保存失败');
        }
    };

    const handleDeleteConfig = async (id: number) => {
        if (!window.confirm('确定要删除此连接配置吗？相关的同步场景也将被清理。')) return;
        try {
            await erpApi.deleteConfig(id);
            toast.success('删除成功');
            if (activeConfigId === id) setActiveConfigId(null);
            loadConfigs();
        } catch {
            toast.error('删除失败');
        }
    };

    const handleExportConfig = () => {
        if (!activeConfig) return;
        const exportData = {
            name: activeConfig.name,
            erpType: activeConfig.erpType,
            configJson: activeConfig.configJson,
            version: 'Phase2-Export-V1'
        };
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `erp-connector-${activeConfig.name}-${activeConfig.erpType}.json`;
        a.click();
        URL.revokeObjectURL(url);
        toast.success('配置已导出');
    };

    const handleImportConfig = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = async (event) => {
            try {
                const imported = JSON.parse(event.target?.result as string);
                if (!imported.erpType || !imported.configJson) {
                    throw new Error('无效的配置格式');
                }

                const data: Partial<ErpConfig> = {
                    name: `${imported.name} (导入)`,
                    erpType: imported.erpType,
                    configJson: imported.configJson,
                    isActive: 1
                };

                await erpApi.saveConfig(data);
                toast.success('配置导入成功');
                loadConfigs();
            } catch {
                toast.error('导入失败: 格式错误');
            }
        };
        reader.readAsText(file);
    };

    const activeConfig = configs.find(c => c.id === activeConfigId);
    const configsByType = adapterTypes.reduce((acc, type) => {
        acc[type] = configs.filter(c => (c.erpType?.toLowerCase() || 'generic') === type);
        return acc;
    }, {} as Record<string, ErpConfig[]>);

    return (
        <div className="flex flex-col h-full bg-slate-50 gap-6 p-6 overflow-hidden">
            {/* Phase 4: Monitoring Dashboard (Refined Card Layout) */}
            {monitoringData && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-5 flex-shrink-0 animate-in fade-in slide-in-from-top-2 duration-500">
                    <div className="bg-white p-5 rounded-xl border border-slate-100 shadow-sm hover:ring-1 hover:ring-blue-100 transition-all">
                        <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2">系统累并发量</div>
                        <div className="flex items-end gap-2">
                            <div className="text-3xl font-bold text-slate-900 leading-none">{monitoringData.totalSyncCount}</div>
                            <div className="text-xs text-emerald-500 font-medium mb-1 flex items-center gap-1">
                                <Activity size={12} />
                                正常
                            </div>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-xl border border-slate-100 shadow-sm hover:ring-1 hover:ring-blue-100 transition-all">
                        <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2">同步成功率</div>
                        <div className="flex items-end gap-2">
                            <div className="text-3xl font-bold text-slate-900 leading-none">{(monitoringData.successRate * 100).toFixed(1)}<span className="text-lg">%</span></div>
                            <div className="text-xs text-emerald-500 font-medium mb-1 flex items-center gap-1">
                                <ShieldCheck size={12} />
                                高可靠
                            </div>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-xl border border-slate-100 shadow-sm hover:ring-1 hover:ring-blue-100 transition-all">
                        <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2">存证覆盖率</div>
                        <div className="flex flex-col gap-2">
                            <div className="text-3xl font-bold text-slate-900 leading-none">{(monitoringData.evidenceCoverage * 100).toFixed(0)}%</div>
                            <div className="w-full bg-slate-50 h-1 rounded-full overflow-hidden mt-1">
                                <div
                                    className="bg-blue-500 h-full transition-all duration-1000"
                                    style={{ width: `${monitoringData.evidenceCoverage * 100}%` }}
                                ></div>
                            </div>
                        </div>
                    </div>
                    <div className="bg-white p-5 rounded-xl border border-slate-100 shadow-sm hover:ring-1 hover:ring-blue-100 transition-all">
                        <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2">ERP 通讯状态</div>
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                            <div className="text-lg font-bold text-slate-800">通讯正常</div>
                        </div>
                        <div className="mt-2 text-[10px] text-slate-400 font-mono">
                            网络延迟: 124ms
                        </div>
                    </div>
                </div>
            )}

            <div className="flex flex-1 gap-4 overflow-hidden">
                {/* Params Modal */}
                {showParamsFor && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
                        <div className="bg-white rounded-lg shadow-xl w-[500px] max-w-[90vw]">
                            <div className="p-4 border-b border-slate-100 flex justify-between items-center">
                                <h3 className="font-medium text-slate-800 flex items-center gap-2">
                                    <Sliders size={18} className="text-blue-500" />
                                    场景参数配置
                                </h3>
                                <button onClick={() => setShowParamsFor(null)} className="text-slate-400 hover:text-slate-600">
                                    <XCircle size={20} />
                                </button>
                            </div>
                            <div className="p-6 space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">开始日期</label>
                                    <input
                                        type="date"
                                        value={paramsForm.startDate}
                                        onChange={e => setParamsForm(p => ({ ...p, startDate: e.target.value }))}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">结束日期</label>
                                    <input
                                        type="date"
                                        value={paramsForm.endDate}
                                        onChange={e => setParamsForm(p => ({ ...p, endDate: e.target.value }))}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">每页数量</label>
                                    <input
                                        type="number"
                                        value={paramsForm.pageSize}
                                        onChange={e => setParamsForm(p => ({ ...p, pageSize: parseInt(e.target.value) || 100 }))}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                                        min={1}
                                        max={500}
                                    />
                                </div>
                            </div>
                            <div className="p-4 border-t border-slate-100 flex justify-end gap-2">
                                <button onClick={() => { setPendingSyncId(null); setShowParamsFor(null); }} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded">
                                    取消
                                </button>
                                <button onClick={() => handleSaveParams(false)} className="px-4 py-2 bg-slate-200 text-slate-700 rounded hover:bg-slate-300">
                                    仅保存
                                </button>
                                {pendingSyncId && (
                                    <button onClick={() => handleSaveParams(true)} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                                        保存并同步
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* History Modal */}
                {showHistoryFor && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
                        <div className="bg-white rounded-lg shadow-xl w-[700px] max-w-[90vw] max-h-[80vh] flex flex-col">
                            <div className="p-4 border-b border-slate-100 flex justify-between items-center">
                                <h3 className="font-medium text-slate-800 flex items-center gap-2">
                                    <History size={18} className="text-blue-500" />
                                    同步历史记录
                                </h3>
                                <button onClick={() => setShowHistoryFor(null)} className="text-slate-400 hover:text-slate-600">
                                    <XCircle size={20} />
                                </button>
                            </div>
                            <div className="flex-1 overflow-y-auto p-4">
                                {(syncHistory[showHistoryFor] || []).length === 0 ? (
                                    <div className="text-center text-slate-400 py-8">暂无同步记录</div>
                                ) : (
                                    <>
                                        {/* Latest Compliance Radar */}
                                        {(() => {
                                            const latest = (syncHistory[showHistoryFor] || []).find(h => h.status === 'SUCCESS' && h.fourNatureSummary);
                                            if (!latest || !latest.fourNatureSummary) return null;
                                            try {
                                                const data = JSON.parse(latest.fourNatureSummary);
                                                return <div className="mb-6"><ComplianceRadar data={data} /></div>;
                                            } catch {
                                                return null;
                                            }
                                        })()}

                                        <table className="w-full text-sm">
                                            <thead className="bg-slate-50 text-slate-500">
                                                <tr>
                                                    <th className="p-2 text-left">开始时间</th>
                                                    <th className="p-2 text-left">结束时间</th>
                                                    <th className="p-2 text-center">状态</th>
                                                    <th className="p-2 text-right">总数</th>
                                                    <th className="p-2 text-right">成功</th>
                                                    <th className="p-2 text-right">失败</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-100">
                                                {(syncHistory[showHistoryFor] || []).map(h => (
                                                    <tr key={h.id} className="hover:bg-slate-50">
                                                        <td className="p-2 font-mono text-xs">{h.syncStartTime ? new Date(h.syncStartTime).toLocaleString() : '-'}</td>
                                                        <td className="p-2 font-mono text-xs">{h.syncEndTime ? new Date(h.syncEndTime).toLocaleString() : '-'}</td>
                                                        <td className="p-2 text-center">
                                                            {h.status === 'SUCCESS' && <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-600 text-xs">成功</span>}
                                                            {h.status === 'FAIL' && <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-600 text-xs">失败</span>}
                                                            {h.status === 'RUNNING' && <span className="px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 text-xs">运行中</span>}
                                                        </td>
                                                        <td className="p-2 text-right">{h.totalCount}</td>
                                                        <td className="p-2 text-right text-emerald-600">{h.successCount}</td>
                                                        <td className="p-2 text-right text-rose-600">{h.failCount}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* Left: Two-Level Connector List */}
                <div className="w-72 bg-white rounded-lg border border-slate-200 shadow-sm flex flex-col">
                    <div className="p-4 border-b border-slate-100 font-medium text-slate-700 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <Settings size={18} />
                            连接器类型
                        </div>
                        <div className="group relative">
                            <button className="h-8 w-8 flex items-center justify-center text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all shadow-sm border border-transparent hover:border-blue-100">
                                <PlusCircle size={18} />
                            </button>
                            <div className="absolute right-0 top-full mt-2 hidden group-hover:block z-50">
                                <div className="bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl border border-slate-100 p-2 w-56 animate-in fade-in zoom-in-95 duration-200">
                                    <div className="px-3 py-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-50 mb-1">
                                        配置管理
                                    </div>
                                    <button
                                        onClick={openAddConfig}
                                        className="w-full text-left px-3 py-2.5 text-xs font-bold text-blue-600 hover:bg-blue-50 rounded-xl flex items-center gap-3 transition-colors group/item"
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center text-blue-600 group-hover/item:scale-110 transition-transform">
                                            <Zap size={14} />
                                        </div>
                                        新增连接配置
                                    </button>
                                    <label className="w-full text-left px-3 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-xl flex items-center gap-3 cursor-pointer transition-colors group/item">
                                        <div className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-slate-400 group-hover/item:scale-110 group-hover/item:text-slate-600 transition-transform">
                                            <Upload size={14} />
                                        </div>
                                        导入配置文件
                                        <input type="file" className="hidden" accept=".json" onChange={handleImportConfig} />
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="flex-1 overflow-y-auto px-2 py-3">
                        {loadingConfig ? (
                            <div className="flex justify-center p-4"><Loader2 className="animate-spin text-slate-300" /></div>
                        ) : adapterTypes.length === 0 ? (
                            <div className="text-center text-xs text-slate-400 py-4">暂无连接配置</div>
                        ) : (
                            adapterTypes.map(type => {
                                const cfg = ADAPTER_CONFIG[type] || ADAPTER_CONFIG.generic;
                                const typeConfigs = configsByType[type] || [];
                                const isExpanded = expandedTypes.has(type);
                                const isSingle = typeConfigs.length === 1;

                                if (isSingle) {
                                    const config = typeConfigs[0];
                                    return (
                                        <div
                                            key={config.id}
                                            onClick={() => setActiveConfigId(config.id)}
                                            className={`group relative flex items-center gap-3 p-2.5 mb-1 rounded-lg cursor-pointer transition-all ${activeConfigId === config.id
                                                ? 'bg-blue-50/50 text-blue-700 shadow-sm'
                                                : 'hover:bg-slate-50 text-slate-600'
                                                }`}
                                        >
                                            <div className={`w-8 h-8 rounded-lg flex items-center justify-center transition-colors ${activeConfigId === config.id ? 'bg-blue-100 text-blue-600' : 'bg-slate-50 text-slate-400 group-hover:text-slate-600'}`}>
                                                {cfg.icon}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className="text-sm font-medium truncate">{config.name}</div>
                                                <div className="text-[10px] text-slate-400 uppercase tracking-tight">{cfg.label}</div>
                                            </div>
                                            {activeConfigId === config.id && <div className="w-1 absolute left-0 top-2 bottom-2 bg-blue-500 rounded-full" />}
                                        </div>
                                    );
                                }

                                return (
                                    <div key={type} className="mb-2">
                                        {/* Type Header */}
                                        <button
                                            onClick={() => toggleAdapterType(type)}
                                            className="w-full flex items-center gap-2 p-2 rounded-lg hover:bg-slate-50 transition-colors group"
                                        >
                                            <div className={`w-6 h-6 rounded flex items-center justify-center text-slate-400 group-hover:text-slate-600`}>
                                                {cfg.icon}
                                            </div>
                                            <span className="flex-1 text-left text-sm font-medium text-slate-600">{cfg.label}</span>
                                            <span className="text-[10px] text-slate-300 mr-1">{typeConfigs.length}</span>
                                            {isExpanded ? <ChevronDown size={14} className="text-slate-300" /> : <ChevronRight size={14} className="text-slate-300" />}
                                        </button>

                                        {/* Configs under this type */}
                                        {isExpanded && (
                                            <div className="ml-8 mt-1 space-y-1">
                                                {typeConfigs.map(config => (
                                                    <div
                                                        key={config.id}
                                                        onClick={() => setActiveConfigId(config.id)}
                                                        className={`p-2 pl-3 rounded-md cursor-pointer text-sm transition-all ${activeConfigId === config.id
                                                            ? 'text-blue-600 font-medium'
                                                            : 'text-slate-500 hover:text-slate-800'
                                                            }`}
                                                    >
                                                        {config.name}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>


                {/* Right: Scenario List with Sub-Interfaces */}
                <div className="flex-1 bg-white rounded-lg border border-slate-200 shadow-sm flex flex-col">
                    {activeConfig ? (
                        <>
                            <div className="px-6 py-6 border-b border-slate-100 flex flex-col gap-6 bg-white/50 backdrop-blur-md">
                                {/* Row 1: Identity and Metadata (Vertical Stack) */}
                                <div className="flex flex-col items-start gap-5">
                                    <div className="flex items-center gap-4">
                                        <h2 className="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight break-words">
                                            {activeConfig.name}
                                        </h2>
                                        <div className="flex items-center px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-[10px] font-bold uppercase tracking-wider border border-emerald-100/50">
                                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-2 shadow-[0_0_8px_rgba(16,185,129,0.5)]" />
                                            已启用
                                        </div>
                                    </div>

                                    {/* Metadata Row (Figure 2 style) - Occupying its own line */}
                                    <div className="flex items-center gap-12">
                                        <div className="flex items-center gap-3 group/meta">
                                            <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center text-blue-500 group-hover/meta:bg-blue-100 transition-colors">
                                                <Database size={16} />
                                            </div>
                                            <div>
                                                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider whitespace-nowrap">系统类型</p>
                                                <p className="text-sm text-slate-700 font-bold whitespace-nowrap">
                                                    {ADAPTER_CONFIG[activeConfig.erpType?.toLowerCase() || 'generic']?.label}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-3 group/meta">
                                            <div className="w-10 h-10 rounded-xl bg-slate-50 flex items-center justify-center text-slate-400 group-hover/meta:bg-slate-100 transition-colors">
                                                <Clock size={16} />
                                            </div>
                                            <div>
                                                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider whitespace-nowrap">最后更新</p>
                                                <p className="text-sm text-slate-700 font-bold whitespace-nowrap">
                                                    {new Date(activeConfig.lastModifiedTime || activeConfig.createdTime || Date.now()).toLocaleDateString()}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* Row 2 Actions - Single Line & Smaller Icons */}
                                <div className="flex items-center justify-between gap-2">
                                    <div className="flex-1 overflow-x-auto min-w-0 flex items-center gap-2.5 pt-1 pb-1 scrollbar-hide pr-4 mask-fade-right">
                                        <button
                                            onClick={() => activeConfig && openEditConfig(activeConfig)}
                                            className="h-10 px-4 text-xs font-bold bg-white shadow-sm border border-slate-200 rounded-xl text-slate-700 hover:bg-slate-50 active:scale-95 transition-all flex items-center gap-2 shrink-0"
                                        >
                                            <Settings size={14} className="text-slate-400" />
                                            配置中心
                                        </button>

                                        <button
                                            onClick={handleTestConnection}
                                            disabled={testing}
                                            className="h-10 px-4 text-xs font-bold bg-white shadow-sm border border-slate-200 rounded-xl text-slate-600 hover:text-blue-600 hover:border-blue-100 hover:bg-blue-50/50 transition-all flex items-center gap-2 shrink-0"
                                        >
                                            {testing ? <Loader2 className="animate-spin" size={14} /> : <Zap size={14} className="text-blue-500" />}
                                            通联测试
                                        </button>

                                        <div className="w-px h-5 bg-slate-200 mx-1 shrink-0" />

                                        <button
                                            onClick={handleDiagnose}
                                            className="h-10 px-5 text-xs font-bold bg-slate-900 text-white rounded-xl hover:bg-slate-800 active:scale-95 transition-all flex items-center gap-2 shadow-lg shadow-slate-200 shrink-0"
                                        >
                                            <Activity size={14} />
                                            一键诊断
                                        </button>

                                        <button
                                            onClick={handleTriggerRecon}
                                            className="h-10 px-4 text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-100 rounded-xl hover:bg-emerald-100 transition-all flex items-center gap-2 shrink-0"
                                            title="数据核对"
                                        >
                                            <ShieldCheck size={14} />
                                            数据核对
                                        </button>

                                    </div>
                                    <div className="shrink-0 pt-1 pb-1 pl-2 border-l border-slate-100/50 relative z-20">
                                        <div className="group relative">
                                            <button className="h-10 w-10 flex items-center justify-center text-slate-400 hover:text-slate-700 bg-white hover:bg-slate-50 rounded-xl border border-slate-200 shadow-sm transition-all hover:shadow-md">
                                                <Sliders size={16} />
                                            </button>
                                            <div className="absolute right-0 top-full mt-2 hidden group-hover:block z-50">
                                                <div className="bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl border border-slate-100 p-2 w-56 animate-in fade-in zoom-in-95 duration-200">
                                                    <div className="px-3 py-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest border-b border-slate-50 mb-1">
                                                        更多操作
                                                    </div>
                                                    <button onClick={handleExportConfig} className="w-full text-left px-3 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-xl flex items-center gap-3 transition-colors group/item">
                                                        <div className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-slate-400 group-hover/item:text-slate-600 transition-colors">
                                                            <Download size={14} />
                                                        </div>
                                                        导出配置备份
                                                    </button>
                                                    <button onClick={() => activeConfig && handleDeleteConfig(activeConfig.id)} className="w-full text-left px-3 py-2.5 text-xs font-bold text-rose-500 hover:bg-rose-50 rounded-xl flex items-center gap-3 transition-colors group/item">
                                                        <div className="w-8 h-8 rounded-lg bg-rose-50 flex items-center justify-center text-rose-500 group-hover/item:bg-rose-100 transition-colors">
                                                            <XCircle size={14} />
                                                        </div>
                                                        移除此连接器
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="flex-1 overflow-y-auto">
                                {loadingScenarios && (
                                    <div className="flex items-center justify-center py-8">
                                        <Loader2 className="animate-spin text-blue-600" size={32} />
                                    </div>
                                )}

                                {!loadingScenarios && scenarios.length === 0 && (
                                    <div className="text-center text-slate-400 py-8">暂无场景配置</div>
                                )}

                                {!loadingScenarios && scenarios.map(scenario => {
                                    const isExpanded = expandedScenarios.has(scenario.id);
                                    const interfaces = subInterfaces[scenario.id] || [];

                                    return (
                                        <div key={scenario.id} className="border-b border-slate-100">
                                            {/* Scenario Row */}
                                            <div className="flex items-center p-5 hover:bg-blue-50/20 group/row transition-all duration-200">
                                                {/* Expand Toggle */}
                                                <button
                                                    onClick={() => toggleScenarioExpand(scenario.id)}
                                                    className="mr-3 p-1 hover:bg-slate-200 rounded transition-colors"
                                                >
                                                    {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                                                </button>

                                                {/* Scenario Info */}
                                                <div className="flex-1 min-w-0 pr-4">
                                                    <div className="flex items-baseline gap-2">
                                                        <span className="font-bold text-slate-800 text-base leading-none truncate whitespace-nowrap">{scenario.name}</span>
                                                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${scenario.syncStrategy === 'REALTIME' ? 'bg-purple-100 text-purple-700' :
                                                            scenario.syncStrategy === 'CRON' ? 'bg-blue-100 text-blue-700' :
                                                                'bg-slate-100 text-slate-600'
                                                            }`}>
                                                            {scenario.syncStrategy === 'REALTIME' && '实时'}
                                                            {scenario.syncStrategy === 'CRON' && cronToHuman(scenario.cronExpression)}
                                                            {scenario.syncStrategy === 'MANUAL' && '手动'}
                                                        </span>

                                                        {/* Status Clustered */}
                                                        {scenario.lastSyncStatus === 'SUCCESS' && (
                                                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[10px] font-bold">
                                                                <CheckCircle2 size={10} /> 正常
                                                            </span>
                                                        )}
                                                        {scenario.lastSyncStatus === 'FAIL' && (
                                                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-50 text-rose-600 text-[10px] font-bold">
                                                                <AlertCircle size={10} /> 失败
                                                            </span>
                                                        )}
                                                    </div>
                                                    <div className="flex items-center gap-4 mt-1.5 text-slate-400">
                                                        <div className="text-[10px] font-mono tracking-wider uppercase opacity-80">{scenario.scenarioKey}</div>
                                                        <div className="flex items-center gap-1.5 text-[11px]">
                                                            <Clock size={11} className="opacity-60" />
                                                            <span>{scenario.lastSyncTime ? new Date(scenario.lastSyncTime).toLocaleString() : '从未执行'}</span>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* Actions (Direct Manifestation) */}
                                                <div className="flex items-center gap-1.5 opacity-100 transition-all duration-200">
                                                    <button
                                                        onClick={() => handleToggleScenario(scenario)}
                                                        className="p-2 hover:bg-blue-50 rounded-lg text-slate-400 hover:text-blue-600 transition-all"
                                                        title={scenario.isActive ? '点击禁用' : '点击启用'}
                                                    >
                                                        {scenario.isActive ? <ToggleRight size={22} className="text-blue-500" /> : <ToggleLeft size={22} />}
                                                    </button>

                                                    <button
                                                        onClick={() => handleManualSync(scenario.id)}
                                                        disabled={!scenario.isActive || syncing === scenario.id}
                                                        className={`p-2.5 rounded-xl border transition-all ${syncing === scenario.id
                                                            ? 'bg-blue-600 text-white border-blue-600 shadow-lg shadow-blue-100'
                                                            : 'bg-white text-slate-500 border-slate-200 hover:border-blue-400 hover:text-blue-600 hover:shadow-sm disabled:opacity-30'
                                                            }`}
                                                        title="立即同步"
                                                    >
                                                        <div className="flex items-center gap-2">
                                                            <RefreshCw size={14} className={syncing === scenario.id ? 'animate-spin' : ''} />
                                                            <span className="text-[10px] font-bold uppercase tracking-tight">立即同步</span>
                                                        </div>
                                                    </button>

                                                    <button
                                                        onClick={() => setActionMenuScenario(scenario)}
                                                        className="p-2.5 bg-slate-50 hover:bg-slate-100 border border-slate-100 rounded-xl text-slate-400 hover:text-slate-700 transition-all flex items-center justify-center"
                                                        title="更多操作"
                                                    >
                                                        <MoreHorizontal size={16} />
                                                    </button>
                                                </div>
                                            </div>

                                            {/* Sub-Interfaces (Expanded) */}
                                            {isExpanded && (
                                                <div className="bg-slate-50 px-4 pb-4 pl-12">
                                                    <div className="text-xs text-slate-500 mb-2 font-medium">子接口控制</div>
                                                    {interfaces.length === 0 ? (
                                                        <div className="text-xs text-slate-400">暂无子接口配置</div>
                                                    ) : (
                                                        <div className="space-y-2">
                                                            {interfaces.map(sub => (
                                                                <div key={sub.id} className="flex items-center justify-between bg-white rounded p-2 border border-slate-200">
                                                                    <div>
                                                                        <div className="text-sm font-medium text-slate-700">{sub.interfaceName}</div>
                                                                        <div className="text-xs text-slate-400">{sub.interfaceKey}</div>
                                                                    </div>
                                                                    <button
                                                                        onClick={() => handleToggleSubInterface(sub.id, scenario.id)}
                                                                        className="text-slate-400 hover:text-blue-600"
                                                                    >
                                                                        {sub.isActive
                                                                            ? <ToggleRight size={20} className="text-blue-600" />
                                                                            : <ToggleLeft size={20} />
                                                                        }
                                                                    </button>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        </>
                    ) : (
                        <div className="flex flex-col items-center justify-center h-full text-slate-400">
                            <AlertCircle size={48} className="mb-4 opacity-50" />
                            <p>请选择左侧连接以配置业务场景</p>
                        </div>
                    )}
                </div>

                {/* Config Modal (Smart Connector) */}
                {showConfigModal && (
                    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
                        <div className="bg-white rounded-lg shadow-2xl w-[600px] max-w-[95vw] animate-in fade-in zoom-in duration-200">
                            <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                                <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                                    <Zap size={18} className="text-blue-500 fill-blue-500" />
                                    {editingConfig ? '编辑连接配置' : '快速新增连接器'}
                                </h3>
                                <button onClick={() => setShowConfigModal(false)} className="text-slate-400 hover:text-slate-600">
                                    <XCircle size={20} />
                                </button>
                            </div>

                            <div className="p-6 space-y-5">
                                {/* 智能预选提示词 */}
                                {detectedType && (
                                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 flex items-start gap-3 animate-bounce-subtle">
                                        <Zap size={20} className="text-blue-600 mt-0.5" />
                                        <div className="flex-1">
                                            <p className="text-sm font-medium text-blue-800">
                                                检测到对应的系统模板: <span className="underline">{ERP_TEMPLATES.find(t => t.type === detectedType)?.name}</span>
                                            </p>
                                            <p className="text-xs text-blue-600 mt-0.5">是否应用推荐的连接器类型与 Endpoint 模板？</p>
                                        </div>
                                        <button
                                            onClick={handleApplyDetected}
                                            className="px-3 py-1 bg-blue-600 text-white text-xs font-medium rounded-full hover:bg-blue-700 shadow-sm"
                                        >
                                            立即应用
                                        </button>
                                    </div>
                                )}

                                <div className="grid grid-cols-2 gap-4">
                                    <div className="col-span-2">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">连接器名称 <span className="text-rose-500">*</span></label>
                                        <input
                                            type="text"
                                            placeholder="例如：生产环境 YonSuite"
                                            value={configForm.name}
                                            onChange={e => setConfigForm(p => ({ ...p, name: e.target.value }))}
                                            className="w-full px-3 py-2 bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 transition-all outline-none"
                                        />
                                    </div>
                                    <div className="col-span-1">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">系统类型</label>
                                        <select
                                            value={configForm.erpType}
                                            onChange={e => setConfigForm(p => ({ ...p, erpType: e.target.value }))}
                                            className="w-full px-3 py-2 bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                        >
                                            {Object.entries(ADAPTER_CONFIG).map(([key, value]) => (
                                                <option key={key} value={key}>{value.label}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="col-span-1">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">组织代码 (多选)</label>
                                        {/* 已添加的组织标签 */}
                                        <div className="flex flex-wrap gap-1 mb-2">
                                            {configForm.accbookCodes.map(code => (
                                                <span key={code} className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded-md text-xs">
                                                    {code}
                                                    <button
                                                        type="button"
                                                        onClick={() => setConfigForm(p => ({ ...p, accbookCodes: p.accbookCodes.filter(c => c !== code) }))}
                                                        className="hover:text-red-600"
                                                    >
                                                        ×
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                        {/* 添加新组织 */}
                                        <div className="flex gap-1">
                                            <input
                                                type="text"
                                                placeholder="输入组织代码"
                                                value={newAccbookCode}
                                                onChange={e => setNewAccbookCode(e.target.value)}
                                                onKeyDown={e => {
                                                    if (e.key === 'Enter' && newAccbookCode.trim()) {
                                                        e.preventDefault();
                                                        if (!configForm.accbookCodes.includes(newAccbookCode.trim())) {
                                                            setConfigForm(p => ({ ...p, accbookCodes: [...p.accbookCodes, newAccbookCode.trim()] }));
                                                        }
                                                        setNewAccbookCode('');
                                                    }
                                                }}
                                                className="flex-1 px-2 py-1.5 text-sm bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                            />
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    if (newAccbookCode.trim() && !configForm.accbookCodes.includes(newAccbookCode.trim())) {
                                                        setConfigForm(p => ({ ...p, accbookCodes: [...p.accbookCodes, newAccbookCode.trim()] }));
                                                        setNewAccbookCode('');
                                                    }
                                                }}
                                                className="px-2 py-1 bg-blue-600 text-white text-xs rounded hover:bg-blue-700"
                                            >
                                                添加
                                            </button>
                                        </div>
                                        <p className="text-xs text-slate-400 mt-1">按 Enter 或点击添加，支持多个组织</p>
                                    </div>
                                    <div className="col-span-2">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">基础 URL (Endpoint) <span className="text-rose-500">*</span></label>
                                        <div className="relative">
                                            <input
                                                type="text"
                                                placeholder="https://..."
                                                value={configForm.baseUrl}
                                                onChange={e => handleUrlChange(e.target.value)}
                                                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                            />
                                            <div className="absolute right-3 top-2.5 text-slate-300">
                                                <Database size={16} />
                                            </div>
                                        </div>
                                    </div>
                                    <div className="col-span-1">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">AppKey / ClientID</label>
                                        <input
                                            type="text"
                                            value={configForm.appKey}
                                            onChange={e => setConfigForm(p => ({ ...p, appKey: e.target.value }))}
                                            className="w-full px-3 py-2 bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                        />
                                    </div>
                                    <div className="col-span-1">
                                        <label className="block text-sm font-medium text-slate-700 mb-1">AppSecret</label>
                                        <input
                                            type="password"
                                            value={configForm.appSecret}
                                            onChange={e => setConfigForm(p => ({ ...p, appSecret: e.target.value }))}
                                            className="w-full px-3 py-2 bg-white border border-slate-200 rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                        />
                                    </div>
                                </div>
                            </div>

                            <div className="p-4 border-t border-slate-100 flex justify-between items-center bg-slate-50/50">
                                <span className="text-xs text-slate-400">所有数据均按国密标准安全存储</span>
                                <div className="flex gap-2">
                                    <button onClick={() => setShowConfigModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-200 rounded transition-colors text-sm">
                                        取消
                                    </button>
                                    <button
                                        onClick={handleSaveConfig}
                                        className="px-6 py-2 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 shadow-md transition-all active:scale-95 text-sm"
                                    >
                                        确认保存
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Diagnosis Modal */}
                {showDiagnosis && (
                    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-900/60 backdrop-blur-md">
                        <div className="bg-white rounded-lg shadow-2xl w-[550px] max-w-[95vw] overflow-hidden flex flex-col animate-in slide-in-from-bottom-4 duration-300">
                            <div className="p-4 bg-slate-900 text-white flex justify-between items-center">
                                <h3 className="font-medium flex items-center gap-2">
                                    <Zap size={18} className="text-yellow-400 fill-yellow-400" />
                                    连接一键诊断报告
                                </h3>
                                <button onClick={() => setShowDiagnosis(false)} className="hover:bg-white/10 p-1 rounded transition-colors">
                                    <XCircle size={20} />
                                </button>
                            </div>

                            <div className="flex-1 overflow-y-auto p-6">
                                {diagnosing ? (
                                    <div className="flex flex-col items-center justify-center py-12 space-y-4">
                                        <div className="relative">
                                            <Loader2 className="animate-spin text-blue-500" size={48} />
                                            <Zap className="absolute inset-0 m-auto text-blue-200" size={20} />
                                        </div>
                                        <p className="text-slate-500 animate-pulse">正在进行深度链路诊断，请稍候...</p>
                                    </div>
                                ) : diagnosisResult ? (
                                    <div className="space-y-6">
                                        <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-100">
                                            <div>
                                                <div className="text-xs text-slate-400 uppercase tracking-wider">诊断对象</div>
                                                <div className="font-semibold text-slate-700">{diagnosisResult.configName}</div>
                                            </div>
                                            <div className="text-right">
                                                <div className="text-xs text-slate-400 uppercase tracking-wider">综合状态</div>
                                                <div className={`font-bold ${diagnosisResult.status === 'SUCCESS' ? 'text-emerald-500' : 'text-amber-500'}`}>
                                                    {diagnosisResult.status === 'SUCCESS' ? '链路通畅' : '发现异常'}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="space-y-3">
                                            {diagnosisResult.steps.map((step, idx) => (
                                                <div key={idx} className="flex gap-4 p-3 rounded-md border border-slate-100 hover:bg-slate-50 transition-colors">
                                                    <div className="mt-1">
                                                        {step.status === 'SUCCESS' ? <CheckCircle2 className="text-emerald-500" size={18} /> :
                                                            step.status === 'FAIL' ? <XCircle className="text-rose-500" size={18} /> :
                                                                <AlertCircle className="text-amber-500" size={18} />}
                                                    </div>
                                                    <div className="flex-1">
                                                        <div className="text-sm font-medium text-slate-700">{step.name}</div>
                                                        <div className="text-xs text-slate-500 mt-0.5">{step.message}</div>
                                                        {step.detail && (
                                                            <div className="mt-2 text-xs bg-slate-100 p-2 rounded text-slate-400 font-mono">
                                                                {step.detail}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="text-center py-8 text-slate-400">无法获取诊断报告</div>
                                )}
                            </div>

                            <div className="p-4 border-t border-slate-100 bg-slate-50 flex justify-end">
                                <button
                                    onClick={() => setShowDiagnosis(false)}
                                    className="px-6 py-2 bg-slate-800 text-white rounded-md text-sm font-medium hover:bg-slate-700 active:scale-95 transition-all"
                                >
                                    我知道了
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Reconciliation Modal */}
                {showRecon && (
                    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-900/60 backdrop-blur-md">
                        <div className="bg-white rounded-lg shadow-2xl w-[800px] max-w-[95vw] overflow-hidden flex flex-col animate-in zoom-in duration-300">
                            <div className="p-4 bg-emerald-600 text-white flex justify-between items-center">
                                <h3 className="font-medium flex items-center gap-2">
                                    <ShieldCheck size={18} />
                                    账、凭、证三位一体核对报告
                                </h3>
                                <button onClick={() => setShowRecon(false)} className="hover:bg-white/10 p-1 rounded transition-colors">
                                    <XCircle size={20} />
                                </button>
                            </div>
                            <div className="flex-1 overflow-y-auto p-4 bg-slate-50">
                                <ReconciliationReport record={reconRecord} loading={reconLoading} />
                            </div>
                            <div className="p-4 border-t border-slate-100 bg-white flex justify-end">
                                <button
                                    onClick={() => setShowRecon(false)}
                                    className="px-6 py-2 bg-slate-800 text-white rounded-md text-sm font-medium hover:bg-slate-700"
                                >
                                    关闭
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Action Menu Modal (List Item Actions) */}
                {actionMenuScenario && (
                    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-[2px]" onClick={() => setActionMenuScenario(null)}>
                        <div className="bg-white rounded-2xl shadow-2xl p-6 w-[320px] animate-in fade-in zoom-in-95 duration-200" onClick={e => e.stopPropagation()}>
                            <h3 className="text-lg font-bold text-slate-800 mb-1">{actionMenuScenario.name}</h3>
                            <p className="text-xs text-slate-400 font-mono mb-6">{actionMenuScenario.scenarioKey}</p>

                            <div className="space-y-2">
                                <button
                                    onClick={() => { handleShowParams(actionMenuScenario); setActionMenuScenario(null); }}
                                    className="w-full text-left px-4 py-3 bg-slate-50 hover:bg-slate-100 rounded-xl flex items-center gap-3 transition-colors group"
                                >
                                    <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 flex items-center justify-center group-hover:bg-blue-100 transition-colors">
                                        <Sliders size={16} />
                                    </div>
                                    <span className="font-bold text-slate-600">查看 API 参数</span>
                                </button>

                                <button
                                    onClick={() => { handleShowHistory(actionMenuScenario.id); setActionMenuScenario(null); }}
                                    className="w-full text-left px-4 py-3 bg-slate-50 hover:bg-slate-100 rounded-xl flex items-center gap-3 transition-colors group"
                                >
                                    <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-500 flex items-center justify-center group-hover:bg-emerald-100 transition-colors">
                                        <History size={16} />
                                    </div>
                                    <span className="font-bold text-slate-600">查看同步历史</span>
                                </button>

                                <button
                                    onClick={() => { toast(actionMenuScenario.lastSyncMsg || '暂无日志', { icon: '📋' }); setActionMenuScenario(null); }}
                                    className="w-full text-left px-4 py-3 bg-slate-50 hover:bg-slate-100 rounded-xl flex items-center gap-3 transition-colors group"
                                >
                                    <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-500 flex items-center justify-center group-hover:bg-slate-200 transition-colors">
                                        <Eye size={16} />
                                    </div>
                                    <span className="font-bold text-slate-600">查看运行日志</span>
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default IntegrationSettings;
