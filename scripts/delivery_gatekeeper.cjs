// Input: Node.js 标准库
// Output: 交付检查脚本
// Pos: 运维脚本
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。


const { execSync, spawn } = require('child_process');
const fs = require('fs');

const HEALTH_URL = 'http://localhost:8080/api/health/self-check';
const LOGIN_URL = 'http://localhost:8080/api/auth/login';
const BUSINESS_URL = 'http://localhost:8080/api/v1/abnormal?page=1&size=1';

const LOG_FILE = 'logs/gatekeeper_test.log';

// Veto Items
const VETO = {
    UNAUTHORIZED: false,
    FAKE_SUCCESS_DOWN: false,
    HEALTH_FAKE_UP: false,
    APP_HANG_ON_DOWN: false,
    GATEKEEPER_MISSING: false,
    LATENCY_VIOLATION: false,
    AUTO_RECOVERY_FAIL: false
};

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function fetchWithTiming(url, options = {}) {
    const start = performance.now();
    try {
        const res = await fetch(url, options);
        const end = performance.now();
        const text = await res.text();
        let json = {};
        try { json = JSON.parse(text); } catch (e) { }
        return { status: res.status, body: json, duration: end - start, error: null };
    } catch (e) {
        const end = performance.now();
        return { status: -1, body: null, duration: end - start, error: e.message };
    }
}

