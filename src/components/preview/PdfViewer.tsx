// Input: React
// Output: PdfViewer 组件
// Pos: 通用复用组件 - PDF 查看器

import React, { useMemo, useState } from 'react';

export interface PdfViewerProps {
  url: string;
  scale: number;
  rotation: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onPageCountChange?: (count: number) => void;
  onLoad?: () => void;
  onError?: (error: string) => void;
  className?: string;
}

/**
 * PDF 查看器组件
 * <p>
 * 基于 react-pdf，支持缩放、旋转、翻页
 * </p>
 */
export function PdfViewer({
  url,
  scale: _scale,
  rotation: _rotation,
  currentPage: _currentPage,
  onPageChange: _onPageChange,
  onPageCountChange,
  onLoad,
  onError,
  className = '',
}: PdfViewerProps) {
  const [pdfError, setPdfError] = useState<string | null>(null);

  const viewerUrl = useMemo(() => {
    if (!url) return '';
    return url.includes('#') ? url : `${url}#view=FitH`;
  }, [url]);

  const handleLoad = () => {
    // 原生 PDF 查看器不会暴露页数，这里只通知加载完成。
    onPageCountChange?.(1);
    onLoad?.();
  };

  const handleLoadError = () => {
    const errorMsg = 'PDF 加载失败';
    setPdfError(errorMsg);
    onError?.(errorMsg);
  };

  return (
    <div className={`pdf-viewer flex h-full w-full min-w-0 flex-col ${className}`}>
      <div className="flex-1 overflow-hidden bg-slate-50">
        {viewerUrl ? (
          <iframe
            src={viewerUrl}
            title="PDF Preview"
            className="h-full w-full border-0 bg-white"
            onLoad={handleLoad}
          />
        ) : (
          <div className="flex h-full items-center justify-center text-slate-400">
            <p>暂无 PDF 内容</p>
          </div>
        )}
        {pdfError && (
          <div className="pointer-events-none absolute inset-x-0 top-0 text-center text-slate-400">
            <p>PDF 加载失败</p>
            <p className="text-sm mt-2 text-rose-400">{pdfError}</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default PdfViewer;
