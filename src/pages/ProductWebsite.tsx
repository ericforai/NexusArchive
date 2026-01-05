// Input: React、lucide-react 图标、react-router-dom 路由、本地模块 ProductWebsite.css、types
// Output: React 组件 ProductWebsite
// Pos: src/pages/ProductWebsite.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import './ProductWebsite.css';
import { Shield, Lock, Server, Search, Activity, ChevronRight, Check, Database, FileText, Cpu, Layers, Key, TrendingUp, AlertTriangle, XCircle, CheckCircle2, Zap, Globe } from 'lucide-react';

import { useNavigate } from 'react-router-dom';

export const ProductWebsite: React.FC = () => {
    const navigate = useNavigate();
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 50);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans selection:bg-cyan-500/30">
            {/* Navigation */}
            <nav className={`fixed w-full z-50 transition-all duration-300 border-b ${scrolled ? 'bg-[#0B1120]/90 backdrop-blur border-slate-800 py-4' : 'bg-transparent border-transparent py-6'}`}>
                <div className="max-w-7xl mx-auto px-6 flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-lg flex items-center justify-center shadow-[0_0_20px_rgba(6,182,212,0.5)]">
                            <Shield className="w-6 h-6 text-white" />
                        </div>
                        <span className="text-2xl font-bold text-white tracking-tight">
                            DigiVoucher<span className="text-cyan-400">数凭档案</span>
                        </span>
                    </div>
                    <div className="flex gap-4">
                        <button
                            onClick={() => navigate('/system')}
                            className="px-6 py-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-900 font-bold rounded-lg shadow-[0_0_15px_rgba(245,158,11,0.4)] transition-all transform hover:scale-105"
                        >
                            立即体验
                        </button>
                    </div>
                </div>
            </nav>

            {/* Hero Section */}
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
                        <Shield className="w-4 h-4 mr-2" />
                        符合 DA/T 94-2022 国家标准
                    </div>
                    <h1 className="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight animate-in fade-in slide-in-from-bottom-8 duration-700 delay-100">
                        让每一张凭证都成为<br />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">
                            合法的数字资产
                        </span>
                    </h1>
                    <p className="text-xl text-slate-400 mb-10 max-w-3xl mx-auto animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200 leading-relaxed">
                        告别纸质档案库房，开启单套制归档新时代。<br />
                        专为大型企业打造，从 ERP 到档案库，实现全链路自动化、无纸化、合规化。
                    </p>
                    <div className="flex flex-col sm:flex-row justify-center gap-6 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300">
                        <button onClick={() => navigate('/system')} className="px-8 py-4 bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-bold rounded-xl shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-all flex items-center justify-center transform hover:scale-105">
                            立即体验 <ChevronRight className="w-5 h-5 ml-2" />
                        </button>
                        <button className="px-8 py-4 bg-slate-800/50 hover:bg-slate-800 border border-slate-700 hover:border-cyan-500/50 text-white rounded-xl transition-all backdrop-blur-sm flex items-center justify-center">
                            预约专家顾问
                        </button>
                    </div>
                </div>

                {/* Animated Stream (Abstract Representation) */}
                <div className="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-[#0B1120] to-transparent z-20"></div>
            </section>

            {/* Pain Points vs Solution Section */}
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
                            {[
                                {
                                    pain: { title: "查找困难", desc: "翻箱倒柜找凭证，耗时耗力，效率极低" },
                                    gain: { title: "秒级检索", desc: "亿级数据毫秒响应，支持穿透式联查", icon: <Search className="w-5 h-5 text-cyan-400" /> }
                                },
                                {
                                    pain: { title: "风险高企", desc: "面临发霉、虫蛀、火灾等物理损毁风险" },
                                    gain: { title: "永久安全", desc: "四性检测 + 异地备份 + 国密加密，数据万无一失", icon: <Shield className="w-5 h-5 text-cyan-400" /> }
                                },
                                {
                                    pain: { title: "成本惊人", desc: "库房租金、打印耗材、人工管理费用高昂" },
                                    gain: { title: "零成本存储", desc: "无纸化存储，节省 90% 以上物理空间与耗材", icon: <Database className="w-5 h-5 text-cyan-400" /> }
                                },
                                {
                                    pain: { title: "审计繁琐", desc: "审计进场需搬运大量案卷，配合工作量大" },
                                    gain: { title: "远程审计", desc: "授权账号在线调阅，轻松应对内外部审计", icon: <Globe className="w-5 h-5 text-cyan-400" /> }
                                }
                            ].map((item, i) => (
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

            {/* Data Value Section */}
            <section className="py-12 border-y border-slate-800 bg-slate-900/30">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
                        {[
                            { val: "90%", label: "存储成本降低", icon: <TrendingUp className="w-5 h-5 text-green-400" /> },
                            { val: "100%", label: "单套制合规", icon: <CheckCircle2 className="w-5 h-5 text-cyan-400" /> },
                            { val: "10x", label: "归档效率提升", icon: <Zap className="w-5 h-5 text-amber-400" /> },
                            { val: "0", label: "纸质凭证打印", icon: <FileText className="w-5 h-5 text-slate-400" /> }
                        ].map((stat, i) => (
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
                        {[
                            {
                                icon: <Search className="w-8 h-8 text-cyan-400" />,
                                title: "智能 OCR 识别",
                                desc: "高精度识别发票、银行回单，自动提取关键元数据，准确率高达 99.9%。"
                            },
                            {
                                icon: <Activity className="w-8 h-8 text-amber-400" />,
                                title: "四性检测引擎",
                                desc: "自动进行真实性、完整性、可用性、安全性检测，确保归档数据合规。"
                            },
                            {
                                icon: <Layers className="w-8 h-8 text-purple-400" />,
                                title: "穿透式联查",
                                desc: "打破数据孤岛，实现从报表到账簿、凭证、原始单据的全链路穿透。"
                            }
                        ].map((feature, idx) => (
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

            {/* 电子会计档案四大核心要素 */}
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
                            {/* 真实性 */}
                            <div className="group bg-slate-900/50 border border-slate-700 hover:border-cyan-500/50 rounded-2xl p-6 transition-all duration-300 hover:shadow-[0_0_30px_rgba(6,182,212,0.15)] hover:-translate-y-1">
                                <div className="w-14 h-14 bg-cyan-500/10 rounded-xl flex items-center justify-center mb-4 border border-cyan-500/20 group-hover:shadow-[0_0_20px_rgba(6,182,212,0.2)] transition-all">
                                    <div className="text-cyan-400"><Shield className="w-7 h-7" /></div>
                                </div>
                                <h4 className="text-white font-bold text-xl mb-1">真实性</h4>
                                <div className="text-cyan-400/60 text-xs font-mono mb-4 uppercase tracking-wider">Authenticity</div>
                                <div className="space-y-2">
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-cyan-400"></div>
                                        哈希值校验
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-cyan-400"></div>
                                        数字签名验证
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-cyan-400"></div>
                                        来源系统追溯
                                    </div>
                                </div>
                            </div>

                            {/* 完整性 */}
                            <div className="group bg-slate-900/50 border border-slate-700 hover:border-purple-500/50 rounded-2xl p-6 transition-all duration-300 hover:shadow-[0_0_30px_rgba(168,85,247,0.15)] hover:-translate-y-1">
                                <div className="w-14 h-14 bg-purple-500/10 rounded-xl flex items-center justify-center mb-4 border border-purple-500/20 group-hover:shadow-[0_0_20px_rgba(168,85,247,0.2)] transition-all">
                                    <div className="text-purple-400"><Database className="w-7 h-7" /></div>
                                </div>
                                <h4 className="text-white font-bold text-xl mb-1">完整性</h4>
                                <div className="text-purple-400/60 text-xs font-mono mb-4 uppercase tracking-wider">Integrity</div>
                                <div className="space-y-2">
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-purple-400"></div>
                                        元数据完整
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-purple-400"></div>
                                        附件齐全
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-purple-400"></div>
                                        关联关系完整
                                    </div>
                                </div>
                            </div>

                            {/* 可用性 */}
                            <div className="group bg-slate-900/50 border border-slate-700 hover:border-emerald-500/50 rounded-2xl p-6 transition-all duration-300 hover:shadow-[0_0_30px_rgba(16,185,129,0.15)] hover:-translate-y-1">
                                <div className="w-14 h-14 bg-emerald-500/10 rounded-xl flex items-center justify-center mb-4 border border-emerald-500/20 group-hover:shadow-[0_0_20px_rgba(16,185,129,0.2)] transition-all">
                                    <div className="text-emerald-400"><FileText className="w-7 h-7" /></div>
                                </div>
                                <h4 className="text-white font-bold text-xl mb-1">可用性</h4>
                                <div className="text-emerald-400/60 text-xs font-mono mb-4 uppercase tracking-wider">Usability</div>
                                <div className="space-y-2">
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
                                        格式可读
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
                                        长期保存
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
                                        随时调阅
                                    </div>
                                </div>
                            </div>

                            {/* 安全性 */}
                            <div className="group bg-slate-900/50 border border-slate-700 hover:border-rose-500/50 rounded-2xl p-6 transition-all duration-300 hover:shadow-[0_0_30px_rgba(244,63,94,0.15)] hover:-translate-y-1">
                                <div className="w-14 h-14 bg-rose-500/10 rounded-xl flex items-center justify-center mb-4 border border-rose-500/20 group-hover:shadow-[0_0_20px_rgba(244,63,94,0.2)] transition-all">
                                    <div className="text-rose-400"><Lock className="w-7 h-7" /></div>
                                </div>
                                <h4 className="text-white font-bold text-xl mb-1">安全性</h4>
                                <div className="text-rose-400/60 text-xs font-mono mb-4 uppercase tracking-wider">Security</div>
                                <div className="space-y-2">
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-rose-400"></div>
                                        权限控制
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-rose-400"></div>
                                        审计日志
                                    </div>
                                    <div className="flex items-center gap-2 text-sm text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-rose-400"></div>
                                        备份恢复
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 3. 技术三件套 */}
                    <div className="mb-16">
                        <div className="text-center mb-10">
                            <h3 className="text-3xl font-bold text-white mb-3">技术三件套</h3>
                            <p className="text-slate-400">构建不可篡改、不可抵赖、可追溯的数据信任链</p>
                        </div>
                        <div className="grid md:grid-cols-3 gap-8">
                            {[
                                {
                                    title: "哈希值 (SM3/SHA256)",
                                    subtitle: "防篡改",
                                    icon: <Key className="w-8 h-8 text-cyan-400" />,
                                    desc: "为每个文件生成唯一数字指纹,任何微小改动都会导致哈希值完全不同",
                                    tech: "国密SM3算法",
                                    gradient: "from-cyan-500/10 to-blue-500/10",
                                    border: "border-cyan-500/30"
                                },
                                {
                                    title: "CA证书 (电子签名)",
                                    subtitle: "防抵赖 / 定身份",
                                    icon: <Shield className="w-8 h-8 text-amber-400" />,
                                    desc: "通过权威CA机构颁发的数字证书,确保签名人身份真实且不可否认",
                                    tech: "PKI公钥基础设施",
                                    gradient: "from-amber-500/10 to-orange-500/10",
                                    border: "border-amber-500/30"
                                },
                                {
                                    title: "可靠时间戳 (TSA)",
                                    subtitle: "定时间",
                                    icon: <Activity className="w-8 h-8 text-purple-400" />,
                                    desc: "由国家授时中心提供的可信时间证明,精确记录档案形成时刻",
                                    tech: "RFC3161标准",
                                    gradient: "from-purple-500/10 to-pink-500/10",
                                    border: "border-purple-500/30"
                                }
                            ].map((item, idx) => (
                                <div key={idx} className={`bg-gradient-to-br ${item.gradient} backdrop-blur border ${item.border} rounded-2xl p-8 hover:shadow-[0_0_40px_rgba(0,0,0,0.3)] transition-all duration-300 group`}>
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
                                    {/* 元数据 */}
                                    <div className="bg-slate-900/60 border border-cyan-500/30 rounded-2xl p-6 hover:border-cyan-500/50 transition-all group">
                                        <div className="flex items-center gap-3 mb-4">
                                            <div className="w-12 h-12 bg-cyan-500/10 rounded-xl flex items-center justify-center border border-cyan-500/20 group-hover:shadow-[0_0_20px_rgba(6,182,212,0.2)] transition-all">
                                                <Database className="w-6 h-6 text-cyan-400" />
                                            </div>
                                            <div>
                                                <h4 className="text-white font-bold">元数据 (XML)</h4>
                                                <div className="text-xs text-cyan-400 font-mono">Metadata</div>
                                            </div>
                                        </div>
                                        <ul className="space-y-2 text-sm text-slate-400">
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-cyan-500 flex-shrink-0 mt-0.5" />
                                                档案编号、题名、日期
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-cyan-500 flex-shrink-0 mt-0.5" />
                                                保管期限、密级、全宗号
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-cyan-500 flex-shrink-0 mt-0.5" />
                                                业务字段(金额、科目等)
                                            </li>
                                        </ul>
                                    </div>

                                    {/* 版式文件 */}
                                    <div className="bg-slate-900/60 border border-purple-500/30 rounded-2xl p-6 hover:border-purple-500/50 transition-all group">
                                        <div className="flex items-center gap-3 mb-4">
                                            <div className="w-12 h-12 bg-purple-500/10 rounded-xl flex items-center justify-center border border-purple-500/20 group-hover:shadow-[0_0_20px_rgba(168,85,247,0.2)] transition-all">
                                                <FileText className="w-6 h-6 text-purple-400" />
                                            </div>
                                            <div>
                                                <h4 className="text-white font-bold">版式文件</h4>
                                                <div className="text-xs text-purple-400 font-mono">Content</div>
                                            </div>
                                        </div>
                                        <ul className="space-y-2 text-sm text-slate-400">
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-purple-500 flex-shrink-0 mt-0.5" />
                                                OFD格式(国产版式标准)
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-purple-500 flex-shrink-0 mt-0.5" />
                                                PDF/A(长期保存格式)
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-purple-500 flex-shrink-0 mt-0.5" />
                                                原始附件(发票、回单等)
                                            </li>
                                        </ul>
                                    </div>

                                    {/* 电子签名 */}
                                    <div className="bg-slate-900/60 border border-amber-500/30 rounded-2xl p-6 hover:border-amber-500/50 transition-all group">
                                        <div className="flex items-center gap-3 mb-4">
                                            <div className="w-12 h-12 bg-amber-500/10 rounded-xl flex items-center justify-center border border-amber-500/20 group-hover:shadow-[0_0_20px_rgba(245,158,11,0.2)] transition-all">
                                                <Lock className="w-6 h-6 text-amber-400" />
                                            </div>
                                            <div>
                                                <h4 className="text-white font-bold">电子签名</h4>
                                                <div className="text-xs text-amber-400 font-mono">Signature</div>
                                            </div>
                                        </div>
                                        <ul className="space-y-2 text-sm text-slate-400">
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" />
                                                归档人员数字签名
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" />
                                                可靠时间戳(TSA)
                                            </li>
                                            <li className="flex items-start gap-2">
                                                <ChevronRight className="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" />
                                                SM3哈希值封装
                                            </li>
                                        </ul>
                                    </div>
                                </div>

                                {/* AIP包结构示意 */}
                                <div className="mt-8 bg-slate-950/50 border border-slate-700 rounded-xl p-6">
                                    <div className="flex items-center gap-2 mb-4 text-slate-400 text-sm font-mono">
                                        <Server className="w-4 h-4" />
                                        /AIP_Root/ARC-2024-001.zip
                                    </div>
                                    <div className="space-y-1 text-sm font-mono">
                                        <div className="flex items-center gap-2 text-slate-500 pl-4">
                                            <div className="w-1 h-1 rounded-full bg-cyan-500"></div>
                                            <span className="text-cyan-400">/Metadata</span>
                                            <span className="text-slate-600">→ metadata.xml (DA/T 94标准)</span>
                                        </div>
                                        <div className="flex items-center gap-2 text-slate-500 pl-4">
                                            <div className="w-1 h-1 rounded-full bg-purple-500"></div>
                                            <span className="text-purple-400">/Content</span>
                                            <span className="text-slate-600">→ voucher_001.ofd, invoice_001.pdf</span>
                                        </div>
                                        <div className="flex items-center gap-2 text-slate-500 pl-4">
                                            <div className="w-1 h-1 rounded-full bg-amber-500"></div>
                                            <span className="text-amber-400">/Signature</span>
                                            <span className="text-slate-600">→ signature.p7s, timestamp.tsr</span>
                                        </div>
                                        <div className="flex items-center gap-2 text-slate-500 pl-4">
                                            <div className="w-1 h-1 rounded-full bg-emerald-500"></div>
                                            <span className="text-emerald-400">/Logs</span>
                                            <span className="text-slate-600">→ audit_trail.log</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Product Showcase (High-Fidelity Mockup) */}
            <section className="py-24 bg-slate-900/30 relative overflow-hidden">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="mb-16 flex items-end justify-between">
                        <div>
                            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">沉浸式管理体验</h2>
                            <p className="text-slate-400">专为财务人员设计的现代化工作台</p>
                        </div>
                        <div className="hidden md:flex gap-2">
                            <div className="w-3 h-3 rounded-full bg-red-500"></div>
                            <div className="w-3 h-3 rounded-full bg-amber-500"></div>
                            <div className="w-3 h-3 rounded-full bg-green-500"></div>
                        </div>
                    </div>

                    {/* CSS Browser Window Mockup */}
                    <div className="rounded-xl border border-slate-700 bg-[#0F172A] shadow-2xl overflow-hidden transform hover:scale-[1.01] transition-transform duration-500">
                        {/* Browser Header */}
                        <div className="bg-slate-800 px-4 py-3 flex items-center gap-4 border-b border-slate-700">
                            <div className="flex gap-2">
                                <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
                                <div className="w-3 h-3 rounded-full bg-amber-500/80"></div>
                                <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
                            </div>
                            <div className="flex-1 max-w-2xl mx-auto bg-slate-900 rounded-md px-4 py-1.5 text-xs text-slate-500 font-mono text-center flex items-center justify-center gap-2">
                                <Lock className="w-3 h-3" />
                                DigiVoucher.internal/dashboard
                            </div>
                        </div>

                        {/* App Interface */}
                        <div className="flex h-[600px]">
                            {/* Sidebar */}
                            <div className="w-64 bg-slate-900 border-r border-slate-800 p-4 hidden md:block">
                                <div className="flex items-center gap-2 mb-8 px-2">
                                    <div className="w-6 h-6 bg-cyan-600 rounded flex items-center justify-center">
                                        <Shield className="w-4 h-4 text-white" />
                                    </div>
                                    <span className="font-bold text-white">DigiVoucher</span>
                                </div>
                                <div className="space-y-1">
                                    {['工作台', '档案收集', '档案管理', '借阅中心', '系统设置'].map((item, i) => (
                                        <button
                                            key={i}
                                            onClick={() => {
                                                // Update active demo view
                                                const demoContent = document.getElementById('demo-content');
                                                if (demoContent) {
                                                    demoContent.setAttribute('data-active-view', i.toString());
                                                }
                                                // Update active state
                                                document.querySelectorAll('.demo-nav-item').forEach((el, idx) => {
                                                    if (idx === i) {
                                                        el.classList.add('bg-cyan-500/10', 'text-cyan-400', 'border', 'border-cyan-500/20');
                                                        el.classList.remove('text-slate-400', 'hover:bg-slate-800');
                                                    } else {
                                                        el.classList.remove('bg-cyan-500/10', 'text-cyan-400', 'border', 'border-cyan-500/20');
                                                        el.classList.add('text-slate-400', 'hover:bg-slate-800');
                                                    }
                                                });
                                            }}
                                            className={`demo-nav-item w-full text-left px-3 py-2 rounded-lg text-sm transition-all cursor-pointer ${i === 0 ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' : 'text-slate-400 hover:bg-slate-800'}`}
                                        >
                                            {item}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Main Content */}
                            <div id="demo-content" data-active-view="0" className="flex-1 bg-[#0B1120] p-8 overflow-auto">
                                {/* View 0: 工作台 */}
                                <div className="demo-view" data-view="0">
                                    <div className="flex justify-between items-center mb-8">
                                        <h3 className="text-2xl font-bold text-white">DigiVoucher 智能归档</h3>
                                        <div className="flex gap-3">
                                            <span className="px-3 py-1 rounded-full bg-green-500/10 text-green-400 text-xs border border-green-500/20 flex items-center gap-1">
                                                <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
                                                系统运行正常
                                            </span>
                                        </div>
                                    </div>

                                    {/* Stats Cards */}
                                    <div className="grid grid-cols-4 gap-6 mb-8">
                                        {[
                                            { label: '待归档凭证', val: '1,284', color: 'text-cyan-400', bg: 'bg-cyan-500/10' },
                                            { label: '本月新增', val: '456', color: 'text-purple-400', bg: 'bg-purple-500/10' },
                                            { label: '借阅申请', val: '12', color: 'text-amber-400', bg: 'bg-amber-500/10' },
                                            { label: '存储占用', val: '2.4 TB', color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
                                        ].map((stat, i) => (
                                            <div key={i} className="bg-slate-900/50 border border-slate-800 p-5 rounded-xl">
                                                <div className="text-slate-400 text-sm mb-2">{stat.label}</div>
                                                <div className={`text-2xl font-bold ${stat.color}`}>{stat.val}</div>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Chart Area */}
                                    <div className="grid grid-cols-3 gap-6 h-64">
                                        <div className="col-span-2 bg-slate-900/50 border border-slate-800 rounded-xl p-6 relative overflow-hidden">
                                            <h4 className="text-white font-medium mb-4">归档趋势</h4>
                                            <div className="absolute bottom-0 left-0 right-0 h-48 flex items-end justify-between px-6 pb-6 gap-2">
                                                {[30, 45, 35, 60, 50, 75, 65, 80, 70, 90, 85, 95].map((h, i) => (
                                                    <div key={i} className="w-full bg-cyan-500/20 hover:bg-cyan-500/40 transition-all rounded-t-sm relative group" style={{ height: `${h}%` }}>
                                                        <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity">
                                                            {h * 10}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
                                            <h4 className="text-white font-medium mb-4">档案构成</h4>
                                            <div className="flex items-center justify-center h-40 relative">
                                                <div className="w-32 h-32 rounded-full border-8 border-slate-800 border-t-cyan-500 border-r-purple-500 border-b-amber-500 rotate-45"></div>
                                                <div className="absolute text-center">
                                                    <div className="text-2xl font-bold text-white">98%</div>
                                                    <div className="text-xs text-slate-500">合规率</div>
                                                </div>
                                            </div>
                                            <div className="flex justify-center gap-4 text-xs text-slate-400 mt-2">
                                                <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-cyan-500"></div>凭证</span>
                                                <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-purple-500"></div>账簿</span>
                                                <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-amber-500"></div>报表</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* View 1: 档案收集 */}
                                <div className="demo-view hidden" data-view="1">
                                    <div className="flex justify-between items-center mb-8">
                                        <h3 className="text-2xl font-bold text-white">档案收集</h3>
                                        <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
                                            + 新建收集任务
                                        </button>
                                    </div>
                                    <div className="space-y-4">
                                        {[
                                            { name: '2024年12月财务凭证', status: '进行中', count: 234, color: 'text-cyan-400' },
                                            { name: '2024年11月财务凭证', status: '已完成', count: 456, color: 'text-green-400' },
                                            { name: '2024年10月财务凭证', status: '已完成', count: 423, color: 'text-green-400' },
                                        ].map((task, i) => (
                                            <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl hover:border-cyan-500/30 transition-all">
                                                <div className="flex justify-between items-center">
                                                    <div>
                                                        <h4 className="text-white font-medium mb-2">{task.name}</h4>
                                                        <div className="flex gap-4 text-sm text-slate-400">
                                                            <span>状态: <span className={task.color}>{task.status}</span></span>
                                                            <span>文件数: {task.count}</span>
                                                        </div>
                                                    </div>
                                                    <ChevronRight className="w-5 h-5 text-slate-600" />
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* View 2: 档案管理 */}
                                <div className="demo-view hidden" data-view="2">
                                    <div className="flex justify-between items-center mb-8">
                                        <h3 className="text-2xl font-bold text-white">档案管理</h3>
                                        <div className="flex gap-2">
                                            <button className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-sm font-medium transition-colors">
                                                四性检测
                                            </button>
                                            <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
                                                正式归档
                                            </button>
                                        </div>
                                    </div>
                                    <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden">
                                        <table className="w-full text-sm">
                                            <thead className="bg-slate-800/50 border-b border-slate-700">
                                                <tr className="text-slate-400">
                                                    <th className="text-left p-4">档案编号</th>
                                                    <th className="text-left p-4">凭证类型</th>
                                                    <th className="text-left p-4">金额</th>
                                                    <th className="text-left p-4">状态</th>
                                                    <th className="text-left p-4">操作</th>
                                                </tr>
                                            </thead>
                                            <tbody className="text-slate-300">
                                                {[
                                                    { code: '记-202511-001', type: '出差审批单', amount: '¥2,580.00', status: '待归档' },
                                                    { code: 'ARC-2024-001', type: '记账凭证', amount: '¥125,000', status: '已归档' },
                                                    { code: 'ARC-2024-002', type: '银行回单', amount: '¥89,500', status: '待审批' },
                                                    { code: 'ARC-2024-003', type: '发票', amount: '¥45,200', status: '已归档' },
                                                ].map((item, i) => (
                                                    <tr key={i} className="border-b border-slate-800 hover:bg-slate-800/30">
                                                        <td className="p-4 text-cyan-400">{item.code}</td>
                                                        <td className="p-4">{item.type}</td>
                                                        <td className="p-4">{item.amount}</td>
                                                        <td className="p-4">
                                                            <span className={`px-2 py-1 rounded text-xs ${item.status === '已归档' ? 'bg-green-500/10 text-green-400' : 'bg-amber-500/10 text-amber-400'}`}>
                                                                {item.status}
                                                            </span>
                                                        </td>
                                                        <td className="p-4">
                                                            <button className="text-cyan-400 hover:text-cyan-300 text-xs flex items-center gap-1 transition-colors">
                                                                <Search className="w-3 h-3" /> 预览
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>

                                {/* View 3: 借阅中心 */}
                                <div className="demo-view hidden" data-view="3">
                                    <div className="flex justify-between items-center mb-8">
                                        <h3 className="text-2xl font-bold text-white">借阅中心</h3>
                                        <button className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-900 rounded-lg text-sm font-medium transition-colors">
                                            + 新建借阅申请
                                        </button>
                                    </div>
                                    <div className="grid grid-cols-3 gap-6 mb-8">
                                        {[
                                            { label: '待审批', count: 5, color: 'text-amber-400' },
                                            { label: '借阅中', count: 12, color: 'text-cyan-400' },
                                            { label: '已归还', count: 234, color: 'text-green-400' },
                                        ].map((stat, i) => (
                                            <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl">
                                                <div className="text-slate-400 text-sm mb-2">{stat.label}</div>
                                                <div className={`text-3xl font-bold ${stat.color}`}>{stat.count}</div>
                                            </div>
                                        ))}
                                    </div>
                                    <div className="space-y-4">
                                        {[
                                            { user: '张三', dept: '财务部', doc: 'ARC-2024-001', date: '2024-12-01', status: '借阅中' },
                                            { user: '李四', dept: '审计部', doc: 'ARC-2024-045', date: '2024-11-28', status: '待审批' },
                                        ].map((item, i) => (
                                            <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl">
                                                <div className="flex justify-between items-center">
                                                    <div className="space-y-2">
                                                        <div className="text-white font-medium">{item.user} - {item.dept}</div>
                                                        <div className="text-sm text-slate-400">档案编号: <span className="text-cyan-400">{item.doc}</span></div>
                                                        <div className="text-sm text-slate-400">申请时间: {item.date}</div>
                                                    </div>
                                                    <span className={`px-3 py-1 rounded-full text-xs ${item.status === '借阅中' ? 'bg-cyan-500/10 text-cyan-400' : 'bg-amber-500/10 text-amber-400'}`}>
                                                        {item.status}
                                                    </span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* View 4: 系统设置 */}
                                <div className="demo-view hidden" data-view="4">
                                    <div className="mb-8">
                                        <h3 className="text-2xl font-bold text-white mb-2">系统设置</h3>
                                        <p className="text-slate-400 text-sm">配置系统参数和权限管理</p>
                                    </div>
                                    <div className="space-y-6">
                                        {[
                                            { title: '用户权限管理', desc: '配置用户角色和访问权限', icon: <Key className="w-5 h-5" /> },
                                            { title: '归档规则配置', desc: '设置自动归档规则和保管期限', icon: <FileText className="w-5 h-5" /> },
                                            { title: '系统日志', desc: '查看系统操作日志和审计记录', icon: <Activity className="w-5 h-5" /> },
                                            { title: '数据备份', desc: '配置自动备份策略', icon: <Database className="w-5 h-5" /> },
                                        ].map((setting, i) => (
                                            <div key={i} className="bg-slate-900/50 border border-slate-800 p-6 rounded-xl hover:border-cyan-500/30 transition-all cursor-pointer group">
                                                <div className="flex items-center gap-4">
                                                    <div className="w-12 h-12 bg-slate-800 rounded-lg flex items-center justify-center text-cyan-400 group-hover:bg-cyan-500/10 transition-colors">
                                                        {setting.icon}
                                                    </div>
                                                    <div className="flex-1">
                                                        <h4 className="text-white font-medium mb-1">{setting.title}</h4>
                                                        <p className="text-sm text-slate-400">{setting.desc}</p>
                                                    </div>
                                                    <ChevronRight className="w-5 h-5 text-slate-600 group-hover:text-cyan-400 transition-colors" />
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <style>{`
                                    .demo-view { display: none; }
                                    #demo-content[data-active-view="0"] .demo-view[data-view="0"] { display: block; }
                                    #demo-content[data-active-view="1"] .demo-view[data-view="1"] { display: block; }
                                    #demo-content[data-active-view="2"] .demo-view[data-view="2"] { display: block; }
                                    #demo-content[data-active-view="3"] .demo-view[data-view="3"] { display: block; }
                                    #demo-content[data-active-view="4"] .demo-view[data-view="4"] { display: block; }
                                `}</style>
                            </div>
                        </div>
                    </div>
                </div>
            </section >

            {/* Compliance Architecture */}
            < section className="py-24 relative" >
                <div className="max-w-5xl mx-auto px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">信创合规架构</h2>
                        <p className="text-slate-400">完全符合 DA/T 94-2022 电子会计档案管理规范</p>
                    </div>

                    <div className="relative">
                        {/* Connecting Lines */}
                        <div className="absolute left-1/2 top-0 bottom-0 w-px bg-gradient-to-b from-transparent via-cyan-500/30 to-transparent"></div>

                        <div className="space-y-8 relative z-10">
                            {/* Layer 1 */}
                            <div className="bg-slate-900/80 backdrop-blur border border-slate-700 rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden group hover:border-cyan-500/50 transition-all">
                                <div className="absolute top-0 left-0 w-1 h-full bg-cyan-500"></div>
                                <div className="flex items-center justify-between mb-6">
                                    <h3 className="text-xl font-bold text-white flex items-center gap-2">
                                        <Layers className="w-5 h-5 text-cyan-400" /> 业务应用层
                                    </h3>
                                    <span className="text-xs font-mono text-slate-500">Application Layer</span>
                                </div>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    {['档案采集', '四性检测', '借阅利用', '鉴定销毁'].map((item, i) => (
                                        <div key={i} className="bg-slate-800/50 border border-slate-700 rounded-lg py-2 text-center text-sm text-slate-300">
                                            {item}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Layer 2 */}
                            <div className="bg-gradient-to-r from-slate-900/90 to-slate-800/90 backdrop-blur border border-amber-500/30 rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden shadow-[0_0_30px_rgba(245,158,11,0.1)]">
                                <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
                                <div className="flex items-center justify-between mb-6">
                                    <h3 className="text-xl font-bold text-amber-400 flex items-center gap-2">
                                        <Shield className="w-5 h-5" /> 安全合规层
                                    </h3>
                                    <span className="text-xs font-mono text-amber-500/50">Security Layer</span>
                                </div>
                                <div className="grid grid-cols-3 gap-6">
                                    <div className="text-center">
                                        <div className="w-10 h-10 mx-auto bg-amber-500/10 rounded-full flex items-center justify-center mb-2">
                                            <Key className="w-5 h-5 text-amber-400" />
                                        </div>
                                        <div className="text-white font-medium text-sm">三员分立</div>
                                        <div className="text-xs text-slate-500 mt-1">系统/安全/审计管理员</div>
                                    </div>
                                    <div className="text-center">
                                        <div className="w-10 h-10 mx-auto bg-amber-500/10 rounded-full flex items-center justify-center mb-2">
                                            <FileText className="w-5 h-5 text-amber-400" />
                                        </div>
                                        <div className="text-white font-medium text-sm">SM3 哈希摘要</div>
                                        <div className="text-xs text-slate-500 mt-1">国密算法防篡改</div>
                                    </div>
                                    <div className="text-center">
                                        <div className="w-10 h-10 mx-auto bg-amber-500/10 rounded-full flex items-center justify-center mb-2">
                                            <Lock className="w-5 h-5 text-amber-400" />
                                        </div>
                                        <div className="text-white font-medium text-sm">电子签名</div>
                                        <div className="text-xs text-slate-500 mt-1">法律效力保障</div>
                                    </div>
                                </div>
                            </div>

                            {/* Layer 3 */}
                            <div className="bg-slate-900/80 backdrop-blur border border-slate-700 rounded-2xl p-8 max-w-3xl mx-auto relative overflow-hidden group hover:border-cyan-500/50 transition-all">
                                <div className="absolute top-0 left-0 w-1 h-full bg-blue-600"></div>
                                <div className="flex items-center justify-between mb-6">
                                    <h3 className="text-xl font-bold text-white flex items-center gap-2">
                                        <Server className="w-5 h-5 text-blue-400" /> 信创基础设施层
                                    </h3>
                                    <span className="text-xs font-mono text-slate-500">Infrastructure Layer</span>
                                </div>
                                <div className="grid grid-cols-3 gap-4">
                                    {[
                                        { label: '麒麟/统信 OS', icon: <Cpu className="w-4 h-4" /> },
                                        { label: '达梦/人大金仓', icon: <Database className="w-4 h-4" /> },
                                        { label: '鲲鹏/海光 CPU', icon: <Activity className="w-4 h-4" /> }
                                    ].map((item, i) => (
                                        <div key={i} className="bg-slate-800/50 border border-slate-700 rounded-lg py-3 flex flex-col items-center justify-center gap-2 text-sm text-slate-300 hover:bg-slate-800 transition-colors">
                                            {item.icon}
                                            {item.label}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section >

            {/* Xinchuang Ecosystem Wall */}
            < section className="py-16 border-t border-slate-800 bg-[#0B1120]" >
                <div className="max-w-7xl mx-auto px-6">
                    <p className="text-center text-slate-500 text-sm mb-8 font-mono tracking-widest uppercase">Trusted by Industry Leaders & Xinchuang Ecosystem</p>

                    {/* Infinite Scroll Logo Wall Simulation */}
                    <div className="flex flex-wrap justify-center gap-8 md:gap-16 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
                        {[
                            { name: "麒麟软件", en: "KylinSoft", icon: <Cpu className="w-6 h-6" /> },
                            { name: "统信软件", en: "UnionTech", icon: <Layers className="w-6 h-6" /> },
                            { name: "达梦数据库", en: "Dameng DB", icon: <Database className="w-6 h-6" /> },
                            { name: "人大金仓", en: "Kingbase", icon: <Database className="w-6 h-6" /> },
                            { name: "华为鲲鹏", en: "Kunpeng", icon: <Cpu className="w-6 h-6" /> },
                            { name: "中科海光", en: "Hygon", icon: <Cpu className="w-6 h-6" /> },
                            { name: "中国电子", en: "CEC", icon: <Shield className="w-6 h-6" /> }
                        ].map((partner, i) => (
                            <div key={i} className="flex items-center gap-2 group cursor-default">
                                <div className="p-2 bg-slate-800 rounded-lg group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors">
                                    {partner.icon}
                                </div>
                                <div>
                                    <div className="font-bold text-slate-300 group-hover:text-white transition-colors">{partner.name}</div>
                                    <div className="text-xs text-slate-600 group-hover:text-cyan-500/70 transition-colors">{partner.en}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </section >

            {/* Footer */}
            < footer className="bg-[#050911] border-t border-slate-800 py-12" >
                <div className="max-w-7xl mx-auto px-6">
                    <div className="flex flex-col md:flex-row justify-between items-center mb-8">
                        <div className="flex items-center gap-2 mb-4 md:mb-0">
                            <Shield className="w-6 h-6 text-slate-600" />
                            <span className="text-xl font-bold text-slate-500">DigiVoucher</span>
                        </div>
                        <div className="flex gap-8 text-sm text-slate-500">
                            <a href="#" className="hover:text-cyan-400 transition-colors">产品白皮书</a>
                            <a href="#" className="hover:text-cyan-400 transition-colors">技术文档</a>
                            <a href="#" className="hover:text-cyan-400 transition-colors">隐私政策</a>
                            <a href="#" className="hover:text-cyan-400 transition-colors">联系我们</a>
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
            </footer >
        </div >
    );
};

export default ProductWebsite;
