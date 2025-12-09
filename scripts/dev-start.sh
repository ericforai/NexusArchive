#!/usr/bin/env bash
#
# NexusArchive 开发环境启动脚本
# 确保后端先于前端启动，避免 ECONNREFUSED 错误
#

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
BACKEND_DIR="$ROOT/nexusarchive-java"

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
        if curl -s http://localhost:8080/api/auth/login -X POST &>/dev/null; then
            log_success "后端已就绪 (端口 8080)"
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

# 主流程
main() {
    echo ""
    echo "=========================================="
    echo "   NexusArchive 开发环境启动脚本"
    echo "=========================================="
    echo ""
    
    # 检查端口
    if ! check_port 8080; then
        log_warn "后端端口8080已占用，尝试终止..."
        lsof -ti :8080 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
    
    if ! check_port 5173; then
        log_warn "前端端口5173已占用，尝试终止..."
        lsof -ti :5173 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
    
    # 步骤1：启动后端
    log_info "步骤 1/3: 启动后端服务..."
    cd "$BACKEND_DIR"
    mvn spring-boot:run -q &
    BACKEND_PID=$!
    echo $BACKEND_PID > "$ROOT/.backend.pid"
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
    echo $FRONTEND_PID > "$ROOT/.frontend.pid"
    log_info "前端进程 PID: $FRONTEND_PID"
    
    sleep 3
    echo ""
    log_success "=========================================="
    log_success "   开发环境启动完成！"
    log_success "=========================================="
    echo ""
    log_info "前端地址: http://localhost:5173"
    log_info "后端地址: http://localhost:8080"
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
    
    if [ -f "$ROOT/.backend.pid" ]; then
        kill $(cat "$ROOT/.backend.pid") 2>/dev/null || true
        rm -f "$ROOT/.backend.pid"
    fi
    
    if [ -f "$ROOT/.frontend.pid" ]; then
        kill $(cat "$ROOT/.frontend.pid") 2>/dev/null || true
        rm -f "$ROOT/.frontend.pid"
    fi
    
    # 确保端口释放
    lsof -ti :8080 | xargs kill -9 2>/dev/null || true
    lsof -ti :5173 | xargs kill -9 2>/dev/null || true
    
    log_success "服务已停止"
    exit 0
}

trap cleanup SIGINT SIGTERM

main "$@"
