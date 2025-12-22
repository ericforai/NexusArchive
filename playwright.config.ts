// Input: Playwright
// Output: Playwright 配置
// Pos: 构建/测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineConfig } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';

export default defineConfig({
  use: {
    baseURL: BASE_URL,
    // Use system Chrome to avoid headless shell permission issues; flip to true if headless is allowed.
    channel: 'chrome',
    headless: false,
  },
});
