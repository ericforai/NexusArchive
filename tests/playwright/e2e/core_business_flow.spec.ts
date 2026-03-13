// Input: Playwright
// Output: 核心业务流程 E2E 测试
// Pos: tests/playwright/e2e/core_business_flow.spec.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

test.describe('NexusArchive Core Business Flow', () => {
  
  test('Voucher Ingestion and Lifecycle Flow', async ({ page }) => {
    await login(page);

    // 1. 上传凭证
    await test.step('Upload Voucher', async () => {
      await page.goto(`${BASE_URL}/system/collection/upload`);
      const fileInput = page.locator('input[type="file"]');
      const testFile = path.resolve('src/data/archives/F001/2025/10Y/AC01/V-202511-001/content/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf');
      await fileInput.setInputFiles(testFile);
      
      // 等待上传结果
      await expect(page.getByText(/上传成功|成功上传/)).toBeVisible({ timeout: 15000 });
    });

    // 2. 看板视图管理
    await test.step('Manage in Kanban View', async () => {
      await page.goto(`${BASE_URL}/system/pre-archive/pool?view=kanban`);
      await expect(page.getByText(/记账凭证库|凭证池/)).toBeVisible();
      
      // 检查“待检测”列是否有数据
      const pendingColumn = page.locator('.kanban-column').filter({ hasText: /待检测|PENDING/ });
      await expect(pendingColumn).toBeVisible();
      
      // 点击第一个卡片查看详情
      const firstCard = pendingColumn.locator('.kanban-card').first();
      await firstCard.click();
      
      // 验证抽屉打开
      await expect(page.locator('.archive-detail-drawer')).toBeVisible();
      await page.locator('.archive-detail-drawer .ant-drawer-close').click();
    });

    // 3. 执行检测与状态流转
    await test.step('Compliance Check', async () => {
      // 在看板执行操作
      const pendingColumn = page.locator('.kanban-column').filter({ hasText: /待检测|PENDING/ });
      const recheckBtn = pendingColumn.getByRole('button', { name: /执行检测|重新检测/ });
      
      if (await recheckBtn.isVisible()) {
        await recheckBtn.click();
        // 等待检测结果通知
        await expect(page.getByText(/检测完成|开始检测|正在检测/)).toBeVisible();
      }
    });

    // 4. 全局搜索验证
    await test.step('Global Search', async () => {
      const searchInput = page.locator('[data-testid=global-search-input]');
      if (await searchInput.isVisible()) {
        await searchInput.fill('餐饮'); // 使用部分关键字而非全名，提高鲁棒性
        await page.keyboard.press('Enter');
        await expect(page.getByText(/搜索结果|餐饮|凭证/i).first()).toBeVisible();
      }
    });
  });

  test('Relationship Graph Exploration', async ({ page }) => {
    await login(page);
    
    await test.step('Navigate to Relationship Query', async () => {
      await page.goto(`${BASE_URL}/system/archive/relations`);
      await expect(page.getByText(/档案关联关系查询|关联图谱/)).toBeVisible();
    });

    await test.step('Search and View Graph', async () => {
      const searchInput = page.getByPlaceholder(/搜索凭证编号|请输入/);
      await searchInput.fill('seed-voucher-001'); // 使用种子数据 ID
      await page.keyboard.press('Enter');
      
      // 验证图谱容器是否渲染
      const graphContainer = page.locator('.relationship-graph-container').or(page.locator('#relationship-graph'));
      await expect(graphContainer).toBeVisible({ timeout: 15000 });
      
      // 检查是否有节点
      const nodes = graphContainer.locator('circle, rect, .react-flow__node');
      await expect(nodes.first()).toBeVisible();
    });
  });
});
