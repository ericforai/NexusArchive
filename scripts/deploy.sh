#!/bin/bash
# ==============================================================================
# 服务器部署脚本
# ==============================================================================
# 用途: 在生产服务器上部署 NexusArchive
#
# 使用:
#   bash scripts/deploy.sh
#   或: npm run deploy
#
# 说明:
#   - 自动拉取最新代码
#   - 构建并启动所有服务（DB + Redis + Backend + Frontend）
#   - 自动导入 seed data
#   - 支持环境变量 RESET_DB=true 重置数据库
# ==============================================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}🚀 NexusArchive 服务器部署${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# ==============================================================================
# 1. 检查环境变量文件
# ==============================================================================
if [ ! -f .env.server ]; then
    echo -e "${RED}❌ .env.server 不存在！${NC}"
    echo -e "${YELLOW}请先创建: cp .env.example .env.server${NC}"
    echo -e "${YELLOW}然后修改 .env.server 中的配置（特别是密码）${NC}"
    exit 1
fi

# 加载环境变量
source .env.server

# ==============================================================================
# 2. 拉取最新代码
# ==============================================================================
echo -e ""
echo -e "${YELLOW}📥 拉取最新代码...${NC}"
git pull

# 检查是否有 seed data 更新
if git diff --name-only HEAD@{1} HEAD | grep -q "db/seed-data.sql"; then
    echo -e "${GREEN}📊 检测到 seed-data.sql 有更新${NC}"
    SEED_UPDATED=true
else
    SEED_UPDATED=false
fi

# ==============================================================================
# 3. 停止旧服务
# ==============================================================================
echo -e ""
echo -e "${YELLOW}🛑 停止旧服务...${NC}"
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server down

# ==============================================================================
# 4. 清理数据库（可选）
# ==============================================================================
if [ "${RESET_DB}" = "true" ]; then
    echo -e "${RED}🗑️  清理数据库（RESET_DB=true）...${NC}"
    docker volume rm nexusarchive-db nexusarchive-redis 2>/dev/null || true
    echo -e "${GREEN}✅ 数据库已清理${NC}"
fi

# ==============================================================================
# 5. 构建镜像
# ==============================================================================
echo -e ""
echo -e "${YELLOW}🔨 构建镜像...${NC}"
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server build
echo -e "${GREEN}✅ 镜像构建完成${NC}"

# ==============================================================================
# 6. 启动服务
# ==============================================================================
echo -e ""
echo -e "${YELLOW}🚀 启动服务...${NC}"
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d

# ==============================================================================
# 7. 等待服务就绪
# ==============================================================================
echo -e ""
echo -e "${YELLOW}⏳ 等待服务就绪...${NC}"

# 等待数据库
for i in {1..30}; do
    if docker exec nexus-db pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PostgreSQL 就绪${NC}"
        break
    fi
    sleep 2
done

# 等待后端 API
for i in {1..60}; do
    if curl -s http://localhost:19090/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端 API 就绪${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ 后端启动超时${NC}"
        echo -e "${YELLOW}请检查日志: docker logs nexus-backend${NC}"
        exit 1
    fi
    sleep 2
done

# 等待前端
for i in {1..30}; do
    if curl -s http://localhost/ > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 前端就绪${NC}"
        break
    fi
    sleep 2
done

# ==============================================================================
# 8. 显示部署状态
# ==============================================================================
echo -e ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 部署完成！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e ""
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server ps
echo -e ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}💡 访问地址:${NC}"
echo -e "  🌐 前端:     ${GREEN}http://localhost${NC}"
echo -e "  🔌 后端 API: ${GREEN}http://localhost:19090/api${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e ""
echo -e "${CYAN}💡 常用命令:${NC}"
echo -e "  查看后端日志: ${YELLOW}docker logs -f nexus-backend${NC}"
echo -e "  查看前端日志: ${YELLOW}docker logs -f nexus-frontend${NC}"
echo -e "  重启服务:     ${YELLOW}docker-compose -f docker-compose.infra.yml -f docker-compose.app.yml --env-file .env.server restart${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
