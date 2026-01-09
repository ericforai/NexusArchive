// Input: React、lucide-react、useFileUpload hook
// Output: FileUploader 组件
// Pos: src/pages/admin/LegacyImportPage/components/FileUploader.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    Upload,
    FileText,
    X,
    Loader2,
    Eye,
    CheckCircle2,
} from 'lucide-react';
import { useFileUpload } from '../hooks/useFileUpload';

/**
 * FileUploader 组件属性
 */
export interface FileUploaderProps {
    /** 文件 */
    file: File | null;
    /** 是否正在加载 */
    loading: boolean;
    /** 是否正在导入 */
    importing: boolean;
    /** 是否显示预览按钮 */
    showPreviewButton: boolean;
    /** 是否显示导入按钮 */
    showImportButton: boolean;
    /** 文件变化回调 */
    onFileChange: (file: File | null) => void;
    /** 预览点击回调 */
    onPreview: () => void;
    /** 导入点击回调 */
    onImport: () => void;
    /** 清除文件回调 */
    onClear?: () => void;
}

/**
 * 文件上传组件
 *
 * 功能：
 * - 支持拖拽上传
 * - 支持点击选择文件
 * - 文件大小验证（最大100MB）
 * - 文件格式验证（CSV、Excel）
 * - 显示已选文件信息
 */
export const FileUploader: React.FC<FileUploaderProps> = ({
    file,
    loading,
    importing,
    showPreviewButton,
    showImportButton,
    onFileChange,
    onPreview,
    onImport,
    onClear,
}) => {
    const {
        validateFile,
        handleDragOver,
        handleDragLeave,
        clearFile,
        getFileSizeInMB,
        isDragging,
        MAX_FILE_SIZE,
        ACCEPTED_FILE_TYPES,
    } = useFileUpload();

    // 使用 hook 的处理函数，并在成功后调用回调
    const handleInputChangeWrapper = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0] ?? null;
        if (selectedFile) {
            const validation = validateFile(selectedFile);
            if (validation.valid) {
                onFileChange(selectedFile);
            }
        }
        e.target.value = '';
    };

    const handleDropWrapper = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        handleDragLeave(e);
        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile) {
            const validation = validateFile(droppedFile);
            if (validation.valid) {
                onFileChange(droppedFile);
            }
        }
    };

    const handleClear = () => {
        clearFile();
        onClear?.();
    };

    const maxSizeInMB = (MAX_FILE_SIZE / 1024 / 1024).toFixed(0);

    return (
        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">选择文件</h2>
            <div
                onDrop={handleDropWrapper}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                    isDragging
                        ? 'border-primary-400 bg-primary-50'
                        : 'border-slate-300 hover:border-primary-400'
                }`}
            >
                <Upload className="w-12 h-12 mx-auto text-slate-400 mb-4" />
                <p className="text-slate-600 mb-2">
                    拖拽文件到此处，或
                    <label className="text-primary-600 cursor-pointer hover:underline ml-1">
                        点击选择文件
                        <input
                            type="file"
                            accept={ACCEPTED_FILE_TYPES.join(',')}
                            onChange={handleInputChangeWrapper}
                            className="hidden"
                        />
                    </label>
                </p>
                <p className="text-sm text-slate-500">
                    支持 CSV、Excel (.xlsx, .xls) 格式
                </p>
                <p className="text-xs text-slate-400 mt-1">
                    <strong>文件大小限制：</strong>最大 {maxSizeInMB}MB | <strong>建议行数：</strong>不超过 10,000 行
                </p>
                {file && (
                    <div className="mt-4 p-3 bg-slate-50 rounded-lg">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <FileText className="w-5 h-5 text-slate-600" />
                                <span className="text-slate-900">{file.name}</span>
                                <span className="text-sm text-slate-500">
                                    ({getFileSizeInMB(file)} MB)
                                </span>
                            </div>
                            <button
                                onClick={handleClear}
                                className="text-slate-400 hover:text-slate-600"
                                type="button"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <div className="mt-4 flex gap-3">
                {showPreviewButton && (
                    <button
                        onClick={onPreview}
                        disabled={!file || loading}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        type="button"
                    >
                        {loading ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                            <Eye className="w-4 h-4" />
                        )}
                        预览数据
                    </button>
                )}
                {showImportButton && (
                    <button
                        onClick={onImport}
                        disabled={importing}
                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        type="button"
                    >
                        {importing ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                            <CheckCircle2 className="w-4 h-4" />
                        )}
                        确认导入
                    </button>
                )}
            </div>
        </div>
    );
};

export default FileUploader;
