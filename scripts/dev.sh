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
# 3. 等待数据库就绪（使用 Docker healthcheck）
# ==============================================================================
echo -e "${YELLOW}⏳ 等待数据库就绪...${NC}"

# 等待 PostgreSQL（超时 180 秒，CI 环境首次启动需要更长时间）
PG_TIMEOUT=180
PG_COUNT=0
until docker inspect nexus-db --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
    sleep 1
    PG_COUNT=$((PG_COUNT + 1))
    if [ $PG_COUNT -ge $PG_TIMEOUT ]; then
        echo -e "${RED}❌ PostgreSQL 启动超时（${PG_TIMEOUT}秒）${NC}"
        echo -e "${YELLOW}📋 容器状态: docker inspect nexus-db${NC}"
        echo -e "${YELLOW}📋 容器日志: docker logs nexus-db${NC}"
        docker inspect nexus-db --format='{{.State.Health.Status}}' 2>/dev/null || echo "无法获取健康状态"
        exit 1
    fi
    if [ $((PG_COUNT % 15)) -eq 0 ] && [ $PG_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⏳ 等待 PostgreSQL 健康检查... (${PG_COUNT}/${PG_TIMEOUT}秒)${NC}"
    fi
done
echo -e "${GREEN}✅ PostgreSQL 就绪${NC}"

# 等待 Redis（超时 60 秒）
REDIS_TIMEOUT=60
REDIS_COUNT=0
until docker inspect nexus-redis --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
    sleep 1
    REDIS_COUNT=$((REDIS_COUNT + 1))
    if [ $REDIS_COUNT -ge $REDIS_TIMEOUT ]; then
        echo -e "${RED}❌ Redis 启动超时（${REDIS_TIMEOUT}秒）${NC}"
        echo -e "${YELLOW}📋 容器状态: docker inspect nexus-redis${NC}"
        echo -e "${YELLOW}📋 容器日志: docker logs nexus-redis${NC}"
        docker inspect nexus-redis --format='{{.State.Health.Status}}' 2>/dev/null || echo "无法获取健康状态"
        exit 1
    fi
    if [ $((REDIS_COUNT % 15)) -eq 0 ] && [ $REDIS_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⏳ 等待 Redis 健康检查... (${REDIS_COUNT}/${REDIS_TIMEOUT}秒)${NC}"
    fi
done
echo -e "${GREEN}✅ Redis 就绪${NC}"

# ==============================================================================
# 4. 检查是否需要导入 seed data
# ==============================================================================
echo -e ""
# 旧逻辑使用 archive_user（历史表）判断，当前库已改为 sys_user，会导致每次重启都误导入 seed。
# 新逻辑：
# 1) 先判断 sys_user 表是否存在
# 2) 再判断 sys_user 是否有数据
# 仅在“表不存在”或“表为空”时导入 seed
SYS_USER_TABLE_EXISTS=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'sys_user'" 2>/dev/null | tr -d '[:space:]')
if [ -z "$SYS_USER_TABLE_EXISTS" ]; then
    SYS_USER_TABLE_EXISTS="0"
fi

if [ "$SYS_USER_TABLE_EXISTS" = "0" ]; then
    if [ -f db/seed-data.sql ]; then
        echo -e "${YELLOW}📥 首次启动（sys_user 表不存在），导入 seed data...${NC}"
        sleep 2
        # 导入 seed data，保留错误输出用于调试
        if cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive -q 2>&1 | tee /tmp/seed-import.log; then
            echo -e "${GREEN}✅ Seed data 导入完成${NC}"
        else
            echo -e "${RED}❌ Seed data 导入失败${NC}"
            echo -e "${YELLOW}📋 查看错误日志: cat /tmp/seed-import.log${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  db/seed-data.sql 不存在，数据库为空${NC}"
        echo -e "${YELLOW}  如需从另一台 Mac 同步数据，请运行: npm run db:load${NC}"
    fi
else
    SYS_USER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM sys_user" 2>/dev/null | tr -d '[:space:]')
    if [ -z "$SYS_USER_COUNT" ]; then
        SYS_USER_COUNT="0"
    fi
    if [ "$SYS_USER_COUNT" = "0" ] && [ -f db/seed-data.sql ]; then
        echo -e "${YELLOW}📥 检测到数据库为空（sys_user=0），导入 seed data...${NC}"
        if cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive -q 2>&1 | tee /tmp/seed-import.log; then
            echo -e "${GREEN}✅ Seed data 导入完成${NC}"
        else
            echo -e "${RED}❌ Seed data 导入失败${NC}"
            echo -e "${YELLOW}📋 查看错误日志: cat /tmp/seed-import.log${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}✅ 检测到已有业务数据（sys_user=${SYS_USER_COUNT}），跳过 seed 导入${NC}"
    fi
fi

# ==============================================================================
# 4.2 数据库序列健康检查（防止 collection_batch 主键序列回退）
# ==============================================================================
echo -e ""
echo -e "${YELLOW}🩺 检查 collection_batch 序列健康...${NC}"
if bash scripts/check-collection-batch-sequence.sh; then
    echo -e "${GREEN}✅ collection_batch 序列检查完成${NC}"
else
    echo -e "${RED}❌ collection_batch 序列检查失败，请先修复数据库后再启动${NC}"
    exit 1
fi

# ==============================================================================
# 4.5 同步 demo 附件到归档存储根（避免历史错误副本导致预览内容错配）
# ==============================================================================
if [ -d uploads/demo ]; then
    DEMO_SRC_DIR="$(cd . && pwd)/uploads/demo"
    DEMO_DST_DIR="$(cd . && pwd)/nexusarchive-java/data/archives/uploads/demo"
    mkdir -p "$DEMO_DST_DIR"
    # 仅覆盖 demo 附件目录，不影响其它业务文件
    cp -f "$DEMO_SRC_DIR"/* "$DEMO_DST_DIR"/ 2>/dev/null || true
    echo -e "${GREEN}✅ Demo 附件已同步到归档存储目录${NC}"
fi

# ==============================================================================
# 5. 启动后端
# ==============================================================================
echo -e ""
echo -e "${YELLOW}☕ 启动后端...${NC}"
cd nexusarchive-java

# 检查端口 19090 是否已有监听进程（支持接管已有后端）
BACKEND_LISTEN_PID=$(lsof -tiTCP:19090 -sTCP:LISTEN 2>/dev/null | head -n 1)
if [ -n "$BACKEND_LISTEN_PID" ]; then
    if [ -f ../.backend.pid ] && ps -p "$(cat ../.backend.pid)" > /dev/null 2>&1; then
        echo -e "${YELLOW}ℹ️  后端端口正常活跃中${NC}"
    elif ps -p "$BACKEND_LISTEN_PID" -o command= 2>/dev/null | grep -Eq "NexusArchiveApplication|spring-boot"; then
        echo "$BACKEND_LISTEN_PID" > ../.backend.pid
        echo -e "${YELLOW}ℹ️  检测到现有后端进程 (PID: $BACKEND_LISTEN_PID)，已接管 PID 文件${NC}"
    else
        echo -e "${RED}❌ 致命错误：端口 19090 被不明进程 (PID: $BACKEND_LISTEN_PID) 占用！${NC}"
        echo -e "${YELLOW}建议运行: kill -9 $BACKEND_LISTEN_PID${NC}"
        exit 1
    fi
fi

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
    # 设置文件存储根路径（使用绝对路径，避免使用 /tmp 被系统清理）
    # 覆盖 .env.local 中的配置，确保路径正确
    export ARCHIVE_ROOT_PATH="$(cd .. && pwd)/nexusarchive-java/data/archives"
    export ARCHIVE_TEMP_PATH="$(cd .. && pwd)/nexusarchive-java/data/temp"
    # 使用 nohup + stdin 重定向，确保脚本退出后后端进程不被回收
    nohup mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=dev > ../backend.log 2>&1 < /dev/null &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../.backend.pid
    sleep 1
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端启动中 (PID: $BACKEND_PID)${NC}"
        echo -e "${CYAN}📁 文件存储路径: ${ARCHIVE_ROOT_PATH}${NC}"
    else
        echo -e "${RED}❌ 后端进程启动失败，请检查 backend.log !${NC}"
        rm -f ../.backend.pid
        tail -n 20 ../backend.log || true
        exit 1
    fi
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
        exit 1
    fi
    sleep 2
done

# ==============================================================================
# 6.5 附件下载冒烟校验（防止历史 404 问题复发）
# ==============================================================================
echo -e "${YELLOW}🧪 执行附件下载冒烟校验...${NC}"
# 注：冒烟测试凭据从环境变量读取，默认使用开发账号
export SMOKE_USER="${SMOKE_USER:-admin}"
export SMOKE_PASS="${SMOKE_PASS:-admin123}"
ATTACHMENT_SMOKE_STRICT="${ATTACHMENT_SMOKE_STRICT:-false}"
if bash scripts/verify_attachment_download_smoke.sh; then
    echo -e "${GREEN}✅ 附件下载冒烟校验通过${NC}"
else
    if [ "$ATTACHMENT_SMOKE_STRICT" = "true" ]; then
        echo -e "${RED}❌ 附件下载冒烟校验失败（严格模式），停止后续启动${NC}"
        exit 1
    fi
    echo -e "${YELLOW}⚠️  附件下载冒烟校验失败，已跳过阻断（设置 ATTACHMENT_SMOKE_STRICT=true 可改为阻断）${NC}"
fi

# ==============================================================================
# 7. 启动前端
# ==============================================================================
echo -e ""
echo -e "${YELLOW}⚛️  启动前端...${NC}"

# 检查端口 15175 是否被意外占用（防止僵尸进程）
FRONTEND_LISTEN_PID=$(lsof -tiTCP:15175 -sTCP:LISTEN 2>/dev/null | head -n 1)
if [ -n "$FRONTEND_LISTEN_PID" ]; then
    # 如果有 PID 文件且对应进程在运行，那是正常的
    if [ -f .frontend.pid ] && ps -p "$(cat .frontend.pid)" > /dev/null 2>&1; then
        echo -e "${YELLOW}ℹ️  前端端口正常活跃中${NC}"
    # 若没有 PID 文件，但监听进程是 vite，也视为可复用
    elif ps -p "$FRONTEND_LISTEN_PID" -o command= 2>/dev/null | grep -q "vite"; then
        echo "$FRONTEND_LISTEN_PID" > .frontend.pid
        echo -e "${YELLOW}ℹ️  检测到现有 Vite 进程 (PID: $FRONTEND_LISTEN_PID)，已接管 PID 文件${NC}"
    else
        echo -e "${RED}❌ 致命错误：端口 15175 被不明进程 (PID: $FRONTEND_LISTEN_PID) 占用！${NC}"
        echo -e "${YELLOW}建议运行: kill -9 $FRONTEND_LISTEN_PID${NC}"
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
    # 使用 nohup + stdin 重定向，确保脚本退出后前端进程不被回收
    nohup npm run dev:vite > frontend.log 2>&1 < /dev/null &
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
