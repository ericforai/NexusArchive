#!/bin/bash
# Input: Offline Bundle
# Output: Running System
# Pos: NexusArchive deploy/install.sh
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

echo "======================================"
echo "   NexusArchive Offline Installer     "
echo "======================================"

# 1. Load Images
if [ -f "load_images.sh" ]; then
    echo "[1/4] Loading Docker images..."
    ./load_images.sh
else
    echo "[WARN] load_images.sh not found, assuming images are pre-loaded."
fi

# 2. Init Directories
echo "[2/4] Initializing data directories..."
mkdir -p data logs sql
# Copy init.sql if exists in bundle root (it should be copied by build script)
if [ -f "init.sql" ]; then
    cp init.sql sql/
fi

# 3. Start Services
echo "[3/4] Starting services..."
docker-compose up -d

# 4. Health Check
echo "[4/4] Waiting for system to be ready..."
sleep 10
MAX_RETRIES=30
COUNT=0
URL="http://localhost:80"

while [ $COUNT -lt $MAX_RETRIES ]; do
    if curl -s $URL > /dev/null; then
        echo "[SUCCESS] NexusArchive is running at $URL"
        exit 0
    fi
    echo -n "."
    sleep 2
    COUNT=$((COUNT+1))
done

echo ""
echo "[ERROR] Service did not start in time. Check logs via 'docker-compose logs'."
exit 1
