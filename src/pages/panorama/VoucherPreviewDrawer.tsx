// Input: [voucherId, isOpen, onClose]（Spin tip 嵌套）
// Output: [Slide-over Drawer with Metadata and File Preview]
// Pos: src/pages/panorama/VoucherPreviewDrawer.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Drawer, Button, Tag, Collapse, List, Space, message, Spin, Empty } from 'antd';
import { FileText, X, Fullscreen, CloudDownload, Link, Download, File } from 'lucide-react';
import { VoucherDetailCard } from './VoucherDetailCard';
import { EvidencePreview } from './EvidencePreview';
import { useNavigate } from 'react-router-dom';
import { yonsuiteApi } from '../../api/yonsuite';
import type { VoucherAttachment } from '../../api/yonsuite';
import { getOriginalVoucherFiles } from '../../api/originalVoucher';

interface VoucherPreviewDrawerProps {
    voucherId: string | null;
    open: boolean;
    onClose: () => void;
}

export const VoucherPreviewDrawer: React.FC<VoucherPreviewDrawerProps> = ({ voucherId, open, onClose }) => {
    const navigate = useNavigate();

    // DEBUG: 在控制台打印版本标记
    React.useEffect(() => {
        console.log('[VoucherPreviewDrawer] Component loaded - VERSION 2025-01-05-12:10');
        if (voucherId) {
            console.log('[VoucherPreviewDrawer] Current voucherId:', voucherId);
        }
    }, [voucherId]);

    // YonSuite 附件相关状态
    const [attachments, setAttachments] = useState<VoucherAttachment[]>([]);
    const [attachmentsLoading, setAttachmentsLoading] = useState(false);
    const [attachmentsFetched, setAttachmentsFetched] = useState(false);

    // 本地文件相关状态
    const [localFiles, setLocalFiles] = useState<any[]>([]);
    const [localFilesLoading, setLocalFilesLoading] = useState(false);
    const [localFilesFetched, setLocalFilesFetched] = useState(false);

    // 查询 YonSuite 附件
    const handleFetchAttachments = async () => {
        if (!voucherId) return;

        setAttachmentsLoading(true);
        try {
            const configId = 1; // TODO: 从系统配置获取
            const response = await yonsuiteApi.queryVoucherAttachments(configId, [voucherId]);

            if (response && response.data && response.data[voucherId]) {
                setAttachments(response.data[voucherId]);
                setAttachmentsFetched(true);
                message.success(`查询到 ${response.data[voucherId].length} 个附件`);
            } else {
                setAttachments([]);
                setAttachmentsFetched(true);
                message.info('该凭证在 YonSuite 中没有附件');
            }
        } catch (error: any) {
            if (import.meta.env.DEV) console.error('查询 YonSuite 附件失败:', error);
            message.error(`查询失败: ${error.message || '未知错误'}`);
            setAttachments([]);
        } finally {
            setAttachmentsLoading(false);
        }
    };

    // 查询本地文件
    const handleFetchLocalFiles = async () => {
        if (!voucherId) return;

        setLocalFilesLoading(true);
        try {
            const files = await getOriginalVoucherFiles(voucherId);
            setLocalFiles(files || []);
            setLocalFilesFetched(true);
            if (files && files.length > 0) {
                message.success(`找到 ${files.length} 个本地文件`);
            }
        } catch (error: any) {
            if (import.meta.env.DEV) console.error('查询本地文件失败:', error);
            message.error(`查询失败: ${error.message || '未知错误'}`);
            setLocalFiles([]);
        } finally {
            setLocalFilesLoading(false);
        }
    };

    // 下载本地文件
    const handleDownloadFile = (file: any) => {
        const downloadUrl = `/api/original-vouchers/files/download/${file.id}`;
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = file.fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        message.success(`开始下载 ${file.fileName}`);
    };

    // 当 voucherId 变化时重置附件状态并自动加载本地文件
    useEffect(() => {
        setAttachments([]);
        setAttachmentsFetched(false);
        setLocalFiles([]);
        setLocalFilesFetched(false);
        // 自动加载本地文件
        if (voucherId) {
            handleFetchLocalFiles();
        }
    }, [voucherId]);

    const handleFullScreen = () => {
        if (voucherId) {
            navigate(`/system/panorama/${voucherId}`);
            onClose();
        }
    };

    return (
        <Drawer
            title={
                <div className="flex items-center justify-between w-full pr-8">
                    <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-600">
                            <FileText size={18} />
                        </div>
                        <div>
                            <span className="text-base font-semibold text-slate-800">原始凭证预览</span>
                            <Tag color="blue" className="ml-2 border-none bg-blue-50 text-blue-600">PRE-ARCHIVE</Tag>
                        </div>
                    </div>
                </div>
            }
            placement="right"
            onClose={onClose}
            open={open}
            size="large"
            extra={
                <div className="flex items-center gap-2">
                    <Button
                        type="text"
                        icon={<Fullscreen size={16} />}
                        onClick={handleFullScreen}
                        className="text-slate-500 hover:text-emerald-600"
                    >
                        全屏查看
                    </Button>
                </div>
            }
            closeIcon={<X size={20} className="text-slate-400" />}
            styles={{ body: { padding: 0, overflow: 'hidden' } }}
        >
            {voucherId ? (
                <div className="flex h-full bg-white">
                    {/* Left: Metadata Details + YonSuite Attachments */}
                    <div className="w-[320px] shrink-0 h-full border-r border-slate-100 bg-white overflow-y-auto">
                        {/* Voucher Detail Card */}
                        <VoucherDetailCard
                            voucherId={voucherId}
                            sourceType="ORIGINAL"
                            compact={true}
                            hideEntries={false}
                        />

                        {/* 本地文件附件 */}
                        <div className="border-t border-slate-100">
                            <Collapse
                                defaultActiveKey={['local-files']}
                                items={[
                                    {
                                        key: 'local-files',
                                        label: (
                                            <Space>
                                                <File size={14} />
                                                <span>关联附件</span>
                                                <Tag color={localFiles.length > 0 ? 'success' : 'default'}>
                                                    {localFiles.length}
                                                </Tag>
                                            </Space>
                                        ),
                                        children: (
                                            <div className="p-2">
                                                {localFilesLoading ? (
                                                    <div className="text-center py-4">
                                                        <Spin size="small" tip="加载中...">
                                                            <div style={{ minHeight: 24 }} />
                                                        </Spin>
                                                    </div>
                                                ) : localFiles.length > 0 ? (
                                                    <List
                                                        size="small"
                                                        dataSource={localFiles}
                                                        renderItem={(file: any) => (
                                                            <List.Item
                                                                className="!px-2 !py-1"
                                                                actions={[
                                                                    <Button
                                                                        key="download"
                                                                        type="text"
                                                                        size="small"
                                                                        icon={<Download size={12} />}
                                                                        onClick={() => handleDownloadFile(file)}
                                                                    >
                                                                        下载
                                                                    </Button>
                                                                ]}
                                                            >
                                                                <List.Item.Meta
                                                                    avatar={<File size={12} className="text-blue-500" />}
                                                                    title={
                                                                        <span className="text-xs truncate max-w-[180px]" title={file.fileName}>
                                                                            {file.fileName}
                                                                        </span>
                                                                    }
                                                                    description={
                                                                        <span className="text-xs text-slate-400">
                                                                            {(file.fileSize / 1024).toFixed(1)} KB · {file.fileType}
                                                                        </span>
                                                                    }
                                                                />
                                                            </List.Item>
                                                        )}
                                                    />
                                                ) : (
                                                    <Empty
                                                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                                                        description="暂无关联附件"
                                                    />
                                                )}
                                            </div>
                                        ),
                                    },
                                ]}
                                bordered={false}
                                size="small"
                            />
                        </div>

                        {/* YonSuite Attachments */}
                        <div className="border-t border-slate-100">
                            <Collapse
                                defaultActiveKey={[]}
                                items={[
                                    {
                                        key: 'yonsuite-attachments',
                                        label: (
                                            <Space>
                                                <CloudDownload size={14} />
                                                <span>YonSuite 附件</span>
                                                {attachmentsFetched && (
                                                    <Tag color={attachments.length ? 'success' : 'default'}>
                                                        {attachments.length}
                                                    </Tag>
                                                )}
                                            </Space>
                                        ),
                                        extra: !attachmentsFetched ? (
                                            <Button
                                                size="small"
                                                type="primary"
                                                icon={<CloudDownload size={12} />}
                                                loading={attachmentsLoading}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleFetchAttachments();
                                                }}
                                            >
                                                查询
                                            </Button>
                                        ) : null,
                                        children: (
                                            <div className="p-2">
                                                {attachmentsLoading ? (
                                                    <div className="text-center py-4">
                                                        <Spin size="small" tip="查询中...">
                                                            <div style={{ minHeight: 24 }} />
                                                        </Spin>
                                                    </div>
                                                ) : !attachmentsFetched ? (
                                                    <Empty
                                                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                                                        description="查询附件"
                                                    />
                                                ) : attachments.length > 0 ? (
                                                    <List
                                                        size="small"
                                                        dataSource={attachments}
                                                        renderItem={(attachment: VoucherAttachment) => (
                                                            <List.Item
                                                                className="!px-2 !py-1"
                                                                actions={[
                                                                    <Button
                                                                        key="download"
                                                                        type="text"
                                                                        size="small"
                                                                        icon={<Download size={12} />}
                                                                        onClick={() => {
                                                                            message.info(`下载 ${attachment.fileName}`);
                                                                            // TODO: 实现下载功能
                                                                        }}
                                                                    />
                                                                ]}
                                                            >
                                                                <List.Item.Meta
                                                                    avatar={<Link size={12} className="text-blue-500" />}
                                                                    title={
                                                                        <span className="text-xs truncate max-w-[180px]" title={attachment.fileName || attachment.name}>
                                                                            {attachment.fileName || attachment.name}
                                                                        </span>
                                                                    }
                                                                    description={
                                                                        <span className="text-xs text-slate-400">
                                                                            {(attachment.fileSize / 1024).toFixed(1)} KB
                                                                        </span>
                                                                    }
                                                                />
                                                            </List.Item>
                                                        )}
                                                    />
                                                ) : (
                                                    <Empty
                                                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                                                        description="暂无附件"
                                                    />
                                                )}
                                            </div>
                                        ),
                                    },
                                ]}
                                bordered={false}
                                size="small"
                            />
                        </div>
                    </div>

                    {/* Right: File Preview (Direct) */}
                    <div className="flex-1 h-full overflow-hidden bg-slate-50/30">
                        <EvidencePreview
                            voucherId={voucherId}
                            sourceType="ORIGINAL"
                            simpleMode={true}
                        />
                    </div>
                </div>
            ) : (
                <div className="h-full flex items-center justify-center text-slate-400">
                    请选择凭证进行预览
                </div>
            )}
        </Drawer>
    );
};
