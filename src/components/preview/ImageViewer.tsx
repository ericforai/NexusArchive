// Input: React
// Output: ImageViewer 组件
// Pos: 通用复用组件 - 图片查看器

import React, { useState, useRef, useEffect } from 'react';

export interface ImageViewerProps {
  url: string;
  scale: number;
  rotation: number;
  onLoad?: () => void;
  onError?: (error: string) => void;
  className?: string;
}

/**
 * 图片查看器组件
 * <p>
 * 支持缩放、旋转、拖拽平移
 * </p>
 */
export function ImageViewer({
  url,
  scale,
  rotation,
  onLoad,
  onError,
  className = '',
}: ImageViewerProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const imgRef = useRef<HTMLImageElement>(null);

  const handleMouseDown = (e: React.MouseEvent) => {
    if (scale > 1) {
      setIsDragging(true);
      setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging) {
      setPosition({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleLoad = () => {
    onLoad?.();
  };

  const handleError = () => {
    onError?.('图片加载失败');
  };

  // 重置位置当缩放变为1时
  useEffect(() => {
    if (scale === 1) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPosition({ x: 0, y: 0 });
    }
  }, [scale]);

  return (
    <div
      className={`image-viewer flex items-center justify-center overflow-hidden ${className}`}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      <img
        ref={imgRef}
        src={url}
        alt="Preview"
        onLoad={handleLoad}
        onError={handleError}
        onMouseDown={handleMouseDown}
        className={`max-w-full max-h-full object-contain transition-transform cursor-${scale > 1 ? 'grab' : 'default'}`}
        style={{
          transform: `translate(${position.x}px, ${position.y}px) scale(${scale}) rotate(${rotation}deg)`,
          cursor: isDragging ? 'grabbing' : scale > 1 ? 'grab' : 'default',
        }}
        draggable={false}
      />
    </div>
  );
}

export default ImageViewer;
