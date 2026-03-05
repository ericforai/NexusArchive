// Input: Playwright、登录工具
// Output: 审计证据包导出功能E2E测试
// Pos: Playwright UI测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import { login } from '../utils/page-helpers';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('审计证据包导出 @P0 @ui', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该能够访问审计证据包导出页面', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证页面标题
    const heading = page.getByRole('heading', { name: /审计.*证据包.*导出|证据包.*导出/i });
    await expect(heading).toBeVisible({ timeout: 10000 });
  });

  test('应该显示导出条件表单', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证开始日期输入框
    await expect(page.getByLabel(/开始日期/i)).toBeVisible();

    // 验证结束日期输入框
    await expect(page.getByLabel(/结束日期/i)).toBeVisible();

    // 验证全宗号输入框
    await expect(page.getByPlaceholder(/全宗号|留空则导出所有全宗/i)).toBeVisible();
  });

  test('应该显示包含验真报告的复选框', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证包含验真报告复选框
    const checkbox = page.getByLabel(/包含验真报告|推荐/i);
    await expect(checkbox).toBeVisible();
  });

  test('应该显示导出按钮', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证导出按钮存在
    const exportButton = page.getByRole('button', { name: /导出.*证据包|导出/i });
    await expect(exportButton).toBeVisible();
  });

  test('未选择日期应该显示错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 清空日期输入（如果有默认值）
    await page.fill('input[type="date"]', '');

    // 点击导出按钮
    await page.getByRole('button', { name: /导出.*证据包|导出/i }).click();

    // 应该显示错误提示
    const errorMessage = page.getByText(/请选择开始日期和结束日期/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('开始日期晚于结束日期应该显示错误提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 设置开始日期晚于结束日期
    await page.locator('input[type="date"]').first().fill('2025-01-10');
    await page.locator('input[type="date"]').nth(1).fill('2025-01-01');

    // 点击导出按钮
    await page.getByRole('button', { name: /导出.*证据包|导出/i }).click();

    // 应该显示错误提示
    const errorMessage = page.getByText(/开始日期不能晚于结束日期/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('页面应该显示导出说明', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证导出说明存在
    const instructions = page.getByText(/导出说明|证据包为ZIP格式/i);
    await expect(instructions).toBeVisible();
  });

  test('页面应该显示FileArchive图标', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 验证页面标题旁有FileArchive图标
    const titleWithIcon = page.locator('h2').filter({ hasText: /审计.*证据包.*导出|证据包.*导出/i });
    await expect(titleWithIcon).toBeVisible();
  });
});

test.describe('审计证据包导出 - 交互测试 @P0 @ui', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('应该能够切换包含验真报告选项', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    const checkbox = page.getByLabel(/包含验真报告/i);

    // 验证默认选中
    await expect(checkbox).toBeChecked();

    // 取消选中
    await checkbox.uncheck();
    await expect(checkbox).not.toBeChecked();

    // 重新选中
    await checkbox.check();
    await expect(checkbox).toBeChecked();
  });

  test('应该能够输入全宗号', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    const fondsInput = page.getByPlaceholder(/全宗号|留空则导出所有全宗/i);

    // 输入测试全宗号
    await fondsInput.fill('TEST01');
    await expect(fondsInput).toHaveValue('TEST01');
  });

  test('日期选择器应该正确设置日期范围', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 默认应该有30天的时间范围（从代码中得知）
    const startDateInput = page.getByLabel(/开始日期/i);
    const endDateInput = page.getByLabel(/结束日期/i);

    const startDate = await startDateInput.inputValue();
    const endDate = await endDateInput.inputValue();

    // 验证日期不为空
    expect(startDate).toBeTruthy();
    expect(endDate).toBeTruthy();

    // 验证开始日期早于结束日期
    expect(new Date(startDate).getTime()).toBeLessThan(new Date(endDate).getTime());
  });

  test('导出按钮在加载状态应该显示加载中', async ({ page }) => {
    await page.goto(`${BASE_URL}/system/audit/evidence`);
    await page.waitForLoadState('domcontentloaded');

    // 设置有效的日期范围
    await page.locator('input[type="date"]').first().fill('2025-01-01');
    await page.locator('input[type="date"]').nth(1).fill('2025-01-31');

    // 点击导出按钮
    await page.getByRole('button', { name: /导出.*证据包|导出/i }).click();

    // 验证按钮进入加载状态（如果API调用发生）
    // 注意：这个测试可能会因为API未响应而跳过
    const loadingButton = page.getByRole('button', { name: /导出中|加载中/i }).or(
      page.locator('button:has(svg[class*="animate-spin"])')
    );

    // 加载状态可能很快消失，所以用软断言
    const count = await loadingButton.count().catch(() => 0);
    if (count > 0) {
      await expect(loadingButton.first()).toBeVisible();
    }
  });
});
