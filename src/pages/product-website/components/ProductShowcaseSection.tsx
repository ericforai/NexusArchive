// Input: Demo 数据配置
// Output: 产品演示区域组件
// Pos: src/pages/product-website/components/ProductShowcaseSection.tsx

import React, { useEffect, useRef } from 'react';
import { Shield, Lock, Search, Key, FileText, Activity, Database, ChevronRight } from 'lucide-react';
import {
  DEMO_NAV_ITEMS,
  DASHBOARD_STATS,
  COLLECTION_TASKS,
  ARCHIVE_TABLE_DATA,
  BORROW_STATS,
  BORROW_RECORDS,
  SYSTEM_SETTINGS,
  ARCHIVE_TREND_DATA,
} from '../data/sections';

export const ProductShowcaseSection: React.FC = () => {
  const demoContentRef = useRef<HTMLDivElement>(null);

  const handleNavClick = (index: number) => {
    const demoContent = demoContentRef.current;
    if (demoContent) {
      demoContent.setAttribute('data-active-view', index.toString());
    }
    // Update active state
    document.querySelectorAll('.demo-nav-item').forEach((el, idx) => {
      if (idx === index) {
        el.classList.add('bg-cyan-500/10', 'text-cyan-400', 'border', 'border-cyan-500/20');
        el.classList.remove('text-slate-400', 'hover:bg-slate-800');
      } else {
        el.classList.remove('bg-cyan-500/10', 'text-cyan-400', 'border', 'border-cyan-500/20');
        el.classList.add('text-slate-400', 'hover:bg-slate-800');
      }
    });
  };

  return (
    <section className="py-24 bg-slate-900/30 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-6">
        <div className="mb-16 flex items-end justify-between">
          <div>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">沉浸式管理体验</h2>
            <p className="text-slate-400">专为财务人员设计的现代化工作台</p>
          </div>
          <div className="hidden md:flex gap-2">
            <div className="w-3 h-3 rounded-full bg-red-500"></div>
            <div className="w-3 h-3 rounded-full bg-amber-500"></div>
            <div className="w-3 h-3 rounded-full bg-green-500"></div>
          </div>
        </div>

        {/* CSS Browser Window Mockup */}
        <div className="rounded-xl border border-slate-700 bg-[#0F172A] shadow-2xl overflow-hidden transform hover:scale-[1.01] transition-transform duration-500">
          {/* Browser Header */}
          <div className="bg-slate-800 px-4 py-3 flex items-center gap-4 border-b border-slate-700">
            <div className="flex gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
              <div className="w-3 h-3 rounded-full bg-amber-500/80"></div>
              <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
            </div>
            <div className="flex-1 max-w-2xl mx-auto bg-slate-900 rounded-md px-4 py-1.5 text-xs text-slate-500 font-mono text-center flex items-center justify-center gap-2">
              <Lock className="w-3 h-3" />
              DigiVoucher.internal/dashboard
            </div>
          </div>

          {/* App Interface */}
          <div className="flex h-[600px]">
            {/* Sidebar */}
            <div className="w-64 bg-slate-900 border-r border-slate-800 p-4 hidden md:block">
              <div className="flex items-center gap-2 mb-8 px-2">
                <div className="w-6 h-6 bg-cyan-600 rounded flex items-center justify-center">
                  <Shield className="w-4 h-4 text-white" />
                </div>
                <span className="font-bold text-white">DigiVoucher</span>
              </div>
              <div className="space-y-1">
                {DEMO_NAV_ITEMS.map((item, i) => (
                  <button
                    key={i}
                    onClick={() => handleNavClick(i)}
                    className={`demo-nav-item w-full text-left px-3 py-2 rounded-lg text-sm transition-all cursor-pointer ${
                      i === 0
                        ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                        : 'text-slate-400 hover:bg-slate-800'
                    }`}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>

            {/* Main Content */}
            <div id="demo-content" ref={demoContentRef} data-active-view="0" className="flex-1 bg-[#0B1120] p-8 overflow-auto">
              {/* View 0: 工作台 */}
              <DemoWorkspaceView stats={DASHBOARD_STATS} trendData={ARCHIVE_TREND_DATA} />

              {/* View 1: 档案收集 */}
              <DemoCollectionView tasks={COLLECTION_TASKS} />

              {/* View 2: 档案管理 */}
              <DemoArchiveView tableData={ARCHIVE_TABLE_DATA} />

              {/* View 3: 借阅中心 */}
              <DemoBorrowView stats={BORROW_STATS} records={BORROW_RECORDS} />

              {/* View 4: 系统设置 */}
              <DemoSettingsView settings={SYSTEM_SETTINGS} />

              <style>{`
                .demo-view { display: none; }
                #demo-content[data-active-view="0"] .demo-view[data-view="0"] { display: block; }
                #demo-content[data-active-view="1"] .demo-view[data-view="1"] { display: block; }
                #demo-content[data-active-view="2"] .demo-view[data-view="2"] { display: block; }
                #demo-content[data-active-view="3"] .demo-view[data-view="3"] { display: block; }
                #demo-content[data-active-view="4"] .demo-view[data-view="4"] { display: block; }
              `}</style>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

// Demo Views Sub-components

interface DemoStatsProps {
  stats: readonly { label: string; val: string; color: string; bg: string }[];
  trendData: readonly number[];
}

const DemoWorkspaceView: React.FC<DemoStatsProps> = ({ stats, trendData }) => (
  <div className="demo-view" data-view="0">
    <div className="flex justify-between items-center mb-8">
      <h3 className="text-2xl font-bold text-white">DigiVoucher 智能归档</h3>
      <div className="flex gap-3">
        <span className="px-3 py-1 rounded-full bg-green-500/10 text-green-400 text-xs border border-green-500/20 flex items-center gap-1">
          <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
          系统运行正常
        </span>
      </div>
    </div>

    {/* Stats Cards */}
    <div className="grid grid-cols-4 gap-6 mb-8">
      {stats.map((stat, i) => (
        <div key={i} className="bg-slate-900/50 border border-slate-800 p-5 rounded-xl">
          <div className="text-slate-400 text-sm mb-2">{stat.label}</div>
          <div className={`text-2xl font-bold ${stat.color}`}>{stat.val}</div>
        </div>
      ))}
    </div>

    {/* Chart Area */}
    <div className="grid grid-cols-3 gap-6 h-64">
      <div className="col-span-2 bg-slate-900/50 border border-slate-800 rounded-xl p-6 relative overflow-hidden">
        <h4 className="text-white font-medium mb-4">归档趋势</h4>
        <div className="absolute bottom-0 left-0 right-0 h-48 flex items-end justify-between px-6 pb-6 gap-2">
          {trendData.map((h, i) => (
            <div key={i} className="w-full bg-cyan-500/20 hover:bg-cyan-500/40 transition-all rounded-t-sm relative group" style={{ height: `${h}%` }}>
              <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity">
                {h * 10}
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
        <h4 className="text-white font-medium mb-4">档案构成</h4>
        <div className="flex items-center justify-center h-40 relative">
          <div className="w-32 h-32 rounded-full border-8 border-slate-800 border-t-cyan-500 border-r-purple-500 border-b-amber-500 rotate-45"></div>
          <div className="absolute text-center">
            <div className="text-2xl font-bold text-white">98%</div>
            <div className="text-xs text-slate-500">合规率</div>
          </div>
        </div>
        <div className="flex justify-center gap-4 text-xs text-slate-400 mt-2">
          <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-cyan-500"></div>凭证</span>
          <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-purple-500"></div>账簿</span>
          <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-amber-500"></div>报表</span>
        </div>
      </div>
    </div>
  </div>
);

interface DemoCollectionProps {
  tasks: readonly { name: string; status: string; count: number; color: string }[];
}

const DemoCollectionView: React.FC<DemoCollectionProps> = ({ tasks }) => (
  <div className="demo-view hidden" data-view="1">
    <div className="flex justify-between items-center mb-8">
      <h3 className="text-2xl font-bold text-white">档案收集</h3>
      <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
        + 新建收集任务
      </button>
    </div>
    <div className="space-y-4">
      {tasks.map((task, i) => (
        <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl hover:border-cyan-500/30 transition-all">
          <div className="flex justify-between items-center">
            <div>
              <h4 className="text-white font-medium mb-2">{task.name}</h4>
              <div className="flex gap-4 text-sm text-slate-400">
                <span>状态: <span className={task.color}>{task.status}</span></span>
                <span>文件数: {task.count}</span>
              </div>
            </div>
            <ChevronRight className="w-5 h-5 text-slate-600" />
          </div>
        </div>
      ))}
    </div>
  </div>
);

interface DemoArchiveViewProps {
  tableData: readonly { code: string; type: string; amount: string; status: string }[];
}

const DemoArchiveView: React.FC<DemoArchiveViewProps> = ({ tableData }) => (
  <div className="demo-view hidden" data-view="2">
    <div className="flex justify-between items-center mb-8">
      <h3 className="text-2xl font-bold text-white">档案管理</h3>
      <div className="flex gap-2">
        <button className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-sm font-medium transition-colors">
          四性检测
        </button>
        <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
          正式归档
        </button>
      </div>
    </div>
    <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-800/50 border-b border-slate-700">
          <tr className="text-slate-400">
            <th className="text-left p-4">档案编号</th>
            <th className="text-left p-4">凭证类型</th>
            <th className="text-left p-4">金额</th>
            <th className="text-left p-4">状态</th>
            <th className="text-left p-4">操作</th>
          </tr>
        </thead>
        <tbody className="text-slate-300">
          {tableData.map((item, i) => (
            <tr key={i} className="border-b border-slate-800 hover:bg-slate-800/30">
              <td className="p-4 text-cyan-400">{item.code}</td>
              <td className="p-4">{item.type}</td>
              <td className="p-4">{item.amount}</td>
              <td className="p-4">
                <span className={`px-2 py-1 rounded text-xs ${
                  item.status === '已归档' ? 'bg-green-500/10 text-green-400' : 'bg-amber-500/10 text-amber-400'
                }`}>
                  {item.status}
                </span>
              </td>
              <td className="p-4">
                <button className="text-cyan-400 hover:text-cyan-300 text-xs flex items-center gap-1 transition-colors">
                  <Search className="w-3 h-3" /> 预览
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);

interface DemoBorrowViewProps {
  stats: readonly { label: string; count: number; color: string }[];
  records: readonly { user: string; dept: string; doc: string; date: string; status: string }[];
}

const DemoBorrowView: React.FC<DemoBorrowViewProps> = ({ stats, records }) => (
  <div className="demo-view hidden" data-view="3">
    <div className="flex justify-between items-center mb-8">
      <h3 className="text-2xl font-bold text-white">借阅中心</h3>
      <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
        + 新建借阅申请
      </button>
    </div>
    <div className="grid grid-cols-3 gap-6 mb-8">
      {stats.map((stat, i) => (
        <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl">
          <div className="text-slate-400 text-sm mb-2">{stat.label}</div>
          <div className={`text-3xl font-bold ${stat.color}`}>{stat.count}</div>
        </div>
      ))}
    </div>
    <div className="space-y-4">
      {records.map((item, i) => (
        <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl">
          <div className="flex justify-between items-center">
            <div className="space-y-2">
              <div className="text-white font-medium">{item.user} - {item.dept}</div>
              <div className="text-sm text-slate-400">档案编号: <span className="text-cyan-400">{item.doc}</span></div>
              <div className="text-sm text-slate-400">申请时间: {item.date}</div>
            </div>
            <span className={`px-3 py-1 rounded-full text-xs ${
              item.status === '借阅中' ? 'bg-cyan-500/10 text-cyan-400' : 'bg-amber-500/10 text-amber-400'
            }`}>
              {item.status}
            </span>
          </div>
        </div>
      ))}
    </div>
  </div>
);

interface DemoSettingsViewProps {
  settings: readonly { title: string; desc: string; icon: React.ReactNode }[];
}

const DemoSettingsView: React.FC<DemoSettingsViewProps> = ({ settings }) => (
  <div className="demo-view hidden" data-view="4">
    <div className="mb-8">
      <h3 className="text-2xl font-bold text-white mb-2">系统设置</h3>
      <p className="text-slate-400 text-sm">配置系统参数和权限管理</p>
    </div>
    <div className="space-y-6">
      {settings.map((setting, i) => (
        <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl hover:border-cyan-500/30 transition-all cursor-pointer group">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-slate-800 rounded-lg flex items-center justify-center text-cyan-400 group-hover:bg-cyan-500/10 transition-colors">
              {setting.icon}
            </div>
            <div className="flex-1">
              <h4 className="text-white font-medium mb-1">{setting.title}</h4>
              <p className="text-sm text-slate-400">{setting.desc}</p>
            </div>
            <ChevronRight className="w-5 h-5 text-slate-600 group-hover:text-cyan-400 transition-colors" />
          </div>
        </div>
      ))}
    </div>
  </div>
);
