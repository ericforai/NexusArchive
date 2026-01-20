// Input: React
// Output: PdfViewer 组件
// Pos: 通用复用组件 - PDF 查看器

import React, { useRef, useState } from 'react';
import { Page, Document, pdfjs } from 'react-pdf';
import { ChevronLeft, ChevronRight } from 'lucide-react';

// 配置 PDF.js worker - 使用 ?url 获取实际的 URL 字符串
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
pdfjs.GlobalWorkerOptions.workerSrc = workerUrl;

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

  const handlePrevPage = () => {
    if (currentPage > 1) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNextPage = () => {
    if (currentPage < numPages) {
      onPageChange(currentPage + 1);
    }
  };

  return (
    <div className={`pdf-viewer flex flex-col h-full ${className}`}>
      {/* PDF 渲染区域 */}
      <div
        ref={containerRef}
        className="flex-1 flex items-center justify-center overflow-auto"
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

      {/* 分页导航栏 */}
      {numPages > 1 && (
        <div className="flex items-center justify-center gap-4 py-3 border-t border-slate-200 bg-white">
          <button
            onClick={handlePrevPage}
            disabled={currentPage <= 1}
            className="p-2 text-slate-600 hover:bg-slate-100 rounded disabled:opacity-40 disabled:cursor-not-allowed"
            title="上一页"
          >
            <ChevronLeft size={20} />
          </button>

          <span className="text-sm text-slate-700">
            {currentPage} / {numPages}
          </span>

          <button
            onClick={handleNextPage}
            disabled={currentPage >= numPages}
            className="p-2 text-slate-600 hover:bg-slate-100 rounded disabled:opacity-40 disabled:cursor-not-allowed"
            title="下一页"
          >
            <ChevronRight size={20} />
          </button>

          {/* 页面输入框 */}
          <input
            type="number"
            min={1}
            max={numPages}
            value={currentPage}
            onChange={(e) => {
              const page = parseInt(e.target.value);
              if (page >= 1 && page <= numPages) {
                onPageChange(page);
              }
            }}
            className="w-16 px-2 py-1 text-center text-sm border border-slate-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      )}
    </div>
  );
}

export default PdfViewer;
