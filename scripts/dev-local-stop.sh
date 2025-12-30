#!/bin/bash
# 停止本地开发环境

echo "🛑 停止开发服务..."

# 停止前端
lsof -ti :15175 | xargs kill -9 2>/dev/null && echo "✅ 前端已停止" || echo "ℹ️ 前端未运行"

# 停止后端
lsof -ti :19090 | xargs kill -9 2>/dev/null && echo "✅ 后端已停止" || echo "ℹ️ 后端未运行"

echo ""
echo "💡 数据库和 Redis 仍在运行（保留数据）"
echo "   如需停止: docker-compose -f docker-compose.dev.yml down"
