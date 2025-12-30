#!/bin/bash
# ==============================================================================
# NexusArchive 开发环境日志查看脚本
# 用法: ./scripts/dev-logs.sh [service]
# 示例: ./scripts/dev-logs.sh nexus-backend
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

if [ -n "$1" ]; then
    docker-compose -f docker-compose.dev.yml logs -f "$1"
else
    docker-compose -f docker-compose.dev.yml logs -f
fi
