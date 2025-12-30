#!/bin/bash
# 本地开发环境启动脚本（非 Docker 方式）
# 使用 tmux 在一个窗口中管理后端和前端

set -e
cd "$(dirname "$0")/.."

# 检查依赖
check_deps() {
    command -v mvn >/dev/null 2>&1 || { echo "❌ 需要安装 Maven"; exit 1; }
    command -v node >/dev/null 2>&1 || { echo "❌ 需要安装 Node.js"; exit 1; }
}

# 检查 Docker 依赖服务（数据库和 Redis）
start_deps() {
    echo "📦 启动数据库和 Redis..."
    docker-compose -f docker-compose.dev.yml up -d nexus-db nexus-redis
    
    echo "⏳ 等待数据库就绪..."
    for i in {1..30}; do
        if docker exec nexus-db-dev pg_isready -U postgres >/dev/null 2>&1; then
            echo "✅ 数据库就绪"
            return 0
        fi
        sleep 1
    done
    echo "⚠️ 数据库启动超时，继续尝试..."
}

# 清理旧进程
cleanup() {
    echo "🧹 清理旧进程..."
    lsof -ti :19090 | xargs kill -9 2>/dev/null || true
    lsof -ti :15175 | xargs kill -9 2>/dev/null || true
}

# 启动后端
start_backend() {
    echo "🔧 启动后端..."
    cd nexusarchive-java
    mvn spring-boot:run -DskipTests &
    BACKEND_PID=$!
    cd ..
    
    echo "⏳ 等待后端启动..."
    for i in {1..60}; do
        if curl -s http://localhost:19090/api/health >/dev/null 2>&1; then
            echo "✅ 后端就绪 (PID: $BACKEND_PID)"
            return 0
        fi
        sleep 2
    done
    echo "⚠️ 后端启动超时"
}

# 启动前端
start_frontend() {
    echo "🎨 启动前端..."
    npm run dev &
    FRONTEND_PID=$!
    
    sleep 3
    if curl -s http://localhost:15175 >/dev/null 2>&1; then
        echo "✅ 前端就绪 (PID: $FRONTEND_PID)"
    fi
}

# 显示状态
show_status() {
    echo ""
    echo "=========================================="
    echo "✅ 开发环境启动完成!"
    echo "=========================================="
    echo ""
    echo "   📊 前端:    http://localhost:15175"
    echo "   🔧 后端:    http://localhost:19090/api"
    echo "   🗄️  数据库:  localhost:54321"
    echo "   📦 Redis:   localhost:16379"
    echo ""
    echo "📋 停止服务: ./scripts/dev-local-stop.sh"
    echo ""
}

# 主流程
main() {
    echo "🚀 启动本地开发环境..."
    echo ""
    
    check_deps
    start_deps
    cleanup
    start_backend
    start_frontend
    show_status
    
    # 等待用户中断
    echo "按 Ctrl+C 停止所有服务..."
    wait
}

# 捕获中断信号
trap 'echo ""; echo "🛑 正在停止服务..."; cleanup; exit 0' INT TERM

main
