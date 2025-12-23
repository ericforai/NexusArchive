#!/bin/bash
# NexusArchive 升级脚本（安全加固版）
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="2.0.0"
INSTALL_DIR="/opt/nexusarchive"
BACKUP_DIR="/opt/nexusarchive_backup_$(date +%Y%m%d_%H%M%S)"
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
    echo "[${timestamp}] [${operator}@$(hostname)] [UPGRADE:${action}] ${detail}" >> "$AUDIT_LOG" 2>/dev/null || true
}

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 升级程序 v${VERSION} (安全加固版)         ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 检查 root 权限
if [[ $EUID -ne 0 ]]; then
    echo -e "${RED}此脚本必须以 root 用户运行${NC}"
    exit 1
fi

# 检查现有安装
if [[ ! -d "$INSTALL_DIR" ]]; then
    echo -e "${YELLOW}未检测到现有安装，请使用 install.sh 进行全新安装${NC}"
    exit 1
fi

log_audit "START" "Upgrade initiated, target version: ${VERSION}"

# 强制数据库备份确认
echo -e "${YELLOW}⚠️  警告: 升级前请确保已完成数据库备份！${NC}"
read -rp "您已完成数据库完整备份了吗? [y/N]: " backup_confirm
if [[ ! "$backup_confirm" =~ ^[Yy]$ ]]; then
    echo "请先备份数据库，脚本已中止。"
    log_audit "ABORTED" "User did not confirm database backup"
    exit 1
fi
log_audit "DB_BACKUP_CONFIRMED" "User confirmed database backup"

echo -e "${GREEN}[1/6]${NC} 备份当前版本..."
mkdir -p "$BACKUP_DIR"
cp "$INSTALL_DIR/app.jar" "$BACKUP_DIR/" 2>/dev/null || true
cp -r "$INSTALL_DIR/frontend" "$BACKUP_DIR/" 2>/dev/null || true
cp "$INSTALL_DIR/.env" "$BACKUP_DIR/" 2>/dev/null || true

# 额外备份 Nginx 和 systemd 配置
cp /etc/nginx/sites-available/nexusarchive.conf "$BACKUP_DIR/" 2>/dev/null || \
cp /etc/nginx/conf.d/nexusarchive.conf "$BACKUP_DIR/" 2>/dev/null || true
cp /etc/systemd/system/nexusarchive.service "$BACKUP_DIR/" 2>/dev/null || true

echo "备份位置: $BACKUP_DIR"
log_audit "BACKUP_CREATED" "Backup saved to $BACKUP_DIR"

# 记录最新备份路径（供回滚脚本使用）
echo "$BACKUP_DIR" > /opt/nexusarchive/.latest_backup

echo -e "${GREEN}[2/6]${NC} 校验新版本制品..."
JAR_FILE="${SCRIPT_DIR}/bin/nexusarchive-backend-${VERSION}.jar"
CHECKSUM_FILE="${JAR_FILE}.sha256"

if [[ -f "$CHECKSUM_FILE" ]]; then
    if sha256sum -c "$CHECKSUM_FILE" &>/dev/null; then
        echo -e "${GREEN}制品完整性校验通过 ✓${NC}"
        log_audit "ARTIFACT_VERIFIED" "JAR checksum OK"
    else
        echo -e "${RED}制品完整性校验失败！${NC}"
        log_audit "ARTIFACT_VERIFY_FAILED" "JAR checksum mismatch"
        exit 1
    fi
else
    echo -e "${YELLOW}未找到校验文件，跳过完整性校验${NC}"
fi

echo -e "${GREEN}[3/6]${NC} 停止服务..."
systemctl stop nexusarchive
log_audit "SERVICE_STOPPED" "nexusarchive service stopped"

echo -e "${GREEN}[4/6]${NC} 更新后端..."
cp "$JAR_FILE" "$INSTALL_DIR/app.jar"
log_audit "JAR_DEPLOYED" "New JAR deployed"

echo -e "${GREEN}[5/6]${NC} 更新前端..."
rm -rf "$INSTALL_DIR/frontend"
cp -r "${SCRIPT_DIR}/frontend" "$INSTALL_DIR/"
log_audit "FRONTEND_DEPLOYED" "Frontend updated"

echo -e "${GREEN}[6/6]${NC} 启动服务..."
systemctl start nexusarchive

# 健康检查（带超时）
echo -n "等待服务启动"
for i in {1..30}; do
    if curl -s --max-time 2 http://localhost:8080/api/actuator/health &>/dev/null; then
        echo ""
        echo -e "${GREEN}服务健康检查通过 ✓${NC}"
        log_audit "HEALTH_CHECK_PASSED" "Service is healthy"
        break
    fi
    echo -n "."
    sleep 1
done

log_audit "UPGRADE_COMPLETE" "Successfully upgraded to ${VERSION}"

echo ""
echo -e "${GREEN}升级完成!${NC}"
echo ""
echo "如需回滚，请执行:"
echo "  ./rollback.sh"
echo ""
echo "或手动回滚:"
echo "  systemctl stop nexusarchive"
echo "  cp $BACKUP_DIR/app.jar $INSTALL_DIR/"
echo "  cp -r $BACKUP_DIR/frontend $INSTALL_DIR/"
echo "  systemctl start nexusarchive"
