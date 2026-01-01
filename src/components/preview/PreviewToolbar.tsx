// Input: React, lucide-react
// Output: PreviewToolbar 组件
// Pos: 通用复用组件 - 预览工具栏

import React from 'react';
import {
  X, ZoomIn, ZoomOut, RotateCw, Download, Maximize2, Minimize2,
  FileText, Image as ImageIcon, File
} from 'lucide-react';

export interface PreviewToolbarProps {
  fileName?: string;
  fileType: 'pdf' | 'image' | 'ofd' | 'unknown';
  scale: number;
  rotation: number;
  currentPage?: number;
  totalPages?: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onResetZoom?: () => void;
  onRotate: () => void;
  onDownload?: () => void;
  onClose?: () => void;
  onFullscreen?: () => void;
  isFullscreen?: boolean;
  className?: string;
}

/**
 * 预览工具栏组件
 * <p>
 * 提供缩放、旋转、下载、关闭等操作
 * </p>
 */
export function PreviewToolbar({
  fileName,
  fileType,
  scale,
  rotation,
  currentPage = 1,
  totalPages,
  onZoomIn,
  onZoomOut,
  onResetZoom,
  onRotate,
  onDownload,
  onClose,
  onFullscreen,
  isFullscreen = false,
  className = '',
}: PreviewToolbarProps) {
  const getFileIcon = () => {
    switch (fileType) {
      case 'pdf': return <FileText size={16} />;
      case 'image': return <ImageIcon size={16} />;
      default: return <File size={16} />;
    }
  };

  const getFileTypeLabel = () => {
    switch (fileType) {
      case 'pdf': return 'PDF';
      case 'image': return '图片';
      case 'ofd': return 'OFD';
      default: return '文件';
    }
  };

  return (
    <div className={`preview-toolbar flex items-center justify-between px-4 py-3 bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 ${className}`}>
      {/* Left: File Info */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        {getFileIcon()}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-slate-800 dark:text-white truncate">
            {fileName || '未命名文件'}
          </p>
          <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
            <span>{getFileTypeLabel()}</span>
            {totalPages && (
              <>
                <span>•</span>
                <span>第 {currentPage} / {totalPages} 页</span>
              </>
            )}
            <span>•</span>
            <span>{Math.round(scale * 100)}%</span>
            {rotation > 0 && (
              <>
                <span>•</span>
                <span>旋转 {rotation}°</span>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-1">
        {/* Zoom */}
        <button
          onClick={onZoomOut}
          disabled={scale <= 0.5}
          className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          title="缩小"
        >
          <ZoomOut size={18} />
        </button>
        <button
          onClick={onResetZoom}
          className="px-2 py-1 text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          title="重置缩放"
        >
          {Math.round(scale * 100)}%
        </button>
        <button
          onClick={onZoomIn}
          disabled={scale >= 3}
          className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          title="放大"
        >
          <ZoomIn size={18} />
        </button>

        {/* Rotate */}
        <button
          onClick={onRotate}
          className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          title="旋转"
        >
          <RotateCw size={18} />
        </button>

        {/* Download */}
        {onDownload && (
          <button
            onClick={onDownload}
            className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
            title="下载"
          >
            <Download size={18} />
          </button>
        )}

        {/* Fullscreen */}
        {onFullscreen && (
          <button
            onClick={onFullscreen}
            className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
            title={isFullscreen ? '退出全屏' : '全屏'}
          >
            {isFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
          </button>
        )}

        {/* Close */}
        {onClose && (
          <button
            onClick={onClose}
            className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
            title="关闭"
          >
            <X size={18} />
          </button>
        )}
      </div>
    </div>
  );
}

export default PreviewToolbar;
