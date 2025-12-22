#!/bin/bash
# Input: Shell、systemctl、rm
# Output: 安装流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}警告: 此操作将完全卸载 NexusArchive${NC}"
echo "包括: 应用程序、配置文件"
echo "不包括: 数据库数据、归档文件 (需手动删除)"
echo ""
read -rp "确定要继续吗? [y/N]: " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

echo -e "${GREEN}[1/4]${NC} 停止服务..."
systemctl stop nexusarchive 2>/dev/null || true
systemctl disable nexusarchive 2>/dev/null || true

echo -e "${GREEN}[2/4]${NC} 删除 Systemd 服务..."
rm -f /etc/systemd/system/nexusarchive.service
systemctl daemon-reload

echo -e "${GREEN}[3/4]${NC} 删除 Nginx 配置..."
rm -f /etc/nginx/sites-enabled/nexusarchive.conf 2>/dev/null || true
rm -f /etc/nginx/sites-available/nexusarchive.conf 2>/dev/null || true
rm -f /etc/nginx/conf.d/nexusarchive.conf 2>/dev/null || true
systemctl reload nginx 2>/dev/null || true

echo -e "${GREEN}[4/4]${NC} 删除应用目录..."
rm -rf /opt/nexusarchive

echo ""
echo -e "${GREEN}卸载完成${NC}"
echo ""
echo "以下内容需要手动清理:"
echo "  - 数据库: DROP DATABASE nexusarchive;"
echo "  - 归档文件: rm -rf /opt/nexusarchive/data"
echo "  - 日志: rm -rf /var/log/nexusarchive"
echo "  - 用户: userdel nexus"