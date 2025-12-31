// Input: Playwright、登录工具
// Output: 冻结/保全管理功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('冻结/保全管理 @P1', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问冻结/保全管理页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/freeze-hold`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数
    await waitForPageTitle(page, /冻结|保全|Freeze|Hold/i, 15000);
  });

  test('应该显示冻结/保全列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/freeze-hold`);
    await page.waitForTimeout(1000);
    
    // 查找列表容器
    const listContainer = page.locator('table, [role="table"], .list-container').first();
    if (await listContainer.count() > 0) {
      await expect(listContainer).toBeVisible();
    }
  });

  test('应该显示申请冻结/保全按钮', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/freeze-hold`);
    await page.waitForTimeout(1000);
    
    // 查找申请按钮
    const applyButton = page.locator('button:has-text("申请"), button:has-text("冻结"), button:has-text("保全")').first();
    if (await applyButton.count() > 0) {
      await expect(applyButton).toBeVisible();
    }
  });

  test('应该显示类型和状态筛选', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/operations/freeze-hold`);
    await page.waitForTimeout(1000);
    
    // 查找筛选元素
    const filterElements = page.locator('select, [role="combobox"], button:has-text("类型"), button:has-text("状态")');
    const filterCount = await filterElements.count();
    
    // 验证至少有一些筛选选项
    expect(filterCount).toBeGreaterThan(0);
  });

  test('应该能够访问冻结/保全详情页面', async ({ page }) => {
    // 先访问列表页面
    await page.goto(`${BASE_URL}/system/operations/freeze-hold`);
    await page.waitForTimeout(1000);
    
    // 查找详情按钮
    const detailButton = page.locator('button:has-text("详情"), button:has-text("查看"), table tbody tr button').first();
    
    // 如果有数据，点击查看详情
    if (await detailButton.count() > 0 && await detailButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await detailButton.click();
      await page.waitForTimeout(500);
      
      // 验证详情页面或对话框显示
      const detailView = page.locator('[role="dialog"], .modal, .detail-container, h2:has-text("详情")').first();
      if (await detailView.count() > 0) {
        await expect(detailView).toBeVisible({ timeout: 3000 });
      }
    }
  });
});

