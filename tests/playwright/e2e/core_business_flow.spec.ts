// Input: Playwright
// Output: 核心业务流程 E2E 测试
// Pos: tests/playwright/e2e/core_business_flow.spec.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect, Page } from '@playwright/test';
import path from 'path';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const USERNAME = process.env.PW_USER ?? 'admin';
const PASSWORD = process.env.PW_PASS ?? 'admin123';

const login = async (page: Page) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', USERNAME);
  await page.fill('[data-testid=login-password]', PASSWORD);
  await page.click('[data-testid=login-submit]');
  await page.waitForURL(url => url.pathname.startsWith('/system'));
};

test.describe('NexusArchive Core Business Flow', () => {
  test('Voucher Ingestion and Lifecycle Flow', async ({ page }) => {
    await login(page);
    await test.step('Upload Voucher', async () => {
      await page.goto(`${BASE_URL}/system/collection/upload`);
      const fileInput = page.locator('input[type="file"]');
      const testFile = path.resolve('src/data/archives/F001/2025/10Y/AC01/V-202511-001/content/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf');
      await fileInput.setInputFiles(testFile);
      await expect(page.getByText(/上传成功|成功上传/)).toBeVisible({ timeout: 15000 });
    });

    await test.step('Global Search', async () => {
      const searchInput = page.locator('[data-testid=global-search-input]');
      if (await searchInput.isVisible()) {
        await searchInput.fill('餐饮');
        await page.keyboard.press('Enter');
        await expect(page.getByText(/搜索结果|餐饮|凭证/i).first()).toBeVisible();
      }
    });
  });
});
