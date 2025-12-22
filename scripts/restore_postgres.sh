#!/usr/bin/env bash
# Input: Shell、psql
# Output: 恢复流程
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

# 简易 PostgreSQL 恢复脚本
# 用法：PGHOST=localhost PGPORT=5432 PGUSER=postgres PGPASSWORD=xxx ./scripts/restore_postgres.sh <db_name> <sql_file>

DB_NAME="${1:?db_name required}"
SQL_FILE="${2:?sql_file required}"

echo "Restoring $SQL_FILE into $DB_NAME"
psql -h "${PGHOST:-localhost}" -p "${PGPORT:-5432}" -U "${PGUSER:-postgres}" -d "$DB_NAME" -f "$SQL_FILE"
echo "Done."