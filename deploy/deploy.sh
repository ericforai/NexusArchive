#!/bin/bash
# Input: Shell、mvn、npm、tar
# Output: 部署流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

# Configuration
SERVER_USER="root" # Change this to your server user
SERVER_HOST="115.190.237.184" # 火山云服务器 IP
REMOTE_DIR="/opt/nexusarchive"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Safety Check
if [[ "$SERVER_HOST" == "your-server-ip" ]]; then
    echo -e "${RED}[Error] Please configure SERVER_HOST in deploy.sh${NC}"
    exit 1
fi

echo -e "${YELLOW}[Deploy] Target: ${SERVER_USER}@${SERVER_HOST}:${REMOTE_DIR}${NC}"
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

# 1. Build locally
echo -e "${GREEN}[Deploy] Starting local build...${NC}"
# Ensure we are in the project root
cd "$(dirname "$0")/.."

# Backend Build
echo -e "${YELLOW}[Build] Building Backend (Maven)...${NC}"
cd nexusarchive-java
mvn clean package -DskipTests
cd ..

# Frontend Build
echo -e "${YELLOW}[Build] Building Frontend (Vite)...${NC}"
npm install
npm run build

# Package
echo -e "${YELLOW}[Build] Packaging artifacts...${NC}"
PROJECT_ROOT=$(pwd)
tar -czf nexusarchive-release.tar.gz \
    -C "$PROJECT_ROOT/nexusarchive-java/target" nexusarchive-backend-2.0.0.jar \
    -C "$PROJECT_ROOT/dist" . \
    -C "$PROJECT_ROOT" deploy/nexusarchive.service deploy/nginx.conf deploy/setup_ssl.sh deploy/setup_demo_aip.sh \
    -C "$PROJECT_ROOT/nexusarchive-java/src/main/resources/db/demo" demo_archive_features.sql \
    -C "$PROJECT_ROOT/nexusarchive-java/src/main/resources/db" demo_aip_data.sql \
    -C "$PROJECT_ROOT/nexusarchive-java/src/main/resources/db/migration" V3__smart_parser_tables.sql V4__fix_archive_and_audit_columns.sql V5__ingest_request_status.sql V6__add_business_modules.sql V7__add_archive_approval.sql V8__add_open_appraisal.sql V9__ensure_metadata_tables.sql V10__compliance_schema_update.sql V11__add_missing_archive_columns.sql

# 2. Upload artifacts
echo -e "${GREEN}[Deploy] Uploading artifacts to ${SERVER_HOST}...${NC}"
scp nexusarchive-release.tar.gz ${SERVER_USER}@${SERVER_HOST}:${REMOTE_DIR}/

