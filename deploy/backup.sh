#!/bin/bash
# Input: NexusArchive 数据备份脚本
# Output: 数据库和归档文件备份
# Pos: 数据备份（支持分层保留策略，满足会计档案保管要求）
# 用法: ./deploy/backup.sh [daily|weekly|monthly|yearly]
# Cron:
#   0 2 * * * /opt/nexusarchive/scripts/backup.sh daily   # 每日备份
#   0 3 * * 0 /opt/nexusarchive/scripts/backup.sh weekly  # 每周日备份
#   0 4 1 * * /opt/nexusarchive/scripts/backup.sh monthly # 每月1号备份

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 配置
BACKUP_ROOT="/opt/nexusarchive/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_TYPE=${1:-daily}

# 分层保留策略（满足《会计档案管理办法》10-30年保管要求）
RETENTION_DAILY=30      # 日备份保留 30 天
RETENTION_WEEKLY=365    # 周备份保留 1 年
RETENTION_MONTHLY=3650  # 月备份保留 10 年
RETENTION_YEARLY=-1     # 年备份永久保留

COMPOSE_FILES="-f docker-compose.infra.yml -f docker-compose.app.yml -f docker-compose.prod.yml"
DB_CONTAINER="nexus-db"
ARCHIVE_DIR="/opt/nexusarchive/archives"

# 根据备份类型设置目录和保留期
case "$BACKUP_TYPE" in
    daily)
        BACKUP_DIR="$BACKUP_ROOT/daily/$TIMESTAMP"
        RETENTION_DAYS=$RETENTION_DAILY
        ;;
    weekly)
        BACKUP_DIR="$BACKUP_ROOT/weekly/$(date +%Y)_week$(date +%U)"
        RETENTION_DAYS=$RETENTION_WEEKLY
        ;;
    monthly)
        BACKUP_DIR="$BACKUP_ROOT/monthly/$(date +%Y%m)"
        RETENTION_DAYS=$RETENTION_MONTHLY
        ;;
    yearly)
        BACKUP_DIR="$BACKUP_ROOT/yearly/$(date +%Y)"
        RETENTION_DAYS=$RETENTION_YEARLY
        ;;
    *)
        echo "用法: $0 [daily|weekly|monthly|yearly]"
        exit 1
        ;;
esac

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "=========================================="
echo "   NexusArchive 数据备份 ($BACKUP_TYPE)"
echo "=========================================="
info "备份时间: $(date '+%Y-%m-%d %H:%M:%S')"
info "备份目录: $BACKUP_DIR"
info "保留策略: $RETENTION_DAYS 天"
echo ""

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 1. 备份数据库
info "步骤 1/4: 备份数据库..."

# 从环境变量读取数据库配置
if [ -f "/opt/nexusarchive/.env.prod" ]; then
    source /opt/nexusarchive/.env.prod
    DB_NAME=${DB_NAME:-nexusarchive}
    DB_USER=${DB_USER:-postgres}
else
    DB_NAME="nexusarchive"
    DB_USER="postgres"
fi

docker exec $DB_CONTAINER pg_dump -U $DB_USER $DB_NAME 2>/dev/null > "$BACKUP_DIR/database.sql"

if [ $? -eq 0 ] && [ -s "$BACKUP_DIR/database.sql" ]; then
    SIZE=$(du -h "$BACKUP_DIR/database.sql" | cut -f1)
    info "✓ 数据库备份完成 (大小: $SIZE)"
else
    error "数据库备份失败"
fi

# 2. 备份归档文件
info "步骤 2/4: 备份归档文件..."

if [ -d "$ARCHIVE_DIR" ] && [ "$(ls -A $ARCHIVE_DIR 2>/dev/null)" ]; then
    tar -czf "$BACKUP_DIR/archives.tar.gz" -C "$ARCHIVE_DIR" . 2>/dev/null
    if [ $? -eq 0 ]; then
        SIZE=$(du -h "$BACKUP_DIR/archives.tar.gz" | cut -f1)
        info "✓ 归档文件备份完成 (大小: $SIZE)"
    else
        warn "归档文件备份失败（可能目录为空）"
    fi
else
    warn "归档目录为空或不存在，跳过"
fi

# 3. 备份配置文件
info "步骤 3/4: 备份配置文件..."

if [ -f "/opt/nexusarchive/.env.prod" ]; then
    cp /opt/nexusarchive/.env.prod "$BACKUP_DIR/env.prod"
    info "✓ 环境变量备份完成（含 HMAC 密钥）"
fi

if [ -f "/opt/nexusarchive/nginx/nginx.prod.conf" ]; then
    cp /opt/nexusarchive/nginx/nginx.prod.conf "$BACKUP_DIR/nginx.conf"
    info "✓ Nginx 配置备份完成"
fi

# 4. 生成备份清单和校验和
info "步骤 4/4: 生成备份清单..."

cat > "$BACKUP_DIR/manifest.txt" << MANIFEST
========================================
   NexusArchive 备份清单
========================================
备份类型: $BACKUP_TYPE
备份时间: $(date '+%Y-%m-%d %H:%M:%S')
备份版本: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
服务器: $(hostname)
----------------------------------------
文件清单:
$(cd "$BACKUP_DIR" && ls -lh | grep -v "^total" | grep -v "^d" | awk '{print $9 ": " $5}')

----------------------------------------
安全提示:
1. env.prod 包含 HMAC 密钥，请妥善保管
2. 建议将年度备份复制到离线存储
3. 每季度验证一次备份可恢复性
========================================
MANIFEST

# 生成校验和
cd "$BACKUP_DIR"
sha256sum database.sql archives.tar.gz env.prod 2>/dev/null > checksums.txt || true
cd - > /dev/null

info "✓ 备份清单已生成"

# 清理旧备份
if [ $RETENTION_DAYS -gt 0 ]; then
    info "清理旧 $BACKUP_TYPE 备份（保留 $RETENTION_DAYS 天）..."
    find "$BACKUP_ROOT/$BACKUP_TYPE" -maxdepth 1 -type d -mtime +$RETENTION_DAYS -exec rm -rf {} + 2>/dev/null || true
else
    info "年度备份永久保留，不执行清理"
fi

# 压缩备份（可选，节省空间）
warn "正在压缩备份..."
tar -czf "$BACKUP_DIR.tar.gz" -C "$BACKUP_ROOT" "$BACKUP_TYPE/" 2>/dev/null
rm -rf "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR.tar.gz"

echo ""
echo "=========================================="
echo "   ✅ 备份完成!"
echo "=========================================="
info "备份文件: $BACKUP_FILE"
info "总大小: $(du -h $BACKUP_FILE | cut -f1)"

# 恢复说明
cat << EOF

📋 恢复方法:
   数据库: docker exec -i nexus-db psql -U postgres nexusarchive < database.sql
   归档: tar -xzf archives.tar.gz -C /opt/nexusarchive/archives
   配置: cp env.prod /opt/nexusarchive/.env.prod

📊 备份统计:
   日备份: $(find $BACKUP_ROOT/daily -type f 2>/dev/null | wc -l) 个
   周备份: $(find $BACKUP_ROOT/weekly -type f 2>/dev/null | wc -l) 个
   月备份: $(find $BACKUP_ROOT/monthly -type f 2>/dev/null | wc -l) 个
   年备份: $(find $BACKUP_ROOT/yearly -type f 2>/dev/null | wc -l) 个

==========================================
EOF
