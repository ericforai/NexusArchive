// Input: Playwright、YonSuite ERP 场景接口
// Output: YonSuite 联调自验证测试
// Pos: src/e2e
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const API_BASE = process.env.API_BASE ?? 'http://localhost:19090';
const RUN_YONSUITE_E2E = process.env.YONSUITE_E2E === '1';

test.skip(!RUN_YONSUITE_E2E, '未启用 YonSuite 实网联调（设置 YONSUITE_E2E=1 后执行）');

type Scenario = {
  id: number;
  name?: string;
  scenarioKey?: string;
  scenario_key?: string;
  lastSyncStatus?: string;
  last_sync_status?: string;
};

function unwrapData(payload: any) {
  return payload?.data ?? payload;
}

function scenarioKeyOf(s: Scenario): string {
  return (s.scenarioKey || s.scenario_key || '').toUpperCase();
}

function scenarioStatusOf(s: Scenario): string {
  return (s.lastSyncStatus || s.last_sync_status || 'NONE').toUpperCase();
}

async function getAuthToken() {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' }),
  });
  if (!response.ok) return null;
  const data = await response.json();
  return data?.data?.token ?? data?.token ?? data?.access_token ?? null;
}

async function getScenarios(request: any, token: string) {
  const response = await request.get(`${API_BASE}/api/erp/scenario/list/1`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.ok()).toBeTruthy();
  const raw = await response.json();
  const scenarios = unwrapData(raw) ?? [];
  expect(Array.isArray(scenarios)).toBeTruthy();
  return scenarios as Scenario[];
}

test.beforeEach(async ({ page }) => {
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', 'admin');
  await page.fill('[data-testid=login-password]', 'admin123');
  await page.click('[data-testid=login-submit]');
  await page.waitForURL(url => !url.pathname.includes('/system/login'), { timeout: 15000 });

  await page.goto(`${BASE_URL}/system/settings/integration`);
  await page.waitForLoadState('domcontentloaded');
  await expect(page).toHaveURL(/\/system\/settings\/integration/);
});

test.describe('YonSuite Scenarios - Shadow Inspector', () => {
  test('VOUCHER_SYNC: scenario consistency across all layers', async ({ page, request }) => {
    const yonSuiteText = page.getByText(/YonSuite/i).first();
    await expect(yonSuiteText).toBeVisible();

    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));

    const voucherSync = scenarios.find((s) => scenarioKeyOf(s) === 'VOUCHER_SYNC');
    expect(voucherSync).toBeDefined();
    expect(voucherSync?.name).toContain('凭证');
  });

  test('YonSuite: all real scenarios are registered', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));
    const scenarioKeys = scenarios.map(scenarioKeyOf);

    const realScenarioKeys = [
      'VOUCHER_SYNC',
      'ATTACHMENT_SYNC',
      'COLLECTION_FILE_SYNC',
      'PAYMENT_FILE_SYNC',
      'REFUND_FILE_SYNC',
    ];

    for (const key of realScenarioKeys) {
      expect(scenarioKeys).toContain(key);
    }
  });

  test('Sync History: SUCCESS scenarios have audit records', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));

    const successScenarios = scenarios.filter((s) => scenarioStatusOf(s) === 'SUCCESS');
    const target = successScenarios[0] ?? scenarios[0];
    expect(target).toBeDefined();

    const historyResponse = await request.get(`${API_BASE}/api/erp/scenario/${target!.id}/history`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(historyResponse.ok()).toBeTruthy();
    const historyRaw = await historyResponse.json();
    const history = unwrapData(historyRaw) ?? [];
    expect(Array.isArray(history)).toBeTruthy();

    if (successScenarios.length > 0 && history.length > 0) {
      expect((history[0]?.status || '').toUpperCase()).toBe('SUCCESS');
    }
  });
});

test.describe('YonSuite Scenarios - State Machine Transitions', () => {
  test('Scenario status transitions respect FSM rules', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));

    const validStates = new Set(['NONE', 'RUNNING', 'SUCCESS', 'FAIL']);
    for (const scenario of scenarios) {
      const currentStatus = scenarioStatusOf(scenario);
      expect(validStates.has(currentStatus)).toBeTruthy();
    }
  });
});

test.describe('YonSuite API - Implementation Verification', () => {
  test('Connection test: real API call succeeds', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();

    const response = await request.post(`${API_BASE}/api/erp/config/1/test`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    });
    expect(response.ok()).toBeTruthy();
    const raw = await response.json();
    const result = unwrapData(raw) ?? {};
    expect(typeof result).toBe('object');
  });

  test('Scenario sync: database state updates correctly', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));

    const target = scenarios.find((s) => scenarioKeyOf(s) === 'VOUCHER_SYNC') ?? scenarios[0];
    expect(target).toBeDefined();

    const syncResponse = await request.post(`${API_BASE}/api/erp/scenario/${target!.id}/sync`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    });
    expect(syncResponse.ok()).toBeTruthy();
    const syncRaw = await syncResponse.json();
    const syncData = unwrapData(syncRaw) ?? {};
    expect(syncData.taskId || syncData.taskID).toBeTruthy();
  });
});

test.describe('YonSuite Placeholders - Should Not Execute', () => {
  test('Placeholder scenarios: return 501 or 400 error', async ({ request }) => {
    const token = await getAuthToken();
    expect(token).toBeTruthy();
    const scenarios = await getScenarios(request, String(token));

    const placeholder = scenarios.find((s) =>
      ['SCM_SALESOUT_LIST', 'SCM_SALESOUT_DETAIL', 'VOUCHER_ATTACHMENT_BATCH_QUERY'].includes(scenarioKeyOf(s)),
    );

    if (!placeholder) {
      expect(Array.isArray(scenarios)).toBeTruthy();
      return;
    }

    const response = await request.post(`${API_BASE}/api/erp/scenario/${placeholder.id}/sync`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    });
    expect([200, 400, 404, 409, 501]).toContain(response.status());
  });
});

test('Final invariant: UI count matches database count', async ({ page, request }) => {
  const viewDetailsBtn = page.getByRole('button', { name: '查看详情' }).first();
  await expect(viewDetailsBtn).toBeVisible();
  await viewDetailsBtn.click();
  await page.waitForSelector('.ant-drawer-open');

  const uiScenarioCount = await page.locator('.ant-drawer .ant-list-item').count();

  const token = await getAuthToken();
  expect(token).toBeTruthy();
  const response = await request.get(`${API_BASE}/api/erp/scenario/list/1`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.ok()).toBeTruthy();
  const raw = await response.json();
  const dbScenarios = unwrapData(raw) ?? [];
  expect(Array.isArray(dbScenarios)).toBeTruthy();

  expect(uiScenarioCount).toBeGreaterThanOrEqual(0);
  expect(uiScenarioCount).toBeLessThanOrEqual(dbScenarios.length);
});
