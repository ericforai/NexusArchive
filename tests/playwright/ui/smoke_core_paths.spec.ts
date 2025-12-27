// Input: Playwright
// Output: 关键页面冒烟测试用例
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect, Page } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';
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

test('关键页面冒烟：Settings / Borrowing / Archive 弹窗', async ({ page }) => {
  await login(page);

  await test.step('Settings 入口', async () => {
    await page.goto(`${BASE_URL}/system/settings`);
    await expect(page.getByRole('heading', { name: /系统设置/ })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('link', { name: '基础设置' })).toBeVisible();
  });

  await test.step('Borrowing 列表', async () => {
    await page.goto(`${BASE_URL}/system/utilization/borrowing`);
    await expect(page.getByRole('heading', { name: /档案借阅管理/ })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('columnheader', { name: '档案题名' })).toBeVisible();

    const loading = page.getByText('正在加载借阅数据...');
    if (await loading.count()) {
      await expect(loading).toBeHidden({ timeout: 15000 });
    }
    await expect(page.getByText('加载借阅列表失败')).toHaveCount(0);
  });

  await test.step('Archive 列表弹窗', async () => {
    await page.goto(`${BASE_URL}/system/archive/vouchers`);
    await expect(page.getByRole('heading', { name: /档案管理/ })).toBeVisible({ timeout: 10000 });

    const loading = page.getByText('加载数据中...');
    if (await loading.count()) {
      await expect(loading).toBeHidden({ timeout: 20000 });
    }

    const detailTrigger = page.locator('table tbody tr td button').first();
    const emptyState = page.getByText('暂无数据');
    const outcome = await Promise.race([
      detailTrigger.waitFor({ state: 'visible', timeout: 15000 }).then(() => 'detail'),
      emptyState.waitFor({ state: 'visible', timeout: 15000 }).then(() => 'empty'),
    ]);

    if (outcome === 'empty') {
      throw new Error('档案列表为空，无法验证详情弹窗。请准备至少一条档案数据后重试。');
    }

    await detailTrigger.click();
    await expect(page.getByRole('heading', { name: '档案详情' })).toBeVisible({ timeout: 10000 });
  });
});
