// Input: 子节点
// Output: 博客页面公共布局 (含导航/SEO 容器)
// Pos: src/pages/product-website/blog/BlogLayout.tsx

import React from 'react';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { ChevronLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

interface BlogLayoutProps {
    children: React.ReactNode;
    title: string;
    category?: string;
    publishDate?: string;
}

export const BlogLayout: React.FC<BlogLayoutProps> = ({ children, title, category, publishDate }) => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            <main className="max-w-4xl mx-auto px-6 pt-32 pb-20">
                <Link
                    to="/blog"
                    className="inline-flex items-center text-cyan-500 hover:text-cyan-400 mb-8 transition-colors group"
                >
                    <ChevronLeft className="w-4 h-4 mr-1 transform group-hover:-translate-x-1 transition-transform" />
                    返回知识库中心
                </Link>

                <article>
                    <header className="mb-12">
                        {category && (
                            <span className="text-cyan-500 text-sm font-mono uppercase tracking-widest mb-4 block">
                                {category}
                            </span>
                        )}
                        <h1 className="text-4xl md:text-5xl font-bold text-white mb-6 leading-tight">
                            {title}
                        </h1>
                        {publishDate && (
                            <time className="text-slate-500 text-sm">
                                发布于 {publishDate}
                            </time>
                        )}
                    </header>

                    <div className="prose prose-invert prose-cyan max-w-none">
                        {children}
                    </div>

                    <footer className="mt-16 pt-8 border-t border-slate-800">
                        <div className="bg-slate-900/50 rounded-2xl p-8 border border-slate-800/50">
                            <h3 className="text-xl font-bold text-white mb-4">想要了解 DigiVoucher 如何助您实现合规归档？</h3>
                            <p className="text-slate-400 mb-6">
                                我们的专家团队可为您提供定制化的电子会计档案单套制实施建议。
                            </p>
                            <Link
                                to="/system/login"
                                className="inline-flex items-center px-6 py-3 bg-cyan-500 text-slate-900 font-bold rounded-xl hover:bg-cyan-400 transition-all shadow-[0_0_15px_rgba(6,182,212,0.3)]"
                            >
                                立即免费试用
                            </Link>
                        </div>
                    </footer>
                </article>
            </main>

            <Footer />
        </div>
    );
};
