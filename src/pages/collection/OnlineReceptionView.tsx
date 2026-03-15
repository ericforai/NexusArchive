// Input: React、lucide-react 图标、本地模块 FourNatureReportView、api/stats、api/erp 等
// Output: React 组件 OnlineReceptionView（含路由校验错误友好提示）
// Pos: src/pages/collection/OnlineReceptionView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, CheckCircle, XCircle, Eye, Trash2, Filter, Server, Clock } from 'lucide-react';
import { FourNatureReportView } from './FourNatureReportView';
import { statsApi, ErpStats } from '../../api/stats';
import { integrationApi, erpApi, IntegrationChannel } from '../../api/erp';
import { client } from '../../api/client';
import { useFondsStore } from '../../store/useFondsStore';

// 扩展 IntegrationChannel 以支持本地状态
interface LocalChannel extends IntegrationChannel {
    localStatus?: 'normal' | 'error' | 'syncing';
}

export const OnlineReceptionView: React.FC = () => {
    const [channels, setChannels] = useState<LocalChannel[]>([]);
    const [stats, setStats] = useState<ErpStats>({
        connectedSystems: 0,
        todayReceived: 0,
        activeInterfaces: 0,
        abnormalCount: 0
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

    // 核心状态：当前全宗与水合状态
    const { currentFonds, _hasHydrated: hasHydrated } = useFondsStore();
    const fondsCode = currentFonds?.fondsCode;

    // UI 交互状态
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isReportOpen, setIsReportOpen] = useState(false);
    const [newChannel, setNewChannel] = useState({ name: '', erpType: 'SAP ERP', description: '' });

    // 同步弹窗状态
    const [isSyncModalOpen, setIsSyncModalOpen] = useState(false);
    const [syncChannelId, setSyncChannelId] = useState<number | null>(null);
    const [syncPeriod, setSyncPeriod] = useState({
        start: `${new Date().getFullYear() - 1}-01`,
        end: `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`
    });
    const [availableOrgs, setAvailableOrgs] = useState<{ code: string, name: string }[]>([]);
    const [selectedOrgs, setSelectedOrgs] = useState<string[]>([]);
    const [syncError, setSyncError] = useState<string | null>(null);

    // 同步进度状态
    const [isPolling, setIsPolling] = useState(false);
    const [syncProgress, setSyncProgress] = useState({ status: '', progress: 0, message: '' });

    // 切换选中状态
    const toggleSelection = (id: number) => {
        setSelectedIds(prev => {
            const newSet = new Set(prev);
            if (newSet.has(id)) {
                newSet.delete(id);
            } else {
                newSet.add(id);
            }
            return newSet;
        });
    };

    // 1. 加载数据逻辑
    const loadData = useCallback(async () => {
        if (!hasHydrated || !fondsCode) return;

        setLoading(true);
        setError(null);
        try {
            // 加载统计数据
            const statsRes = await statsApi.getErpStats();
            if (statsRes.code === 200) {
                setStats(statsRes.data);
            }

            // 加载组织/全宗数据用于映射名称
            const fondsRes = await client.get('/bas/fonds/list');
            const fondsMap: Record<string, string> = {};
            if (fondsRes.data.code === 200 && fondsRes.data.data) {
                fondsRes.data.data.forEach((f: any) => {
                    fondsMap[f.fondsCode] = f.fondsName;
                });
            }

            // 加载集成通道列表
            const channelsRes = await integrationApi.getChannels();
            if (channelsRes.code === 200 && channelsRes.data) {
                setChannels(channelsRes.data.map(ch => ({
                    ...ch,
                    localStatus: ch.status
                })));
            } else {
                setError(channelsRes.message || '获取集成通道数据失败');
            }

            // 导出 fondsMap 供后续处理使用
            (window as any)._fondsMap = fondsMap;
        } catch (err: any) {
            setError(err?.message || '网络连接异常');
            console.error('Failed to load integration data', err);
        } finally {
            setLoading(false);
        }
    }, [hasHydrated, fondsCode]);

    useEffect(() => {
        if (hasHydrated && fondsCode) {
            loadData();
        }
    }, [fondsCode, hasHydrated, loadData]);

    const handleViewLog = (channel: LocalChannel) => {
        alert(channel.lastSyncMsg || '暂无日志记录');
    };

    const pollSyncStatus = useCallback(async (scenarioId: number, taskId: string) => {
        const pollInterval = 2000;
        const maxAttempts = 150;
        let attempts = 0;

        const poll = async () => {
            if (attempts >= maxAttempts) {
                setIsPolling(false);
                setSyncError('同步超时，请稍后查看同步历史');
                return;
            }

            attempts++;
            try {
                const statusRes = await erpApi.getSyncStatus(scenarioId, taskId);
                if (statusRes.code === 200 && statusRes.data) {
                    const status = statusRes.data;
                    setSyncProgress({
                        status: status.status,
                        progress: Math.round(status.progress * 100),
                        message: status.status === 'RUNNING' ? `正在同步: ${Math.round(status.progress * 100)}%` : '排队中...'
                    });

                    if (status.status === 'SUCCESS') {
                        setIsPolling(false);
                        loadData();
                        setTimeout(() => setIsSyncModalOpen(false), 2000);
                        return;
                    }

                    if (status.status === 'FAIL') {
                        setIsPolling(false);
                        setSyncError(status.errorMessage || '同步失败');
                        return;
                    }

                    setTimeout(poll, pollInterval);
                }
            } catch (err) {
                console.error('Polling error', err);
                setTimeout(poll, pollInterval);
            }
        };
        poll();
    }, [loadData]);

    const handleSync = (id: number) => {
        const channel = channels.find(c => c.id === id);
        if (!channel) return;

        const codes = channel.accbookCodes || (channel.accbookCode ? [channel.accbookCode] : []);
        const fondsMap = (window as any)._fondsMap || {};

        const orgs = codes.map(code => {
            const fCode = channel.accbookMapping?.[code] || code;
            const fName = fondsMap[fCode];
            return {
                code,
                name: fName ? `${fName} (${code})` : `组织 (${code})`
            };
        });

        setAvailableOrgs(orgs);
        setSelectedOrgs(codes.length > 0 ? [codes[0]] : []);
        setSyncChannelId(id);
        setIsSyncModalOpen(true);
        setSyncError(null);
    };

    const executeRealSync = async () => {
        if (!syncChannelId) return;
        setSyncError(null);
        setSyncProgress({ status: 'SUBMITTED', progress: 0, message: '正在提交任务...' });

        try {
            const res = await erpApi.syncScenario(syncChannelId, {
                periodStart: syncPeriod.start,
                periodEnd: syncPeriod.end,
                accbookCodes: selectedOrgs
            });

            if (res.code === 200 && res.data?.taskId) {
                setIsPolling(true);
                pollSyncStatus(syncChannelId, res.data.taskId);
            } else {
                throw new Error(res.message || '触发同步失败');
            }
        } catch (err: any) {
            setSyncError(err.message || '请求失败');
        }
    };

    const handleAddChannel = () => {
        setIsModalOpen(false);
    };

    return (
        <div className="p-6 max-w-[1600px] mx-auto space-y-6 relative">
            <div className="flex justify-between items-start">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">在此接收</h1>
                    <p className="text-slate-500">管理来自 ERP/OA 的集成通道</p>
                </div>
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                    新增通道
                </button>
            </div>

            {loading && !channels.length && (
                <div className="flex justify-center p-12">
                    <RefreshCw className="w-8 h-8 animate-spin text-blue-500" />
                </div>
            )}

            {error && (
                <div className="p-4 bg-red-50 text-red-700 border border-red-200 rounded-lg">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">系统总数</div>
                    <div className="text-2xl font-bold text-slate-900">{stats.connectedSystems}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">今日接收</div>
                    <div className="text-2xl font-bold text-emerald-600">{stats.todayReceived}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">运行中接口</div>
                    <div className="text-2xl font-bold text-blue-600">{stats.activeInterfaces}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">异常报警</div>
                    <div className="text-2xl font-bold text-red-600">{stats.abnormalCount}</div>
                </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                <table className="w-full text-left text-sm">
                    <thead>
                        <tr className="border-b border-slate-200 bg-slate-50 text-slate-600">
                            <th className="p-4">接口名称</th>
                            <th className="p-4">对接系统</th>
                            <th className="p-4">同步频率</th>
                            <th className="p-4">状态</th>
                            <th className="p-4 text-right">操作</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {channels.map(channel => (
                            <tr key={channel.id} className="hover:bg-slate-50 transition-colors">
                                <td className="p-4 font-medium">{channel.name}</td>
                                <td className="p-4">
                                    <div className="flex items-center gap-2">
                                        <Server className="w-4 h-4 text-slate-400" />
                                        {channel.configName}
                                    </div>
                                </td>
                                <td className="p-4 text-slate-500">{channel.frequency}</td>
                                <td className="p-4">
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${channel.status === 'normal' ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-red-50 text-red-700 border border-red-100'
                                        }`}>
                                        {channel.status === 'normal' ? '正常' : '异常'}
                                    </span>
                                </td>
                                <td className="p-4 text-right">
                                    <button onClick={() => handleSync(channel.id)} className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg mr-1"><RefreshCw className="w-4 h-4" /></button>
                                    <button onClick={() => handleViewLog(channel)} className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-lg mr-1"><Eye className="w-4 h-4" /></button>
                                    <button className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg"><Trash2 className="w-4 h-4" /></button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {isSyncModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl shadow-xl w-[500px] p-6">
                        <h2 className="text-xl font-bold mb-4">手动同步数据</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium mb-2">选择组织</label>
                                <div className="space-y-1 max-h-40 overflow-y-auto border p-2 rounded bg-slate-50">
                                    {availableOrgs.map(org => (
                                        <label key={org.code} className="flex items-center gap-2 py-1">
                                            <input
                                                type="checkbox"
                                                checked={selectedOrgs.includes(org.code)}
                                                onChange={e => {
                                                    if (e.target.checked) setSelectedOrgs([...selectedOrgs, org.code]);
                                                    else setSelectedOrgs(selectedOrgs.filter(c => c !== org.code));
                                                }}
                                            />
                                            <span className="text-sm">{org.name}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">开始期间</label>
                                    <input type="month" value={syncPeriod.start} onChange={e => setSyncPeriod({ ...syncPeriod, start: e.target.value })} className="w-full border p-2 rounded" />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium mb-1">结束期间</label>
                                    <input type="month" value={syncPeriod.end} onChange={e => setSyncPeriod({ ...syncPeriod, end: e.target.value })} className="w-full border p-2 rounded" />
                                </div>
                            </div>
                            {syncError && <div className="p-3 bg-red-50 text-red-700 text-sm rounded border border-red-100">❌ {syncError}</div>}
                            {isPolling && (
                                <div className="p-3 bg-blue-50 border border-blue-100 rounded">
                                    <div className="flex justify-between text-xs text-blue-700 mb-1">
                                        <span>同步进度</span>
                                        <span>{syncProgress.progress}%</span>
                                    </div>
                                    <div className="w-full bg-blue-200 rounded-full h-1.5">
                                        <div className="bg-blue-600 h-1.5 rounded-full transition-all" style={{ width: `${syncProgress.progress}%` }} />
                                    </div>
                                    <p className="text-xs text-blue-600 mt-1">{syncProgress.message}</p>
                                </div>
                            )}
                        </div>
                        <div className="flex justify-end gap-3 mt-6">
                            <button
                                onClick={() => setIsSyncModalOpen(false)}
                                disabled={isPolling}
                                className="px-4 py-2 border rounded-lg hover:bg-slate-50"
                            >
                                取消
                            </button>
                            <button
                                onClick={executeRealSync}
                                disabled={isPolling || !selectedOrgs.length}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                            >
                                {isPolling ? '正在同步...' : '开始同步'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {isModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl shadow-xl w-[500px] p-6">
                        <h2 className="text-xl font-bold mb-4">新增集成接口</h2>
                        <div className="space-y-4">
                            <input
                                placeholder="接口名称"
                                value={newChannel.name}
                                onChange={e => setNewChannel({ ...newChannel, name: e.target.value })}
                                className="w-full border p-2 rounded"
                            />
                            <select
                                value={newChannel.erpType}
                                onChange={e => setNewChannel({ ...newChannel, erpType: e.target.value })}
                                className="w-full border p-2 rounded"
                            >
                                <option>SAP ERP</option>
                                <option>用友 YonSuite</option>
                                <option>金蝶云星空</option>
                            </select>
                        </div>
                        <div className="flex justify-end gap-3 mt-6">
                            <button onClick={() => setIsModalOpen(false)} className="px-4 py-2 border rounded">取消</button>
                            <button onClick={handleAddChannel} className="px-4 py-2 bg-blue-600 text-white rounded">确认</button>
                        </div>
                    </div>
                </div>
            )}

            {isReportOpen && <FourNatureReportView fileId="" onClose={() => setIsReportOpen(false)} />}
        </div>
    );
};

export default OnlineReceptionView;
