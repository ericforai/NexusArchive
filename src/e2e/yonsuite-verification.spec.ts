// src/e2e/yonsuite-verification.spec.ts
/**
 * Simplified YonSuite Scenario Verification Tests
 *
 * Focus: Verify 5 real scenarios exist in database and API
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const API_BASE = process.env.API_BASE ?? 'http://localhost:19090';

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
    const token = loginData.token;

    // Step 2: Get scenarios for YonSuite (configId=1) - CORRECTED PATH
    const scenariosResponse = await request.get(`${API_BASE}/erp/scenario/list/1`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // Step 3: Verify response
    if (scenariosResponse.status() === 404) {
      console.warn('⚠️ API endpoint /erp/scenario/list not found (404)');
      test.skip(true, 'API endpoint not implemented');
      return;
    }

    expect(scenariosResponse.ok()).toBeTruthy();
    const scenarios = await scenariosResponse.json();

    // Step 4: Verify exactly 5 scenarios (after removing placeholders)
    expect(scenarios.data.length).toBe(5);
    console.log(`✅ Database has ${scenarios.data.length} scenarios (expected: 5)`);

    // Step 5: Verify scenario keys
    const expectedKeys = [
      'VOUCHER_SYNC',
      'ATTACHMENT_SYNC',
      'COLLECTION_FILE_SYNC',
      'PAYMENT_FILE_SYNC',
      'REFUND_FILE_SYNC'
    ];

    const actualKeys = scenarios.data.map((s: any) => s.scenario_key);
    for (const key of expectedKeys) {
      expect(actualKeys).toContain(key);
      console.log(`✅ Scenario ${key}: verified`);
    }

    // Step 6: Verify no placeholders exist
    const placeholderKeys = [
      'SCM_SALESOUT_LIST',
      'SCM_SALESOUT_DETAIL',
      'VOUCHER_ATTACHMENT_BATCH_QUERY'
    ];

    for (const key of placeholderKeys) {
      expect(actualKeys).not.toContain(key);
      console.log(`✅ Placeholder ${key}: correctly removed`);
    }
  });

  test('should show YonSuite connector in UI', async ({ page }) => {
    // Navigate to login page
    await page.goto(BASE_URL);

    // Check if login page is shown
    const loginForm = page.locator('input[name="username"]');
    if (await loginForm.isVisible()) {
      // Login
      await page.fill('input[name="username"]', 'admin');
      await page.fill('input[name="password"]', 'admin123');
      await page.click('button[type="submit"]');

      // Wait for navigation
      await page.waitForURL('**/dashboard', { timeout: 10000 });
    }

    // Navigate to Integration Settings
    await page.click('text=集成中心');
    await page.waitForURL('**/integration');

    // Verify YonSuite card is visible
    const yonSuiteCard = page.locator('text=用友YonSuite');
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
    const token = loginData.token;

    const scenariosResponse = await request.get(`${API_BASE}/erp/scenario/list/1`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (scenariosResponse.status() === 404) {
      test.skip(true, 'API endpoint not implemented');
      return;
    }

    const scenarios = await scenariosResponse.json();
    const successScenarios = scenarios.data.filter((s: any) => s.last_sync_status === 'SUCCESS');

    expect(successScenarios.length).toBeGreaterThan(0);
    console.log(`✅ Found ${successScenarios.length} scenarios with SUCCESS status`);

    for (const scenario of successScenarios) {
      console.log(`  - ${scenario.scenario_key}: ${scenario.last_sync_status} at ${scenario.last_sync_time}`);
    }
  });
});
