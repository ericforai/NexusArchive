#!/bin/bash
# ==============================================================================
# 数据导出脚本 - 从 Docker PostgreSQL 导出数据到 seed-data.sql
# ==============================================================================
# 用途: 将当前数据库数据导出到 db/seed-data.sql
#
# 使用:
#   bash scripts/db-dump.sh
#   或: npm run db:dump
#
# 说明:
#   - 导出的文件可以提交到 Git，用于两台 Mac 同步
#   - 公司离开前执行此命令，回到家后用 db-load.sh 导入
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

echo -e "${YELLOW}📤 导出数据库数据...${NC}"
echo -e "  数据库: ${DB_NAME}"
echo -e "  端口: ${DB_PORT}"

# 检查容器是否运行
if ! docker ps | grep -q nexus-db; then
    echo -e "${RED}❌ nexus-db 容器未运行！${NC}"
    echo -e "${YELLOW}请先启动: docker-compose -f docker-compose.infra.yml up -d${NC}"
    exit 1
fi

# 确保目录存在
mkdir -p db

# 导出数据
docker exec nexus-db pg_dump -U ${DB_USER} \
    --no-owner \
    --no-acl \
    --schema=public \
    --disable-triggers \
    ${DB_NAME} > db/seed-data.sql

# 检查导出是否成功
if [ -s db/seed-data.sql ]; then
    FILE_SIZE=$(wc -c < db/seed-data.sql | tr -d ' ')
    echo -e "${GREEN}✅ 数据已导出到 db/seed-data.sql${NC}"
    echo -e "  文件大小: ${FILE_SIZE} bytes"

    # 提示下一步
    echo -e ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}💡 下一步操作:${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  1. 检查导出的文件: cat db/seed-data.sql"
    echo -e "  2. 提交到 Git:"
    echo -e "     ${GREEN}git add db/seed-data.sql${NC}"
    echo -e "     ${GREEN}git commit -m \"sync: update seed data\"${NC}"
    echo -e "     ${GREEN}git push${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
else
    echo -e "${RED}❌ 导出失败！${NC}"
    exit 1
fi
