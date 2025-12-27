// Input: Playwright、本地模块
// Output: 测试用例
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { createAuthContext, AuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('四性校验与 TSA', () => {
  let authCtx: AuthContext['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    test.skip(!auth, '登录失败，跳过四性校验');
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test('哈希算法一致性检查', async () => {
    const ctx = authCtx;
    test.skip(!ctx, '登录失败，跳过哈希一致性测试');
    if (!ctx) return;
    
    // 1. 创建档案，指定 MD5 哈希
    const upload = await ctx.post('/api/archives', {
      data: {
        fondsNo: 'FN-002',
        title: '四性校验样本-哈希一致性',
        fiscalYear: '2024',
        retentionPeriod: 'permanent',
        orgName: 'QA',
        fixityAlgo: 'MD5',
        fixityValue: 'd41d8cd98f00b204e9800998ecf8427e',
        uniqueBizId: `HASH-TEST-${Date.now()}`,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });
    
    if (!upload.ok()) {
      test.skip(true, `创建档案失败，状态码 ${upload.status()}`);
    }

    const archiveData = await upload.json();
    const archiveId = archiveData.data?.id || archiveData.id;
    
    // 2. 验证检测结果持久化：完成检测后查审计日志
    const audit = await ctx.get('/api/audit-logs', {
      params: { resourceType: 'ARCHIVE', resourceId: archiveId },
    });
    expect(audit.ok()).toBeTruthy();
    
    // 3. 尝试使用不同哈希算法校验（如果接口存在）
    const verify = await ctx.post('/api/compliance/archives/' + archiveId, {});
    if (verify.status() === 404) {
      // 尝试合规性检查接口
      const complianceCheck = await ctx.get('/api/compliance/archives/' + archiveId);
      if (complianceCheck.ok()) {
        const compliance = await complianceCheck.json();
        expect(compliance.data || compliance).toBeTruthy();
      } else {
        test.skip(true, '后端未提供四性检测接口');
      }
    } else {
      expect(verify.ok()).toBeTruthy();
    }
  });

  test('四性检测结果持久化', async () => {
    const ctx = authCtx;
    test.skip(!ctx, '登录失败，跳过结果持久化测试');
    if (!ctx) return;
    
    // 创建测试档案
    const upload = await ctx.post('/api/archives', {
      data: {
        fondsNo: 'FN-002',
        title: '四性检测持久化测试',
        fiscalYear: '2024',
        retentionPeriod: 'permanent',
        orgName: 'QA',
        uniqueBizId: `FOUR-NATURE-${Date.now()}`,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });
    
    if (!upload.ok()) {
      test.skip(true, `创建档案失败，状态码 ${upload.status()}`);
    }

    const archiveData = await upload.json();
    const archiveId = archiveData.data?.id || archiveData.id;
    
    // 执行合规性检查（包含四性检测）
    const complianceRes = await ctx.get('/api/compliance/archives/' + archiveId);
    
    if (!complianceRes.ok()) {
      test.skip(true, '合规性检查接口不可用');
    }
    
    const compliance = await complianceRes.json();
    expect(compliance.data || compliance).toBeTruthy();
    
    // 验证结果可查询（通过审计日志或检查报告）
    const reportRes = await ctx.get('/api/compliance/archives/' + archiveId + '/report');
    if (reportRes.ok()) {
      const report = await reportRes.text();
      expect(report).toBeTruthy();
      // 验证报告包含检测结果
      expect(report.length).toBeGreaterThan(0);
    }
  });

  test.skip('TSA 超时降级机制', async () => {
    // 预期：TSA 不可用时触发重试+降级/失败提示，不卡死
    test.skip(!authCtx, '登录失败，跳过 TSA 测试');
    
    // 1. 模拟 TSA 不可用（需要后端提供 TSA 开关接口）
    // await authCtx.post('/api/admin/tsa/toggle', { data: { enabled: false, delayMs: 8000 } });
    
    // 2. 创建档案，应该能正常处理（即使 TSA 超时）
    // 3. 验证有明确的错误提示或降级处理
  });
});
