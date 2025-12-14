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
