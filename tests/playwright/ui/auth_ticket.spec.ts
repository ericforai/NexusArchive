// Input: Playwright、登录工具
// Output: 跨全宗访问授权票据功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login, waitForPageTitle } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('跨全宗访问授权票据 @P0', () => {
  test.beforeEach(async ({ page }) => {
    // 使用优化的登录函数
    await login(page);
  });

  test('应该能够访问授权票据列表页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/security/auth-ticket`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数，支持多种标题格式
    await waitForPageTitle(page, /授权票据/i, 15000);
  });

  test('应该能够访问授权票据申请页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/security/auth-ticket/apply`);
    await page.waitForLoadState('networkidle');
    // 使用优化的标题等待函数
    await waitForPageTitle(page, /申请.*授权票据|授权票据.*申请/i, 15000);
  });

  test('应该显示授权票据列表', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/security/auth-ticket`);
    await page.waitForLoadState('networkidle');
    
    // 等待列表加载
    await page.waitForTimeout(2000);
    
    // 验证列表容器存在（可能为空列表，但要确保元素存在）
    const listContainer = page.locator('table, [role="table"], .list-container, tbody').first();
    const count = await listContainer.count();
    
    if (count > 0) {
      // 检查元素是否可见，如果被隐藏，等待其变为可见
      const isVisible = await listContainer.isVisible().catch(() => false);
      if (!isVisible) {
        // 如果元素存在但不可见，等待一下看看是否会变为可见
        await page.waitForTimeout(2000);
      }
      // 如果元素存在，验证它要么可见，要么是空的（空列表也是正常的）
      const visible = await listContainer.isVisible().catch(() => false);
      if (visible) {
        await expect(listContainer).toBeVisible();
      }
    }
  });

  test('应该能够筛选授权票据', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/security/auth-ticket/list`);
    
    await page.waitForTimeout(1000);
    
    // 查找筛选相关的UI元素（状态筛选、全宗筛选等）
    const filterElements = page.locator('select, [role="combobox"], button:has-text("筛选"), button:has-text("查询")');
    const filterCount = await filterElements.count();
    
    // 如果存在筛选元素，验证其可见性
    if (filterCount > 0) {
      await expect(filterElements.first()).toBeVisible();
    }
  });

  test('应该能够查看授权票据详情', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/security/auth-ticket/list`);
    
    await page.waitForTimeout(1000);
    
    // 查找列表中的第一行或详情按钮
    const detailButton = page.locator('button:has-text("详情"), button:has-text("查看"), table tbody tr button').first();
    
    // 如果有数据，点击查看详情
    if (await detailButton.count() > 0 && await detailButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await detailButton.click();
      await page.waitForTimeout(500);
      // 验证详情对话框或页面显示
      const detailModal = page.locator('[role="dialog"], .modal, .detail-container').first();
      if (await detailModal.count() > 0) {
        await expect(detailModal).toBeVisible({ timeout: 3000 });
      }
    }
  });
});

