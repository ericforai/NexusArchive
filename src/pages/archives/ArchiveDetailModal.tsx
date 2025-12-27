// Input: 行数据、配置、预览状态、附件列表
// Output: ArchiveDetailModal 组件
// Pos: src/pages/archives/ArchiveDetailModal.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive Detail Modal
 * 
 * 档案详情弹窗，包含业务元数据展示、文件预览、附件列表。
 * 从 ArchiveListView 中抽离，减少主组件 280+ 行。
 */
import React, { useState, useRef } from 'react';
import {
    FileText, X, Package, Loader2, Layers, Eye, Upload, Receipt
} from 'lucide-react';
import { OfdViewer } from '../../components/common/OfdViewer';
import { ModuleConfig, GenericRow } from '../../types';
import { PoolItem } from '../../api/pool';

// ============ 类型定义 ============

interface ArchiveDetailModalProps {
    open: boolean;
    onClose: () => void;
    row: GenericRow | null;
    config: ModuleConfig;
    isPoolView: boolean;

    // 预览相关
    activePreviewId: string | null;
    onPreviewIdChange: (id: string | null) => void;
    mainFileId: string | null;
    relatedFiles: PoolItem[];

    // 附件上传（仅 Pool 模式）
    onUploadAttachment?: (file: File) => Promise<void>;
    isUploading?: boolean;

    // AIP 导出（仅归档模式）
    onAipExport?: (row: GenericRow) => void;
    isExporting?: string | null;

    // 渲染辅助函数
    renderCell: (row: GenericRow, col: any) => React.ReactNode;
    formatStatus: (status?: string) => string;
    resolveDocumentTypeLabel: (type?: string) => string;
    getPreviewUrl: (fileId: string, isPoolMode: boolean) => string;
}

// ============ 组件实现 ============

