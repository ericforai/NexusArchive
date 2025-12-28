#!/bin/bash
# ============================================================
# 生产服务器升级脚本 (Production Server Upgrade Script)
# 用途：拉取最新代码、重新构建镜像、重启服务（保留现有配置）
# 使用：./scripts/upgrade-prod.sh
# 版本：v1.0
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
echo -e "${BLUE}   NexusArchive 生产服务器升级脚本 v1.0${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# ========== Step 0: 检查前置条件 ==========
echo -e "${YELLOW}[0/7] 检查前置条件...${NC}"

# 检查 Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行${NC}"
    exit 1
fi

# 检查 .env.prod 是否存在
if [ ! -f "$PROJECT_ROOT/.env.prod" ]; then
    echo -e "${RED}❌ .env.prod 文件不存在${NC}"
    echo -e "${YELLOW}请先创建 .env.prod 配置文件，参考模板：${NC}"
    echo ""
    echo "TAG=latest"
    echo "DB_HOST=nexus-db"
    echo "DB_PORT=5432"
    echo "DB_PASSWORD=your_password"
    echo "SM4_KEY=your_sm4_key"
    echo "SPRING_PROFILES_ACTIVE=prod"
    exit 1
fi

echo -e "${GREEN}✅ 前置条件检查通过${NC}"
echo ""

# ========== Step 1: 备份当前版本信息 ==========
echo -e "${YELLOW}[1/7] 记录当前版本...${NC}"
CURRENT_TAG=$(grep "^TAG=" .env.prod | cut -d'=' -f2 || echo "unknown")
echo "  当前版本 TAG: $CURRENT_TAG"
echo "$CURRENT_TAG" > /tmp/nexus-previous-tag
echo -e "${GREEN}✅ 版本信息已记录（回滚用）${NC}"
echo ""

# ========== Step 2: 备份数据库（可选）==========
echo -e "${YELLOW}[2/7] 数据库备份...${NC}"
read -p "是否在升级前备份数据库？(推荐) (Y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    BACKUP_FILE="$PROJECT_ROOT/backup_prod_$(date +%Y%m%d_%H%M%S).sql"
    if docker compose -f docker-compose.prod.yml exec -T nexus-db pg_dump -U postgres -d nexusarchive > "$BACKUP_FILE" 2>/dev/null; then
        BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        echo -e "${GREEN}✅ 数据库已备份: $BACKUP_FILE ($BACKUP_SIZE)${NC}"
    else
        echo -e "${YELLOW}⚠️ 数据库备份失败（可能服务未运行），继续升级...${NC}"
    fi
else
    echo "跳过数据库备份"
fi
echo ""

# ========== Step 3: 拉取最新代码 ==========
echo -e "${YELLOW}[3/7] 拉取最新代码...${NC}"
git pull origin main
echo -e "${GREEN}✅ 代码已更新${NC}"
echo ""

# ========== Step 4: 构建后端 JAR ==========
echo -e "${YELLOW}[4/7] 构建后端 JAR...${NC}"
cd "$PROJECT_ROOT/nexusarchive-java"
mvn clean package -DskipTests -q
if [ ! -f target/nexusarchive-backend-*.jar ]; then
    echo -e "${RED}❌ JAR 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✅ JAR 构建完成${NC}"
cd "$PROJECT_ROOT"
echo ""

# ========== Step 5: 构建 Docker 镜像 ==========
echo -e "${YELLOW}[5/7] 构建 Docker 镜像...${NC}"
NEW_TAG=$(git rev-parse --short HEAD)
echo "  新版本 TAG: $NEW_TAG"

# 构建后端镜像
echo "  构建后端镜像..."
docker build -t nexusarchive-backend:$NEW_TAG -f nexusarchive-java/Dockerfile nexusarchive-java

# 构建前端镜像
echo "  构建前端镜像..."
docker build -t nexusarchive-web:$NEW_TAG -f Dockerfile.frontend.prod .

echo -e "${GREEN}✅ 镜像构建完成${NC}"
echo ""

# ========== Step 6: 更新配置并重启服务 ==========
echo -e "${YELLOW}[6/7] 更新配置并重启服务...${NC}"

# 更新 .env.prod 中的 TAG
sed -i.bak "s/^TAG=.*/TAG=$NEW_TAG/" .env.prod
echo "  已更新 .env.prod 中的 TAG 为 $NEW_TAG"

# 重启服务
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

echo -e "${GREEN}✅ 服务已重启${NC}"
echo ""

# ========== Step 7: 验证服务 ==========
echo -e "${YELLOW}[7/7] 验证服务...${NC}"

echo "  等待服务启动..."
MAX_WAIT=90
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    # 检查前端
    if curl -fsS http://localhost/ > /dev/null 2>&1; then
        # 检查后端
        if curl -fsS http://localhost/api/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 前端正常${NC}"
            echo -e "${GREEN}✅ 后端 API 正常${NC}"
            break
        fi
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo "  等待中... ($WAITED/$MAX_WAIT 秒)"
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo -e "${RED}❌ 服务启动超时，可能需要检查日志${NC}"
    echo -e "${YELLOW}查看日志: docker compose -f docker-compose.prod.yml logs -f${NC}"
    echo ""
    echo -e "${YELLOW}如需回滚，执行：${NC}"
    echo "  sed -i.bak 's/^TAG=.*/TAG=$CURRENT_TAG/' .env.prod"
    echo "  docker compose -f docker-compose.prod.yml --env-file .env.prod up -d"
    exit 1
fi

# 显示容器状态
echo ""
docker compose -f docker-compose.prod.yml ps

echo ""
echo -e "${BLUE}============================================================${NC}"
echo -e "${GREEN}✅ 升级完成！${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""
echo -e "  旧版本: $CURRENT_TAG"
echo -e "  新版本: $NEW_TAG"
echo ""
echo -e "${YELLOW}如需回滚：${NC}"
echo "  ./scripts/rollback-prod.sh"
echo "  或手动: sed -i.bak 's/^TAG=.*/TAG=$CURRENT_TAG/' .env.prod && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d"
