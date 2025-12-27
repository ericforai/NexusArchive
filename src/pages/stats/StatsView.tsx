// Input: React、recharts、lucide-react 图标、本地模块 api/stats
// Output: React 组件 StatsView
// Pos: src/pages/stats/StatsView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useMemo, useState } from 'react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, PieChart, Pie, Cell } from 'recharts';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { statsApi, DashboardStats, StorageStats, ArchivalTrendPoint, TaskStatusStats } from '../../api/stats';

interface StatsViewProps {
  drillDown?: string;
  onNavigate?: (...args: any[]) => void;
}

const STORAGE_COLORS = ['#0ea5e9', '#cbd5e1'];
const TASK_COLORS = ['#0ea5e9', '#22c55e', '#f97316', '#f43f5e', '#8b5cf6', '#14b8a6'];

export const StatsView: React.FC<StatsViewProps> = () => {
  const [dashboard, setDashboard] = useState<DashboardStats | null>(null);
  const [storage, setStorage] = useState<StorageStats | null>(null);
  const [trend, setTrend] = useState<ArchivalTrendPoint[]>([]);
  const [taskStatus, setTaskStatus] = useState<TaskStatusStats | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const storagePieData = useMemo(() => {
    if (!storage) return [];
    const used = Number(storage.usagePercent || 0);
    const free = Math.max(0, 100 - used);
    return [
      { name: `已使用 ${storage.used}`, value: used },
      { name: `剩余 (估算)`, value: free }
    ];
  }, [storage]);

  const taskStatusData = useMemo(() => {
    if (!taskStatus?.byStatus) return [];
    return Object.entries(taskStatus.byStatus).map(([name, value]) => ({
      name,
      value
    }));
  }, [taskStatus]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, storageRes, trendRes, taskRes] = await Promise.all([
        statsApi.getDashboard(),
        statsApi.getStorage(),
        statsApi.getTrend(),
        statsApi.getTaskStatus()
      ]);

      const errors: string[] = [];

      if (dashRes.code === 200) {
        setDashboard(dashRes.data);
        if (dashRes.data?.recentTrend?.length) {
          setTrend(dashRes.data.recentTrend);
        }
      } else {
        errors.push('仪表盘指标');
      }

      if (storageRes.code === 200) {
        setStorage(storageRes.data);
      } else {
        errors.push('存储占用');
      }

      if (trendRes.code === 200 && (!dashRes.data?.recentTrend?.length)) {
        setTrend(trendRes.data || []);
      } else if (trendRes.code !== 200) {
        errors.push('归档趋势');
      }

      if (taskRes.code === 200) {
        setTaskStatus(taskRes.data);
      } else {
        errors.push('任务状态');
      }

      if (errors.length) {
        setError(`${errors.join(' / ')} 加载失败`);
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || '统计数据加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const statCards = [
    { label: '总归档量', value: dashboard?.totalArchives ?? '--' },
    { label: '存储占用', value: dashboard?.storageUsed ?? '--' },
    { label: '待处理任务', value: dashboard?.pendingTasks ?? '--' },
    { label: '今日入库', value: dashboard?.todayIngest ?? '--' }
  ];

  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">数据统计中心</h2>
          <p className="text-slate-500">基于真实接口的归档趋势与任务监控</p>
        </div>
        {loading && (
          <div className="text-sm text-slate-500 flex items-center gap-2">
            <Loader2 className="animate-spin" size={16} /> 数据刷新中...
          </div>
        )}
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-sm flex items-center gap-2">
          <AlertTriangle size={16} /> {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => (
          <div
            key={index}
            className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 relative overflow-hidden group"
          >
            <div className="absolute right-0 top-0 w-24 h-24 rounded-bl-full opacity-10 transition-transform group-hover:scale-110 bg-primary-500" />
            <div className="space-y-1">
              <h3 className="text-3xl font-bold text-slate-800 tracking-tight">{stat.value}</h3>
              <p className="text-sm text-slate-500 font-medium">{stat.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[420px] flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-slate-800">归档量趋势</h3>
              <p className="text-xs text-slate-500">近 14 日归档数量</p>
            </div>
          </div>
          <div className="flex-1">
            {trend.length === 0 ? (
              <div className="h-full flex items-center justify-center text-slate-400 text-sm">
                暂无趋势数据
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="date" axisLine={false} tickLine={false} />
                  <YAxis axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={{borderRadius: '8px', border:'none', boxShadow:'0 4px 12px rgba(0,0,0,0.1)'}} />
                  <Bar dataKey="count" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[420px] flex flex-col">
          <h3 className="text-lg font-bold text-slate-800 mb-2">存储占用</h3>
          <p className="text-xs text-slate-500 mb-4">实时容量占比</p>
          <div className="flex-1 flex items-center justify-center">
            {storagePieData.length === 0 ? (
              <div className="text-slate-400 text-sm">暂无存储数据</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={storagePieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={100}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {storagePieData.map((_, index) => (
                      <Cell key={index} fill={STORAGE_COLORS[index % STORAGE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
          <div className="mt-4 text-center text-sm text-slate-600">
            <div>已使用：{storage?.used || '--'}</div>
            <div className="text-slate-500">总容量：{storage?.total || '未知'}</div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[360px] flex flex-col">
          <h3 className="text-lg font-bold text-slate-800 mb-2">任务状态</h3>
          <p className="text-xs text-slate-500 mb-4">SIP / 归档任务进度</p>
          <div className="flex-1">
            {taskStatusData.length === 0 ? (
              <div className="text-slate-400 text-sm h-full flex items-center justify-center">暂无任务数据</div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={taskStatusData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                    {taskStatusData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={TASK_COLORS[index % TASK_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[360px] flex flex-col">
          <h3 className="text-lg font-bold text-slate-800 mb-2">执行摘要</h3>
          <div className="grid grid-cols-2 gap-4 text-sm text-slate-700 flex-1">
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 mb-1">处理中</div>
              <div className="text-2xl font-bold text-slate-800">{taskStatus?.running ?? '--'}</div>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 mb-1">已完成</div>
              <div className="text-2xl font-bold text-emerald-600">{taskStatus?.completed ?? '--'}</div>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 mb-1">失败</div>
              <div className="text-2xl font-bold text-rose-600">{taskStatus?.failed ?? '--'}</div>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <div className="text-xs text-slate-500 mb-1">等待中</div>
              <div className="text-2xl font-bold text-amber-600">{taskStatus?.pending ?? '--'}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatsView;
