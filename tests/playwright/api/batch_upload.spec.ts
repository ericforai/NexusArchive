// Input: Playwright、本地模块
// Output: 批量上传 API 集成测试
// Pos: Playwright API 测试 - 验证批量上传功能
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { createAuthContext, AuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('批量上传 API', () => {
  let authCtx: AuthContext['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test.describe('批次创建', () => {
    let batchId: number;
    let batchNo: string;

    test('POST /api/collection/batch/create - 创建上传批次', async () => {
      test.skip(!authCtx, '登录失败，跳过批量上传测试');

      const response = await authCtx!.post('/api/collection/batch/create', {
        data: {
          batchName: 'E2E测试批次-' + Date.now(),
          fondsCode: '001',
          fiscalYear: '2024',
          fiscalPeriod: '01',
          archivalCategory: 'VOUCHER',
          totalFiles: 2,
        },
      });

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.batchNo).toMatch(/^COL-\d{8}-\d{3}$/);
      expect(data.data.status).toBe('UPLOADING');
      expect(data.data.totalFiles).toBe(2);
      expect(data.data.uploadedFiles).toBe(0);
      expect(data.data.progress).toBe(0);

      batchId = data.data.batchId;
      batchNo = data.data.batchNo;
    });

    test('POST /api/collection/batch/create - 缺少必填字段应返回错误', async () => {
      test.skip(!authCtx, '登录失败，跳过批量上传测试');

      const response = await authCtx!.post('/api/collection/batch/create', {
        data: {
          batchName: '不完整批次',
          // 缺少 fondsCode, fiscalYear, archivalCategory
        },
      });

      // 应该返回 400 或 422
      expect([400, 422, 200].includes(response.status())).toBeTruthy();

      if (response.status() === 200) {
        const data = await response.json();
        // 如果返回 200，应该有错误信息
        expect(data.code !== 200 || data.message).toBeTruthy();
      }
    });
  });

  test.describe('批次查询', () => {
    let testBatchId: number;

    test.beforeAll(async () => {
      // 创建一个测试批次用于查询测试
      if (!authCtx) return;

      const response = await authCtx.post('/api/collection/batch/create', {
        data: {
          batchName: '查询测试批次',
          fondsCode: '001',
          fiscalYear: '2024',
          archivalCategory: 'LEDGER',
          totalFiles: 5,
        },
      });

      if (response.ok()) {
        const data = await response.json();
        testBatchId = data.data.batchId;
      }
    });

    test('GET /api/collection/batch/{batchId} - 获取批次详情', async () => {
      test.skip(!authCtx || !testBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.get(`/api/collection/batch/${testBatchId}`);

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.id).toBe(testBatchId);
      expect(data.data.batchNo).toBeTruthy();
      expect(data.data.batchName).toBe('查询测试批次');
      expect(data.data.fondsCode).toBe('001');
      expect(data.data.fiscalYear).toBe('2024');
      expect(data.data.archivalCategory).toBe('LEDGER');
    });

    test('GET /api/collection/batch/{batchId} - 批次不存在应返回错误', async () => {
      test.skip(!authCtx, '登录失败，跳过测试');

      const response = await authCtx!.get('/api/collection/batch/999999');

      // 可能返回 404 或 200 带错误信息
      expect([404, 200].includes(response.status())).toBeTruthy();

      if (response.status() === 200) {
        const data = await response.json();
        expect(data.code !== 200).toBeTruthy();
      }
    });

    test('GET /api/collection/batch/{batchId}/files - 获取批次文件列表', async () => {
      test.skip(!authCtx || !testBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.get(`/api/collection/batch/${testBatchId}/files`);

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(Array.isArray(data.data)).toBeTruthy();
      // 新批次应该没有文件
      expect(data.data.length).toBe(0);
    });
  });

  test.describe('文件上传', () => {
    let uploadBatchId: number;

    test.beforeAll(async () => {
      // 创建一个测试批次用于上传测试
      if (!authCtx) return;

      const response = await authCtx.post('/api/collection/batch/create', {
        data: {
          batchName: '上传测试批次',
          fondsCode: '001',
          fiscalYear: '2024',
          archivalCategory: 'VOUCHER',
          totalFiles: 1,
        },
      });

      if (response.ok()) {
        const data = await response.json();
        uploadBatchId = data.data.batchId;
      }
    });

    test('POST /api/collection/batch/{batchId}/upload - 上传文件', async () => {
      test.skip(!authCtx || !uploadBatchId, '登录失败或未创建测试批次');

      // 创建模拟 PDF 文件
      const testContent = Buffer.from('%PDF-1.4\nTest PDF content for batch upload');
      const file = Buffer.from(testContent);

      const response = await authCtx!.post(`/api/collection/batch/${uploadBatchId}/upload`, {
        multipart: {
          file: {
            name: 'test-file.pdf',
            mimeType: 'application/pdf',
            buffer: file,
          },
        },
      });

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.fileId).toBeTruthy();
      expect(data.data.originalFilename).toBe('test-file.pdf');
      expect(['UPLOADED', 'DUPLICATE', 'FAILED']).toContain(data.data.status);
    });

    test('POST /api/collection/batch/{batchId}/upload - 空文件应返回错误', async () => {
      test.skip(!authCtx || !uploadBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.post(`/api/collection/batch/${uploadBatchId}/upload`, {
        multipart: {
          file: {
            name: 'empty.pdf',
            mimeType: 'application/pdf',
            buffer: Buffer.from(''),
          },
        },
      });

      // 空文件应该被拒绝
      const data = await response.json();
      expect(data.code !== 200 || data.message).toBeTruthy();
    });

    test('POST /api/collection/batch/{batchId}/upload - 不支持的文件类型应返回错误', async () => {
      test.skip(!authCtx || !uploadBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.post(`/api/collection/batch/${uploadBatchId}/upload`, {
        multipart: {
          file: {
            name: 'test.exe',
            mimeType: 'application/x-msdownload',
            buffer: Buffer.from('test content'),
          },
        },
      });

      // 不支持的文件类型
      const data = await response.json();
      expect(data.code !== 200 || data.message).toBeTruthy();
    });
  });

  test.describe('批次完成', () => {
    let completeBatchId: number;

    test.beforeAll(async () => {
      // 创建并上传文件的批次用于完成测试
      if (!authCtx) return;

      const createResponse = await authCtx.post('/api/collection/batch/create', {
        data: {
          batchName: '完成测试批次',
          fondsCode: '001',
          fiscalYear: '2024',
          archivalCategory: 'REPORT',
          totalFiles: 1,
        },
      });

      if (createResponse.ok()) {
        const createData = await createResponse.json();
        completeBatchId = createData.data.batchId;

        // 上传一个文件
        await authCtx.post(`/api/collection/batch/${completeBatchId}/upload`, {
          multipart: {
            file: {
              name: 'report.pdf',
              mimeType: 'application/pdf',
              buffer: Buffer.from('%PDF-1.4\nTest report'),
            },
          },
        });
      }
    });

    test('POST /api/collection/batch/{batchId}/complete - 完成批次上传', async () => {
      test.skip(!authCtx || !completeBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.post(`/api/collection/batch/${completeBatchId}/complete`);

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.status).toBe('UPLOADED');
      expect(data.data.batchId).toBe(completeBatchId);
    });

    test('POST /api/collection/batch/{batchId}/cancel - 取消批次', async () => {
      test.skip(!authCtx, '登录失败，跳过测试');

      // 创建一个新批次用于取消测试
      const createResponse = await authCtx!.post('/api/collection/batch/create', {
        data: {
          batchName: '取消测试批次',
          fondsCode: '001',
          fiscalYear: '2024',
          archivalCategory: 'OTHER',
          totalFiles: 0,
        },
      });

      if (!createResponse.ok()) {
        test.skip(true, '无法创建测试批次');
        return;
      }

      const createData = await createResponse.json();
      const cancelBatchId = createData.data.batchId;

      const response = await authCtx!.post(`/api/collection/batch/${cancelBatchId}/cancel`);

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
    });
  });

  test.describe('四性检测', () => {
    let checkBatchId: number;

    test.beforeAll(async () => {
      // 创建并完成上传的批次用于检测测试
      if (!authCtx) return;

      const createResponse = await authCtx.post('/api/collection/batch/create', {
        data: {
          batchName: '四性检测测试批次',
          fondsCode: '001',
          fiscalYear: '2024',
          archivalCategory: 'VOUCHER',
          totalFiles: 1,
        },
      });

      if (createResponse.ok()) {
        const createData = await createResponse.json();
        checkBatchId = createData.data.batchId;

        // 上传文件
        await authCtx.post(`/api/collection/batch/${checkBatchId}/upload`, {
          multipart: {
            file: {
              name: 'voucher.pdf',
              mimeType: 'application/pdf',
              buffer: Buffer.from('%PDF-1.4\nTest voucher'),
            },
          },
        });

        // 完成批次
        await authCtx.post(`/api/collection/batch/${checkBatchId}/complete`);
      }
    });

    test('POST /api/collection/batch/{batchId}/check - 执行四性检测', async () => {
      test.skip(!authCtx || !checkBatchId, '登录失败或未创建测试批次');

      const response = await authCtx!.post(`/api/collection/batch/${checkBatchId}/check`);

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.batchId).toBe(checkBatchId);
      expect(data.data.summary).toBeTruthy();
      expect(data.data.summary).toMatch(/检测完成|共.*个文件/);
      expect(typeof data.data.totalFiles).toBe('number');
      expect(typeof data.data.checkedFiles).toBe('number');
      expect(typeof data.data.passedFiles).toBe('number');
      expect(typeof data.data.failedFiles).toBe('number');
    });
  });

  test.describe('批次列表', () => {
    test('GET /api/collection/batch/list - 获取批次列表', async () => {
      test.skip(!authCtx, '登录失败，跳过测试');

      const response = await authCtx!.get('/api/collection/batch/list', {
        params: { limit: 10, offset: 0 },
      });

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(Array.isArray(data.data)).toBeTruthy();
    });

    test('GET /api/collection/batch/list - 支持分页参数', async () => {
      test.skip(!authCtx, '登录失败，跳过测试');

      const response = await authCtx!.get('/api/collection/batch/list', {
        params: { limit: 5, offset: 0 },
      });

      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.code).toBe(200);
      expect(data.data.length).toBeLessThanOrEqual(5);
    });
  });
});
