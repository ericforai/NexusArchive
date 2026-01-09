// Input: Playwright E2E 测试
// Output: 电子凭证池看板 E2E 测试用例
// Pos: tests/playwright/ui/pool_kanban.spec.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect, Page } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const USERNAME = process.env.PW_USER ?? 'admin';
const PASSWORD = process.env.PW_PASS ?? 'admin123';

const login = async (page: Page) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', USERNAME);
  await page.fill('[data-testid=login-password]', PASSWORD);
  await page.click('[data-testid=login-submit]');

  const loginSucceeded = await page
    .waitForURL(url => url.pathname !== '/system/login', { timeout: 10000 })
    .then(() => true)
    .catch(() => false);
  if (!loginSucceeded) {
    const errorText = await page.locator('[data-testid=login-error]').textContent().catch(() => null);
    throw new Error(`登录未成功${errorText ? `: ${errorText}` : ''}`);
  }
  await page.waitForTimeout(500);
};

test.describe('电子凭证池看板', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该能导航到看板页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 验证 URL 正确
    await page.waitForURL('**/pre-archive/pool/kanban', { timeout: 10000 });
    expect(page.url()).toContain('/pre-archive/pool/kanban');
  });

  test('应该渲染页面容器', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 等待页面加载
    await page.waitForTimeout(1000);

    // 验证页面没有崩溃
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Runtime Error');
    expect(bodyText).not.toContain('TypeError:');
    expect(bodyText).not.toContain('Cannot read');
    expect(bodyText).not.toContain('Application Error');
  });

  test('应该显示页面内容', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 等待页面加载
    await page.waitForTimeout(1000);

    // 验证页面有内容
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    expect(bodyText!.length).toBeGreaterThan(0);
  });

  test('应该能处理 API 请求', async ({ page }) => {
    // 记录 API 请求
    const apiRequests: string[] = [];

    await page.route('**/pool/**', route => {
      apiRequests.push(route.request().url());
      route.continue();
    });

    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 等待一段时间让 API 请求完成
    await page.waitForTimeout(3000);

    // 验证页面没有崩溃
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Runtime Error');
    expect(bodyText).not.toContain('TypeError:');
  });
});

test.describe('电子凭证池看板 - 响应式', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该在小屏幕上正确显示', async ({ page }) => {
    // 设置小屏幕尺寸
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 验证 URL 正确
    await page.waitForURL('**/pre-archive/pool/kanban', { timeout: 10000 });

    // 等待页面加载
    await page.waitForTimeout(1000);

    // 验证页面没有崩溃
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Runtime Error');
  });

  test('应该在非常小的屏幕上正确显示', async ({ page }) => {
    // 设置非常小的屏幕尺寸
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 验证 URL 正确
    await page.waitForURL('**/pre-archive/pool/kanban', { timeout: 10000 });

    // 等待页面加载
    await page.waitForTimeout(1000);

    // 验证页面没有崩溃
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('Runtime Error');
  });
});

test.describe('电子凭证池看板 - 错误处理', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该能处理 API 错误', async ({ page }) => {
    // 模拟 API 失败
    await page.route('**/pool/**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' }),
      });
    });

    await page.goto(`${BASE_URL}/system/pre-archive/pool/kanban`);

    // 等待页面响应
    await page.waitForTimeout(2000);

    // 验证页面没有完全崩溃
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toBeNull();
    expect(bodyText!.length).toBeGreaterThan(0);
  });
});
