// Input: 无
// Output: 行政事业单位财务报销技术规范 (DA/T 95-2022) 详情页
// Pos: src/pages/product-website/regulations/DAT95Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT95Spec: React.FC = () => {
    return (
        <RegulationLayout
            title="DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》"
            category="技术规范"
            source="国家档案局"
            effectiveDate="2022-07-01"
        >
            <section className="space-y-6">
                <h2>1 范围</h2>
                <p>本文件规定了行政事业单位一般公共预算支出财务报销电子会计凭证的采集、组件、归档、存储、统计、利用以及相关系统衔接的要求。</p>

                <h2>5 采集</h2>
                <p><strong>5.1 获取方式：</strong> 单位应通过财政、税务等相关部门提供的正式接口或渠道获取电子会计凭证及其相关数据。</p>
                <p><strong>5.2 查验：</strong> 应利用国家确定的第三方服务渠道对电子会计凭证的签名、有效性进行查验。</p>

                <h2>6 组件 (AIP/SIP)</h2>
                <p>电子会计凭证应与相关元数据封装成归档信息包（AIP）。</p>
                <p><strong>6.1 物理命名：</strong> 物理文件名应与会计核算功能中记账数据的关联关系保持一致。</p>

                <h2>7 归档与存储</h2>
                <p>行政事业单位应确保电子会计凭证在归档过程中的固化，防止重复入账。系统应自动判定凭证是否已存在于档案库中。</p>

                <div className="bg-amber-500/10 border p-8 rounded-3xl border-amber-500/20 mt-16 text-center">
                    <h3 className="text-xl font-bold text-white mb-4">行政事业单位“电子报销”最佳实践</h3>
                    <p className="text-slate-400 mb-6 leading-relaxed">
                        DigiVoucher 支持与财政预算系统深度对接，实现从“报销采集”到“电子归档”的全流程自动化，完美适配 DA/T 95 技术标准。
                    </p>
                    <a href="/solutions/finance" className="text-amber-500 font-bold underline">查看政务版实施方案</a>
                </div>
            </section>
        </RegulationLayout>
    );
};

export default DAT95Spec;
