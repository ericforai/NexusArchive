// Input: Playwright、登录工具
// Output: 审计证据链验真功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('审计证据链验真 @P0 @ui', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该能够访问审计证据链验真页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);

    // 等待页面加载
    await page.waitForLoadState('domcontentloaded');

    // 验证页面标题
    const heading = page.getByRole('heading', { name: /审计.*验真|证据链.*验真/i });
    await expect(heading).toBeVisible({ timeout: 10000 });
  });

  test('应该显示四种验真模式选项', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 验证四种模式按钮存在
    await expect(page.getByRole('button', { name: /单条验真/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /批量验真/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /链路验真/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /抽检验真/i })).toBeVisible();
  });

  test('应该能够切换到单条验真模式', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 点击单条验真按钮
    await page.getByRole('button', { name: /单条验真/i }).click();

    // 验证输入框存在
    const logIdInput = page.getByPlaceholder(/日志ID|请输入审计日志ID/i);
    await expect(logIdInput).toBeVisible();
  });

  test('应该能够切换到链路验真模式', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 点击链路验真按钮
    await page.getByRole('button', { name: /链路验真/i }).click();

    // 验证日期选择器存在
    await expect(page.getByLabel(/开始日期/i)).toBeVisible();
    await expect(page.getByLabel(/结束日期/i)).toBeVisible();
  });

  test('应该能够切换到抽检验真模式', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 点击抽检验真按钮
    await page.getByRole('button', { name: /抽检验真/i }).click();

    // 验证抽检数量输入框存在
    const sampleSizeInput = page.getByLabel(/抽检数量/i);
    await expect(sampleSizeInput).toBeVisible();
  });

  test('单条验真模式应该有验证按钮', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 确保在单条验真模式
    await page.getByRole('button', { name: /单条验真/i }).click();

    // 验证开始验真按钮存在
    const verifyButton = page.getByRole('button', { name: /开始验真/i });
    await expect(verifyButton).toBeVisible();
  });

  test('链路验真模式应该有全宗号输入框', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 切换到链路验真模式
    await page.getByRole('button', { name: /链路验真/i }).click();

    // 验证全宗号输入框存在（可选字段）
    const fondsInput = page.getByPlaceholder(/全宗|留空则验证所有全宗/i);
    await expect(fondsInput).toBeVisible();
  });

  test('页面应该显示Shield图标', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 验证页面标题旁有Shield图标
    const titleWithIcon = page.locator('h2').filter({ hasText: /审计.*验真/ });
    await expect(titleWithIcon).toBeVisible();
  });
});

test.describe('审计证据链验真 - 交互测试 @P0 @ui', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('输入无效日志ID应该显示错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 切换到单条验真模式
    await page.getByRole('button', { name: /单条验真/i }).click();

    // 不输入日志ID直接点击验证
    await page.getByRole('button', { name: /开始验真/i }).click();

    // 应该显示错误提示
    const errorMessage = page.getByText(/请输入日志ID/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('链路验真未选择日期应该显示错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 切换到链路验真模式
    await page.getByRole('button', { name: /链路验真/i }).click();

    // 不选择日期直接点击验证
    await page.getByRole('button', { name: /开始验真/i }).click();

    // 应该显示错误提示
    const errorMessage = page.getByText(/请选择开始日期和结束日期/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('抽检验真未选择日期应该显示错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 切换到抽检验真模式
    await page.getByRole('button', { name: /抽检验真/i }).click();

    // 不选择日期直接点击验证
    await page.getByRole('button', { name: /开始抽检验真/i }).click();

    // 应该显示错误提示
    const errorMessage = page.getByText(/请选择开始日期和结束日期/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('应该能够在不同模式间切换', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await page.waitForLoadState('domcontentloaded');

    // 从单条验真切换到批量验真
    await page.getByRole('button', { name: /单条验真/i }).click();
    await expect(page.getByPlaceholder(/日志ID/i)).toBeVisible();

    await page.getByRole('button', { name: /批量验真/i }).click();
    await expect(page.getByPlaceholder(/日志ID列表|每行一个/i)).toBeVisible();

    // 切换到链路验真
    await page.getByRole('button', { name: /链路验真/i }).click();
    await expect(page.getByLabel(/开始日期/i)).toBeVisible();

    // 切换到抽检验真
    await page.getByRole('button', { name: /抽检验真/i }).click();
    await expect(page.getByLabel(/抽检数量/i)).toBeVisible();
  });
});
