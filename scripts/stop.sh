#!/usr/bin/env bash
# Input: Shell、rm
# Output: 停止服务
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"

PID_FILE="$ROOT/run_backend.pid"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  echo "Stopping backend PID $PID"
  kill "$PID" || true
  rm -f "$PID_FILE"
else
  echo "PID file not found, backend may not be running."
fi