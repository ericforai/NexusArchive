#!/bin/bash
# 验证序列检查脚本是否能识别 is_called=false 的隐患场景

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

DB_EXEC_MODE="${DB_EXEC_MODE:-docker}"   # docker | direct
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

run_sql() {
  local sql="$1"
  if [ "$DB_EXEC_MODE" = "docker" ]; then
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$sql"
  else
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "$sql"
  fi
}

SEQ_META_ORIG="$(run_sql "SELECT last_value, is_called FROM collection_batch_id_seq;" | tr -d '[:space:]')"
IFS='|' read -r ORIG_LAST ORIG_CALLED <<< "$SEQ_META_ORIG"

if [ -z "$ORIG_LAST" ] || [ -z "$ORIG_CALLED" ]; then
  echo -e "${RED}❌ 无法读取原始序列状态${NC}"
  exit 2
fi

restore_seq() {
  local called_sql="false"
  if [ "$ORIG_CALLED" = "t" ]; then
    called_sql="true"
  fi
  run_sql "SELECT setval('collection_batch_id_seq', ${ORIG_LAST}, ${called_sql});" >/dev/null || true
}
trap restore_seq EXIT

MAX_ID="$(run_sql "SELECT COALESCE(MAX(id), 0) FROM collection_batch;" | tr -d '[:space:]')"
if ! [[ "$MAX_ID" =~ ^[0-9]+$ ]]; then
  echo -e "${RED}❌ 非法 max_id: $MAX_ID${NC}"
  exit 2
fi

echo -e "${YELLOW}=== 验证 is_called=false 拦截 ===${NC}"
echo -e "原始状态: last_value=${ORIG_LAST}, is_called=${ORIG_CALLED}, max_id=${MAX_ID}"

# 人为制造隐患：last_value == max(id) 且 is_called=false
run_sql "SELECT setval('collection_batch_id_seq', ${MAX_ID}, false);" >/dev/null

echo -e "${YELLOW}[1/3] 运行 check-only（预期失败）...${NC}"
if bash scripts/check-collection-batch-sequence.sh --check-only >/dev/null 2>&1; then
  echo -e "${RED}❌ 预期 check-only 失败，但实际成功${NC}"
  exit 1
fi
echo -e "${GREEN}✅ check-only 正确识别到风险并返回失败${NC}"

echo -e "${YELLOW}[2/3] 运行自动修复（预期成功）...${NC}"
bash scripts/check-collection-batch-sequence.sh >/dev/null

echo -e "${YELLOW}[3/3] 验证修复后状态...${NC}"
SEQ_META_FIXED="$(run_sql "SELECT last_value, is_called FROM collection_batch_id_seq;" | tr -d '[:space:]')"
IFS='|' read -r FIXED_LAST FIXED_CALLED <<< "$SEQ_META_FIXED"

if [ -z "$FIXED_LAST" ] || [ -z "$FIXED_CALLED" ]; then
  echo -e "${RED}❌ 修复后读取序列状态失败${NC}"
  exit 1
fi

if ! [[ "$FIXED_LAST" =~ ^[0-9]+$ ]]; then
  echo -e "${RED}❌ 修复后 seq_last 非法: $FIXED_LAST${NC}"
  exit 1
fi

if [ "$FIXED_LAST" -lt "$MAX_ID" ] || [ "$FIXED_CALLED" != "t" ]; then
  echo -e "${RED}❌ 修复结果异常: seq_last=${FIXED_LAST}, is_called=${FIXED_CALLED}${NC}"
  exit 1
fi

echo -e "${GREEN}✅ 修复有效: seq_last=${FIXED_LAST}, is_called=${FIXED_CALLED}${NC}"
echo -e "${GREEN}✅ 验证通过（退出时将恢复原始序列状态）${NC}"
