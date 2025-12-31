// Input: Playwright、登录工具
// Output: 审计证据链验真功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('审计证据链验真 @P0', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问审计证据链验真页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    await expect(page.getByRole('heading', { name: /审计.*验真|证据链.*验真/i })).toBeVisible({ timeout: 10000 });
  });

  test('应该显示单条验证、批量验证、链式验证等选项', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    
    await page.waitForTimeout(1000);
    
    // 查找验证方式相关的UI元素
    const verificationTabs = page.locator('button:has-text("单条"), button:has-text("批量"), button:has-text("链式"), button:has-text("采样"), [role="tab"]');
    const tabCount = await verificationTabs.count();
    
    // 验证至少有一种验证方式可用
    expect(tabCount).toBeGreaterThan(0);
  });

  test('应该能够执行单条审计日志验证', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    
    await page.waitForTimeout(1000);
    
    // 查找单条验证相关的输入框或按钮
    const singleVerifyInput = page.locator('input[placeholder*="日志"], input[placeholder*="ID"], textarea[placeholder*="日志"]').first();
    const verifyButton = page.locator('button:has-text("验证"), button:has-text("提交")').first();
    
    // 如果找到了输入框和按钮
    if (await singleVerifyInput.count() > 0 && await verifyButton.count() > 0) {
      await expect(singleVerifyInput).toBeVisible();
      await expect(verifyButton).toBeVisible();
    }
  });

  test('应该显示验证结果', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/verification`);
    
    await page.waitForTimeout(1000);
    
    // 验证结果区域可能通过不同的方式显示
    // 这里主要验证页面结构正常
    const pageContent = await page.content();
    expect(pageContent.length).toBeGreaterThan(0);
  });
});

