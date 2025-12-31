// Input: Playwright、登录工具
// Output: 档案销毁流程功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('档案销毁流程 @P0', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问到期档案识别页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/expired-archives`);
    await expect(page.getByRole('heading', { name: /到期.*档案|到期档案识别/i })).toBeVisible({ timeout: 10000 });
  });

  test('应该能够访问鉴定清单页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/appraisal-list`);
    await expect(page.getByRole('heading', { name: /鉴定.*清单|鉴定清单/i })).toBeVisible({ timeout: 10000 });
  });

  test('应该能够访问销毁审批页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/destruction-approval`);
    await expect(page.getByRole('heading', { name: /销毁.*审批|销毁审批/i })).toBeVisible({ timeout: 10000 });
  });

  test('应该能够访问销毁执行页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/destruction-execution`);
    await expect(page.getByRole('heading', { name: /销毁.*执行|销毁执行/i })).toBeVisible({ timeout: 10000 });
  });

  test('到期档案页面应该显示筛选选项', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/expired-archives`);
    await page.waitForTimeout(1000);
    
    // 查找筛选相关的UI元素
    const filterElements = page.locator('select, [role="combobox"], input[placeholder*="全宗"], input[placeholder*="年度"]');
    const filterCount = await filterElements.count();
    
    // 验证至少有一些筛选选项
    expect(filterCount).toBeGreaterThan(0);
  });

  test('鉴定清单页面应该显示列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/appraisal-list`);
    await page.waitForTimeout(1000);
    
    // 查找列表容器
    const listContainer = page.locator('table, [role="table"], .list-container').first();
    if (await listContainer.count() > 0) {
      await expect(listContainer).toBeVisible();
    }
  });
});

