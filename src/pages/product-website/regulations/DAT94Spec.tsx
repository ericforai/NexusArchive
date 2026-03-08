// Input: 无
// Output: 电子会计档案管理规范 (DA/T 94-2022) 详情页
// Pos: src/pages/product-website/regulations/DAT94Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT94Spec: React.FC = () => {
    return (
        <RegulationLayout
            title="DA/T 94—2022《电子会计档案管理规范》"
            category="核心规范"
            source="国家档案局"
            effectiveDate="2022-01-01"
        >
            <section className="space-y-6">
                <h2>1 范围</h2>
                <p>本文件规定了电子会计档案的元数据要求、归档、整理、保管、鉴定、销毁、排移和利用要求。适用于各单位电子会计档案的管理。</p>

                <h2>4 一般要求</h2>
                <p><strong>4.1 法律效力：</strong> 满足真实性、完整性、可用性、安全性要求的电子会计档案，与纸质会计档案具有同等法律效力。</p>
                <p><strong>4.2 单套制管理：</strong> 具备条件的单位，可以仅以电子形式保存会计档案。</p>

                <h2>6 “四性”检测要求</h2>
                <p>单位应对电子会计档案进行以下四个层面的检测：</p>
                <ul>
                    <li><strong>真实性 (Authenticity)：</strong> 来源真实，内容未被非法篡改。</li>
                    <li><strong>完整性 (Integrity)：</strong> 电子会计文件及其相关元数据、电子签名齐全。</li>
                    <li><strong>可用性 (Usability)：</strong> 能够被读取、理解和利用。</li>
                    <li><strong>安全性 (Safety)：</strong> 存储环境安全，管理过程可追溯。</li>
                </ul>

                <h2>附录 B：元数据方案</h2>
                <p>规范明确要求元数据应包括：业务标识符、文件名称、形成者、形成日期、保管期限、审计日志等 38 项核心元数据。</p>

                <div className="bg-amber-500/10 border p-8 rounded-3xl border-amber-500/20 mt-16">
                    <h3 className="text-xl font-bold text-white mb-4">DigiVoucher 合规性优势</h3>
                    <p className="text-slate-400 mb-6 leading-relaxed">
                        DigiVoucher 内置了 DA/T 94 要求的“四性检测”自动化引擎。每一个凭证入库时，系统都会自动计算国密哈希（SM3）并校验元数据完整性，确保审计无忧。
                    </p>
                </div>
            </section>
        </RegulationLayout>
    );
};

export default DAT94Spec;
