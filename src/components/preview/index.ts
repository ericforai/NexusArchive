// Preview 组件库统一导出
export { FilePreview } from './FilePreview';
export type { FilePreviewProps, FileType } from './FilePreview';

export { PdfViewer } from './PdfViewer';
export type { PdfViewerProps } from './PdfViewer';

export { ImageViewer } from './ImageViewer';
export type { ImageViewerProps } from './ImageViewer';

export { PreviewToolbar } from './PreviewToolbar';
export type { PreviewToolbarProps } from './PreviewToolbar';
export { OfdViewer } from './OfdViewer';
export type { OfdViewerProps } from './OfdViewer';

// Smart 预览组件（使用 previewApi）
export { SmartFilePreview } from './SmartFilePreview';
export type { SmartFilePreviewProps, FileItem } from './SmartFilePreview';

// Modal 封装
export { FilePreviewModal } from './FilePreviewModal';
export type { FilePreviewModalProps } from './FilePreviewModal';

// Hook
export { useFilePreview } from './useFilePreview';
export type { UseFilePreviewParams, UseFilePreviewReturn } from './useFilePreview';