# 3. Remote execution
echo -e "${GREEN}[Deploy] Executing remote update...${NC}"
ssh ${SERVER_USER}@${SERVER_HOST} << EOF
    set -e
    cd ${REMOTE_DIR}
    
    # Environment Check
    if ! command -v java &> /dev/null; then
        echo "Java not found! Please install JDK 17+."
        exit 1
    fi

    echo "[Remote] Backing up old version..."
    if [ -d "frontend" ]; then mv frontend frontend_backup_\$(date +%s); fi
    if [ -f "app.jar" ]; then mv app.jar app.jar_backup_\$(date +%s); fi
    
    echo "[Remote] Unpacking new version..."
    # Extract jar and config
    # Extract jar and config
    tar -xzf nexusarchive-release.tar.gz nexusarchive-backend-2.0.0.jar deploy/nexusarchive.service deploy/nginx.conf deploy/setup_ssl.sh deploy/setup_demo_aip.sh demo_archive_features.sql demo_aip_data.sql V3__smart_parser_tables.sql V4__fix_archive_and_audit_columns.sql V5__ingest_request_status.sql V6__add_business_modules.sql V7__add_archive_approval.sql V8__add_open_appraisal.sql V9__ensure_metadata_tables.sql V10__compliance_schema_update.sql V11__add_missing_archive_columns.sql
    mv nexusarchive-backend-2.0.0.jar app.jar
    
    # Extract frontend
    mkdir -p frontend
    tar -xzf nexusarchive-release.tar.gz -C frontend --exclude='nexusarchive-backend-2.0.0.jar' --exclude='deploy/*' --exclude='*.sql'
    
    echo "[Remote] Cleaning up..."
    rm nexusarchive-release.tar.gz
    # Optional: Keep only last 5 backups
    # find . -name "*_backup_*" -mtime +7 -exec rm -rf {} \;

    echo "[Remote] Setting up Demo AIPs..."
    chmod +x deploy/setup_demo_aip.sh
    ./deploy/setup_demo_aip.sh

    echo "[Remote] Restarting Backend Service..."
    # Check if service exists before restarting
    if systemctl list-unit-files | grep -q nexusarchive.service; then
        systemctl restart nexusarchive
    else
        echo "Service not found. Please install deploy/nexusarchive.service first."
    fi
    
    echo "[Remote] Reloading Nginx..."
    if systemctl is-active --quiet nginx; then
        systemctl reload nginx
    else
        echo "Nginx not running. Please start Nginx."
    fi
    
    echo "[Remote] Importing Demo Data..."
    # Debug: List files to confirm SQL file exists
    ls -l *.sql || echo "SQL files missing!"
    
    if [ -f "demo_archive_features.sql" ] && [ -f "/opt/nexusarchive/.env" ]; then
        echo "[Remote] Loading environment variables..."
        # Robust way to load .env
        set -a
        source /opt/nexusarchive/.env
        set +a
        
        echo "[Remote] DB_USER: \$DB_USER"
        echo "[Remote] DB_NAME: \$DB_NAME"
        # Do not print password
        
        if [ -z "\$DB_PASSWORD" ]; then
            echo "[Remote] Error: DB_PASSWORD is empty!"
        else
            # Execute SQL
            export PGPASSWORD=\$DB_PASSWORD
            
            echo "[Remote] Executing Schema Migrations..."
            
            # Helper function for migration
            run_migration() {
                local file=\$1
                local logfile="migration_\${file%.*}.log"
                echo "[Remote] Running \$file..."
                if psql -h localhost -U \$DB_USER -d \$DB_NAME -f \$file > \$logfile 2>&1; then
                     echo "[Remote] \$file executed successfully."
                else
                     echo "[Remote] Warning: \$file failed. Details:"
                     cat \$logfile
                fi
            }

            # Execute migrations in order
            run_migration "V3__smart_parser_tables.sql"
            run_migration "V4__fix_archive_and_audit_columns.sql"
            run_migration "V5__ingest_request_status.sql"
            run_migration "V6__add_business_modules.sql"
            run_migration "V7__add_archive_approval.sql"
            run_migration "V8__add_open_appraisal.sql"
            run_migration "V7__add_archive_approval.sql"
            run_migration "V8__add_open_appraisal.sql"
            run_migration "V9__ensure_metadata_tables.sql"
            run_migration "V10__compliance_schema_update.sql"
            run_migration "V11__add_missing_archive_columns.sql"

            echo "[Remote] Executing Demo Data Import..."
            if psql -h localhost -U \$DB_USER -d \$DB_NAME -f demo_archive_features.sql > import_demo.log 2>&1; then
                 echo "[Remote] Demo data imported successfully."
            else
                 echo "[Remote] Warning: Failed to import demo data. Error details:"
                 cat import_demo.log
            fi

            echo "[Remote] Importing Structured AIP Demo Data..."
            if psql -h localhost -U \$DB_USER -d \$DB_NAME -f demo_aip_data.sql > import_aip_demo.log 2>&1; then
                 echo "[Remote] Structured AIP Demo data imported successfully."
            else
                 echo "[Remote] Warning: Failed to import AIP demo data. Error details:"
                 cat import_aip_demo.log
            fi
            unset PGPASSWORD
        fi
    else
        echo "[Remote] Skipping demo data import (SQL file or .env not found)."
    fi

    echo "[Remote] Deployment Complete!"
EOF

echo -e "${GREEN}[Deploy] Success!${NC}"