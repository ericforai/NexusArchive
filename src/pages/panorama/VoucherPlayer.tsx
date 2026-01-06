// Input: React、lucide-react 图标、本地模块 VoucherDetailCard、EvidencePreview、api/archives
// Output: React 组件 VoucherPlayer
// Pos: src/pages/panorama/VoucherPlayer.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import { ChevronLeft, ChevronRight, X, ShieldCheck } from 'lucide-react';
import { VoucherDetailCard } from './VoucherDetailCard';
import { EvidencePreview } from './EvidencePreview';
import { archivesApi } from '../../api/archives';

interface VoucherPlayerProps {
    initialVoucherId: string;
    onClose: () => void;
    sourceType?: 'ARCHIVE' | 'ORIGINAL' | null;
}

export const VoucherPlayer: React.FC<VoucherPlayerProps> = ({ initialVoucherId, onClose, sourceType }) => {
    const [currentVoucherId, setCurrentVoucherId] = useState(initialVoucherId);
    const [hoveredField, setHoveredField] = useState<string | null>(null);

    const [voucherList, setVoucherList] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Fetch voucher list dynamically (e.g., from archives API)
    useEffect(() => {
        const fetchVouchers = async () => {
            if (sourceType === 'ARCHIVE') {
                setLoading(true);
                setError(null);
                try {
                    const res = await archivesApi.getArchives({ page: 1, limit: 100, categoryCode: 'AC01' });
                    if (res && res.code === 200 && res.data) {
                        const records = (res.data as any).records || [];
                        setVoucherList(records.map((r: any) => r.id));
                    }
                } catch (e) {
                    console.error('Failed to fetch voucher list in Player:', e);
                    setError('无法获取凭证列表');
                } finally {
                    setLoading(false);
                }
            } else {
                // For ORIGINAL or other modes, use a fallback list or just the initial ID
                setVoucherList([initialVoucherId]);
            }
        };

        fetchVouchers();
    }, [initialVoucherId, sourceType]);

    const currentIndex = voucherList.indexOf(currentVoucherId);

    const handlePrev = useCallback(() => {
        if (currentIndex > 0) {
            setCurrentVoucherId(voucherList[currentIndex - 1]);
        }
    }, [currentIndex, voucherList]);

    const handleNext = useCallback(() => {
        if (currentIndex < voucherList.length - 1) {
            setCurrentVoucherId(voucherList[currentIndex + 1]);
        }
    }, [currentIndex, voucherList]);

    // Keyboard navigation
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'ArrowLeft') handlePrev();
            if (e.key === 'ArrowRight') handleNext();
            if (e.key === 'Escape') onClose();
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [handlePrev, handleNext, onClose]);

    return (
        <div className="fixed inset-0 z-50 bg-slate-900 flex flex-col">
            {/* Top Bar */}
            <div className="h-12 bg-slate-800 flex items-center justify-between px-4 shrink-0 border-b border-slate-700">
                <div className="flex items-center gap-4 text-white">
                    <span className="font-bold text-lg">电子凭证翻阅器</span>
                    <span className="text-slate-400 text-sm">|</span>
                    <span className="text-slate-300 text-sm font-mono">{currentVoucherId}</span>
                </div>
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2 text-slate-400 text-xs">
                        <ShieldCheck size={14} className="text-emerald-500" />
                        <span>已通过四性检测</span>
                    </div>
                    <button onClick={onClose} className="p-1 hover:bg-slate-700 rounded-full text-slate-400 hover:text-white transition-colors">
                        <X size={20} />
                    </button>
                </div>
            </div>

            {/* Main Content Area */}
            <div className="flex-1 flex overflow-hidden relative">
                {/* Watermark Overlay */}
                <div className="absolute inset-0 pointer-events-none z-50 flex items-center justify-center opacity-[0.03] overflow-hidden select-none">
                    <div className="rotate-[-30deg] text-slate-900 text-6xl font-bold whitespace-nowrap">
                        {Array(10).fill('操作员：张三  2025-12-04 10:30:00   ').map((text, i) => (
                            <div key={i} className="mb-32">{text}</div>
                        ))}
                    </div>
                </div>

                {/* Left Panel: Voucher (40%) */}
                <div className="w-[40%] bg-white h-full border-r border-slate-200 relative z-10">
                    <VoucherDetailCard
                        voucherId={currentVoucherId}
                        compact={true}
                        onFieldHover={setHoveredField}
                        activeField={hoveredField}
                        sourceType={sourceType}
                    />
                </div>

                {/* Right Panel: Evidence (60%) */}
                <div className="w-[60%] bg-slate-100 h-full relative z-10">
                    <EvidencePreview
                        voucherId={currentVoucherId}
                        highlightField={hoveredField}
                        onInteract={(field) => setHoveredField(field)}
                        sourceType={sourceType}
                    />
                </div>
            </div>

            {/* Bottom Navigation Bar */}
            <div className="h-14 bg-slate-800 border-t border-slate-700 flex items-center justify-center gap-8 shrink-0 relative z-20">
                <button
                    onClick={handlePrev}
                    disabled={currentIndex <= 0}
                    className={`flex items-center gap-2 px-6 py-2 rounded-full transition-all ${currentIndex <= 0
                        ? 'text-slate-600 cursor-not-allowed'
                        : 'text-white hover:bg-slate-700 active:scale-95'
                        }`}
                >
                    <ChevronLeft size={20} />
                    <span>上一张</span>
                </button>

                <div className="flex flex-col items-center">
                    <span className="text-white font-mono font-bold">{currentIndex + 1} / {voucherList.length}</span>
                    <div className="w-64 h-1 bg-slate-700 rounded-full mt-2 overflow-hidden">
                        <div
                            className="h-full bg-primary-500 transition-all duration-300"
                            style={{ width: `${((currentIndex + 1) / voucherList.length) * 100}%` }}
                        />
                    </div>
                </div>

                <button
                    onClick={handleNext}
                    disabled={currentIndex >= voucherList.length - 1}
                    className={`flex items-center gap-2 px-6 py-2 rounded-full transition-all ${currentIndex >= voucherList.length - 1
                        ? 'text-slate-600 cursor-not-allowed'
                        : 'text-white hover:bg-slate-700 active:scale-95'
                        }`}
                >
                    <span>下一张</span>
                    <ChevronRight size={20} />
                </button>
            </div>
        </div>
    );
};
