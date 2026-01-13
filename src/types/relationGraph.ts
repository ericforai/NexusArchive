// Input: React Flow Node/Edge 类型、本地 RelationGraph 类型
// Output: 关系图谱专用类型定义
// Pos: src/types/relationGraph.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import type { Node, Edge } from '@xyflow/react';
import type { RelationGraph, RelationGraphNode, RelationGraphEdge } from '@/api/autoAssociation';

/**
 * 档案类型枚举
 */
export type ArchiveType = 'contract' | 'invoice' | 'voucher' | 'receipt' | 'report' | 'ledger' | 'other';

/**
 * 关系类型枚举
 */
export type RelationType = 'BASIS' | 'ORIGINAL_VOUCHER' | 'CASH_FLOW' | 'ARCHIVE' | 'SYSTEM_AUTO' | string;

/**
 * 关系类型中文映射
 */
export const RELATION_TYPE_LABELS: Record<string, string> = {
  BASIS: '依据',
  ORIGINAL_VOUCHER: '原始凭证',
  CASH_FLOW: '资金流',
  ARCHIVE: '归档',
  SYSTEM_AUTO: '系统自动'
};

/**
 * 节点数据结构（React Flow Node data）
 */
export interface RelationNodeData extends RelationGraphNode {
  /** 节点距中心的深度（0=中心节点） */
  depth: number;
  /** 是否为中心节点 */
  isCenter: boolean;
  /** 是否已展开（是否有子节点） */
  isExpanded: boolean;
  /** 是否正在加载 */
  isLoading?: boolean;
  /** 加载错误信息 */
  error?: string;
}

/**
 * React Flow 节点类型
 */
export type RelationNode = Node<RelationNodeData & Record<string, unknown>>;

/**
 * 连线数据结构（React Flow Edge data）
 */
export interface RelationEdgeData extends RelationGraphEdge, Record<string, unknown> {
  /** 关系类型 */
  relationType?: RelationType;
  /** 是否悬停 */
  isHovered?: boolean;
}

/**
 * React Flow 连线类型
 */
export type RelationEdge = Edge<RelationEdgeData>;

/**
 * 图谱状态接口
 */
export interface RelationGraphState {
  // === 图谱数据 ===
  /** React Flow 节点数组 */
  nodes: RelationNode[];
  /** React Flow 连线数组 */
  edges: RelationEdge[];
  /** 中心节点ID */
  centerNodeId: string;

  // === 展开/折叠状态 ===
  /** 已展开的节点ID集合 */
  expandedNodeIds: Set<string>;
  /** 节点深度映射 (nodeId -> depth) */
  nodeDepths: Map<string, number>;
  /** 节点父子关系映射 (childId -> parentId) */
  nodeParents: Map<string, string>;

  // === 缓存 ===
  /** 已加载的关系数据缓存 (nodeId -> RelationGraph) */
  loadedRelations: Map<string, RelationGraph>;

  // === UI 状态 ===
  /** 是否正在加载初始图谱 */
  isInitialLoading: boolean;
  /** 初始加载错误 */
  initialError: string | null;

  // === 操作 ===
  /** 初始化图谱（设置中心节点） */
  initializeGraph: (archiveId: string) => Promise<void>;
  /** 展开节点 */
  expandNode: (nodeId: string) => Promise<void>;
  /** 折叠节点 */
  collapseNode: (nodeId: string) => void;
  /** 后台刷新节点数据 */
  refreshNodeData: (nodeId: string) => Promise<void>;
  /** 重置图谱 */
  resetGraph: () => void;
  /** 设置节点 */
  setNodes: (nodes: RelationNode[]) => void;
  /** 设置连线 */
  setEdges: (edges: RelationEdge[]) => void;
  /** 更新节点数据 */
  updateNodeData: (nodeId: string, data: Partial<RelationNodeData>) => void;
}

/**
 * 布局配置
 */
export interface LayoutConfig {
  /** 节点宽度 */
  nodeWidth: number;
  /** 节点高度 */
  nodeHeight: number;
  /** 水平间距 */
  horizontalSpacing: number;
  /** 垂直间距 */
  verticalSpacing: number;
  /** 力导向布局强度 */
  forceStrength: number;
  /** 力导向迭代次数 */
  forceIterations: number;
}

/**
 * 默认布局配置
 */
export const DEFAULT_LAYOUT_CONFIG: LayoutConfig = {
  nodeWidth: 200,
  nodeHeight: 120,
  horizontalSpacing: 100,
  verticalSpacing: 80,
  forceStrength: -300,
  forceIterations: 100
};

/**
 * 档案类型元数据
 */
export interface ArchiveTypeMeta {
  /** 类型标识 */
  type: ArchiveType;
  /** 标签 */
  label: string;
  /** 背景色 */
  bg: string;
  /** 边框色 */
  border: string;
  /** 文字色 */
  text: string;
  /** 图标组件名称 */
  icon: string;
}

/**
 * 档案类型样式映射
 */
export const ARCHIVE_TYPE_STYLES: Record<ArchiveType, Omit<ArchiveTypeMeta, 'type'>> = {
  contract: {
    label: '合同',
    bg: 'bg-indigo-50',
    border: 'border-indigo-400',
    text: 'text-indigo-700',
    icon: 'Building'
  },
  invoice: {
    label: '发票',
    bg: 'bg-purple-50',
    border: 'border-purple-400',
    text: 'text-purple-700',
    icon: 'Receipt'
  },
  voucher: {
    label: '凭证',
    bg: 'bg-blue-50',
    border: 'border-blue-400',
    text: 'text-blue-700',
    icon: 'FileText'
  },
  receipt: {
    label: '回单',
    bg: 'bg-emerald-50',
    border: 'border-emerald-400',
    text: 'text-emerald-700',
    icon: 'CreditCard'
  },
  report: {
    label: '报表',
    bg: 'bg-amber-50',
    border: 'border-amber-400',
    text: 'text-amber-700',
    icon: 'FileSpreadsheet'
  },
  ledger: {
    label: '账簿',
    bg: 'bg-slate-50',
    border: 'border-slate-400',
    text: 'text-slate-700',
    icon: 'FileSpreadsheet'
  },
  other: {
    label: '其他',
    bg: 'bg-slate-50',
    border: 'border-slate-400',
    text: 'text-slate-700',
    icon: 'FileText'
  }
};

/**
 * 根据档号前缀解析档案类型
 */
export function resolveArchiveType(archiveCode: string): ArchiveType {
  if (!archiveCode) return 'other';
  const prefix = archiveCode.toUpperCase().substring(0, Math.min(2, archiveCode.length));
  const typeMap: Record<string, ArchiveType> = {
    'HT': 'contract',
    'FP': 'invoice',
    'JZ': 'voucher',
    'PZ': 'voucher',
    'HD': 'receipt',
    'BB': 'report',
    'ZB': 'ledger'
  };
  return typeMap[prefix] || 'other';
}
