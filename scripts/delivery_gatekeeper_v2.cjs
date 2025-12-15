
const { execSync } = require('child_process');

console.log('=== DELIVERY GATEKEEPER V2 (SMOKE + SoD) ===');

try {
    // 1. Run Playwright
    console.log('Running Playwright Tests...');
    execSync('npx playwright test tests/playwright/delivery_v2.spec.ts', { stdio: 'inherit' });
    console.log('\nRESULT: PASS');
} catch (e) {
    console.error('\nRESULT: FAIL');
    process.exit(1);
}
