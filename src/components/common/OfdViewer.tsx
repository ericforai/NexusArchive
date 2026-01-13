// Input: React、本地模块 utils/storage
// Output: React 组件 OfdViewer
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useRef } from 'react'

interface FileViewerProps {
    fileUrl: string
    fileType?: string  // 'pdf', 'ofd', etc.
    fileName?: string
    className?: string
    style?: React.CSSProperties
    token?: string  // Auth token for fetching files (avoid store coupling)
}

function FileViewerComponent({ fileUrl, fileType, fileName, className, style, token }: FileViewerProps) {
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [blobUrl, setBlobUrl] = useState<string | null>(null)
    const [detectedType, setDetectedType] = useState<string>('')
    // Use ref to track the current blob URL for cleanup
    const blobUrlRef = useRef<string | null>(null)

    // Sync ref with state
    useEffect(() => {
        blobUrlRef.current = blobUrl
    }, [blobUrl])

    console.log('[FileViewer] Render:', { fileUrl, fileType, fileName, hasToken: !!token, currentBlobUrl: blobUrl })

    useEffect(() => {
        // Use a ref to track if this specific effect instance is still valid
        let isCurrentEffect = true
        let localObjectUrl: string | null = null
        let aborted = false

        // Skip if fileUrl is empty
        if (!fileUrl) {
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);

        console.log('[FileViewer] Starting load for:', fileUrl, '| token:', token ? 'present' : 'null/undefined')

        async function loadFile() {
            try {
                // Use token from props instead of store (avoid shared component coupling)
                const headers: Record<string, string> = {}
                if (token) {
                    headers['Authorization'] = `Bearer ${token}`
                }

                console.log('[FileViewer] Fetching with headers:', Object.keys(headers))

                const controller = new AbortController()
                const timeoutId = setTimeout(() => controller.abort(), 60000) // 60s timeout

                const response = await fetch(fileUrl, {
                    headers,
                    signal: controller.signal
                })
                clearTimeout(timeoutId)

                console.log('[FileViewer] Response status:', response.status, response.statusText, 'isCurrentEffect:', isCurrentEffect, 'aborted:', aborted)

                if (aborted || !isCurrentEffect) {
                    console.log('[FileViewer] Request was aborted or effect is stale')
                    return
                }

                if (!response.ok) {
                    throw new Error(`Failed to fetch file: ${response.statusText}`)
                }

                const contentType = response.headers.get('content-type') || ''
                console.log('[FileViewer] Content-Type:', contentType, 'isCurrentEffect:', isCurrentEffect)

                if (!isCurrentEffect) {
                    console.log('[FileViewer] Effect is stale after response')
                    return
                }

                console.log('[FileViewer] Calling blob()...')

                // Try with arrayBuffer first, then create blob
                const arrayBuffer = await response.arrayBuffer()
                console.log('[FileViewer] ArrayBuffer size:', arrayBuffer.byteLength)

                if (!isCurrentEffect) {
                    console.log('[FileViewer] Effect is stale after blob')
                    return
                }

                console.log('[FileViewer] Creating object URL...')
                const blob = new Blob([arrayBuffer], { type: contentType })
                const url = URL.createObjectURL(blob)
                localObjectUrl = url
                console.log('[FileViewer] Object URL created:', url)

                // Detect file type from content-type or file extension
                let type = ''
                const inputType = fileType?.toLowerCase() || ''
                console.log('[FileViewer] Input fileType:', inputType)

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

                console.log('[FileViewer] Detected type:', type, 'isCurrentEffect:', isCurrentEffect)

                // Only update state if this effect is still current
                if (isCurrentEffect && !aborted) {
                    setBlobUrl(url)
                    setDetectedType(type)
                    setLoading(false)
                    console.log('[FileViewer] State updated, blobUrl:', url)
                } else {
                    URL.revokeObjectURL(url)
                    console.log('[FileViewer] Revoked URL, effect no longer current')
                }
            } catch (err: any) {
                console.error('[FileViewer] Load error:', err, 'isCurrentEffect:', isCurrentEffect)
                if (err.name === 'AbortError') {
                    if (isCurrentEffect) {
                        setError('文件加载超时')
                        setLoading(false)
                    }
                } else if (isCurrentEffect) {
                    setError(err.message || 'Failed to load file')
                    setLoading(false)
                }
            }
        }

        loadFile()

        return () => {
            console.log('[FileViewer] Cleanup, isCurrentEffect:', isCurrentEffect, 'localObjectUrl:', localObjectUrl)
            isCurrentEffect = false
            aborted = true
            if (localObjectUrl) {
                URL.revokeObjectURL(localObjectUrl)
            }
        }
    }, [fileUrl, token]) // fileType and fileName are used inside but not as dependencies

    // Cleanup blob URL on unmount
    useEffect(() => {
        return () => {
            if (blobUrlRef.current) {
                console.log('[FileViewer] Final cleanup on unmount, revoking:', blobUrlRef.current)
                URL.revokeObjectURL(blobUrlRef.current)
            }
        }
    }, [])

    const handleDownload = () => {
        if (blobUrl) {
            const link = document.createElement('a')
            link.href = blobUrl
            link.download = fileName || `document.${detectedType || 'file'}`
            link.click()
        }
    }

    if (loading) {
        console.log('[FileViewer] Rendering loading state')
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
        console.log('[FileViewer] Rendering error state:', error)
        return (
            <div className={`file-viewer-wrapper ${className || ''}`} style={style}>
                <div className="p-8 text-center text-red-500 h-full flex items-center justify-center">
                    <p>加载失败: {error}</p>
                </div>
            </div>
        )
    }

    console.log('[FileViewer] Rendering content, detectedType:', detectedType, 'blobUrl:', blobUrl)

    // PDF - Use browser's built-in viewer
    if (detectedType === 'pdf' && blobUrl) {
        console.log('[FileViewer] Rendering PDF iframe with blobUrl:', blobUrl)
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

// Wrap with React.memo to prevent unnecessary re-renders
export const FileViewer = React.memo(FileViewerComponent)

// 保持向后兼容的 OfdViewer 导出
export function OfdViewer(props: FileViewerProps) {
    return <FileViewer {...props} fileType={props.fileType || 'ofd'} />
}
