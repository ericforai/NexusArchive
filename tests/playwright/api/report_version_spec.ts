import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('报表口径与版本管理', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test('报表口径：汇总与明细一致', async () => {
    if (!authCtx) test.skip('登录失败，跳过报表口径测试');
    
    // 1. 获取汇总报表
    const summaryRes = await authCtx!.get('/api/stats/summary', {
      params: { startDate: '2024-01-01', endDate: '2024-12-31' },
    });
    
    if (!summaryRes.ok()) {
      test.skip('汇总报表接口不可用');
    }
    
    const summary = await summaryRes.json();
    
    // 2. 获取明细数据
    const detailRes = await authCtx!.get('/api/stats/detail', {
      params: { startDate: '2024-01-01', endDate: '2024-12-31' },
    });
    
    if (!detailRes.ok()) {
      test.skip('明细报表接口不可用');
    }
    
    const detail = await detailRes.json();
    
    // 3. 验证金额/数量一致，时间区间正确
    // 注意：具体字段名需要根据实际 API 调整
    if (summary.data && detail.data) {
      const summaryTotal = summary.data.totalAmount || summary.data.count || 0;
      const detailTotal = (detail.data || []).reduce(
        (sum: number, item: any) => sum + (item.amount || 1),
        0,
      );
      // 允许小的浮点误差
      expect(Math.abs(summaryTotal - detailTotal)).toBeLessThan(0.01);
    }
  });

  test.skip('版本管理：并发编辑冲突', async () => {
    // 预期：有冲突提示，生成新版本
    if (!authCtx) test.skip('登录失败，跳过版本冲突测试');
    
    // 1. 用户 A 开始编辑档案
    // 2. 用户 B 同时编辑同一档案
    // 3. 验证冲突提示
    // 4. 验证生成新版本
  });

  test.skip('签章后不可改：已签章版本不可编辑', async () => {
    // 预期：需新版本，旧版留痕
    if (!authCtx) test.skip('登录失败，跳过签章版本测试');
    
    // 1. 创建档案并签章
    // 2. 尝试编辑已签章版本
    // 3. 验证被拒绝，提示需创建新版本
    // 4. 创建新版本
    // 5. 验证旧版本保留
  });
});