async function main() {
    console.log('=== DELIVERY GATEKEEPER SCRIPT ===');
    console.log(`Log File: ${LOG_FILE}`);

    // Cleanup
    try { execSync('brew services stop postgresql@14', { stdio: 'ignore' }); } catch (e) { }
    try { execSync('pkill -f "spring-boot:run"', { stdio: 'ignore' }); } catch (e) { }
    try { execSync('lsof -ti :8080 | xargs kill -9', { stdio: 'ignore' }); } catch (e) { }
    await sleep(2000);

    // 1. Start App with DB Down
    console.log('\n[1] Starting App (DB DOWN)...');
    const outLog = fs.openSync(LOG_FILE, 'w');
    const app = spawn('mvn', ['spring-boot:run', '-Dspring-boot.run.profiles=dev'], {
        cwd: '/Users/user/nexusarchive/nexusarchive-java',
        detached: true,
        stdio: ['ignore', outLog, outLog]
    });
    app.unref();

    console.log('Waiting for Health Check (App Alive)...');
    let appStarted = false;
    let healthRes = null;

    // Allow 40s for startup (Maven might download things)
    for (let i = 0; i < 40; i++) {
        healthRes = await fetchWithTiming(HEALTH_URL);
        if (healthRes.status !== -1) {
            appStarted = true;
            break;
        }
        await sleep(1000);
    }

    if (!appStarted) {
        console.error('FATAL: App failed to start within 40s');
        process.exit(1);
    }

    console.log(`App Started. Health Status: ${healthRes.status}, Duration: ${healthRes.duration.toFixed(2)}ms`);

    // Check 1: Health during DOWN (Must be 503 MIGRATING, < 1000ms)
    if (healthRes.status === 503) {
        const migrationStatus = healthRes.body?.data?.migration;
        if (migrationStatus === 'MIGRATING') {
            console.log(' PASS: Health is 503 MIGRATING');
        } else {
            console.error(' FAIL: Health 503 but wrong migration status:', migrationStatus);
            VETO.HEALTH_FAKE_UP = true;
        }
    } else {
        console.error(' FAIL: Health status is not 503. Got:', healthRes.status);
        VETO.HEALTH_FAKE_UP = true;
    }

    if (healthRes.duration > 1000) {
        console.error(' FAIL: Health check too slow:', healthRes.duration);
        VETO.LATENCY_VIOLATION = true;
    } else {
        console.log(' PASS: Health latency < 1s');
    }

    // Check 2: Gatekeeper (Business API during MIGRATING)
    // We need a token first? No, gatekeeper should block BEFORE auth or return 503 even for unauthorized?
    // Usually Filter runs before Interceptor, so 401 might happen first.
    // Let's try to login. Login also needs DB, so Login should Fail or be Blocked.
    // Ideally Login endpoint is also blocked by Gatekeeper? Interceptor "/**" includes /auth/login.
    // Let's check Login.
    console.log('\n[Checking Gatekeeper on Login endpoint]');
    const loginRes = await fetchWithTiming(LOGIN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'admin123' })
    });
    console.log(` Login Status: ${loginRes.status}, Body:`, loginRes.body);

    if (loginRes.status === 503) {
        console.log(' PASS: Login blocked with 503 (Gatekeeper working)');
    } else if (loginRes.status === 200) {
        console.error(' FAIL: Login succeeded while DB Down! (Fake Success?)');
        VETO.FAKE_SUCCESS_DOWN = true;
    } else {
        console.log(` INFO: Login returned ${loginRes.status} (Acceptable if not 200)`);
    }

    // 3. Start DB
    console.log('\n[2] Starting DB...');
    try { execSync('brew services start postgresql@14', { stdio: 'ignore' }); } catch (e) { }

    // 4. Wait for Recovery (Max 60s)
    console.log('Waiting for Recovery (READY)...');
    let recovered = false;
    for (let i = 0; i < 60; i++) {
        healthRes = await fetchWithTiming(HEALTH_URL);
        if (healthRes.status === 200 && healthRes.body?.data?.migration === 'READY') {
            recovered = true;
            console.log(`Recovered in ~${i}s`);
            break;
        }
        await sleep(1000);
    }

    if (!recovered) {
        console.error(' FAIL: Auto-recovery failed within 60s');
        VETO.AUTO_RECOVERY_FAIL = true;
    } else {
        console.log(' PASS: System recovered to 200 READY');
    }

    // 5. Business API Verification
    if (recovered) {
        console.log('\n[3] verifying Business API...');
        // Login first
        const tokenRes = await fetchWithTiming(LOGIN_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin', password: 'admin123' })
        });

        if (tokenRes.status === 200) {
            const token = tokenRes.body.data.token;
            // Call Business API
            const busRes = await fetchWithTiming(BUSINESS_URL, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            console.log(` Business API Status: ${busRes.status}, Duration: ${busRes.duration.toFixed(2)}ms`);

            if (busRes.status === 200) {
                console.log(' PASS: Business API success');
            } else {
                console.error(' FAIL: Business API failed failed after recovery');
                VETO.AUTO_RECOVERY_FAIL = true;
            }

            if (busRes.duration > 1500) {
                console.error(' FAIL: Business API too slow > 1.5s');
                VETO.LATENCY_VIOLATION = true;
            } else {
                console.log(' PASS: Business API latency < 1.5s');
            }

        } else {
            console.error(' FAIL: Login failed after recovery');
            VETO.AUTO_RECOVERY_FAIL = true;
        }
    }

    // 6. Log Analysis
    console.log('\n[4] Log Signature Check...');
    const logContent = fs.readFileSync(LOG_FILE, 'utf8');
    const hasStart = logContent.includes('Starting Resilient Flyway Runner');
    const hasAttempt = logContent.includes('Attempting database migration');
    const hasSuccess = logContent.includes('Database migration completed successfully');

    console.log(` Log: Runner Start? ${hasStart}`);
    console.log(` Log: Migration Attempt? ${hasAttempt}`);
    console.log(` Log: Success? ${hasSuccess}`);

    if (!hasStart || !hasAttempt || !hasSuccess) {
        console.warn(' WARN: Missing log signatures');
    } else {
        console.log(' PASS: All log signatures found');
    }

    // Final Report
    console.log('\n========== FINAL REPORT ==========');
    const failedVetos = Object.entries(VETO).filter(([k, v]) => v);
    if (failedVetos.length > 0) {
        console.error('RESULT: FAIL');
        console.error('VETO ITEMS TRIGGERED:');
        failedVetos.forEach(([k]) => console.error(` - ${k}`));
        process.exit(1);
    } else {
        console.log('RESULT: PASS');
        process.exit(0);
    }
}

main();
