// Input: zustand、React Flow 类型、本地 API 与类型
// Output: useRelationGraphStore（初始化保留全量关系图与方向视图）
// Pos: 关系图谱状态管理
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { create } from 'zustand';
import type {
  RelationNode,
  RelationEdge,
  RelationGraphState,
  LayoutConfig
} from '@/types/relationGraph';
import { DEFAULT_LAYOUT_CONFIG } from '@/types/relationGraph';
import { autoAssociationApi } from '@/api/autoAssociation';
import { resolveArchiveType } from '@/types/relationGraph';
// Node 类型由 RelationNode 继承，无需单独导入

/**
 * 最大展开深度
 */
const MAX_DEPTH = 3;

/**
 * 生成初始坐标（围绕父节点呈扇形分布）
 */
function generateChildPosition(
  parentNode: RelationNode,
  index: number,
  total: number,
  config: LayoutConfig
): { x: number; y: number } {
  const { nodeWidth, horizontalSpacing, verticalSpacing } = config;

  // 根据深度决定展开方向
  const depth = (parentNode.data?.depth ?? 0) + 1;

  // 奇数层向右展开，偶数层向左展开
  const direction = depth % 2 === 1 ? 1 : -1;

  // 计算相对于父节点的位置
  const baseX = parentNode.position.x + direction * (nodeWidth + horizontalSpacing);
  const baseY = parentNode.position.y;

  // 多个子节点垂直分布
  if (total > 1) {
    const totalHeight = (total - 1) * (config.nodeHeight + verticalSpacing);
    const offsetY = -totalHeight / 2 + index * (config.nodeHeight + verticalSpacing);
    return { x: baseX, y: baseY + offsetY };
  }

  return { x: baseX, y: baseY };
}

/**
 * 关系图谱状态 Store
 */
