// Input: 子节点、元数据
// Output: 法规条文页面公共布局
// Pos: src/pages/product-website/regulations/RegulationLayout.tsx

import React from 'react';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { ChevronLeft, Scale } from 'lucide-react';
import { Link } from 'react-router-dom';

interface RegulationLayoutProps {
    children: React.ReactNode;
    title: string;
    source?: string;
    effectiveDate?: string;
    category?: string;
}

export const RegulationLayout: React.FC<RegulationLayoutProps> = ({
    children, title, source, effectiveDate, category
}) => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            <main className="max-w-5xl mx-auto px-6 pt-32 pb-20">
                <Link
                    to="/regulations"
                    className="inline-flex items-center text-amber-500 hover:text-amber-400 mb-8 transition-colors group"
                >
                    <ChevronLeft className="w-4 h-4 mr-1 transform group-hover:-translate-x-1 transition-transform" />
                    返回法律法规库
                </Link>

                <article className="bg-slate-900/30 rounded-3xl p-8 md:p-12 border border-slate-800 shadow-2xl">
                    <header className="mb-12 border-b border-slate-800 pb-10">
                        <div className="flex items-center gap-3 mb-6">
                            <div className="p-2 bg-amber-500/20 rounded-lg text-amber-500">
                                <Scale className="w-6 h-6" />
                            </div>
                            <span className="text-amber-500 text-sm font-mono uppercase tracking-widest">
                                {category || '法律法规'}
                            </span>
                        </div>

                        <h1 className="text-3xl md:text-5xl font-bold text-white mb-6 leading-tight">
                            {title}
                        </h1>

                        <div className="flex flex-wrap gap-6 text-slate-500 text-sm">
                            <p>发布单位：{source || '国家档案局/财政部'}</p>
                            {effectiveDate && <p>实施日期：{effectiveDate}</p>}
                        </div>
                    </header>

                    <div className="prose prose-invert prose-amber max-w-none 
            prose-h2:text-white prose-h2:border-l-4 prose-h2:border-amber-500 prose-h2:pl-4 prose-h2:mt-12
            prose-strong:text-amber-400">
                        {children}
                    </div>
                </article>
            </main>

            <Footer />
        </div>
    );
};
