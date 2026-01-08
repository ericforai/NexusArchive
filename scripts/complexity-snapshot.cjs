// Input: ESLint 复杂度检查输出
// Output: 追加到 docs/metrics/complexity-history.json
// Pos: scripts/ 复杂度快照生成脚本
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const HISTORY_FILE = path.join(__dirname, '../docs/metrics/complexity-history.json');
const SRC_DIR = path.join(__dirname, '../src');

/**
 * 解析 ESLint JSON 输出，提取复杂度违规
 */
function parseEslintOutput(eslintJson) {
    const results = JSON.parse(eslintJson);
    const files = [];
    let totalViolations = 0;
    let highSeverity = 0;
    let mediumSeverity = 0;
    let lowSeverity = 0;

    for (const result of results) {
        if (!result.filePath.startsWith(SRC_DIR)) continue;

        const fileViolations = [];
        let maxFunctionLines = 0;
        let maxComplexity = 0;

        for (const message of result.messages) {
            if (message.ruleId === 'max-lines-per-function') {
                const lines = parseInt(message.message.match(/(\d+) lines/)?.[1] || '0');
                maxFunctionLines = Math.max(maxFunctionLines, lines);
                fileViolations.push({
                    rule: 'max-lines-per-function',
                    line: message.line,
                    message: message.message
                });
            }
            if (message.ruleId === 'complexity') {
                const complexity = parseInt(message.message.match(/(\d+)/)?.[1] || '0');
                maxComplexity = Math.max(maxComplexity, complexity);
                fileViolations.push({
                    rule: 'complexity',
                    line: message.line,
                    message: message.message
                });
            }
        }

        if (fileViolations.length > 0) {
            const relativePath = path.relative(path.join(__dirname, '../'), result.filePath);
            const severity = getSeverity(maxFunctionLines, maxComplexity);

            files.push({
                path: relativePath,
                lines: result.source?.split('\n').length || 0,
                maxFunctionLines,
                complexity: maxComplexity,
                violations: fileViolations.map(v => v.rule)
            });

            totalViolations += fileViolations.length;
            if (severity === 'high') highSeverity++;
            else if (severity === 'medium') mediumSeverity++;
            else lowSeverity++;
        }
    }

    return { files, totalViolations, highSeverity, mediumSeverity, lowSeverity };
}

/**
 * 根据指标确定严重程度
 */
function getSeverity(maxFunctionLines, complexity) {
    if (complexity > 15 || maxFunctionLines > 100) return 'high';
    if (complexity > 10 || maxFunctionLines > 50) return 'medium';
    return 'low';
}

/**
 * 获取当前 Git 信息
 */
function getGitInfo() {
    try {
        const commit = execSync('git rev-parse --short HEAD', { encoding: 'utf-8' }).trim();
        const branch = execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf-8' }).trim();
        return { commit, branch };
    } catch {
        return { commit: 'unknown', branch: 'unknown' };
    }
}

/**
 * 主函数
 */
function main() {
    // 运行 ESLint 获取 JSON 输出（使用临时文件避免缓冲区溢出）
    const tmpFile = path.join(__dirname, '.eslint-output.json');
    const eslintCmd = `npx eslint "src/**/*.ts" "src/**/*.tsx" -c .eslintrc.complexity.cjs --format json --output-file ${tmpFile}`;
    try {
        execSync(eslintCmd, { encoding: 'utf-8', stdio: 'pipe' });
    } catch {
        // ESLint 返回非零退出码（有警告），但文件已生成
    }

    // 读取输出文件
    if (!fs.existsSync(tmpFile)) {
        console.log('✅ Complexity snapshot saved: 0 violations');
        return;
    }

    const eslintOutput = fs.readFileSync(tmpFile, 'utf-8');
    fs.unlinkSync(tmpFile);

    const { files, totalViolations, highSeverity, mediumSeverity, lowSeverity } = parseEslintOutput(eslintOutput);

    // 读取现有历史
    let history = { metadata: {}, snapshots: [] };
    if (fs.existsSync(HISTORY_FILE)) {
        history = JSON.parse(fs.readFileSync(HISTORY_FILE, 'utf-8'));
    }

    // 初始化 metadata
    if (!history.metadata.createdAt) {
        history.metadata = {
            formatVersion: '1.0',
            createdAt: new Date().toISOString(),
            lastUpdated: new Date().toISOString()
        };
    } else {
        history.metadata.lastUpdated = new Date().toISOString();
    }

    // 创建新快照
    const { commit, branch } = getGitInfo();
    const snapshot = {
        timestamp: new Date().toISOString(),
        commit,
        branch,
        summary: {
            total: totalViolations,
            high: highSeverity,
            medium: mediumSeverity,
            low: lowSeverity
        },
        files
    };

    // 追加快照
    history.snapshots.push(snapshot);

    // 保留最近 100 条快照
    if (history.snapshots.length > 100) {
        history.snapshots = history.snapshots.slice(-100);
    }

    // 写入文件
    fs.writeFileSync(HISTORY_FILE, JSON.stringify(history, null, 2));
    console.log(`✅ Complexity snapshot saved: ${totalViolations} violations`);
}

main();
