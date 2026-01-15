// Input: 四性检测、技术三件套、AIP 包结构数据
// Output: 电子会计档案核心体系区域组件
// Pos: src/pages/product-website/components/CoreSystemSection.tsx

import React from 'react';
import { Shield, FileText, Check, Layers, Cpu, Server, ChevronRight } from 'lucide-react';
import { FOUR_PROPERTIES, TECH_SUITE, AIP_COMPONENTS, AIP_STRUCTURE } from '../data/sections';

export const CoreSystemSection: React.FC = () => {
  const colorMap = {
    cyan: { bg: 'bg-cyan-500/10', border: 'border-cyan-500/20', text: 'text-cyan-400', borderHover: 'hover:border-cyan-500/50', shadow: '[0_0_30px_rgba(6,182,212,0.15)]' },
    purple: { bg: 'bg-purple-500/10', border: 'border-purple-500/20', text: 'text-purple-400', borderHover: 'hover:border-purple-500/50', shadow: '[0_0_30px_rgba(168,85,247,0.15)]' },
    emerald: { bg: 'bg-emerald-500/10', border: 'border-emerald-500/20', text: 'text-emerald-400', borderHover: 'hover:border-emerald-500/50', shadow: '[0_0_30px_rgba(16,185,129,0.15)]' },
    rose: { bg: 'bg-rose-500/10', border: 'border-rose-500/20', text: 'text-rose-400', borderHover: 'hover:border-rose-500/50', shadow: '[0_0_30px_rgba(244,63,94,0.15)]' },
  } as const;

  return (
    <section className="py-32 relative overflow-hidden bg-gradient-to-b from-[#0B1120] via-slate-900/80 to-[#0B1120]">
      {/* Background Effects */}
      <div className="absolute inset-0 z-0">
        <div className="absolute top-1/3 left-1/4 w-[500px] h-[500px] bg-amber-500/10 rounded-full blur-[120px]"></div>
        <div className="absolute bottom-1/3 right-1/4 w-[500px] h-[500px] bg-cyan-500/10 rounded-full blur-[120px]"></div>
      </div>

      <div className="max-w-7xl mx-auto px-6 relative z-10">
        {/* Section Header */}
        <div className="text-center mb-20">
          <div className="inline-flex items-center px-5 py-2 rounded-full border border-amber-500/40 bg-amber-500/10 text-amber-400 text-sm font-bold mb-6 shadow-[0_0_20px_rgba(245,158,11,0.2)]">
            <Shield className="w-4 h-4 mr-2" />
            电子会计档案核心体系
          </div>
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-6 leading-tight">
            从法规到技术的
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-amber-300 to-amber-500"> 完整闭环</span>
          </h2>
          <p className="text-xl text-slate-400 max-w-3xl mx-auto">
            基于《会计档案管理办法》(79号令)构建的四层防护体系,确保每一份电子档案都具备法律效力
          </p>
        </div>

        {/* 1. 核心法规 - 79号令 */}
        <div className="mb-16">
          <div className="bg-gradient-to-br from-amber-500/5 via-slate-900/80 to-slate-900/80 backdrop-blur border-2 border-amber-500/30 rounded-3xl p-10 shadow-[0_0_40px_rgba(245,158,11,0.15)] relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-amber-500/5 rounded-full blur-3xl"></div>
            <div className="relative z-10">
              <div className="flex items-center gap-4 mb-8">
                <div className="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center border border-amber-500/30 shadow-[0_0_20px_rgba(245,158,11,0.3)]">
                  <FileText className="w-8 h-8 text-amber-400" />
                </div>
                <div>
                  <div className="text-sm text-amber-400 font-bold mb-1">核心法规基础</div>
                  <h3 className="text-3xl font-bold text-white">财政部 79号令</h3>
                </div>
              </div>
              <div className="grid md:grid-cols-2 gap-8">
                <div className="bg-slate-900/60 border border-slate-700 rounded-2xl p-6">
                  <div className="flex items-start gap-3 mb-4">
                    <div className="w-8 h-8 bg-amber-500/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
                      <Check className="w-5 h-5 text-amber-400" />
                    </div>
                    <div>
                      <h4 className="text-white font-bold text-lg mb-2">单套制合法地位</h4>
                      <p className="text-slate-400 text-sm leading-relaxed">
                        满足条件的电子会计档案可<span className="text-amber-400 font-medium">仅以电子形式保存</span>,形成唯一法定档案,无需同时保存纸质档案
                      </p>
                    </div>
                  </div>
                </div>
                <div className="bg-slate-900/60 border border-slate-700 rounded-2xl p-6">
                  <div className="flex items-start gap-3 mb-4">
                    <div className="w-8 h-8 bg-cyan-500/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
                      <Shield className="w-5 h-5 text-cyan-400" />
                    </div>
                    <div>
                      <h4 className="text-white font-bold text-lg mb-2">法律效力保障</h4>
                      <p className="text-slate-400 text-sm leading-relaxed">
                        通过<span className="text-cyan-400 font-medium">电子签名+可靠时间戳</span>,电子档案与纸质档案具有同等法律效力,可用于审计、诉讼等场景
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 2. 四大护法 - 四性检测 */}
        <div className="mb-16">
          <div className="text-center mb-10">
            <h3 className="text-3xl font-bold text-white mb-3">四大护法 · 四性检测</h3>
            <p className="text-slate-400">系统建设的验收标准,每一项都是合规的必要条件</p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {FOUR_PROPERTIES.map((prop) => {
              const Icon = prop.icon;
              const colors = colorMap[prop.color as keyof typeof colorMap];
              return (
                <div
                  key={prop.id}
                  className={`group bg-slate-900/50 border border-slate-700 ${colors.borderHover} rounded-2xl p-6 transition-all duration-300 hover:shadow-${colors.shadow} hover:-translate-y-1`}
                >
                  <div className={`w-14 h-14 ${colors.bg} rounded-xl flex items-center justify-center mb-4 border ${colors.border} group-hover:shadow-[0_0_20px_rgba(6,182,212,0.2)] transition-all`}>
                    <div className={colors.text}><Icon className="w-7 h-7" /></div>
                  </div>
                  <h4 className="text-white font-bold text-xl mb-1">{prop.title}</h4>
                  <div className={`${colors.text}/60 text-xs font-mono mb-4 uppercase tracking-wider`}>{prop.subtitle}</div>
                  <div className="space-y-2">
                    {prop.items.map((item, j) => (
                      <div key={j} className="flex items-center gap-2 text-sm text-slate-400">
                        <div className={`w-1.5 h-1.5 rounded-full ${colors.text.replace('text-', 'bg-')}`}></div>
                        {item}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 3. 技术三件套 */}
        <div className="mb-16">
          <div className="text-center mb-10">
            <h3 className="text-3xl font-bold text-white mb-3">技术三件套</h3>
            <p className="text-slate-400">构建不可篡改、不可抵赖、可追溯的数据信任链</p>
          </div>
          <div className="grid md:grid-cols-3 gap-8">
            {TECH_SUITE.map((item, idx) => (
              <div
                key={idx}
                className={`bg-gradient-to-br ${item.gradient} backdrop-blur border ${item.border} rounded-2xl p-8 hover:shadow-[0_0_40px_rgba(0,0,0,0.3)] transition-all duration-300 group`}
              >
                <div className="mb-6">
                  <div className="w-16 h-16 bg-slate-900/50 rounded-2xl flex items-center justify-center mb-4 border border-slate-700 group-hover:scale-110 transition-transform">
                    {item.icon}
                  </div>
                  <h4 className="text-white font-bold text-xl mb-1">{item.title}</h4>
                  <div className="text-amber-400 text-sm font-medium mb-4">{item.subtitle}</div>
                </div>
                <p className="text-slate-300 text-sm leading-relaxed mb-4">{item.desc}</p>
                <div className="inline-flex items-center px-3 py-1.5 rounded-full bg-slate-900/60 border border-slate-700 text-xs text-slate-400">
                  <Cpu className="w-3 h-3 mr-1.5" />
                  {item.tech}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 4. 最终形态 - AIP包 */}
        <div>
          <div className="bg-gradient-to-br from-slate-900 via-slate-800/90 to-slate-900 border-2 border-slate-700 rounded-3xl p-10 shadow-2xl relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-cyan-500 via-purple-500 to-amber-500"></div>
            <div className="absolute -top-20 -right-20 w-64 h-64 bg-cyan-500/5 rounded-full blur-3xl"></div>

            <div className="relative z-10">
              <div className="text-center mb-12">
                <div className="inline-flex items-center px-4 py-2 rounded-full bg-gradient-to-r from-cyan-500/10 to-purple-500/10 border border-cyan-500/30 text-cyan-400 text-sm font-bold mb-4">
                  <Layers className="w-4 h-4 mr-2" />
                  最终形态
                </div>
                <h3 className="text-3xl font-bold text-white mb-3">归档信息包 (AIP Package)</h3>
                <p className="text-slate-400">一个合规的电子档案不是散乱的文件,而是结构化的&quot;包&quot;</p>
              </div>

              <div className="grid md:grid-cols-3 gap-6">
                {AIP_COMPONENTS.map((comp, i) => {
                  const Icon = comp.icon;
                  return (
                    <div key={i} className={`bg-slate-900/60 border border-${comp.color}-500/30 rounded-2xl p-6 hover:border-${comp.color}-500/50 transition-all group`}>
                      <div className="flex items-center gap-3 mb-4">
                        <div className={`w-12 h-12 bg-${comp.color}-500/10 rounded-xl flex items-center justify-center border border-${comp.color}-500/20 group-hover:shadow-[0_0_20px_rgba(6,182,212,0.2)] transition-all`}>
                          <Icon className={`w-6 h-6 text-${comp.color}-400`} />
                        </div>
                        <div>
                          <h4 className="text-white font-bold">{comp.title}</h4>
                          <div className={`text-xs text-${comp.color}-400 font-mono`}>{comp.subtitle}</div>
                        </div>
                      </div>
                      <ul className="space-y-2 text-sm text-slate-400">
                        {comp.items.map((item, j) => (
                          <li key={j} className="flex items-start gap-2">
                            <ChevronRight className={`w-4 h-4 text-${comp.color}-500 flex-shrink-0 mt-0.5`} />
                            {item}
                          </li>
                        ))}
                      </ul>
                    </div>
                  );
                })}
              </div>

              {/* AIP包结构示意 */}
              <div className="mt-8 bg-slate-950/50 border border-slate-700 rounded-xl p-6">
                <div className="flex items-center gap-2 mb-4 text-slate-400 text-sm font-mono">
                  <Server className="w-4 h-4" />
                  /AIP_Root/ARC-2024-001.zip
                </div>
                <div className="space-y-1 text-sm font-mono">
                  {AIP_STRUCTURE.map((item, i) => (
                    <div key={i} className="flex items-center gap-2 text-slate-500 pl-4">
                      <div className={`w-1 h-1 rounded-full bg-${item.color}-500`}></div>
                      <span className={`text-${item.color}-400`}>{item.name}</span>
                      <span className="text-slate-600">{item.desc}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
