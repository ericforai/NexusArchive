// Input: React、lucide-react 图标、本地模块 api/attachments、common/OfdViewer
// Output: React 组件 EvidencePreview
// Pos: src/pages/panorama/EvidencePreview.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useCallback } from 'react';
import { FileText, Paperclip, ExternalLink, Download, AlertCircle } from 'lucide-react';
import { attachmentsApi, AttachmentFile } from '../../api/attachments';
import { originalVoucherApi } from '../../api/originalVoucher';
import { useAuthStore } from '../../store';
import { FileViewer } from '../../components/common';

interface EvidencePreviewProps {
    voucherId: string;
    highlightField?: string | null;
    onInteract?: (fieldId: string) => void;
    sourceType?: 'ARCHIVE' | 'ORIGINAL' | null;
    simpleMode?: boolean;
}

const TAB_CONFIG = [
    { id: 'invoice', label: '原始凭证' },
    { id: 'bank_slip', label: '银行回单' },
    { id: 'contract', label: '合同文件' },
    { id: 'other', label: '附件' }
] as const;

type TabType = typeof TAB_CONFIG[number]['id'];

/**
 * 全景视图 - 证据预览组件（只读模式）
 * 
 * 【合规说明】DA/T 94-2022 第6.3条
 * 电子会计档案一经归档，不得进行修改、删除、替换原有内容。
 * 因此已归档档案的附件区只能查看，不能添加/修改。
 * 
 * 附件上传功能应在"电子凭证池"中进行（归档前阶段）。
 */
interface InvoiceOverlayProps {
    highlightField?: string | null;
    onInteract?: (fieldId: string) => void;
    highlightMeta?: string | null; // JSON string from backend
}

interface HighlightMetaData {
    width?: number;
    height?: number;
    regions?: Record<string, { x: number; y: number; w: number; h: number }>;
}

const InvoiceOverlay: React.FC<InvoiceOverlayProps> = ({ highlightField, onInteract, highlightMeta }) => {
    // 只使用后端真实解析的数据，不使用任何 Mock 数据
    // If no highlightMeta from backend, don't render any overlay
    if (!highlightMeta) {
        return null;
    }

    const regions: Record<string, React.CSSProperties> = {};

    try {
        const meta: HighlightMetaData = JSON.parse(highlightMeta);
        if (meta.regions && meta.width && meta.height) {
            const pageWidth = meta.width;
            const pageHeight = meta.height;
            // Convert to percentage-based styles for CSS overlay
            for (const [key, rect] of Object.entries(meta.regions)) {
                regions[key] = {
                    left: `${(rect.x / pageWidth) * 100}%`,
                    top: `${(rect.y / pageHeight) * 100}%`,
                    width: `${(rect.w / pageWidth) * 100}%`,
                    height: `${(rect.h / pageHeight) * 100}%`,
                    position: 'absolute',
                };
            }
        }
    } catch (e) {
        console.warn('Failed to parse highlightMeta', e);
        return null;
    }

    // If no regions parsed, don't render anything
    if (Object.keys(regions).length === 0) {
        return null;
    }

    return (
        <div className="absolute inset-0 z-10 pointer-events-none">
            {Object.entries(regions).map(([key, style]) => {
                const isActive = highlightField === key ||
                    (key.startsWith('tax_') && highlightField?.startsWith('entry_')) ||
                    (key === 'total_amount' && highlightField === 'total_amount');

                return (
                    <div
                        key={key}
                        className={`cursor-pointer transition-all duration-300 pointer-events-auto ${isActive
                            ? 'bg-yellow-400/30 border-2 border-yellow-500 shadow-[0_0_15px_rgba(234,179,8,0.5)]'
                            : 'hover:bg-yellow-400/10 hover:border-2 hover:border-yellow-300'
                            }`}
                        style={style}
                        onClick={() => onInteract?.(key)}
                        title={key === 'total_amount' ? '金额合计' : key.includes('tax') ? '税额' : key}
                    />
                );
            })}
        </div>
    );
};

