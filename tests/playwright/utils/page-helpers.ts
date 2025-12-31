// Input: Playwright Page类型
// Output: 页面操作辅助函数
// Pos: 测试工具 - 页面操作辅助函数
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { Page, expect } from '@playwright/test';

const DEFAULT_USERNAME = process.env.PW_USER ?? 'admin';
const DEFAULT_PASSWORD = process.env.PW_PASS ?? 'admin123';
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

/**
 * 登录辅助函数 - 优化版本，增加重试机制和更智能的等待策略
 */
export async function login(page: Page, username = DEFAULT_USERNAME, password = DEFAULT_PASSWORD): Promise<void> {
  await page.goto(`${BASE_URL}/system/login`, { waitUntil: 'domcontentloaded' });
  
  // 等待登录表单可见
  await page.waitForSelector('[data-testid=login-username]', { timeout: 15000, state: 'visible' });
  await page.waitForSelector('[data-testid=login-password]', { timeout: 15000, state: 'visible' });
  
  // 填写登录表单
  await page.fill('[data-testid=login-username]', username);
  await page.fill('[data-testid=login-password]', password);
  
  // 点击登录按钮
  await page.click('[data-testid=login-submit]');
  
  // 使用更智能的等待策略
  try {
    // 等待URL变化，使用较长的超时时间
    await page.waitForURL(url => url.pathname !== '/system/login', { timeout: 25000, waitUntil: 'domcontentloaded' });
    
    // URL变化后，等待一小段时间确保页面开始加载
    await page.waitForTimeout(500);
    
    // 验证登录是否成功（检查是否还在登录页）
    const currentUrl = page.url();
    if (!currentUrl.includes('/system/login')) {
      // 登录成功，等待页面基本加载
      await page.waitForLoadState('domcontentloaded', { timeout: 5000 }).catch(() => {});
      return; // 登录成功，退出函数
    }
    
    // 如果还在登录页，检查是否有错误消息
    const errorElement = page.locator('[data-testid=login-error], .error, .alert-error').first();
    if (await errorElement.count() > 0) {
      const errorText = await errorElement.textContent().catch(() => '未知错误');
      throw new Error(`登录失败: ${errorText}`);
    }
    
    // 没有错误但仍在登录页，可能是响应慢，再等待一下
    await page.waitForTimeout(3000);
    const finalUrl = page.url();
    if (!finalUrl.includes('/system/login')) {
      return; // 登录成功
    }
    
    throw new Error('登录超时：仍在登录页面');
  } catch (error: any) {
    // 检查当前URL，可能实际上已经登录成功
    const currentUrl = page.url();
    if (!currentUrl.includes('/system/login')) {
      return; // 实际上已经登录成功
    }
    
    // 如果是超时错误，提供更详细的错误信息
    if (error.message && error.message.includes('Timeout')) {
      throw new Error(`登录超时：25秒内未能完成登录，当前URL: ${currentUrl}`);
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

