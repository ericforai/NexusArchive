// Input: React、RelationNodeData、RelationEdgeData
// Output: ThreeColumnLayout 组件（三栏布局）
// Pos: 关系图谱三栏布局组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo, useState, useRef, useEffect } from 'react';
import { ArchiveCard } from './ArchiveCard';
import type { RelationNodeData, RelationEdgeData, RelationType } from '@/types/relationGraph';

interface ThreeColumnLayoutProps {
  /** 中心节点 */
  centerNode: RelationNodeData;
  /** 所有节点 */
  nodes: RelationNodeData[];
  /** 所有关系 */
  relations: RelationEdgeData[];
  /** 节点点击回调 */
  onNodeClick?: (nodeId: string, nodeData: RelationNodeData) => void;
  /** 需要高亮的档案ID（原始查询档案） */
  highlightedArchiveId?: string | null;
}

/**
 * 三栏布局组件
 * 左侧：上游数据，中心：核心单据，右侧：下游数据
 */
export const ThreeColumnLayout: React.FC<ThreeColumnLayoutProps> = ({
  centerNode,
  nodes,
  relations,
  onNodeClick,
  highlightedArchiveId = null
}) => {
  const [highlightedNodeId, setHighlightedNodeId] = useState<string | null>(null);
  
  // Refs for position calculation
  const containerRef = useRef<HTMLDivElement>(null);
  const centerCardRef = useRef<HTMLDivElement>(null);
  const upstreamCardRefs = useRef<Map<string, HTMLDivElement>>(new Map());
  const downstreamCardRefs = useRef<Map<string, HTMLDivElement>>(new Map());
  const [connectionLines, setConnectionLines] = useState<Array<{
    id: string;
    x1: number;
    y1: number;
    x2: number;
    y2: number;
    color: string;
    dasharray?: string;
  }>>([]);

  // 关系类型颜色映射（用于连线）
  const getRelationColor = (relationType?: RelationType): string => {
    const colorMap: Record<string, string> = {
      BASIS: '#818cf8',           // 依据 - 靛蓝色
      ORIGINAL_VOUCHER: '#c084fc', // 原始凭证 - 紫色
      CASH_FLOW: '#34d399',        // 资金流 - 绿色
      ARCHIVE: '#fbbf24',          // 归档 - 黄色
      SYSTEM_AUTO: '#94a3b8',      // 系统自动 - 灰色
      default: '#64748b'           // 默认 - 灰色
    };
    return colorMap[relationType || 'default'] || colorMap.default;
  };

  // 分类上游和下游节点
  // 业务逻辑：
  // - 上游：中心节点的依据、原始凭证、来源（如合同→报销单，发票→报销单）
  // - 下游：中心节点的流向、归档、结果（如报销单→付款单，付款单→银行回单，凭证→报表）
  const { upstreamNodes, downstreamNodes } = useMemo(() => {
    const upstream: Array<{ node: RelationNodeData; relationType?: RelationType }> = [];
    const downstream: Array<{ node: RelationNodeData; relationType?: RelationType }> = [];

    relations.forEach(relation => {
      const relationType = (relation.relationType || 'default') as RelationType;
      
      // 根据关系类型判断业务方向
      // BASIS（依据）、ORIGINAL_VOUCHER（原始凭证）表示：from 是依据/凭证（上游），to 是单据（下游）
      // CASH_FLOW（资金流）、ARCHIVE（归档）表示：from 是源头（上游），to 是流向/归档（下游）
      
      if (relation.to === centerNode.id) {
        // 关系指向中心节点
        // 对于 BASIS 和 ORIGINAL_VOUCHER，from 是依据/凭证（上游）
        // 对于其他类型，from 也可能是上游
        const node = nodes.find(n => n.id === relation.from);
        if (node && (relationType === 'BASIS' || relationType === 'ORIGINAL_VOUCHER' || relationType === 'default')) {
          upstream.push({ node, relationType });
        } else if (node) {
          // 其他情况，如果指向中心节点，可能是上游（如资金流来源）
          upstream.push({ node, relationType });
        }
      } else if (relation.from === centerNode.id) {
        // 关系从中心节点出发
        // 对于 BASIS 和 ORIGINAL_VOUCHER，to 是单据（下游）
        // 对于 CASH_FLOW 和 ARCHIVE，to 是流向/归档（下游）
        const node = nodes.find(n => n.id === relation.to);
        if (node) {
          downstream.push({ node, relationType });
        }
      }
    });

    return {
      upstreamNodes: upstream,
      downstreamNodes: downstream
    };
  }, [centerNode.id, nodes, relations]);

  // 计算连线位置
  useEffect(() => {
    const updateLines = () => {
      if (!containerRef.current || !centerCardRef.current) return;

      const containerRect = containerRef.current.getBoundingClientRect();
      const centerRect = centerCardRef.current.getBoundingClientRect();
      
      const centerY = centerRect.top + centerRect.height / 2 - containerRect.top;
      const centerLeft = centerRect.left - containerRect.left;
      const centerRight = centerRect.right - containerRect.left;

      const lines: Array<{
        id: string;
        x1: number;
        y1: number;
        x2: number;
        y2: number;
        color: string;
        dasharray?: string;
      }> = [];

      // 上游到中心的连线
      upstreamNodes.forEach(({ node, relationType }) => {
        const cardEl = upstreamCardRefs.current.get(node.id);
        if (cardEl) {
          const cardRect = cardEl.getBoundingClientRect();
          const cardX = cardRect.right - containerRect.left;
          const cardY = cardRect.top + cardRect.height / 2 - containerRect.top;
          
          const color = getRelationColor(relationType);
          lines.push({
            id: `upstream-${node.id}`,
            x1: cardX,
            y1: cardY,
            x2: centerLeft,
            y2: centerY,
            color,
            dasharray: relationType === 'BASIS' || relationType === 'ORIGINAL_VOUCHER' ? '5,5' : undefined
          });
        }
      });

      // 中心到下游的连线
      downstreamNodes.forEach(({ node, relationType }) => {
        const cardEl = downstreamCardRefs.current.get(node.id);
        if (cardEl) {
          const cardRect = cardEl.getBoundingClientRect();
          const cardX = cardRect.left - containerRect.left;
          const cardY = cardRect.top + cardRect.height / 2 - containerRect.top;
          
          const color = getRelationColor(relationType);
          lines.push({
            id: `downstream-${node.id}`,
            x1: centerRight,
            y1: centerY,
            x2: cardX,
            y2: cardY,
            color,
            dasharray: relationType === 'ARCHIVE' ? '5,5' : undefined
          });
        }
      });

      setConnectionLines(lines);
    };

    updateLines();
    
    // 监听窗口大小变化和滚动
    window.addEventListener('resize', updateLines);
    window.addEventListener('scroll', updateLines, true);
    
    // 使用 ResizeObserver 监听容器大小变化 (解决侧边栏收缩/展开导致的问题)
    let resizeObserver: ResizeObserver | null = null;
    if (containerRef.current) {
      resizeObserver = new ResizeObserver(() => {
        // 请求动画帧以避免频繁更新，并确保布局已完成
        requestAnimationFrame(updateLines);
      });
      resizeObserver.observe(containerRef.current);
    }
    
    // 延迟执行，确保 DOM 已渲染
    const timer = setTimeout(updateLines, 100);
    
    return () => {
      window.removeEventListener('resize', updateLines);
      window.removeEventListener('scroll', updateLines, true);
      if (resizeObserver) {
        resizeObserver.disconnect();
      }
      clearTimeout(timer);
    };
  }, [upstreamNodes, downstreamNodes, centerNode.id]);

  // 处理节点点击：打开右侧详情抽屉
  const handleNodeClick = (nodeId: string, nodeData: RelationNodeData) => {
    // 高亮当前点击的节点
    setHighlightedNodeId(nodeId);
    // 调用父组件的点击处理（打开详情抽屉）
    onNodeClick?.(nodeId, nodeData);
  };

  return (
    <div ref={containerRef} className="flex h-full gap-4 px-6 py-4 relative">
      {/* SVG 连线层（在所有内容之上） */}
      {connectionLines.length > 0 && (
        <svg 
          className="absolute inset-0 pointer-events-none z-10"
          style={{ width: '100%', height: '100%' }}
        >
          <defs>
            {/* 箭头标记 */}
            <marker
              id="arrowhead"
              markerWidth="10"
              markerHeight="10"
              refX="9"
              refY="3"
              orient="auto"
            >
              <polygon points="0 0, 10 3, 0 6" fill="#64748b" />
            </marker>
          </defs>
          
          {connectionLines.map(line => (
            <line
              key={line.id}
              x1={line.x1}
              y1={line.y1}
              x2={line.x2}
              y2={line.y2}
              stroke={line.color}
              strokeWidth="2"
              strokeDasharray={line.dasharray}
              markerEnd="url(#arrowhead)"
              opacity="0.6"
            />
          ))}
        </svg>
      )}

      {/* 左侧栏 - 上游数据 */}
      <div className="w-[300px] flex flex-col relative z-20">
        <div className="mb-4">
          <h3 className="text-lg font-semibold text-slate-700">上游数据</h3>
          <p className="text-xs text-slate-400 mt-1">依据、凭证、来源</p>
        </div>
        <div className="flex-1 overflow-y-auto space-y-4">
          {upstreamNodes.length > 0 ? (
            upstreamNodes.map(({ node, relationType }) => (
              <div
                key={node.id}
                ref={(el) => {
                  if (el) upstreamCardRefs.current.set(node.id, el);
                  else upstreamCardRefs.current.delete(node.id);
                }}
              >
                <ArchiveCard
                  node={node}
                  relationType={relationType}
                  onClick={() => handleNodeClick(node.id, node)}
                  highlighted={highlightedNodeId === node.id || highlightedArchiveId === node.id}
                />
              </div>
            ))
          ) : (
            <EmptyColumnState message="暂无上游数据" isUpstream={true} />
          )}
        </div>
      </div>

      {/* 中心栏 */}
      <div className="flex-1 flex items-center justify-center relative z-20">
        <div className="flex flex-col items-center">
          {/* 核心单据卡片 */}
          <div ref={centerCardRef}>
            <ArchiveCard
              node={centerNode}
              isCenter
              onClick={() => handleNodeClick(centerNode.id, centerNode)}
              highlighted={highlightedNodeId === centerNode.id || highlightedArchiveId === centerNode.id}
            />
          </div>

          {/* 业务逻辑说明 */}
          {(upstreamNodes.length > 0 || downstreamNodes.length > 0) && (
            <div className="mt-4 text-xs text-slate-500 text-center max-w-md">
              <div className="inline-block bg-slate-50 px-3 py-2 rounded-lg border border-slate-200">
                <div className="font-medium mb-1">业务流向说明</div>
                <div className="text-slate-600 space-y-0.5">
                  {upstreamNodes.length > 0 && (
                    <div>← 左侧：该单据的依据、凭证、来源</div>
                  )}
                  {downstreamNodes.length > 0 && (
                    <div>→ 右侧：该单据的流向、归档、结果</div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 右侧栏 - 下游数据 */}
      <div className="w-[300px] flex flex-col relative z-20">
        <div className="mb-4">
          <h3 className="text-lg font-semibold text-slate-700">下游数据</h3>
          <p className="text-xs text-slate-400 mt-1">流向、归档、结果</p>
        </div>
        <div className="flex-1 overflow-y-auto space-y-4">
          {downstreamNodes.length > 0 ? (
            downstreamNodes.map(({ node, relationType }) => (
              <div
                key={node.id}
                ref={(el) => {
                  if (el) downstreamCardRefs.current.set(node.id, el);
                  else downstreamCardRefs.current.delete(node.id);
                }}
              >
                <ArchiveCard
                  node={node}
                  relationType={relationType}
                  onClick={() => handleNodeClick(node.id, node)}
                  highlighted={highlightedNodeId === node.id || highlightedArchiveId === node.id}
                />
              </div>
            ))
          ) : (
            <EmptyColumnState message="暂无下游数据" />
          )}
        </div>
      </div>
    </div>
  );
};

/**
 * 空状态组件
 */
const EmptyColumnState: React.FC<{ message: string; isUpstream?: boolean }> = ({ message, isUpstream = false }) => (
  <div className="flex flex-col items-center justify-center h-32 text-slate-400 text-sm p-4">
    <div className="mb-1">{message}</div>
    {isUpstream && (
      <div className="text-xs text-slate-300 mt-2 text-center">
        该单据可能是业务起点<br />
        （如原始凭证、合同等）
      </div>
    )}
  </div>
);
