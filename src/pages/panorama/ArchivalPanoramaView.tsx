// Input: React、lucide-react 图标、本地模块 panorama/ArchiveStructureTree、panorama/VoucherDetailCard、panorama/EvidencePreview 等
// Output: React 组件 ArchivalPanoramaView
// Pos: src/pages/panorama/ArchivalPanoramaView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ArchiveStructureTree } from './ArchiveStructureTree';
import { VoucherDetailCard } from './VoucherDetailCard';
import { EvidencePreview } from './EvidencePreview';
import { VoucherPlayer } from './VoucherPlayer';
import { Maximize2 } from 'lucide-react';

interface ArchivalPanoramaViewProps {
    initialVoucherId?: string;
}

import { archivesApi, Archive } from '../../api/archives';
import { originalVoucherApi, OriginalVoucher } from '../../api/originalVoucher';

export const ArchivalPanoramaView: React.FC<ArchivalPanoramaViewProps> = ({ initialVoucherId }) => {
    const { id } = useParams<{ id: string }>();
    const [selectedVoucherId, setSelectedVoucherId] = useState<string>(id || initialVoucherId || '');
    const [isPlayerOpen, setIsPlayerOpen] = useState(false);
    const [sourceType, setSourceType] = useState<'ARCHIVE' | 'ORIGINAL' | null>(null);
    const [loading, setLoading] = useState(false);

    // 当 URL 参数变化时更新选中状态
    useEffect(() => {
        if (id) {
            setSelectedVoucherId(id);
        }
    }, [id]);

    // 探测数据源
    useEffect(() => {
        if (!selectedVoucherId) return;

        const detectSource = async () => {
            setLoading(true);
            try {
                const res = await archivesApi.getArchiveById(selectedVoucherId);
                if (res.code === 200 && res.data) {
                    setSourceType('ARCHIVE');
                } else {
                    const ov = await originalVoucherApi.getOriginalVoucher(selectedVoucherId);
                    if (ov) {
                        setSourceType('ORIGINAL');
                    }
                }
            } catch {
                try {
                    const ov = await originalVoucherApi.getOriginalVoucher(selectedVoucherId);
                    if (ov) setSourceType('ORIGINAL');
                } catch {
                    setSourceType(null);
                }
            } finally {
                setLoading(false);
            }
        };

        detectSource();
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
                <div className="flex-1 min-w-[400px] h-full overflow-hidden border-r border-slate-200">
                    <VoucherDetailCard voucherId={selectedVoucherId} sourceType={sourceType} />
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
                />
            )}
        </div>
    );
};

export default ArchivalPanoramaView;
