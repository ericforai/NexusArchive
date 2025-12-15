
const { execSync, spawn } = require('child_process');
const fs = require('fs');

const HEALTH_URL = 'http://localhost:8080/api/health/self-check';
const LOGIN_URL = 'http://localhost:8080/api/auth/login';
const BUSINESS_URL = 'http://localhost:8080/api/v1/abnormal?page=1&size=1';

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function checkHealth() {
    try {
        const res = await fetch(HEALTH_URL);
        const text = await res.text();
        let json = {};
        try { json = JSON.parse(text); } catch (e) { }
        return { status: res.status, body: json };
    } catch (e) {
        return { status: 'ECONNREFUSED', error: e.message };
    }
}

async function main() {
    console.log('--- Cold Start Delivery Verification ---');
    const results = {};

    // 1. Full Stop
    console.log('[1] Stopping services...');
    try { execSync('brew services stop postgresql@14', { stdio: 'ignore' }); } catch (e) { }
    try { execSync('pkill -f "spring-boot:run"', { stdio: 'ignore' }); } catch (e) { }
    try { execSync('lsof -ti :8080 | xargs kill -9', { stdio: 'ignore' }); } catch (e) { }
    await sleep(2000);

    // 2. Start App (DB is DOWN)
    console.log('[2] Starting App (background)...');
    const outLog = fs.openSync('logs/cold_start.log', 'w');
    const app = spawn('mvn', ['spring-boot:run', '-Dspring-boot.run.profiles=dev'], {
        cwd: '/Users/user/nexusarchive/nexusarchive-java',
        detached: true,
        stdio: ['ignore', outLog, outLog]
    });
    app.unref();

    console.log('Waiting for App to attempt startup (30s)...');
    // we poll for health.
    let step3Status = 'TIMEOUT';
    for (let i = 0; i < 30; i++) {
        const res = await checkHealth();
        if (res.status === 503) { // Expect 503 MIGRATING now, not just any status
            // Check migration field
            if (res.body.data && res.body.data.migration === 'MIGRATING') {
                console.log(`App responded with ${res.status} (MIGRATING)`);
                step3Status = res.status;
                break;
            }
        }
        await sleep(1000);
    }
    results['Step3_Health_DB_Down'] = step3Status;

    // 4. Start DB
    console.log('[4] Starting DB...');
    try { execSync('brew services start postgresql@14', { stdio: 'ignore' }); } catch (e) { }
    console.log('Waiting for DB ready (10s)...');
    await sleep(10000);

    // 5. Check Health (Auto Recovery)
    console.log('[5] Checking Health (Auto Recovery)...');
    let step5Status = 'TIMEOUT';
    for (let i = 0; i < 40; i++) { // Increase wait time for migration
        const res = await checkHealth();
        if (res.status === 200 && res.body.data.migration === 'READY') { // Expect 200 READY
            step5Status = 200;
            break;
        }
        if (i % 5 === 0) console.log(`Stats: ${res.status}, Migration: ${res.body?.data?.migration}`);
        await sleep(1000);
    }
    results['Step5_Health_DB_Up'] = step5Status;

    // 6. Business API
    console.log('[6] Running Core Business API...');
    let step6Status = 'SKIPPED';
    if (step5Status === 200) {
        // Login first
        let token = null;
        try {
            const loginRes = await fetch(LOGIN_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: 'admin', password: 'admin123' })
            });
            if (loginRes.status === 200) {
                const body = await loginRes.json();
                token = body.data.token;
            }
        } catch (e) {
            console.error('Login failed', e);
        }

        if (token) {
            try {
                const busRes = await fetch(BUSINESS_URL, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                step6Status = busRes.status;
            } catch (e) {
                step6Status = 'ERROR';
            }
        } else {
            step6Status = 'LOGIN_FAIL';
        }
    }
    results['Step6_Business_API'] = step6Status;

    console.log('\n=== Final Results ===');
    console.log(`Step 3 (Health DB Down): ${results['Step3_Health_DB_Down']}`);
    console.log(`Step 5 (Health DB Up)  : ${results['Step5_Health_DB_Up']}`);
    console.log(`Step 6 (Business API)  : ${results['Step6_Business_API']}`);

    // Cleanup (leave running or kill?) User implied "Delivery verification", usually implies leaving it running if successful?
    // "5) No need to restart app" -> Implies app stays running.
}

main();
