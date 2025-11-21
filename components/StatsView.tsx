import React from 'react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, PieChart, Pie, Cell } from 'recharts';

const barData = [
  { name: '1月', 凭证: 4000, 发票: 2400, 合同: 2400 },
  { name: '2月', 凭证: 3000, 发票: 1398, 合同: 2210 },
  { name: '3月', 凭证: 2000, 发票: 9800, 合同: 2290 },
  { name: '4月', 凭证: 2780, 发票: 3908, 合同: 2000 },
  { name: '5月', 凭证: 1890, 发票: 4800, 合同: 2181 },
  { name: '6月', 凭证: 2390, 发票: 3800, 合同: 2500 },
];

const pieData = [
  { name: '会计凭证', value: 400 },
  { name: '财务报告', value: 300 },
  { name: '银行回单', value: 300 },
  { name: '合同协议', value: 200 },
];

const COLORS = ['#0ea5e9', '#8b5cf6', '#10b981', '#f59e0b'];

export const StatsView: React.FC = () => {
  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500">
       <h2 className="text-2xl font-bold text-slate-800">数据统计中心</h2>
       <p className="text-slate-500">全维度数据分析与归档驾驶舱</p>

       <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Bar Chart */}
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[400px] flex flex-col">
             <h3 className="text-lg font-bold text-slate-800 mb-4">上半年归档类型统计</h3>
             <div className="flex-1">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={barData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} />
                    <YAxis axisLine={false} tickLine={false} />
                    <Tooltip contentStyle={{borderRadius: '8px', border:'none', boxShadow:'0 4px 12px rgba(0,0,0,0.1)'}} />
                    <Legend />
                    <Bar dataKey="凭证" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="发票" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="合同" fill="#10b981" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
             </div>
          </div>

          {/* Pie Chart */}
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 h-[400px] flex flex-col">
             <h3 className="text-lg font-bold text-slate-800 mb-4">档案库存储占比</h3>
             <div className="flex-1">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={80}
                      outerRadius={120}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
             </div>
          </div>
       </div>
    </div>
  );
};
