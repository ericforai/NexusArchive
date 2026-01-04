// Input: React、本地模块 utils/storage
// Output: React 组件 OfdViewer
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react'

interface FileViewerProps {
    fileUrl: string
    fileType?: string  // 'pdf', 'ofd', etc.
    fileName?: string
    className?: string
    style?: React.CSSProperties
    token?: string  // Auth token for fetching files (avoid store coupling)
}

export function FileViewer({ fileUrl, fileType, fileName, className, style, token }: FileViewerProps) {
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [blobUrl, setBlobUrl] = useState<string | null>(null)
    const [detectedType, setDetectedType] = useState<string>('')

    useEffect(() => {
        let mounted = true
        let objectUrl: string | null = null
        setLoading(true)
        setError(null)

        async function loadFile() {
            try {
                // Use token from props instead of store (avoid shared component coupling)
                const headers: Record<string, string> = {}
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`
                }

                const response = await fetch(fileUrl, { headers })
                if (!response.ok) {
                    throw new Error(`Failed to fetch file: ${response.statusText}`)
                }

                const contentType = response.headers.get('content-type') || ''
                const blob = await response.blob()
                const url = URL.createObjectURL(blob)
                objectUrl = url

                // Detect file type from content-type or file extension
                let type = ''
                const inputType = fileType?.toLowerCase() || ''

                // First check input fileType
                if (inputType.includes('pdf') || inputType === 'pdf') {
                    type = 'pdf'
                } else if (inputType.includes('ofd') || inputType === 'ofd') {
                    type = 'ofd'
                }

                // Fallback to content-type detection
                if (!type) {
                    if (contentType.includes('pdf')) {
                        type = 'pdf'
                    } else if (contentType.includes('ofd')) {
                        type = 'ofd'
                    } else if (contentType.includes('image')) {
                        type = 'image'
                    } else if (fileName?.toLowerCase().endsWith('.pdf')) {
                        type = 'pdf'
                    } else if (fileName?.toLowerCase().endsWith('.ofd')) {
                        type = 'ofd'
                    } else if (fileName?.toLowerCase().match(/\.(jpg|jpeg|png|gif|bmp|webp)$/)) {
                        type = 'image'
                    }
                }

                if (mounted) {
                    setBlobUrl(url)
                    setDetectedType(type)
                }
            } catch (err: any) {
                console.error("File load error:", err)
                if (mounted) {
                    setError(err.message || 'Failed to load file')
                }
            } finally {
                if (mounted) {
                    setLoading(false)
                }
            }
        }

        loadFile()

        return () => {
            mounted = false
            if (objectUrl) {
                URL.revokeObjectURL(objectUrl)
            }
        }
    }, [fileUrl, fileType, fileName])

    const handleDownload = () => {
        if (blobUrl) {
            const link = document.createElement('a')
            link.href = blobUrl
            link.download = fileName || `document.${detectedType || 'file'}`
            link.click()
        }
    }

    if (loading) {
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <div className="p-8 text-center text-gray-500 h-full flex flex-col items-center justify-center">
                    <div className="animate-spin inline-block w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mb-4"></div>
                    <p>正在加载文件...</p>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <div className="p-8 text-center text-red-500 h-full flex items-center justify-center">
                    <p>加载失败: {error}</p>
                </div>
            </div>
        )
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
        )
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
        )
    }

    // OFD and other formats - Show download UI
    return (
        <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
            <div className="flex flex-col items-center justify-center h-full min-h-[400px] bg-slate-50 rounded-lg p-8">
                <div className="text-6xl mb-4">{detectedType === 'ofd' ? '📄' : '📁'}</div>
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
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                    下载文件
                </button>
                {detectedType === 'ofd' && (
                    <p className="text-xs text-slate-400 mt-4">
                        推荐使用：数科阅读器、福昕OFD阅读器、WPS
                    </p>
                )}
            </div>
        </div>
    )
}

// 保持向后兼容的 OfdViewer 导出
export function OfdViewer(props: FileViewerProps) {
    return <FileViewer {...props} fileType={props.fileType || 'ofd'} />
}
