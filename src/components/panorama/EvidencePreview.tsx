// Input: React、lucide-react 图标、本地模块 api/attachments、common/OfdViewer
// Output: React 组件 EvidencePreview
// Pos: 归档全景子组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { FileText, Paperclip, ExternalLink, Download, ZoomIn, ZoomOut, RotateCcw, AlertCircle } from 'lucide-react';
import { attachmentsApi, AttachmentFile } from '../../api/attachments';
import { FileViewer } from '../common/OfdViewer';

interface EvidencePreviewProps {
    voucherId: string;
    highlightField?: string | null;
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
export const EvidencePreview: React.FC<EvidencePreviewProps> = ({ voucherId, highlightField }) => {
    const [files, setFiles] = useState<AttachmentFile[]>([]);
    const [activeTab, setActiveTab] = useState<TabType>('invoice');
    const [selectedFile, setSelectedFile] = useState<AttachmentFile | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // 加载附件列表
    const loadFiles = async () => {
        if (!voucherId) return;

        setLoading(true);
        setError(null);
        try {
            const attachments = await attachmentsApi.getByArchive(voucherId);
            setFiles(attachments);

            // 自动选择当前标签页的第一个文件
            const firstFile = attachments.find(f => f.docType === activeTab);
            setSelectedFile(firstFile || attachments[0] || null);
        } catch (err: any) {
            console.error('Failed to fetch attachments', err);
            setError('加载附件失败');
            setFiles([]);
            setSelectedFile(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadFiles();
    }, [voucherId]);

    // 切换标签时更新选中文件
    useEffect(() => {
        const firstFile = files.find(f => f.docType === activeTab);
        setSelectedFile(firstFile || null);
    }, [activeTab, files]);

    // 构建文件预览 URL
    const getPreviewUrl = (file: AttachmentFile): string => {
        // 使用后端文件下载接口
        if (file.id) {
            return `/api/archive/files/download/${file.id}`;
        }
        return '';
    };

    // 获取当前标签页的文件列表
    const currentTabFiles = files.filter(f => f.docType === activeTab);

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

            {/* 工具栏 - 只读模式，已归档档案不可添加附件 */}
            <div className="flex items-center justify-between px-3 py-2 bg-slate-50 border-b border-slate-200">
                <span className="text-xs text-slate-500">
                    关联文件 {files.length} 个
                </span>
                <span className="text-xs text-slate-400">
                    已归档 · 只读
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
            <div className="flex-1 overflow-hidden">
                {loading ? (
                    <div className="h-full flex items-center justify-center text-slate-500">
                        <div className="animate-pulse">加载关联文件中...</div>
                    </div>
                ) : selectedFile ? (
                    <div className="h-full flex flex-col">
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
                        <div className="flex-1 bg-slate-200 overflow-hidden">
                            <FileViewer
                                fileUrl={getPreviewUrl(selectedFile)}
                                fileType={selectedFile.fileType?.toLowerCase()}
                                fileName={selectedFile.fileName}
                                className="h-full"
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
