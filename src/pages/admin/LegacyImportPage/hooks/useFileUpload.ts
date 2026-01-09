// Input: React, toast notification service
// Output: File upload hook (useFileUpload)
// Pos: src/pages/admin/LegacyImportPage/hooks/useFileUpload.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback } from 'react';
import { toast } from '../../../../utils/notificationService';

/**
 * 文件大小限制（100MB）
 */
const MAX_FILE_SIZE = 100 * 1024 * 1024;

/**
 * 支持的文件类型
 */
const ACCEPTED_FILE_TYPES = ['.csv', '.xlsx', '.xls'];

/**
 * 文件上传状态
 */
export interface FileUploadState {
    file: File | null;
    uploading: boolean;
    error: string | null;
}

/**
 * 文件验证结果
 */
export interface FileValidationResult {
    valid: boolean;
    error?: string;
}

/**
 * 文件上传 Hook
 *
 * 负责处理文件选择、拖拽上传、文件验证等功能
 *
 * @returns 文件上传状态和操作方法
 */
export function useFileUpload() {
    const [file, setFile] = useState<File | null>(null);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isDragging, setIsDragging] = useState(false);

    /**
     * 验证文件
     */
    const validateFile = useCallback((fileToValidate: File): FileValidationResult => {
        // 清除之前的错误
        setError(null);

        // 检查文件大小
        if (fileToValidate.size > MAX_FILE_SIZE) {
            const sizeInMB = (fileToValidate.size / 1024 / 1024).toFixed(2);
            const errorMessage = `文件大小超过限制！最大允许 100MB，当前文件：${sizeInMB} MB`;
            setError(errorMessage);
            toast.warning(errorMessage);
            return { valid: false, error: errorMessage };
        }

        // 检查文件扩展名
        const fileName = fileToValidate.name.toLowerCase();
        const hasValidExtension = ACCEPTED_FILE_TYPES.some(ext => fileName.endsWith(ext));

        if (!hasValidExtension) {
            const errorMessage = `不支持的文件格式。请使用 ${ACCEPTED_FILE_TYPES.join(', ')} 格式`;
            setError(errorMessage);
            toast.warning(errorMessage);
            return { valid: false, error: errorMessage };
        }

        return { valid: true };
    }, []);

    /**
     * 处理文件选择（通过 input 元素）
     */
    const handleFileSelect = useCallback((
        selectedFile: File | null,
        onSuccess?: (file: File) => void
    ) => {
        if (!selectedFile) {
            return;
        }

        const validation = validateFile(selectedFile);

        if (validation.valid) {
            setFile(selectedFile);
            onSuccess?.(selectedFile);
        }
    }, [validateFile]);

    /**
     * 处理 input change 事件
     */
    const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0] ?? null;

        if (selectedFile) {
            handleFileSelect(selectedFile);
        }

        // 重置 input 以允许重复选择同一文件
        e.target.value = '';
    }, [handleFileSelect]);

    /**
     * 处理文件拖拽放下
     */
    const handleDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(false);

        const droppedFile = e.dataTransfer.files[0];

        if (droppedFile) {
            handleFileSelect(droppedFile);
        }
    }, [handleFileSelect]);

    /**
     * 处理拖拽经过
     */
    const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(true);
    }, []);

    /**
     * 处理拖拽离开
     */
    const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragging(false);
    }, []);

    /**
     * 清除文件
     */
    const clearFile = useCallback(() => {
        setFile(null);
        setError(null);
    }, []);

    /**
     * 获取文件大小（格式化为 MB）
     */
    const getFileSizeInMB = useCallback((fileToCheck: File): string => {
        return (fileToCheck.size / 1024 / 1024).toFixed(2);
    }, []);

    return {
        // 状态
        file,
        uploading,
        error,
        isDragging,

        // 设置方法
        setFile,
        setUploading,
        setError,

        // 操作方法
        handleFileSelect,
        handleInputChange,
        handleDrop,
        handleDragOver,
        handleDragLeave,
        clearFile,
        getFileSizeInMB,
        validateFile,

        // 常量
        MAX_FILE_SIZE,
        ACCEPTED_FILE_TYPES,
    };
}
