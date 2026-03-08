// Input: 无
// Output: 法律法规库列表页 (Regulations Hub)
// Pos: src/pages/product-website/regulations/index.tsx

import React from 'react';
import { Gavel, Search, ExternalLink, ShieldCheck } from 'lucide-react';
import { SEO } from '../components/SEO';
import { Navigation } from '../components/Navigation';
import { Footer } from '../components/Footer';
import { Link } from 'react-router-dom';

const REG_LIST = [
    {
        slug: 'dat-94-spec',
        title: 'DA/T 94—2022《电子会计档案管理规范》',
        source: '国家档案局',
        category: '核心规范',
        desc: '电子会计档案管理的核心准则，定义了元数据、归档、整理及四性检测的强制性要求。'
    },
    {
        slug: 'erp-archive-spec',
        title: 'DA/T 104—2024《企业资源计划（ERP）系统电子文件归档和电子档案管理规范》',
        source: '国家档案局',
        category: '行业标准',
        desc: '2024年4月起实施，专门针对企业 ERP 系统（SAP/Oracle等）电子文件归档的最新权威指南。'
    },
    {
        slug: 'dat-95-spec',
        title: 'DA/T 95—2022《行政事业单位一般公共预算支出财务报销...管理技术规范》',
        source: '国家档案局',
        category: '技术规范',
        desc: '深度解析行政事业单位在电子发票采集、报销预览及归档全过程的技术要求。'
    },
    {
        slug: 'dat-92-spec',
        title: 'DA/T 92—2022《电子档案单套管理一般要求》',
        source: '国家档案局',
        category: '核心要求',
        desc: '规定了电子档案单套管理（无纸化归档）的组织管理、系统要求及流程标准。'
    },
    {
        slug: 'system-functional-req',
        title: 'GB/T 39784—2021《电子档案管理系统通用功能要求》',
        source: '国家标准',
        category: '通用标准',
        desc: '国家级推荐性标准，定义了合格的电子档案系统必须具备的 8 大功能域与 100+ 功能点。'
    },
    {
        slug: 'gbt-18894-spec',
        title: 'GB/T 18894—2016《电子文件归档与电子档案管理规范》',
        source: '国家标准',
        category: '国家标准',
        desc: '电子文件归档领域的奠基性标准，涵盖了从形成、积累到归档保存的通用生命周期管理。'
    }
];

export const RegulationsIndex: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans">
            <SEO
                title="电子会计档案法律法规库"
                description="权威收录国家档案局、财政部颁布的核心电子会计档案标准，包括 DA/T 94, DA/T 104, GB/T 39784 等。DigiVoucher 为您提供专业的合规解读与在线 PDF 预览。"
                keywords="电子会计档案法规, DA/T 94-2022, 电子凭证归档标准, 国家档案局规范, 数字化转型合规"
            />
            <Navigation scrolled={true} onNavigate={(path) => window.location.href = path} />

            <main className="max-w-7xl mx-auto px-6 pt-32 pb-20">
                <header className="mb-16">
                    <div className="flex items-center gap-4 mb-6">
                        <div className="p-3 bg-amber-500/20 rounded-2xl text-amber-500 shadow-[0_0_20px_rgba(245,158,11,0.2)]">
                            <Gavel className="w-8 h-8" />
                        </div>
                        <div>
                            <h1 className="text-4xl md:text-5xl font-bold text-white">
                                电子会计档案 <span className="text-amber-500">法律法规库</span>
                            </h1>
                            <p className="text-slate-400 mt-2">权威收录国家档案局、财政部颁布的核心标准与实施细则</p>
                        </div>
                    </div>

                    <div className="relative max-w-2xl mt-12">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 w-5 h-5" />
                        <input
                            type="text"
                            placeholder="搜索法规名称、文号或关键词 (如: DA/T 94)..."
                            className="w-full bg-slate-900 border border-slate-800 rounded-2xl py-4 pl-12 pr-6 outline-none focus:border-amber-500 transition-colors"
                        />
                    </div>
                </header>

                <section className="grid grid-cols-1 gap-6">
                    {REG_LIST.map((reg, i) => (
                        <Link
                            key={i}
                            to={`/regulations/${reg.slug}`}
                            className="group flex flex-col md:flex-row md:items-center justify-between p-8 bg-slate-900/50 border border-slate-800 rounded-3xl hover:border-amber-500/50 transition-all"
                        >
                            <div className="flex-1 pr-8">
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="px-3 py-1 bg-slate-800 rounded-full text-xs font-mono text-amber-500 border border-amber-500/20">
                                        {reg.category}
                                    </span>
                                    <span className="text-slate-500 text-xs">来源：{reg.source}</span>
                                </div>
                                <h2 className="text-xl md:text-2xl font-bold text-white mb-3 group-hover:text-amber-400 transition-colors">
                                    {reg.title}
                                </h2>
                                <p className="text-slate-400 leading-relaxed max-w-3xl">
                                    {reg.desc}
                                </p>
                            </div>
                            <div className="mt-6 md:mt-0 flex items-center text-amber-500 font-bold opacity-0 group-hover:opacity-100 transition-all translate-x-4 group-hover:translate-x-0">
                                查看全文 <ExternalLink className="w-5 h-5 ml-2" />
                            </div>
                        </Link>
                    ))}
                </section>

                <div className="mt-16 p-8 rounded-3xl bg-gradient-to-r from-slate-900/80 to-amber-900/20 border border-amber-500/10 flex flex-col md:flex-row items-center justify-between gap-8">
                    <div className="flex items-center gap-6">
                        <ShieldCheck className="w-12 h-12 text-amber-500 shrink-0" />
                        <div>
                            <h3 className="text-xl font-bold text-white mb-1">法规合规性声明</h3>
                            <p className="text-slate-400 text-sm leading-relaxed">
                                本库所收录法规均经过专家团队校验，DigiVoucher 系统功能严格遵循以上标准实现，确保您的电子档案具备 100% 法律效力。
                            </p>
                        </div>
                    </div>
                    <Link
                        to="/solutions/finance"
                        className="whitespace-nowrap px-8 py-3 bg-amber-500 text-slate-900 font-bold rounded-xl hover:bg-amber-400 transition-colors"
                    >
                        下载合规白皮书
                    </Link>
                </div>
            </main>

            <Footer />
        </div>
    );
};

export default RegulationsIndex;
