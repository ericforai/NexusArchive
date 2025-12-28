#!/bin/bash
# ============================================================
# 生产服务器回滚脚本 (Production Server Rollback Script)
# 用途：回滚到上一个版本
# 使用：./scripts/rollback-prod.sh
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}   NexusArchive 生产服务器回滚${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# 获取上一个版本
if [ -f /tmp/nexus-previous-tag ]; then
    PREVIOUS_TAG=$(cat /tmp/nexus-previous-tag)
else
    echo -e "${YELLOW}未找到上一个版本记录，请手动输入：${NC}"
    read -p "请输入要回滚到的 TAG: " PREVIOUS_TAG
fi

CURRENT_TAG=$(grep "^TAG=" .env.prod | cut -d'=' -f2)

echo "  当前版本: $CURRENT_TAG"
echo "  回滚版本: $PREVIOUS_TAG"
echo ""

read -p "确认回滚到 $PREVIOUS_TAG？(y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

# 检查镜像是否存在
if ! docker image inspect nexusarchive-backend:$PREVIOUS_TAG > /dev/null 2>&1; then
    echo -e "${RED}❌ 镜像 nexusarchive-backend:$PREVIOUS_TAG 不存在${NC}"
    echo -e "${YELLOW}可用的镜像版本：${NC}"
    docker images nexusarchive-backend --format "{{.Tag}}"
    exit 1
fi

# 更新 .env.prod
sed -i.bak "s/^TAG=.*/TAG=$PREVIOUS_TAG/" .env.prod
echo "已更新 .env.prod 中的 TAG 为 $PREVIOUS_TAG"

# 重启服务
echo "重启服务..."
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 验证
if curl -fsS http://localhost/ > /dev/null 2>&1 && curl -fsS http://localhost/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 回滚成功！${NC}"
else
    echo -e "${YELLOW}⚠️ 服务可能还在启动中，请稍后检查${NC}"
fi

docker compose -f docker-compose.prod.yml ps
