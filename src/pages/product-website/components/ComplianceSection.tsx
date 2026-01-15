// Input: 合规架构层级数据
// Output: 合规架构展示区域组件
// Pos: src/pages/product-website/components/ComplianceSection.tsx

import React from 'react';
import { Layers, Shield, Key, Server, Cpu } from 'lucide-react';
import { COMPLIANCE_LAYERS } from '../data/sections';

export const ComplianceSection: React.FC = () => {
  return (
    <section className="py-24 relative">
      <div className="max-w-5xl mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">信创合规架构</h2>
          <p className="text-slate-400">完全符合 DA/T 94-2022 电子会计档案管理规范</p>
        </div>

        <div className="relative">
          {/* Connecting Lines */}
          <div className="absolute left-1/2 top-0 bottom-0 w-px bg-gradient-to-b from-transparent via-cyan-500/30 to-transparent"></div>

          <div className="space-y-8 relative z-10">
            {/* Layer 1: 业务应用层 */}
            <ComplianceLayer layer={COMPLIANCE_LAYERS[0]} />

            {/* Layer 2: 安全合规层 */}
            <ComplianceLayerSecurity layer={COMPLIANCE_LAYERS[1]} />

            {/* Layer 3: 信创基础设施层 */}
            <ComplianceLayerInfra layer={COMPLIANCE_LAYERS[2]} />
          </div>
        </div>
      </div>
    </section>
  );
};

interface ComplianceLayerProps {
  layer: {
    title: string;
    subtitle: string;
    color: string;
    icon: typeof Layers;
    items: readonly string[];
  };
}

const ComplianceLayer: React.FC<ComplianceLayerProps> = ({ layer }) => {
  const Icon = layer.icon;
  const isAmber = layer.color === 'amber';

  return (
    <div className={`bg-slate-900/80 backdrop-blur border ${isAmber ? 'border-amber-500/30 shadow-[0_0_30px_rgba(245,158,11,0.1)]' : 'border-slate-700'} rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden group hover:border-cyan-500/50 transition-all`}>
      <div className={`absolute top-0 left-0 w-1 h-full ${isAmber ? 'bg-amber-500' : `bg-${layer.color}-600`}`}></div>
      <div className="flex items-center justify-between mb-6">
        <h3 className={`text-xl font-bold ${isAmber ? 'text-amber-400' : 'text-white'} flex items-center gap-2`}>
          <Icon className={`w-5 h-5 ${isAmber ? '' : `text-${layer.color}-400`}`} /> {layer.title}
        </h3>
        <span className={`text-xs font-mono ${isAmber ? 'text-amber-500/50' : 'text-slate-500'}`}>{layer.subtitle}</span>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {layer.items.map((item, i) => (
          <div key={i} className="bg-slate-800/50 border border-slate-700 rounded-lg py-2 text-center text-sm text-slate-300">
            {item}
          </div>
        ))}
      </div>
    </div>
  );
};

interface ComplianceLayerSecurityProps {
  layer: {
    title: string;
    subtitle: string;
    color: string;
    icon: typeof Shield;
    items: readonly { label: string; desc: string; icon: typeof Key }[];
  };
}

const ComplianceLayerSecurity: React.FC<ComplianceLayerSecurityProps> = ({ layer }) => {
  const Icon = layer.icon;

  return (
    <div className="bg-gradient-to-r from-slate-900/90 to-slate-800/90 backdrop-blur border border-amber-500/30 rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden shadow-[0_0_30px_rgba(245,158,11,0.1)]">
      <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-xl font-bold text-amber-400 flex items-center gap-2">
          <Icon className="w-5 h-5" /> {layer.title}
        </h3>
        <span className="text-xs font-mono text-amber-500/50">{layer.subtitle}</span>
      </div>
      <div className="grid grid-cols-3 gap-6">
        {layer.items.map((item, i) => {
          const ItemIcon = item.icon;
          return (
            <div key={i} className="text-center">
              <div className="w-10 h-10 mx-auto bg-amber-500/10 rounded-full flex items-center justify-center mb-2">
                <ItemIcon className="w-5 h-5 text-amber-400" />
              </div>
              <div className="text-white font-medium text-sm">{item.label}</div>
              <div className="text-xs text-slate-500 mt-1">{item.desc}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

interface ComplianceLayerInfraProps {
  layer: {
    title: string;
    subtitle: string;
    color: string;
    icon: typeof Server;
    items: readonly { label: string; icon: typeof Cpu }[];
  };
}

const ComplianceLayerInfra: React.FC<ComplianceLayerInfraProps> = ({ layer }) => {
  const Icon = layer.icon;

  return (
    <div className="bg-slate-900/80 backdrop-blur border border-slate-700 rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden group hover:border-cyan-500/50 transition-all">
      <div className="absolute top-0 left-0 w-1 h-full bg-blue-600"></div>
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-xl font-bold text-white flex items-center gap-2">
          <Icon className="w-5 h-5 text-blue-400" /> {layer.title}
        </h3>
        <span className="text-xs font-mono text-slate-500">{layer.subtitle}</span>
      </div>
      <div className="grid grid-cols-3 gap-4">
        {layer.items.map((item, i) => {
          const ItemIcon = item.icon;
          return (
            <div key={i} className="bg-slate-800/50 border border-slate-700 rounded-lg py-3 flex flex-col items-center justify-center gap-2 text-sm text-slate-300 hover:bg-slate-800 transition-colors">
              <ItemIcon className="w-4 h-4" />
              {item.label}
            </div>
          );
        })}
      </div>
    </div>
  );
};
