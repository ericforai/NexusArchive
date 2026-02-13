// Input: 关系图节点与边
// Output: 主线识别（按流程方向）、缺口检测、打印范围聚合工具
// Pos: src/pages/utilization/relationDrilldown.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import type { RelationNodeData, RelationEdgeData } from '@/types/relationGraph';

export interface MissingMainlineStep {
  stepLabel: string;
  expectedType: RelationNodeData['type'];
  fromNodeId: string;
}

export interface MainlineResult {
  nodeIds: string[];
  missingSteps: MissingMainlineStep[];
}

const MAINLINE_FLOW: Array<RelationNodeData['type']> = [
  'voucher',
  'payment',
  'application',
  'contract',
  'invoice',
  'receipt',
];

const MAINLINE_LABELS: Record<RelationNodeData['type'], string> = {
  voucher: '凭证',
  payment: '付款单',
  application: '付款申请',
  contract: '合同',
  invoice: '发票',
  receipt: '回单',
  report: '报表',
  ledger: '账簿',
  reimbursement: '报销单',
  other: '其他',
};

const createOutgoingSelector = (relations: RelationEdgeData[]) => {
  const outgoing = new Map<string, RelationEdgeData[]>();

  relations.forEach((edge) => {
    const fromList = outgoing.get(edge.from) || [];
    fromList.push(edge);
    outgoing.set(edge.from, fromList);
  });

  return (nodeId: string): RelationEdgeData[] => outgoing.get(nodeId) || [];
};

const sortNodeIdsByStablePriority = (
  nodeIds: string[],
  nodeMap: Map<string, RelationNodeData>
): string[] => {
  return [...nodeIds].sort((a, b) => {
    const nodeA = nodeMap.get(a);
    const nodeB = nodeMap.get(b);
    const dateA = nodeA?.date || '';
    const dateB = nodeB?.date || '';
    if (dateA !== dateB) return dateA.localeCompare(dateB);
    return a.localeCompare(b);
  });
};

export function detectPaymentMainline(
  nodes: RelationNodeData[],
  relations: RelationEdgeData[],
  centerNodeId: string
): MainlineResult {
  const nodeMap = new Map(nodes.map((n) => [n.id, n]));
  const center = nodeMap.get(centerNodeId);

  if (!center) {
    return { nodeIds: [], missingSteps: [] };
  }

  const getOutgoing = createOutgoingSelector(relations);
  const nodeIds: string[] = [centerNodeId];
  const missingSteps: MissingMainlineStep[] = [];
  let cursorId = centerNodeId;

  const currentTypeIndex = MAINLINE_FLOW.indexOf(center.type);
  const expectedFlow = currentTypeIndex >= 0 ? MAINLINE_FLOW.slice(currentTypeIndex) : [center.type, ...MAINLINE_FLOW];

  for (let index = 1; index < expectedFlow.length; index += 1) {
    const expectedType = expectedFlow[index];
    const neighbors = getOutgoing(cursorId);
    const candidateNodeIds = neighbors
      .map((edge) => edge.to)
      .filter((id) => (nodeMap.get(id)?.type || 'other') === expectedType);

    const sortedCandidates = sortNodeIdsByStablePriority(candidateNodeIds, nodeMap);
    const selectedId = sortedCandidates[0];
    if (!selectedId) {
      missingSteps.push({
        stepLabel: MAINLINE_LABELS[expectedType] || expectedType,
        expectedType,
        fromNodeId: cursorId,
      });
      continue;
    }

    nodeIds.push(selectedId);
    cursorId = selectedId;
  }

  return {
    nodeIds,
    missingSteps,
  };
}

export function buildPrintNodeScope(mainlineNodeIds: string[], manuallyExpandedNodeIds: string[]): string[] {
  const scope = new Set<string>();
  mainlineNodeIds.forEach((id) => scope.add(id));
  manuallyExpandedNodeIds.forEach((id) => scope.add(id));
  return Array.from(scope);
}

export interface PrintableAttachment {
  id?: string;
  fileUrl?: string;
}

export function dedupeAttachments<T extends PrintableAttachment>(attachments: T[]): T[] {
  const deduped = new Map<string, T>();
  attachments.forEach((attachment) => {
    const key = attachment.id || attachment.fileUrl;
    if (!key) return;
    if (!deduped.has(key)) {
      deduped.set(key, attachment);
    }
  });
  return Array.from(deduped.values());
}
