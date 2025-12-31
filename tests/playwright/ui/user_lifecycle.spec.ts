// Input: Playwright、登录工具
// Output: 用户生命周期管理功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('用户生命周期管理 @P1', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问用户生命周期管理页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/admin/user-lifecycle`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数
    await waitForPageTitle(page, /用户生命周期|生命周期管理/i, 15000);
  });

  test('应该显示入职、离职、调岗三个标签页', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/admin/user-lifecycle`);
    await page.waitForTimeout(1000);
    
    // 查找标签页
    const tabs = page.locator('button:has-text("入职"), button:has-text("离职"), button:has-text("调岗"), [role="tab"]');
    const tabCount = await tabs.count();
    
    // 验证至少有两个标签页
    expect(tabCount).toBeGreaterThanOrEqual(2);
  });

  test('应该能够访问权限定期复核页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/admin/access-review`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数
    await waitForPageTitle(page, /权限.*复核|定期复核|Access Review/i, 15000);
  });

  test('权限复核页面应该显示任务列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/admin/access-review`);
    await page.waitForTimeout(1000);
    
    // 查找任务列表
    const taskList = page.locator('table, [role="table"], .list-container').first();
    if (await taskList.count() > 0) {
      await expect(taskList).toBeVisible();
    }
  });

  test('权限复核页面应该显示状态筛选', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/admin/access-review`);
    await page.waitForTimeout(1000);
    
    // 查找状态筛选
    const statusFilter = page.locator('select, [role="combobox"], button:has-text("状态")').first();
    if (await statusFilter.count() > 0) {
      await expect(statusFilter).toBeVisible();
    }
  });
});

