// Input: Hero 区域配置、路由跳转函数
// Output: Hero 落地区域组件（含咨询模态框）
// Pos: src/pages/product-website/components/HeroSection.tsx

import React, { useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { HERO_SECTION } from '../data/sections';
import { ConsultationModal } from './ConsultationModal';

interface HeroSectionProps {
  onNavigate: (path: string) => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onNavigate }) => {
  const BadgeIcon = HERO_SECTION.badge.icon;
  const [showConsultModal, setShowConsultModal] = useState(false);

  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-20">
      {/* Dynamic Background */}
      <div className="absolute inset-0 z-0">
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20"></div>
        <svg className="absolute inset-0 w-full h-full" preserveAspectRatio="none">
          <defs>
            <linearGradient id="grid-grad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#06b6d4" stopOpacity="0.1" />
              <stop offset="100%" stopColor="#0B1120" stopOpacity="0" />
            </linearGradient>
          </defs>
          <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="url(#grid-grad)" strokeWidth="1" />
          </pattern>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </svg>
        {/* Glowing Orbs */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-cyan-500/20 rounded-full blur-[100px] animate-pulse"></div>
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-600/20 rounded-full blur-[100px] animate-pulse delay-1000"></div>
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
        <div className="flex flex-col sm:flex-row justify-center gap-6 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300">
          {HERO_SECTION.buttons.map((btn, i) => (
            <button
              key={i}
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

      {/* Consultation Modal */}
      <ConsultationModal isOpen={showConsultModal} onClose={() => setShowConsultModal(false)} />
    </section>
  );
};
