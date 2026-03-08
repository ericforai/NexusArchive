// Input: 无
// Output: 知识库中心列表页
// Pos: src/pages/product-website/blog/index.tsx

import React from 'react';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { Link } from 'react-router-dom';
import { ArrowRight, BookOpen, ShieldCheck, Zap } from 'lucide-react';

const FEATURED_POSTS = [
    {
        slug: 'dat-94-2022-interpretation',
        title: '深度解读 DA/T 94-2022：会计档案电子化归档的新标准',
        excerpt: '国家标准 DA/T 94-2022 的发布标志着会计档案管理进入了全数字化时代。本文将为您拆解核心合规要点。',
        category: '行业规范',
        date: '2026-03-05',
        icon: ShieldCheck
    },
    {
        slug: 'dat-92-interpretation',
        title: 'DA/T 92-2022 解析：如何实现电子档案的合规“单套制”',
        excerpt: '单套制管理不仅是技术升级，更是法律效力的保障。了解单套制落地的“四性”要求与实施路径。',
        category: '行业规范',
        date: '2026-03-08',
        icon: ShieldCheck
    },
    {
        slug: 'dat-95-interpretation',
        title: 'DA/T 95-2022 解读：电子会计凭证从报销到归档的全流程',
        excerpt: '数电发票时代，如何处理 XML 原件？深度解析电子会计凭证的唯一性校验与长期保存。',
        category: '技术规范',
        date: '2026-03-08',
        icon: Zap
    },
    {
        slug: 'dat-104-interpretation',
        title: 'DA/T 104-2024 新标：ERP 系统与档案系统的接口红线',
        excerpt: '2024年最新接口标准发布！解析 SIP 提交包结构以及 ERP 元数据自动捕获的关键技术。',
        category: '行业趋势',
        date: '2026-03-08',
        icon: Zap
    },
    {
        slug: 'gbt-18894-interpretation',
        title: 'GB/T 18894 深度解码：企业数字化档案管理的底层逻辑',
        excerpt: '作为电子档案管理的“母法”，GB/T 18894 规定了全生命周期受控的核心原则。',
        category: '国家标准',
        date: '2026-03-08',
        icon: BookOpen
    },
    {
        slug: 'gbt-39784-interpretation',
        title: 'GB/T 39784 功能要求：衡量合规档案系统的“黄金标尺”',
        excerpt: '深入理解三权分立与高强度审计日志。如何构建一个经得起国家审计的电子档案系统？',
        category: '国标解读',
        date: '2026-03-08',
        icon: ShieldCheck
    },
    {
        slug: 'single-set-system-implementation',
        title: '企业单套制归档实施指南：从纸质到数字的平滑路径',
        excerpt: '如何确保电子凭证与原始单据的一一对应？单套制实施中常见的坑有哪些？点击查看完整指南。',
        category: '技术方案',
        date: '2026-02-28',
        icon: Zap
    }
];

export const BlogIndex: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            <main className="max-w-7xl mx-auto px-6 pt-32 pb-20">
                <header className="mb-16 text-center">
                    <div className="inline-flex items-center px-4 py-1.5 rounded-full border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 text-sm font-medium mb-6">
                        <BookOpen className="w-4 h-4 mr-2" />
                        DigiVoucher 知识库
                    </div>
                    <h1 className="text-4xl md:text-6xl font-bold text-white mb-6">
                        深入了解电子会计档案 <br />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">数字化转型</span>
                    </h1>
                    <p className="text-xl text-slate-400 max-w-3xl mx-auto">
                        获取最新的行业标准解读、合规指南以及大型企业数字化转型案例。
                    </p>
                </header>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-16">
                    {FEATURED_POSTS.map((post, i) => {
                        const Icon = post.icon;
                        return (
                            <Link
                                key={i}
                                to={`/blog/${post.slug}`}
                                className="group p-8 rounded-3xl bg-slate-900/50 border border-slate-800 hover:border-cyan-500/50 transition-all duration-500"
                            >
                                <div className="flex items-start justify-between mb-6">
                                    <div className="p-3 bg-slate-800 rounded-xl group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors">
                                        <Icon className="w-6 h-6" />
                                    </div>
                                    <span className="text-slate-500 text-sm font-mono">{post.category}</span>
                                </div>
                                <h2 className="text-2xl font-bold text-white mb-4 group-hover:text-cyan-400 transition-colors">
                                    {post.title}
                                </h2>
                                <p className="text-slate-400 mb-8 leading-relaxed">
                                    {post.excerpt}
                                </p>
                                <div className="flex items-center text-cyan-500 font-bold group-hover:gap-2 transition-all">
                                    阅读全文 <ArrowRight className="w-5 h-5 ml-1" />
                                </div>
                            </Link>
                        );
                    })}
                </div>
            </main>

            <Footer />
        </div>
    );
};

export default BlogIndex;
