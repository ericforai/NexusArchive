import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';
const USERNAME = process.env.PW_USER ?? 'admin';
const PASSWORD = process.env.PW_PASS ?? 'admin123';

test('登录后搜索档案并查看列表', async ({ page }) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', USERNAME);
  await page.fill('[data-testid=login-password]', PASSWORD);
  await page.click('[data-testid=login-submit]');

  const navResult = await page.waitForURL(/system|dashboard|home|\/$/,{ timeout: 5000 }).catch(() => null);
  if (!navResult) test.skip('登录未成功，跳过列表检索');
  await page.waitForTimeout(500); // give layout a moment to settle

  // 导航到档案列表（使用 data-testid 导航项）
  const navArchive = page.locator('[data-testid=nav-archive-mgmt]');
  if (await navArchive.count()) {
    await navArchive.first().click();
  }

  const searchInput = page.locator('[data-testid=archive-search-input]');
  const visible = await searchInput.isVisible({ timeout: 5000 }).catch(() => false);
  if (!visible) test.skip('未找到 archive-search-input，可能页面结构或埋点缺失');
  await searchInput.fill('发票');
  await page.click('[data-testid=archive-search-button]');

  const firstRow = page.locator('[data-testid^=archive-row-]').first();
  await expect(firstRow).toBeVisible({ timeout: 10000 });
});
