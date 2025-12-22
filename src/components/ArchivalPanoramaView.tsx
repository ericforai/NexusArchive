// Input: React、lucide-react 图标、本地模块 panorama/ArchiveStructureTree、panorama/VoucherDetailCard、panorama/EvidencePreview 等
// Output: React 组件 ArchivalPanoramaView
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { ArchiveStructureTree } from './panorama/ArchiveStructureTree';
import { VoucherDetailCard } from './panorama/VoucherDetailCard';
import { EvidencePreview } from './panorama/EvidencePreview';
import { VoucherPlayer } from './panorama/VoucherPlayer';
import { Maximize2 } from 'lucide-react';

interface ArchivalPanoramaViewProps {
    initialVoucherId?: string;
}

export const ArchivalPanoramaView: React.FC<ArchivalPanoramaViewProps> = ({ initialVoucherId }) => {
    const [selectedVoucherId, setSelectedVoucherId] = useState<string>(initialVoucherId || '');
    const [isPlayerOpen, setIsPlayerOpen] = useState(false);

    return (
        <div className="h-full flex flex-col bg-white relative">
            {/* Header Area */}
            <div className="h-12 border-b border-slate-200 flex items-center justify-between px-4 bg-slate-50">
                <span className="font-bold text-slate-700">档案全景视图</span>
                <div className="flex items-center gap-2">
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

