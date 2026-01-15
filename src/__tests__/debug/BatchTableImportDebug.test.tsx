import { describe, it, expect } from 'vitest';

describe('BatchTable Import Debug', () => {
  it('should import BatchTable', async () => {
    const { BatchTable } = await import('@/pages/operations/archive-batch/components');
    expect(BatchTable).toBeDefined();
  });

  it('should import operations components', async () => {
    const ops = await import('@/components/operations');
    console.log('Operations exports:', Object.keys(ops));
    expect(ops.BatchOperationBar).toBeDefined();
    expect(ops.BatchApprovalDialog).toBeDefined();
    expect(ops.BatchResultModal).toBeDefined();
  });
});
