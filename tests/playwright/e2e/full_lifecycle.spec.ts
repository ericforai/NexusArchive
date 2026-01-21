// Input: Playwright
// Output: 全生命周期 E2E 测试
// Pos: E2E 测试

import { test, expect, Page } from '@playwright/test';
import path from 'path';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const USERNAME = process.env.PW_USER ?? 'admin';
const PASSWORD = process.env.PW_PASS ?? 'admin123';

const login = async (page: Page) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', USERNAME);
  await page.fill('[data-testid=login-password]', PASSWORD);
  await page.click('[data-testid=login-submit]');
  await page.waitForURL(url => url.pathname.startsWith('/system'));
};

test.describe('Electronic File Full Lifecycle E2E', () => {
  
  test('Login -> Upload -> Check -> Pool Verification', async ({ page }) => {
    await login(page);

    // 1. 导航到批量上传
    await test.step('Navigate to Batch Upload', async () => {
      await page.goto(`${BASE_URL}/system/collection/upload`);
      await expect(page.getByText('批量上传', { exact: true })).toBeVisible({ timeout: 15000 });
    });

    // 2. 执行上传 (这里模拟 UI 操作，假设有相应的 data-testid)
    await test.step('Perform File Upload', async () => {
      // 这里的选择器需要根据实际 UI 调整
      // 由于不知道准确的选择器，我将尝试使用常用的 antd 选择器或文本
      const fileInput = page.locator('input[type="file"]');
      const testFile = path.resolve('src/data/archives/F001/2025/10Y/AC01/V-202511-001/content/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf');
      
      await fileInput.setInputFiles(testFile);
      
      // 填写批次信息 (如果是弹窗或表单)
      // 假设点击了“开始上传”
      const uploadBtn = page.getByRole('button', { name: /开始上传|确定/ });
      if (await uploadBtn.isVisible()) {
        await uploadBtn.click();
      }
      
      // 等待上传完成
      await expect(page.getByText('上传成功')).toBeVisible({ timeout: 20000 });
    });

    // 3. 触发四性检测并查看报告
    await test.step('Run Compliance Check', async () => {
      const checkBtn = page.getByRole('button', { name: /执行检测|开始检测/ });
      await checkBtn.click();
      
      // 等待检测结果
      await expect(page.getByText(/检测完成/)).toBeVisible({ timeout: 30000 });
    });

    // 4. 在预归档池中验证
    await test.step('Verify in Pre-archive Pool', async () => {
      await page.goto(`${BASE_URL}/system/pre-archive/pool`);
      await expect(page.getByText('电子凭证池')).toBeVisible();
      
      // 确认刚才上传的文件在列表中 (模糊匹配文件名)
      await expect(page.getByText('晓旻餐饮店')).toBeVisible({ timeout: 15000 });
    });
  });
});
