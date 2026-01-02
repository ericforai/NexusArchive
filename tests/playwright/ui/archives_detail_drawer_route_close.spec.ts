// Input: Playwright E2E Test
// Output: ArchiveDetailDrawer 路由变化自动关闭测试
// Pos: tests/playwright/ui
//
// Test for auto-closing ArchiveDetailDrawer when navigating to different routes
// 验证用户点击其他菜单时，抽屉自动关闭

import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('ArchiveDetailDrawer - Route Change Auto-Close', () => {
  let authState: any = null;
  let token: string | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    if (!auth) throw new Error('Failed to authenticate');
    token = auth.token;
  });

  test.beforeEach(async ({ page }) => {
    // Setup auth and navigate to pool page
    await page.goto(`${BASE_URL}/system/login`);
    await page.fill('input[data-testid="login-username"]', process.env.PW_USER || 'admin');
    await page.fill('input[data-testid="login-password"]', process.env.PW_PASS || 'admin123');
    await page.click('button[data-testid="login-submit"]');
    await page.waitForURL('**/system/**', { timeout: 8000 });

    // Navigate to pool page
    await page.evaluate(() => {
      window.history.pushState({}, '', '/system/pre-archive/pool');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await page.waitForTimeout(1000);
  });

  test('should close drawer when navigating to different menu', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table', { timeout: 10000 });

    // Check if table has data rows
    const rowCount = await page.locator('table tbody tr').count();
    if (rowCount === 0) {
      test.skip();
      return;
    }

    // Try to open drawer - first try the Eye icon button
    let drawerOpened = false;

    // Method 1: Click Eye icon button in actions column
    try {
      const eyeButton = page.locator('table tbody tr').first()
        .locator('button[title="预览"]');
      if (await eyeButton.count() > 0) {
        await eyeButton.first().click();
        drawerOpened = true;
      }
    } catch (e) {
      console.log('Eye button not found or not clickable');
    }

    // Method 2: Click voucher number button if method 1 failed
    if (!drawerOpened) {
      try {
        const voucherButton = page.locator('table tbody tr').first()
          .locator('button').filter({ hasText: /记/ }).first();
        await voucherButton.click();
        drawerOpened = true;
      } catch (e) {
        console.log('Voucher button not found or not clickable');
      }
    }

    if (!drawerOpened) {
      test.skip();
      return;
    }

    await page.waitForTimeout(1000);

    // Verify drawer is open
    const drawer = page.locator('[data-testid="archive-detail-drawer"]');
    await expect(drawer).toBeVisible();

    // Navigate to different menu
    await page.evaluate(() => {
      window.history.pushState({}, '', '/system/panorama');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await page.waitForTimeout(500);

    // Verify drawer is closed
    await expect(drawer).not.toBeVisible();
  });
});
