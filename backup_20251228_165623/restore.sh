#!/bin/bash
# 恢复脚本 - 在新机器上执行
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "============================================"
echo "   NexusArchive 数据恢复"
echo "============================================"

# 检查是否在正确位置
if [ ! -f "$PROJECT_ROOT/docker-compose.deps.yml" ]; then
    echo "❌ 请将此备份目录放到项目根目录下再执行"
    echo "   例如: /path/to/NexusArchive/backup_xxx/"
    exit 1
fi

# 启动 Docker 依赖
echo "[1/4] 启动 Docker 依赖..."
cd "$PROJECT_ROOT"
docker compose -f docker-compose.deps.yml up -d
sleep 5

# 等待数据库就绪
echo "[2/4] 等待数据库就绪..."
MAX_RETRIES=10
RETRY=0
while ! docker compose -f docker-compose.deps.yml exec -T nexus-db psql -U postgres -c "SELECT 1;" > /dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge $MAX_RETRIES ]; then
        echo "❌ 数据库未就绪"
        exit 1
    fi
    echo "等待中... ($RETRY/$MAX_RETRIES)"
    sleep 2
done
echo "✅ 数据库已就绪"

# 恢复数据库
echo "[3/4] 恢复数据库..."
docker compose -f docker-compose.deps.yml exec -T nexus-db \
    psql -U postgres -d nexusarchive < "$SCRIPT_DIR/database.sql"
echo "✅ 数据库已恢复"

# 恢复 JWT 密钥
echo "[4/4] 恢复配置文件..."
if [ -d "$SCRIPT_DIR/keystore" ]; then
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/keystore"
    cp -r "$SCRIPT_DIR/keystore/"* "$PROJECT_ROOT/nexusarchive-java/keystore/"
    echo "✅ JWT 密钥已恢复"
fi

# 恢复归档文件
if [ -d "$SCRIPT_DIR/data" ] && [ "$(ls -A $SCRIPT_DIR/data 2>/dev/null)" ]; then
    mkdir -p "$PROJECT_ROOT/nexusarchive-java/data"
    cp -r "$SCRIPT_DIR/data/"* "$PROJECT_ROOT/nexusarchive-java/data/"
    echo "✅ 归档文件已恢复"
fi

echo ""
echo "============================================"
echo "✅ 数据恢复完成！"
echo "============================================"
echo ""
echo "现在可以启动服务了："
echo "  终端1: cd $PROJECT_ROOT/nexusarchive-java && mvn spring-boot:run"
echo "  终端2: cd $PROJECT_ROOT && npm run dev"
