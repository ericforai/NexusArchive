// Input: React
// Output: PdfViewer 组件
// Pos: 通用复用组件 - PDF 查看器

import React, { useRef, useEffect, useState } from 'react';
import { Page, Document, pdfjs } from 'react-pdf';

// 配置 PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

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
  scale,
  rotation,
  currentPage,
  onPageChange,
  onPageCountChange,
  onLoad,
  onError,
  className = '',
}: PdfViewerProps) {
  const [numPages, setNumPages] = useState(0);
  const [pdfError, setPdfError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
    onPageCountChange?.(numPages);
    onLoad?.();
  };

  const handleLoadError = (error: Error) => {
    const errorMsg = error.message || 'PDF 加载失败';
    setPdfError(errorMsg);
    onError?.(errorMsg);
  };

  return (
    <div
      ref={containerRef}
      className={`pdf-viewer flex items-center justify-center ${className}`}
      style={{
        transform: `scale(${scale}) rotate(${rotation}deg)`,
        transition: 'transform 0.3s ease',
      }}
    >
      <Document
        file={url}
        onLoadSuccess={handleLoadSuccess}
        onLoadError={handleLoadError}
        loading={
          <div className="flex items-center justify-center p-8">
            <div className="text-slate-400">加载中...</div>
          </div>
        }
        error={
          <div className="text-center text-slate-400">
            <p>PDF 加载失败</p>
            {pdfError && <p className="text-sm mt-2 text-rose-400">{pdfError}</p>}
          </div>
        }
      >
        <Page
          pageNumber={currentPage}
          renderTextLayer={false}
          renderAnnotationLayer={false}
          className="shadow-lg"
        />
      </Document>
    </div>
  );
}

export default PdfViewer;
