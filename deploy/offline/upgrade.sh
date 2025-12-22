#!/bin/bash
# Input: Shell、mkdir、cp、systemctl
# Output: 升级流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="2.0.0"
INSTALL_DIR="/opt/nexusarchive"
BACKUP_DIR="/opt/nexusarchive_backup_$(date +%Y%m%d_%H%M%S)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 升级程序 v${VERSION}                        ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 检查现有安装
if [[ ! -d "$INSTALL_DIR" ]]; then
    echo -e "${YELLOW}未检测到现有安装，请使用 install.sh 进行全新安装${NC}"
    exit 1
fi

echo -e "${GREEN}[1/5]${NC} 备份当前版本..."
mkdir -p "$BACKUP_DIR"
cp "$INSTALL_DIR/app.jar" "$BACKUP_DIR/" 2>/dev/null || true
cp -r "$INSTALL_DIR/frontend" "$BACKUP_DIR/" 2>/dev/null || true
cp "$INSTALL_DIR/.env" "$BACKUP_DIR/" 2>/dev/null || true
echo "备份位置: $BACKUP_DIR"

echo -e "${GREEN}[2/5]${NC} 停止服务..."
systemctl stop nexusarchive

echo -e "${GREEN}[3/5]${NC} 更新后端..."
cp "${SCRIPT_DIR}/bin/nexusarchive-backend-${VERSION}.jar" "$INSTALL_DIR/app.jar"

echo -e "${GREEN}[4/5]${NC} 更新前端..."
rm -rf "$INSTALL_DIR/frontend"
cp -r "${SCRIPT_DIR}/frontend" "$INSTALL_DIR/"

echo -e "${GREEN}[5/5]${NC} 启动服务..."
systemctl start nexusarchive

echo ""
echo -e "${GREEN}升级完成!${NC}"
echo "如需回滚，请执行:"
echo "  systemctl stop nexusarchive"
echo "  cp $BACKUP_DIR/app.jar $INSTALL_DIR/"
echo "  cp -r $BACKUP_DIR/frontend $INSTALL_DIR/"
echo "  systemctl start nexusarchive"