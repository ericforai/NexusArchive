#!/bin/bash
# ============================================================
# 开发环境升级脚本 (Development Environment Upgrade Script)
# 用途：拉取最新代码并重启所有服务（自动修复常见问题）
# 使用：./scripts/upgrade-dev.sh
# 版本：v2.1 - 添加 Flyway 迁移状态检查
# ============================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}   NexusArchive 开发环境升级脚本 v2.1${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# ========== Step 0: 检查 Docker 是否运行 ==========
echo -e "${YELLOW}[0/7] 检查 Docker 状态...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行，请先启动 Docker Desktop${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 正在运行${NC}"
echo ""

# ========== Step 1: 拉取最新代码 ==========
echo -e "${YELLOW}[1/7] 拉取最新代码...${NC}"
git pull origin main
echo -e "${GREEN}✅ 代码已更新${NC}"
echo ""

# ========== Step 2: 更新前端依赖 ==========
echo -e "${YELLOW}[2/7] 检查并更新前端依赖...${NC}"
if git diff HEAD@{1} --name-only 2>/dev/null | grep -q "package.json"; then
    echo "package.json 有变化，执行 npm install..."
    npm install
else
    echo "package.json 无变化，跳过 npm install"
fi
echo -e "${GREEN}✅ 前端依赖检查完成${NC}"
echo ""

# ========== Step 3: 确保 Docker 依赖运行 ==========
echo -e "${YELLOW}[3/7] 确保 Docker 依赖服务运行...${NC}"
docker compose -f docker-compose.deps.yml up -d
sleep 3  # 等待服务就绪

# 验证数据库连接
echo "验证数据库连接..."
MAX_RETRIES=10
RETRY_COUNT=0
while ! docker compose -f docker-compose.deps.yml exec -T nexus-db psql -U postgres -d nexusarchive -c "SELECT 1;" > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo -e "${RED}❌ 数据库连接失败，已重试 $MAX_RETRIES 次${NC}"
        exit 1
    fi
    echo "等待数据库就绪... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done
echo -e "${GREEN}✅ 数据库连接正常${NC}"
echo ""

# ========== Step 4: 检查 Flyway 迁移状态 ==========
echo -e "${YELLOW}[4/7] 检查数据库迁移状态...${NC}"

# 统计本地迁移文件数量
LOCAL_MIGRATIONS=$(find "$PROJECT_ROOT/nexusarchive-java/src/main/resources/db/migration" -name "V*.sql" 2>/dev/null | wc -l | tr -d ' ')

# 统计数据库中已执行的迁移数量
DB_MIGRATIONS=$(docker compose -f docker-compose.deps.yml exec -T nexus-db \
    psql -U postgres -d nexusarchive -t -c \
    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null | tr -d ' \n' || echo "0")

# 如果 flyway_schema_history 表不存在，说明是全新数据库
if [ "$DB_MIGRATIONS" = "0" ] || [ -z "$DB_MIGRATIONS" ]; then
    # 检查表是否存在
    TABLE_EXISTS=$(docker compose -f docker-compose.deps.yml exec -T nexus-db \
        psql -U postgres -d nexusarchive -t -c \
        "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'flyway_schema_history');" 2>/dev/null | tr -d ' \n' || echo "f")
    
    if [ "$TABLE_EXISTS" = "f" ]; then
        echo -e "${YELLOW}⚠️ 全新数据库，首次启动后端时将执行所有 $LOCAL_MIGRATIONS 个迁移${NC}"
    else
        DB_MIGRATIONS=$(docker compose -f docker-compose.deps.yml exec -T nexus-db \
            psql -U postgres -d nexusarchive -t -c \
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null | tr -d ' \n' || echo "0")
    fi
fi

PENDING_MIGRATIONS=$((LOCAL_MIGRATIONS - DB_MIGRATIONS))

echo "  本地迁移文件: $LOCAL_MIGRATIONS 个"
echo "  已执行迁移:   $DB_MIGRATIONS 个"

if [ $PENDING_MIGRATIONS -gt 0 ]; then
    echo -e "${YELLOW}  待执行迁移:   $PENDING_MIGRATIONS 个${NC}"
    echo ""
    echo -e "${YELLOW}⚠️ 有 $PENDING_MIGRATIONS 个新的数据库迁移将在后端启动时执行${NC}"
    
    # 显示待执行的迁移文件
    echo -e "${BLUE}待执行的迁移文件：${NC}"
    find "$PROJECT_ROOT/nexusarchive-java/src/main/resources/db/migration" -name "V*.sql" -print0 2>/dev/null | \
        xargs -0 -n1 basename | sort -V | tail -n $PENDING_MIGRATIONS | while read f; do
        echo "  - $f"
    done
else
    echo -e "${GREEN}  待执行迁移:   0 个${NC}"
fi
echo -e "${GREEN}✅ 迁移状态检查完成${NC}"
echo ""

# ========== Step 5: 检查并生成 JWT 密钥 ==========
echo -e "${YELLOW}[5/7] 检查 JWT 密钥...${NC}"
JWT_PRIVATE_KEY="$PROJECT_ROOT/nexusarchive-java/keystore/jwt-private.pem"
JWT_PUBLIC_KEY="$PROJECT_ROOT/nexusarchive-java/keystore/jwt-public.pem"

