// Input: React, lucide-react
// Output: FilePreview 组件
// Pos: 通用复用组件 - 文件预览器

import React, { useState, useCallback } from 'react';
import {
  File, Loader2, AlertCircle
} from 'lucide-react';
import { PdfViewer } from './PdfViewer';
import { ImageViewer } from './ImageViewer';
import { OfdViewer } from './OfdViewer';
import { PreviewToolbar } from './PreviewToolbar';

export type FileType = 'pdf' | 'image' | 'ofd' | 'unknown';

export interface FilePreviewProps {
  url: string;
  fileName?: string;
  fileType?: FileType;
  onClose?: () => void;
  onDownload?: () => void;
  className?: string;
  showToolbar?: boolean;
  initialPage?: number;
}

/**
 * 统一的文件预览组件
 * <p>
 * 支持 PDF、图片、OFD 等多种文件格式的预览
 * </p>
 */
export function FilePreview({
  url,
  fileName,
  fileType: propFileType,
  onClose,
  onDownload,
  className = '',
  showToolbar = true,
  initialPage = 1,
}: FilePreviewProps) {
  const [scale, setScale] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // 检测文件类型
  const detectFileType = useCallback((): FileType => {
    if (propFileType) return propFileType;

    const ext = url.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return 'pdf';
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) return 'image';
    if (ext === 'ofd') return 'ofd';
    return 'unknown';
  }, [url, propFileType]);

  const fileType = detectFileType();

  // 缩放操作
  const handleZoomIn = () => setScale(prev => Math.min(prev + 0.25, 3));
  const handleZoomOut = () => setScale(prev => Math.max(prev - 0.25, 0.5));
  const handleResetZoom = () => setScale(1);

  // 旋转操作
  const handleRotate = () => setRotation(prev => (prev + 90) % 360);

  // 全屏操作
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  // 监听全屏变化
  React.useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  // 渲染预览内容
  const renderPreview = () => {
    switch (fileType) {
      case 'pdf':
        return (
          <PdfViewer
            url={url}
            scale={scale}
            rotation={rotation}
            currentPage={currentPage}
            onPageChange={setCurrentPage}
            onLoad={() => setLoading(false)}
            onError={(err) => {
              setError(err);
              setLoading(false);
            }}
          />
        );

      case 'image':
        return (
          <ImageViewer
            url={url}
            scale={scale}
            rotation={rotation}
            onLoad={() => setLoading(false)}
            onError={(err) => {
              setError(err);
              setLoading(false);
            }}
          />
        );

      case 'ofd':
        return (
          <OfdViewer
            url={url}
            fileName={fileName}
            scale={scale}
            rotation={rotation}
            downloadUrl={url}
            onLoad={() => setLoading(false)}
            onError={(err) => {
              setError(err);
              setLoading(false);
            }}
          />
        );

      default:
        return (
          <div className="flex items-center justify-center h-full text-slate-500">
            <div className="text-center">
              <File size={48} className="mx-auto mb-4 opacity-50" />
              <p>不支持的文件格式</p>
              <p className="text-sm mt-2">{fileName || url}</p>
            </div>
          </div>
        );
    }
  };

  return (
    <div className={`file-preview relative bg-slate-900 dark:bg-black ${className} ${isFullscreen ? 'fixed inset-0 z-50' : ''}`}>
      {/* Toolbar */}
      {showToolbar && (
        <PreviewToolbar
          fileName={fileName}
          fileType={fileType}
          scale={scale}
          rotation={rotation}
          currentPage={currentPage}
          onZoomIn={handleZoomIn}
          onZoomOut={handleZoomOut}
          onResetZoom={handleResetZoom}
          onRotate={handleRotate}
          onDownload={onDownload}
          onClose={onClose}
          onFullscreen={toggleFullscreen}
          isFullscreen={isFullscreen}
        />
      )}

      {/* Preview Content */}
      <div className="flex items-center justify-center h-full overflow-auto p-4">
        {loading && fileType !== 'ofd' && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/50">
            <Loader2 size={32} className="animate-spin text-blue-500" />
          </div>
        )}

        {error && fileType !== 'ofd' && (
          <div className="text-center text-slate-400">
            <AlertCircle size={48} className="mx-auto mb-4 text-rose-500" />
            <p>加载失败</p>
            <p className="text-sm mt-2">{error}</p>
          </div>
        )}

        {(fileType === 'ofd' || (!loading && !error)) && renderPreview()}
      </div>
    </div>
  );
}

export default FilePreview;
