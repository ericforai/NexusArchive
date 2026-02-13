#!/bin/bash
# ==============================================================================
# 附件下载冒烟校验脚本
# ==============================================================================
# 用途:
#  1) 通过登录接口获取 JWT
#  2) 校验关键附件下载接口可用性（HTTP 200 + PDF 类型）
#
# 用法:
#   SMOKE_USER="admin" SMOKE_PASS="your-password" bash scripts/verify_attachment_download_smoke.sh
#   API_BASE="http://localhost:19090/api" SMOKE_USER="admin" SMOKE_PASS="..." bash scripts/verify_attachment_download_smoke.sh
# ==============================================================================

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:19090/api}"
SMOKE_USER="${SMOKE_USER:-}"
SMOKE_PASS="${SMOKE_PASS:-}"

# 强制要求通过环境变量提供凭据（禁止硬编码密码）
if [ -z "$SMOKE_USER" ] || [ -z "$SMOKE_PASS" ]; then
  echo "ERROR: SMOKE_USER and SMOKE_PASS environment variables are required" >&2
  echo "Usage: SMOKE_USER=\"admin\" SMOKE_PASS=\"...\" bash $0" >&2
  exit 1
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

fail_count=0

print_ok() { echo -e "${GREEN}✅ $1${NC}"; }
print_fail() { echo -e "${RED}❌ $1${NC}"; fail_count=$((fail_count + 1)); }

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}附件下载冒烟校验开始${NC}"
echo -e "${CYAN}API_BASE: ${API_BASE}${NC}"
echo -e "${CYAN}SMOKE_USER: ${SMOKE_USER}${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

health_code="$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/health" || true)"
if [ "$health_code" != "200" ]; then
  print_fail "后端健康检查失败: ${API_BASE}/health -> ${health_code}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  exit 1
fi
print_ok "后端健康检查通过"

login_resp="$(curl -sS -m 15 -X POST "${API_BASE}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${SMOKE_USER}\",\"password\":\"${SMOKE_PASS}\"}" || true)"
token="$(printf "%s" "$login_resp" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
if [ -z "$token" ]; then
  print_fail "登录失败，无法获取 token（请检查 SMOKE_USER/SMOKE_PASS）"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  exit 1
fi
print_ok "登录成功，token 已获取"

# 关键附件 fileId 清单
FILE_IDS=(
  "seed-invoice-002-file"
  "demo-file-002"
  "file-invoice-002"
)

for file_id in "${FILE_IDS[@]}"; do
  header_file="$(mktemp)"
  code="$(curl -sS -m 20 -D "$header_file" -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer ${token}" \
    "${API_BASE}/archive/files/download/${file_id}" || true)"
  content_type="$(tr -d '\r' < "$header_file" | awk -F': ' 'tolower($1)=="content-type"{print tolower($2)}' | tail -n 1)"
  rm -f "$header_file"

  if [ "$code" != "200" ]; then
    print_fail "${file_id}: 下载接口返回 ${code}（期望 200）"
    continue
  fi
  if ! printf "%s" "$content_type" | grep -q "application/pdf"; then
    print_fail "${file_id}: Content-Type 非 PDF（实际: ${content_type:-<empty>}）"
    continue
  fi
  print_ok "${file_id}: 下载接口通过（200 + application/pdf）"
done

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if [ "$fail_count" -eq 0 ]; then
  echo -e "${GREEN}🎯 附件下载冒烟校验全部通过${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  exit 0
fi

echo -e "${RED}校验失败：共 ${fail_count} 项异常${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
exit 1
