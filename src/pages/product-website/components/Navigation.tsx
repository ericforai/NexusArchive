// Input: React 状态、导航配置、路由跳转函数
// Output: 固定导航栏组件
// Pos: src/pages/product-website/components/Navigation.tsx

import React from 'react';
import { Shield } from 'lucide-react';
import { NAV_CONFIG } from '../data/sections';

interface NavigationProps {
  scrolled: boolean;
  onNavigate: (path: string) => void;
}

export const Navigation: React.FC<NavigationProps> = ({ scrolled, onNavigate }) => {
  return (
    <nav className={`fixed w-full z-50 transition-all duration-300 border-b ${
      scrolled
        ? 'bg-[#0B1120]/90 backdrop-blur border-slate-800 py-4'
        : 'bg-transparent border-transparent py-6'
    }`}>
      <div className="max-w-7xl mx-auto px-6 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-lg flex items-center justify-center shadow-[0_0_20px_rgba(6,182,212,0.5)]">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <span className="text-2xl font-bold text-white tracking-tight">
            {NAV_CONFIG.brandName}<span className="text-cyan-400">{NAV_CONFIG.brandSuffix}</span>
          </span>
        </div>
        <div className="flex gap-4">
          <button
            onClick={() => onNavigate('/system')}
            className="px-6 py-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-900 font-bold rounded-lg shadow-[0_0_15px_rgba(245,158,11,0.4)] transition-all transform hover:scale-105"
          >
            {NAV_CONFIG.ctaButton}
          </button>
        </div>
      </div>
    </nav>
  );
};
