
const { execSync } = require('child_process');

async function main() {
    const HEALTH_URL = 'http://localhost:8080/api/health/self-check';
    const LOGIN_URL = 'http://localhost:8080/api/auth/login';
    const BUSINESS_URL = 'http://localhost:8080/api/v1/abnormal?page=1&size=1';

    let token = '';

    console.log('--- Health Check Resilience & Fail-Fast Test ---');

    // Helper to login
    async function login() {
        console.log('[0] Logging in to get token...');
        try {
            const res = await fetch(LOGIN_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: 'admin', password: 'admin123' })
            });
            const body = await res.json();
            if (res.status === 200 && body.data && body.data.token) {
                token = body.data.token;
                console.log('Login successful.');
            } else {
                console.error('Login failed:', body);
                process.exit(1);
            }
        } catch (e) {
            console.error('Login error:', e.message);
            process.exit(1);
        }
    }

    // Helper to fetch health
    async function checkHealth() {
        try {
            const res = await fetch(HEALTH_URL);
            const status = res.status;
            let body = {};
            try { body = await res.json(); } catch (e) { }
            return { status, body };
        } catch (e) {
            return { status: 'ERROR', error: e.message };
        }
    }

    // 0. Login
    await login();

    // 1. Initial State
    console.log('\n[1] Checking initial state (expecting 200 UP)...');
    let res = await checkHealth();
    console.log(`Status: ${res.status}, Body:`, res.body);
    if (res.status !== 200 || res.body.data.status !== 'UP') {
        console.error('FAIL: Initial state is not UP');
        process.exit(1);
    }

    // 2. Stop DB
    console.log('\n[2] Stopping PostgreSQL...');
    try {
        execSync('brew services stop postgresql@14', { stdio: 'inherit' });
    } catch (e) {
        console.error('Failed to stop DB:', e.message);
    }
    await new Promise(r => setTimeout(r, 5000));

    // 3. Verify Outage
    console.log('\n[3] Verify Outage (expecting 503 DOWN)...');
    res = await checkHealth();
    console.log(`Status: ${res.status}, Body:`, res.body);

    // We expect 503. The body data.status should be DOWN.
    if (res.status === 503 && res.body.data.status === 'DOWN') {
        console.log('PASS: Correctly reported 503 DOWN');
    } else {
        console.error('FAIL: Expected 503 DOWN, got', res.status, res.body);
        // Don't exit yet, try to restore
    }

    // 3.5 Verify Business API Fail-Fast
    console.log('\n[3.5] Verify Business API Fail-Fast (expecting <= 1500ms)...');
    const start = performance.now();
    try {
        const busRes = await fetch(BUSINESS_URL, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const end = performance.now();
        const duration = end - start;
        console.log(`Business API Status: ${busRes.status}, Duration: ${duration.toFixed(2)}ms`); // Expect 500

        if (duration < 1500) {
            console.log('PASS: Fail-fast working (Response time < 1500ms)');
        } else {
            console.error(`FAIL: Response too slow (${duration.toFixed(2)}ms)`);
        }
    } catch (e) {
        const end = performance.now();
        const duration = end - start;
        console.log(`Business API Error: ${e.message}, Duration: ${duration.toFixed(2)}ms`);
        if (duration < 1500) {
            console.log('PASS: Fail-fast working (Error time < 1500ms)');
        } else {
            console.error(`FAIL: Error too slow (${duration.toFixed(2)}ms)`);
        }
    }

    // 4. Restore DB
    console.log('\n[4] Restoring PostgreSQL...');
    try {
        execSync('brew services start postgresql@14', { stdio: 'inherit' });
    } catch (e) {
        console.error('Failed to start DB:', e.message);
    }
    // Wait for DB to be ready
    console.log('Waiting for DB to start (10s)...');
    await new Promise(r => setTimeout(r, 10000));

    // 5. Verify Recovery
    console.log('\n[5] Verify Recovery (expecting 200 UP)...');

    // Retry logic
    for (let i = 0; i < 5; i++) {
        res = await checkHealth();
        if (res.status === 200 && res.body.data.status === 'UP') {
            console.log('PASS: Correctly recovered to 200 UP');
            process.exit(0);
        }
        console.log(`Attempt ${i + 1}: Still ${res.status}, retrying...`);
        await new Promise(r => setTimeout(r, 2000));
    }

    console.error('FAIL: Did not recover to 200 UP');
    process.exit(1);
}

main();
