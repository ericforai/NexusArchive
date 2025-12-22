#!/bin/bash
# Input: Shell、mvn
# Output: 初始化流程
# Pos: 脚本工具
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# ==============================================================================
# NexusArchive Java 后端开发环境初始化脚本
# 适用环境：macOS / Linux 开发机
# 功能：检查依赖、初始化数据库、编译项目
# ==============================================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}  NexusArchive Java 后端开发环境初始化   ${NC}"
echo -e "${YELLOW}=========================================${NC}"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. 检查 Java 版本
echo -e "\n${YELLOW}[1/4] 检查 Java 版本...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    echo -e "${GREEN}✓ Java 版本: $JAVA_VERSION${NC}"
    
    MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d. -f1)
    if [ "$MAJOR_VERSION" -lt 17 ]; then
        echo -e "${RED}✗ 需要 Java 17+，当前版本过低${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ 未安装 Java，请先安装 Java 17+${NC}"
    exit 1
fi

# 2. 检查 Maven
echo -e "\n${YELLOW}[2/4] 检查 Maven...${NC}"
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ $MVN_VERSION${NC}"
else
    echo -e "${RED}✗ 未安装 Maven，请先安装 Maven 3.8+${NC}"
    exit 1
fi

# 3. 检查/初始化数据库
echo -e "\n${YELLOW}[3/4] 检查 PostgreSQL...${NC}"
if command -v psql &> /dev/null; then
    PSQL_VERSION=$(psql --version 2>&1)
    echo -e "${GREEN}✓ $PSQL_VERSION${NC}"
    
    # 尝试创建数据库 (忽略已存在错误)
    echo "尝试创建数据库 nexusarchive..."
    createdb nexusarchive 2>/dev/null || echo -e "${YELLOW}  数据库已存在，跳过创建${NC}"
else
    echo -e "${YELLOW}⚠ 未安装 PostgreSQL CLI，跳过数据库初始化${NC}"
    echo -e "  请手动安装: brew install postgresql@14"
fi

# 4. 编译项目
echo -e "\n${YELLOW}[4/4] 编译项目...${NC}"
cd "$SCRIPT_DIR"
mvn clean install -DskipTests -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 项目编译成功${NC}"
else
    echo -e "${RED}✗ 项目编译失败${NC}"
    exit 1
fi

echo -e "\n${GREEN}=========================================${NC}"
echo -e "${GREEN}  ✅ 开发环境初始化完成!                 ${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "启动后端服务: ${YELLOW}mvn spring-boot:run${NC}"
echo -e "访问 API 文档: ${YELLOW}http://localhost:8080/swagger-ui.html${NC}"
echo ""

# 提示：如果需要生产部署，使用根目录的 setup.sh
echo -e "${YELLOW}提示: 生产环境部署请使用项目根目录的 setup.sh${NC}"