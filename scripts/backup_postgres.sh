#!/usr/bin/env bash
# Input: Shell、mkdir、pg_dump
# Output: 备份流程
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

# 简易 PostgreSQL 备份脚本
# 用法：PGHOST=localhost PGPORT=5432 PGUSER=postgres PGPASSWORD=xxx ./scripts/backup_postgres.sh <db_name> [output_dir]

DB_NAME="${1:-nexusarchive}"
OUT_DIR="${2:-./backups}"
TIMESTAMP=$(date +"%Y%m%d%H%M%S")

mkdir -p "$OUT_DIR"
FILE="$OUT_DIR/${DB_NAME}_${TIMESTAMP}.sql"

echo "Backing up $DB_NAME to $FILE"
pg_dump -h "${PGHOST:-localhost}" -p "${PGPORT:-5432}" -U "${PGUSER:-postgres}" -F p "$DB_NAME" > "$FILE"
echo "Done."