#!/bin/bash
# ==============================================================================
# 本地开发环境启动脚本
# ==============================================================================
# 用途: 启动本地开发环境（Docker 跑 DB+Redis，本地跑应用）
#
# 使用:
#   bash scripts/dev.sh
#   或: npm run dev
#
# 说明:
#   - 首次运行会自动创建 .env.local
#   - 首次运行会自动导入 db/seed-data.sql
#   - 后端日志输出到 backend.log
#   - 支持"公司←→家里"数据同步
# ==============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}🚀 启动 NexusArchive 本地开发环境${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# ==============================================================================
# 0. 检查系统资源
# ==============================================================================
DISK_USAGE=$(df -h . | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 90 ]; then
    echo -e "${RED}⚠️  警告: 磁盘空间已用 $DISK_USAGE%，剩余空间不足可能导致应用运行异常。${NC}"
fi

# ==============================================================================
# 1. 检查并创建 .env.local
# ==============================================================================
if [ ! -f .env.local ]; then
    echo -e "${YELLOW}📝 .env.local 不存在，从模板创建...${NC}"
    cp .env.example .env.local
    echo -e "${GREEN}✅ 已创建 .env.local${NC}"
    echo -e "${YELLOW}⚠️  请根据需要修改配置${NC}"
fi

# 加载环境变量（导出到子进程）
set -a  # 自动导出所有后续变量
source .env.local
set +a  # 停止自动导出

# ==============================================================================
# 2. 启动基础设施（DB + Redis）
# ==============================================================================
echo -e ""
echo -e "${YELLOW}📦 启动 PostgreSQL + Redis...${NC}"
docker-compose -f docker-compose.infra.yml --env-file .env.local up -d

# ==============================================================================
# 3. 等待数据库就绪
# ==============================================================================
echo -e "${YELLOW}⏳ 等待数据库就绪...${NC}"
until docker exec nexus-db pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo -e "${GREEN}✅ PostgreSQL 就绪${NC}"

# 等待 Redis
until docker exec nexus-redis redis-cli ping > /dev/null 2>&1; do
    sleep 1
done
echo -e "${GREEN}✅ Redis 就绪${NC}"

# ==============================================================================
# 4. 检查是否需要导入 seed data
# ==============================================================================
echo -e ""
# 检查 archive_user 表是否存在并有数据
USER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'archive_user'" 2>/dev/null || echo "0")

if [ "$USER_COUNT" = "0" ]; then
    # 表不存在，可能是首次启动
    if [ -f db/seed-data.sql ]; then
        echo -e "${YELLOW}📥 首次启动，导入 seed data...${NC}"
        # 等待一下确保数据库完全就绪
        sleep 2
        cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive -q > /dev/null 2>&1
        echo -e "${GREEN}✅ Seed data 导入完成${NC}"
    else
        echo -e "${YELLOW}⚠️  db/seed-data.sql 不存在，数据库为空${NC}"
        echo -e "${YELLOW}  如需从另一台 Mac 同步数据，请运行: npm run db:load${NC}"
    fi
else
    # 表存在，检查是否有数据
    USER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM archive_user" 2>/dev/null || echo "0")
    if [ "$USER_COUNT" = "0" ] && [ -f db/seed-data.sql ]; then
        echo -e "${YELLOW}📥 数据库为空，导入 seed data...${NC}"
        cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive -q > /dev/null 2>&1
        echo -e "${GREEN}✅ Seed data 导入完成${NC}"
    fi
fi

# ==============================================================================
# 5. 启动后端
# ==============================================================================
echo -e ""
echo -e "${YELLOW}☕ 启动后端...${NC}"
cd nexusarchive-java

# 检查是否已有进程在运行
if [ -f ../.backend.pid ]; then
    OLD_PID=$(cat ../.backend.pid)
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  后端已在运行 (PID: $OLD_PID)${NC}"
        echo -e "${YELLOW}如需重启，请先运行: npm run dev:stop${NC}"
    else
        rm ../.backend.pid
    fi
fi

if [ ! -f ../.backend.pid ]; then
    mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=dev > ../backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../.backend.pid
    echo -e "${GREEN}✅ 后端启动中 (PID: $BACKEND_PID)${NC}"
fi

cd ..

# ==============================================================================
# 6. 等待后端就绪
# ==============================================================================
echo -e "${YELLOW}⏳ 等待后端 API 就绪...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:19090/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端 API 就绪${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ 后端启动超时，请检查 backend.log !${NC}"
        echo -e "${YELLOW}最后 10 行日志内容：${NC}"
        tail -n 10 backend.log
    fi
    sleep 2
done

# ==============================================================================
# 7. 启动前端
# ==============================================================================
echo -e ""
echo -e "${YELLOW}⚛️  启动前端...${NC}"

# 检查端口 15175 是否被意外占用（防止僵尸进程）
if lsof -i :15175 -t >/dev/null; then
    # 如果有 PID 文件且对应进程在运行，那是正常的
    if [ -f .frontend.pid ] && ps -p $(cat .frontend.pid) > /dev/null 2>&1; then
        echo -e "${YELLOW}ℹ️  前端端口正常活跃中${NC}"
    else
        ZOMBIE_PID=$(lsof -i :15175 -t)
        echo -e "${RED}❌ 致命错误：端口 15175 被不明进程 (PID: $ZOMBIE_PID) 占用！${NC}"
        echo -e "${YELLOW}建议运行: kill -9 $ZOMBIE_PID${NC}"
        exit 1
    fi
fi

# 检查是否已有进程在运行
if [ -f .frontend.pid ]; then
    OLD_PID=$(cat .frontend.pid)
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  前端已在运行 (PID: $OLD_PID)${NC}"
    else
        rm .frontend.pid
    fi
fi

if [ ! -f .frontend.pid ]; then
    npm run dev:vite > frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > .frontend.pid
    echo -e "${GREEN}✅ 前端启动中 (PID: $FRONTEND_PID)${NC}"
fi

# ==============================================================================
# 8. 完成
# ==============================================================================
echo -e ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 开发环境启动完成！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  🌐 前端:     ${GREEN}http://localhost:15175${NC}"
echo -e "  🔌 后端 API: ${GREEN}http://localhost:19090/api${NC}"
echo -e "  📄 后端日志: ${GREEN}tail -f backend.log${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e ""
echo -e "${CYAN}💡 常用命令:${NC}"
echo -e "  停止环境:  ${YELLOW}npm run dev:stop${NC}"
echo -e "  导出数据:  ${YELLOW}npm run db:dump${NC}  (离开公司前执行)"
echo -e "  导入数据:  ${YELLOW}npm run db:load${NC}  (回到家后执行)"
echo -e "  重置数据库: ${YELLOW}npm run db:reset${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
