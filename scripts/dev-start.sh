#!/usr/bin/env bash
#
# NexusArchive 开发环境启动脚本
# 确保后端先于前端启动，避免 ECONNREFUSED 错误
#

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
BACKEND_DIR="$ROOT/nexusarchive-java"

# 加载环境变量文件 (如果存在)
if [ -f "$ROOT/.env" ]; then
    echo "加载环境变量: $ROOT/.env"
    set -a  # 自动导出变量
    source "$ROOT/.env"
    set +a
fi

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查后端是否就绪
wait_for_backend() {
    local max_attempts=60
    local attempt=1
    
    log_info "等待后端启动..."
    
    while [ $attempt -le $max_attempts ]; do
        # 检查8080端口是否在监听
        if curl -s http://127.0.0.1:19090/api/auth/login -X POST &>/dev/null; then
            log_success "后端已就绪 (端口 19090)"
            return 0
        fi
        
        printf "."
        sleep 1
        ((attempt++))
    done
    
    echo ""
    log_error "后端启动超时（${max_attempts}秒）"
    return 1
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i ":$port" &>/dev/null; then
        log_warn "端口 $port 已被占用"
        return 1
    fi
    return 0
}

# 检查依赖服务连接
check_service_connection() {
    local host=$1
    local port=$2
    local name=$3
    local max_attempts=5 # 快速检查，5秒超时
    local attempt=1

    log_info "检查 $name ($host:$port)..."

    while [ $attempt -le $max_attempts ]; do
        if nc -z "$host" "$port" 2>/dev/null || timeout 1 bash -c "cat < /dev/null > /dev/tcp/$host/$port" 2>/dev/null; then
            log_success "$name 已就绪"
            return 0
        fi
        
        sleep 1
        ((attempt++))
    done

    log_warn "$name 未就绪或无法连接 (尝试了 $max_attempts 次)"
    return 1
}

# 主流程
main() {
    echo ""
    echo "=========================================="
    echo "   NexusArchive 开发环境启动脚本"
    echo "=========================================="
    echo ""
    
    # 0. 预检依赖服务
    log_info "步骤 0/3: 检查依赖服务..."
    
    # 数据库 (默认 5432)
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-5432}
    if ! check_service_connection "$DB_HOST" "$DB_PORT" "PostgreSQL"; then
        log_warn "继续启动，但后端可能会因无法连接数据库而失败..."
        sleep 2
    fi

    # Redis (默认 6379)
    REDIS_HOST=${REDIS_HOST:-localhost}
    REDIS_PORT=${REDIS_PORT:-6379}
    if ! check_service_connection "$REDIS_HOST" "$REDIS_PORT" "Redis"; then
        log_warn "继续启动，但后端可能会因无法连接Redis而失败..."
        sleep 2
    fi

    # 检查端口
    if ! check_port 19090; then
        log_warn "后端端口 19090 已占用，尝试终止..."
        lsof -ti :19090 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
    
    if ! check_port 15175; then
        log_warn "前端端口 15175 已占用，尝试终止..."
        lsof -ti :15175 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
    
    # 清理 Vite 缓存以防止端口残留
    log_info "清理 Vite 缓存..."
    rm -rf "$ROOT/node_modules/.vite" || true
    
    # Ensure logs directory exists
    mkdir -p "$ROOT/logs"

    # 步骤1：启动后端
    log_info "步骤 1/3: 启动后端服务..."
    cd "$BACKEND_DIR"
    mvn spring-boot:run -Dmaven.test.skip=true > "$ROOT/logs/backend.log" 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > "$ROOT/logs/backend.pid"
    log_info "后端进程 PID: $BACKEND_PID"
    
    # 步骤2：等待后端就绪
    log_info "步骤 2/3: 等待后端就绪..."
    if ! wait_for_backend; then
        log_error "后端启动失败，请检查日志"
        exit 1
    fi
    
    # 步骤3：启动前端
    log_info "步骤 3/3: 启动前端服务..."
    cd "$ROOT"
    npm run dev &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > "$ROOT/logs/frontend.pid"
    log_info "前端进程 PID: $FRONTEND_PID"
    
    sleep 3
    echo ""
    log_success "=========================================="
    log_success "   开发环境启动完成！"
    log_success "=========================================="
    echo ""
    log_info "前端地址: http://localhost:15175"
    log_info "后端地址: http://localhost:19090"
    echo ""
    log_info "按 Ctrl+C 停止服务"
    echo ""
    
    # 等待子进程
    wait
}

# 清理函数
cleanup() {
    echo ""
    log_warn "正在停止服务..."
    
    if [ -f "$ROOT/logs/backend.pid" ]; then
        kill $(cat "$ROOT/logs/backend.pid") 2>/dev/null || true
        rm -f "$ROOT/logs/backend.pid"
    fi
    
    if [ -f "$ROOT/logs/frontend.pid" ]; then
        kill $(cat "$ROOT/logs/frontend.pid") 2>/dev/null || true
        rm -f "$ROOT/logs/frontend.pid"
    fi
    
    # 确保端口释放
    lsof -ti :19090 | xargs kill -9 2>/dev/null || true
    lsof -ti :15175 | xargs kill -9 2>/dev/null || true
    
    log_success "服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

main "$@"
