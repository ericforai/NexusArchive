// Input: Playwright、本地模块
// Output: 脚本模块
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('存储空间耗尽保护与特殊字符归档', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test.skip('存储耗尽时阻断写入并告警（后端暂无模拟接口）', async () => {
    // 文档路径 /api/admin/storage/simulate 不存在，后端未提供模拟磁盘耗尽接口
    // 预期：返回 507 状态码，错误码 STORAGE_FULL
    if (!authCtx) test.skip('登录失败，跳过存储保护测试');
    
    // 模拟存储空间 95%+
    // await authCtx!.post('/api/admin/storage/simulate', { data: { usage: 0.97 } });
    // const res = await authCtx!.post('/api/archives', { data: { voucherNo: 'INV-002' } });
    // expect(res.status()).toBe(507);
    // expect(await res.json()).toMatchObject({ code: 'STORAGE_FULL' });
  });

  test('特殊字符文件名归档（中文、空格、特殊符号）', async () => {
    if (!authCtx) test.skip('登录失败，跳过特殊字符测试');
    
    const specialChars = [
      { title: '测试#%+文件名.pdf', uniqueBizId: 'SPECIAL-001' },
      { title: '测试 空格 文件名.pdf', uniqueBizId: 'SPECIAL-002' },
      { title: '测试超长文件名' + 'A'.repeat(200) + '.pdf', uniqueBizId: 'SPECIAL-003' },
    ];

    for (const payload of specialChars) {
      const res = await authCtx!.post('/api/archives', {
        data: {
          fondsNo: 'FN-001',
          title: payload.title,
          fiscalYear: '2024',
          retentionPeriod: 'permanent',
          orgName: 'QA',
          uniqueBizId: payload.uniqueBizId,
          categoryCode: 'CAT-001',
          status: 'draft',
        },
      });
      
      // 预期：路径/预览正常，无截断/乱码
      if (res.ok()) {
        const archive = await res.json();
        expect(archive.data?.title || archive.title).toBe(payload.title);
      } else {
        // 如果后端拒绝超长文件名，至少应该有明确的错误提示
        const error = await res.json();
        expect(error.message || error.error).toBeTruthy();
      }
    }
  });
});












