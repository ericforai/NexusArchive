#!/usr/bin/env bash
set -euo pipefail

# 简易 PostgreSQL 恢复脚本
# 用法：PGHOST=localhost PGPORT=5432 PGUSER=postgres PGPASSWORD=xxx ./scripts/restore_postgres.sh <db_name> <sql_file>

DB_NAME="${1:?db_name required}"
SQL_FILE="${2:?sql_file required}"

echo "Restoring $SQL_FILE into $DB_NAME"
psql -h "${PGHOST:-localhost}" -p "${PGPORT:-5432}" -U "${PGUSER:-postgres}" -d "$DB_NAME" -f "$SQL_FILE"
echo "Done."
