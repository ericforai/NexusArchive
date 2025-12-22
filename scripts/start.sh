#!/usr/bin/env bash
# Input: Shell、mkdir
# Output: 启动服务
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"

JAR="$ROOT/backend/nexusarchive-backend-2.0.0.jar"
CONFIG="$ROOT/config/application.yml"
LOG_DIR="$ROOT/logs"

mkdir -p "$LOG_DIR"

echo "Starting backend..."
nohup java -jar "$JAR" --spring.config.location="$CONFIG" > "$LOG_DIR/backend.out" 2>&1 &
echo $! > "$ROOT/run_backend.pid"
echo "Backend started with PID $(cat "$ROOT/run_backend.pid")"