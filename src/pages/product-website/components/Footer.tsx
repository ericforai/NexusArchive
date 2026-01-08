// Input: 页脚链接数据
// Output: 页脚组件
// Pos: src/pages/product-website/components/Footer.tsx

import React from 'react';
import { Shield, Activity } from 'lucide-react';
import { FOOTER_LINKS } from '../data/sections';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-[#050911] border-t border-slate-800 py-12">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex flex-col md:flex-row justify-between items-center mb-8">
          <div className="flex items-center gap-2 mb-4 md:mb-0">
            <Shield className="w-6 h-6 text-slate-600" />
            <span className="text-xl font-bold text-slate-500">DigiVoucher</span>
          </div>
          <div className="flex gap-8 text-sm text-slate-500">
            {FOOTER_LINKS.map((link, i) => (
              <a key={i} href="#" className="hover:text-cyan-400 transition-colors">
                {link}
              </a>
            ))}
          </div>
          <button className="mt-4 md:mt-0 px-6 py-2 bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-cyan-400 rounded-full text-sm font-bold transition-all flex items-center gap-2">
            <Activity className="w-4 h-4" />
            立即咨询专家
          </button>
        </div>
        <div className="text-center md:text-left text-xs text-slate-700">
          <p>© 2025 DigiVoucher. All rights reserved. | 沪ICP备2025125372号-4</p>
          <p className="mt-2">本系统符合《会计档案管理办法》及 DA/T 94-2022 标准要求。</p>
        </div>
      </div>
    </footer>
  );
};
