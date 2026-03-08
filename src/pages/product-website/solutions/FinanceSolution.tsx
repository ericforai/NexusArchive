// Input: 支柱页面配置 (Pillar Config)
// Output: 行业解决方案支柱页面组件
// Pos: src/pages/product-website/solutions/FinanceSolution.tsx

import React from 'react';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { Shield, BarChart, Server, CheckCircle } from 'lucide-react';

export const FinanceSolution: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            {/* Hero Section */}
            <section className="relative pt-32 pb-20 overflow-hidden">
                <div className="max-w-7xl mx-auto px-6 relative z-10 text-center">
                    <h1 className="text-5xl md:text-7xl font-bold text-white mb-8">
                        金融行业 <br />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">电子档案合规归档</span>
                    </h1>
                    <p className="text-xl text-slate-400 max-w-3xl mx-auto mb-10 leading-relaxed">
                        针对银行、证券、保险机构的极速归档、海量存储与极致安全需求。
                        助力金融机构从物理库房向“云档案”转型。
                    </p>
                </div>
            </section>

            {/* Value Pillars */}
            <section className="py-20 bg-slate-900/30">
                <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-3 gap-12">
                    <div className="p-8 bg-slate-800/50 rounded-3xl border border-slate-700/50">
                        <Shield className="w-10 h-10 text-cyan-500 mb-6" />
                        <h3 className="text-2xl font-bold text-white mb-4">极致合规</h3>
                        <p className="text-slate-400">支持国密 SM3 加密、四性检测及 DA/T 94-2022 标准全链路追踪。</p>
                    </div>
                    <div className="p-8 bg-slate-800/50 rounded-3xl border border-slate-700/50">
                        <BarChart className="w-10 h-10 text-blue-500 mb-6" />
                        <h3 className="text-2xl font-bold text-white mb-4">海量吞吐</h3>
                        <p className="text-slate-400">分布式架构，日处理千万量级凭证。无惧业务高峰压力。</p>
                    </div>
                    <div className="p-8 bg-slate-800/50 rounded-3xl border border-slate-700/50">
                        <Server className="w-10 h-10 text-cyan-400 mb-6" />
                        <h3 className="text-2xl font-bold text-white mb-4">信创适配</h3>
                        <p className="text-slate-400">原生适配国产 OS（麒麟/统信）及数据库（达梦/人大金仓）。</p>
                    </div>
                </div>
            </section>

            {/* Cluster Linking Section (内链闭环) */}
            <section className="py-20 border-t border-slate-800">
                <div className="max-w-4xl mx-auto px-6">
                    <h2 className="text-3xl font-bold text-white mb-10 text-center">行业深度解读</h2>
                    <div className="space-y-6">
                        <a href="/blog/dat-94-2022-interpretation" className="flex items-center justify-between p-6 bg-slate-900/50 border border-slate-800 rounded-2xl hover:border-cyan-500 transition-all group">
                            <div>
                                <h4 className="text-lg font-bold text-white mb-1">DA/T 94-2022 规范对金融机构的影响</h4>
                                <p className="text-sm text-slate-500">深入解析标准合规项，避免审计风险</p>
                            </div>
                            <CheckCircle className="w-6 h-6 text-cyan-500 opacity-0 group-hover:opacity-100 transition-opacity" />
                        </a>
                    </div>
                </div>
            </section>

            <Footer />
        </div>
    );
};

export default FinanceSolution;
