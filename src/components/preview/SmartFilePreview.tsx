// Input: React, useFilePreview, FilePreview components
// Output: SmartFilePreview 组件
// Pos: 通用复用组件 - 智能文件预览器（使用 previewApi）

import React, { useState, useCallback } from 'react';
import {
  Loader2,
  AlertCircle,
  RefreshCw,
  ZoomIn,
  ZoomOut,
  RotateCw,
  Download,
  ChevronLeft,
  ChevronRight,
  FileText,
} from 'lucide-react';
import { useFilePreview, UseFilePreviewParams } from './useFilePreview';
import { PdfViewer } from './PdfViewer';
import { ImageViewer } from './ImageViewer';
import { OfdViewer } from './OfdViewer';

export interface FileItem {
  id: string;
  fileName: string;
  fileType?: string;
}

export interface SmartFilePreviewProps extends Omit<UseFilePreviewParams, 'autoLoad'> {
  /** 文件列表（用于多文件切换） */
  files?: FileItem[];
  /** 当前选中的文件ID */
  currentFileId?: string;
  /** 文件切换回调 */
  onFileChange?: (fileId: string) => void;
  /** 显示工具栏 */
  showToolbar?: boolean;
  /** 显示文件切换按钮 */
  showFileNav?: boolean;
  /** 文件名（用于识别类型和显示） */
  fileName?: string;
  /** 自定义类名 */
  className?: string;
}

/**
 * 智能文件预览组件
 * <p>
 * 自动调用 previewApi 获取文件内容，支持权限验证、水印、错误处理
 * 支持多文件切换、缩放、旋转等操作
 * </p>
 */
