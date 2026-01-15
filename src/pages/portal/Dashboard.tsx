// Input: React、react-router-dom 路由、本地模块 types、api/stats
// Output: React 组件 Dashboard
// Pos: src/pages/portal/Dashboard.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ViewState } from '../../types';
import type { DashboardStats } from '../../api/stats';
import {
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip
} from 'recharts';
import {
  FileUp,
  Bell,
  CheckCircle2,
  AlertTriangle,
  Loader2
} from 'lucide-react';
import { statsApi } from '../../api/stats';
import { notificationsApi, NotificationItem } from '../../api/notifications';
import { ROUTE_PATHS } from '../../routes/paths';
import { useFondsStore } from '../../store/useFondsStore';

const ComplianceChart: React.FC<{ trend: { date: string; count: number }[] }> = ({ trend }) => (
  <div className="h-full w-full">
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={trend}>
        <defs>
          <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2} />
            <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
        <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
        <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
        <Tooltip
          contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
        />
        <Area type="monotone" dataKey="count" stroke="#0ea5e9" strokeWidth={2} fillOpacity={1} fill="url(#colorVolume)" />
      </AreaChart>
    </ResponsiveContainer>
  </div>
);

interface DashboardProps {
  // 保留可选 props 用于向后兼容
  onNavigate?: (view: ViewState, subItem?: string) => void;
}

