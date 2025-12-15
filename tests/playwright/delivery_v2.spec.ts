
import { test, expect, request } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const BASE_URL = 'http://localhost:8080';
const USERS = {
    admin: { username: 'admin', password: 'admin123', roleId: 'super_admin' },
    auditor: { username: 'auditor', password: 'auditor123', roleId: 'role_auditor' },
    user: { username: 'user', password: 'user123', roleId: 'role_user' }
};

let adminToken = '';
let auditorToken = '';
let userToken = '';

// Helper to get token
async function getToken(request, user) {
    const res = await request.post(`${BASE_URL}/api/auth/login`, {
        data: { username: user.username, password: user.password }
    });
    const body = await res.json();
    return body.data?.token;
}

test.describe('Delivery Acceptance v2 (Smoke + SoD)', () => {

    test.beforeAll(async ({ request }) => {
        // 1. Login as Admin
        adminToken = await getToken(request, USERS.admin);
        expect(adminToken).toBeTruthy();

        // 2. Ensure Users Exist (Create if missing)
        for (const key of ['auditor', 'user']) {
            const u = USERS[key];
            // Check existence logic skipped, just try create and ignore 409/Error or delete first?
            // Better: Try login. If fail, create.
            let token = await getToken(request, u);
            if (!token) {
                console.log(`Creating user ${u.username}...`);
                const createRes = await request.post(`${BASE_URL}/admin/users`, {
                    headers: { 'Authorization': `Bearer ${adminToken}` },
                    data: {
                        username: u.username,
                        password: u.password,
                        fullName: `Test ${u.username}`,
                        roleIds: [u.roleId],
                        orgCode: 'TEST_ORG',
                        status: 'active'
                    }
                });
                if (createRes.status() !== 200) {
                    console.log(`Create user result: ${createRes.status()}`);
                }
            }
        }
    });

    // --- 1. Login Smoke ---
    test('1. Login Success/Failure', async ({ request }) => {
        // Success
        for (const u of Object.values(USERS)) {
            const res = await request.post(`${BASE_URL}/api/auth/login`, {
                data: { username: u.username, password: u.password }
            });
            expect(res.status()).toBe(200);
            const body = await res.json();
            expect(body.data.token).toBeTruthy();
        }

        // Failure
        const failRes = await request.post(`${BASE_URL}/api/auth/login`, {
            data: { username: 'admin', password: 'wrongpassword' }
        });
        expect(failRes.status()).not.toBe(200); // 401 or 400
    });

    // --- 2. SoD (Separation of Duties) ---
    test('2. Separation of Duties (Admin/Auditor/User)', async ({ request }) => {
        // Re-login to get fresh tokens
        adminToken = await getToken(request, USERS.admin);
        auditorToken = await getToken(request, USERS.auditor);
        userToken = await getToken(request, USERS.user);

        // A. Admin: +UserMgmt, -AuditLog (strict check? usually admin sees everything, but let's check roles)
        // Check User Mgmt (List Users)
        const adminUsers = await request.get(`${BASE_URL}/admin/users`, { headers: { Authorization: `Bearer ${adminToken}` } });
        expect(adminUsers.status()).toBe(200);

        // B. Auditor: +AuditLog, -UserMgmt
        const auditLogs = await request.get(`${BASE_URL}/audit-logs`, { headers: { Authorization: `Bearer ${auditorToken}` } });
        expect(auditLogs.status()).toBe(200);

        const auditorUsers = await request.get(`${BASE_URL}/admin/users`, { headers: { Authorization: `Bearer ${auditorToken}` } });
        expect(auditorUsers.status()).toBe(403); // Auditor cannot manage users

        // C. User: +Archive, -AuditLog, -UserMgmt
        const userArchives = await request.get(`${BASE_URL}/archives`, { headers: { Authorization: `Bearer ${userToken}` } });
        expect(userArchives.status()).toBe(200);

        const userAudit = await request.get(`${BASE_URL}/audit-logs`, { headers: { Authorization: `Bearer ${userToken}` } });
        expect(userAudit.status()).toBe(403);

        const userUsers = await request.get(`${BASE_URL}/admin/users`, { headers: { Authorization: `Bearer ${userToken}` } });
        expect(userUsers.status()).toBe(403);
    });

    // --- 3. Archive (Upload) ---
    let archiveId = '';
    test('3. Archive Flow', async ({ request }) => {
        const filePath = path.join(__dirname, '../fixtures/test.pdf');
        const fileBuffer = fs.readFileSync(filePath);

        const res = await request.post(`${BASE_URL}/archives`, {
            headers: { Authorization: `Bearer ${userToken}` },
            data: {
                title: 'Smoke Test Archive',
                fondsNo: 'F001',
                fiscalYear: '2025',
                retentionPeriod: '30Y',
                orgName: 'Test Org',
                amount: 100.50,
                // simplified, add other required fields if validation fails
                files: [
                    {
                        name: 'test.pdf',
                        size: fileBuffer.length,
                        contentType: 'application/pdf',
                        // ... mock file upload separately or enable multipart upload if endpoint supports it
                        // Assuming metadata-first or multipart. Let's assume JSON create first then upload, or straightforward.
                        // ArchiveController create takes @RequestBody Archive. 
                        // It does NOT handle file upload directly in `create`. Usually `ArchiveFileController` or separate.
                        // Let's create metadata first.
                    }
                ]
            }
        });

        // If 400, check validation errors
        if (res.status() !== 200) console.log(await res.text());
        expect(res.status()).toBe(200);
        const body = await res.json();
        archiveId = body.data.id;
        expect(archiveId).toBeTruthy();
    });

    // --- 4. Search ---
    test('4. Search', async ({ request }) => {
        await expect.poll(async () => {
            const res = await request.get(`${BASE_URL}/archives?search=Smoke`, {
                headers: { Authorization: `Bearer ${userToken}` }
            });
            const body = await res.json();
            return body.data.records.some(r => r.id === archiveId);
        }).toBeTruthy();
    });

    // --- 7. Delete (Admin) ---
    test('7. Delete (Admin Only)', async ({ request }) => {
        // User try delete
        const userDel = await request.delete(`${BASE_URL}/archives/${archiveId}`, {
            headers: { Authorization: `Bearer ${userToken}` }
        });
        expect(userDel.status()).toBe(403); // User cannot delete? (Assuming only Manage/Admin can, or User can delete own draft? Status=archived usually immutable)

        // Admin delete
        const adminDel = await request.delete(`${BASE_URL}/archives/${archiveId}`, {
            headers: { Authorization: `Bearer ${adminToken}` }
        });
        expect(adminDel.status()).toBe(200);
    });

    // --- 8. Audit Logs ---
    test('8. View Audit Logs', async ({ request }) => {
        // Auditor checks
        await expect.poll(async () => {
            const res = await request.get(`${BASE_URL}/audit-logs?resourceType=ARCHIVE`, {
                headers: { Authorization: `Bearer ${auditorToken}` }
            });
            const body = await res.json();
            // Look for DELETE operation on our resource
            return body.data.records.some(log => log.operationType === 'DELETE' && log.resourceId === archiveId); // Checking general match
        }).toBeTruthy();
    });

    // --- UI Smoke Tests ---
    test('9. UI Golden Path (User)', async ({ page }) => {
        // Login
        await page.goto(`${BASE_URL}/login`);
        await page.fill('input[name="username"]', 'user');
        await page.fill('input[name="password"]', 'user123');
        await page.click('button[type="submit"]');
        await expect(page).toHaveURL(`${BASE_URL}/`); // or dashboard

        // Nav to Archive
        // Assuming Nav setup. Click "档案归档" or similar
        // Just verify main elements present
        await expect(page.locator('text=电子会计档案系统')).toBeVisible();
    });

    test('10. UI Permissions (User)', async ({ page }) => {
        await page.goto(`${BASE_URL}/login`);
        await page.fill('input[name="username"]', 'user');
        await page.fill('input[name="password"]', 'user123');
        await page.click('button[type="submit"]');

        // Settings should be missing or disabled
        // Check for "系统设置" menu item
        const settingsMenu = page.locator('text=系统设置');
        if (await settingsMenu.count() > 0) {
            await expect(settingsMenu).toBeHidden();
        }
    });

});
