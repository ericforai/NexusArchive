#!/bin/bash
# ==============================================================================
# 本地开发环境停止脚本
# ==============================================================================
# 用途: 停止本地开发环境
#
# 使用:
#   bash scripts/dev-stop.sh
#   或: npm run dev:stop
#
# 说明:
#   - 停止前端和后端进程
#   - 可选择是否停止 Docker 容器（DB + Redis）
# ==============================================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}🛑 停止开发环境...${NC}"

# ==============================================================================
# 1. 停止前端
# ==============================================================================
if [ -f .frontend.pid ]; then
    FRONTEND_PID=$(cat .frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}⚛️  停止前端 (PID: $FRONTEND_PID)...${NC}"
        kill $FRONTEND_PID 2>/dev/null || true
        # 等待进程结束
        for i in {1..10}; do
            if ! ps -p $FRONTEND_PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        # 如果还没结束，强制杀死
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            kill -9 $FRONTEND_PID 2>/dev/null || true
        fi
        echo -e "${GREEN}✅ 前端已停止${NC}"
    else
        echo -e "${YELLOW}⚠️  前端进程不存在${NC}"
    fi
    rm .frontend.pid
else
    echo -e "${YELLOW}⚠️  未找到前端 PID 文件${NC}"
fi

# ==============================================================================
# 2. 停止后端
# ==============================================================================
if [ -f .backend.pid ]; then
    BACKEND_PID=$(cat .backend.pid)
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}☕ 停止后端 (PID: $BACKEND_PID)...${NC}"
        kill $BACKEND_PID 2>/dev/null || true
        # 等待进程结束
        for i in {1..10}; do
            if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        # 如果还没结束，强制杀死
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            kill -9 $BACKEND_PID 2>/dev/null || true
        fi
        echo -e "${GREEN}✅ 后端已停止${NC}"
    else
        echo -e "${YELLOW}⚠️  后端进程不存在${NC}"
    fi
    rm .backend.pid
else
    echo -e "${YELLOW}⚠️  未找到后端 PID 文件${NC}"
fi

# ==============================================================================
# 3. 询问是否停止 Docker 容器
# ==============================================================================
echo -e ""
echo -e "${YELLOW}是否同时停止 Docker 容器 (DB + Redis)?${NC}"
echo -e "  ${GREEN}y${NC} - 停止所有容器"
echo -e "  ${YELLOW}n${NC} - 只停止应用，保留容器运行"
read -p "请选择 (y/n): " stop_containers

if [ "$stop_containers" = "y" ] || [ "$stop_containers" = "Y" ]; then
    echo -e "${YELLOW}🐳 停止 Docker 容器...${NC}"
    docker-compose -f docker-compose.infra.yml down
    echo -e "${GREEN}✅ Docker 容器已停止${NC}"
else
    echo -e "${YELLOW}🐳 Docker 容器保持运行${NC}"
    echo -e "${YELLOW}   下次启动应用会更快${NC}"
fi

# ==============================================================================
# 4. 清理端口占用（如果有残留进程）
# ==============================================================================
echo -e ""
# 检查 19090 端口
if lsof -ti:19090 > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  端口 19090 仍有进程占用，尝试清理...${NC}"
    lsof -ti:19090 | xargs kill -9 2>/dev/null || true
fi

# 检查 15175 端口
if lsof -ti:15175 > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  端口 15175 仍有进程占用，尝试清理...${NC}"
    lsof -ti:15175 | xargs kill -9 2>/dev/null || true
fi

echo -e ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 开发环境已停止${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
