#!/usr/bin/env node
/**
 * Input: Playwright JSON report.
 * Output: Exit code for integration gate enforcement.
 * Pos: Integration-gate Playwright report validator.
 */

import fs from 'node:fs';

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i += 1) {
    const k = argv[i];
    if (!k.startsWith('--')) continue;
    const key = k.slice(2);
    const v = argv[i + 1];
    if (!v || v.startsWith('--')) {
      args[key] = true;
      continue;
    }
    args[key] = v;
    i += 1;
  }
  return args;
}

function readJson(path) {
  if (!path || !fs.existsSync(path)) {
    throw new Error(`report_not_found: ${path || 'empty'}`);
  }
  return JSON.parse(fs.readFileSync(path, 'utf8'));
}

function collect(report) {
  const totals = { passed: 0, failed: 0, timedOut: 0, interrupted: 0, skipped: 0 };

  function walk(suite) {
    for (const spec of suite.specs || []) {
      for (const t of spec.tests || []) {
        for (const r of t.results || []) {
          const status = r.status || 'unknown';
          if (status === 'passed') totals.passed += 1;
          else if (status === 'failed') totals.failed += 1;
          else if (status === 'timedOut') totals.timedOut += 1;
          else if (status === 'interrupted') totals.interrupted += 1;
          else if (status === 'skipped') totals.skipped += 1;
        }
      }
    }
    for (const child of suite.suites || []) walk(child);
  }

  for (const root of report.suites || []) walk(root);
  return totals;
}

function main() {
  const args = parseArgs(process.argv);
  const report = readJson(args.json);
  const totals = collect(report);
  const failOnSkipped = args['fail-on-skipped'] === true || args['fail-on-skipped'] === 'true';

  console.log(
    `[integration-gate] playwright passed=${totals.passed} failed=${totals.failed} timedOut=${totals.timedOut} interrupted=${totals.interrupted} skipped=${totals.skipped}`
  );

  if (totals.failed > 0 || totals.timedOut > 0 || totals.interrupted > 0) {
    process.exit(2);
  }
  if (failOnSkipped && totals.skipped > 0) {
    process.exit(3);
  }
}

main();
