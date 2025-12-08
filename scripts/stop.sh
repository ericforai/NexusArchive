#!/usr/bin/env bash
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