export function SmartFilePreview({
  files,
  currentFileId,
  onFileChange,
  showToolbar = true,
  showFileNav = true,
  fileName: propFileName,
  className = '',
  ...previewParams
}: SmartFilePreviewProps) {
  const [scale, setScale] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);

  // 使用 useFilePreview 获取数据
  const { blobUrl, presignedUrl, loading, error, retry } = useFilePreview({
    ...previewParams,
    autoLoad: true,
  });

  // 当前文件信息
  const currentFile = files?.find(f => f.id === currentFileId);
  const fileName = currentFile?.fileName || propFileName || previewParams.fileId || '预览';

  // 文件索引
  const currentIndex = files?.findIndex(f => f.id === currentFileId) ?? -1;
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < (files?.length ?? 0) - 1;

  // 文件类型检测
  const fileType = currentFile?.fileType || detectFileType(fileName);

  // 缩放操作
  const handleZoomIn = () => setScale(prev => Math.min(prev + 0.25, 3));
  const handleZoomOut = () => setScale(prev => Math.max(prev - 0.25, 0.5));
  const handleResetZoom = () => setScale(1);

  // 旋转操作
  const handleRotate = () => setRotation(prev => (prev + 90) % 360);

  // 文件切换
  const handlePrev = useCallback(() => {
    if (hasPrev && files && onFileChange) {
      onFileChange(files[currentIndex - 1].id);
    }
  }, [hasPrev, files, currentIndex, onFileChange]);

  const handleNext = useCallback(() => {
    if (hasNext && files && onFileChange) {
      onFileChange(files[currentIndex + 1].id);
    }
  }, [hasNext, files, currentIndex, onFileChange]);

  // 下载
  const handleDownload = useCallback(() => {
    if (blobUrl) {
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = fileName;
      link.click();
    }
  }, [blobUrl, fileName]);

  // 渲染内容
  const renderContent = () => {
    // Loading
    if (loading) {
      return (
        <div className="flex items-center justify-center h-full">
          <Loader2 size={32} className="animate-spin text-blue-500" />
        </div>
      );
    }

    // Error
    if (error) {
      return (
        <div className="flex flex-col items-center justify-center h-full text-slate-500">
          <AlertCircle size={48} className="mb-4 text-rose-500" />
          <p className="font-medium text-slate-700">{error}</p>
          <button
            onClick={retry}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <RefreshCw size={16} />
            重新加载
          </button>
        </div>
      );
    }

    // No content
    if (!blobUrl && !presignedUrl) {
      return (
        <div className="flex items-center justify-center h-full text-slate-500">
          <p>暂无预览内容</p>
        </div>
      );
    }

    const url = blobUrl || presignedUrl || '';

    // 根据文件类型渲染
    if (fileType === 'pdf') {
      return (
        <PdfViewer
          url={url}
          scale={scale}
          rotation={rotation}
          currentPage={currentPage}
          onPageChange={setCurrentPage}
        />
      );
    }

    if (fileType === 'image') {
      return (
        <ImageViewer
          url={url}
          scale={scale}
          rotation={rotation}
        />
      );
    }

    if (fileType === 'ofd') {
      return (
        <OfdViewer
          url={url}
          fileName={fileName}
          scale={scale}
          rotation={rotation}
          downloadUrl={url}
        />
      );
    }

    // 其他格式
    return (
      <div className="flex flex-col items-center justify-center h-full bg-slate-100">
        <FileText size={48} className="mb-4 text-slate-400" />
        <p className="text-slate-600 font-medium">
          文件预览
        </p>
        <p className="text-sm text-slate-500 mt-2">
          此文件类型暂不支持在线预览
        </p>
        {blobUrl && (
          <button
            onClick={handleDownload}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Download size={16} />
            下载文件
          </button>
        )}
      </div>
    );
  };

  return (
    <div
      data-testid="smart-file-preview"
      className={`smart-file-preview relative bg-white dark:bg-slate-800 ${className}`}
    >
      {/* 悬浮工具栏 */}
      {showToolbar && !loading && !error && (
        <div className="preview-toolbar absolute top-0 left-0 right-0 z-10 flex items-center justify-between px-4 py-2 bg-white/90 dark:bg-slate-800/90 backdrop-blur-sm border-b border-slate-200 dark:border-slate-700">
          {/* 左侧：文件名 */}
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <FileText size={16} />
            <span className="text-sm font-medium text-slate-800 dark:text-white truncate">
              {fileName}
            </span>
            {files && files.length > 1 && (
              <>
                <span className="text-slate-400">•</span>
                <span className="text-xs text-slate-500">
                  {currentIndex + 1} / {files.length}
                </span>
              </>
            )}
          </div>

          {/* 中间：工具按钮 */}
          <div className="flex items-center gap-1">
            {/* 缩放 */}
            <button
              onClick={handleZoomOut}
              disabled={scale <= 0.5}
              className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded disabled:opacity-40"
              title="缩小"
            >
              <ZoomOut size={16} />
            </button>
            <span className="text-xs text-slate-600 px-2">{Math.round(scale * 100)}%</span>
            <button
              onClick={handleZoomIn}
              disabled={scale >= 3}
              className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded disabled:opacity-40"
              title="放大"
            >
              <ZoomIn size={16} />
            </button>
            {scale !== 1 && (
              <button
                onClick={handleResetZoom}
                className="px-2 py-1 text-xs text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded"
              >
                重置
              </button>
            )}

            {/* 旋转 */}
            <button
              onClick={handleRotate}
              className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded"
              title="旋转"
            >
              <RotateCw size={16} />
            </button>

            {/* 下载 */}
            {blobUrl && (
              <button
                onClick={handleDownload}
                className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded"
                title="下载"
              >
                <Download size={16} />
              </button>
            )}
          </div>

          {/* 右侧：文件切换 */}
          {showFileNav && files && files.length > 1 && (
            <div className="flex items-center gap-1">
              <button
                onClick={handlePrev}
                disabled={!hasPrev}
                className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded disabled:opacity-40"
                title="上一张"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                onClick={handleNext}
                disabled={!hasNext}
                className="p-1.5 text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded disabled:opacity-40"
                title="下一张"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </div>
      )}

      {/* 预览内容 */}
      <div className="preview-content flex items-center justify-center h-full min-h-[400px]">
        {renderContent()}
      </div>
    </div>
  );
}

// 辅助函数：检测文件类型
function detectFileType(fileName: string): 'pdf' | 'image' | 'ofd' | 'unknown' {
  const ext = fileName.split('.').pop()?.toLowerCase();
  if (ext === 'pdf') return 'pdf';
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) return 'image';
  if (ext === 'ofd') return 'ofd';
  return 'unknown';
}

export default SmartFilePreview;
