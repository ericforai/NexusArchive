import { describe, expect, it } from 'vitest';
import type { RelationNodeData, RelationEdgeData } from '@/types/relationGraph';
import {
  detectPaymentMainline,
  buildPrintNodeScope,
  dedupeAttachments,
} from '@/pages/utilization/relationDrilldown';

const createNode = (id: string, type: RelationNodeData['type'], date = '2025-01-01'): RelationNodeData => ({
  id,
  type,
  code: id.toUpperCase(),
  depth: 0,
  isCenter: false,
  isExpanded: false,
  date,
});

const createEdge = (from: string, to: string): RelationEdgeData => ({
  id: `${from}->${to}`,
  from,
  to,
});

describe('relationDrilldown', () => {
  it('应按付款主线识别可用链路并标记缺口', () => {
    const nodes: RelationNodeData[] = [
      createNode('voucher-1', 'voucher'),
      createNode('payment-1', 'payment'),
      createNode('contract-1', 'contract'),
      createNode('invoice-1', 'invoice'),
      createNode('receipt-1', 'receipt'),
    ];
    const edges: RelationEdgeData[] = [
      createEdge('voucher-1', 'payment-1'),
      createEdge('payment-1', 'contract-1'),
      createEdge('contract-1', 'invoice-1'),
      createEdge('invoice-1', 'receipt-1'),
    ];

    const result = detectPaymentMainline(nodes, edges, 'voucher-1');

    expect(result.nodeIds).toEqual(['voucher-1', 'payment-1', 'contract-1', 'invoice-1', 'receipt-1']);
    expect(result.missingSteps).toHaveLength(1);
    expect(result.missingSteps[0].expectedType).toBe('application');
  });

  it('应合并主线与手动展开节点用于打印范围', () => {
    const scope = buildPrintNodeScope(
      ['voucher-1', 'payment-1', 'invoice-1'],
      ['invoice-1', 'contract-1']
    );
    expect(scope).toEqual(['voucher-1', 'payment-1', 'invoice-1', 'contract-1']);
  });

  it('应按id或URL去重附件', () => {
    const deduped = dedupeAttachments([
      { id: 'A', fileUrl: '/a.pdf' },
      { id: 'A', fileUrl: '/a-copy.pdf' },
      { fileUrl: '/b.pdf' },
      { fileUrl: '/b.pdf' },
    ]);
    expect(deduped).toHaveLength(2);
    expect(deduped[0].id).toBe('A');
    expect(deduped[1].fileUrl).toBe('/b.pdf');
  });
});
