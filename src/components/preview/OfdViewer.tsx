// Input: React、本地 OFD 解析器
// Output: OFD 在线预览组件
// Pos: 通用复用组件 - OFD 查看器

import React, { useEffect, useState } from 'react';
import { AlertCircle, Download, FileText, Loader2 } from 'lucide-react';
import { ParsedOfdDocument, parseOfdDocument } from './ofdParser';

const PX_PER_MM = 3.8;

export interface OfdViewerProps {
  url: string;
  fileName?: string;
  className?: string;
  scale?: number;
  rotation?: number;
  downloadUrl?: string;
  onLoad?: () => void;
  onError?: (message: string) => void;
}

export function OfdViewer({
  url,
  fileName,
  className = '',
  scale = 1,
  rotation = 0,
  downloadUrl,
  onLoad,
  onError,
}: OfdViewerProps) {
  const [document, setDocument] = useState<ParsedOfdDocument | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadDocument() {
      if (!url) {
        setLoading(false);
        setError('缺少 OFD 文件地址');
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error(`加载失败: ${response.status}`);
        }

        const parsed = await parseOfdDocument(await response.arrayBuffer());
        if (cancelled) return;

        setDocument(parsed);
        onLoad?.();
      } catch (loadError) {
        if (cancelled) return;

        const message = loadError instanceof Error ? loadError.message : 'OFD 解析失败';
        setError(message);
        onError?.(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadDocument();

    return () => {
      cancelled = true;
    };
  }, [onError, onLoad, url]);

  if (loading) {
    return (
      <div data-testid="ofd-viewer" className={`flex items-center justify-center h-full bg-slate-100 ${className}`}>
        <div className="text-center text-slate-500">
          <Loader2 size={32} className="mx-auto mb-3 animate-spin text-blue-500" />
          <p className="font-medium">正在解析 OFD...</p>
        </div>
      </div>
    );
  }

  if (error || !document) {
    return (
      <div data-testid="ofd-viewer" className={`flex items-center justify-center h-full bg-slate-50 ${className}`}>
        <div className="max-w-md text-center text-slate-500 px-6">
          <AlertCircle size={40} className="mx-auto mb-3 text-rose-500" />
          <p className="font-medium text-slate-700">OFD 预览失败</p>
          <p className="text-sm mt-2">{error || '无法解析当前 OFD 文件'}</p>
          <p className="text-xs mt-3 text-slate-400">
            请尝试在较新的 Chromium 浏览器中打开，或下载后使用专业阅读器查看。
          </p>
          {downloadUrl && (
            <a
              href={downloadUrl}
              download={fileName}
              className="mt-4 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
            >
              <Download size={16} />
              下载 OFD
            </a>
          )}
        </div>
      </div>
    );
  }

  return (
    <div
      data-testid="ofd-viewer"
      className={`h-full overflow-auto bg-slate-100 px-4 py-6 ${className}`}
    >
      <div className="mx-auto flex w-fit flex-col gap-6">
        {document.pages.map((page, index) => {
          const width = page.width * PX_PER_MM * scale;
          const height = page.height * PX_PER_MM * scale;

          return (
            <div
              key={page.id}
              className="relative overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm"
              style={{
                width,
                height,
                transform: rotation ? `rotate(${rotation}deg)` : undefined,
                transformOrigin: 'center center',
              }}
            >
              <div className="absolute left-3 top-3 z-10 rounded-full bg-slate-900/75 px-2 py-1 text-[11px] text-white">
                第 {index + 1} 页
              </div>

              {page.images.map((image) => (
                <img
                  key={image.id}
                  src={image.src}
                  alt={fileName || 'OFD 页面'}
                  className="absolute block select-none"
                  style={{
                    left: image.x * PX_PER_MM * scale,
                    top: image.y * PX_PER_MM * scale,
                    width: image.width * PX_PER_MM * scale,
                    height: image.height * PX_PER_MM * scale,
                  }}
                />
              ))}

              {page.texts.map((text) => (
                <div
                  key={text.id}
                  className="absolute whitespace-pre-wrap break-words text-slate-900"
                  style={{
                    left: text.x * PX_PER_MM * scale,
                    top: text.y * PX_PER_MM * scale,
                    width: text.width * PX_PER_MM * scale,
                    minHeight: text.height * PX_PER_MM * scale,
                    fontSize: Math.max(text.fontSize * PX_PER_MM * scale, 12),
                    fontFamily: text.fontFamily || 'sans-serif',
                    lineHeight: 1.35,
                  }}
                >
                  {text.text}
                </div>
              ))}

              {!page.images.length && !page.texts.length && (
                <div className="flex h-full items-center justify-center text-slate-400">
                  <div className="text-center">
                    <FileText size={36} className="mx-auto mb-2 opacity-50" />
                    <p>当前页未解析出可渲染内容</p>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default OfdViewer;
