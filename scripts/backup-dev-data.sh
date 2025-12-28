#!/bin/bash
# ============================================================
# 开发环境数据打包脚本 (Development Environment Backup Script)
# 用途：打包数据库 + 归档文件 + JWT密钥，用于迁移到新机器
# 使用：./scripts/backup-dev-data.sh
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

# 备份目录
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$PROJECT_ROOT/backup_$BACKUP_DATE"
BACKUP_ARCHIVE="nexusarchive_backup_$BACKUP_DATE.tar.gz"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}   NexusArchive 开发环境数据打包${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# 检查 Docker
echo -e "${YELLOW}[1/5] 检查 Docker 状态...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行${NC}"
    exit 1
fi

if ! docker compose -f docker-compose.deps.yml ps | grep -q "nexus-db.*running"; then
    echo -e "${RED}❌ 数据库容器未运行，请先启动: docker compose -f docker-compose.deps.yml up -d${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 和数据库正常${NC}"
echo ""

# 创建备份目录
echo -e "${YELLOW}[2/5] 创建备份目录...${NC}"
mkdir -p "$BACKUP_DIR"
echo -e "${GREEN}✅ 备份目录: $BACKUP_DIR${NC}"
echo ""

# 备份数据库
echo -e "${YELLOW}[3/5] 备份数据库...${NC}"
docker compose -f docker-compose.deps.yml exec -T nexus-db \
    pg_dump -U postgres -d nexusarchive > "$BACKUP_DIR/database.sql"

DB_SIZE=$(du -h "$BACKUP_DIR/database.sql" | cut -f1)
echo -e "${GREEN}✅ 数据库备份完成 ($DB_SIZE)${NC}"
echo ""

# 备份 JWT 密钥
echo -e "${YELLOW}[4/5] 备份 JWT 密钥...${NC}"
if [ -d "$PROJECT_ROOT/nexusarchive-java/keystore" ]; then
    cp -r "$PROJECT_ROOT/nexusarchive-java/keystore" "$BACKUP_DIR/keystore"
    echo -e "${GREEN}✅ JWT 密钥已备份${NC}"
else
    echo -e "${YELLOW}⚠️ keystore 目录不存在，跳过${NC}"
fi
echo ""

# 备份归档文件
echo -e "${YELLOW}[5/5] 备份归档文件...${NC}"
if [ -d "$PROJECT_ROOT/nexusarchive-java/data" ] && [ "$(ls -A $PROJECT_ROOT/nexusarchive-java/data 2>/dev/null)" ]; then
    cp -r "$PROJECT_ROOT/nexusarchive-java/data" "$BACKUP_DIR/data"
    DATA_SIZE=$(du -sh "$BACKUP_DIR/data" | cut -f1)
    echo -e "${GREEN}✅ 归档文件已备份 ($DATA_SIZE)${NC}"
else
    mkdir -p "$BACKUP_DIR/data"
    echo -e "${YELLOW}⚠️ data 目录为空或不存在，创建空目录${NC}"
fi
echo ""

# 创建恢复脚本
echo -e "${YELLOW}创建恢复脚本...${NC}"
cat > "$BACKUP_DIR/restore.sh" << 'RESTORE_SCRIPT'
#!/bin/bash
# 恢复脚本 - 在新机器上执行
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "============================================"
echo "   NexusArchive 数据恢复"
echo "============================================"

# 检查是否在正确位置
if [ ! -f "$PROJECT_ROOT/docker-compose.deps.yml" ]; then
    echo "❌ 请将此备份目录放到项目根目录下再执行"
    echo "   例如: /path/to/NexusArchive/backup_xxx/"
    exit 1
fi

# 启动 Docker 依赖
echo "[1/4] 启动 Docker 依赖..."
cd "$PROJECT_ROOT"
docker compose -f docker-compose.deps.yml up -d
sleep 5

# 等待数据库就绪
echo "[2/4] 等待数据库就绪..."
MAX_RETRIES=10
RETRY=0
while ! docker compose -f docker-compose.deps.yml exec -T nexus-db psql -U postgres -c "SELECT 1;" > /dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge $MAX_RETRIES ]; then
        echo "❌ 数据库未就绪"
        exit 1
    fi
    echo "等待中... ($RETRY/$MAX_RETRIES)"
    sleep 2
done
echo "✅ 数据库已就绪"

# 恢复数据库
echo "[3/4] 恢复数据库..."
docker compose -f docker-compose.deps.yml exec -T nexus-db \
    psql -U postgres -d nexusarchive < "$SCRIPT_DIR/database.sql"
echo "✅ 数据库已恢复"

# 恢复 JWT 密钥
echo "[4/4] 恢复配置文件..."
if [ -d "$SCRIPT_DIR/keystore" ]; then
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/keystore"
    cp -r "$SCRIPT_DIR/keystore/"* "$PROJECT_ROOT/nexusarchive-java/keystore/"
    echo "✅ JWT 密钥已恢复"
fi

# 恢复归档文件
if [ -d "$SCRIPT_DIR/data" ] && [ "$(ls -A $SCRIPT_DIR/data 2>/dev/null)" ]; then
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/data"
    cp -r "$SCRIPT_DIR/data/"* "$PROJECT_ROOT/nexusarchive-java/data/"
    echo "✅ 归档文件已恢复"
fi

echo ""
echo "============================================"
echo "✅ 数据恢复完成！"
echo "============================================"
echo ""
echo "现在可以启动服务了："
echo "  终端1: cd $PROJECT_ROOT/nexusarchive-java && mvn spring-boot:run"
echo "  终端2: cd $PROJECT_ROOT && npm run dev"
RESTORE_SCRIPT

chmod +x "$BACKUP_DIR/restore.sh"
echo -e "${GREEN}✅ 恢复脚本已创建${NC}"
echo ""

# 打包
echo -e "${YELLOW}打包备份文件...${NC}"
cd "$PROJECT_ROOT"
tar -czf "$BACKUP_ARCHIVE" -C "$PROJECT_ROOT" "backup_$BACKUP_DATE"

ARCHIVE_SIZE=$(du -h "$BACKUP_ARCHIVE" | cut -f1)
echo ""
echo -e "${BLUE}============================================================${NC}"
echo -e "${GREEN}✅ 备份完成！${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""
echo -e "备份文件: ${YELLOW}$PROJECT_ROOT/$BACKUP_ARCHIVE${NC} ($ARCHIVE_SIZE)"
echo ""
echo -e "${BLUE}在新机器上恢复：${NC}"
echo -e "  1. 拷贝 $BACKUP_ARCHIVE 到新机器的项目目录"
echo -e "  2. 解压: tar -xzf $BACKUP_ARCHIVE"
echo -e "  3. 执行: ./backup_$BACKUP_DATE/restore.sh"
echo ""
echo -e "${YELLOW}提示：恢复完成后可删除备份目录${NC}"
