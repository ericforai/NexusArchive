// Input: 数据价值统计、核心特性数据
// Output: 数据价值展示区域组件
// Pos: src/pages/product-website/components/DataValueSection.tsx

import React from 'react';
import { DATA_STATS, CORE_FEATURES } from '../data/sections';

export const DataValueSection: React.FC = () => {
  return (
    <section className="py-12 border-y border-slate-800 bg-slate-900/30">
      <div className="max-w-7xl mx-auto px-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {DATA_STATS.map((stat, i) => (
            <div key={i} className="text-center group">
              <div className="flex items-center justify-center gap-2 mb-2 text-slate-400 text-sm">
                {stat.icon}
                {stat.label}
              </div>
              <div className="text-4xl md:text-5xl font-bold text-white group-hover:text-cyan-400 transition-colors font-mono">
                {stat.val}
              </div>
            </div>
          ))}
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {CORE_FEATURES.map((feature, idx) => (
            <div key={idx} className="group p-8 rounded-2xl bg-slate-900/50 border border-slate-800 hover:border-cyan-500/30 hover:bg-slate-800/80 transition-all duration-300">
              <div className="mb-6 p-4 rounded-xl bg-slate-950 inline-block border border-slate-800 group-hover:border-cyan-500/20 group-hover:shadow-[0_0_15px_rgba(6,182,212,0.15)] transition-all">
                {feature.icon}
              </div>
              <h3 className="text-xl font-bold text-white mb-3">{feature.title}</h3>
              <p className="text-slate-400 leading-relaxed">{feature.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
