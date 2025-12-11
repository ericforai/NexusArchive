import React, { useState } from 'react';
import { Activity, RefreshCw, CheckCircle, XCircle, Eye, Trash2, Filter, Plus, Server, Clock } from 'lucide-react';
import { FourNatureReportView } from './FourNatureReportView';
import { statsApi, ErpStats } from '../api/stats';
import { client } from '../api/client';

interface IntegrationChannel {
    id: string;
    name: string;
    system: string;
    frequency: string;
    lastSync: string;
    receivedCount: number;
    status: 'normal' | 'error' | 'syncing';
    description: string;
    apiEndpoint?: string; // 真实同步API端点
    syncConfig?: {
        accbookCode?: string;
        periodStart?: string;
        periodEnd?: string;
    };
    accbookCode?: string;
    periodStart?: string;
    periodEnd?: string;
}

const MOCK_CHANNELS: IntegrationChannel[] = [
    {
        id: 'yonsuite',
        name: 'YONSUITE_VOUCHER_SYNC',
        system: '用友YonSuite',
        frequency: '手动/实时',
        lastSync: '-',
        receivedCount: 0,
        status: 'normal',
        description: '用友YonSuite会计凭证自动采集接口',
        apiEndpoint: '/integration/yonsuite/vouchers/sync',
        syncConfig: {
            accbookCode: 'BR01'
        }
    },
    {
        id: '1',
        name: 'SAP_VOUCHER_SYNC',
        system: 'SAP ERP',
        frequency: '实时',
        lastSync: '2025-12-02 19:00:00',
        receivedCount: 128,
        status: 'normal',
        description: 'SAP 财务凭证自动同步接口'
    },
    {
        id: '2',
        name: 'K3_INVENTORY_SYNC',
        system: '金蝶云星空',
        frequency: '每日 23:00',
        lastSync: '2025-12-01 23:00:00',
        receivedCount: 56,
        status: 'normal',
        description: '存货核算数据同步'
    },
    {
        id: '3',
        name: 'OA_EXPENSE_SYNC',
        system: '泛微OA',
        frequency: '每小时',
        lastSync: '2025-12-02 18:00:00',
        receivedCount: 12,
        status: 'normal',
        description: '员工报销单据同步'
    },
    {
        id: '4',
        name: 'EKB_TRAVEL_SYNC',
        system: '易快报',
        frequency: '每小时',
        lastSync: '2025-12-02 18:30:00',
        receivedCount: 45,
        status: 'normal',
        description: '差旅费用数据同步'
    },
    {
        id: '5',
        name: 'HLY_REIMBURSE_SYNC',
        system: '汇联易',
        frequency: '每小时',
        lastSync: '2025-12-02 18:15:00',
        receivedCount: 0,
        status: 'error',
        description: '费用报销同步 (连接超时)'
    }
];

// 默认数据（种子数据来自数据库，此处作为前端 fallback）
const DEFAULT_CHANNELS = MOCK_CHANNELS;

