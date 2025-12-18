#!/bin/bash
# ==============================================================================
# NexusArchive 私有化部署一键安装脚本 (Private Deployment Tool)
# 适用环境：内网/离线 Linux 服务器 (RHEL/CentOS/Ubuntu/Debian)
# ==============================================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}====================================================${NC}"
echo -e "${YELLOW}   NexusArchive 档案系统 - 私有化部署工具 (Phase 3)   ${NC}"
echo -e "${YELLOW}====================================================${NC}"

# 1. 权限检查
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}[Error] 请以 root 权限运行此脚本${NC}"
    exit 1
fi

# 2. 系统依赖检查与安装
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
    else
        OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    fi
    echo -e "${GREEN}[1/5] 检测到操作系统: $OS${NC}"
}

install_dependencies() {
    echo -e "${YELLOW}[2/5] 正在安装系统依赖 (Java 17, Nginx, PostgreSQL)...${NC}"
    case "$OS" in
        ubuntu|debian)
            apt-get update && apt-get install -y openjdk-17-jre nginx postgresql postgresql-contrib openssl
            ;;
        centos|rhel|almalinux|rocky)
            yum install -y epel-release
            yum install -y java-17-openjdk nginx postgresql-server postgresql-contrib openssl
            if ! systemctl is-active --quiet postgresql; then
                postgresql-setup initdb || true
            fi
            ;;
        *)
            echo -e "${RED}[Error] 不支持的操作系统，请手动安装 Java 17, Nginx, PostgreSQL${NC}"
            exit 1
            ;;
    esac
    systemctl enable nginx
    systemctl enable postgresql
    systemctl start postgresql
}

# 3. 数据库初始化
setup_db() {
    echo -e "${YELLOW}[3/5] 初始化本地数据库...${NC}"
    sudo -u postgres psql -c "CREATE USER nexus WITH PASSWORD 'Nexus@2025';" || true
    sudo -u postgres psql -c "CREATE DATABASE nexusarchive OWNER nexus;" || true
    echo -e "${GREEN}数据库 nexusarchive 已创建${NC}"
}

# 4. 环境变量配置
setup_env() {
    echo -e "${YELLOW}[4/5] 配置环境变量与安全密钥...${NC}"
    INSTALL_DIR="/opt/nexusarchive"
    mkdir -p $INSTALL_DIR
    
    # 生成随机 SM4 密钥 (32位十六进制)
    SM4_KEY=$(openssl rand -hex 16)
    JWT_SECRET=$(openssl rand -hex 32)

    cat > $INSTALL_DIR/.env << EOL
# NexusArchive 生产环境配置
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# 数据库配置
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=nexus
DB_PASSWORD=Nexus@2025

# 安全加固 (Phase 3)
SM4_KEY=$SM4_KEY
JWT_SECRET=$JWT_SECRET

# 存储路径
STORAGE_PATH=$INSTALL_DIR/storage
LOG_PATH=$INSTALL_DIR/logs
EOL

    mkdir -p $INSTALL_DIR/storage $INSTALL_DIR/logs
    chmod 600 $INSTALL_DIR/.env
    echo -e "${GREEN}环境配置文件已生成: $INSTALL_DIR/.env${NC}"
}

# 5. 服务部署准备
finalize() {
    echo -e "${YELLOW}[5/5] 完成部署准备...${NC}"
    # 模拟移动构建产物 (在实际发布包中这些文件应已存在)
    # cp -r ./dist/* /var/www/html/
    # cp nexusarchive-backend.jar /opt/nexusarchive/app.jar
    
    echo -e "${GREEN}====================================================${NC}"
    echo -e "${GREEN}   NexusArchive 部署准备完成！                       ${NC}"
    echo -e "${GREEN}   1. 请将后端 JAR 包放置于 /opt/nexusarchive/app.jar ${NC}"
    echo -e "${GREEN}   2. 请将前端产物放置于 /var/www/html/               ${NC}"
    echo -e "${GREEN}   3. 运行 'systemctl start nexusarchive' 启动服务    ${NC}"
    echo -e "${GREEN}====================================================${NC}"
}

# 执行流程
detect_os
install_dependencies
setup_db
setup_env
finalize
