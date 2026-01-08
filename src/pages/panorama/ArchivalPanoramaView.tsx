// Input: React、lucide-react 图标、本地模块 panorama/ArchiveStructureTree、panorama/VoucherDetailCard、panorama/EvidencePreview 等
// Output: React 组件 ArchivalPanoramaView
// Pos: src/pages/panorama/ArchivalPanoramaView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { ArchiveStructureTree } from './ArchiveStructureTree';
import { VoucherDetailCard } from './VoucherDetailCard';
import { EvidencePreview } from './EvidencePreview';
import { VoucherPlayer } from './VoucherPlayer';
import { VoucherPreview } from '../../components/voucher';
import { Maximize2, FileText, Receipt, CloudDownload } from 'lucide-react';
import { Collapse, Button, List, Tag, Space, message } from 'antd';
import { yonsuiteApi, type VoucherAttachment } from '../../api/yonsuite';

interface ArchivalPanoramaViewProps {
    initialVoucherId?: string;
}

import { archivesApi } from '../../api/archives';
import { originalVoucherApi } from '../../api/originalVoucher';
import { VoucherDTO } from '../../components/voucher';

// ============ VoucherPreview Wrapper Component ============

interface VoucherPreviewWrapperProps {
    voucherId: string;
    sourceType: 'ARCHIVE' | 'ORIGINAL' | null;
}

const VoucherPreviewWrapper: React.FC<VoucherPreviewWrapperProps> = ({ voucherId, sourceType }) => {
    const [voucherData, setVoucherData] = useState<VoucherDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!voucherId) return;

        const loadData = async () => {
            setLoading(true);
            setError(null);
            try {
                if (sourceType === 'ORIGINAL') {
                    // 原始凭证
                    const ov = await originalVoucherApi.getOriginalVoucher(voucherId);
                    if (ov) {
                        const ovData = ov as any; // Use type assertion to access optional properties
                        setVoucherData({
                            voucherId: ov.id,
                            voucherNo: ov.voucherNo || '',
                            voucherWord: '记',
                            voucherDate: ov.businessDate || '',
                            orgName: ovData.orgName || '',
                            summary: ov.summary || '',
                            debitTotal: ov.amount || 0,
                            creditTotal: ov.amount || 0,
                            creator: ov.creator || ovData.uploadedBy || '',
                            entries: [],
                        });
                    } else {
                        setError('未找到原始凭证');
                    }
                } else {
                    // 归档档案
                    const res = await archivesApi.getArchiveById(voucherId);
                    if (res.code === 200 && res.data) {
                        const archive = res.data as any;
                        // 解析分录数据
                        let entries: any[] = [];
                        if (archive.customMetadata) {
                            try {
                                const parsed = JSON.parse(archive.customMetadata);
                                if (Array.isArray(parsed)) {
                                    entries = parsed.map((entry: any, idx: number) => ({
                                        lineNo: idx + 1,
                                        summary: entry.description || entry.summary || '',
                                        accountCode: entry.accsubject?.code || entry.subjectCode || '',
                                        accountName: entry.accsubject?.name || entry.subjectName || '',
                                        debit: entry.debit_org || 0,
                                        credit: entry.credit_org || 0,
                                    }));
                                }
                            } catch (e) {
                                console.warn('Failed to parse customMetadata:', e);
                            }
                        }

                        setVoucherData({
                            voucherId: archive.id,
                            voucherNo: archive.archiveCode || archive.id,
                            voucherWord: '记',
                            voucherDate: archive.docDate || archive.createdTime || '',
                            orgName: archive.orgName || '',
                            summary: archive.title || archive.summary || '',
                            debitTotal: entries.reduce((sum, e) => sum + (e.debit || 0), 0) || archive.amount || 0,
                            creditTotal: entries.reduce((sum, e) => sum + (e.credit || 0), 0) || archive.amount || 0,
                            creator: archive.creator || archive.createdBy || '',
                            auditor: archive.auditor || '',
                            poster: archive.poster || '',
                            entries,
                        });
                    } else if (sourceType === null) {
                        // 尝试原始凭证作为降级
                        const ov = await originalVoucherApi.getOriginalVoucher(voucherId);
                        if (ov) {
                            const ovData = ov as any;
                            setVoucherData({
                                voucherId: ov.id,
                                voucherNo: ov.voucherNo || '',
                                voucherWord: '记',
                                voucherDate: ov.businessDate || '',
                                orgName: ovData.orgName || '',
                                summary: ov.summary || '',
                                debitTotal: ov.amount || 0,
                                creditTotal: ov.amount || 0,
                                creator: ov.creator || '',
                                entries: [],
                            });
                        } else {
                            setError('未找到档案或原始凭证');
                        }
                    } else {
                        setError('未找到该档案');
                    }
                }
            } catch (err: any) {
                console.error('VoucherPreviewWrapper load error:', err);
                setError('加载数据失败');
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [voucherId, sourceType]);

    if (loading) {
        return (
            <div className="h-full flex items-center justify-center text-slate-500">
                <div className="animate-pulse">加载中...</div>
            </div>
        );
    }

    if (error || !voucherData) {
        return (
            <div className="h-full flex items-center justify-center text-slate-400 bg-slate-50/50">
                <p>{error || '暂无数据'}</p>
            </div>
        );
    }

    return <VoucherPreview data={voucherData} layout="vertical" size="normal" />;
};

