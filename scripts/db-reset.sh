#!/bin/bash
# ==============================================================================
# 数据库重置脚本 - 删除 Volume 并重新初始化
# ==============================================================================
# 用途: 完全重置数据库，删除 Volume 并重新创建
#
# 使用:
#   bash scripts/db-reset.sh
#   或: npm run db:reset
#
# 说明:
#   - 会删除 nexusarchive-db volume，所有数据将丢失
#   - 重启后自动导入 db/seed-data.sql
#   - 用于开发环境彻底重置
# ==============================================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}🗑️  重置数据库...${NC}"
echo -e "${RED}⚠️  这将删除所有数据！${NC}"

# 确认操作
read -p "确认重置？请输入 'yes' 继续: " confirm
if [ "$confirm" != "yes" ]; then
    echo -e "${YELLOW}已取消${NC}"
    exit 0
fi

# 二次确认
read -p "真的确定吗？数据将无法恢复！(yes/no): " confirm2
if [ "$confirm2" != "yes" ]; then
    echo -e "${YELLOW}已取消${NC}"
    exit 0
fi

# 停止服务
echo -e "${YELLOW}🛑 停止服务...${NC}"
docker-compose -f docker-compose.infra.yml down

# 删除 volume
echo -e "${YELLOW}🗑️  删除数据库 volume...${NC}"
docker volume rm nexusarchive-db 2>/dev/null || echo -e "${YELLOW}  Volume 不存在，跳过${NC}"
docker volume rm nexusarchive-redis 2>/dev/null || echo -e "${YELLOW}  Redis volume 不存在，跳过${NC}"

# 重新启动
echo -e "${YELLOW}🚀 重新启动基础设施...${NC}"
docker-compose -f docker-compose.infra.yml up -d

# 等待数据库就绪
echo -e "${YELLOW}⏳ 等待数据库就绪...${NC}"
until docker exec nexus-db pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done

# 检查是否有 seed data
if [ -f db/seed-data.sql ]; then
    echo -e "${GREEN}✅ 数据库已重置，seed data 已自动导入${NC}"

    # 显示数据统计
    sleep 2
    USER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM archive_user" 2>/dev/null || echo "0")
    echo -e "  用户数: ${USER_COUNT}"
else
    echo -e "${YELLOW}⚠️  db/seed-data.sql 不存在，数据库为空${NC}"
    echo -e "${YELLOW}  如需导入数据，请运行: npm run db:load${NC}"
fi

echo -e ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 数据库重置完成！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
