// Input: React、liteofd、调试 API、真实 OFD 样例
// Output: OFD 技术路线 spike 页面（前端 liteofd + 后端 ofdrw）
// Pos: src/pages/demo/OfdSpikePage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useRef, useState } from 'react';
import LiteOfdDefault, { LiteOfd as LiteOfdNamed } from 'liteofd';
import 'liteofd/main.css';

// 处理 ESM/CJS 的默认导出问题
const LiteOfd = (typeof LiteOfdDefault === 'function' ? LiteOfdDefault : (typeof LiteOfdNamed === 'function' ? LiteOfdNamed : (LiteOfdDefault as any)?.default)) as any;

import { AlertCircle, CheckCircle2, FileText, Loader2, RefreshCw, Upload } from 'lucide-react';
import { DEFAULT_OFD_SPIKE_SAMPLE, ofdSpikeApi } from '../../api/ofdSpike';

type RenderMode = 'sample' | 'upload' | 'backend-convert';

interface RenderSummary {
    mode: RenderMode;
    fileName: string;
    pageCount: number | null;
}

function isPdfFile(file: File): boolean {
    return file.name.toLowerCase().endsWith('.pdf') || file.type === 'application/pdf';
}

export const OfdSpikePage: React.FC = () => {
    const renderContainerRef = useRef<HTMLDivElement | null>(null);
    const liteOfdRef = useRef<any | null>(null);
    const objectUrlRef = useRef<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [summary, setSummary] = useState<RenderSummary | null>(null);

    const cleanupRenderArtifacts = () => {
        if (renderContainerRef.current) {
            renderContainerRef.current.innerHTML = '';
        }
        if (objectUrlRef.current) {
            URL.revokeObjectURL(objectUrlRef.current);
            objectUrlRef.current = null;
        }
        liteOfdRef.current = null;
    };

    useEffect(() => cleanupRenderArtifacts, []);

    const renderOfd = async (buffer: ArrayBuffer, fileName: string, mode: RenderMode) => {
        if (!renderContainerRef.current) {
            return;
        }

        setLoading(true);
        setError(null);
        cleanupRenderArtifacts();

        try {
            const liteOfd = new LiteOfd();
            liteOfdRef.current = liteOfd;
            
            // 为解析过程添加 10 秒超时，防止库内部挂起导致 UI 卡死
            const parsed = await Promise.race([
                liteOfd.parse(buffer),
                new Promise((_, reject) => 
                    setTimeout(() => reject(new Error('LiteOFD 解析超时 (10s)，可能是字体资源加载失败')), 10000)
                )
            ]);
            
            const rendered = liteOfd.render(
                undefined,
                'background-color:#f8fafc; padding: 16px; border-radius: 16px; margin: 0 auto;',
            );
            renderContainerRef.current.appendChild(rendered);
            const pageCount = Array.isArray((parsed as any)?.pages)
                ? (parsed as any).pages.length
                : Array.isArray((parsed as any)?.docBody?.pages)
                    ? (parsed as any).docBody.pages.length
                    : null;
            setSummary({ mode, fileName, pageCount });
        } catch (renderError) {
            const message = renderError instanceof Error ? renderError.message : 'LiteOFD 渲染失败';
            setError(message);
            setSummary(null);
        } finally {
            setLoading(false);
        }
    };

    const handleLoadSample = async () => {
        const buffer = await ofdSpikeApi.fetchOriginalVoucherFile(DEFAULT_OFD_SPIKE_SAMPLE.fileId);
        await renderOfd(buffer, DEFAULT_OFD_SPIKE_SAMPLE.fileName, 'sample');
    };

    const handleOfdUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file) {
            return;
        }
        const buffer = await file.arrayBuffer();
        await renderOfd(buffer, file.name, 'upload');
    };

    const handlePdfConvert = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file) {
            return;
        }
        if (!isPdfFile(file)) {
            setError('后端 ofdrw spike 仅接受 PDF 文件');
            return;
        }

        setLoading(true);
        setError(null);
        cleanupRenderArtifacts();

        try {
            const blob = await ofdSpikeApi.convertPdfToOfd(file);
            objectUrlRef.current = URL.createObjectURL(blob);
            const buffer = await blob.arrayBuffer();
            await renderOfd(buffer, file.name.replace(/\.pdf$/i, '.ofd'), 'backend-convert');
        } catch (convertError) {
            const message = convertError instanceof Error ? convertError.message : '后端 ofdrw 转换失败';
            setError(message);
            setSummary(null);
            setLoading(false);
        }
    };

    return (
        <div className="min-h-full bg-slate-100">
            <div className="mx-auto max-w-7xl px-6 py-8">
                <div className="mb-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div>
                            <p className="text-sm font-medium uppercase tracking-[0.2em] text-slate-500">OFD Spike Lab</p>
                            <h1 className="mt-2 text-3xl font-semibold text-slate-900">liteofd / ofdrw 路线验证</h1>
                            <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
                                这个页面不再走仓库里的自研解析器，只做两条实验链路：
                                前端用 <code>liteofd</code> 直接渲染 OFD，后端用 <code>ofdrw</code> 将 PDF 转 OFD 后回传。
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={handleLoadSample}
                            disabled={loading}
                            className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
                        >
                            {loading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
                            加载真实发票样例
                        </button>
                    </div>
                    <div className="mt-4 grid gap-3 text-xs text-slate-500 md:grid-cols-3">
                        <div className="rounded-2xl bg-slate-50 px-4 py-3">
                            <div className="font-medium text-slate-700">样例凭证</div>
                            <div className="mt-1 break-all">{DEFAULT_OFD_SPIKE_SAMPLE.voucherId}</div>
                        </div>
                        <div className="rounded-2xl bg-slate-50 px-4 py-3">
                            <div className="font-medium text-slate-700">样例文件</div>
                            <div className="mt-1 break-all">{DEFAULT_OFD_SPIKE_SAMPLE.fileId}</div>
                        </div>
                        <div className="rounded-2xl bg-slate-50 px-4 py-3">
                            <div className="font-medium text-slate-700">验证目标</div>
                            <div className="mt-1">字体、路径、模板页、后端转换可行性</div>
                        </div>
                    </div>
                </div>

                <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
                    <div className="space-y-6">
                        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                            <div className="flex items-center gap-2 text-slate-900">
                                <CheckCircle2 size={18} className="text-emerald-600" />
                                <h2 className="text-lg font-semibold">前端 liteofd spike</h2>
                            </div>
                            <p className="mt-3 text-sm leading-6 text-slate-600">
                                直接用 <code>liteofd</code> 渲染 OFD，不经过当前仓库自研解析器。
                            </p>
                            <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100">
                                <Upload size={16} />
                                上传本地 OFD
                                <input type="file" accept=".ofd" className="hidden" onChange={handleOfdUpload} />
                            </label>
                        </section>

                        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                            <div className="flex items-center gap-2 text-slate-900">
                                <FileText size={18} className="text-blue-600" />
                                <h2 className="text-lg font-semibold">后端 ofdrw spike</h2>
                            </div>
                            <p className="mt-3 text-sm leading-6 text-slate-600">
                                上传 PDF 给后端 debug 接口，使用仓库内已存在的 <code>ofdrw</code> 依赖与辅助转换器生成 OFD，
                                再回传给本页用 <code>liteofd</code> 验证能否展示。
                            </p>
                            <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-100">
                                <Upload size={16} />
                                上传 PDF 并调用 ofdrw
                                <input type="file" accept=".pdf,application/pdf" className="hidden" onChange={handlePdfConvert} />
                            </label>
                            {objectUrlRef.current && (
                                <a
                                    href={objectUrlRef.current}
                                    download={(summary?.fileName || 'converted').replace(/\.pdf$/i, '.ofd')}
                                    className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-blue-700 hover:text-blue-900"
                                >
                                    下载后端转换产物
                                </a>
                            )}
                        </section>

                        <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                            <h2 className="text-lg font-semibold text-slate-900">状态</h2>
                            {loading && (
                                <div className="mt-3 flex items-center gap-2 text-sm text-slate-600">
                                    <Loader2 size={16} className="animate-spin" />
                                    正在执行 spike...
                                </div>
                            )}
                            {error && (
                                <div className="mt-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                                    <div className="flex items-start gap-2">
                                        <AlertCircle size={16} className="mt-0.5 shrink-0" />
                                        <span>{error}</span>
                                    </div>
                                </div>
                            )}
                            {summary && !loading && !error && (
                                <div className="mt-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                                    <div>来源：{summary.mode}</div>
                                    <div className="mt-1 break-all">文件：{summary.fileName}</div>
                                    <div className="mt-1">页数：{summary.pageCount ?? 'liteofd 未返回显式页数'}</div>
                                </div>
                            )}
                        </section>
                    </div>

                    <section className="rounded-[28px] border border-slate-200 bg-white p-4 shadow-sm">
                        <div className="mb-4 flex items-center justify-between border-b border-slate-100 px-2 pb-3">
                            <div>
                                <h2 className="text-lg font-semibold text-slate-900">渲染结果</h2>
                                <p className="text-sm text-slate-500">这里展示的是 `liteofd` 真实渲染结果，不再经过仓库自研 OFD 解析器。</p>
                            </div>
                        </div>
                        <div className="min-h-[70vh] overflow-auto rounded-2xl bg-slate-50 p-3">
                            <div ref={renderContainerRef} />
                            {!loading && !summary && !error && (
                                <div className="flex min-h-[60vh] items-center justify-center text-center text-slate-400">
                                    <div>
                                        <FileText size={44} className="mx-auto mb-4 opacity-30" />
                                        <p>选择上面的任一 spike 入口后，这里会直接展示渲染结果。</p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </section>
                </div>
            </div>
        </div>
    );
};

export default OfdSpikePage;
