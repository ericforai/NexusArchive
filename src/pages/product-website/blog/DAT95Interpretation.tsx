import React from 'react';
import { BlogLayout } from './BlogLayout';

export const DAT95Interpretation: React.FC = () => {
    return (
        <BlogLayout
            title="解读 DA/T 95-2022：电子会计凭证报销、入账与归档的全流程规范"
            category="技术规范"
            publishDate="2026-03-08"
        >
            <section className="space-y-6">
                <p className="text-lg leading-relaxed mb-8">
                    <strong>DA/T 95-2022《电子会计凭证报销入账归档规范》</strong>是处理电子发票、电子行程单等新型凭证的“操作手册”。
                    它解决了企业在前端报销与后端归档之间的标准脱节问题。
                </p>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">1. 电子发票如何“合规”入账？</h2>
                <p className="leading-relaxed text-slate-400">
                    规范指出，企业必须建立电子会计凭证的<strong>唯一性校验</strong>机制，防止重复报销。
                    DigiVoucher 系统通过智能 OCR 与全国电子发票查验平台实时对接，实现自动化查验与去重。
                </p>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">2. 关于“原件”的保存要求</h2>
                <p className="leading-relaxed text-slate-400">
                    DA/T 95 强调，电子凭证及其元数据必须以原件形式保存，不能仅保存打印件的扫描件。
                    这意味着企业需要具备处理 XML (数电发票) 和 OFD 文件的能力。DigiVoucher 支持全格式解析与原生预览。
                </p>

                <div className="bg-purple-500/10 border-l-4 border-purple-500 p-6 my-10 rounded-r-xl">
                    <h4 className="font-bold text-white mb-2">💡 技术要点</h4>
                    <p className="text-slate-400 text-sm">
                        根据规范，系统必须支持归档包（AIP）的结构化存储。DigiVoucher 自动封装 Metadata.xml 与 Content 目录，符合国家馆藏标准。
                    </p>
                </div>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">结论</h2>
                <p className="leading-relaxed text-slate-400 italic">
                    通过贯彻 DA/T 95，企业能将财务共享中心的效率提升 40% 以上，并彻底规避虚假报销风险。
                </p>

                <div className="mt-12 p-8 border-2 border-slate-800 rounded-3xl text-center">
                    <h3 className="text-xl font-bold text-white mb-4">准备好体验智能报销归档了吗？</h3>
                    <a href="/regulations/dat-95-spec" className="text-cyan-500 font-bold hover:underline">
                        查看标准原文细则 →
                    </a>
                </div>
            </section>
        </BlogLayout>
    );
};

export default DAT95Interpretation;
