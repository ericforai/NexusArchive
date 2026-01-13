#!/bin/bash
# Input: NexusArchive 版本回滚脚本
# Output: 回滚到上一版本
# Pos: 版本回滚
# 用法: ./deploy/rollback.sh [version]

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

COMPOSE_FILES="-f docker-compose.infra.yml -f docker-compose.app.yml -f docker-compose.prod.yml"
VERSION=${1:-}

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "=========================================="
echo "   NexusArchive 版本回滚"
echo "=========================================="
echo ""

# 检查是否指定了版本
if [ -z "$VERSION" ]; then
    # 列出可用的备份版本
    info "可用的备份版本:"
    ls -lt /opt/nexusarchive/backups/ 2>/dev/null | head -10 || error "没有找到备份"
    echo ""
    error "请指定要回滚的版本: ./rollback.sh <backup-directory>"
fi

BACKUP_PATH="/opt/nexusarchive/backups/$VERSION"

if [ ! -d "$BACKUP_PATH" ]; then
    error "备份目录不存在: $BACKUP_PATH"
fi

warn "⚠️  即将回滚到版本: $VERSION"
warn "⚠️  此操作将覆盖当前数据!"
read -p "确认继续? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    info "已取消"
    exit 0
fi

# 1. 停止服务
info "步骤 1/4: 停止服务..."
docker-compose $COMPOSE_FILES down
info "✓ 服务已停止"

# 2. 恢复数据库
info "步骤 2/4: 恢复数据库..."
if [ -f "$BACKUP_PATH/database.sql" ]; then
    docker-compose $COMPOSE_FILES up -d nexus-db
    sleep 10
    docker exec -i nexus-db psql -U postgres nexusarchive < "$BACKUP_PATH/database.sql"
    info "✓ 数据库已恢复"
else
    warn "数据库备份文件不存在，跳过"
fi

# 3. 恢复归档文件
info "步骤 3/4: 恢复归档文件..."
if [ -f "$BACKUP_PATH/archives.tar.gz" ]; then
    mkdir -p /opt/nexusarchive/archives
    tar -xzf "$BACKUP_PATH/archives.tar.gz" -C /opt/nexusarchive/archives
    info "✓ 归档文件已恢复"
else
    warn "归档备份文件不存在，跳过"
fi

# 4. 重启服务
info "步骤 4/4: 重启服务..."
docker-compose $COMPOSE_FILES --env-file .env.prod up -d
info "✓ 服务已启动"

echo ""
echo "=========================================="
echo "   ✅ 回滚完成!"
echo "=========================================="
info "已恢复到版本: $VERSION"

# 显示服务状态
sleep 5
docker-compose $COMPOSE_FILES ps
echo ""
