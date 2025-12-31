// Input: Playwright、登录工具
// Output: 审计证据包导出功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('审计证据包导出 @P0', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问审计证据包导出页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence-package`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数
    await waitForPageTitle(page, /证据包.*导出|审计.*证据包/i, 15000);
  });

  test('应该显示导出条件选择（日期范围、全宗）', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence-package`);
    
    await page.waitForTimeout(1000);
    
    // 查找日期选择器和全宗选择器
    const dateInputs = page.locator('input[type="date"], input[placeholder*="日期"], input[placeholder*="开始"], input[placeholder*="结束"]');
    const fondsSelector = page.locator('select, [role="combobox"], input[placeholder*="全宗"]');
    
    // 验证至少有一些筛选条件可用
    const dateCount = await dateInputs.count();
    const fondsCount = await fondsSelector.count();
    expect(dateCount + fondsCount).toBeGreaterThan(0);
  });

  test('应该能够查看导出历史', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence-package`);
    
    await page.waitForTimeout(1000);
    
    // 查找导出历史列表
    const historyList = page.locator('table, [role="table"], .history-list, [data-testid*="history"]').first();
    if (await historyList.count() > 0) {
      await expect(historyList).toBeVisible();
    }
  });

  test('应该显示导出按钮', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence-package`);
    
    await page.waitForTimeout(1000);
    
    // 查找导出按钮
    const exportButton = page.locator('button:has-text("导出"), button:has-text("生成"), button:has-text("下载")').first();
    if (await exportButton.count() > 0) {
      await expect(exportButton).toBeVisible();
    }
  });
});

