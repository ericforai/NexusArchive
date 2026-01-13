// Input: React、SVG
// Output: 简单关系图谱组件
// Pos: 关系图谱简单实现

import React, { useMemo, useState, useRef, useCallback } from 'react';
import {
  FileText,
  Receipt,
  FileSpreadsheet,
  Building,
  CreditCard,
  Loader2,
  AlertTriangle,
  ZoomIn,
  ZoomOut,
  Maximize
} from 'lucide-react';
import type { RelationNodeData } from '@/types/relationGraph';
import { ARCHIVE_TYPE_STYLES } from '@/types/relationGraph';
import { useRelationGraphStore } from '@/store/useRelationGraphStore';

// 图标映射
const iconMap = {
  Building,
  Receipt,
  FileText,
  CreditCard,
  FileSpreadsheet
};

interface SimpleGraphViewProps {
  onNodeClick?: (nodeId: string, nodeData: RelationNodeData) => void;
}

/**
 * 简单关系图谱 - 纯 CSS + SVG 实现
 */
export const SimpleGraphView: React.FC<SimpleGraphViewProps> = ({ onNodeClick }) => {
  const nodes = useRelationGraphStore(s => s.nodes);
  const edges = useRelationGraphStore(s => s.edges);
  const isInitialLoading = useRelationGraphStore(s => s.isInitialLoading);

  // 缩放状态
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const isDragging = useRef(false);
  const dragStart = useRef({ x: 0, y: 0 });

  // 缩放控制
  const handleZoomIn = useCallback(() => setZoom(z => Math.min(z + 0.2, 3)), []);
  const handleZoomOut = useCallback(() => setZoom(z => Math.max(z - 0.2, 0.3)), []);
  const handleReset = useCallback(() => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }, []);

  // 拖拽
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('.cursor-pointer')) return;
    isDragging.current = true;
    dragStart.current = { x: e.clientX - pan.x, y: e.clientY - pan.y };
  }, [pan]);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!isDragging.current) return;
    setPan({
      x: e.clientX - dragStart.current.x,
      y: e.clientY - dragStart.current.y
    });
  }, []);

  const handleMouseUp = useCallback(() => {
    isDragging.current = false;
  }, []);

  // 滚轮缩放
  const handleWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setZoom(z => Math.max(0.3, Math.min(3, z + delta)));
  }, []);

  // 计算画布尺寸
  const canvasSize = useMemo(() => {
    if (nodes.length === 0) return { width: 800, height: 600 };

    let minX = 0, maxX = 0, minY = 0, maxY = 0;
    nodes.forEach(n => {
      minX = Math.min(minX, n.position.x);
      maxX = Math.max(maxX, n.position.x + 200);
      minY = Math.min(minY, n.position.y);
      maxY = Math.max(maxY, n.position.y + 140);
    });

    return {
      width: Math.max(800, maxX - minX + 400),
      height: Math.max(600, maxY - minY + 300)
    };
  }, [nodes]);

  // 中心节点
  const centerNode = nodes.find(n => n.data?.isCenter) || nodes[0];

  // 计算居中偏移量（所有节点共用）
  const offsetX = canvasSize.width / 2 - (centerNode?.position.x || 0) - 100;
  const offsetY = canvasSize.height / 2 - (centerNode?.position.y || 0) - 70;

  if (isInitialLoading) {
    return (
      <div className="w-full h-full flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-slate-400" />
      </div>
    );
  }

  if (nodes.length === 0) {
    return (
      <div className="w-full h-full flex items-center justify-center text-slate-400">
        <div className="text-center">
          <FileText size={48} className="mx-auto mb-4 opacity-50" />
          <p className="text-sm">请输入档号查询关系数据</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="w-full h-full overflow-hidden bg-slate-50 relative"
      style={{
        backgroundImage: 'radial-gradient(#cbd5e1 1px, transparent 1px)',
        backgroundSize: '20px 20px',
        minHeight: '500px'
      }}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onWheel={handleWheel}
    >
      {/* 缩放控制 */}
      <div className="absolute top-4 right-4 z-20 flex flex-col gap-2">
        <button onClick={handleZoomIn} className="p-2 bg-white border border-slate-200 rounded-lg shadow-md hover:bg-slate-50" title="放大">
          <ZoomIn size={18} />
        </button>
        <button onClick={handleZoomOut} className="p-2 bg-white border border-slate-200 rounded-lg shadow-md hover:bg-slate-50" title="缩小">
          <ZoomOut size={18} />
        </button>
        <button onClick={handleReset} className="p-2 bg-white border border-slate-200 rounded-lg shadow-md hover:bg-slate-50" title="重置">
          <Maximize size={18} />
        </button>
        <div className="px-2 py-1 bg-white border border-slate-200 rounded-lg text-xs text-center">
          {Math.round(zoom * 100)}%
        </div>
      </div>

      {/* 图谱容器 */}
      <div className="absolute inset-0 flex items-center justify-center overflow-auto">
        <div
          className="relative"
          style={{
            width: canvasSize.width,
            height: canvasSize.height,
            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
            transformOrigin: 'center center',
            transition: 'transform 0.1s ease-out'
          }}
        >
        {/* SVG 连线层 */}
        <svg
          className="absolute inset-0 pointer-events-none"
          style={{
            width: canvasSize.width,
            height: canvasSize.height
          }}
        >
          <defs>
            <marker
              id="arrowhead"
              markerWidth="10"
              markerHeight="7"
              refX="9"
              refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" fill="#94a3b8" />
            </marker>
            <marker
              id="arrowhead-start"
              markerWidth="10"
              markerHeight="7"
              refX="1"
              refY="3.5"
              orient="auto"
            >
              <polygon points="10 0, 0 3.5, 10 7" fill="#94a3b8" />
            </marker>
          </defs>

          {edges.map(edge => {
            const sourceNode = nodes.find(n => n.id === edge.source);
            const targetNode = nodes.find(n => n.id === edge.target);
            if (!sourceNode || !targetNode) return null;

            // 计算连线两端点（节点中心），应用居中偏移
            const sourceX = sourceNode.position.x + offsetX + 100;
            const sourceY = sourceNode.position.y + offsetY + (sourceNode.data?.isCenter ? 70 : 55);
            const targetX = targetNode.position.x + offsetX + 100;
            const targetY = targetNode.position.y + offsetY + (targetNode.data?.isCenter ? 70 : 55);

            return (
              <g key={edge.id}>
                {/* 连线 */}
                <line
                  x1={sourceX}
                  y1={sourceY}
                  x2={targetX}
                  y2={targetY}
                  stroke="#94a3b8"
                  strokeWidth={2}
                  markerEnd="url(#arrowhead)"
                  markerStart="url(#arrowhead-start)"
                />
                {/* 关系标签 */}
                <text
                  x={(sourceX + targetX) / 2}
                  y={(sourceY + targetY) / 2 - 8}
                  textAnchor="middle"
                  className="text-[10px] fill-slate-500"
                  style={{
                    background: 'white',
                    paintOrder: 'stroke'
                  }}
                >
                  {edge.data?.relationType || '关联'}
                </text>
              </g>
            );
          })}
        </svg>

        {/* 节点层 */}
        {nodes.map(node => {
          const data = node.data as RelationNodeData | undefined;
          if (!data) return null;

          const meta = ARCHIVE_TYPE_STYLES[data.type] || ARCHIVE_TYPE_STYLES.other;
          const IconComponent = iconMap[meta.icon as keyof typeof iconMap] || FileText;
          const isCenter = data.isCenter;
          const isLoading = data.isLoading;
          const hasError = !!data.error;

          return (
            <div
              key={node.id}
              onClick={() => onNodeClick?.(node.id, data)}
              className="absolute cursor-pointer hover:scale-105"
              style={{
                left: node.position.x + offsetX,
                top: node.position.y + offsetY,
                width: 200,
                height: isCenter ? 140 : 110
              }}
            >
              <div
                className={`relative rounded-xl shadow-md border-2 ${
                  isCenter ? 'border-amber-400 bg-amber-50' : `bg-white border-slate-200`
                } ${hasError ? 'border-rose-400' : ''}`}
                style={{ width: '100%', height: '100%' }}
              >
                {/* 类型标签 */}
                <div
                  className={`absolute top-2 left-2 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                    isCenter ? 'bg-amber-200 text-amber-800' : `${meta.bg} ${meta.text}`
                  }`}
                >
                  {meta.label}
                </div>

                {/* 中心标记 */}
                {isCenter && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm">
                    核心单据
                  </div>
                )}

                {/* 加载中 */}
                {isLoading && (
                  <div className="absolute inset-0 bg-white/80 flex items-center justify-center rounded-xl z-10">
                    <Loader2 size={24} className="text-primary-500 animate-spin" />
                  </div>
                )}

                {/* 错误 */}
                {hasError && !isLoading && (
                  <div className="absolute top-2 right-2 text-rose-500" title={data.error}>
                    <AlertTriangle size={16} />
                  </div>
                )}

                {/* 内容 */}
                <div className={`p-3 h-full flex flex-col ${isCenter ? 'pt-4' : ''}`}>
                  <div className="flex items-center gap-2 mb-2">
                    <div className={`p-1.5 rounded-lg ${isCenter ? 'bg-amber-200 text-amber-800' : `${meta.bg} ${meta.text}`}`}>
                      <IconComponent size={16} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-bold text-slate-800 truncate" title={data.code}>
                        {data.code || data.id}
                      </div>
                    </div>
                  </div>

                  {data.name && (
                    <div className="text-xs text-slate-500 truncate mb-2" title={data.name}>
                      {data.name}
                    </div>
                  )}

                  <div className="mt-auto flex items-center justify-between text-xs">
                    {data.amount && (
                      <span className="font-mono font-medium text-slate-700">{data.amount}</span>
                    )}
                    {!isCenter && (
                      <div className="text-slate-400">
                        {data.isExpanded ? '▼' : '▶'}
                      </div>
                    )}
                  </div>

                  {isCenter && data.status && (
                    <div className={`mt-2 text-center px-2 py-1 rounded-full text-xs font-medium ${
                      data.status === '已归档' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'
                    }`}>
                      {data.status}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
        </div>
      </div>
    </div>
  );
};
