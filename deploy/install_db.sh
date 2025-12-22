#!/bin/bash
# Input: Shell、ssh、systemctl、sed
# Output: 安装流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

# Usage: ./deploy/install_db.sh <SERVER_IP>
if [ -z "$1" ]; then
    echo "Usage: ./deploy/install_db.sh <SERVER_IP>"
    echo "Example: ./deploy/install_db.sh 180.184.54.214"
    exit 1
fi

SERVER_HOST=$1
SERVER_USER="root"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[DB Init] Connecting to ${SERVER_HOST}...${NC}"

ssh ${SERVER_USER}@${SERVER_HOST} << 'EOF'
    set -e
    
    echo ">>> Checking for existing PostgreSQL..."
    if systemctl is-active --quiet postgresql; then
        echo "PostgreSQL is already running. Skipping installation."
        exit 0
    fi

    echo ">>> Installing PostgreSQL Server..."
    if command -v yum &> /dev/null; then
        yum install -y postgresql-server postgresql-contrib
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y postgresql postgresql-contrib
    else
        echo "Unsupported OS."
        exit 1
    fi

    echo ">>> Initializing Database..."
    if [ -f /usr/bin/postgresql-setup ]; then
        # CentOS/RHEL specific
        if [ ! -d /var/lib/pgsql/data/base ]; then
            /usr/bin/postgresql-setup --initdb
        fi
    fi

    echo ">>> Starting PostgreSQL..."
    systemctl enable --now postgresql

    echo ">>> Configuring Database User & Schema..."
    # Generate a random password
    DB_PASSWORD=$(openssl rand -base64 12)
    
    # Switch to postgres user to execute commands
    sudo -u postgres psql << SQL
    -- Create user if not exists
    DO
    \$do\$
    BEGIN
       IF NOT EXISTS (
          SELECT FROM pg_catalog.pg_roles
          WHERE  rolname = 'nexus') THEN
          CREATE ROLE nexus LOGIN PASSWORD '${DB_PASSWORD}';
       ELSE
          ALTER ROLE nexus WITH PASSWORD '${DB_PASSWORD}';
       END IF;
    END
    \$do\$;

    -- Create database if not exists
    SELECT 'CREATE DATABASE nexusarchive OWNER nexus'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'nexusarchive')\gexec
SQL

    echo ">>> Updating .env configuration..."
    ENV_FILE="/opt/nexusarchive/.env"
    if [ -f "$ENV_FILE" ]; then
        # Update DB_PASSWORD
        sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=${DB_PASSWORD}|" "$ENV_FILE"
        # Update DB_USER if needed (assuming template uses SYSDBA, change to nexus)
        sed -i "s|DB_USER=.*|DB_USER=nexus|" "$ENV_FILE"
        # Update DB_NAME if needed
        sed -i "s|DB_NAME=.*|DB_NAME=nexusarchive|" "$ENV_FILE"
        # Update DB_PORT to 5432 (Postgres default)
        sed -i "s|DB_PORT=.*|DB_PORT=5432|" "$ENV_FILE"
        # Update Active Profile to prod (remove dameng if present)
        sed -i "s|SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod|" "$ENV_FILE"
        
        echo "Updated .env with new credentials."
    else
        echo "Warning: .env file not found at $ENV_FILE"
    fi

    echo ">>> Configuring pg_hba.conf (Allow password auth)..."
    # Find pg_hba.conf
    PG_HBA=$(find /etc /var/lib/pgsql -name pg_hba.conf | head -n 1)
    if [ -n "$PG_HBA" ]; then
        # Backup
        cp "$PG_HBA" "${PG_HBA}.bak"
        # Change ident/peer to md5/scram-sha-256 for local connections
        sed -i 's/ident/md5/g' "$PG_HBA"
        sed -i 's/peer/md5/g' "$PG_HBA"
        
        echo "Reloading PostgreSQL..."
        systemctl reload postgresql
    fi

    echo ">>> Database Setup Complete!"
    echo "User: nexus"
    echo "Database: nexusarchive"
    echo "Password: ${DB_PASSWORD}"
EOF

echo -e "${GREEN}[DB Init] Success! Database is ready and .env has been updated.${NC}"