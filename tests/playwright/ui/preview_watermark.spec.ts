// Input: Playwright
// Output: 预览水印链路端到端验证
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

const seedAuth = (fondsCode = 'F001') => {
  const authState = {
    state: {
      token: 'test-token',
      user: {
        id: 'u-1',
        username: 'preview-tester',
        realName: '预览测试员',
        roles: [],
        permissions: [],
      },
      isAuthenticated: true,
    },
    version: 1,
  };

  const fondsState = {
    state: {
      currentFonds: {
        id: 'fonds-1',
        fondsCode,
        fondsName: '测试全宗',
      },
    },
    version: 1,
  };

  return { authState, fondsState };
};

test('预览链路应消费 X-Watermark-* 响应头', async ({ page }) => {
  const { authState, fondsState } = seedAuth('F001');

  await page.addInitScript(({ authState, fondsState }) => {
    localStorage.setItem('nexus-auth', JSON.stringify(authState));
    localStorage.setItem('nexus-fonds', JSON.stringify(fondsState));
  }, { authState, fondsState });

  await page.route('**/api/archive/preview**', async (route) => {
    const pdfBody = '%PDF-1.4\n1 0 obj\n<<>>\nendobj\nxref\n0 1\n0000000000 65535 f \ntrailer\n<<>>\nstartxref\n0\n%%EOF';
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'application/pdf',
        'x-trace-id': 'trace-abc',
        'x-watermark-text': 'preview-tester 2025-01-01 trace-abc',
        'x-watermark-subtext': 'trace-abc | Fonds:F001',
        'x-watermark-opacity': '0.22',
        'x-watermark-rotate': '-30',
      },
      body: pdfBody,
    });
  });

  await page.goto(`${BASE_URL}/system/debug/preview-watermark`);

  const watermarkOverlay = page.getByTestId('watermark-overlay');
  await expect(watermarkOverlay).toBeVisible({ timeout: 10000 });
  await expect(watermarkOverlay).toHaveAttribute('data-watermark-text', 'preview-tester 2025-01-01 trace-abc');
  await expect(watermarkOverlay).toHaveAttribute('data-watermark-subtext', 'trace-abc | Fonds:F001');
  await expect(watermarkOverlay).toHaveAttribute('data-watermark-opacity', '0.22');
  await expect(watermarkOverlay).toHaveAttribute('data-watermark-rotate', '-30');
});
