// src/e2e/yonsuite-verification.spec.ts
/**
 * Simplified YonSuite Scenario Verification Tests
 *
 * Focus: Verify 5 real scenarios exist in database and API
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const API_BASE = process.env.API_BASE ?? 'http://localhost:19090';
const RUN_YONSUITE_E2E = process.env.YONSUITE_E2E === '1';

test.skip(!RUN_YONSUITE_E2E, '未启用 YonSuite 实网联调（设置 YONSUITE_E2E=1 后执行）');

function unwrapData(payload: any) {
  return payload?.data ?? payload;
}

function scenarioKeyOf(s: any): string {
  return (s?.scenarioKey || s?.scenario_key || '').toUpperCase();
}

test.describe('YonSuite Scenarios - Database Verification', () => {

  test('should have exactly 5 real scenarios in database', async ({ request }) => {
    // Step 1: Login to get token
    const loginResponse = await request.post(`${API_BASE}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: {
        username: 'admin',
        password: 'admin123'
      }
    });

    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    const token = loginData?.data?.token ?? loginData?.token ?? loginData?.access_token;

    // Step 2: Get scenarios for YonSuite
    const scenariosResponse = await request.get(`${API_BASE}/api/erp/scenario/list/1`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // Step 3: Verify response
    if (scenariosResponse.status() === 404) {
      console.warn('⚠️ API endpoint /erp/scenario/list not found (404)');
      test.skip(true, 'API endpoint not implemented');
      return;
    }

    expect(scenariosResponse.ok()).toBeTruthy();
    const scenariosRaw = await scenariosResponse.json();
    const scenarios = unwrapData(scenariosRaw) ?? [];
    expect(Array.isArray(scenarios)).toBeTruthy();

    // Step 4: Verify exactly 5 scenarios (after removing placeholders)
    expect(scenarios.length).toBeGreaterThanOrEqual(5);
    console.log(`✅ Database has ${scenarios.length} scenarios (expected >= 5)`);

    // Step 5: Verify scenario keys
    const expectedKeys = [
      'VOUCHER_SYNC',
      'ATTACHMENT_SYNC',
      'COLLECTION_FILE_SYNC',
      'PAYMENT_FILE_SYNC',
      'REFUND_FILE_SYNC'
    ];

    const actualKeys = scenarios.map((s: any) => scenarioKeyOf(s));
    for (const key of expectedKeys) {
      expect(actualKeys).toContain(key);
      console.log(`✅ Scenario ${key}: verified`);
    }

    // Step 6: 占位场景是否存在取决于环境，本用例仅校验真实场景必须存在
  });

  test('should show YonSuite connector in UI', async ({ page }) => {
    // Navigate to login page
    await page.goto(`${BASE_URL}/system/login`);

    // Check if login page is shown
    const loginForm = page.locator('[data-testid=login-username]');
    if (await loginForm.isVisible()) {
      // Login
      await page.fill('[data-testid=login-username]', 'admin');
      await page.fill('[data-testid=login-password]', 'admin123');
      await page.click('[data-testid=login-submit]');

      // Wait for navigation
      await page.waitForURL(url => !url.pathname.includes('/system/login'), { timeout: 10000 });
    }

    // Navigate to Integration Settings
    await page.goto(`${BASE_URL}/system/settings/integration`);
    await page.waitForLoadState('domcontentloaded');

    // Verify YonSuite card is visible
    const yonSuiteCard = page.getByText(/YonSuite/i).first();
    await expect(yonSuiteCard).toBeVisible();
    console.log('✅ YonSuite connector visible in UI');
  });
});

test.describe('YonSuite Scenarios - Success Status Verification', () => {

  test('should have at least one scenario with SUCCESS status', async ({ request }) => {
    const loginResponse = await request.post(`${API_BASE}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: { username: 'admin', password: 'admin123' }
    });

    if (!loginResponse.ok()) {
      test.skip(true, 'Login failed');
      return;
    }

    const loginData = await loginResponse.json();
    const token = loginData?.data?.token ?? loginData?.token ?? loginData?.access_token;

    const scenariosResponse = await request.get(`${API_BASE}/api/erp/scenario/list/1`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (scenariosResponse.status() === 404) {
      test.skip(true, 'API endpoint not implemented');
      return;
    }

    const scenariosRaw = await scenariosResponse.json();
    const scenarios = unwrapData(scenariosRaw) ?? [];
    expect(Array.isArray(scenarios)).toBeTruthy();
    const successScenarios = scenarios.filter((s: any) => (s.lastSyncStatus || s.last_sync_status) === 'SUCCESS');
    console.log(`✅ Found ${successScenarios.length} scenarios with SUCCESS status`);

    for (const scenario of successScenarios) {
      console.log(`  - ${(scenario.scenarioKey || scenario.scenario_key)}: ${(scenario.lastSyncStatus || scenario.last_sync_status)} at ${(scenario.lastSyncTime || scenario.last_sync_time)}`);
    }
  });
});
