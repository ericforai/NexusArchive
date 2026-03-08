import React from 'react';
import { BlogLayout } from './BlogLayout';

export const ERP104Interpretation: React.FC = () => {
    return (
        <BlogLayout
            title="深度解析 DA/T 104-2024：ERP 系统归档接口的“技术红线”"
            category="专家会诊"
            publishDate="2026-03-08"
        >
            <section className="space-y-8">
                <div className="p-8 bg-slate-900/80 border border-slate-800 rounded-3xl mb-12">
                    <h3 className="text-xl font-bold text-amber-500 mb-6 flex items-center">
                        <span className="w-1.5 h-6 bg-amber-500 mr-3 rounded-full"></span>
                        技术专家组技术简讯
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="space-y-2">
                            <h4 className="font-bold text-white text-sm">🏗️ 架构专家：</h4>
                            <p className="text-slate-400 text-xs leading-relaxed">2024 新标的核心在于 SIP（提交信息包）的标准化，禁止私有协议，要求必须包含完整元数据链路。</p>
                        </div>
                        <div className="space-y-2">
                            <h4 className="font-bold text-white text-sm">🛡️ 安全专家：</h4>
                            <p className="text-slate-400 text-xs leading-relaxed">源系统（ERP）到档案系统的接口必须进行端到端加密，建议采用国密 SM4 算法确保链路安全。</p>
                        </div>
                    </div>
                </div>

                <h2 className="text-3xl font-bold text-white border-b border-slate-800 pb-4">1. 为什么传统的 API 对接已不再合规？</h2>
                <p className="leading-relaxed text-slate-300">
                    在 DA/T 104-2024 发布前，多数企业采用临时的“丢文件”模式。新标明确规定：归档必须是<strong>原子化操作</strong>。
                    这意味着如果文件上传成功但元数据写入失败，整个归档过程必须回滚。DigiVoucher 系统通过分布式事务确保了归档操作的强一致性。
                </p>

                <h2 className="text-3xl font-bold text-white border-b border-slate-800 pb-4">2. SIP 提交包的深度拆解</h2>
                <div className="bg-slate-800/50 p-6 rounded-2xl border border-slate-700 font-mono text-xs text-slate-400 mb-6">
                    <p className="text-cyan-400 mb-2">// 满足 DA/T 104 的标准 SIP 包结构示例</p>
                    <p>AIP_CONTENT</p>
                    <p>├── METADATA.xml (24项核心审计字段)</p>
                    <p>├── ATTACHMENTS (原始凭证 PDF/OFD)</p>
                    <p>└── DIGEST (SM3 哈希清单)</p>
                </div>
                <p className="leading-relaxed text-slate-300">
                    不仅仅是把文件传过来，归档接口必须能够“反向追随”到业务源。DigiVoucher 适配器能够自动抓取 ERP 中的审批全链条信息，将其封装在元数据中。
                </p>

                <div className="flex flex-col md:flex-row items-center gap-6 mt-16 p-8 bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-3xl">
                    <div className="flex-1">
                        <h3 className="text-xl font-bold text-white mb-2">正在进行 SAP 或 Oracle 的归档集成？</h3>
                        <p className="text-slate-400 text-sm">获取 DigiVoucher 针对大型 ERP 的标准接口适配文档</p>
                    </div>
                    <a href="/system/settings/erp-ai/preview" className="px-8 py-3 bg-amber-500 text-slate-900 font-bold rounded-xl hover:bg-amber-400 transition-colors text-center">
                        获取适配器文档 →
                    </a>
                </div>
            </section>
        </BlogLayout>
    );
};

export default ERP104Interpretation;
