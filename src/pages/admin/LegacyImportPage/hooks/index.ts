// Input: Custom hooks exports
// Output: Hooks index barrel file
// Pos: src/pages/admin/LegacyImportPage/hooks/index.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * LegacyImportPage 自定义 Hooks 集合
 *
 * 本目录包含从 LegacyImportPage 组件中提取的可复用 Hooks：
 * - useFileUpload: 文件上传逻辑（拖拽、验证、大小限制）
 * - useImportPreview: 导入预览和执行逻辑
 * - useImportHistory: 导入历史查询和分页
 * - useFieldMapping: 字段映射说明和模板下载
 */

export { useFileUpload } from './useFileUpload';
export type { FileUploadState, FileValidationResult } from './useFileUpload';

export { useImportPreview } from './useImportPreview';
export type { PreviewState } from './useImportPreview';

export { useImportHistory } from './useImportHistory';
export type { ImportHistoryState, PaginationParams } from './useImportHistory';

export { useFieldMapping } from './useFieldMapping';
export type { DownloadState, RequiredField, OptionalField, TemplateType } from './useFieldMapping';
