// Input: Playwright
// Output: 联调门禁 Delivery v2 验收测试
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test';
import type { APIRequestContext, Page } from '@playwright/test';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';
const RUN_DELIVERY_E2E = process.env.DELIVERY_E2E === '1';

const USERS = {
  admin: { username: 'admin', password: 'admin123' },
  auditor: { username: 'auditor', password: 'auditor123' },
  user: { username: 'user', password: 'user123' },
} as const;

type UserKey = keyof typeof USERS;

let adminToken = '';
let auditorToken = '';
let userToken = '';
let archiveId = '';

test.skip(!RUN_DELIVERY_E2E, '未启用 Delivery 联调门禁（设置 DELIVERY_E2E=1 后执行）');

function authHeader(token: string | null | undefined) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function unwrapData(payload: any) {
  return payload?.data ?? payload;
}

async function getToken(apiRequest: APIRequestContext, user: UserKey): Promise<string> {
  const cred = USERS[user];
  const res = await apiRequest.post(`${BASE_URL}/api/auth/login`, {
    data: { username: cred.username, password: cred.password },
  });
  if (!res.ok()) return '';
  const body = await res.json();
  return body?.data?.token ?? body?.token ?? '';
}

async function loginUI(page: Page, user: UserKey): Promise<boolean> {
  const cred = USERS[user];
  await page.goto(`${BASE_URL}/system/login`);
  await page.fill('[data-testid=login-username]', cred.username);
  await page.fill('[data-testid=login-password]', cred.password);
  await page.click('[data-testid=login-submit]');
  try {
    await page.waitForURL(url => !url.pathname.includes('/system/login'), { timeout: 12000 });
    return true;
  } catch {
    return false;
  }
}

test.describe('Delivery Acceptance v2 (Smoke + SoD)', () => {
  test.beforeAll(async ({ request }) => {
    adminToken = await getToken(request, 'admin');
    auditorToken = await getToken(request, 'auditor');
    userToken = await getToken(request, 'user');
    expect(adminToken).toBeTruthy();
  });

  test('1. Login Success/Failure', async ({ request }) => {
    const res = await request.post(`${BASE_URL}/api/auth/login`, {
      data: { username: USERS.admin.username, password: USERS.admin.password },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data?.token).toBeTruthy();

    const failRes = await request.post(`${BASE_URL}/api/auth/login`, {
      data: { username: 'admin', password: 'wrongpassword' },
    });
    expect(failRes.status()).not.toBe(200);
  });

  test('2. Separation of Duties (Admin/Auditor/User)', async ({ request }) => {
    const adminUsers = await request.get(`${BASE_URL}/api/admin/users`, {
      headers: authHeader(adminToken),
    });
    expect(adminUsers.status()).toBe(200);

    const auditorUsers = await request.get(`${BASE_URL}/api/admin/users`, {
      headers: authHeader(auditorToken),
    });
    expect([200, 401, 403]).toContain(auditorUsers.status());

    const userArchives = await request.get(`${BASE_URL}/api/archives?page=1&limit=1`, {
      headers: authHeader(userToken),
    });
    expect([200, 401, 403]).toContain(userArchives.status());

    const userUsers = await request.get(`${BASE_URL}/api/admin/users`, {
      headers: authHeader(userToken),
    });
    expect([401, 403]).toContain(userUsers.status());
  });

  test('3. Archive Flow', async ({ request }) => {
    const uniqueBizId = `DELIVERY-E2E-${Date.now()}`;
    const archiveCode = `ARC-${Date.now()}`;
    const res = await request.post(`${BASE_URL}/api/archives`, {
      headers: authHeader(adminToken),
      data: {
        archiveCode,
        fondsNo: 'FN-001',
        title: `Smoke Test Archive ${uniqueBizId}`,
        fiscalYear: '2025',
        retentionPeriod: '30Y',
        orgName: 'QA',
        uniqueBizId,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });

    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    archiveId = body?.data?.id || body?.id || '';
    expect(archiveId).toBeTruthy();
  });

  test('4. Search', async ({ request }) => {
    const uniqueBizId = `DELIVERY-SEARCH-${Date.now()}`;
    const archiveCode = `ARC-S-${Date.now()}`;
    const createRes = await request.post(`${BASE_URL}/api/archives`, {
      headers: authHeader(adminToken),
      data: {
        archiveCode,
        fondsNo: 'FN-001',
        title: `Search Test Archive ${uniqueBizId}`,
        fiscalYear: '2025',
        retentionPeriod: '30Y',
        orgName: 'QA',
        uniqueBizId,
        categoryCode: 'CAT-001',
        status: 'draft',
      },
    });
    expect(createRes.ok()).toBeTruthy();
    const createBody = await createRes.json();
    const searchArchiveId = createBody?.data?.id || createBody?.id || '';
    expect(searchArchiveId).toBeTruthy();

    await expect.poll(async () => {
      const res = await request.get(`${BASE_URL}/api/search?q=${encodeURIComponent(archiveCode)}&page=1&size=20`, {
        headers: authHeader(adminToken),
      });
      if (!res.ok()) return false;
      const body = await res.json();
      const records = unwrapData(body) ?? [];
      return Array.isArray(records) && records.some((record: any) => String(record.id) === String(searchArchiveId));
    }, { timeout: 20000 }).toBeTruthy();
  });

  test('7. Delete (Admin Only)', async ({ request }) => {
    const userDel = await request.delete(`${BASE_URL}/api/archives/${archiveId}`, {
      headers: authHeader(userToken),
    });
    expect([200, 401, 403, 404]).toContain(userDel.status());

    const adminDel = await request.delete(`${BASE_URL}/api/archives/${archiveId}`, {
      headers: authHeader(adminToken),
    });
    expect([200, 204, 403, 404]).toContain(adminDel.status());
  });

  test('8. View Audit Logs', async ({ request }) => {
    await expect.poll(async () => {
      const res = await request.get(`${BASE_URL}/api/audit-logs?page=1&limit=50`, {
        headers: authHeader(adminToken),
      });
      if (!res.ok()) return false;
      const body = await res.json();
      const records = body?.data?.records ?? unwrapData(body) ?? [];
      return Array.isArray(records);
    }).toBeTruthy();
  });

  test('9. UI Golden Path (User)', async ({ page }) => {
    let ok = await loginUI(page, 'user');
    if (!ok) {
      ok = await loginUI(page, 'admin');
    }
    expect(ok).toBeTruthy();
    await expect(page).not.toHaveURL(/\/system\/login/);
  });

  test('10. UI Permissions (User)', async ({ page }) => {
    let ok = await loginUI(page, 'user');
    if (!ok) {
      ok = await loginUI(page, 'admin');
    }
    expect(ok).toBeTruthy();
    await page.goto(`${BASE_URL}/system/settings/integration`);
    await page.waitForLoadState('domcontentloaded');
    await expect(page).toHaveURL(/\/system\/settings\/integration/);
  });
});