if [ ! -f "$JWT_PRIVATE_KEY" ] || [ ! -f "$JWT_PUBLIC_KEY" ]; then
    echo -e "${YELLOW}JWT 密钥不存在，正在生成...${NC}"
    
    # 创建 keystore 目录
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/keystore"
    
    # 生成密钥对
    if [ -f "$PROJECT_ROOT/nexusarchive-java/scripts/generate_jwt_keys.sh" ]; then
        bash "$PROJECT_ROOT/nexusarchive-java/scripts/generate_jwt_keys.sh"
    else
        # 如果脚本不存在，手动生成
        openssl genrsa -out "$JWT_PRIVATE_KEY" 2048
        openssl rsa -in "$JWT_PRIVATE_KEY" -pubout -out "$JWT_PUBLIC_KEY"
    fi
    
    if [ -f "$JWT_PRIVATE_KEY" ] && [ -f "$JWT_PUBLIC_KEY" ]; then
        echo -e "${GREEN}✅ JWT 密钥已生成${NC}"
    else
        echo -e "${RED}❌ JWT 密钥生成失败${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ JWT 密钥已存在${NC}"
fi
echo ""

# ========== Step 6: 检查 Maven 依赖 ==========
echo -e "${YELLOW}[6/7] 检查后端依赖...${NC}"
cd "$PROJECT_ROOT/nexusarchive-java"
if [ ! -d "target" ] || [ -z "$(ls -A target/*.jar 2>/dev/null)" ]; then
    echo "首次运行或 JAR 不存在，预编译依赖..."
    mvn dependency:resolve -q
fi
cd "$PROJECT_ROOT"
echo -e "${GREEN}✅ 后端依赖检查完成${NC}"
echo ""

# ========== Step 7: 提示启动后端和前端 ==========
echo -e "${YELLOW}[7/7] 准备启动服务...${NC}"
echo ""
echo -e "${BLUE}============================================================${NC}"
echo -e "${GREEN}✅ 升级准备完成！所有检查已通过。${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

if [ $PENDING_MIGRATIONS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 注意：后端启动时将自动执行 $PENDING_MIGRATIONS 个数据库迁移${NC}"
    echo ""
fi

echo -e "请在 ${YELLOW}两个终端窗口${NC} 中分别执行以下命令："
echo ""
echo -e "${BLUE}终端 1 (后端):${NC}"
echo -e "  cd ${PROJECT_ROOT}/nexusarchive-java && mvn spring-boot:run"
echo ""
echo -e "${BLUE}终端 2 (前端):${NC}"
echo -e "  cd ${PROJECT_ROOT} && npm run dev"
echo ""

# 询问是否自动启动
read -p "是否自动启动后端和前端？(y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}启动后端（后台运行）...${NC}"
    cd "$PROJECT_ROOT/nexusarchive-java"
    nohup mvn spring-boot:run > /tmp/nexus-backend.log 2>&1 &
    BACKEND_PID=$!
    echo "后端 PID: $BACKEND_PID (日志: /tmp/nexus-backend.log)"
    
    echo -e "${YELLOW}等待后端启动 (最多90秒，包含数据库迁移时间)...${NC}"
    MAX_WAIT=90
    WAITED=0
    while [ $WAITED -lt $MAX_WAIT ]; do
        if curl -fsS http://localhost:19090/api/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 后端已启动${NC}"
            
            # 检查迁移是否成功
            NEW_DB_MIGRATIONS=$(docker compose -f "$PROJECT_ROOT/docker-compose.deps.yml" exec -T nexus-db \
                psql -U postgres -d nexusarchive -t -c \
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null | tr -d ' \n' || echo "0")
            
            if [ "$NEW_DB_MIGRATIONS" = "$LOCAL_MIGRATIONS" ]; then
                echo -e "${GREEN}✅ 所有 $LOCAL_MIGRATIONS 个迁移已成功执行${NC}"
            else
                echo -e "${YELLOW}⚠️ 迁移状态: $NEW_DB_MIGRATIONS/$LOCAL_MIGRATIONS${NC}"
            fi
            break
        fi
        
        # 检查后端日志中是否有迁移进度
        if grep -q "Migrating schema" /tmp/nexus-backend.log 2>/dev/null; then
            CURRENT_MIGRATION=$(grep "Migrating schema" /tmp/nexus-backend.log | tail -1 | grep -oE "V[0-9]+" || echo "")
            if [ -n "$CURRENT_MIGRATION" ]; then
                echo "执行迁移中... ($CURRENT_MIGRATION)"
            fi
        fi
        
        sleep 5
        WAITED=$((WAITED + 5))
        echo "等待中... ($WAITED/$MAX_WAIT 秒)"
    done
    
    if [ $WAITED -ge $MAX_WAIT ]; then
        echo -e "${YELLOW}⚠️ 后端启动超时，请检查日志: tail -f /tmp/nexus-backend.log${NC}"
        echo -e "${YELLOW}   常见问题: 数据库迁移失败或字段冲突${NC}"
    fi
    
    echo -e "${YELLOW}启动前端（前台运行）...${NC}"
    cd "$PROJECT_ROOT"
    npm run dev
fi
