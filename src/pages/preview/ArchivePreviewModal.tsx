import React, { useEffect, useState } from 'react';
import { Modal, Spin, Alert, Button } from 'antd';
import { previewApi } from '../../api/preview';
import WatermarkOverlay from '../../components/watermark/WatermarkOverlay';
import { useAuthStore, useFondsStore } from '../../store';

interface ArchivePreviewModalProps {
    visible: boolean;
    onCancel: () => void;
    archiveId: string;
    fileId?: string;
    fileName?: string;
}

const ArchivePreviewModal: React.FC<ArchivePreviewModalProps> = ({
    visible,
    onCancel,
    archiveId,
    fileId,
    fileName = 'Preview',
}) => {
    const [loading, setLoading] = useState(false);
    const [blobUrl, setBlobUrl] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [watermark, setWatermark] = useState<{
        text?: string;
        subText?: string;
        opacity?: number;
        rotate?: number;
        traceId?: string;
    } | null>(null);
    const [watermarkTimestamp, setWatermarkTimestamp] = useState<string | null>(null);
    const { user } = useAuthStore();
    const { currentFonds } = useFondsStore();

    useEffect(() => {
        if (visible && archiveId) {
            loadPreview();
        }
        return () => {
            // Cleanup blob URL
            if (blobUrl) {
                URL.revokeObjectURL(blobUrl);
            }
        };
    }, [visible, archiveId, fileId]);

    const loadPreview = async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await previewApi.getPreview({
                archiveId,
                fileId,
                mode: 'stream', // Default to stream (client-side rendering via browser)
            });
            if (result.mode === 'presigned') {
                setError('当前预览模式为预签名URL，请切换为流式预览。');
                return;
            }
            const url = URL.createObjectURL(result.blob);
            setBlobUrl(url);
            setWatermark({
                text: result.watermark?.text,
                subText: result.watermark?.subText,
                opacity: result.watermark?.opacity,
                rotate: result.watermark?.rotate,
                traceId: result.traceId,
            });
            setWatermarkTimestamp(new Date().toLocaleString());
        } catch (err) {
            console.error('Preview failed', err);
            setError('Failed to load preview.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Modal
            title={`预览: ${fileName}`}
            open={visible}
            onCancel={onCancel}
            width="90%"
            style={{ top: 20 }}
            footer={[
                <Button key="close" onClick={onCancel}>
                    关闭
                </Button>,
            ]}
            bodyStyle={{ height: '80vh', padding: 0 }}
            destroyOnClose
        >
            <div className="relative w-full h-full" style={{ position: 'relative', height: '100%' }}>
                {loading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10"
                        style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(255,255,255,0.8)', zIndex: 10 }}>
                        <Spin size="large" tip="Loading..." />
                    </div>
                )}

                {error && (
                    <div className="p-4" style={{ padding: '20px' }}>
                        <Alert message="Error" description={error} type="error" showIcon />
                    </div>
                )}

                {blobUrl && !error && (
                    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
                        <iframe
                            src={blobUrl}
                            className="w-full h-full border-none"
                            style={{ width: '100%', height: '100%', border: 'none' }}
                            title="Preview"
                        />
                        {/* Frontend Watermark Overlay */}
                        <WatermarkOverlay
                            text={
                                watermark?.text ||
                                `${user?.realName || user?.fullName || user?.username || 'User'} ${watermarkTimestamp || ''} ${watermark?.traceId || archiveId.substring(0, 8)}`
                            }
                            subText={
                                watermark?.subText ||
                                `Trace:${watermark?.traceId || archiveId.substring(0, 8)}${currentFonds?.fondsCode ? ` | Fonds:${currentFonds.fondsCode}` : ''}`
                            }
                            opacity={watermark?.opacity}
                            rotate={watermark?.rotate}
                        />
                    </div>
                )}
            </div>
        </Modal>
    );
};

export default ArchivePreviewModal;
