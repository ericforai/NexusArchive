#!/bin/bash
# Input: Shell、scp、ssh、mkdir
# Output: 运维脚本逻辑
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

# Usage: ./deploy/init_server.sh <SERVER_IP>
if [ -z "$1" ]; then
    echo "Usage: ./deploy/init_server.sh <SERVER_IP>"
    echo "Example: ./deploy/init_server.sh 123.45.67.89"
    exit 1
fi

SERVER_HOST=$1
SERVER_USER="root" # Must be root for initial setup

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[Init] Connecting to ${SERVER_HOST} as ${SERVER_USER}...${NC}"

# 1. Upload Config Files
echo -e "${GREEN}[Init] Uploading configuration files...${NC}"
scp deploy/nexusarchive.service deploy/nginx.conf ${SERVER_USER}@${SERVER_HOST}:/tmp/

# 2. Remote Execution
ssh ${SERVER_USER}@${SERVER_HOST} << 'EOF'
    set -e
    
    echo ">>> Detecting OS..."
    if command -v apt-get &> /dev/null; then
        echo "Detected Debian/Ubuntu system."
        echo ">>> Updating package lists..."
        apt-get update
        
        echo ">>> Installing Dependencies..."
        apt-get install -y openjdk-17-jre nginx postgresql-client
    elif command -v yum &> /dev/null; then
        echo "Detected CentOS/RHEL system."
        echo ">>> Installing EPEL & Updating..."
        yum install -y epel-release
        yum update -y
        
        echo ">>> Installing Dependencies..."
        yum install -y java-17-openjdk nginx postgresql
    else
        echo "Error: Unsupported Package Manager. Please install Java 17 and Nginx manually."
        exit 1
    fi
    
    echo ">>> Creating 'nexus' user..."
    if ! id -u nexus > /dev/null 2>&1; then
        useradd -r -s /bin/false nexus
    fi
    
    echo ">>> Creating directories..."
    mkdir -p /opt/nexusarchive
    chown -R nexus:nexus /opt/nexusarchive
    
    echo ">>> Setting up .env template..."
    if [ ! -f /opt/nexusarchive/.env ]; then
        cat > /opt/nexusarchive/.env << EOL
SPRING_PROFILES_ACTIVE=prod-dameng
SERVER_PORT=8080
DB_HOST=127.0.0.1
DB_PORT=5236
DB_NAME=NEXUSARCHIVE
DB_USER=SYSDBA
DB_PASSWORD=ChangeMe
JWT_SECRET=$(openssl rand -hex 32)
EOL
        chmod 600 /opt/nexusarchive/.env
        chown nexus:nexus /opt/nexusarchive/.env
        echo "Created /opt/nexusarchive/.env"
    else
        echo ".env already exists, skipping."
    fi
    
    echo ">>> Configuring Systemd..."
    mv /tmp/nexusarchive.service /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable nexusarchive
    
    echo ">>> Configuring Nginx..."
    if [ -d "/etc/nginx/sites-available" ]; then
        # Debian/Ubuntu style
        mv /tmp/nginx.conf /etc/nginx/sites-available/nexusarchive
        ln -sf /etc/nginx/sites-available/nexusarchive /etc/nginx/sites-enabled/
        rm -f /etc/nginx/sites-enabled/default
    else
        # CentOS/RHEL style
        mv /tmp/nginx.conf /etc/nginx/conf.d/nexusarchive.conf
        # Ensure the file ends with .conf for CentOS
    fi
    
    # Test config
    nginx -t
    systemctl enable nginx
    systemctl restart nginx
    
    echo ">>> Initialization Complete!"
EOF

echo -e "${GREEN}[Init] Server setup finished successfully!${NC}"
echo -e "${YELLOW}Next Step: SSH into server and edit /opt/nexusarchive/.env with real database credentials.${NC}"