#!/bin/bash
# ==============================================================================
# 数据导入脚本 - 从 seed-data.sql 导入数据到 Docker PostgreSQL
# ==============================================================================
# 用途: 将 db/seed-data.sql 导入到当前数据库
#
# 使用:
#   bash scripts/db-load.sh
#   或: npm run db:load
#
# 说明:
#   - 会先清空现有数据，然后导入 seed data
#   - 回到家后执行此命令，同步公司 Mac 的数据
#   - 离开公司前在公司 Mac 执行 db-dump.sh 导出数据
# ==============================================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 加载环境变量（导出到子进程）
if [ -f .env.local ]; then
    set -a
    source .env.local
    set +a
elif [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# 默认值
DB_NAME=${DB_NAME:-nexusarchive}
DB_USER=${DB_USER:-postgres}
DB_PORT=${DB_PORT:-54321}

echo -e "${YELLOW}📥 导入数据库数据...${NC}"
echo -e "  数据库: ${DB_NAME}"
echo -e "  端口: ${DB_PORT}"

# 检查 seed-data.sql 是否存在
if [ ! -f db/seed-data.sql ]; then
    echo -e "${RED}❌ db/seed-data.sql 不存在！${NC}"
    echo -e "${YELLOW}请先在公司 Mac 上执行: npm run db:dump${NC}"
    exit 1
fi

# 检查容器是否运行
if ! docker ps | grep -q nexus-db; then
    echo -e "${RED}❌ nexus-db 容器未运行！${NC}"
    echo -e "${YELLOW}请先启动: npm run dev${NC}"
    exit 1
fi

# 确认操作
echo -e "${YELLOW}⚠️  警告：这会清空当前数据库的所有数据！${NC}"
read -p "确认导入？(yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo -e "${YELLOW}已取消${NC}"
    exit 0
fi

# 清空现有数据（保留结构）
echo -e "${YELLOW}🗑️  清空现有数据...${NC}"
docker exec nexus-db psql -U ${DB_USER} -d ${DB_NAME} \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" \
    > /dev/null 2>&1 || echo -e "${YELLOW}  (数据库为空，跳过清理)${NC}"

# 导入 seed data
echo -e "${YELLOW}📥 导入 seed data...${NC}"
cat db/seed-data.sql | docker exec -i nexus-db psql -U ${DB_USER} -d ${DB_NAME} -q

echo -e "${GREEN}✅ 数据导入完成！${NC}"

# 显示数据统计
USER_COUNT=$(docker exec nexus-db psql -U ${DB_USER} -d ${DB_NAME} -tAc "SELECT COUNT(*) FROM archive_user" 2>/dev/null || echo "0")
echo -e "  用户数: ${USER_COUNT}"

echo -e ""
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 数据已同步！现在可以继续开发了${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
