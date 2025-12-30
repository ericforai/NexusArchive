#!/bin/bash
# ==============================================================================
# NexusArchive 开发环境停止脚本
# 用法: ./scripts/dev-stop.sh
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🛑 停止 NexusArchive 开发环境..."

docker-compose -f docker-compose.dev.yml down

echo ""
echo "✅ 开发环境已停止"
echo ""
echo "💡 提示: 数据库数据保留在 Docker Volume 中"
echo "   完全清理: docker-compose -f docker-compose.dev.yml down -v"
echo ""
