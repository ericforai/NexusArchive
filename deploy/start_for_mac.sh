#!/bin/bash
# NexusArchive Mac 简易启动脚本 (离线包专用)

# 1. 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== NexusArchive Mac 启动助手 ===${NC}"

# 2. 定位 jar 文件
JAR_FILE=$(find . -name "nexusarchive-backend-*.jar" | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
    # 尝试在 bin 目录下找
    JAR_FILE=$(find bin -name "nexusarchive-backend-*.jar" 2>/dev/null | head -n 1)
fi

if [[ -z "$JAR_FILE" ]]; then
    echo -e "${RED}❌ 未找到后端 JAR 文件!${NC}"
    echo "请确保您处于离线安装包解压后的根目录下。"
    exit 1
fi

echo -e "后端程序: $JAR_FILE"

# 3. 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 未检测到 Java 环境${NC}"
    echo "请先安装 Java 17+ (推荐 ARM64 版本)"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -n 1)
echo -e "Java 环境: $JAVA_VER"

# 4. 设置临时环境变量 (使用 h2 或 sqlite 内存模式? 不，还是用 PG 默认配置，或者让用户确认)
# 由于是 Mac 演示/试用，通常需要连接本地数据库。
# 如果没有数据库，很难启动。
# 为了"傻瓜式"，这里检查 .env 是否存在，如果不存在则创建默认配置

if [[ ! -f ".env" ]]; then
    echo -e "${RED}⚠️  未检测到配置文件 .env${NC}"
    echo "正在生成默认配置 (默认连接本地 PostgreSQL:5432)..."
    
    JWT_SECRET=$(openssl rand -hex 32)
    
    cat > .env << EOF
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=$JWT_SECRET
ARCHIVE_ROOT_PATH=./data/archives
ARCHIVE_TEMP_PATH=./data/temp
EOF
    echo -e "${GREEN}✓ 已生成默认 .env 文件，请根据实际情况修改数据库密码${NC}"
fi

# 5. 加载环境变量并启动
echo -e "${GREEN}🚀 正在启动服务... (按 Ctrl+C 停止)${NC}"

# export 所有变量
set -a
source .env
set +a

# 确保目录存在
mkdir -p ./data/archives
mkdir -p ./data/temp

# 启动
java -jar "$JAR_FILE"