export const ArchiveDetailModal: React.FC<ArchiveDetailModalProps> = ({
    open,
    onClose,
    row,
    config,
    isPoolView,
    activePreviewId,
    onPreviewIdChange,
    mainFileId,
    relatedFiles,
    onUploadAttachment,
    isUploading = false,
    onAipExport,
    isExporting,
    renderCell,
    formatStatus,
    resolveDocumentTypeLabel,
    getPreviewUrl,
}) => {
    const [activeDetailTab, setActiveDetailTab] = useState<'main' | 'attachments'>('main');
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file || !onUploadAttachment) return;
        await onUploadAttachment(file);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    if (!open || !row) return null;

    const isMainFile = activePreviewId === row.id || activePreviewId === mainFileId;

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[100] flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-6xl h-[85vh] border border-slate-100 flex flex-col mt-10">
                {/* Header */}
                <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl shrink-0">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
                            <FileText size={20} />
                        </div>
                        <div>
                            <h3 className="text-lg font-bold text-slate-800">档案详情</h3>
                            <p className="text-xs text-slate-500 font-mono">{row.code}</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        {!isPoolView && onAipExport && (row.archivalCode || row.code) && (
                            <button
                                onClick={() => onAipExport(row)}
                                disabled={isExporting === (row.archivalCode || row.code)}
                                className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:text-emerald-700 hover:border-emerald-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                            >
                                {isExporting === (row.archivalCode || row.code) ? <Loader2 size={14} className="animate-spin" /> : <Package size={14} />}
                                导出AIP
                            </button>
                        )}
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
                        >
                            <X size={20} />
                        </button>
                    </div>
                </div>

                {/* Content Container */}
                <div className="flex-1 overflow-hidden flex flex-col lg:flex-row bg-slate-50/50">
                    {/* Left Panel: Business Data */}
                    <div className="flex-1 lg:w-3/5 flex flex-col min-w-0 border-r border-slate-200 bg-white">
                        <div className="flex-1 overflow-y-auto p-6">
                            {/* Voucher Header Info */}
                            <div className="mb-6">
                                <div className="flex items-center gap-3 mb-2">
                                    <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                                        <FileText size={24} />
                                    </div>
                                    <div>
                                        <h2 className="text-lg font-bold text-slate-800">{row?.code || '未命名档案'}</h2>
                                        <p className="text-xs text-slate-500 font-mono">ID: {row?.id}</p>
                                    </div>
                                    <div className="ml-auto">
                                        <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${(!row?.status || row.status === 'draft') ? 'bg-slate-100 text-slate-600 border-slate-200' :
                                            row.status === 'archived' ? 'bg-green-50 text-green-700 border-green-200' :
                                                'bg-blue-50 text-blue-700 border-blue-200'
                                            }`}>
                                            {formatStatus(row?.status)}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {/* Metadata Grid */}
                            <div className="bg-slate-50 rounded-xl border border-slate-100 p-5 mb-6">
                                <h3 className="text-sm font-bold text-slate-700 mb-4 flex items-center gap-2">
                                    <Layers size={16} /> 业务元数据
                                </h3>
                                <div className="grid grid-cols-2 gap-y-4 gap-x-8">
                                    {config.columns.filter((col: any) => !['selection', 'actions', 'status'].includes(col.key)).map((col: any) => (
                                        <div key={col.key} className="group">
                                            <label className="text-xs font-medium text-slate-400 mb-1 block group-hover:text-primary-600 transition-colors">
                                                {col.header}
                                            </label>
                                            <div className="text-sm font-medium text-slate-700 min-h-[20px] break-words">
                                                {renderCell(row!, col)}
                                            </div>
                                        </div>
                                    ))}
                                    <div>
                                        <label className="text-xs font-medium text-slate-400 mb-1 block">入池时间</label>
                                        <div className="text-sm font-medium text-slate-700">
                                            {row?.date || '-'}
                                        </div>
                                    </div>
                                    <div>
                                        <label className="text-xs font-medium text-slate-400 mb-1 block">存储ID</label>
                                        <div className="text-xs font-mono text-slate-500 truncate" title={String(row?.fileId || '-')}>
                                            {String(row?.fileId || '-')}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Right Panel: Files & Preview */}
                    <div className="lg:w-2/5 flex flex-col bg-slate-100/50 border-l border-white shadow-inner">
                        {/* Tabs */}
                        <div className="flex items-center px-4 pt-4 gap-2 border-b border-slate-200 bg-white shadow-sm z-10">
                            <button
                                onClick={() => {
                                    setActiveDetailTab('main');
                                    onPreviewIdChange(mainFileId || row?.id || null);
                                }}
                                className={`px-4 py-2.5 text-sm font-medium rounded-t-lg transition-all relative ${activeDetailTab === 'main'
                                    ? 'text-primary-600 bg-white border-x border-t border-slate-200 shadow-[0_-2px_5px_rgba(0,0,0,0.02)] -mb-px hover:text-primary-700'
                                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                                    }`}
                            >
                                {resolveDocumentTypeLabel(row?.type)}
                                {activeDetailTab === 'main' && (
                                    <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-500 rounded-t-full" />
                                )}
                            </button>

                            <button
                                onClick={() => {
                                    setActiveDetailTab('attachments');
                                    if (relatedFiles.length > 0) {
                                        onPreviewIdChange(relatedFiles[0].id);
                                    } else {
                                        onPreviewIdChange(null);
                                    }
                                }}
                                className={`px-4 py-2.5 text-sm font-medium rounded-t-lg transition-all relative flex items-center gap-2 ${activeDetailTab === 'attachments'
                                    ? 'text-primary-600 bg-white border-x border-t border-slate-200 shadow-[0_-2px_5px_rgba(0,0,0,0.02)] -mb-px hover:text-primary-700'
                                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'
                                    }`}
                            >
                                关联附件
                                {relatedFiles.length > 0 && (
                                    <span className={`px-1.5 py-0.5 text-[10px] rounded-full ${activeDetailTab === 'attachments' ? 'bg-primary-100 text-primary-700' : 'bg-slate-200 text-slate-600'
                                        }`}>
                                        {relatedFiles.length}
                                    </span>
                                )}
                                {activeDetailTab === 'attachments' && (
                                    <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-500 rounded-t-full" />
                                )}
                            </button>
                        </div>

                        {/* Attachment List (Visible only when 'attachments' tab is active) */}
                        {activeDetailTab === 'attachments' && (
                            <div className="max-h-[200px] overflow-y-auto bg-white border-b border-slate-200 p-2 space-y-1 shadow-sm relative z-10">
                                {isPoolView && onUploadAttachment && (
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-xs text-slate-500">共 {relatedFiles.length} 个附件</span>
                                        <div>
                                            <input
                                                type="file"
                                                ref={fileInputRef}
                                                onChange={handleUpload}
                                                className="hidden"
                                                accept=".pdf,.ofd,.jpg,.jpeg,.png"
                                            />
                                            <button
                                                onClick={() => fileInputRef.current?.click()}
                                                disabled={isUploading}
                                                className={`px-3 py-1.5 text-xs bg-primary-50 text-primary-600 border border-primary-200 rounded hover:bg-primary-100 flex items-center gap-1 ${isUploading ? 'opacity-50 cursor-not-allowed' : ''}`}
                                            >
                                                {isUploading ? <Loader2 size={12} className="animate-spin" /> : <Upload size={12} />}
                                                {isUploading ? '上传中...' : '添加附件'}
                                            </button>
                                        </div>
                                    </div>
                                )}
                                {relatedFiles.length === 0 ? (
                                    <div className="text-center py-8 text-slate-400 text-xs">
                                        <p>暂无关联附件</p>
                                        {!isPoolView && (
                                            <p className="mt-2 text-slate-400">已归档档案不可添加附件</p>
                                        )}
                                    </div>
                                ) : (
                                    relatedFiles.map((file, idx) => (
                                        <div
                                            key={file.id || idx}
                                            onClick={() => onPreviewIdChange(file.id)}
                                            className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all border ${activePreviewId === file.id
                                                ? 'bg-blue-50 border-blue-200 shadow-sm'
                                                : 'bg-white border-slate-100 hover:border-blue-200 hover:shadow-sm'
                                                }`}
                                        >
                                            <div className={`p-2 rounded-lg ${activePreviewId === file.id ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-500'
                                                }`}>
                                                {file.fileName?.endsWith('.pdf') ? <FileText size={18} /> :
                                                    file.fileName?.match(/\.(jpg|jpeg|png)$/i) ? <Receipt size={18} /> : <FileText size={18} />}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className={`text-sm font-medium truncate ${activePreviewId === file.id ? 'text-blue-700' : 'text-slate-700'
                                                    }`}>
                                                    {file.fileName || '未知文件'}
                                                </div>
                                                <div className="text-xs text-slate-400 mt-0.5 flex items-center gap-2">
                                                    <span>{file.type || '附件'}</span>
                                                    {activePreviewId === file.id && (
                                                        <span className="ml-auto text-blue-500 flex items-center gap-1">
                                                            <Eye size={12} /> 预览中
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        )}

                        {/* Preview Area */}
                        <div className="flex-1 bg-slate-200 overflow-hidden relative">
                            {activePreviewId ? (
                                (() => {
                                    // 若在附件标签页下，且当前预览的是主文件，则显示提示
                                    if (activeDetailTab === 'attachments' && isMainFile) {
                                        return (
                                            <div className="absolute inset-0 flex items-center justify-center text-slate-400 bg-slate-100 p-6 text-center">
                                                <div>
                                                    <FileText size={48} className="mx-auto mb-4 opacity-20" />
                                                    <p className="max-w-[200px]">请从上方附件列表中选择一个文件进行预览</p>
                                                </div>
                                            </div>
                                        );
                                    }

                                    let fileName = '';
                                    if (isMainFile) {
                                        fileName = row?.title || (row?.code ? row.code + '.pdf' : 'unknown.pdf');
                                    } else {
                                        const att = relatedFiles.find(f => f.id === activePreviewId);
                                        fileName = att?.fileName || '';
                                    }

                                    if (!fileName) return null;

                                    const isImage = fileName.match(/\.(jpg|jpeg|png|gif|bmp|webp)$/i);
                                    if (isImage) {
                                        return (
                                            <div className="w-full h-full flex items-center justify-center bg-slate-800 overflow-auto p-4">
                                                <img
                                                    src={getPreviewUrl(activePreviewId, isPoolView)}
                                                    alt="Preview"
                                                    className="max-w-full max-h-full object-contain shadow-2xl"
                                                />
                                            </div>
                                        );
                                    }

                                    const type = fileName.toLowerCase().endsWith('.ofd') ? 'ofd' : 'pdf';
                                    return (
                                        <OfdViewer
                                            fileUrl={getPreviewUrl(activePreviewId, isPoolView)}
                                            fileName={fileName}
                                            fileType={type}
                                            className="w-full h-full"
                                        />
                                    );
                                })()
                            ) : (
                                <div className="absolute inset-0 flex items-center justify-center text-slate-400 bg-slate-100">
                                    <div className="text-center">
                                        <FileText size={48} className="mx-auto mb-4 opacity-20" />
                                        <p>{relatedFiles.length === 0 ? '暂无关联文件' : '请选择文件预览'}</p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ArchiveDetailModal;
