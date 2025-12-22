// Input: Playwright、Node.js 标准库
// Output: 脚本模块
// Pos: Playwright 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。


import { test, expect } from '@playwright/test';
import { execSync } from 'child_process';

test.describe('Health Check Resilience', () => {
    const HEALTH_URL = 'http://localhost:8080/api/health/self-check';

    test('should return 503 DOWN when DB is stopped and 200 UP when DB is running', async ({ request }) => {
        // 1. Initial State: DB should be UP
        let response = await request.get(HEALTH_URL);
        expect(response.status()).toBe(200);
        let body = await response.json();
        expect(body.data.status).toBe('UP');
        expect(body.data.dbConnection).toBe('UP');

        console.log('Stopping PostgreSQL...');
        execSync('brew services stop postgresql@14');

        // Give it a moment to stop
        await new Promise(r => setTimeout(r, 5000));

        // 2. Outage State: DB Down
        console.log('Verifying Health Check during outage...');
        response = await request.get(HEALTH_URL);

        // Expect 503
        expect(response.status()).toBe(503);
        body = await response.json();
        expect(body.data.status).toBe('DOWN');
        expect(body.data.dbConnection).toBe('DOWN');

        console.log('Restoring PostgreSQL...');
        execSync('brew services start postgresql@14');

        // Give it a moment to start
        await new Promise(r => setTimeout(r, 5000));

        // 3. Recovered State: DB Up
        console.log('Verifying Health Check after recovery...');
        // Retry a few times as DB startup might take a few seconds
        await expect(async () => {
            response = await request.get(HEALTH_URL);
            expect(response.status()).toBe(200);
            body = await response.json();
            expect(body.data.status).toBe('UP');
            expect(body.data.dbConnection).toBe('UP');
        }).toPass({ timeout: 30000 });
    });
});
