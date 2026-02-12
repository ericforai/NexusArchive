#!/bin/bash
# ==============================================================================
# 全景视图附件一致性校验脚本
# ==============================================================================
# 用途:
#  1) 校验 demo 源目录 与 运行时归档目录 的关键附件是否一致
#  2) (可选) 校验下载接口返回状态码和 Content-Length 是否正确
#
# 用法:
#   bash scripts/verify_panorama_attachments.sh
#   TOKEN="<JWT>" bash scripts/verify_panorama_attachments.sh
#   API_BASE="http://localhost:19090/api" TOKEN="<JWT>" bash scripts/verify_panorama_attachments.sh
# ==============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
API_BASE="${API_BASE:-http://localhost:19090/api}"
TOKEN="${TOKEN:-}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

fail_count=0

print_ok() { echo -e "${GREEN}✅ $1${NC}"; }
print_warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_fail() { echo -e "${RED}❌ $1${NC}"; fail_count=$((fail_count + 1)); }

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}附件一致性校验开始${NC}"
echo -e "${CYAN}ROOT_DIR: ${ROOT_DIR}${NC}"
echo -e "${CYAN}API_BASE: ${API_BASE}${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 文件清单: file_id|source_path|runtime_path|label
FILES=(
  "f4653466-b670-a083-acff-19ff6d55be02|uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf|nexusarchive-java/data/archives/uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf|INV-202311-089"
  "seed-invoice-002-file|uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf|nexusarchive-java/data/archives/uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf|INV-202311-092"
  "file-bank-receipt-1002|uploads/demo/bank_receipt_1002.pdf|nexusarchive-java/data/archives/uploads/demo/bank_receipt_1002.pdf|BR-GROUP-2022-1002-BANK"
  "file-reimbursement-1002|uploads/demo/reimbursement_1002.pdf|nexusarchive-java/data/archives/uploads/demo/reimbursement_1002.pdf|BR-GROUP-2022-1002-REIMB"
)

echo
echo "1) 校验物理文件一致性"
for item in "${FILES[@]}"; do
  IFS='|' read -r file_id source_rel runtime_rel label <<< "$item"
  source_abs="${ROOT_DIR}/${source_rel}"
  runtime_abs="${ROOT_DIR}/${runtime_rel}"

  if [ ! -f "$source_abs" ]; then
    print_fail "${label}: 源文件不存在 -> ${source_abs}"
    continue
  fi
  if [ ! -f "$runtime_abs" ]; then
    print_fail "${label}: 运行时文件不存在 -> ${runtime_abs}"
    continue
  fi

  source_size="$(wc -c < "$source_abs" | tr -d ' ')"
  runtime_size="$(wc -c < "$runtime_abs" | tr -d ' ')"
  source_hash="$(shasum -a 256 "$source_abs" | awk '{print $1}')"
  runtime_hash="$(shasum -a 256 "$runtime_abs" | awk '{print $1}')"

  if [ "$source_size" != "$runtime_size" ]; then
    print_fail "${label}: 大小不一致 source=${source_size} runtime=${runtime_size}"
  elif [ "$source_hash" != "$runtime_hash" ]; then
    print_fail "${label}: 哈希不一致 source=${source_hash} runtime=${runtime_hash}"
  else
    print_ok "${label}: 文件一致 size=${source_size} sha256=${source_hash}"
  fi
done

echo
echo "2) 校验下载接口头部"

health_code="$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/health" || true)"
if [ "$health_code" != "200" ]; then
  print_fail "后端健康检查失败: ${API_BASE}/health -> ${health_code}"
else
  print_ok "后端健康检查通过"
fi

if [ -z "$TOKEN" ]; then
  print_warn "未提供 TOKEN，跳过下载接口验收（只做了物理文件校验）"
else
  for item in "${FILES[@]}"; do
    IFS='|' read -r file_id source_rel _runtime_rel label <<< "$item"
    source_abs="${ROOT_DIR}/${source_rel}"
    expected_size="$(wc -c < "$source_abs" | tr -d ' ')"

    header_file="$(mktemp)"
    code="$(curl -sS -m 20 -D "$header_file" -o /dev/null -w "%{http_code}" \
      -H "Authorization: Bearer ${TOKEN}" \
      "${API_BASE}/archive/files/download/${file_id}" || true)"
    length="$(tr -d '\r' < "$header_file" | awk -F': ' 'tolower($1)=="content-length"{print $2}' | tail -n 1)"
    rm -f "$header_file"

    if [ "$code" != "200" ]; then
      print_fail "${label}: 下载接口返回 ${code}（期望 200）"
      continue
    fi
    if [ -z "$length" ]; then
      print_fail "${label}: 响应头缺失 Content-Length"
      continue
    fi
    if [ "$length" != "$expected_size" ]; then
      print_fail "${label}: Content-Length 异常 header=${length} expected=${expected_size}"
      continue
    fi
    print_ok "${label}: 下载头校验通过 status=200 length=${length}"
  done
fi

echo
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if [ "$fail_count" -eq 0 ]; then
  echo -e "${GREEN}🎯 校验完成：全部通过${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  exit 0
else
  echo -e "${RED}校验完成：发现 ${fail_count} 个问题${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  exit 1
fi
