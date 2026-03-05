#!/bin/bash
# Input: Full Playwright + Vitest regression commands.
# Output: Timestamped self-check artifacts + module-test-status matrix.
# Pos: Continuous system self-check entrypoint.

set -u

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR" || exit 1

TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$ROOT_DIR/reports/self-check/$TS"
mkdir -p "$OUT_DIR"
LATEST_DIR="$ROOT_DIR/reports/self-check/latest"
DATE_TAG="$(date +%Y-%m-%d)"
DOC_REPORT="$ROOT_DIR/docs/reports/${DATE_TAG}-system-self-check-matrix.md"

PLAYWRIGHT_JSON="$OUT_DIR/playwright.json"
VITEST_JSON="$OUT_DIR/vitest.json"
MATRIX_JSON="$OUT_DIR/module-test-matrix.json"
MATRIX_MD="$OUT_DIR/module-test-matrix.md"

DEV_RC=99
BACKEND_READY=0
FRONTEND_READY=0
PLAYWRIGHT_RC=99
VITEST_RC=99
MATRIX_RC=99
MATRIX_GUARD_RC=99
STOP_RC=99

echo "[self-check] output=$OUT_DIR"

npm run dev
DEV_RC=$?

for i in {1..90}; do
  if curl -sS http://localhost:19090/api/health >/dev/null 2>&1; then
    BACKEND_READY=1
    break
  fi
  sleep 1
done

for i in {1..90}; do
  if curl -sS -I http://localhost:15175 >/dev/null 2>&1; then
    FRONTEND_READY=1
    break
  fi
  sleep 1
done

if [ "$BACKEND_READY" = "1" ] && [ "$FRONTEND_READY" = "1" ]; then
  # 基础门禁仅执行本地可稳定回归集合；联调场景由 self-check:integration 独立门禁承接
  npx playwright test tests/playwright/api tests/playwright/ui --reporter=json >"$PLAYWRIGHT_JSON"
  PLAYWRIGHT_RC=$?
else
  echo "[self-check] playwright skipped: backend_ready=$BACKEND_READY frontend_ready=$FRONTEND_READY"
fi

npx vitest run --reporter=json --outputFile "$VITEST_JSON"
VITEST_RC=$?

node scripts/system-self-check/generate-module-test-matrix.mjs \
  --playwright "$PLAYWRIGHT_JSON" \
  --vitest "$VITEST_JSON" \
  --output-dir "$OUT_DIR" \
  --json "$MATRIX_JSON" \
  --md "$MATRIX_MD"
MATRIX_RC=$?

if [ "$MATRIX_RC" = "0" ] && [ -f "$MATRIX_JSON" ]; then
  node -e '
const fs = require("fs");
const p = process.argv[1];
const payload = JSON.parse(fs.readFileSync(p, "utf8"));
const s = payload.summary || {};
const failed = Number(s.failed || 0);
const mappedNotRun = Number(s.mappedNotRun || 0);
const uncovered = Number(s.uncovered || 0);
const skippedOnly = Number(s.skippedOnly || 0);
console.log(`[self-check] matrix-guard failed=${failed} mappedNotRun=${mappedNotRun} uncovered=${uncovered} skippedOnly=${skippedOnly}`);
if (failed > 0 || mappedNotRun > 0 || uncovered > 0) {
  process.exit(2);
}
' "$MATRIX_JSON"
  MATRIX_GUARD_RC=$?
else
  MATRIX_GUARD_RC=2
  echo "[self-check] matrix-guard skipped: matrix generation failed or matrix json missing"
fi

mkdir -p "$LATEST_DIR"
cp -f "$MATRIX_JSON" "$LATEST_DIR/module-test-matrix.json"
cp -f "$MATRIX_MD" "$LATEST_DIR/module-test-matrix.md"
cp -f "$MATRIX_MD" "$DOC_REPORT"

printf "n\n" | npm run dev:stop
STOP_RC=$?

echo "SUMMARY DEV=$DEV_RC BACKEND=$BACKEND_READY FRONTEND=$FRONTEND_READY PLAYWRIGHT=$PLAYWRIGHT_RC VITEST=$VITEST_RC MATRIX=$MATRIX_RC MATRIX_GUARD=$MATRIX_GUARD_RC STOP=$STOP_RC OUT=$OUT_DIR"

EXIT_CODE=0
if [ "$DEV_RC" != "0" ] || [ "$BACKEND_READY" != "1" ] || [ "$FRONTEND_READY" != "1" ] || [ "$PLAYWRIGHT_RC" != "0" ] || [ "$VITEST_RC" != "0" ] || [ "$MATRIX_RC" != "0" ] || [ "$MATRIX_GUARD_RC" != "0" ] || [ "$STOP_RC" != "0" ]; then
  EXIT_CODE=1
fi
exit "$EXIT_CODE"
