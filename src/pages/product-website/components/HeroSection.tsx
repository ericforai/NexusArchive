// Input: Hero 区域配置、路由跳转函数
// Output: Hero 落地区域组件
// Pos: src/pages/product-website/components/HeroSection.tsx

import React, { useState } from 'react';
import { ChevronRight, ShieldCheck, FileCheck, Fingerprint } from 'lucide-react';
import { HERO_SECTION } from '../data/sections';
import { ConsultationModal } from './ConsultationModal';

interface HeroSectionProps {
  onNavigate: (path: string) => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onNavigate }) => {
  const [showConsultModal, setShowConsultModal] = useState(false);
  const BadgeIcon = HERO_SECTION.badge.icon;

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-20">
      {/* Background - 简化背景 */}
      <div className="absolute inset-0 z-0">
        <div className="absolute inset-0 bg-gradient-to-b from-[#0B1120] via-[#0f172a] to-[#0B1120]" />
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-cyan-500/10 rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px]" />
      </div>

      <div className="relative z-10 max-w-5xl mx-auto px-6 text-center">
        <div className="inline-flex items-center px-4 py-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 text-amber-400 text-sm font-medium mb-8 animate-in fade-in slide-in-from-top-4 duration-700">
          <BadgeIcon className="w-4 h-4 mr-2" />
          {HERO_SECTION.badge.text}
        </div>
        <h1 className="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight animate-in fade-in slide-in-from-bottom-8 duration-700 delay-100">
          {HERO_SECTION.title[0]}<br />
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">
            {HERO_SECTION.title[1]}
          </span>
        </h1>
        <p className="text-xl text-slate-400 mb-10 max-w-3xl mx-auto animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200 leading-relaxed whitespace-pre-line">
          {HERO_SECTION.subtitle}
        </p>
        
        {/* 三个核心价值点 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12 animate-in fade-in slide-in-from-bottom-4 duration-700 delay-400">
          <div className="p-6 bg-slate-800/30 border border-slate-700/50 rounded-2xl hover:bg-slate-800/50 hover:border-cyan-500/30 hover:-translate-y-1 transition-all duration-300">
            <ShieldCheck className="w-8 h-8 text-cyan-400 mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">国标合规</h3>
            <p className="text-sm text-slate-400">符合 DA/T 94-2022<br />轻松通过审计</p>
          </div>
          <div className="p-6 bg-slate-800/30 border border-slate-700/50 rounded-2xl hover:bg-slate-800/50 hover:border-emerald-500/30 hover:-translate-y-1 transition-all duration-300">
            <FileCheck className="w-8 h-8 text-emerald-400 mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">单套归档</h3>
            <p className="text-sm text-slate-400">告别纸质库房<br />电子原件直接归档</p>
          </div>
          <div className="p-6 bg-slate-800/30 border border-slate-700/50 rounded-2xl hover:bg-slate-800/50 hover:border-purple-500/30 hover:-translate-y-1 transition-all duration-300">
            <Fingerprint className="w-8 h-8 text-purple-400 mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">四性保障</h3>
            <p className="text-sm text-slate-400">真实完整可验证<br />防篡改零风险</p>
          </div>
        </div>
        
        <div className="flex flex-col sm:flex-row justify-center gap-6 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-500">
          {HERO_SECTION.buttons.map((btn, i) => (
            <button
              key={btn.text}
              onClick={() => btn.primary ? onNavigate('/system') : setShowConsultModal(true)}
              className={`px-8 py-4 rounded-xl transition-all flex items-center justify-center ${btn.primary
                  ? 'bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-bold shadow-[0_0_20px_rgba(6,182,212,0.4)] transform hover:scale-105'
                  : 'bg-slate-800/50 hover:bg-slate-800 border border-slate-700 hover:border-cyan-500/50 text-white backdrop-blur-sm'
                }`}
            >
              {btn.text}
              {btn.primary && <ChevronRight className="w-5 h-5 ml-2" />}
            </button>
          ))}
        </div>
      </div>

      {/* Animated Stream (Abstract Representation) */}
      <div className="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-[#0B1120] to-transparent z-20"></div>

      {/* 咨询模态框 */}
      <ConsultationModal isOpen={showConsultModal} onClose={() => setShowConsultModal(false)} />
    </section>
  );
};
