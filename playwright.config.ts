// Input: Playwright
// Output: Playwright 配置（CI 默认 headless）
// Pos: 构建/测试配置
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineConfig } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const CHANNEL = process.env.PW_CHANNEL;
const HEADLESS = process.env.PW_HEADLESS ? process.env.PW_HEADLESS !== 'false' : true;

export default defineConfig({
  use: {
    baseURL: BASE_URL,
    headless: HEADLESS,
    ...(CHANNEL ? { channel: CHANNEL } : {}),
  },
});
