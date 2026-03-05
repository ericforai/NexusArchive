#!/usr/bin/env node
/**
 * Input: Manifest files + test files + optional test result JSON files.
 * Output: Module-Test-Status matrix in JSON and Markdown.
 * Pos: System self-check report generator.
 */

import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';

const cwd = process.cwd();

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i += 1) {
    const a = argv[i];
    if (!a.startsWith('--')) continue;
    const key = a.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) {
      args[key] = true;
      continue;
    }
    args[key] = next;
    i += 1;
  }
  return args;
}

function readJsonIfExists(filePath) {
  if (!filePath) return null;
  if (!fs.existsSync(filePath)) return null;
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch {
    return null;
  }
}

function runFileQuery(cmd) {
  try {
    const output = execSync(cmd, { cwd, encoding: 'utf8' }).trim();
    if (!output) return [];
    return output.split('\n').filter(Boolean);
  } catch {
    return [];
  }
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function nowIsoDate() {
  const now = new Date();
  const y = String(now.getFullYear());
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function tokenize(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, ' ')
    .split(/\s+/)
    .filter(Boolean);
}

function unique(arr) {
  return [...new Set(arr)];
}

function normalizePath(p) {
  return p.replace(/\\/g, '/');
}

function canonicalTestPath(p) {
  const normalized = normalizePath(p || '');
  if (!normalized) return '';
  if (path.isAbsolute(normalized)) {
    return normalizePath(path.relative(cwd, normalized));
  }
  return normalized.replace(/^\.\//, '');
}

function extractManifestInfo(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const idMatch = content.match(/id:\s*['"]([^'"]+)['"]/);
  const id = idMatch ? idMatch[1] : normalizePath(filePath);
  const rel = normalizePath(path.relative(cwd, filePath));
  const segments = rel.split('/');
  const lastDir = segments.length > 1 ? segments[segments.length - 2] : segments[0];
  const idTokens = tokenize(id.replace(/[._-]/g, ' '));
  const pathTokens = tokenize(lastDir);

  const weakTokens = new Set([
    'src',
    'pages',
    'page',
    'components',
    'component',
    'features',
    'feature',
    'utils',
    'java',
    'main',
    'com',
    'nexusarchive',
    'manifest',
    'config'
  ]);

  const tokens = unique([...idTokens, ...pathTokens]).filter((t) => !weakTokens.has(t));
  return { id, filePath: rel, tokens };
}

function parsePlaywrightJson(playwrightJson) {
  const fileStatus = new Map();
  if (!playwrightJson || !Array.isArray(playwrightJson.suites)) return fileStatus;

  function walkSuite(suite, fileHint = '') {
    const nextHint = suite.file ? normalizePath(suite.file) : fileHint;

    if (Array.isArray(suite.specs)) {
      for (const spec of suite.specs) {
        const file = canonicalTestPath(spec.file || nextHint || '');
        if (!file) continue;
        const bucket = fileStatus.get(file) || {
          passed: 0,
          failed: 0,
          skipped: 0,
          timedOut: 0,
          interrupted: 0
        };
        const tests = Array.isArray(spec.tests) ? spec.tests : [];
        for (const t of tests) {
          const results = Array.isArray(t.results) ? t.results : [];
          for (const r of results) {
            const status = r.status || 'unknown';
            if (status === 'passed') bucket.passed += 1;
            else if (status === 'failed') bucket.failed += 1;
            else if (status === 'skipped') bucket.skipped += 1;
            else if (status === 'timedOut') bucket.timedOut += 1;
            else if (status === 'interrupted') bucket.interrupted += 1;
          }
        }
        fileStatus.set(file, bucket);
      }
    }

    if (Array.isArray(suite.suites)) {
      for (const child of suite.suites) walkSuite(child, nextHint);
    }
  }

  for (const rootSuite of playwrightJson.suites) {
    walkSuite(rootSuite);
  }
  return fileStatus;
}

function parseVitestJson(vitestJson) {
  const fileStatus = new Map();
  if (!vitestJson) return fileStatus;

  const files = Array.isArray(vitestJson.testResults) ? vitestJson.testResults : [];
  for (const f of files) {
    const file = canonicalTestPath(f.name || f.file || '');
    if (!file) continue;
    const bucket = fileStatus.get(file) || {
      passed: 0,
      failed: 0,
      skipped: 0,
      timedOut: 0,
      interrupted: 0
    };

    const assertions = Array.isArray(f.assertionResults) ? f.assertionResults : [];
    if (assertions.length > 0) {
      for (const a of assertions) {
        const status = a.status || 'unknown';
        if (status === 'passed') bucket.passed += 1;
        else if (status === 'failed') bucket.failed += 1;
        else if (status === 'pending' || status === 'skipped') bucket.skipped += 1;
      }
    } else {
      const status = f.status || 'unknown';
      if (status === 'passed') bucket.passed += 1;
      else if (status === 'failed') bucket.failed += 1;
      else if (status === 'pending' || status === 'skipped') bucket.skipped += 1;
    }

    fileStatus.set(file, bucket);
  }
  return fileStatus;
}

function mergeStatuses(...maps) {
  const result = new Map();
  for (const map of maps) {
    for (const [file, stat] of map.entries()) {
      const key = canonicalTestPath(file);
      const prev = result.get(key) || {
        passed: 0,
        failed: 0,
        skipped: 0,
        timedOut: 0,
        interrupted: 0
      };
      result.set(key, {
        passed: prev.passed + (stat.passed || 0),
        failed: prev.failed + (stat.failed || 0),
        skipped: prev.skipped + (stat.skipped || 0),
        timedOut: prev.timedOut + (stat.timedOut || 0),
        interrupted: prev.interrupted + (stat.interrupted || 0)
      });
    }
  }
  return result;
}

function collectExplicitManifestMappings(testFiles) {
  const map = new Map();
  const marker = /@manifest:([a-zA-Z0-9._-]+)/g;

  for (const testFile of testFiles) {
    const absPath = path.resolve(cwd, testFile);
    if (!fs.existsSync(absPath)) continue;

    let content = '';
    try {
      content = fs.readFileSync(absPath, 'utf8');
    } catch {
      continue;
    }

    let m = marker.exec(content);
    while (m) {
      const moduleId = m[1];
      const prev = map.get(moduleId) || new Set();
      prev.add(testFile);
      map.set(moduleId, prev);
      m = marker.exec(content);
    }
  }

  const normalized = new Map();
  for (const [moduleId, files] of map.entries()) {
    normalized.set(moduleId, [...files].sort());
  }
  return normalized;
}

function mapModuleToTests(moduleInfo, allTests, explicitMap) {
  const explicit = explicitMap.get(moduleInfo.id);
  if (explicit && explicit.length > 0) {
    return explicit;
  }

  const hits = [];
  const tokenSet = new Set(moduleInfo.tokens);

  for (const testFile of allTests) {
    const lowerFile = testFile.toLowerCase();
    let matched = false;
    for (const token of tokenSet) {
      if (token.length < 3) continue;
      if (lowerFile.includes(token)) {
        matched = true;
        break;
      }
    }
    if (matched) hits.push(testFile);
  }

  return unique(hits).sort();
}

function aggregateModuleStatus(mappedTests, fileStatusMap) {
  if (mappedTests.length === 0) {
    return {
      status: 'uncovered',
      counts: { passed: 0, failed: 0, skipped: 0, timedOut: 0, interrupted: 0 },
      withRuntimeStatus: 0
    };
  }

  const total = { passed: 0, failed: 0, skipped: 0, timedOut: 0, interrupted: 0 };
  let filesWithRuntimeStatus = 0;

  for (const tf of mappedTests) {
    const stat = fileStatusMap.get(tf);
    if (!stat) continue;
    filesWithRuntimeStatus += 1;
    total.passed += stat.passed || 0;
    total.failed += stat.failed || 0;
    total.skipped += stat.skipped || 0;
    total.timedOut += stat.timedOut || 0;
    total.interrupted += stat.interrupted || 0;
  }

  if (filesWithRuntimeStatus === 0) {
    return { status: 'mapped_not_run', counts: total, withRuntimeStatus: 0 };
  }

  if (total.failed > 0 || total.timedOut > 0 || total.interrupted > 0) {
    return { status: 'failed', counts: total, withRuntimeStatus: filesWithRuntimeStatus };
  }
  if (total.passed > 0) {
    return { status: 'passed', counts: total, withRuntimeStatus: filesWithRuntimeStatus };
  }
  return { status: 'skipped_only', counts: total, withRuntimeStatus: filesWithRuntimeStatus };
}

function renderMarkdown({ generatedAt, modules, summary }) {
  const lines = [];
  lines.push('# 系统全面自查矩阵');
  lines.push('');
  lines.push(`- 生成时间: ${generatedAt}`);
  lines.push(`- 模块总数: ${summary.totalModules}`);
  lines.push(`- 有测试映射: ${summary.mappedModules}`);
  lines.push(`- 无测试映射: ${summary.uncoveredModules}`);
  lines.push(`- 状态分布: passed=${summary.passed}, failed=${summary.failed}, mapped_not_run=${summary.mappedNotRun}, uncovered=${summary.uncovered}, skipped_only=${summary.skippedOnly}`);
  lines.push('');
  lines.push('## 模块状态');
  lines.push('');
  lines.push('| 模块ID | 状态 | 映射测试文件数 | 有运行状态文件数 | 失败断言数 | Manifest |');
  lines.push('| --- | --- | ---: | ---: | ---: | --- |');

  for (const m of modules) {
    const manifestLink = `\`${m.manifestPath}\``;
    lines.push(`| ${m.id} | ${m.status} | ${m.mappedTests.length} | ${m.runtimeStatusFiles} | ${m.counts.failed + m.counts.timedOut + m.counts.interrupted} | ${manifestLink} |`);
  }

  lines.push('');
  lines.push('## 无测试映射模块');
  lines.push('');
  const uncovered = modules.filter((m) => m.status === 'uncovered');
  if (uncovered.length === 0) {
    lines.push('- 无');
  } else {
    for (const m of uncovered) {
      lines.push(`- ${m.id} (${m.manifestPath})`);
    }
  }

  lines.push('');
  lines.push('## 使用说明');
  lines.push('');
  lines.push('- 执行 `npm run self-check:run` 触发持续回归并产出本矩阵。');
  lines.push('- 执行 `npm run self-check:matrix` 仅根据现有测试清单重建矩阵（不跑测试）。');

  return `${lines.join('\n')}\n`;
}

function main() {
  const args = parseArgs(process.argv);
  const outputDir = args['output-dir']
    ? path.resolve(cwd, args['output-dir'])
    : path.resolve(cwd, 'reports/self-check/latest');

  const dateStr = nowIsoDate();
  const mdOutput = args['md']
    ? path.resolve(cwd, args['md'])
    : path.resolve(cwd, `docs/reports/${dateStr}-system-self-check-matrix.md`);
  const jsonOutput = args['json']
    ? path.resolve(cwd, args['json'])
    : path.join(outputDir, 'module-test-matrix.json');

  const manifestFiles = runFileQuery("rg --files -g 'manifest.config.ts'");
  const testFiles = runFileQuery("rg --files tests src | rg '(spec\\.ts|spec\\.tsx|test\\.ts|test\\.tsx)$'");

  const absManifestFiles = manifestFiles.map((p) => path.resolve(cwd, p));
  const moduleInfos = absManifestFiles.map((fp) => extractManifestInfo(fp));
  const normalizedTests = testFiles.map((p) => canonicalTestPath(p));
  const explicitMap = collectExplicitManifestMappings(normalizedTests);

  const playwrightJson = readJsonIfExists(args.playwright);
  const vitestJson = readJsonIfExists(args.vitest);
  const playwrightStatus = parsePlaywrightJson(playwrightJson);
  const vitestStatus = parseVitestJson(vitestJson);
  const statusMap = mergeStatuses(playwrightStatus, vitestStatus);

  const modules = moduleInfos
    .map((m) => {
      const mappedTests = mapModuleToTests(m, normalizedTests, explicitMap);
      const agg = aggregateModuleStatus(mappedTests, statusMap);
      return {
        id: m.id,
        manifestPath: m.filePath,
        tokens: m.tokens,
        mappedTests,
        status: agg.status,
        counts: agg.counts,
        runtimeStatusFiles: agg.withRuntimeStatus
      };
    })
    .sort((a, b) => a.id.localeCompare(b.id));

  const summary = {
    totalModules: modules.length,
    mappedModules: modules.filter((m) => m.mappedTests.length > 0).length,
    uncoveredModules: modules.filter((m) => m.mappedTests.length === 0).length,
    passed: modules.filter((m) => m.status === 'passed').length,
    failed: modules.filter((m) => m.status === 'failed').length,
    mappedNotRun: modules.filter((m) => m.status === 'mapped_not_run').length,
    uncovered: modules.filter((m) => m.status === 'uncovered').length,
    skippedOnly: modules.filter((m) => m.status === 'skipped_only').length
  };

  ensureDir(path.dirname(jsonOutput));
  ensureDir(path.dirname(mdOutput));
  ensureDir(outputDir);

  const payload = {
    generatedAt: new Date().toISOString(),
    input: {
      playwrightJson: args.playwright || null,
      vitestJson: args.vitest || null
    },
    summary,
    modules
  };

  fs.writeFileSync(jsonOutput, `${JSON.stringify(payload, null, 2)}\n`, 'utf8');
  fs.writeFileSync(
    mdOutput,
    renderMarkdown({
      generatedAt: payload.generatedAt,
      modules,
      summary
    }),
    'utf8'
  );

  console.log(`matrix_json=${jsonOutput}`);
  console.log(`matrix_md=${mdOutput}`);
  console.log(`modules_total=${summary.totalModules}`);
  console.log(`modules_mapped=${summary.mappedModules}`);
  console.log(`modules_uncovered=${summary.uncoveredModules}`);
}

main();
