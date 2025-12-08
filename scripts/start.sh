#!/usr/bin/env bash
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
