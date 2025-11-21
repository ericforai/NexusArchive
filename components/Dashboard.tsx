import React from 'react';
import { MOCK_STATS, RECENT_DOCS, NOTIFICATIONS } from '../constants';
import { ViewState } from '../types';
import { 
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip
} from 'recharts';
import { 
  ArrowUpRight, 
  ArrowDownRight, 
  MoreHorizontal, 
  ScanLine, 
  FileUp, 
  Bell, 
  ExternalLink,
  CheckCircle2,
  AlertTriangle
} from 'lucide-react';

const data = [
  { name: '1月', volume: 4000, ocr: 2400 },
  { name: '2月', volume: 3000, ocr: 1398 },
  { name: '3月', volume: 2000, ocr: 9800 },
  { name: '4月', volume: 2780, ocr: 3908 },
  { name: '5月', volume: 1890, ocr: 4800 },
  { name: '6月', volume: 2390, ocr: 3800 },
  { name: '7月', volume: 3490, ocr: 4300 },
];

const ComplianceChart = () => (
  <div className="h-full w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data}>
          <defs>
            <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
              <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
          <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} />
          <YAxis axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} />
          <Tooltip 
            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
          />
          <Area type="monotone" dataKey="volume" stroke="#0ea5e9" strokeWidth={2} fillOpacity={1} fill="url(#colorVolume)" />
        </AreaChart>
      </ResponsiveContainer>
  </div>
);

interface DashboardProps {
  onNavigate: (view: ViewState, subItem?: string) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  const handleClick = (action: string) => {
    alert(`功能演示: ${action}`);
  };

  return (
    <div className="p-8 space-y-8 max-w-[1600px] mx-auto">
      
      {/* Welcome Section */}
      <div className="flex justify-between items-end">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">工作台概览</h2>
          <p className="text-slate-500 mt-1">欢迎回到 NexusArchive，系统运行正常，四性检测实时监控中。</p>
        </div>
        <div className="flex space-x-3">
           <button 
            onClick={() => onNavigate(ViewState.PRE_ARCHIVE, 'OCR识别')}
            className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95"
           >
              <ScanLine size={16} className="mr-2" /> OCR 批量处理
           </button>
           <button 
            onClick={() => onNavigate(ViewState.PRE_ARCHIVE, '电子凭证池')}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all active:scale-95"
           >
              <FileUp size={16} className="mr-2" /> 快速归档
           </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {MOCK_STATS.map((stat, index) => (
          <div 
            key={index} 
            onClick={() => onNavigate(ViewState.STATS)}
            className="bg-white rounded-2xl p-6 shadow-sm border border-slate-100 relative overflow-hidden group hover:shadow-md transition-all duration-300 cursor-pointer"
          >
            <div className={`absolute right-0 top-0 w-24 h-24 rounded-bl-full opacity-10 transition-transform group-hover:scale-110 ${stat.color}`} />
            <div className="flex justify-between items-start mb-4">
              <div className={`p-3 rounded-xl ${stat.color} bg-opacity-10 text-${stat.color.replace('bg-', '')}`}>
                <stat.icon size={24} className={stat.color.replace('bg-', 'text-')} />
              </div>
              {stat.trend !== 0 && (
                <div className={`flex items-center text-xs font-medium px-2 py-1 rounded-full ${stat.trend > 0 ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}`}>
                  {stat.trend > 0 ? <ArrowUpRight size={12} className="mr-1" /> : <ArrowDownRight size={12} className="mr-1" />}
                  {Math.abs(stat.trend)}%
                </div>
              )}
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
            <ComplianceChart />
          </div>
        </div>

        {/* Notifications & Tasks */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-0 flex flex-col overflow-hidden">
          <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <Bell size={18} className="text-primary-500" /> 待办与通知
            </h3>
            <span className="bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">3</span>
          </div>
          <div className="divide-y divide-slate-100 overflow-y-auto max-h-[320px]">
            {NOTIFICATIONS.map((note) => (
              <div key={note.id} onClick={() => handleClick(`查看通知: ${note.title}`)} className="p-4 hover:bg-slate-50 transition-colors cursor-pointer group">
                <div className="flex items-start gap-3">
                  <div className={`w-2 h-2 mt-2 rounded-full shrink-0 ${
                    note.type === 'success' ? 'bg-emerald-500' : note.type === 'warning' ? 'bg-amber-500' : 'bg-blue-500'
                  }`} />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-slate-800 group-hover:text-primary-600 transition-colors line-clamp-2">{note.title}</p>
                    <p className="text-xs text-slate-400 mt-1">{note.time}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="p-4 bg-slate-50 border-t border-slate-100 mt-auto text-center">
            <button onClick={() => handleClick('查看全部通知历史')} className="text-sm text-primary-600 font-medium hover:text-primary-700">查看全部通知</button>
          </div>
        </div>
      </div>

      {/* Recent Documents Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex justify-between items-center">
            <h3 className="text-lg font-bold text-slate-800">最近归档记录</h3>
            <button onClick={() => onNavigate(ViewState.QUERY)} className="text-slate-400 hover:text-primary-600 transition-colors">
              <ExternalLink size={18} />
            </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/50 text-slate-500 text-xs uppercase tracking-wider border-b border-slate-100">
                <th className="p-4 font-medium">档案编号</th>
                <th className="p-4 font-medium">名称/摘要</th>
                <th className="p-4 font-medium">类型</th>
                <th className="p-4 font-medium">金额</th>
                <th className="p-4 font-medium">日期</th>
                <th className="p-4 font-medium">四性评分</th>
                <th className="p-4 font-medium">状态</th>
                <th className="p-4 font-medium text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {RECENT_DOCS.map((doc) => (
                <tr key={doc.id} className="hover:bg-slate-50 transition-colors group cursor-pointer" onClick={() => handleClick(`打开档案详情: ${doc.code}`)}>
                  <td className="p-4 font-mono text-slate-600">{doc.code}</td>
                  <td className="p-4 font-medium text-slate-800">{doc.name}</td>
                  <td className="p-4">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600 border border-slate-200">
                      {doc.type}
                    </span>
                  </td>
                  <td className="p-4 text-slate-600 font-mono">{doc.amount}</td>
                  <td className="p-4 text-slate-500">{doc.date}</td>
                  <td className="p-4">
                    <div className="flex items-center gap-2">
                      <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                        <div 
                          className={`h-full rounded-full ${doc.complianceScore > 90 ? 'bg-emerald-500' : doc.complianceScore > 70 ? 'bg-amber-500' : 'bg-rose-500'}`} 
                          style={{width: `${doc.complianceScore}%`}}
                        />
                      </div>
                      <span className={`text-xs font-bold ${doc.complianceScore > 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{doc.complianceScore}</span>
                    </div>
                  </td>
                  <td className="p-4">
                     <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                       doc.status === '已归档' ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' :
                       doc.status === '审计失败' ? 'bg-rose-50 text-rose-700 border border-rose-100' :
                       'bg-blue-50 text-blue-700 border border-blue-100'
                     }`}>
                       {doc.status === '已归档' && <CheckCircle2 size={12} />}
                       {doc.status === '审计失败' && <AlertTriangle size={12} />}
                       {doc.status}
                     </span>
                  </td>
                  <td className="p-4 text-right">
                    <button 
                      onClick={(e) => { e.stopPropagation(); handleClick(`更多操作: ${doc.code}`); }}
                      className="p-1 text-slate-400 hover:text-primary-600 rounded hover:bg-primary-50 transition-all"
                    >
                      <MoreHorizontal size={18} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};