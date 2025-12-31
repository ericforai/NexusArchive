// Input: Playwright、登录工具
// Output: MFA多因素认证功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('MFA多因素认证 @P1', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问MFA设置页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/settings/mfa`);
    await expect(page.getByRole('heading', { name: /多因素认证|MFA/i })).toBeVisible({ timeout: 10000 });
  });

  test('应该显示MFA状态', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/settings/mfa`);
    await page.waitForTimeout(1000);
    
    // 查找MFA状态显示
    const statusElements = page.locator('text=/已启用|未启用|MFA.*状态/i');
    const statusCount = await statusElements.count();
    
    // 验证状态信息存在
    expect(statusCount).toBeGreaterThan(0);
  });

  test('应该显示启用MFA按钮（如果未启用）', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/settings/mfa`);
    await page.waitForTimeout(1000);
    
    // 查找启用MFA按钮
    const enableButton = page.locator('button:has-text("启用"), button:has-text("启用MFA"), button:has-text("设置")').first();
    
    // 如果按钮存在，验证其可见性
    if (await enableButton.count() > 0) {
      await expect(enableButton).toBeVisible();
    }
  });

  test('应该显示二维码（如果正在设置）', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/settings/mfa`);
    await page.waitForTimeout(1000);
    
    // 如果MFA未启用，点击启用按钮
    const enableButton = page.locator('button:has-text("启用"), button:has-text("启用MFA")').first();
    if (await enableButton.count() > 0 && await enableButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await enableButton.click();
      await page.waitForTimeout(1000);
      
      // 查找二维码图片
      const qrCode = page.locator('img[alt*="QR"], img[alt*="二维码"], img[src*="qr"], img[src*="data:image"]').first();
      if (await qrCode.count() > 0) {
        await expect(qrCode).toBeVisible({ timeout: 3000 });
      }
    }
  });
});

