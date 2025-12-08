#!/usr/bin/env bash
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
