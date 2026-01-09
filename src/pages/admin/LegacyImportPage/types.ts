// Input: React、legacy import API types
// Output: LegacyImportPage 类型定义
// Pos: src/pages/admin/LegacyImportPage/types.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import type { ImportPreviewResult, ImportResult, LegacyImportTask, ImportError } from '../../../api/legacyImport';

/**
 * 标签页类型
 */
export type ActiveTab = 'import' | 'history';

/**
 * 导入历史项（LegacyImportTask 的别名）
 */
export type ImportHistoryItem = LegacyImportTask;

/**
 * 导入预览结果（ImportPreviewResult 的别名）
 */
export type ImportPreviewData = ImportPreviewResult;

/**
 * 导入结果（ImportResult 的别名）
 */
export type ImportResultData = ImportResult;

/**
 * 导入错误（ImportError 的别名）
 */
export type ImportErrorItem = ImportError;

/**
 * 状态筛选选项
 */
export interface StatusFilterOption {
    value: string;
    label: string;
}

/**
 * 预览统计数据
 */
export interface PreviewStatistics {
    totalRows: number;
    validRows: number;
    invalidRows: number;
    fondsCount: number;
    entityCount: number;
    willCreateFonds: string[];
    willCreateEntities: string[];
}

/**
 * 模板下载类型
 */
export type TemplateType = 'csv' | 'excel';

/**
 * 文件上传回调
 */
export interface FileUploadCallbacks {
    onFileChange: (file: File | null) => void;
    onPreview: () => void;
    onImport: () => void;
}

/**
 * 历史列表回调
 */
export interface HistoryListCallbacks {
    onDownloadErrorReport: (importId: string) => void;
    onRetry?: (importId: string) => void;
}

/**
 * 模态框状态
 */
export interface ModalState {
    visible: boolean;
    title: string;
    content: React.ReactNode;
}

/**
 * 导入指南状态
 */
export interface GuideState {
    showDetail: boolean;
    showCompliance: boolean;
}
