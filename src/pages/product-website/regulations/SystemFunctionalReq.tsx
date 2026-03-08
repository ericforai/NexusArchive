// Input: 无
// Output: 电子档案管理系统通用功能要求 (GB/T 39784-2021) 详情页
// Pos: src/pages/product-website/regulations/SystemFunctionalReq.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const SystemFunctionalReq: React.FC = () => {
    return (
        <RegulationLayout
            title="GB/T 39784—2021《电子档案管理系统通用功能要求》"
            category="通用标准"
            source="国家标准局"
            effectiveDate="2021-10-01"
        >
            <section className="space-y-6">
                <h2>1 范围</h2>
                <p>本文件规定了电子档案管理系统的通用功能要求，包括收集、整理、保存、利用、鉴定、销毁、移交、系统管理等。</p>

                <h2>5 核心功能模块</h2>

                <h3>5.1 收集 (Capture)</h3>
                <p>系统应支持接收符合规范的电子文件及其元数据，并进行完整性、准确性校验。</p>

                <h3>5.2 保存 (Preservation)</h3>
                <p>应支持对电子档案进行长久保存，包括格式转换、安全性检查、存储介质管理等。</p>

                <h2>6 功能要求清单</h2>
                <ul className="list-disc pl-6 space-y-2">
                    <li><strong>安全性：</strong> 支持身份认证、权限控制、审计追踪。</li>
                    <li><strong>可扩展性：</strong> 能够适应业务量增长和技术环境变化。</li>
                    <li><strong>集成性：</strong> 能与业务系统、数字化协同办公系统无缝衔接。</li>
                </ul>

                <div className="bg-amber-500/10 border p-8 rounded-3xl border-amber-500/20 mt-16">
                    <h3 className="text-xl font-bold text-white mb-4">DigiVoucher 的功能对标</h3>
                    <p className="text-slate-400 mb-6 leading-relaxed">
                        作为全行业领先的系统，DigiVoucher 不仅满足 GB/T 39784 的 100% 核心功能要求，还额外增强了“AI 自动分类”与“低代码流程引擎”，实现超越国标的智能管理。
                    </p>
                </div>
            </section>
        </RegulationLayout>
    );
};

export default SystemFunctionalReq;
