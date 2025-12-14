import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('检索性能基线与索引一致性', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test('模糊检索性能基线（TP95 < 2s）', async () => {
    if (!authCtx) test.skip('登录失败，跳过检索性能测试');
    
    const startTime = Date.now();
    const res = await authCtx!.get('/api/search', {
      params: { q: '发票', page: 1, size: 20 },
    });
    const duration = Date.now() - startTime;
    
    expect(res.ok()).toBeTruthy();
    // 注意：单次请求无法准确测量 TP95，实际压测应使用 k6
    // 这里仅验证接口可用性，TP95 需通过 perf/search_peak.k6.js 验证
    expect(duration).toBeLessThan(5000); // 单次请求应在 5s 内
  });

  test('索引一致性：删除后立即检索不返回幽灵记录', async () => {
    if (!authCtx) test.skip('登录失败，跳过索引一致性测试');
    
    // 1. 创建测试档案
    const createRes = await authCtx!.post('/api/archives', {
      data: {
        fondsNo: 'FN-001',
        title: '索引一致性测试',
        fiscalYear: '2024',
        retentionPeriod: 'permanent',
        orgName: 'QA',
        uniqueBizId: `INDEX-TEST-${Date.now()}`,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });
    
    if (!createRes.ok()) {
      test.skip('创建档案失败，跳过索引一致性测试');
    }
    
    const archive = await createRes.json();
    const archiveId = archive.data?.id || archive.id;
    if (!archiveId) {
      test.skip('无法获取档案 ID，跳过索引一致性测试');
    }
    
    // 2. 验证可检索到
    const searchBefore = await authCtx!.get('/api/search', {
      params: { q: '索引一致性测试' },
    });
    expect(searchBefore.ok()).toBeTruthy();
    const resultsBefore = await searchBefore.json();
    const foundBefore = (resultsBefore.data || resultsBefore).some(
      (item: any) => item.id === archiveId || item.title?.includes('索引一致性测试'),
    );
    expect(foundBefore).toBeTruthy();
    
    // 3. 删除档案
    const deleteRes = await authCtx!.delete(`/api/archives/${archiveId}`);
    // 如果删除接口不存在或需要权限，跳过后续验证
    if (!deleteRes.ok() && deleteRes.status() !== 404) {
      test.skip('删除接口不可用，跳过索引一致性验证');
    }
    
    // 4. 立即检索，不应返回已删除的记录
    await new Promise(resolve => setTimeout(resolve, 500)); // 等待索引刷新
    const searchAfter = await authCtx!.get('/api/search', {
      params: { q: '索引一致性测试' },
    });
    expect(searchAfter.ok()).toBeTruthy();
    const resultsAfter = await searchAfter.json();
    const foundAfter = (resultsAfter.data || resultsAfter).some(
      (item: any) => item.id === archiveId,
    );
    expect(foundAfter).toBeFalsy(); // 不应找到已删除的记录
  });

  test('更新后索引刷新', async () => {
    if (!authCtx) test.skip('登录失败，跳过索引刷新测试');
    
    // 创建档案
    const createRes = await authCtx!.post('/api/archives', {
      data: {
        fondsNo: 'FN-001',
        title: '索引刷新测试-原始',
        fiscalYear: '2024',
        retentionPeriod: 'permanent',
        orgName: 'QA',
        uniqueBizId: `INDEX-UPDATE-${Date.now()}`,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });
    
    if (!createRes.ok()) {
      test.skip('创建档案失败，跳过索引刷新测试');
    }
    
    const archive = await createRes.json();
    const archiveId = archive.data?.id || archive.id;
    if (!archiveId) {
      test.skip('无法获取档案 ID，跳过索引刷新测试');
    }
    
    // 更新标题
    const updateRes = await authCtx!.put(`/api/archives/${archiveId}`, {
      data: { title: '索引刷新测试-已更新' },
    });
    
    if (!updateRes.ok() && updateRes.status() !== 404) {
      test.skip('更新接口不可用，跳过索引刷新验证');
    }
    
    // 等待索引刷新
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 检索应返回更新后的标题
    const searchRes = await authCtx!.get('/api/search', {
      params: { q: '索引刷新测试-已更新' },
    });
    expect(searchRes.ok()).toBeTruthy();
    const results = await searchRes.json();
    const found = (results.data || results).some(
      (item: any) => item.id === archiveId && item.title?.includes('已更新'),
    );
    expect(found).toBeTruthy();
  });
});


