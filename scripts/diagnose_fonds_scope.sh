#!/bin/bash
# 诊断用户全宗权限问题的脚本
# 用法：./scripts/diagnose_fonds_scope.sh

set -e

echo "=== 诊断用户全宗权限问题 ==="
echo ""

# 检查数据库连接配置（从docker-compose或环境变量读取）
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-54321}"
DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-nexusarchive}"
DB_PASSWORD="${DB_PASSWORD:-nexusarchive}"

echo "数据库连接信息:"
echo "  Host: $DB_HOST:$DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# 检查PostgreSQL客户端是否可用
if ! command -v psql &> /dev/null; then
    echo "错误: 未找到 psql 命令，请安装 PostgreSQL 客户端"
    exit 1
fi

echo "=== 1. 检查现有数据的全宗号分布 ==="
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT fonds_no, COUNT(*) as archive_count 
FROM acc_archive 
WHERE deleted = 0 
  AND fonds_no IS NOT NULL
  AND fonds_no <> ''
GROUP BY fonds_no 
ORDER BY fonds_no;
"

echo ""
echo "=== 2. 检查用户数量 ==="
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT COUNT(*) as user_count 
FROM sys_user 
WHERE deleted = 0;
"

echo ""
echo "=== 3. 检查当前用户全宗权限数量 ==="
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT COUNT(*) as permission_count 
FROM sys_user_fonds_scope 
WHERE deleted = 0;
"

echo ""
echo "=== 4. 检查每个用户的权限分布 ==="
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT u.username, COUNT(s.fonds_no) as permission_count, 
       STRING_AGG(DISTINCT s.fonds_no, ', ' ORDER BY s.fonds_no) as fonds_list
FROM sys_user u
LEFT JOIN sys_user_fonds_scope s ON u.id = s.user_id AND s.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id, u.username
ORDER BY u.username;
"

echo ""
echo "=== 5. 检查是否有数据的全宗号没有对应的用户权限 ==="
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT DISTINCT a.fonds_no, COUNT(a.id) as archive_count
FROM acc_archive a
WHERE a.deleted = 0
  AND a.fonds_no IS NOT NULL
  AND a.fonds_no <> ''
  AND NOT EXISTS (
      SELECT 1 
      FROM sys_user_fonds_scope s 
      WHERE s.fonds_no = a.fonds_no 
        AND s.deleted = 0
  )
GROUP BY a.fonds_no;
"

echo ""
echo "=== 诊断完成 ==="
echo ""
echo "如果步骤5返回了全宗号，说明这些全宗号没有对应的用户权限，需要执行修复脚本"
echo "修复脚本: scripts/fix_fonds_scope_manual.sql"