export const EvidencePreview: React.FC<EvidencePreviewProps> = ({ voucherId, highlightField, onInteract, sourceType, simpleMode = false }) => {
    const [files, setFiles] = useState<AttachmentFile[]>([]);
    const [activeTab, setActiveTab] = useState<TabType>('invoice');
    const [selectedFile, setSelectedFile] = useState<AttachmentFile | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Get auth token for FileViewer (pages can use store)
    const token = useAuthStore(state => state.token);

    // 加载附件列表
    const loadFiles = useCallback(async () => {
        if (!voucherId) return;

        setLoading(true);
        setError(null);
        try {
            if (sourceType === 'ORIGINAL') {
                const ovFiles = await originalVoucherApi.getOriginalVoucherFiles(voucherId);
                if (ovFiles && ovFiles.length > 0) {
                    const converted = ovFiles.map(f => ({
                        id: f.id,
                        fileName: f.fileName,
                        fileType: f.fileType,
                        fileSize: f.fileSize,
                        storagePath: f.storagePath,
                        docType: (f.fileRole === 'PRIMARY' || f.fileRole === 'ORIGINAL') ? 'invoice' :
                            (f.fileName?.includes('回单') ? 'bank_slip' :
                                (f.fileName?.includes('合同') ? 'contract' : 'other')),
                        highlightMeta: (f as any).highlightMeta || null
                    })) as any[];
                    setFiles(converted);
                } else {
                    setFiles([]);
                    setSelectedFile(null);
                }
            } else {
                const attachments = await attachmentsApi.getByArchive(voucherId);
                if (attachments && attachments.length > 0) {
                    setFiles(attachments);
                } else if (sourceType === null) {
                    // 降级尝试
                    const ovFiles = await originalVoucherApi.getOriginalVoucherFiles(voucherId);
                    if (ovFiles && ovFiles.length > 0) {
                        const converted = ovFiles.map(f => ({
                            id: f.id,
                            fileName: f.fileName,
                            fileType: f.fileType,
                            fileSize: f.fileSize,
                            storagePath: f.storagePath,
                            docType: (f.fileRole === 'PRIMARY' || f.fileRole === 'ORIGINAL') ? 'invoice' : 'other'
                        })) as any[];
                        setFiles(converted);
                        setSelectedFile(converted[0] || null);
                    }
                } else {
                    setFiles([]);
                    setSelectedFile(null);
                }
            }
        } catch {
            setError('加载附件失败');
        } finally {
            setLoading(false);
        }
    }, [voucherId, sourceType]);

    useEffect(() => {
        loadFiles();
    }, [loadFiles]);

    // 切换标签时更新选中文件
    useEffect(() => {
        const firstFile = files.find(f => f.docType === activeTab);
        setSelectedFile(firstFile || null);
    }, [activeTab, files]);

    // 构建文件预览 URL
    const getPreviewUrl = (file: AttachmentFile): string => {
        if (!file.id) return '';

        // 根据来源选择不同的下载接口
        if (sourceType === 'ARCHIVE') {
            return `/api/archive/files/download/${file.id}`;
        } else {
            return `/api/original-vouchers/files/download/${file.id}`;
        }
    };

    // 当前页签对应的文件
    const currentTabFiles = simpleMode ? files : files.filter(f => f.docType === activeTab);

    // 渲染空状态
    if (!voucherId) {
        return (
            <div className="h-full flex items-center justify-center text-slate-400 bg-slate-50/50 border-l border-slate-200">
                <div className="text-center">
                    <Paperclip size={48} className="mx-auto mb-4 opacity-20" />
                    <p>选择凭证以查看关联证据</p>
                </div>
            </div>
        );
    }

    return (
        <div className="h-full flex flex-col bg-white border-l border-slate-200">
            {/* 标签页 */}
            {!simpleMode && (
                <div className="flex border-b border-slate-200 bg-slate-50">
                    {TAB_CONFIG.map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`flex-1 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === tab.id
                                ? 'border-primary-500 text-primary-700 bg-white'
                                : 'border-transparent text-slate-500 hover:text-slate-700 hover:bg-slate-100'
                                }`}
                        >
                            {tab.label}
                            {files.filter(f => f.docType === tab.id).length > 0 && (
                                <span className="ml-1 text-xs text-slate-400">
                                    ({files.filter(f => f.docType === tab.id).length})
                                </span>
                            )}
                        </button>
                    ))}
                </div>
            )}

            {/* 工具栏 - 只读模式，已归档档案不可添加附件 */}
            <div className="flex items-center justify-between px-3 py-2 bg-slate-50 border-b border-slate-200">
                <span className="text-xs text-slate-500">
                    关联文件 {files.length} 个
                </span>
                <span className="text-xs text-slate-400 font-medium">
                    {sourceType === 'ARCHIVE' ? '已归档 · 只读' : '原始凭证 · 待归档'}
                </span>
            </div>

            {/* 错误提示 */}
            {error && (
                <div className="px-3 py-2 bg-red-50 border-b border-red-100 flex items-center gap-2 text-sm text-red-600">
                    <AlertCircle size={14} />
                    {error}
                    <button onClick={() => setError(null)} className="ml-auto text-xs underline">关闭</button>
                </div>
            )}

            {/* 文件列表（多个文件时显示） */}
            {currentTabFiles.length > 1 && (
                <div className="p-2 bg-slate-50 border-b border-slate-200 flex gap-2 overflow-x-auto">
                    {currentTabFiles.map(file => (
                        <button
                            key={file.id}
                            onClick={() => setSelectedFile(file)}
                            className={`px-3 py-1.5 rounded text-xs border whitespace-nowrap transition-colors ${selectedFile?.id === file.id
                                ? 'bg-white border-primary-200 text-primary-700 shadow-sm'
                                : 'bg-slate-100 border-transparent text-slate-600 hover:bg-white hover:border-slate-200'
                                }`}
                        >
                            {file.fileName}
                        </button>
                    ))}
                </div>
            )}

            {/* 预览区域 */}
            <div className="flex-1 overflow-hidden relative">
                {loading ? (
                    <div className="h-full flex items-center justify-center text-slate-500">
                        <div className="animate-pulse">加载关联文件中...</div>
                    </div>
                ) : selectedFile ? (
                    <div className="h-full flex flex-col relative">
                        {/* 文件信息头 */}
                        <div className="flex items-center justify-between p-3 bg-slate-100 border-b border-slate-200">
                            <div className="flex items-center gap-2 truncate">
                                <FileText size={16} className="text-slate-500" />
                                <span className="text-sm font-medium text-slate-700 truncate">
                                    {selectedFile.fileName}
                                </span>
                                <span className="text-xs text-slate-400">
                                    ({(selectedFile.fileSize / 1024).toFixed(1)} KB)
                                </span>
                            </div>
                            <div className="flex items-center gap-2">
                                <a
                                    href={getPreviewUrl(selectedFile)}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="p-1.5 hover:bg-white rounded text-slate-500 hover:text-primary-600 transition-colors"
                                    title="新窗口打开"
                                >
                                    <ExternalLink size={16} />
                                </a>
                                <a
                                    href={getPreviewUrl(selectedFile)}
                                    download={selectedFile.fileName}
                                    className="p-1.5 hover:bg-white rounded text-slate-500 hover:text-primary-600 transition-colors"
                                    title="下载"
                                >
                                    <Download size={16} />
                                </a>
                            </div>
                        </div>

                        {/* 真实文件预览 */}
                        <div className="flex-1 bg-slate-200 overflow-hidden relative">
                            {/* Overlay for interaction (Only for invoices in this demo) */}
                            {activeTab === 'invoice' && (
                                <InvoiceOverlay
                                    highlightField={highlightField}
                                    onInteract={onInteract}
                                    highlightMeta={selectedFile.highlightMeta}
                                />
                            )}

                            <FileViewer
                                fileUrl={getPreviewUrl(selectedFile)}
                                fileType={selectedFile.fileType?.toLowerCase()}
                                fileName={selectedFile.fileName}
                                className="h-full"
                                token={token}
                            />
                        </div>
                    </div>
                ) : (
                    <div className="h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50 m-4 rounded-lg border border-dashed border-slate-300">
                        <Paperclip size={48} className="mb-4 opacity-20" />
                        <p>暂无该类型文件</p>
                        <p className="text-xs mt-2 text-slate-400">已归档档案不可添加附件</p>
                    </div>
                )}
            </div>
        </div>
    );
};
