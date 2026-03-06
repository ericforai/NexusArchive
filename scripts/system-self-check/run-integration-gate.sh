#!/bin/bash
# Input: Playwright integration test suites with external/system dependencies.
# Output: Hard gate result for interoperability/integration pipeline.
# Pos: Integration gate entrypoint.

set -u

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR" || exit 1

TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$ROOT_DIR/reports/self-check/integration/$TS"
mkdir -p "$OUT_DIR"

PLAYWRIGHT_JSON="$OUT_DIR/playwright-integration.json"

DEV_RC=99
BACKEND_READY=0
FRONTEND_READY=0
PLAYWRIGHT_RC=99
VERIFY_RC=99
STOP_RC=99

echo "[integration-gate] output=$OUT_DIR"

npm run dev
DEV_RC=$?

# CI 环境首次启动需要更长时间（最多 5 分钟）
for i in {1..300}; do
  if curl -sS http://localhost:19090/api/health >/dev/null 2>&1; then
    BACKEND_READY=1
    break
  fi
  # 每 30 秒输出一次进度
  if [ $((i % 30)) -eq 0 ] && [ $i -gt 0 ]; then
    echo "[integration-gate] 等待后端 API 就绪... (${i}s)"
  fi
  sleep 1
done

# 如果后端未就绪，输出 backend.log 诊断
if [ "$BACKEND_READY" != "1" ]; then
  echo "[integration-gate] 后端 API 未就绪，输出 backend.log:"
  if [ -f backend.log ]; then
    tail -n 50 backend.log
  else
    echo "[integration-gate] backend.log 不存在"
  fi
fi

# 前端启动等待（最多 3 分钟）
for i in {1..180}; do
  if curl -sS -I http://localhost:15175 >/dev/null 2>&1; then
    FRONTEND_READY=1
    break
  fi
  # 每 30 秒输出一次进度
  if [ $((i % 30)) -eq 0 ] && [ $i -gt 0 ]; then
    echo "[integration-gate] 等待前端就绪... (${i}s)"
  fi
  sleep 1
done

if [ "$BACKEND_READY" = "1" ] && [ "$FRONTEND_READY" = "1" ]; then
  YONSUITE_E2E=1 DELIVERY_E2E=1 npx playwright test \
    src/e2e/yonsuite-scenarios.spec.ts \
    src/e2e/yonsuite-verification.spec.ts \
    tests/playwright/delivery_v2.spec.ts \
    --reporter=json >"$PLAYWRIGHT_JSON"
  PLAYWRIGHT_RC=$?
else
  echo "[integration-gate] playwright skipped: backend_ready=$BACKEND_READY frontend_ready=$FRONTEND_READY"
fi

if [ "$PLAYWRIGHT_RC" = "0" ]; then
  node scripts/system-self-check/verify-playwright-gate.mjs \
    --json "$PLAYWRIGHT_JSON" \
    --fail-on-skipped true
  VERIFY_RC=$?
else
  VERIFY_RC=2
fi

printf "n\n" | npm run dev:stop
STOP_RC=$?

echo "SUMMARY DEV=$DEV_RC BACKEND=$BACKEND_READY FRONTEND=$FRONTEND_READY PLAYWRIGHT=$PLAYWRIGHT_RC VERIFY=$VERIFY_RC STOP=$STOP_RC OUT=$OUT_DIR"

EXIT_CODE=0
if [ "$DEV_RC" != "0" ] || [ "$BACKEND_READY" != "1" ] || [ "$FRONTEND_READY" != "1" ] || [ "$PLAYWRIGHT_RC" != "0" ] || [ "$VERIFY_RC" != "0" ] || [ "$STOP_RC" != "0" ]; then
  EXIT_CODE=1
fi
exit "$EXIT_CODE"
