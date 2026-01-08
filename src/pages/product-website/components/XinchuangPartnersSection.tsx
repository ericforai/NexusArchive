// Input: 信创生态合作伙伴数据
// Output: 信创生态展示区域组件
// Pos: src/pages/product-website/components/XinchuangPartnersSection.tsx

import React from 'react';
import { Cpu, Layers, Database, Shield } from 'lucide-react';
import { XINCHUANG_PARTNERS } from '../data/sections';

export const XinchuangPartnersSection: React.FC = () => {
  return (
    <section className="py-16 border-t border-slate-800 bg-[#0B1120]">
      <div className="max-w-7xl mx-auto px-6">
        <p className="text-center text-slate-500 text-sm mb-8 font-mono tracking-widest uppercase">
          Trusted by Industry Leaders & Xinchuang Ecosystem
        </p>

        {/* Infinite Scroll Logo Wall Simulation */}
        <div className="flex flex-wrap justify-center gap-8 md:gap-16 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
          {XINCHUANG_PARTNERS.map((partner, i) => (
            <div key={i} className="flex items-center gap-2 group cursor-default">
              <div className="p-2 bg-slate-800 rounded-lg group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors">
                {partner.icon as unknown as React.ReactNode}
              </div>
              <div>
                <div className="font-bold text-slate-300 group-hover:text-white transition-colors">{partner.name}</div>
                <div className="text-xs text-slate-600 group-hover:text-cyan-500/70 transition-colors">{partner.en}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
