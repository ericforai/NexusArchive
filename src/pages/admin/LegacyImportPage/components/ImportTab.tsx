// Input: React、API types、Sub-components
// Output: ImportTab 组件
// Pos: src/pages/admin/LegacyImportPage/components/ImportTab.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import type { ImportPreviewData, ImportResultData } from '../types';
import { FileUploader } from './FileUploader';
import { ComplianceWarning } from './import/ComplianceWarning';
import { PreviewResultSection } from './import/PreviewResultSection';

export interface ImportTabProps {
    file: File | null;
    loading: boolean;
    importing: boolean;
    previewResult: ImportPreviewData | null;
    importResult: ImportResultData | null;
    onFileChange: (file: File | null) => void;
    onPreview: () => void;
    onImport: () => void;
    onDownloadCsvTemplate: () => void;
    onDownloadExcelTemplate: () => void;
    onDownloadErrorReport: (importId: string) => void;
}

export const ImportTab: React.FC<ImportTabProps> = (props) => (
    <div className="max-w-6xl mx-auto space-y-6">
        <ComplianceWarning />
        <div className="flex gap-3">
            <button onClick={props.onDownloadCsvTemplate} className="px-4 py-2 border rounded-lg hover:bg-slate-50">下载 CSV 模板</button>
            <button onClick={props.onDownloadExcelTemplate} className="px-4 py-2 border rounded-lg hover:bg-slate-50">下载 Excel 模板</button>
        </div>
        <FileUploader
            file={props.file} loading={props.loading} importing={props.importing}
            showPreviewButton={true} showImportButton={!!props.previewResult}
            onFileChange={props.onFileChange} onPreview={props.onPreview} onImport={props.onImport}
            onClear={() => props.onFileChange(null)}
        />
        {props.previewResult && <PreviewResultSection previewResult={props.previewResult} />}
    </div>
);
