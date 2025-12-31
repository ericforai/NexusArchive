// Input: Playwright Page类型
// Output: 页面操作辅助函数
// Pos: 测试工具 - 页面操作辅助函数
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { Page, expect } from '@playwright/test';

const DEFAULT_USERNAME = process.env.PW_USER ?? 'admin';
const DEFAULT_PASSWORD = process.env.PW_PASS ?? 'admin123';
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

/**
 * 登录辅助函数 - 优化版本，增加重试和更长的等待时间
 */
export async function login(page: Page, username = DEFAULT_USERNAME, password = DEFAULT_PASSWORD): Promise<void> {
  await page.goto(`${BASE_URL}/system/login`);
  
  // 等待登录页面加载完成
  await page.waitForLoadState('networkidle');
  
  // 填写登录表单
  await page.fill('[data-testid=login-username]', username);
  await page.fill('[data-testid=login-password]', password);
  
  // 点击登录按钮
  await page.click('[data-testid=login-submit]');
  
  // 等待导航完成，使用更灵活的策略
  try {
    // 首先等待URL变化（主要等待条件）
    await page.waitForURL(url => url.pathname !== '/system/login', { timeout: 30000 });
    
    // 等待网络空闲，确保页面完全加载
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
      // 如果网络空闲超时，继续执行（可能是页面已经加载完成）
    });
    
    // 额外等待确保页面完全加载
    await page.waitForTimeout(500);
    
    // 验证登录是否成功（检查是否还在登录页）
    const currentUrl = page.url();
    if (currentUrl.includes('/system/login')) {
      // 检查是否有错误消息
      const errorElement = page.locator('[data-testid=login-error], .error, .alert-error').first();
      if (await errorElement.count() > 0) {
        const errorText = await errorElement.textContent().catch(() => '未知错误');
        throw new Error(`登录失败: ${errorText}`);
      }
      // 如果还在登录页但没有错误消息，可能是页面加载慢，再等待一下
      await page.waitForTimeout(2000);
      const finalUrl = page.url();
      if (finalUrl.includes('/system/login')) {
        throw new Error('登录超时：仍在登录页面');
      }
    }
  } catch (error) {
    // 如果是超时错误，提供更详细的错误信息
    if (error instanceof Error && error.message.includes('Timeout')) {
      throw new Error(`登录超时：30秒内未能完成登录，当前URL: ${page.url()}`);
    }
    throw error;
  }
}

/**
 * 等待页面标题可见 - 支持多种标题选择器
 */
export async function waitForPageTitle(
  page: Page,
  titleText: string | RegExp,
  timeout = 10000
): Promise<void> {
  // 尝试多种选择器策略
  const selectors = [
    `h1:has-text("${titleText}")`,
    `h2:has-text("${titleText}")`,
    `h3:has-text("${titleText}")`,
    `[role="heading"]:has-text("${titleText}")`,
    `text=/${titleText}/i`,
  ];
  
  for (const selector of selectors) {
    try {
      const element = page.locator(selector).first();
      if (await element.isVisible({ timeout: 2000 })) {
        return;
      }
    } catch (e) {
      // 继续尝试下一个选择器
      continue;
    }
  }
  
  // 如果所有选择器都失败，使用更通用的方法
  const heading = page.getByRole('heading', { name: titleText });
  await expect(heading.first()).toBeVisible({ timeout });
}

/**
 * 等待元素可见 - 支持多种等待策略
 */
export async function waitForElementVisible(
  page: Page,
  selector: string,
  timeout = 10000
): Promise<void> {
  const element = page.locator(selector).first();
  
  // 首先检查元素是否存在
  const count = await element.count();
  if (count === 0) {
    throw new Error(`元素不存在: ${selector}`);
  }
  
  // 等待元素可见
  await expect(element).toBeVisible({ timeout });
}

