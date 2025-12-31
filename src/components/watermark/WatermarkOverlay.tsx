import React, { useEffect, useRef, useState } from 'react';

export interface WatermarkOverlayProps {
    text: string;
    subText?: string;
    opacity?: number;
    rotate?: number;
}

const WatermarkOverlay: React.FC<WatermarkOverlayProps> = ({
    text,
    subText,
    opacity = 0.15,
    rotate = -30,
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [isTampered, setIsTampered] = useState(false);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const drawWatermark = () => {
            const ctx = canvas.getContext('2d');
            if (!ctx) return;

            const dpr = window.devicePixelRatio || 1;
            const width = window.innerWidth;
            const height = window.innerHeight;

            canvas.width = width * dpr;
            canvas.height = height * dpr;
            canvas.style.width = `${width}px`;
            canvas.style.height = `${height}px`;

            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
            ctx.clearRect(0, 0, width, height);

            const tileWidth = 320;
            const tileHeight = 200;
            const tile = document.createElement('canvas');
            tile.width = tileWidth * dpr;
            tile.height = tileHeight * dpr;

            const tileCtx = tile.getContext('2d');
            if (!tileCtx) return;

            tileCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
            tileCtx.clearRect(0, 0, tileWidth, tileHeight);
            tileCtx.font = '16px sans-serif';
            tileCtx.fillStyle = `rgba(100, 100, 100, ${opacity})`;
            tileCtx.textBaseline = 'middle';
            tileCtx.textAlign = 'center';

            tileCtx.translate(tileWidth / 2, tileHeight / 2);
            tileCtx.rotate((rotate * Math.PI) / 180);
            tileCtx.fillText(text, 0, 0);

            if (subText) {
                tileCtx.font = '12px sans-serif';
                tileCtx.fillText(subText, 0, 26);
            }

            const pattern = ctx.createPattern(tile, 'repeat');
            if (!pattern) return;

            ctx.fillStyle = pattern;
            ctx.fillRect(0, 0, width, height);
        };

        const handleResize = () => {
            drawWatermark();
        };

        drawWatermark();
        window.addEventListener('resize', handleResize);

        const observer = new MutationObserver(() => {
            const container = containerRef.current;
            if (container && !document.body.contains(container)) {
                setIsTampered(true);
                setTimeout(() => {
                    setIsTampered(false);
                    drawWatermark();
                }, 200);
            }
        });

        observer.observe(document.body, { childList: true, subtree: true });
        const refreshTimer = window.setInterval(drawWatermark, 5000);

        return () => {
            window.removeEventListener('resize', handleResize);
            observer.disconnect();
            window.clearInterval(refreshTimer);
        };
    }, [text, subText, opacity, rotate]);

    return (
        <div
            ref={containerRef}
            data-testid="watermark-overlay"
            data-watermark-text={text}
            data-watermark-subtext={subText ?? ''}
            data-watermark-opacity={opacity}
            data-watermark-rotate={rotate}
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                pointerEvents: isTampered ? 'auto' : 'none',
                zIndex: 9999,
                overflow: 'hidden',
                backgroundColor: isTampered ? '#ffffff' : 'transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#111111',
            }}
        >
            {isTampered ? (
                <div style={{ textAlign: 'center' }}>
                    <h2>安全警告</h2>
                    <p>检测到水印被移除，已锁定当前页面</p>
                </div>
            ) : (
                <canvas ref={canvasRef} />
            )}
        </div>
    );
};

export default WatermarkOverlay;
