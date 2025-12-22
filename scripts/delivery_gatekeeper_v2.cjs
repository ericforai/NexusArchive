// Input: Node.js 标准库
// Output: 交付检查脚本
// Pos: 运维脚本
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。


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
