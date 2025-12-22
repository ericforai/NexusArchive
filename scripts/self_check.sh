#!/usr/bin/env bash
# Input: Shell
# Output: 健康检查
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

echo "== Self Check =="

# Check Java
if command -v java >/dev/null 2>&1; then
  echo "[OK] Java found: $(java -version 2>&1 | head -n1)"
else
  echo "[WARN] Java not found"
fi

# Disk space (root)
DISK_FREE=$(df -h / | tail -1 | awk '{print $4}')
echo "[INFO] Disk free: $DISK_FREE"

# Port check (8080)
if lsof -i :8080 >/dev/null 2>&1; then
  echo "[WARN] Port 8080 in use"
else
  echo "[OK] Port 8080 free"
fi

# DB connectivity (optional)
if command -v psql >/dev/null 2>&1; then
  if PGHOST=${PGHOST:-localhost} PGPORT=${PGPORT:-5432} PGUSER=${PGUSER:-postgres} PGPASSWORD=${PGPASSWORD:-""} psql -c "SELECT 1" >/dev/null 2>&1; then
    echo "[OK] PostgreSQL reachable"
  else
    echo "[WARN] PostgreSQL not reachable with current env (PGHOST/PGPORT/PGUSER/PGPASSWORD)"
  fi
else
  echo "[WARN] psql not installed, skip DB check"
fi