#!/bin/bash
# collection_batch 主键序列健康检查
# 功能：
# 1) 比较 collection_batch_id_seq.last_value 与 collection_batch MAX(id)
# 2) 若序列落后，自动 setval 修复（默认）
# 3) --check-only 模式下仅检查并返回非零退出码（适合 CI）

set -euo pipefail

CHECK_ONLY=false
DB_EXEC_MODE="${DB_EXEC_MODE:-docker}"   # docker | direct
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

if [ "${1:-}" = "--check-only" ]; then
  CHECK_ONLY=true
fi

run_sql() {
  local sql="$1"
  if [ "$DB_EXEC_MODE" = "docker" ]; then
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$sql"
  else
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "$sql"
  fi
}

MAX_ID="$(run_sql "SELECT COALESCE(MAX(id), 0) FROM collection_batch;" | tr -d '[:space:]')"
SEQ_META="$(run_sql "SELECT last_value, is_called FROM collection_batch_id_seq;" | tr -d '[:space:]')"
IFS='|' read -r SEQ_LAST SEQ_CALLED <<< "$SEQ_META"

if [ -z "$MAX_ID" ] || [ -z "$SEQ_LAST" ] || [ -z "$SEQ_CALLED" ]; then
  echo "[DB-SEQ-CHECK] ERROR: 无法读取 max(id) 或 sequence 元数据(last_value/is_called)"
  exit 2
fi

if ! [[ "$MAX_ID" =~ ^[0-9]+$ ]] || ! [[ "$SEQ_LAST" =~ ^[0-9]+$ ]]; then
  echo "[DB-SEQ-CHECK] ERROR: 非法数值 max_id=${MAX_ID}, seq_last=${SEQ_LAST}"
  exit 2
fi

if [ "$SEQ_CALLED" != "t" ] && [ "$SEQ_CALLED" != "f" ]; then
  echo "[DB-SEQ-CHECK] ERROR: 非法 is_called 值: ${SEQ_CALLED}"
  exit 2
fi

echo "[DB-SEQ-CHECK] collection_batch max_id=${MAX_ID}, seq_last=${SEQ_LAST}, is_called=${SEQ_CALLED}"

NEEDS_FIX=false
if [ "$SEQ_LAST" -lt "$MAX_ID" ]; then
  NEEDS_FIX=true
elif [ "$SEQ_LAST" -eq "$MAX_ID" ] && [ "$SEQ_CALLED" = "f" ]; then
  # is_called=false 时 nextval 会先返回 last_value 本身，等于 max(id) 会直接撞主键
  NEEDS_FIX=true
fi

if [ "$NEEDS_FIX" = true ]; then
  echo "[DB-SEQ-CHECK] ALERT: sequence 与表数据不同步，可能导致 409/主键冲突"
  if [ "$CHECK_ONLY" = true ]; then
    exit 1
  fi

  run_sql "SELECT setval('collection_batch_id_seq', (SELECT COALESCE(MAX(id), 1) FROM collection_batch), true);" >/dev/null
  NEW_LAST="$(run_sql "SELECT last_value FROM collection_batch_id_seq;" | tr -d '[:space:]')"
  echo "[DB-SEQ-CHECK] FIXED: sequence 已对齐，new_seq_last=${NEW_LAST}, is_called=t"
else
  echo "[DB-SEQ-CHECK] OK: sequence 健康"
fi