export const OnlineReceptionView: React.FC = () => {
    const [channels, setChannels] = useState<IntegrationChannel[]>(DEFAULT_CHANNELS);
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isReportOpen, setIsReportOpen] = useState(false);
    const [newChannel, setNewChannel] = useState({ name: '', system: 'SAP ERP', description: '' });

    const [stats, setStats] = useState<ErpStats>({
        connectedSystems: 0,
        todayReceived: 0,
        activeInterfaces: 0,
        abnormalCount: 0
    });

    React.useEffect(() => {
        const fetchStats = async () => {
            try {
                const res = await statsApi.getErpStats();
                if (res.code === 200) {
                    setStats(res.data);
                }
            } catch (e) {
                console.error("Failed to fetch ERP stats", e);
            }
        };
        fetchStats();
    }, []);

    // YonSuite 同步配置
    const [isSyncModalOpen, setIsSyncModalOpen] = useState(false);
    const [syncChannelId, setSyncChannelId] = useState<string | null>(null);
    const [syncPeriod, setSyncPeriod] = useState({ start: '2025-08', end: '2025-08' });
    const [syncError, setSyncError] = useState<string | null>(null);

    const toggleSelection = (id: string) => {
        const newSelected = new Set(selectedIds);
        if (newSelected.has(id)) {
            newSelected.delete(id);
        } else {
            newSelected.add(id);
        }
        setSelectedIds(newSelected);
    };

    const handleSync = async (id: string) => {
        const channel = channels.find(c => c.id === id);
        if (!channel) return;

        // 如果是有API端点的真实集成（如YonSuite），打开同步配置对话框
        if (channel.apiEndpoint) {
            setSyncChannelId(id);
            setIsSyncModalOpen(true);
            setSyncError(null);
            return;
        }

        // 其他接口 - 模拟同步
        const newChannels = channels.map(c => {
            if (c.id === id) {
                return {
                    ...c,
                    lastSync: new Date().toLocaleString('zh-CN'),
                    receivedCount: Math.floor(Math.random() * 50) + 1,
                    status: 'normal' as const
                };
            }
            return c;
        });
        setChannels(newChannels);
    };

    const executeRealSync = async () => {
        if (!syncChannelId) return;

        const channel = channels.find(c => c.id === syncChannelId);
        if (!channel || !channel.apiEndpoint) return;

        // 更新状态为同步中
        setChannels(prev => prev.map(c =>
            c.id === syncChannelId ? { ...c, status: 'syncing' as const } : c
        ));
        setSyncError(null);

        try {
            // 使用统一 client 进行请求，自动处理 token
            const response = await client.post(channel.apiEndpoint, {
                accbookCode: channel.syncConfig?.accbookCode || 'BR01',
                periodStart: syncPeriod.start,
                periodEnd: syncPeriod.end
            });

            // Axios result is in data
            const result = response.data;

            if (response.status === 200 && result.status === 'SUCCESS') {
                setChannels(prev => prev.map(c =>
                    c.id === syncChannelId ? {
                        ...c,
                        status: 'normal' as const,
                        lastSync: new Date().toLocaleString('zh-CN'),
                        receivedCount: result.synced_count || 0
                    } : c
                ));
                setIsSyncModalOpen(false);
            } else {
                throw new Error(result.message || '同步失败');
            }
        } catch (error: any) {
            setSyncError(error.message || '同步请求失败');
            setChannels(prev => prev.map(c =>
                c.id === syncChannelId ? { ...c, status: 'error' as const } : c
            ));
        }
    };

    const handleAddChannel = () => {
        const channel: IntegrationChannel = {
            id: Date.now().toString(),
            name: newChannel.name,
            system: newChannel.system,
            frequency: '每小时',
            lastSync: '-',
            receivedCount: 0,
            status: 'normal',
            description: newChannel.description
        };
        setChannels([...channels, channel]);
        setIsModalOpen(false);
        setNewChannel({ name: '', system: 'SAP ERP', description: '' });
    };

    return (
        <div className="p-6 max-w-[1600px] mx-auto space-y-6">
            {/* Header */}
            <div className="flex justify-between items-start">
                <div>
                    <div className="flex items-center gap-3 mb-2">
                        <h1 className="text-2xl font-bold text-slate-900">资料收集 / 在线接收</h1>
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded-full font-medium">
                            集成监控
                        </span>
                    </div>
                    <p className="text-slate-500">
                        管理和监控来自外部系统 (ERP, OA, 费控) 的数据集成通道与同步状态。
                    </p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => setIsReportOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                        <Activity className="w-4 h-4" />
                        四性检测
                    </button>
                    <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors">
                        <Filter className="w-4 h-4" />
                        筛选
                    </button>
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
                    >
                        <Plus className="w-4 h-4" />
                        新增接口
                    </button>
                </div>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">接入系统总数</div>
                    <div className="text-2xl font-bold text-slate-900">{stats.connectedSystems}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">今日接收数据</div>
                    <div className="text-2xl font-bold text-emerald-600">{stats.todayReceived} 条</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">运行正常接口</div>
                    <div className="text-2xl font-bold text-blue-600">{stats.activeInterfaces}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                    <div className="text-slate-500 text-sm mb-1">异常报警</div>
                    <div className="text-2xl font-bold text-red-600">{stats.abnormalCount}</div>
                </div>
            </div>

            {/* Main Table */}
            <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                <div className="p-4 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
                    <div className="flex items-center gap-4">
                        <span className="text-sm text-slate-500">
                            已选择 <span className="font-medium text-slate-900">{selectedIds.size}</span> 项
                        </span>
                        {selectedIds.size > 0 && (
                            <button className="text-sm text-red-600 hover:text-red-700 font-medium">
                                批量删除
                            </button>
                        )}
                    </div>
                    <button className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1">
                        <RefreshCw className="w-3 h-3" /> 重置视图
                    </button>
                </div>

                <table className="w-full text-left text-sm">
                    <thead>
                        <tr className="border-b border-slate-200 bg-slate-50 text-slate-600">
                            <th className="p-4 w-10">
                                <input type="checkbox" className="rounded border-slate-300" />
                            </th>
                            <th className="p-4 font-medium">接口名称 / 描述</th>
                            <th className="p-4 font-medium">对接系统</th>
                            <th className="p-4 font-medium">同步频率</th>
                            <th className="p-4 font-medium">最后同步</th>
                            <th className="p-4 font-medium">本次接收</th>
                            <th className="p-4 font-medium">接口状态</th>
                            <th className="p-4 font-medium text-right">操作</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {channels.map((channel) => (
                            <tr key={channel.id} className="hover:bg-slate-50/80 transition-colors group">
                                <td className="p-4">
                                    <input
                                        type="checkbox"
                                        checked={selectedIds.has(channel.id)}
                                        onChange={() => toggleSelection(channel.id)}
                                        className="rounded border-slate-300"
                                    />
                                </td>
                                <td className="p-4">
                                    <div className="font-medium text-slate-900">{channel.name}</div>
                                    <div className="text-slate-500 text-xs mt-0.5">{channel.description}</div>
                                </td>
                                <td className="p-4">
                                    <div className="flex items-center gap-2">
                                        <div className="w-8 h-8 rounded bg-slate-100 flex items-center justify-center text-slate-500">
                                            <Server className="w-4 h-4" />
                                        </div>
                                        <span className="font-medium text-slate-700">{channel.system}</span>
                                    </div>
                                </td>
                                <td className="p-4">
                                    <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-medium border border-slate-200">
                                        <Clock className="w-3 h-3" />
                                        {channel.frequency}
                                    </div>
                                </td>
                                <td className="p-4 text-slate-600 font-mono text-xs">
                                    {channel.lastSync}
                                </td>
                                <td className="p-4">
                                    <span className="font-medium text-slate-900">{channel.receivedCount}</span>
                                    <span className="text-slate-500 ml-1">条</span>
                                </td>
                                <td className="p-4">
                                    {channel.status === 'syncing' ? (
                                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-medium border border-blue-100">
                                            <RefreshCw className="w-3 h-3 animate-spin" />
                                            同步中
                                        </div>
                                    ) : channel.status === 'normal' ? (
                                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-medium border border-emerald-100">
                                            <CheckCircle className="w-3 h-3" />
                                            正常
                                        </div>
                                    ) : (
                                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-red-50 text-red-700 text-xs font-medium border border-red-100">
                                            <XCircle className="w-3 h-3" />
                                            异常
                                        </div>
                                    )}
                                </td>
                                <td className="p-4 text-right">
                                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            onClick={() => handleSync(channel.id)}
                                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg tooltip"
                                            title="立即同步"
                                        >
                                            <RefreshCw className="w-4 h-4" />
                                        </button>
                                        <button className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-lg" title="查看日志">
                                            <Eye className="w-4 h-4" />
                                        </button>
                                        <button className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg" title="删除配置">
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {/* Add Channel Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl shadow-xl w-[500px] p-6 animate-in fade-in zoom-in duration-200">
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-xl font-bold text-slate-900">新增集成接口</h2>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                                <XCircle className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">接口名称</label>
                                <input
                                    type="text"
                                    value={newChannel.name}
                                    onChange={e => setNewChannel({ ...newChannel, name: e.target.value })}
                                    placeholder="例如: SAP_VOUCHER_SYNC"
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">对接系统</label>
                                <select
                                    value={newChannel.system}
                                    onChange={e => setNewChannel({ ...newChannel, system: e.target.value })}
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                >
                                    <option>用友YonSuite</option>
                                    <option>SAP ERP</option>
                                    <option>金蝶云星空</option>
                                    <option>用友 U8</option>
                                    <option>泛微OA</option>
                                    <option>易快报</option>
                                    <option>汇联易</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">描述</label>
                                <textarea
                                    value={newChannel.description}
                                    onChange={e => setNewChannel({ ...newChannel, description: e.target.value })}
                                    placeholder="接口用途描述..."
                                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none h-24 resize-none"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end gap-3 mt-8">
                            <button
                                onClick={() => setIsModalOpen(false)}
                                className="px-4 py-2 text-slate-700 hover:bg-slate-100 rounded-lg font-medium"
                            >
                                取消
                            </button>
                            <button
                                onClick={handleAddChannel}
                                className="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg font-medium shadow-sm"
                            >
                                确认添加
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Four Nature Report Modal */}
            {isReportOpen && (
                <FourNatureReportView onClose={() => setIsReportOpen(false)} />
            )}

            {/* YonSuite Sync Modal */}
            {isSyncModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl shadow-xl w-[500px] p-6 animate-in fade-in zoom-in duration-200">
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-xl font-bold text-slate-900">同步凭证数据</h2>
                            <button onClick={() => setIsSyncModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                                <XCircle className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div className="p-4 bg-blue-50 rounded-lg border border-blue-100">
                                <p className="text-sm text-blue-800">
                                    将从用友YonSuite系统同步指定期间的会计凭证数据到本系统。
                                </p>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">开始期间</label>
                                    <input
                                        type="month"
                                        value={syncPeriod.start}
                                        onChange={e => setSyncPeriod({ ...syncPeriod, start: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700 mb-1">结束期间</label>
                                    <input
                                        type="month"
                                        value={syncPeriod.end}
                                        onChange={e => setSyncPeriod({ ...syncPeriod, end: e.target.value })}
                                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    />
                                </div>
                            </div>

                            {syncError && (
                                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                                    ❌ {syncError}
                                </div>
                            )}
                        </div>

                        <div className="flex justify-end gap-3 mt-8">
                            <button
                                onClick={() => setIsSyncModalOpen(false)}
                                className="px-4 py-2 text-slate-700 hover:bg-slate-100 rounded-lg font-medium"
                            >
                                取消
                            </button>
                            <button
                                onClick={executeRealSync}
                                disabled={channels.find(c => c.id === syncChannelId)?.status === 'syncing'}
                                className="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg font-medium shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {channels.find(c => c.id === syncChannelId)?.status === 'syncing' ? (
                                    <>
                                        <RefreshCw className="w-4 h-4 animate-spin" />
                                        同步中...
                                    </>
                                ) : (
                                    <>
                                        <RefreshCw className="w-4 h-4" />
                                        开始同步
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OnlineReceptionView;
