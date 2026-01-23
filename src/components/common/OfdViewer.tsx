// Input: React
// Output: React 组件 OfdViewer/FileViewer
// Pos: 通用复用组件 - 文件查看器（支持 PDF/OFD/图片）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { FileText, File, Download } from 'lucide-react';

interface FileViewerProps {
    fileUrl: string;
    fileType?: string;  // 'pdf', 'ofd', etc.
    fileName?: string;
    className?: string;
    style?: React.CSSProperties;
    token?: string;  // Auth token for fetching files (avoid store coupling)
}

interface FileLoaderResult {
    loading: boolean;
    error: string | null;
    blobUrl: string | null;
    detectedType: string;
    retry: () => void;
}

/**
 * Detect file type from content-type, fileType prop, or file extension
 */
function detectFileType(
    contentType: string,
    fileType: string | undefined,
    fileName: string | undefined
): string {
    const inputType = fileType?.toLowerCase() || '';

    // First check input fileType
    if (inputType.includes('pdf') || inputType === 'pdf') {
        return 'pdf';
    }
    if (inputType.includes('ofd') || inputType === 'ofd') {
        return 'ofd';
    }

    // Fallback to content-type detection
    if (contentType.includes('pdf')) {
        return 'pdf';
    }
    if (contentType.includes('ofd')) {
        return 'ofd';
    }
    if (contentType.includes('image')) {
        return 'image';
    }

    // Fallback to file extension
    const lowerFileName = fileName?.toLowerCase() || '';
    if (lowerFileName.endsWith('.pdf')) {
        return 'pdf';
    }
    if (lowerFileName.endsWith('.ofd')) {
        return 'ofd';
    }
    if (/\.(jpg|jpeg|png|gif|bmp|webp)$/.test(lowerFileName)) {
        return 'image';
    }

    return '';
}

/**
 * Custom hook for loading files and creating blob URLs
 * Handles aborting, timeout, and cleanup
 */
function useFileLoader(
    fileUrl: string,
    token: string | undefined,
    fileType: string | undefined,
    fileName: string | undefined
): FileLoaderResult {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [blobUrl, setBlobUrl] = useState<string | null>(null);
    const [detectedType, setDetectedType] = useState<string>('');
    const [retryKey, setRetryKey] = useState(0);

    // Track blob URL for cleanup
    const blobUrlRef = useRef<string | null>(null);

    // Sync ref with state
    useEffect(() => {
        blobUrlRef.current = blobUrl;
    }, [blobUrl]);

    // Cleanup blob URL on unmount
    useEffect(() => {
        return () => {
            if (blobUrlRef.current) {
                URL.revokeObjectURL(blobUrlRef.current);
            }
        };
    }, []);

    // Load file
    useEffect(() => {
        let isCurrentEffect = true;
        let localObjectUrl: string | null = null;
        let aborted = false;

        // Skip if fileUrl is empty
        if (!fileUrl) {
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);

        async function loadFile() {
            try {
                // Prepare headers
                const headers: Record<string, string> = {};
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                // Fetch with timeout
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 60000); // 60s timeout

                const response = await fetch(fileUrl, {
                    headers,
                    signal: controller.signal,
                });
                clearTimeout(timeoutId);

                if (aborted || !isCurrentEffect) {
                    return;
                }

                if (!response.ok) {
                    throw new Error(`Failed to fetch file: ${response.statusText}`);
                }

                const contentType = response.headers.get('content-type') || '';

                if (!isCurrentEffect) {
                    return;
                }

                // Create blob URL
                const arrayBuffer = await response.arrayBuffer();

                if (!isCurrentEffect) {
                    return;
                }

                const blob = new Blob([arrayBuffer], { type: contentType });
                const url = URL.createObjectURL(blob);
                localObjectUrl = url;

                // Detect file type
                const type = detectFileType(contentType, fileType, fileName);

                // Only update state if this effect is still current
                if (isCurrentEffect && !aborted) {
                    setBlobUrl(url);
                    setDetectedType(type);
                    setLoading(false);
                } else {
                    URL.revokeObjectURL(url);
                }
            } catch (err: unknown) {
                if (err instanceof Error && err.name === 'AbortError') {
                    if (isCurrentEffect) {
                        setError('文件加载超时');
                        setLoading(false);
                    }
                } else if (isCurrentEffect) {
                    const errorMessage = err instanceof Error ? err.message : 'Failed to load file';
                    setError(errorMessage);
                    setLoading(false);
                }
            }
        }

        loadFile();

        return () => {
            isCurrentEffect = false;
            aborted = true;
            if (localObjectUrl) {
                URL.revokeObjectURL(localObjectUrl);
            }
        };
    }, [fileUrl, token, fileType, fileName, retryKey]); // Include all dependencies

    // Retry function
    const retry = useCallback(() => {
        setRetryKey(prev => prev + 1);
    }, []);

    return { loading, error, blobUrl, detectedType, retry };
}

