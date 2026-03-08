import React from 'react';
import { BlogLayout } from './BlogLayout';

export const GBT39784Interpretation: React.FC = () => {
    return (
        <BlogLayout
            title="深入理解 GB/T 39784：电子档案管理系统的“合规金标准”"
            category="专家会诊"
            publishDate="2026-03-08"
        >
            <section className="space-y-8">
                <div className="p-8 bg-slate-900/80 border border-slate-800 rounded-3xl mb-12">
                    <h3 className="text-xl font-bold text-red-500 mb-6 flex items-center">
                        <span className="w-1.5 h-6 bg-red-500 mr-3 rounded-full"></span>
                        合规专家组安全通告
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="space-y-2">
                            <h4 className="font-bold text-white text-sm">🛡️ 安全专家：</h4>
                            <p className="text-slate-400 text-xs leading-relaxed">必须严格落地“三员管理”模型（系统员、安全员、审计员），任何超级管理员权限的滥用都将导致系统合规性失效。</p>
                        </div>
                        <div className="space-y-2">
                            <h4 className="font-bold text-white text-sm">⚖️ 审计专家：</h4>
                            <p className="text-slate-400 text-xs leading-relaxed">审计日志必须具备不可篡改性，应采用 HMAC 或区块链技术对日志链进行加固。</p>
                        </div>
                    </div>
                </div>

                <h2 className="text-3xl font-bold text-white border-b border-slate-800 pb-4">1. 三权分立：从理论到 DigiVoucher 的实操</h2>
                <p className="leading-relaxed text-slate-300">
                    GB/T 39784-2021 定义了合格系统的“职能底座”。在 DigiVoucher 中，我们不仅仅是创建了三个角色，而是通过<strong>内核级拦截</strong>实现了权限隔离：
                </p>
                <ul className="list-disc pl-6 space-y-4 text-slate-400">
                    <li><strong className="text-white">系统管理员：</strong> 仅负责配置服务器参数，无权查看任何业务档案内容。</li>
                    <li><strong className="text-white">审计员：</strong> 拥有“上帝视角”监控所有人的操作，但无法修改任何一条系统配置。</li>
                    <li><strong className="text-white">档案业务员：</strong> 仅能在被授权的范围内进行归档与利用。</li>
                </ul>

                <h2 className="text-3xl font-bold text-white border-b border-slate-800 pb-4">2. 审计日志的“法律防线”</h2>
                <p className="leading-relaxed text-slate-300">
                    当面临国家审计时，系统日志是唯一的判据。GB/T 39784 要求对敏感操作（导出、打印、解密）进行 100% 记录。
                    DigiVoucher 引入了<strong>审计链技术</strong>：每一条日志都包含上一条日志的唯一摘要值，形成有序链条，任何中间删除逻辑都会触发全局报警。
                </p>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 my-10">
                    <div className="p-6 bg-red-500/5 rounded-2xl border border-red-500/20">
                        <h4 className="text-white font-bold mb-2">等保 2.0</h4>
                        <p className="text-xs text-slate-400 text-center">深度适配三级要求</p>
                    </div>
                    <div className="p-6 bg-cyan-500/5 rounded-2xl border border-cyan-500/20">
                        <h4 className="text-white font-bold mb-2">日志加固</h4>
                        <p className="text-xs text-slate-400 text-center">HMAC-SHA256 算法</p>
                    </div>
                    <div className="p-6 bg-green-500/5 rounded-2xl border border-green-500/20">
                        <h4 className="text-white font-bold mb-2">多端审计</h4>
                        <p className="text-xs text-slate-400 text-center">含移动端操作轨迹</p>
                    </div>
                </div>

                <h2 className="text-3xl font-bold text-white border-b border-slate-800 pb-4">3. 总结</h2>
                <p className="leading-relaxed text-slate-300 italic">
                    如果您正面临系统等保验收或国家级档案局测评，GB/T 39784 的实操建议将是您最稳固的支撑。
                </p>

                <div className="flex flex-col md:flex-row items-center gap-6 mt-16 p-8 bg-gradient-to-r from-red-500/10 to-orange-500/10 border border-red-500/20 rounded-3xl">
                    <div className="flex-1">
                        <h3 className="text-xl font-bold text-white mb-2">担心系统无法通过合规测评？</h3>
                        <p className="text-slate-400 text-sm">下载我们的《GB/T 39784 合规技术对照表》，快速自检</p>
                    </div>
                    <a href="/regulations/system-functional-req" className="px-8 py-3 bg-slate-100 text-slate-900 font-bold rounded-xl hover:bg-white transition-colors text-center whitespace-nowrap">
                        立即获取自检表 →
                    </a>
                </div>
            </section>
        </BlogLayout>
    );
};

export default GBT39784Interpretation;
