// Input: 痛点对比数据
// Output: 痛点对比区域组件
// Pos: src/pages/product-website/components/PainPointsSection.tsx

import React from 'react';
import { XCircle, CheckCircle2, AlertTriangle, ChevronRight } from 'lucide-react';
import { PAIN_POINTS } from '../data/sections';

export const PainPointsSection: React.FC = () => {
  return (
    <section className="py-24 relative bg-[#0B1120]">
      <div className="max-w-7xl mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">为什么选择 DigiVoucher?</h2>
          <p className="text-slate-400">直击传统档案管理痛点，提供现代化解决方案</p>
        </div>

        {/* Comparison Grid */}
        <div className="relative">
          {/* Central Gradient Line (Background) */}
          <div className="absolute left-1/2 top-0 bottom-0 w-px bg-gradient-to-b from-transparent via-slate-700 to-transparent hidden md:block"></div>

          {/* Headers */}
          <div className="grid md:grid-cols-[1fr_auto_1fr] gap-8 mb-12">
            <div className="flex items-center justify-center md:justify-end gap-3">
              <div className="w-10 h-10 rounded-full bg-red-500/10 flex items-center justify-center border border-red-500/20">
                <XCircle className="w-6 h-6 text-red-500" />
              </div>
              <h3 className="text-2xl font-bold text-slate-300">传统纸质归档</h3>
            </div>
            <div className="w-12 hidden md:block"></div>
            <div className="flex items-center justify-center md:justify-start gap-3">
              <div className="w-10 h-10 rounded-full bg-cyan-500/10 flex items-center justify-center border border-cyan-500/20">
                <CheckCircle2 className="w-6 h-6 text-cyan-400" />
              </div>
              <h3 className="text-2xl font-bold text-white">DigiVoucher 模式</h3>
            </div>
          </div>

          {/* Connected Rows */}
          <div className="space-y-6">
            {PAIN_POINTS.map((item, i) => (
              <div key={i} className="grid md:grid-cols-[1fr_auto_1fr] gap-4 md:gap-8 items-center group">
                {/* Left: Pain */}
                <div className="bg-slate-900/30 border border-slate-800/50 p-6 rounded-xl text-right hover:bg-red-500/5 hover:border-red-500/20 transition-all duration-300">
                  <div className="flex items-center justify-end gap-3 mb-2">
                    <h4 className="text-slate-300 font-bold text-lg group-hover:text-red-400 transition-colors">{item.pain.title}</h4>
                    <AlertTriangle className="w-5 h-5 text-slate-600 group-hover:text-red-500 transition-colors" />
                  </div>
                  <p className="text-slate-500 text-sm">{item.pain.desc}</p>
                </div>

                {/* Center: Arrow */}
                <div className="hidden md:flex items-center justify-center">
                  <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center group-hover:bg-cyan-500 group-hover:border-cyan-400 transition-all duration-300 shadow-lg z-10">
                    <ChevronRight className="w-5 h-5 text-slate-500 group-hover:text-white transition-colors" />
                  </div>
                </div>

                {/* Right: Gain */}
                <div className="bg-slate-800/50 border border-slate-700 p-6 rounded-xl text-left hover:bg-cyan-500/10 hover:border-cyan-500/50 transition-all duration-300 shadow-lg">
                  <div className="flex items-center justify-start gap-3 mb-2">
                    <div className="p-1.5 rounded-lg bg-cyan-500/10 group-hover:bg-cyan-500/20 transition-colors">
                      {item.gain.icon}
                    </div>
                    <h4 className="text-white font-bold text-lg group-hover:text-cyan-400 transition-colors">{item.gain.title}</h4>
                  </div>
                  <p className="text-slate-400 text-sm">{item.gain.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
