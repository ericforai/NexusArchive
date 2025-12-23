#!/bin/bash
# NexusArchive 一键回滚脚本
set -e

INSTALL_DIR="/opt/nexusarchive"
AUDIT_LOG="/var/log/nexusarchive/deploy_audit.log"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# --- 审计日志函数 ---
log_audit() {
    local action="$1"
    local detail="${2:-}"
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local operator="${SUDO_USER:-$(whoami)}"
    mkdir -p "$(dirname "$AUDIT_LOG")" 2>/dev/null || true
    echo "[${timestamp}] [${operator}@$(hostname)] [ROLLBACK:${action}] ${detail}" >> "$AUDIT_LOG" 2>/dev/null || true
}

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 回滚程序                                 ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 检查 root 权限
if [[ $EUID -ne 0 ]]; then
    echo -e "${RED}此脚本必须以 root 用户运行${NC}"
    exit 1
fi

# 检测最新备份目录
if [[ -f "$INSTALL_DIR/.latest_backup" ]]; then
    BACKUP_DIR=$(cat "$INSTALL_DIR/.latest_backup")
    echo -e "检测到最新备份: ${GREEN}$BACKUP_DIR${NC}"
else
    # 自动查找最新备份
    BACKUP_DIR=$(ls -dt /opt/nexusarchive_backup_* 2>/dev/null | head -1)
    if [[ -z "$BACKUP_DIR" ]]; then
        echo -e "${RED}未找到任何备份目录${NC}"
        exit 1
    fi
    echo -e "自动检测到备份: ${GREEN}$BACKUP_DIR${NC}"
fi

# 验证备份完整性
if [[ ! -f "$BACKUP_DIR/app.jar" ]]; then
    echo -e "${RED}备份不完整：缺少 app.jar${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}⚠️  警告: 回滚将恢复到以下备份版本${NC}"
echo "  备份时间: $(basename "$BACKUP_DIR" | sed 's/nexusarchive_backup_//')"
echo "  备份路径: $BACKUP_DIR"
echo ""
read -rp "确认执行回滚? [y/N]: " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "回滚已取消"
    exit 0
fi

log_audit "START" "Rollback initiated from $BACKUP_DIR"

echo -e "${GREEN}[1/4]${NC} 停止服务..."
systemctl stop nexusarchive
log_audit "SERVICE_STOPPED" "nexusarchive service stopped"

echo -e "${GREEN}[2/4]${NC} 恢复后端应用..."
cp "$BACKUP_DIR/app.jar" "$INSTALL_DIR/"
log_audit "JAR_RESTORED" "app.jar restored from backup"

echo -e "${GREEN}[3/4]${NC} 恢复前端资源..."
if [[ -d "$BACKUP_DIR/frontend" ]]; then
    rm -rf "$INSTALL_DIR/frontend"
    cp -r "$BACKUP_DIR/frontend" "$INSTALL_DIR/"
    log_audit "FRONTEND_RESTORED" "Frontend restored from backup"
fi

# 恢复配置文件（如有）
if [[ -f "$BACKUP_DIR/.env" ]]; then
    cp "$BACKUP_DIR/.env" "$INSTALL_DIR/"
    chmod 600 "$INSTALL_DIR/.env"
    log_audit "ENV_RESTORED" ".env restored from backup"
fi

echo -e "${GREEN}[4/4]${NC} 启动服务..."
systemctl start nexusarchive

# 健康检查
echo -n "等待服务启动"
for i in {1..30}; do
    if curl -s --max-time 2 http://localhost:8080/api/actuator/health &>/dev/null; then
        echo ""
        echo -e "${GREEN}服务健康检查通过 ✓${NC}"
        log_audit "HEALTH_CHECK_PASSED" "Service is healthy after rollback"
        break
    fi
    echo -n "."
    sleep 1
done

log_audit "ROLLBACK_COMPLETE" "Successfully rolled back from $BACKUP_DIR"

echo ""
echo -e "${GREEN}回滚完成!${NC}"
echo ""
echo -e "${YELLOW}重要提醒:${NC}"
echo "  - 如果涉及数据库变更，请联系 DBA 进行数据库回滚"
echo "  - 参照 SOP 文档中的 \"5.4 迁移回滚\" 章节"
