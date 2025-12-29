#!/bin/bash
# ============================================================
# 生产服务器升级脚本 (Production Server Upgrade Script)
# 用途：拉取最新代码、重新构建镜像、重启服务（保留现有配置）
# 使用：./scripts/upgrade-prod.sh
# 版本：v2.0 - 修复环境变量和镜像构建问题
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
echo -e "${BLUE}   NexusArchive 生产服务器升级脚本 v2.0${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# ========== Step 0: 检查前置条件 ==========
echo -e "${YELLOW}[0/7] 检查前置条件...${NC}"

# 检查 Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行${NC}"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven 未安装${NC}"
    echo "请安装 Maven: yum install maven 或 apt install maven"
    exit 1
fi

# 检查 .env.prod 是否存在
if [ ! -f "$PROJECT_ROOT/.env.prod" ]; then
    echo -e "${RED}❌ .env.prod 文件不存在${NC}"
    echo -e "${YELLOW}正在从模板创建...${NC}"
    if [ -f "$PROJECT_ROOT/.env.prod.template" ]; then
        cp "$PROJECT_ROOT/.env.prod.template" "$PROJECT_ROOT/.env.prod"
        echo -e "${YELLOW}⚠️ 已创建 .env.prod，请修改其中的密码和密钥！${NC}"
        echo "  nano .env.prod"
        exit 1
    else
        echo "请手动创建 .env.prod 文件"
        exit 1
    fi
fi

# 验证关键环境变量
echo "验证环境变量..."
source "$PROJECT_ROOT/.env.prod"
VALIDATION_FAILED=false

if [[ "$DB_PASSWORD" == *"改成"* ]] || [[ "$DB_PASSWORD" == *"your"* ]] || [ -z "$DB_PASSWORD" ]; then
    echo -e "${RED}❌ DB_PASSWORD 未配置或仍是占位符${NC}"
    VALIDATION_FAILED=true
fi

if [[ "$AUDIT_LOG_HMAC_KEY" == *"改成"* ]] || [ -z "$AUDIT_LOG_HMAC_KEY" ]; then
    echo -e "${RED}❌ AUDIT_LOG_HMAC_KEY 未配置或仍是占位符${NC}"
    VALIDATION_FAILED=true
fi

if [ -z "$VIRUS_SCAN_TYPE" ]; then
    echo -e "${YELLOW}⚠️ VIRUS_SCAN_TYPE 未配置，将使用默认值 skip${NC}"
    echo "VIRUS_SCAN_TYPE=skip" >> .env.prod
fi

if [ "$VALIDATION_FAILED" = true ]; then
    echo -e "${YELLOW}请编辑 .env.prod 修改占位符:${NC}"
    echo "  nano .env.prod"
    exit 1
fi

# 检查 JWT 密钥
if [ ! -f "$PROJECT_ROOT/nexusarchive-java/keystore/jwt_private.pem" ]; then
    echo -e "${YELLOW}JWT 密钥不存在，正在生成...${NC}"
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/keystore"
    openssl genrsa -out "$PROJECT_ROOT/nexusarchive-java/keystore/jwt_private.pem" 2048
    openssl rsa -in "$PROJECT_ROOT/nexusarchive-java/keystore/jwt_private.pem" -pubout -out "$PROJECT_ROOT/nexusarchive-java/keystore/jwt_public.pem"
    echo -e "${GREEN}✅ JWT 密钥已生成${NC}"
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

# 构建后端镜像（不使用缓存确保最新代码）
echo "  构建后端镜像..."
docker build --no-cache -t nexusarchive-backend:$NEW_TAG -f nexusarchive-java/Dockerfile nexusarchive-java

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

# 停止旧容器并清理冲突
docker compose -f docker-compose.prod.yml down 2>/dev/null || true

# 重启服务
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

echo -e "${GREEN}✅ 服务已重启${NC}"
echo ""

# ========== Step 7: 验证服务 ==========
echo -e "${YELLOW}[7/7] 验证服务...${NC}"

echo "  等待服务启动..."
MAX_WAIT=120
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    # 检查后端健康状态
    if docker compose -f docker-compose.prod.yml ps | grep -q "nexus-backend.*healthy"; then
        echo -e "${GREEN}✅ 后端已启动${NC}"
        # 检查 API
        if curl -fsS http://localhost/api/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ API 响应正常${NC}"
            break
        fi
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo "  等待中... ($WAITED/$MAX_WAIT 秒)"
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo -e "${RED}❌ 服务启动超时${NC}"
    echo ""
    echo -e "${YELLOW}排查步骤：${NC}"
    echo "  1. 查看后端日志: docker logs nexus-backend --tail 50"
    echo "  2. 查看前端日志: docker logs nexus-frontend --tail 20"
    echo "  3. 检查服务状态: docker compose -f docker-compose.prod.yml ps"
    echo ""
    echo -e "${YELLOW}如需回滚：${NC}"
    echo "  ./scripts/rollback-prod.sh"
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
