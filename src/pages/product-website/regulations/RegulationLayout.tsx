// Input: 子节点、元数据
// Output: 法规条文页面公共布局
// Pos: src/pages/product-website/regulations/RegulationLayout.tsx

import React from 'react';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { ChevronLeft, Scale } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SEO } from '../components/SEO';

interface RegulationLayoutProps {
    children?: React.ReactNode;
    title: string;
    description?: string;
    keywords?: string;
    source?: string;
    effectiveDate?: string;
    category?: string;
    pdfUrl?: string; // 新增 PDF 链接
}

export const RegulationLayout: React.FC<RegulationLayoutProps> = ({
    children, title, description, keywords, source, effectiveDate, category, pdfUrl
}) => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <SEO
                title={title}
                description={description || `DigiVoucher 专业解读：${title}。本规范详细规定了电子会计档案的管理要求，助力企业合规实现单套制归档。`}
                keywords={keywords}
                parentTitle="法律法规库"
            />
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            <main className="max-w-6xl mx-auto px-6 pt-32 pb-20">
                <div className="flex justify-between items-center mb-8">
                    <Link
                        to="/regulations"
                        className="inline-flex items-center text-amber-500 hover:text-amber-400 transition-colors group"
                    >
                        <ChevronLeft className="w-4 h-4 mr-1 transform group-hover:-translate-x-1 transition-transform" />
                        返回法律法规库
                    </Link>

                    {pdfUrl && (
                        <a
                            href={pdfUrl}
                            download
                            className="bg-slate-800 hover:bg-slate-700 text-white px-4 py-2 rounded-xl text-sm transition-all border border-slate-700 flex items-center gap-2"
                        >
                            <Scale className="w-4 h-4 text-amber-500" />
                            下载官方 PDF 原件
                        </a>
                    )}
                </div>

                <article className="bg-slate-900/30 rounded-3xl overflow-hidden border border-slate-800 shadow-2xl">
                    <header className="p-8 md:p-12 border-b border-slate-800 bg-slate-900/50">
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

                    {pdfUrl ? (
                        <div className="h-[900px] w-full bg-slate-800/20">
                            <iframe
                                src={`${pdfUrl}#toolbar=0`}
                                className="w-full h-full border-none"
                                title={title}
                            />
                        </div>
                    ) : (
                        <div className="p-8 md:p-12 prose prose-invert prose-amber max-w-none 
                            prose-h2:text-white prose-h2:border-l-4 prose-h2:border-amber-500 prose-h2:pl-4 prose-h2:mt-12
                            prose-strong:text-amber-400">
                            {children}
                        </div>
                    )}
                </article>
            </main>

            <Footer />
        </div>
    );
};