export const useRelationGraphStore = create<RelationGraphState>((set, get) => ({
  // === 初始状态 ===
  nodes: [],
  edges: [],
  centerNodeId: '',
  expandedNodeIds: new Set<string>(),
  nodeDepths: new Map<string, number>(),
  nodeParents: new Map<string, string>(),
  loadedRelations: new Map<string, any>(),
  isInitialLoading: false,
  initialError: null,
  originalQueryId: null,
  redirectMessage: null,
  directionalView: null,

  // === 操作 ===

  /**
   * 初始化图谱（设置中心节点）
   */
  initializeGraph: async (archiveId: string) => {
    set({ isInitialLoading: true, initialError: null });

    try {
      const graph = await autoAssociationApi.getRelationGraph(archiveId);
      const centerNode = graph.nodes.find(n => n.id === graph.centerId) || graph.nodes[0];

      if (!centerNode) {
        throw new Error('未找到档案数据');
      }

      // 处理自动转换（如果发生了自动转换，显示提示并高亮原始查询档案）
      if (graph.autoRedirected && graph.originalQueryId) {
        // 将原始查询档案ID保存到 state，供前端使用
        set({ 
          originalQueryId: graph.originalQueryId, 
          redirectMessage: graph.redirectMessage || '已自动切换到关联的记账凭证查看完整业务链路' 
        });
      } else {
        set({ originalQueryId: null, redirectMessage: null });
      }

      const nodes: RelationNode[] = [];
      const nodeDepths = new Map<string, number>();

      // 计算全图深度（按无向关系最短层级）
      const adjacency = new Map<string, Set<string>>();
      graph.edges.forEach((edge) => {
        const fromSet = adjacency.get(edge.from) || new Set<string>();
        fromSet.add(edge.to);
        adjacency.set(edge.from, fromSet);

        const toSet = adjacency.get(edge.to) || new Set<string>();
        toSet.add(edge.from);
        adjacency.set(edge.to, toSet);
      });

      const depthMap = new Map<string, number>();
      const queue: string[] = [centerNode.id];
      depthMap.set(centerNode.id, 0);

      while (queue.length > 0) {
        const current = queue.shift();
        if (!current) continue;
        const currentDepth = depthMap.get(current) ?? 0;
        const neighbors = adjacency.get(current) || new Set<string>();
        neighbors.forEach((neighbor) => {
          if (depthMap.has(neighbor)) return;
          depthMap.set(neighbor, currentDepth + 1);
          queue.push(neighbor);
        });
      }

      // 创建全量节点（保留中心标识）
      graph.nodes.forEach((graphNode, idx) => {
        const depth = depthMap.get(graphNode.id) ?? 1;
        const x = depth === 0 ? 0 : (idx % 2 === 0 ? -1 : 1) * (200 + depth * 120);
        const y = (idx - graph.nodes.length / 2) * 120;

        nodes.push({
          id: graphNode.id,
          type: 'relationNode',
          position: { x, y },
          data: {
            ...graphNode,
            type: resolveArchiveType(graphNode.code || ''),
            depth,
            isCenter: graphNode.id === centerNode.id,
            isExpanded: false
          }
        });
        nodeDepths.set(graphNode.id, depth);
      });

      // 创建连线（只包含两端节点都存在的边）
      const nodeIdSet = new Set(nodes.map(n => n.id));
      const edges: RelationEdge[] = graph.edges
        .filter(edge => nodeIdSet.has(edge.from) && nodeIdSet.has(edge.to))
        .map((edge, idx) => ({
          id: `edge-${idx}`,
          source: edge.from,
          target: edge.to,
          type: 'relationEdge',
          data: {
            ...edge,
            relationType: edge.relationType || 'SYSTEM_AUTO'
          },
          animated: false
        }));

      set({
        nodes,
        edges,
        centerNodeId: centerNode.id,
        expandedNodeIds: new Set(),
        nodeDepths,
        nodeParents: new Map(),
        loadedRelations: new Map([[centerNode.id, graph]]),
        isInitialLoading: false,
        initialError: null,
        directionalView: graph.directionalView || null
      });
    } catch (error: any) {
      let errorMessage = '加载失败，请稍后重试';
      if (error.response?.status === 401) {
        errorMessage = '请先登录系统后再查询关系数据';
      } else if (error.response?.status === 403) {
        errorMessage = '您没有权限查看此档案的关系数据';
      } else if (error.response?.status === 404) {
        errorMessage = '未找到该档案';
      } else if (error.message) {
        errorMessage = error.message;
      }

      set({
        nodes: [],
        edges: [],
        centerNodeId: '',
        isInitialLoading: false,
        initialError: errorMessage,
        directionalView: null
      });
    }
  },

  /**
   * 展开节点
   */
  expandNode: async (nodeId: string) => {
    const state = get();
    const targetNode = state.nodes.find(n => n.id === nodeId);

    if (!targetNode) return;

    // 如果已展开，则折叠
    if (state.expandedNodeIds.has(nodeId)) {
      get().collapseNode(nodeId);
      return;
    }

    const currentDepth = state.nodeDepths.get(nodeId) ?? 0;

    // 深度检查：超过 3 度时，折叠最早的 1 度节点
    if (currentDepth >= MAX_DEPTH) {
      const depth1Nodes = Array.from(state.nodeDepths.entries())
        .filter(([_nodeId, depth]) => depth === 1)
        .map(([id]) => id)
        .filter(id => id !== nodeId && id !== state.centerNodeId);

      if (depth1Nodes.length > 0) {
        get().collapseNode(depth1Nodes[0]);
      }
    }

    // 标记为加载中
    set({
      nodes: state.nodes.map(n =>
        n.id === nodeId
          ? { ...n, data: { ...n.data, isLoading: true, error: undefined } }
          : n
      )
    });

    try {
      // 调用 API 获取关系数据
      const graph = await autoAssociationApi.getRelationGraph(nodeId);

      // 过滤掉已存在的节点
      const existingIds = new Set(state.nodes.map(n => n.id));
      const existingEdgeKeys = new Set(
        state.edges.map(e => `${e.source}-${e.target}`)
      );

      const newNodes: RelationNode[] = [];
      const newEdges: RelationEdge[] = [];

      const newDepth = currentDepth + 1;

      // 创建新节点
      for (let i = 0; i < graph.nodes.length; i++) {
        const graphNode = graph.nodes[i];
        if (graphNode.id === nodeId || existingIds.has(graphNode.id)) continue;

        const position = generateChildPosition(
          targetNode,
          newNodes.length,
          graph.nodes.length,
          DEFAULT_LAYOUT_CONFIG
        );

        newNodes.push({
          id: graphNode.id,
          type: 'relationNode',
          position,
          data: {
            ...graphNode,
            type: resolveArchiveType(graphNode.code || ''),
            depth: newDepth,
            isCenter: false,
            isExpanded: false
          }
        });
      }

      // 创建新连线
      for (const edge of graph.edges) {
        const edgeKey = `${edge.from}-${edge.to}`;
        if (existingEdgeKeys.has(edgeKey)) continue;

        newEdges.push({
          id: `edge-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
          source: edge.from,
          target: edge.to,
          type: 'relationEdge',
          data: {
            ...edge,
            relationType: edge.relationType || 'SYSTEM_AUTO'
          },
          animated: false
        });
      }

      // 更新状态
      const updatedExpandedNodeIds = new Set(state.expandedNodeIds);
      updatedExpandedNodeIds.add(nodeId);

      const updatedNodeDepths = new Map(state.nodeDepths);
      const updatedNodeParents = new Map(state.nodeParents);

      newNodes.forEach(node => {
        updatedNodeDepths.set(node.id, newDepth);
        updatedNodeParents.set(node.id, nodeId);
      });

      const updatedLoadedRelations = new Map(state.loadedRelations);
      updatedLoadedRelations.set(nodeId, graph);

      set({
        nodes: [
          ...state.nodes.map(n =>
            n.id === nodeId
              ? { ...n, data: { ...n.data, isLoading: false, isExpanded: true } }
              : n
          ),
          ...newNodes
        ],
        edges: [...state.edges, ...newEdges],
        expandedNodeIds: updatedExpandedNodeIds,
        nodeDepths: updatedNodeDepths,
        nodeParents: updatedNodeParents,
        loadedRelations: updatedLoadedRelations
      });

      // 后台刷新（300ms 后）
      setTimeout(() => {
        get().refreshNodeData(nodeId);
      }, 300);

    } catch (error: any) {
      let errorMessage = '加载失败';
      if (error.response?.status === 403) {
        errorMessage = '无权限访问';
      }

      set({
        nodes: state.nodes.map(n =>
          n.id === nodeId
            ? { ...n, data: { ...n.data, isLoading: false, error: errorMessage } }
            : n
        )
      });
    }
  },

  /**
   * 折叠节点（移除该节点的所有子孙节点）
   */
  collapseNode: (nodeId: string) => {
    const state = get();

    // 找出所有子孙节点
    const descendants = new Set<string>();
    const collectDescendants = (parentId: string) => {
      state.nodeParents.forEach((pid, childId) => {
        if (pid === parentId) {
          descendants.add(childId);
          collectDescendants(childId);
        }
      });
    };
    collectDescendants(nodeId);

    // 移除子孙节点和相关连线
    const remainingNodes = state.nodes.filter(n => !descendants.has(n.id));
    const remainingEdges = state.edges.filter(
      e => !descendants.has(e.source as string) && !descendants.has(e.target as string)
    );

    // 更新状态
    const updatedExpandedNodeIds = new Set(state.expandedNodeIds);
    updatedExpandedNodeIds.delete(nodeId);

    const updatedNodeDepths = new Map(state.nodeDepths);
    const updatedNodeParents = new Map(state.nodeParents);
    descendants.forEach(id => {
      updatedNodeDepths.delete(id);
      updatedNodeParents.delete(id);
    });

    set({
      nodes: remainingNodes.map(n =>
        n.id === nodeId
          ? { ...n, data: { ...n.data, isExpanded: false } }
          : n
      ),
      edges: remainingEdges,
      expandedNodeIds: updatedExpandedNodeIds,
      nodeDepths: updatedNodeDepths,
      nodeParents: updatedNodeParents
    });
  },

  /**
   * 后台刷新节点数据
   */
  refreshNodeData: async (nodeId: string) => {
    const state = get();

    try {
      const graph = await autoAssociationApi.getRelationGraph(nodeId);
      const updatedLoadedRelations = new Map(state.loadedRelations);
      updatedLoadedRelations.set(nodeId, graph);

      set({ loadedRelations: updatedLoadedRelations });
    } catch {
      // 静默失败，不影响用户体验
    }
  },

  /**
   * 重置图谱
   */
  resetGraph: () => {
    set({
      nodes: [],
      edges: [],
      centerNodeId: '',
      expandedNodeIds: new Set(),
      nodeDepths: new Map(),
      nodeParents: new Map(),
      loadedRelations: new Map(),
      isInitialLoading: false,
      initialError: null,
      originalQueryId: null,
      redirectMessage: null,
      directionalView: null
    });
  },

  /**
   * 设置节点
   */
  setNodes: (nodes: RelationNode[]) => set({ nodes }),

  /**
   * 设置连线
   */
  setEdges: (edges: RelationEdge[]) => set({ edges }),

  /**
   * 更新节点数据
   */
  updateNodeData: (nodeId: string, data: Partial<RelationNode['data']>) => {
    set({
      nodes: get().nodes.map(n =>
        n.id === nodeId
          ? { ...n, data: { ...n.data, ...data } }
          : n
      )
    });
  }
}));
