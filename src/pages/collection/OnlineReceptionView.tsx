// Input: React、lucide-react 图标、本地模块 FourNatureReportView、api/stats、api/erp 等
// Output: React 组件 OnlineReceptionView
// Pos: src/pages/collection/OnlineReceptionView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { RefreshCw, CheckCircle, XCircle, Eye, Trash2, Filter, Server, Clock } from 'lucide-react';
import { FourNatureReportView } from './FourNatureReportView';
import { statsApi, ErpStats } from '@api/stats';
import { integrationApi, IntegrationChannel } from '@api/erp';
import { client } from '@api/client';

// 扩展 IntegrationChannel 以支持本地状态
interface LocalChannel extends IntegrationChannel {
    localStatus?: 'normal' | 'error' | 'syncing';
}

export const OnlineReceptionView: React.FC = () => {
    const [channels, setChannels] = useState<LocalChannel[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

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

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isReportOpen, setIsReportOpen] = useState(false);
    const [newChannel, setNewChannel] = useState({ name: '', system: 'SAP ERP', description: '' });

    const [stats, setStats] = useState<ErpStats>({
        connectedSystems: 0,
        todayReceived: 0,
        activeInterfaces: 0,
        abnormalCount: 0
    });

    // 计算初始同步期间（最近一年）
    const getInitialPeriod = () => {
        const now = new Date();
        const endYear = now.getFullYear();
        const endMonth = String(now.getMonth() + 1).padStart(2, '0');
        const startYear = endYear - 1;
        return {
            start: `${startYear}-01`,
            end: `${endYear}-${endMonth}`
        };
    };

    // Sync Modal State
    const [isSyncModalOpen, setIsSyncModalOpen] = useState(false);
    const [syncChannelId, setSyncChannelId] = useState<number | null>(null);
    // 默认同步最近一年的数据
    const [syncPeriod, setSyncPeriod] = useState(getInitialPeriod());
    // Multi-org sync: available orgs and selected orgs
    const [availableOrgs, setAvailableOrgs] = useState<{ code: string, name: string }[]>([]);
    const [selectedOrgs, setSelectedOrgs] = useState<string[]>([]);
    const [syncError, setSyncError] = useState<string | null>(null);

    // 同步进度状态
    const [_syncTaskId, setSyncTaskId] = useState<string | null>(null); // 内部跟踪用
    const [syncProgress, setSyncProgress] = useState({ status: '', progress: 0, message: '' });
    const [isPolling, setIsPolling] = useState(false);

    // 加载集成通道数据
    useEffect(() => {
        const loadData = async () => {
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
                }

                // 导出 fondsMap 供后续弹窗使用
                (window as any)._fondsMap = fondsMap;
            } catch (e: any) {
                console.error("Failed to load integration data", e);
                setError(e.message || String(e));
            }
        };
        loadData();
    }, []);

    // YonSuite 同步配置
    // State for Log Modal
    const [logModalOpen, setLogModalOpen] = useState(false);
    const [currentLog, setCurrentLog] = useState<{ title: string, content: string | null }>({ title: '', content: '' });

    const handleViewLog = (channel: LocalChannel) => {
        setCurrentLog({
            title: `${channel.name} - 同步日志`,
            content: channel.lastSyncMsg || '暂无日志记录'
        });
        setLogModalOpen(true);
    };

    const handleSync = async (id: number) => {
        const channel = channels.find(c => c.id === id);
        if (!channel) return;

        // 根据后端返回的多组织代码，动态构建弹窗选项
        const codes = channel.accbookCodes || (channel.accbookCode ? [channel.accbookCode] : []);
        const fondsMap = (window as any)._fondsMap || {};

        const orgs = codes.map(code => {
            // 优先使用后端返回的 mapping 找到对应的全宗号
            const fondsCode = channel.accbookMapping?.[code] || code;
            // 再根据全宗号查找全宗名称
            const fondsName = fondsMap[fondsCode];
            return {
                code,
                name: fondsName ? `${fondsName} (${code})` : `组织 (${code})`
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

        const channel = channels.find(c => c.id === syncChannelId);
        if (!channel) return;

        // 重置进度状态
        setSyncError(null);
        setSyncProgress({ status: 'SUBMITTED', progress: 0, message: '正在提交同步任务...' });

        try {
            let taskId: string | null = null;

            // 提交同步任务
            if (channel.apiEndpoint) {
                // 有 apiEndpoint：使用统一 client 进行请求
                const response = await client.post(channel.apiEndpoint, {
                    accbookCode: selectedOrgs.length > 0 ? selectedOrgs[0] : (channel.accbookCode || 'BR01'),
                    accbookCodes: selectedOrgs.length > 0 ? selectedOrgs : [channel.accbookCode || 'BR01'],
                    periodStart: syncPeriod.start,
                    periodEnd: syncPeriod.end
                });
                const result = response.data;
                if (response.status !== 200) throw new Error(result.message || '同步失败');
                taskId = result.data?.taskId;
            } else {
                // 无 apiEndpoint：使用通用触发接口，传递日期参数
                const { erpApi } = await import('@api/erp');
                const res = await erpApi.syncScenario(syncChannelId, {
                    periodStart: syncPeriod.start,
                    periodEnd: syncPeriod.end
                });
                if (res.code !== 200) throw new Error(res?.message || '触发失败');
                taskId = res.data?.taskId;
            }

            if (taskId) {
                setSyncTaskId(taskId);
                setIsPolling(true);
                // 开始轮询任务状态
                pollSyncStatus(syncChannelId, taskId);
            } else {
                throw new Error('未获取到任务ID');
            }
        } catch (error: any) {
            setSyncError(error.message || '同步请求失败');
            setChannels(prev => prev.map(c =>
                c.id === syncChannelId ? { ...c, status: 'error' as const } : c
            ));
            setIsPolling(false);
        }
    };

    // 轮询同步任务状态
    const pollSyncStatus = async (scenarioId: number, taskId: string) => {
        const pollInterval = 2000; // 每2秒轮询一次
        const maxAttempts = 150; // 最多轮询5分钟
        let attempts = 0;

        const poll = async () => {
            if (attempts >= maxAttempts) {
                setIsPolling(false);
                setSyncError('同步超时，请稍后查看同步历史');
                setChannels(prev => prev.map(c =>
                    c.id === scenarioId ? { ...c, status: 'error' as const } : c
                ));
                return;
            }

            attempts++;
            try {
                const { erpApi } = await import('@api/erp');
                const statusRes = await erpApi.getSyncStatus(scenarioId, taskId);

                if (statusRes.code === 200 && statusRes.data) {
                    const status = statusRes.data;

                    // 更新进度
                    setSyncProgress({
                        status: status.status,
                        progress: Math.round(status.progress * 100),
                        message: getStatusMessage(status)
                    });

                    // 更新通道状态为同步中
                    if (status.status === 'RUNNING') {
                        setChannels(prev => prev.map(c =>
                            c.id === scenarioId ? { ...c, status: 'syncing' as const } : c
                        ));
                    }

                    // 检查是否完成
                    if (status.status === 'SUCCESS') {
                        setIsPolling(false);
                        setSyncProgress({
                            status: 'SUCCESS',
                            progress: 100,
                            message: `同步完成！获取 ${status.totalCount} 条，新增 ${status.successCount} 条`
                        });

                        // 刷新通道列表
                        const channelsRes = await integrationApi.getChannels();
                        if (channelsRes.code === 200 && channelsRes.data) {
                            setChannels(channelsRes.data.map(ch => ({
                                ...ch,
                                localStatus: ch.status
                            })));
                        }

                        // 2秒后关闭弹窗
                        setTimeout(() => {
                            setIsSyncModalOpen(false);
                            setSyncProgress({ status: '', progress: 0, message: '' });
                        }, 2000);
                        return;
                    }

                    if (status.status === 'FAIL') {
                        setIsPolling(false);
                        setSyncError(status.errorMessage || '同步失败');
                        setChannels(prev => prev.map(c =>
                            c.id === scenarioId ? { ...c, status: 'error' as const } : c
                        ));
                        return;
                    }

                    // 继续轮询
                    setTimeout(poll, pollInterval);
                }
            } catch (error) {
                console.error('轮询同步状态失败:', error);
                // 出错后继续轮询
                setTimeout(poll, pollInterval);
            }
        };

        poll();
    };

    // 获取状态显示消息
    const getStatusMessage = (status: any): string => {
        if (status.status === 'SUBMITTED') return '任务已提交，等待处理...';
        if (status.status === 'RUNNING') {
            if (status.totalCount > 0) {
                return `正在同步... 已获取 ${status.totalCount} 条，新增 ${status.successCount} 条`;
            }
            return '正在同步数据，请稍候...';
        }
        if (status.status === 'SUCCESS') {
            return `同步完成！获取 ${status.totalCount} 条，新增 ${status.successCount} 条`;
        }
        return status.message || '';
    };

    const handleAddChannel = () => {
        const channel: LocalChannel = {
            id: Date.now(),
            name: newChannel.name,
            displayName: newChannel.name, // Added displayName
            configName: newChannel.system, // Added configName
            erpType: 'unknown', // Added erpType
            frequency: '每小时',
            lastSync: '-',
            receivedCount: 0,
            status: 'normal',
            description: newChannel.description,
            apiEndpoint: null,
            accbookCode: null,
            accbookCodes: null,
            lastSyncMsg: null
        };
        // Note: This local add is just for UI demo of "Add Channel" which might not be fully backed by API yet
        // casting to any to bypass strict type checks for this demo feature
        setChannels([...channels, channel as any]);
        setIsModalOpen(false);
        setNewChannel({ name: '', system: 'SAP ERP', description: '' });
    };

    return (
        <div className="p-6 max-w-[1600px] mx-auto space-y-6 relative">
            {/* Log Modal */}
            {logModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
                    <div className="bg-white rounded-lg shadow-xl w-[600px] max-w-[90vw] flex flex-col max-h-[80vh]">
                        <div className="p-4 border-b border-slate-100 flex justify-between items-center">
                            <h3 className="font-medium text-slate-800 flex items-center gap-2">
                                <CheckCircle size={16} className="text-blue-500" />
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

            {/* Header */}
            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-lg mb-4">
                    <strong>Error:</strong> {error}
                </div>
            )}

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
                    <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors">
                        <Filter className="w-4 h-4" />
                        筛选
                    </button>
                    {/* 新增接口功能已移至集成中心，以保持职责分离 */}
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
                                    <div className="font-medium text-slate-900">{channel.displayName || channel.name}</div>
                                    <div className="text-slate-500 text-xs mt-0.5">{channel.description}</div>
                                </td>
                                <td className="p-4">
                                    <div className="flex items-center gap-2">
                                        <div className="w-8 h-8 rounded bg-slate-100 flex items-center justify-center text-slate-500">
                                            <Server className="w-4 h-4" />
                                        </div>
                                        <span className="font-medium text-slate-700">{channel.configName}</span>
                                    </div>
                                </td>
                                <td className="p-4">
                                    <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-medium border border-slate-200">
                                        <Clock className="w-3 h-3" />
                                        {channel.frequency}
                                    </div>
                                </td>
                                <td className="p-4 text-slate-600 font-mono text-xs">
                                    {channel.lastSync || '-'}
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
                                    <div className="flex items-center justify-end gap-2">
                                        <button
                                            onClick={() => handleSync(channel.id)}
                                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg tooltip"
                                            title="立即同步"
                                        >
                                            <RefreshCw className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => handleViewLog(channel)}
                                            className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-lg"
                                            title="查看日志"
                                        >
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

            {/* Four Nature Report Modal - 需要传入选中文件的 ID */}
            {isReportOpen && (
                <FourNatureReportView fileId="" onClose={() => setIsReportOpen(false)} />
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

                            {/* Multi-Org Selector */}
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">选择同步组织</label>
                                <div className="space-y-2 p-3 border border-slate-200 rounded-lg bg-slate-50">
                                    {availableOrgs.map(org => (
                                        <label key={org.code} className="flex items-center gap-2 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={selectedOrgs.includes(org.code)}
                                                onChange={(e) => {
                                                    if (e.target.checked) {
                                                        setSelectedOrgs([...selectedOrgs, org.code]);
                                                    } else {
                                                        setSelectedOrgs(selectedOrgs.filter(c => c !== org.code));
                                                    }
                                                }}
                                                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                            />
                                            <span className="text-sm text-slate-700">{org.name}</span>
                                        </label>
                                    ))}
                                </div>
                                <p className="text-xs text-slate-500 mt-1">可选择多个组织同时同步</p>
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

                            {/* Progress Bar - 实时同步进度显示 */}
                            {isPolling && syncProgress.status !== '' && (
                                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-sm font-medium text-blue-900">同步进度</span>
                                        <span className="text-sm text-blue-700 font-semibold">{syncProgress.progress}%</span>
                                    </div>
                                    <div className="w-full bg-blue-200 rounded-full h-2.5 mb-3 overflow-hidden">
                                        <div
                                            className="bg-blue-600 h-2.5 rounded-full transition-all duration-500 ease-out"
                                            style={{ width: `${syncProgress.progress}%` }}
                                        />
                                    </div>
                                    <p className="text-sm text-blue-800">{syncProgress.message}</p>
                                </div>
                            )}
                        </div>

                        <div className="flex justify-end gap-3 mt-8">
                            <button
                                onClick={() => setIsSyncModalOpen(false)}
                                disabled={isPolling}
                                className="px-4 py-2 text-slate-700 hover:bg-slate-100 rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                取消
                            </button>
                            <button
                                onClick={executeRealSync}
                                disabled={isPolling || channels.find(c => c.id === syncChannelId)?.status === 'syncing'}
                                className="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded-lg font-medium shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {isPolling ? (
                                    <>
                                        <RefreshCw className="w-4 h-4 animate-spin" />
                                        同步中... {syncProgress.progress > 0 && `${syncProgress.progress}%`}
                                    </>
                                ) : channels.find(c => c.id === syncChannelId)?.status === 'syncing' ? (
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
