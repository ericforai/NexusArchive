// Input: Playwright、本地模块
// Output: 测试用例
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('ERP 对接', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test('ERP 通道列表可访问', async () => {
    if (!authCtx) test.skip('登录失败，跳过 ERP 用例');
    const channels = await authCtx!.get('/api/erp/scenario/channels');
    expect(channels.ok()).toBeTruthy();
    const body = await channels.json();
    expect(Array.isArray(body.data ?? body)).toBeTruthy();
  });

  test('ERP 场景同步触发（需预置场景ID，否则跳过）', async () => {
    if (!authCtx) test.skip('登录失败，跳过 ERP 用例');
    const channels = await authCtx!.get('/api/erp/scenario/channels');
    const body = await channels.json();
    const firstScenarioId = (body.data ?? body)?.[0]?.id;
    if (!firstScenarioId) {
      test.skip('无可用 ERP 场景 ID，跳过同步触发');
    }
    const syncRes = await authCtx!.post(`/api/erp/scenario/${firstScenarioId}/sync`);
    expect(syncRes.ok()).toBeTruthy();
  });

  test.skip('ERP 对接幂等+超时重试+防重复', async () => {
    // 预期：自动重试，无丢单，无重复档案
    if (!authCtx) test.skip('登录失败，跳过 ERP 幂等测试');
    
    // 1. 模拟网络抖动（需要后端支持或使用代理）
    // 2. 发送重复的同步请求
    // 3. 验证仅一份档案，有去重标记
  });

  test.skip('字段映射正确：ERP 凭证号/金额/税率映射', async () => {
    // 预期：映射准确，无错位
    if (!authCtx) test.skip('登录失败，跳过字段映射测试');
    
    // 1. 从 ERP 同步凭证
    // 2. 验证字段映射正确
  });
});