// ============ Main Component ============

export const ArchivalPanoramaView: React.FC<ArchivalPanoramaViewProps> = ({ initialVoucherId }) => {
    const { id } = useParams<{ id: string }>();
    const [selectedVoucherId, setSelectedVoucherId] = useState<string>(id || initialVoucherId || '');
    const [isPlayerOpen, setIsPlayerOpen] = useState(false);
    const [sourceType, setSourceType] = useState<'ARCHIVE' | 'ORIGINAL' | null>(null);
    const [loading, setLoading] = useState(false);
    const [detailViewMode, setDetailViewMode] = useState<'detail' | 'voucher'>('detail');

    // YonSuite attachments state
    const [yonsuiteAttachments, setYonsuiteAttachments] = useState<VoucherAttachment[]>([]);
    const [attachmentsLoading, setAttachmentsLoading] = useState(false);
    const [attachmentsFetched, setAttachmentsFetched] = useState(false);

    // 当 URL 参数变化时更新选中状态
    useEffect(() => {
        if (id) {
            setSelectedVoucherId(id);
        }
    }, [id]);

    // 探测数据源
    // 探测数据源
    useEffect(() => {
        if (!selectedVoucherId) return;

        console.log('[ArchivalPanoramaView] Detecting source for voucher:', selectedVoucherId);

        const detectSource = async () => {
            setLoading(true);
            try {
                // Try Archive first
                const res = await archivesApi.getArchiveById(selectedVoucherId);
                if (res.code === 200 && res.data) {
                    console.log('[ArchivalPanoramaView] Found in Archive');
                    setSourceType('ARCHIVE');
                    return; // Found, exit early
                }
            } catch (archiveErr) {
                console.warn('[ArchivalPanoramaView] Not found in Archive:', archiveErr);
            }

            try {
                // Try Original Voucher
                const ov = await originalVoucherApi.getOriginalVoucher(selectedVoucherId);
                if (ov) {
                    console.log('[ArchivalPanoramaView] Found in OriginalVoucher');
                    setSourceType('ORIGINAL');
                    return; // Found
                }
            } catch (ovErr) {
                console.warn('[ArchivalPanoramaView] Not found in OriginalVoucher:', ovErr);
            }

            // If we reach here, neither was found
            console.error('[ArchivalPanoramaView] Source not found for id:', selectedVoucherId);
            setSourceType(null);
            setLoading(false);
        };

        detectSource().finally(() => {
            setLoading(false);
        });
    }, [selectedVoucherId]);

    // Fetch YonSuite attachments
    const handleFetchYonsuiteAttachments = useCallback(async () => {
        if (!selectedVoucherId) {
            message.warning('需要凭证ID才能查询附件');
            return;
        }
        setAttachmentsLoading(true);
        try {
            const configId = 1;
            const response = await yonsuiteApi.queryVoucherAttachments(configId, [selectedVoucherId]);
            if (response && response.data && response.data[selectedVoucherId]) {
                setYonsuiteAttachments(response.data[selectedVoucherId]);
                setAttachmentsFetched(true);
                message.success(`查询到 ${response.data[selectedVoucherId].length} 个附件`);
            } else {
                setYonsuiteAttachments([]);
                setAttachmentsFetched(true);
                message.info('该凭证在 YonSuite 中没有附件');
            }
        } catch (error: any) {
            console.error('查询 YonSuite 附件失败:', error);
            message.error(`查询失败: ${error.message || '未知错误'}`);
            setYonsuiteAttachments([]);
            setAttachmentsFetched(true);
        } finally {
            setAttachmentsLoading(false);
        }
    }, [selectedVoucherId]);

    // Reset attachments state when voucher changes
    useEffect(() => {
        setAttachmentsFetched(false);
        setYonsuiteAttachments([]);
    }, [selectedVoucherId]);

    const isOriginal = sourceType === 'ORIGINAL';

    return (
        <div className="h-full flex flex-col bg-white relative">
            {/* Header Area - 专家建议：区分档案全景与凭证预览 */}
            <div className={`h-12 border-b border-slate-200 flex items-center justify-between px-4 transition-colors ${isOriginal ? 'bg-slate-100' : 'bg-emerald-50/50'
                }`}>
                <div className="flex items-center gap-2">
                    <span className={`font-bold ${isOriginal ? 'text-slate-600' : 'text-emerald-700'}`}>
                        {isOriginal ? '原始凭证预览' : '档案全景视图'}
                    </span>
                    {isOriginal && (
                        <span className="px-1.5 py-0.5 bg-slate-200 text-slate-500 rounded text-[10px] font-bold uppercase tracking-wider">
                            Pre-Archive
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setIsPlayerOpen(true)}
                        disabled={!selectedVoucherId || loading}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded text-sm transition-all ${selectedVoucherId && !loading
                            ? 'bg-primary-600 text-white hover:bg-primary-700 shadow-sm active:scale-95'
                            : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                            }`}
                    >
                        <Maximize2 size={14} />
                        <span>进入翻阅模式</span>
                    </button>
                </div>
            </div>

            <div className="flex-1 flex overflow-hidden">
                {/* Left Pane: Archive Structure Tree */}
                <div className="w-64 shrink-0 h-full overflow-hidden">
                    <ArchiveStructureTree onSelectVoucher={setSelectedVoucherId} />
                </div>

                {/* Middle Pane: Voucher Detail Card */}
                <div className="flex-1 min-w-[400px] h-full overflow-hidden border-r border-slate-200 flex flex-col">
                    {/* View Mode Toggle */}
                    <div className="h-10 border-b border-slate-100 flex items-center justify-between px-3 bg-white shrink-0">
                        <span className="text-xs font-medium text-slate-500">凭证详情</span>
                        <div className="flex items-center gap-1 bg-slate-100 rounded-lg p-0.5">
                            <button
                                onClick={() => setDetailViewMode('detail')}
                                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition-all ${detailViewMode === 'detail'
                                    ? 'bg-white text-primary-700 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                                    }`}
                            >
                                <FileText size={12} />
                                <span>详情</span>
                            </button>
                            <button
                                onClick={() => setDetailViewMode('voucher')}
                                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition-all ${detailViewMode === 'voucher'
                                    ? 'bg-white text-primary-700 shadow-sm'
                                    : 'text-slate-500 hover:text-slate-700'
                                    }`}
                            >
                                <Receipt size={12} />
                                <span>凭证预览</span>
                            </button>
                        </div>
                    </div>
                    {/* Content */}
                    <div className="flex-1 overflow-hidden flex flex-col">
                        <div className="flex-1 overflow-auto">
                            {detailViewMode === 'detail' ? (
                                <VoucherDetailCard voucherId={selectedVoucherId} sourceType={sourceType} />
                            ) : (
                                <VoucherPreviewWrapper voucherId={selectedVoucherId} sourceType={sourceType} />
                            )}
                        </div>
                        {/* YonSuite Attachments Panel */}
                        <div className="border-t border-slate-100 bg-white shrink-0">
                            <Collapse
                                size="small"
                                items={[
                                    {
                                        key: 'yonsuite-attachments',
                                        label: (
                                            <Space size={4}>
                                                <CloudDownload size={14} />
                                                <span>YonSuite 附件</span>
                                                {attachmentsFetched && (
                                                    <Tag color={yonsuiteAttachments.length > 0 ? 'success' : 'default'}>
                                                        {yonsuiteAttachments.length}
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
                                                    handleFetchYonsuiteAttachments();
                                                }}
                                            >
                                                查询
                                            </Button>
                                        ) : null,
                                        children: (
                                            <div className="max-h-48 overflow-auto">
                                                {attachmentsLoading ? (
                                                    <div className="flex items-center justify-center h-24 text-slate-400">
                                                        加载中...
                                                    </div>
                                                ) : yonsuiteAttachments.length > 0 ? (
                                                    <List
                                                        size="small"
                                                        dataSource={yonsuiteAttachments}
                                                        renderItem={(item) => (
                                                            <List.Item className="!py-2 !px-0">
                                                                <div className="flex items-center justify-between w-full">
                                                                    <div className="flex items-center gap-2 flex-1 min-w-0">
                                                                        <FileText size={14} className="shrink-0 text-slate-400" />
                                                                        <span className="text-sm text-slate-700 truncate">
                                                                            {item.fileName || item.name || '未命名'}
                                                                        </span>
                                                                    </div>
                                                                    <Tag color="blue" className="shrink-0">
                                                                        {(item.fileSize || 0) > 0
                                                                            ? `${(item.fileSize / 1024).toFixed(1)} KB`
                                                                            : '-'}
                                                                    </Tag>
                                                                </div>
                                                            </List.Item>
                                                        )}
                                                    />
                                                ) : (
                                                    <div className="flex items-center justify-center h-24 text-slate-400 text-sm">
                                                        暂无附件
                                                    </div>
                                                )}
                                            </div>
                                        ),
                                    },
                                ]}
                            />
                        </div>
                    </div>
                </div>

                {/* Right Pane: Evidence Preview */}
                <div className="w-[450px] shrink-0 h-full overflow-hidden bg-slate-50">
                    <EvidencePreview voucherId={selectedVoucherId} sourceType={sourceType} />
                </div>
            </div>

            {/* Full Screen Player Overlay */}
            {isPlayerOpen && selectedVoucherId && (
                <VoucherPlayer
                    initialVoucherId={selectedVoucherId}
                    onClose={() => setIsPlayerOpen(false)}
                    sourceType={sourceType}
                />
            )}
        </div>
    );
};

export default ArchivalPanoramaView;
