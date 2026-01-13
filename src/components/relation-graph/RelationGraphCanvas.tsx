// Input: React Flow、自定义节点/边、Zustand store
// Output: 关系图谱画布组件
// Pos: 关系图谱画布容器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useReactFlow,
  MarkerType
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { RelationNode } from './RelationNode';
import { RelationEdge } from './RelationEdge';
import { useRelationGraphStore } from '@/store/useRelationGraphStore';
import type { RelationNodeData } from '@/types/relationGraph';
import type { Node, Edge, EdgeTypes, NodeTypes } from '@xyflow/react';

// 节点类型配置
const nodeTypes: NodeTypes = {
  relationNode: RelationNode,
};

// 边类型配置
const edgeTypes: EdgeTypes = {
  relationEdge: RelationEdge,
};

// 小地图节点颜色
const minimapNodeColor = (node: Node) => {
  const data = (node.data as unknown) as RelationNodeData | undefined;
  const colorMap: Record<string, string> = {
    contract: '#818cf8',
    invoice: '#c084fc',
    voucher: '#60a5fa',
    receipt: '#34d399',
    report: '#fbbf24',
    ledger: '#94a3b8',
    other: '#94a3b8'
  };
  return data?.isCenter ? '#f59e0b' : (colorMap[data?.type || 'other'] || '#94a3b8');
};

// 小地图节点背景色
const minimapNodeBackgroundColor = (node: Node) => {
  const data = (node.data as unknown) as RelationNodeData | undefined;
  if (data?.isCenter) return '#fef3c7';
  return '#ffffff';
};

/**
 * React Flow 内部组件：处理自动聚焦
 */
const GraphFlowContent = () => {
  const { fitView } = useReactFlow();
  const nodes = useRelationGraphStore(s => s.nodes) as unknown as Node[];
  const prevNodesCountRef = useRef(0);

  // 节点数量变化时自动适配视野
  useEffect(() => {
    if (nodes.length > 0 && nodes.length !== prevNodesCountRef.current) {
      requestAnimationFrame(() => {
        fitView({ padding: 0.2, duration: 300 });
      });
    }
    prevNodesCountRef.current = nodes.length;
  }, [nodes.length, fitView]);

  return null;
};

/**
 * 关系图谱画布组件属性
 */
export interface RelationGraphCanvasProps {
  /** 节点点击回调 */
  onNodeClick?: (nodeId: string, nodeData: RelationNodeData) => void;
  /** 画布类名 */
  className?: string;
}

/**
 * 关系图谱画布组件
 */
export const RelationGraphCanvas: React.FC<RelationGraphCanvasProps> = ({
  onNodeClick,
  className = ''
}) => {
  const storeNodes = useRelationGraphStore(s => s.nodes) as unknown as Node[];
  const storeEdges = useRelationGraphStore(s => s.edges) as unknown as Edge[];
  const expandNode = useRelationGraphStore(s => s.expandNode);
  const setEdges = useRelationGraphStore(s => s.setEdges);

  const flowInstanceRef = useRef<any>(null);

  // 节点点击处理
  const onNodeClickHandler = useCallback(
    (_: React.MouseEvent, node: Node) => {
      const nodeData = (node.data as unknown) as RelationNodeData;

      if (onNodeClick) {
        onNodeClick(node.id, nodeData);
      }

      expandNode(node.id);
    },
    [onNodeClick, expandNode]
  );

  // 添加双向箭头标记
  const edgesWithMarkers = useMemo(() =>
    (storeEdges as Edge[]).map(edge => ({
      ...edge,
      markerEnd: { type: MarkerType.ArrowClosed, color: '#94a3b8', strokeWidth: 1.5 },
      markerStart: { type: MarkerType.Arrow, color: '#94a3b8', strokeWidth: 1.5 }
    })),
    [storeEdges]
  );

  return (
    <div className={`w-full h-full ${className}`}>
      <ReactFlow
        nodes={storeNodes}
        edges={edgesWithMarkers}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        onNodeClick={onNodeClickHandler as any}
        fitView
        fitViewOptions={{ padding: 0.3 }}
        minZoom={0.2}
        maxZoom={2}
        defaultViewport={{ x: 0, y: 0, zoom: 1 }}
        nodesDraggable
        nodesConnectable={false}
        elementsSelectable
        selectNodesOnDrag={true}
        onInit={(instance) => {
          flowInstanceRef.current = instance;
        }}
        proOptions={{ hideAttribution: true }}
      >
        <GraphFlowContent />

        {/* 网格背景 */}
        <Background
          color="#e2e8f0"
          gap={16}
          size={1}
        />

        {/* 控制栏 */}
        <Controls
          className="!bg-white !border !border-slate-200 !shadow-lg"
          showZoom={true}
          showFitView={true}
          showInteractive={false}
        />

        {/* 小地图 */}
        <MiniMap
          nodeColor={minimapNodeColor}
          nodeStrokeWidth={2}
          nodeStrokeColor="#94a3b8"
          nodeBorderRadius={8}
          maskColor="rgba(0, 0, 0, 0.05)"
          pannable
          zoomable
          className="!bg-white !border !border-slate-200 !shadow-lg"
        />
      </ReactFlow>
    </div>
  );
};

/**
 * 导出带有 Provider 的组件
 */
import { ReactFlowProvider } from '@xyflow/react';

export const RelationGraphCanvasWithProvider: React.FC<RelationGraphCanvasProps> = (props) => {
  return (
    <ReactFlowProvider>
      <RelationGraphCanvas {...props} />
    </ReactFlowProvider>
  );
};