function FileViewerComponent({ fileUrl, fileType, fileName, className, style, token }: FileViewerProps) {
    const { loading, error, blobUrl, detectedType, retry } = useFileLoader(
        fileUrl,
        token,
        fileType,
        fileName
    );

    const handleDownload = useCallback(() => {
        if (blobUrl) {
            const link = document.createElement('a');
            link.href = blobUrl;
            link.download = fileName || `document.${detectedType || 'file'}`;
            link.click();
        }
    }, [blobUrl, fileName, detectedType]);

    // Loading state
    if (loading) {
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <div className="p-8 text-center text-gray-500 h-full flex flex-col items-center justify-center">
                    <div className="animate-spin inline-block w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mb-4" />
                    <p>正在加载文件...</p>
                </div>
            </div>
        );
    }

    // Error state
    if (error) {
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <div className="p-8 text-center text-red-500 h-full flex flex-col items-center justify-center">
                    <p>加载失败: {error}</p>
                    <button
                        onClick={retry}
                        className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                    >
                        重试
                    </button>
                </div>
            </div>
        );
    }

    // PDF - Use browser's built-in viewer
    if (detectedType === 'pdf' && blobUrl) {
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <iframe
                    src={blobUrl}
                    className="w-full h-full min-h-[500px] border-0"
                    title="PDF文件预览"
                />
            </div>
        );
    }

    // Image Preview
    if (detectedType === 'image' && blobUrl) {
        return (
            <div className={`file-viewer-wrapper ${className || ''} flex items-center justify-center bg-gray-100 overflow-auto`} style={style}>
                <img
                    src={blobUrl}
                    alt={fileName || 'Preview'}
                    className="max-w-full max-h-full object-contain shadow-lg"
                />
            </div>
        );
    }

    // OFD and other formats - Show download UI
    return (
        <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
            <div className="flex flex-col items-center justify-center h-full min-h-[400px] bg-slate-50 rounded-lg p-8">
                <div className="mb-4 text-slate-400">
                    {detectedType === 'ofd' ? (
                        <FileText size={64} strokeWidth={1.5} />
                    ) : (
                        <File size={64} strokeWidth={1.5} />
                    )}
                </div>
                <h3 className="text-xl font-bold text-slate-700 mb-2">
                    {detectedType === 'ofd' ? 'OFD 电子凭证' : '文件预览'}
                </h3>
                <p className="text-slate-500 mb-6 text-center">
                    {detectedType === 'ofd' ? (
                        <>OFD 是国家标准版式文档格式<br />请下载后使用专业阅读器查看</>
                    ) : (
                        <>此文件类型暂不支持在线预览<br />请下载后查看</>
                    )}
                </p>
                <button
                    onClick={handleDownload}
                    className="px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors shadow-lg shadow-blue-500/30 flex items-center gap-2"
                >
                    <Download size={20} />
                    下载文件
                </button>
                {detectedType === 'ofd' && (
                    <p className="text-xs text-slate-400 mt-4">
                        推荐使用：数科阅读器、福昕OFD阅读器、WPS
                    </p>
                )}
            </div>
        </div>
    );
}

// Wrap with React.memo to prevent unnecessary re-renders
export const FileViewer = React.memo(FileViewerComponent);

// 保持向后兼容的 OfdViewer 导出
export function OfdViewer(props: FileViewerProps) {
    return <FileViewer {...props} fileType={props.fileType || 'ofd'} />;
}