export const Dashboard: React.FC<DashboardProps> = () => {
  const routerNavigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [trend, setTrend] = useState<{ date: string; count: number }[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [notifRefreshing, setNotifRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 监听全宗变化，自动刷新数据
  const currentFondsCode = useFondsStore((state) => state.currentFonds?.fondsCode);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, trendRes, notifRes] = await Promise.all([
        statsApi.getDashboard(),
        statsApi.getTrend(),
        notificationsApi.list()
      ]);

      const errors: string[] = [];

      if (statsRes.code === 200) {
        setStats(statsRes.data);
        if (statsRes.data?.recentTrend?.length) {
          setTrend(statsRes.data.recentTrend);
        }
      } else {
        errors.push('统计信息加载失败');
      }

      if (trendRes.code === 200 && (!statsRes.data?.recentTrend?.length)) {
        setTrend(trendRes.data || []);
      } else if (trendRes.code !== 200) {
        errors.push('趋势数据加载失败');
      }

      if (notifRes.code === 200) {
        setNotifications(notifRes.data || []);
      } else {
        errors.push('通知加载失败');
      }

      if (errors.length) {
        setError(errors.join(' / '));
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || '加载仪表盘数据失败');
    } finally {
      setLoading(false);
    }
  }, []); // 空依赖数组，API 和 setState 都是稳定的

  const refreshNotifications = async () => {
    setNotifRefreshing(true);
    try {
      const res = await notificationsApi.list();
      if (res.code === 200) {
        setNotifications(res.data || []);
      }
    } catch {
      // ignore,加载失败时保持现有列表
    } finally {
      setNotifRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  // 当全宗变化时自动刷新数据
  }, [currentFondsCode, loadData]);

  const statCards = [
    { label: '总归档量', value: stats?.totalArchives ?? '--', color: 'bg-blue-500', icon: CheckCircle2 },
    { label: '存储占用', value: stats?.storageUsed ?? '--', color: 'bg-emerald-500', icon: AlertTriangle },
    { label: '待处理任务', value: stats?.pendingTasks ?? '--', color: 'bg-amber-500', icon: AlertTriangle },
    { label: '今日入库', value: stats?.todayIngest ?? '--', color: 'bg-purple-500', icon: AlertTriangle }
  ];

  return (
    <div className="p-8 space-y-8 max-w-[1600px] mx-auto">

      {/* Welcome Section */}
      <div className="flex justify-between items-end">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">工作台概览</h2>
          <p className="text-slate-500 mt-1">欢迎回到 DigiVoucher，系统运行正常，四性检测实时监控中。</p>
        </div>
        <div className="flex space-x-3">

          <button
            onClick={() => routerNavigate(ROUTE_PATHS.PRE_ARCHIVE_POOL)}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all active:scale-95"
          >
            <FileUp size={16} className="mr-2" /> 快速归档
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-600 px-4 py-2 rounded-lg text-sm">
          {error}
        </div>
      )}
      {loading && (
        <div className="text-sm text-slate-500 flex items-center">
          <Loader2 className="animate-spin mr-2" size={16} /> 加载中...
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => (
          <div
            key={index}
            onClick={() => routerNavigate(ROUTE_PATHS.STATS)}
            className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 relative overflow-hidden group hover:shadow-md transition-all duration-300 cursor-pointer"
          >
            <div className={`absolute right-0 top-0 w-24 h-24 rounded-bl-full opacity-10 transition-transform group-hover:scale-110 ${stat.color}`} />
            <div className="flex justify-between items-start mb-4">
              <div className={`p-3 rounded-xl ${stat.color} bg-opacity-10 text-${stat.color.replace('bg-', '')}`}>
                <stat.icon size={24} className={stat.color.replace('bg-', 'text-')} />
              </div>
            </div>
            <div className="space-y-1">
              <h3 className="text-3xl font-bold text-slate-800 tracking-tight">{stat.value}</h3>
              <p className="text-sm text-slate-500 font-medium">{stat.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Chart Section */}
        <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-100 p-6 flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-2">
              <div className="w-1 h-6 bg-primary-500 rounded-full"></div>
              <h3 className="text-lg font-bold text-slate-800">归档量趋势 & 智能化分析</h3>
            </div>
            <select className="bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-1 outline-none focus:ring-2 focus:ring-primary-500/20 cursor-pointer">
              <option>最近 6 个月</option>
              <option>最近 1 年</option>
            </select>
          </div>
          <div className="flex-1 min-h-[300px]">
            {trend.length === 0 ? (
              <div className="text-sm text-slate-400">暂无趋势数据</div>
            ) : (
              <ComplianceChart trend={trend} />
            )}
          </div>
        </div>

        {/* Notifications & Tasks */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-0 flex flex-col overflow-hidden">
          <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <Bell size={18} className="text-primary-500" /> 待办与通知
            </h3>
            <button
              onClick={refreshNotifications}
              className="text-xs text-primary-600 font-semibold flex items-center gap-1 hover:text-primary-700"
              disabled={notifRefreshing}
            >
              {notifRefreshing && <Loader2 size={14} className="animate-spin" />}
              {notifRefreshing ? '刷新中...' : '刷新通知'}
            </button>
          </div>
          <div className="divide-y divide-slate-100 overflow-y-auto max-h-[320px]">
            {loading && notifications.length === 0 ? (
              <div className="p-4 text-sm text-slate-400">加载通知中...</div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-sm text-slate-400">暂无通知</div>
            ) : (
              notifications.map((note) => (
                <div key={note.id} className="p-4 hover:bg-slate-50 transition-colors cursor-pointer group">
                  <div className="flex items-start gap-3">
                    <div className={`w-2 h-2 mt-2 rounded-full shrink-0 ${note.type === 'success' ? 'bg-emerald-500' : note.type === 'warning' ? 'bg-amber-500' : 'bg-blue-500'
                      }`} />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-slate-800 group-hover:text-primary-600 transition-colors line-clamp-2">{note.title}</p>
                      <p className="text-xs text-slate-400 mt-1">{note.time}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
          <div className="p-4 bg-slate-50 border-t border-slate-100 mt-auto text-center">
            <button
              onClick={refreshNotifications}
              className="text-sm text-primary-600 font-medium hover:text-primary-700 disabled:text-slate-400"
              disabled={notifRefreshing}
            >
              {notifRefreshing ? '刷新中...' : '查看全部通知（刷新）'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
