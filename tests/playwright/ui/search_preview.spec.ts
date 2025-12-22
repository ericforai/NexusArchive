// Input: Playwright
// Output: 测试用例
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';
const USERNAME = process.env.PW_USER ?? 'admin';
const PASSWORD = process.env.PW_PASS ?? 'admin123';

test('登录后搜索档案并查看列表', async ({ page }) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', USERNAME);
  await page.fill('[data-testid=login-password]', PASSWORD);
  await page.click('[data-testid=login-submit]');

  const loginSucceeded = await page
    .waitForURL(url => url.pathname !== '/system/login', { timeout: 5000 })
    .then(() => true)
    .catch(() => false);
  if (!loginSucceeded) {
    const errorText = await page.locator('[data-testid=login-error]').textContent().catch(() => null);
    throw new Error(`登录未成功${errorText ? `: ${errorText}` : ''}`);
  }
  await page.waitForTimeout(500); // give layout a moment to settle

  // 直接进入档案列表页，避免依赖导航埋点
  await page.goto(`${BASE_URL}/system/archive`);

  const filterToggle = page.locator('[data-testid=archive-search-button]');
  if (await filterToggle.count()) {
    await filterToggle.first().click();
  }

  const searchInput = page.locator('[data-testid=archive-search-input]');
  await expect(searchInput).toBeVisible({ timeout: 5000 });
  await searchInput.fill('发票');

  const firstRow = page.locator('[data-testid^=archive-row-]').first();
  await expect(firstRow).toBeVisible({ timeout: 10000 });
});
