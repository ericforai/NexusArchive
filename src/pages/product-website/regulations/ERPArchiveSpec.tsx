// Input: 无
// Output: ERP 系统电子文件归档规范 (DA/T 104-2024) 详情页
// Pos: src/pages/product-website/regulations/ERPArchiveSpec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const ERPArchiveSpec: React.FC = () => {
    return (
        <RegulationLayout
            title="DA/T 104—2024《企业资源计划（ERP）系统电子文件归档和电子档案管理规范》"
            category="行业标准"
            source="国家档案局"
            effectiveDate="2024-04-01"
        >
            <section className="space-y-6">
                <h2>1 范围</h2>
                <p>本文件规定了企业资源计划 (ERP) 系统电子文件的归档要求、电子档案的管理以及系统建设要求。适用于企业 ERP 系统中产生的电子文件归档及其形成的电子档案的管理。</p>

                <h2>2 规范性引用文件</h2>
                <p>下列文件中的条款通过本标准的引用而成为本标准的条款：</p>
                <ul>
                    <li>GB/T 18894 电子文件归档与电子档案管理规范</li>
                    <li>DA/T 94 会计档案管理规范</li>
                    <li>DA/T 46 企业档案工作规范</li>
                </ul>

                <h2>10 检索利用</h2>
                <p><strong>10.2.1</strong> ERP 系统应具备基本的检索利用功能，支持跨业务领域的关联查询。</p>
                <p><strong>10.2.4</strong> 应支持对采购系统报表、物料系统报表、记账凭证、生产系统报表等分别查询检索。</p>

                <h2>11 鉴定与处置</h2>
                <p><strong>11.2</strong> 对经鉴定后确无保存价值的电子档案进行销毁。物理删除时应由业务部门、档案部门共同监督，销毁清单及记录需打印存档。</p>

                <h2>附录 A：核心归档范围与保管期限 (重点)</h2>
                <div className="overflow-x-auto">
                    <table className="min-w-full border-collapse border border-slate-700 mt-6">
                        <thead className="bg-slate-800">
                            <tr>
                                <th className="border border-slate-700 p-4 text-left">序号</th>
                                <th className="border border-slate-700 p-4 text-left">业务子系统</th>
                                <th className="border border-slate-700 p-4 text-left">归档名称</th>
                                <th className="border border-slate-700 p-4 text-left">保管期限</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td className="border border-slate-700 p-4">1</td>
                                <td className="border border-slate-700 p-4">采购管理</td>
                                <td className="border border-slate-700 p-4">采购合同及变更文件</td>
                                <td className="border border-slate-700 p-4">30年</td>
                            </tr>
                            <tr>
                                <td className="border border-slate-700 p-4">2</td>
                                <td className="border border-slate-700 p-4">财务核算</td>
                                <td className="border border-slate-700 p-4">记账凭证 (Voucher)</td>
                                <td className="border border-slate-700 p-4 text-amber-500 font-bold">30年</td>
                            </tr>
                            <tr>
                                <td className="border border-slate-700 p-4">3</td>
                                <td className="border border-slate-700 p-4">物料管理</td>
                                <td className="border border-slate-700 p-4">主要产品销售、库存清单</td>
                                <td className="border border-slate-700 p-4 text-red-500 font-bold">永久</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div className="bg-amber-500/10 border p-8 rounded-3xl border-amber-500/20 mt-16">
                    <h3 className="text-xl font-bold text-white mb-4">DigiVoucher 是如何执行此标准的？</h3>
                    <p className="text-slate-400 mb-6 leading-relaxed">
                        我们的系统预设了符合 DA/T 104 规范的元数据模板，能够自动从 SAP/Oracle/U8 等 ERP 系统中捕获归档范围内的电子文件，并依据附件 A 自动设定保管期限提醒。
                    </p>
                    <a href="/solutions/finance" className="text-amber-500 font-bold underline">查看 ERP 系统对接建议书</a>
                </div>
            </section>
        </RegulationLayout>
    );
};

export default ERPArchiveSpec;
