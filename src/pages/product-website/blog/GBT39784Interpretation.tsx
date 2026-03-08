import React from 'react';
import { BlogLayout } from './BlogLayout';

export const GBT39784Interpretation: React.FC = () => {
    return (
        <BlogLayout
            title="深入理解 GB/T 39784：电子档案管理系统的功能架构与安全基准"
            category="国标解读"
            publishDate="2026-03-08"
        >
            <section className="space-y-6">
                <p className="text-lg leading-relaxed mb-8">
                    <strong>GB/T 39784-2021《电子档案管理系统通用功能要求》</strong>是衡量一个档案软件“好不好用”的核心量目，
                    更是系统通过合规性审计的最终考卷。
                </p>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">1. 核心功能：三员管理与权限隔离</h2>
                <p className="leading-relaxed text-slate-400">
                    规范明确了系统必须具备系统管理员、审计员和安全管理员的“三权分立”。
                    DigiVoucher 严格遵循此原则，确保最高权限者也无法在无审计的情况下删除档案数据。
                </p>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">2. 全覆盖的审计日志逻辑</h2>
                <p className="leading-relaxed text-slate-400">
                    任何对此份标准有深入研究的人都会发现，国标对审计粒度要求极高。
                    DigiVoucher 记录包括查看、打印、下载在内的 100% 用户操作日志，并采用 HMAC 算法对日志进行防篡改处理。
                </p>

                <div className="bg-red-500/10 border-l-4 border-red-500 p-6 my-10 rounded-r-xl">
                    <h4 className="font-bold text-white mb-2">⚠️ 安全警告</h4>
                    <p className="text-slate-400 text-sm">
                        未适配 GB/T 39784 的档案系统在面对国资委、审计署的数字化审计时，将面临极高的合规风险。
                    </p>
                </div>

                <h2 className="text-2xl font-bold text-white mt-12 mb-6">结论</h2>
                <p className="leading-relaxed text-slate-400 italic">
                    DigiVoucher 基于 GB/T 39784 框架构建，不仅是工具，更是您的合规保障屏障。
                </p>

                <div className="mt-12 p-8 border-2 border-slate-800 rounded-3xl text-center">
                    <h3 className="text-xl font-bold text-white mb-4">查看系统的安全审核机制</h3>
                    <a href="/regulations/system-functional-req" className="text-cyan-500 font-bold hover:underline">
                        功能要求原文对比 →
                    </a>
                </div>
            </section>
        </BlogLayout>
    );
};

export default GBT39784Interpretation;
