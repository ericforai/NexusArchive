// Input: React、liteofd
// Output: LiteOfdPreview 组件
// Pos: 通用复用组件 - liteofd OFD 预览器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useRef, useState } from 'react';
import LiteOfdDefault, { LiteOfd as LiteOfdNamed } from 'liteofd';
import 'liteofd/main.css';

// 处理 ESM/CJS 的默认导出问题
const LiteOfd = (typeof LiteOfdDefault === 'function' ? LiteOfdDefault : (typeof LiteOfdNamed === 'function' ? LiteOfdNamed : (LiteOfdDefault as any)?.default)) as any;

import { AlertCircle, Download, Loader2, ZoomIn, ZoomOut, Maximize, RotateCcw } from 'lucide-react';
import { client } from '@/api/client';

interface LiteOfdPreviewProps {
  fileUrl: string;
  fileName?: string;
  downloadUrl?: string;
  className?: string;
  onError?: (message: string) => void;
}

export function LiteOfdPreview({
  fileUrl,
  fileName,
  downloadUrl,
  className = '',
  onError,
}: LiteOfdPreviewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [scale, setScale] = useState(1.0);

  // 缩放处理函数
  const handleZoomIn = () => setScale(prev => Math.min(prev + 0.2, 3.0));
  const handleZoomOut = () => setScale(prev => Math.max(prev - 0.2, 0.2));
  const handleResetZoom = () => setScale(1.0);
  
  const handleFitWidth = () => {
    if (containerRef.current && wrapperRef.current) {
      const ofdElement = containerRef.current.firstElementChild as HTMLElement;
      if (ofdElement) {
        // 获取 OFD 页面的实际宽度
        const ofdWidth = ofdElement.offsetWidth || 800; // 兜底 800
        const availableWidth = wrapperRef.current.clientWidth - 48; // 减去 padding
        if (availableWidth > 0) {
          setScale(availableWidth / ofdWidth);
        }
      }
    }
  };

  console.log('[LiteOfdPreview] Props:', { fileUrl, fileName, downloadUrl, className });

  // 实际的渲染逻辑
  useEffect(() => {
    if (!fileUrl) {
      setError('缺少 OFD 文件地址');
      setLoading(false);
      return;
    }

    let cancelled = false;
    const currentContainer = containerRef.current;

    async function render() {
      console.log('[LiteOfdPreview] render called, fileUrl:', fileUrl, 'containerRef.current:', !!containerRef.current);

      // 等待 ref 被设置
      let attempts = 0;
      while (!containerRef.current && attempts < 10) {
        await new Promise(resolve => setTimeout(resolve, 10));
        attempts++;
      }

      if (!containerRef.current) {
        setError('OFD 预览容器未准备好');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      containerRef.current.innerHTML = '';
      setScale(1.0); // 重置缩放

      try {
        const normalizedUrl = normalizeClientUrl(fileUrl);
        const response = await client.get(normalizedUrl, { responseType: 'arraybuffer' });

        if (cancelled || !containerRef.current) return;

        console.log('[LiteOfdPreview] Starting liteofd parse...');
        const liteOfd = new LiteOfd();
        
        const _parsed = await Promise.race([
          liteOfd.parse(response.data),
          new Promise((_, reject) => 
            setTimeout(() => reject(new Error('LiteOFD 解析超时 (10s)')), 10000)
          )
        ]);
        
        if (cancelled || !containerRef.current) return;

        const rendered = liteOfd.render(
          undefined,
          'background-color:#f8fafc; padding: 16px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        );

        if (rendered && containerRef.current) {
          containerRef.current.appendChild(rendered);
          // 渲染后尝试一次自动适应宽度
          setTimeout(handleFitWidth, 100);
        } else {
          throw new Error('liteofd 渲染结果为空');
        }
      } catch (renderError) {
        console.error('[LiteOfdPreview] Render error:', renderError);
        if (cancelled) return;
        const errorMessage = renderError instanceof Error ? renderError.message : 'liteofd 渲染失败';
        setError(errorMessage);
        onError?.(errorMessage);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    render();

    return () => {
      cancelled = true;
      if (currentContainer) currentContainer.innerHTML = '';
    };
  }, [fileUrl, onError]);


  return (
    <div ref={wrapperRef} className={`relative h-full w-full overflow-hidden bg-slate-200 flex flex-col ${className}`}>
      {/* 缩放工具栏 */}
      {!loading && !error && (
        <div className="absolute top-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-1 bg-white/90 backdrop-blur-sm border border-slate-200 p-1 rounded-xl shadow-lg transition-opacity hover:opacity-100 opacity-60">
          <button onClick={handleZoomOut} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" title="缩小"><ZoomOut size={18} /></button>
          <div className="px-2 text-xs font-bold text-slate-500 min-w-[3rem] text-center">{Math.round(scale * 100)}%</div>
          <button onClick={handleZoomIn} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" title="放大"><ZoomIn size={18} /></button>
          <div className="w-px h-4 bg-slate-200 mx-1" />
          <button onClick={handleFitWidth} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" title="适应宽度"><Maximize size={18} /></button>
          <button onClick={handleResetZoom} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" title="重置"><RotateCcw size={18} /></button>
        </div>
      )}

      {/* 滚动容器 */}
      <div className="flex-1 overflow-auto p-4 flex flex-col items-center">
        <div 
          ref={containerRef} 
          className="transition-transform duration-200 ease-out origin-top"
          style={{ 
            transform: `scale(${scale})`,
            marginBottom: `${(scale - 1) * 50}%` // 补偿缩放后的底部空白
          }}
        />
      </div>

      {/* 覆盖层：加载状态 */}
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-slate-50/90">
          <div className="text-center text-slate-500">
            <Loader2 size={28} className="mx-auto mb-3 animate-spin text-blue-500" />
            <p className="text-sm font-medium">正在使用 liteofd 渲染...</p>
          </div>
        </div>
      )}
      {/* 覆盖层：错误状态 */}
      {error && !loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-slate-50/95">
          <div className="max-w-md px-6 text-center text-slate-500">
            <AlertCircle size={36} className="mx-auto mb-3 text-rose-500" />
            <p className="font-medium text-slate-700">OFD 渲染失败</p>
            <p className="mt-2 text-sm">{error}</p>
            {downloadUrl && (
              <a
                href={downloadUrl}
                download={fileName}
                className="mt-4 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
              >
                <Download size={16} />
                下载原始 OFD
              </a>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function normalizeClientUrl(fileUrl: string): string {
  if (!fileUrl) return fileUrl;
  if (fileUrl.startsWith('http://') || fileUrl.startsWith('https://')) {
    return fileUrl;
  }
  
  let url = fileUrl;

  // 针对服务器端出现的重复前缀进行硬修复
  if (url.includes('/api/api/')) {
    url = url.replace('/api/api/', '/api/');
  }

  // 如果以 /api/ 开头，移除它及其后的斜杠，使之相对于 client 的 baseURL (/api)
  if (url.startsWith('/api/')) {
    return url.slice(5);
  }
  // 如果以 api/ 开头
  if (url.startsWith('api/')) {
    return url.slice(4);
  }
  
  // 如果已经是以 / 开头的路径且不含 api，axios 可能会根据配置处理
  // 为保险起见，如果它以 / 开头，我们也移除它，使之相对于 baseURL
  if (url.startsWith('/')) {
    return url.slice(1);
  }
  
  return url;
}

export default LiteOfdPreview;
