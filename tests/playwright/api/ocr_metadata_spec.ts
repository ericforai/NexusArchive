import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

test.describe('OCR 置信度阈值与回退人工校正', () => {
  let authCtx: Awaited<ReturnType<typeof createAuthContext>>['context'] | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    authCtx = auth?.context ?? null;
  });

  test.afterAll(async () => {
    await authCtx?.dispose();
  });

  test.skip('OCR 准确率：多版式发票采样识别（识别率≥95%）', async () => {
    // 预期：识别率≥95%，低置信度需人工校正入口
    if (!authCtx) test.skip('登录失败，跳过 OCR 测试');
    
    // 1. 上传多种版式发票
    // 2. 触发 OCR 识别
    // 3. 验证识别准确率
    // 4. 验证低置信度记录有校正入口
  });

  test.skip('异常版式回退：模糊/旋转影像', async () => {
    // 预期：回退人工录入，不阻断归档
    if (!authCtx) test.skip('登录失败，跳过异常版式测试');
    
    // 1. 上传模糊/旋转影像
    // 2. 触发 OCR
    // 3. 验证回退到人工录入流程
    // 4. 验证归档流程不中断
  });
});











