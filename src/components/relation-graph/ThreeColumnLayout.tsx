// Input: React、RelationNodeData、RelationEdgeData
// Output: ThreeColumnLayout 组件（三栏布局，后端方向优先 + 逐层展开）
// Pos: 关系图谱三栏布局组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo, useState, useRef, useEffect } from 'react';
import { ArchiveCard } from './ArchiveCard';
import type { RelationNodeData, RelationEdgeData, RelationType } from '@/types/relationGraph';
import type { RelationDirectionalView } from '@/api/autoAssociation';

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
  /** 付款主线节点ID */
  mainlineNodeIds?: string[];
  /** 主线缺口 */
  missingSteps?: Array<{ stepLabel: string; fromNodeId: string }>;
  /** 后端解析的上下游视图（优先使用） */
  directionalView?: RelationDirectionalView;
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
  highlightedArchiveId = null,
  mainlineNodeIds = [],
  missingSteps = [],
  directionalView
}) => {
  const [highlightedNodeId, setHighlightedNodeId] = useState<string | null>(null);
  const [visibleDepth, setVisibleDepth] = useState(1);

  useEffect(() => {
    setVisibleDepth(1);
  }, [centerNode.id]);
  
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

  const nodeMap = useMemo(() => new Map(nodes.map(node => [node.id, node])), [nodes]);
  const mainlineSet = useMemo(() => new Set(mainlineNodeIds), [mainlineNodeIds]);

  const {
    upstreamNodes,
    downstreamNodes,
    maxUpstreamDepth,
    maxDownstreamDepth
  } = useMemo(() => {
    type LayeredNode = { node: RelationNodeData; relationType?: RelationType; depth: number };
    type Side = 'upstream' | 'downstream';

    const outgoing = new Map<string, RelationEdgeData[]>();
    const incoming = new Map<string, RelationEdgeData[]>();
    const undirected = new Map<string, Array<{ neighborId: string; relationType?: RelationType }>>();

    relations.forEach((relation) => {
      const relationType = (relation.relationType || 'default') as RelationType;

      const outList = outgoing.get(relation.from) || [];
      outList.push(relation);
      outgoing.set(relation.from, outList);

      const inList = incoming.get(relation.to) || [];
      inList.push(relation);
      incoming.set(relation.to, inList);

      const fromNeighbors = undirected.get(relation.from) || [];
      fromNeighbors.push({ neighborId: relation.to, relationType });
      undirected.set(relation.from, fromNeighbors);

      const toNeighbors = undirected.get(relation.to) || [];
      toNeighbors.push({ neighborId: relation.from, relationType });
      undirected.set(relation.to, toNeighbors);
    });

    const assigned = new Set<string>([centerNode.id]);
    const sideMap = new Map<string, Side>();
    const depthMap = new Map<string, number>();
    const relationTypeMap = new Map<string, RelationType | undefined>();
    const queue: Array<{ id: string; side: Side; depth: number }> = [];

    // 优先使用后端 directionalView，避免前端固定流程推断与真实业务冲突
    const hasDirectionalView = (directionalView?.upstream?.length || 0) > 0
      || (directionalView?.downstream?.length || 0) > 0;
    if (hasDirectionalView) {
      const layers = directionalView?.layers || {};
      const upstreamIds = directionalView?.upstream || [];
      const downstreamIds = directionalView?.downstream || [];

      upstreamIds.forEach((id) => {
        if (!id || id === centerNode.id || !nodeMap.has(id) || assigned.has(id)) return;
        assigned.add(id);
        sideMap.set(id, 'upstream');
        depthMap.set(id, Math.max(1, layers[id] || 1));
      });

      downstreamIds.forEach((id) => {
        if (!id || id === centerNode.id || !nodeMap.has(id) || assigned.has(id)) return;
        assigned.add(id);
        sideMap.set(id, 'downstream');
        depthMap.set(id, Math.max(1, layers[id] || 1));
      });
    } else {
      // 一跳定向作为“根归属”：指向中心的是上游，从中心发出的是下游
      (incoming.get(centerNode.id) || []).forEach((edge) => {
        const id = edge.from;
        if (!id || assigned.has(id)) return;
        assigned.add(id);
        sideMap.set(id, 'upstream');
        depthMap.set(id, 1);
        relationTypeMap.set(id, (edge.relationType || 'default') as RelationType);
        queue.push({ id, side: 'upstream', depth: 1 });
      });
      (outgoing.get(centerNode.id) || []).forEach((edge) => {
        const id = edge.to;
        if (!id || assigned.has(id)) return;
        assigned.add(id);
        sideMap.set(id, 'downstream');
        depthMap.set(id, 1);
        relationTypeMap.set(id, (edge.relationType || 'default') as RelationType);
        queue.push({ id, side: 'downstream', depth: 1 });
      });

      // 向外扩展，继承根归属；全局去重，确保单节点只在一侧出现
      while (queue.length > 0) {
        const current = queue.shift();
        if (!current) continue;
        const neighbors = undirected.get(current.id) || [];
        neighbors.forEach(({ neighborId, relationType }) => {
          if (!neighborId || assigned.has(neighborId) || neighborId === centerNode.id) return;
          if (!nodeMap.has(neighborId)) return;
          assigned.add(neighborId);
          sideMap.set(neighborId, current.side);
          depthMap.set(neighborId, current.depth + 1);
          relationTypeMap.set(neighborId, relationType);
          queue.push({ id: neighborId, side: current.side, depth: current.depth + 1 });
        });
      }
    }

    const upstream: LayeredNode[] = [];
    const downstream: LayeredNode[] = [];
    let maxUpstream = 0;
    let maxDownstream = 0;
    const resolveRelationType = (id: string, side: Side): RelationType | undefined => {
      if (relationTypeMap.has(id)) return relationTypeMap.get(id);
      if (side === 'upstream') {
        const incomingEdge = incoming.get(id)?.[0];
        return (incomingEdge?.relationType || 'default') as RelationType;
      }
      const outgoingEdge = outgoing.get(id)?.[0];
      return (outgoingEdge?.relationType || 'default') as RelationType;
    };

    sideMap.forEach((side, id) => {
      const node = nodeMap.get(id);
      if (!node) return;
      const depth = depthMap.get(id) || 1;
      const entry: LayeredNode = {
        node,
        relationType: resolveRelationType(id, side),
        depth
      };
      if (side === 'upstream') {
        upstream.push(entry);
        maxUpstream = Math.max(maxUpstream, depth);
      } else {
        downstream.push(entry);
        maxDownstream = Math.max(maxDownstream, depth);
      }
    });

    const sorter = (a: LayeredNode, b: LayeredNode) => {
      if (a.depth !== b.depth) return a.depth - b.depth;
      return a.node.id.localeCompare(b.node.id);
    };

    upstream.sort(sorter);
    downstream.sort(sorter);

    return {
      upstreamNodes: upstream,
      downstreamNodes: downstream,
      maxUpstreamDepth: maxUpstream,
      maxDownstreamDepth: maxDownstream
    };
  }, [centerNode.id, directionalView, nodeMap, relations]);

  const effectiveMaxDepth = Math.max(maxUpstreamDepth, maxDownstreamDepth, 1);
  const visibleUpstream = useMemo(
    () => upstreamNodes.filter(item => item.depth <= visibleDepth || mainlineSet.has(item.node.id)),
    [upstreamNodes, visibleDepth, mainlineSet]
  );
  const visibleDownstream = useMemo(
    () => downstreamNodes.filter(item => item.depth <= visibleDepth || mainlineSet.has(item.node.id)),
    [downstreamNodes, visibleDepth, mainlineSet]
  );

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
      visibleUpstream.forEach(({ node, relationType }) => {
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
      visibleDownstream.forEach(({ node, relationType }) => {
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
  }, [visibleUpstream, visibleDownstream, centerNode.id]);

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
          {visibleUpstream.length > 0 ? (
            visibleUpstream.map(({ node, relationType }) => (
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
                  highlighted={
                    highlightedNodeId === node.id ||
                    highlightedArchiveId === node.id ||
                    mainlineSet.has(node.id)
                  }
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
          <div className="mb-3 flex items-center gap-2">
            <button
              onClick={() => setVisibleDepth((d) => Math.min(d + 1, effectiveMaxDepth))}
              disabled={visibleDepth >= effectiveMaxDepth}
              className="px-3 py-1.5 text-xs rounded-lg bg-primary-600 text-white hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              展开一层
            </button>
            <button
              onClick={() => setVisibleDepth(1)}
              disabled={visibleDepth <= 1}
              className="px-3 py-1.5 text-xs rounded-lg bg-slate-200 text-slate-700 hover:bg-slate-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              收起到首层
            </button>
            {mainlineSet.size === 0 && (
              <span className="text-xs text-slate-500">分支展开深度：{visibleDepth}</span>
            )}
            {mainlineSet.size > 0 && (
              <span className="text-xs text-slate-500">分支展开深度：{visibleDepth}（主线始终可见）</span>
            )}
          </div>

          {/* 核心单据卡片 */}
          <div ref={centerCardRef}>
            <ArchiveCard
              node={centerNode}
              isCenter
              onClick={() => handleNodeClick(centerNode.id, centerNode)}
              highlighted={
                highlightedNodeId === centerNode.id ||
                highlightedArchiveId === centerNode.id ||
                mainlineSet.has(centerNode.id)
              }
            />
          </div>

          {missingSteps.length > 0 && (
            <div className="mt-3 w-full max-w-md rounded-lg border border-rose-300 bg-rose-50 px-3 py-2">
              <div className="text-xs font-semibold text-rose-700 mb-1">主线缺口（已自动跳过）</div>
              <div className="space-y-1">
                {missingSteps.map((step, idx) => (
                  <div key={`${step.fromNodeId}-${step.stepLabel}-${idx}`} className="text-xs text-rose-600">
                    缺失：{step.stepLabel}（来源节点：{step.fromNodeId}）
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 业务逻辑说明 */}
          {(visibleUpstream.length > 0 || visibleDownstream.length > 0) && (
            <div className="mt-4 text-xs text-slate-500 text-center max-w-md">
              <div className="inline-block bg-slate-50 px-3 py-2 rounded-lg border border-slate-200">
                <div className="font-medium mb-1">业务流向说明</div>
                <div className="text-slate-600 space-y-0.5">
                  {visibleUpstream.length > 0 && (
                    <div>← 左侧：该单据的依据、凭证、来源</div>
                  )}
                  {visibleDownstream.length > 0 && (
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
          {visibleDownstream.length > 0 ? (
            visibleDownstream.map(({ node, relationType }) => (
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
                  highlighted={
                    highlightedNodeId === node.id ||
                    highlightedArchiveId === node.id ||
                    mainlineSet.has(node.id)
                  }
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
