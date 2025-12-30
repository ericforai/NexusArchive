#!/bin/bash
# ==============================================================================
# NexusArchive 开发环境一键启动脚本
# 用法: ./scripts/dev-start.sh
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🚀 启动 NexusArchive 开发环境..."
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker Desktop"
    exit 1
fi

# 检查 .env.dev 文件
if [ ! -f ".env.dev" ]; then
    echo "⚠️  未找到 .env.dev，使用默认配置..."
fi

# 启动所有服务（多阶段构建，Docker 内自动编译）
echo "🏗️  构建镜像并启动服务（首次可能需要 3-5 分钟）..."
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d --build

echo ""
echo "⏳ 等待服务健康检查..."
sleep 5

# 检查服务状态
docker-compose -f docker-compose.dev.yml ps

echo ""
echo "✅ 开发环境启动完成!"
echo ""
echo "   📊 前端:    http://localhost:15175"
echo "   🔧 后端:    http://localhost:19090/api"
echo "   🩺 健康:    http://localhost:19090/api/health"
echo "   🗄️  数据库:  localhost:54321"
echo "   📦 Redis:   localhost:16379"
echo ""
echo "📋 常用命令:"
echo "   查看日志:   docker-compose -f docker-compose.dev.yml logs -f"
echo "   停止服务:   ./scripts/dev-stop.sh"
echo ""