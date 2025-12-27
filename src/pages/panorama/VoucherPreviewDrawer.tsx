// Input: [voucherId, isOpen, onClose]
// Output: [Slide-over Drawer with Metadata and File Preview]
// Pos: src/pages/panorama/VoucherPreviewDrawer.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { Drawer, Button, Tag, Divider } from 'antd';
import { FileText, X, Maximize2, ExternalLink } from 'lucide-react';
import { VoucherDetailCard } from './VoucherDetailCard';
import { EvidencePreview } from './EvidencePreview';
import { useNavigate } from 'react-router-dom';

interface VoucherPreviewDrawerProps {
    voucherId: string | null;
    open: boolean;
    onClose: () => void;
}

export const VoucherPreviewDrawer: React.FC<VoucherPreviewDrawerProps> = ({ voucherId, open, onClose }) => {
    const navigate = useNavigate();

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
            width="85%"
            extra={
                <div className="flex items-center gap-2">
                    <Button
                        type="text"
                        icon={<Maximize2 size={16} />}
                        onClick={handleFullScreen}
                        className="text-slate-500 hover:text-emerald-600"
                    >
                        全屏查看
                    </Button>
                </div>
            }
            closeIcon={<X size={20} className="text-slate-400" />}
            bodyStyle={{ padding: 0, overflow: 'hidden' }}
        >
            {voucherId ? (
                <div className="flex h-full bg-white">
                    {/* Left: Metadata Details (Minimalist) */}
                    <div className="w-[320px] shrink-0 h-full border-r border-slate-100 bg-white overflow-y-auto">
                        <VoucherDetailCard
                            voucherId={voucherId}
                            sourceType="ORIGINAL"
                            compact={true}
                            hideEntries={true}
                        />
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
