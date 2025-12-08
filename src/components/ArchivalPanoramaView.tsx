import React, { useState } from 'react';
import { ArchiveStructureTree } from './panorama/ArchiveStructureTree';
import { VoucherDetailCard } from './panorama/VoucherDetailCard';
import { EvidencePreview } from './panorama/EvidencePreview';
import { VoucherPlayer } from './panorama/VoucherPlayer';
import { Maximize2 } from 'lucide-react';
import { isDemoMode } from '../utils/env';
import { safeStorage } from '../utils/storage';
import { DemoBadge } from './common/DemoBadge';

interface ArchivalPanoramaViewProps {
    initialVoucherId?: string;
}

export const ArchivalPanoramaView: React.FC<ArchivalPanoramaViewProps> = ({ initialVoucherId }) => {
    const [selectedVoucherId, setSelectedVoucherId] = useState<string>(initialVoucherId || '');
    const [isPlayerOpen, setIsPlayerOpen] = useState(false);
    const [demoMode, setDemoMode] = useState(isDemoMode());

    const toggleDemoMode = (flag: boolean) => {
        safeStorage.setItem('demoMode', flag ? 'true' : 'false');
        setDemoMode(flag);
        window.location.reload();
    };

    return (
        <div className="h-full flex flex-col bg-white relative">
            {/* Header Area */}
            <div className="h-12 border-b border-slate-200 flex items-center justify-between px-4 bg-slate-50">
                <span className="font-bold text-slate-700">档案全景视图</span>
                <div className="flex items-center gap-2">
                    {demoMode && <span className="text-[11px] text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded">演示模式</span>}
                    <button
                        onClick={() => toggleDemoMode(!demoMode)}
                        className="px-2 py-1 text-xs rounded border border-slate-200 text-slate-600 hover:bg-slate-100"
                    >
                        {demoMode ? '关闭演示' : '开启演示'}
                    </button>
                    <button
                        onClick={() => setIsPlayerOpen(true)}
                        disabled={!selectedVoucherId}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded text-sm transition-colors ${selectedVoucherId
                            ? 'bg-primary-600 text-white hover:bg-primary-700 shadow-sm'
                            : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                            }`}
                    >
                        <Maximize2 size={14} />
                        <span>进入翻阅模式</span>
                    </button>
                </div>
            </div>

            <div className="flex-1 flex overflow-hidden">
                {demoMode && <div className="absolute top-14 left-4 right-4"><DemoBadge text="全景视图当前为演示数据，接入真实目录与凭证后可关闭演示模式。" /></div>}
                {/* Left Pane: Archive Structure Tree */}
                <div className="w-64 shrink-0 h-full overflow-hidden">
                    <ArchiveStructureTree onSelectVoucher={setSelectedVoucherId} />
                </div>

                {/* Middle Pane: Voucher Detail Card */}
                <div className="flex-1 min-w-[400px] h-full overflow-hidden border-r border-slate-200">
                    <VoucherDetailCard voucherId={selectedVoucherId} />
                </div>

                {/* Right Pane: Evidence Preview */}
                <div className="w-[450px] shrink-0 h-full overflow-hidden bg-slate-50">
                    <EvidencePreview voucherId={selectedVoucherId} />
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
