#!/bin/bash
#===============================================================================
# NexusArchive 配置验证脚本
#===============================================================================
# 用途: 验证所有配置文件和环境变量中的端口配置是否一致
#
# 检查项:
#   1. 环境变量文件端口配置
#   2. 应用配置文件端口配置
#   3. Docker Compose 文件端口配置
#   4. 前端配置文件端口配置
#   5. 服务运行状态与配置一致性
#
# 使用: bash scripts/validate-config.sh [--fix] [--verbose]
#===============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# 期望的端口配置
EXPECTED_FRONTEND_PORT=15175
EXPECTED_BACKEND_PORT=19090
EXPECTED_DB_PORT_LOCAL=54321
EXPECTED_DB_PORT_CONTAINER=5432
EXPECTED_REDIS_PORT_LOCAL=16379
EXPECTED_REDIS_PORT_CONTAINER=6379

# 解析参数
FIX_MODE=false
VERBOSE=false
for arg in "$@"; do
    case $arg in
        --fix) FIX_MODE=true ;;
        --verbose|-v) VERBOSE=true ;;
    esac
done

# 计数器
TOTAL_ISSUES=0
TOTAL_CHECKS=0
FIXED_COUNT=0

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}🔍 NexusArchive 配置验证${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 辅助函数
check_pass() {
    echo -e "  ${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "  ${RED}✗${NC} $1"
    TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
}

check_warn() {
    echo -e "  ${YELLOW}⚠${NC} $1"
}

info() {
    [ "$VERBOSE" = true ] && echo -e "  ${CYAN}ℹ${NC} $1"
}

section() {
    echo -e "\n${YELLOW}$1${NC}"
}

# =============================================================================
# 1. 检查环境变量文件
# =============================================================================
section "📋 检查 1/5: 环境变量文件"

# 检查 .env.template
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "${MAGENTA}检查 .env.template${NC}"
if [ -f .env.template ]; then
    if grep -q "DB_PORT=54321" .env.template; then
        check_pass "DB_PORT=54321"
    else
        CURRENT=$(grep "^DB_PORT=" .env.template | cut -d'=' -f2)
        check_fail "DB_PORT=$CURRENT (期望: 54321)"
        if [ "$FIX_MODE" = true ]; then
            sed -i '' 's/^DB_PORT=.*/DB_PORT=54321/' .env.template
            echo -e "    ${GREEN}已修复${NC}"
            FIXED_COUNT=$((FIXED_COUNT + 1))
        fi
    fi

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if grep -q "REDIS_PORT=16379" .env.template; then
        check_pass "REDIS_PORT=16379"
    else
        CURRENT=$(grep "^REDIS_PORT=" .env.template | cut -d'=' -f2)
        check_fail "REDIS_PORT=$CURRENT (期望: 16379)"
        if [ "$FIX_MODE" = true ]; then
            sed -i '' 's/^REDIS_PORT=.*/REDIS_PORT=16379/' .env.template
            echo -e "    ${GREEN}已修复${NC}"
            FIXED_COUNT=$((FIXED_COUNT + 1))
        fi
    fi

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if grep -q "SERVER_PORT=19090" .env.template; then
        check_pass "SERVER_PORT=19090"
    else
        CURRENT=$(grep "^SERVER_PORT=" .env.template | cut -d'=' -f2)
        check_fail "SERVER_PORT=$CURRENT (期望: 19090)"
    fi

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    if grep -q "FRONTEND_PORT=15175" .env.template; then
        check_pass "FRONTEND_PORT=15175"
    else
        CURRENT=$(grep "^FRONTEND_PORT=" .env.template | cut -d'=' -f2)
        check_fail "FRONTEND_PORT=$CURRENT (期望: 15175)"
    fi
else
    check_fail ".env.template 文件不存在"
fi

# 检查 .env.local
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查 .env.local${NC}"
if [ -f .env.local ]; then
    DB_PORT=$(grep "^DB_PORT=" .env.local | cut -d'=' -f2)
    REDIS_PORT=$(grep "^REDIS_PORT=" .env.local | cut -d'=' -f2)
    SERVER_PORT=$(grep "^SERVER_PORT=" .env.local | cut -d'=' -f2)
    FRONTEND_PORT=$(grep "^FRONTEND_PORT=" .env.local | cut -d'=' -f2)

    [ "$DB_PORT" = "$EXPECTED_DB_PORT_LOCAL" ] && check_pass "DB_PORT=$DB_PORT" || check_fail "DB_PORT=$DB_PORT (期望: $EXPECTED_DB_PORT_LOCAL)"
    [ "$REDIS_PORT" = "$EXPECTED_REDIS_PORT_LOCAL" ] && check_pass "REDIS_PORT=$REDIS_PORT" || check_fail "REDIS_PORT=$REDIS_PORT (期望: $EXPECTED_REDIS_PORT_LOCAL)"
    [ "$SERVER_PORT" = "$EXPECTED_BACKEND_PORT" ] && check_pass "SERVER_PORT=$SERVER_PORT" || check_fail "SERVER_PORT=$SERVER_PORT (期望: $EXPECTED_BACKEND_PORT)"
    [ "$FRONTEND_PORT" = "$EXPECTED_FRONTEND_PORT" ] && check_pass "FRONTEND_PORT=$FRONTEND_PORT" || check_fail "FRONTEND_PORT=$FRONTEND_PORT (期望: $EXPECTED_FRONTEND_PORT)"
else
    check_warn ".env.local 文件不存在（首次开发需要创建）"
fi

# 检查 .env.example
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查 .env.example${NC}"
if [ -f .env.example ]; then
    if grep -q "DB_PORT=54321" .env.example; then
        check_pass "DB_PORT=54321"
    else
        CURRENT=$(grep "^DB_PORT=" .env.example | cut -d'=' -f2)
        check_fail "DB_PORT=$CURRENT (期望: 54321)"
    fi
else
    check_warn ".env.example 文件不存在"
fi

# =============================================================================
# 2. 检查应用配置文件
# =============================================================================
section "📋 检查 2/5: 应用配置文件"

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "${MAGENTA}检查 application.yml${NC}"
if [ -f nexusarchive-java/src/main/resources/application.yml ]; then
    # 检查 dev profile 的 DB_PORT
    if grep -q "DB_PORT:54321" nexusarchive-java/src/main/resources/application.yml; then
        check_pass "dev profile DB_PORT:54321"
    else
        check_fail "dev profile DB_PORT 不是 54321"
    fi

    # 检查 Redis 端口
    if grep -q "port: \${REDIS_PORT:16379}" nexusarchive-java/src/main/resources/application.yml; then
        check_pass "REDIS_PORT 默认值: 16379"
    else
        check_fail "REDIS_PORT 默认值不是 16379"
    fi

    # 检查 server.port
    if grep -q "port: \${SERVER_PORT:19090}" nexusarchive-java/src/main/resources/application.yml; then
        check_pass "SERVER_PORT 默认值: 19090"
    else
        check_fail "SERVER_PORT 默认值不是 19090"
    fi

    # 检查 CORS 配置
    if grep -q "http://localhost:15175" nexusarchive-java/src/main/resources/application.yml; then
        check_pass "CORS 包含 http://localhost:15175"
    else
        check_fail "CORS 未包含 http://localhost:15175"
    fi
else
    check_fail "application.yml 文件不存在"
fi

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查 application.properties${NC}"
if [ -f nexusarchive-java/src/main/resources/application.properties ]; then
    if grep -q "server.port=19090" nexusarchive-java/src/main/resources/application.properties; then
        check_pass "server.port=19090"
    else
        check_fail "server.port 不是 19090"
    fi
else
    check_warn "application.properties 文件不存在"
fi

# =============================================================================
# 3. 检查 Docker Compose 文件
# =============================================================================
section "📋 检查 3/5: Docker Compose 文件"

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "${MAGENTA}检查 docker-compose.infra.yml${NC}"
if [ -f docker-compose.infra.yml ]; then
    # 支持 "54321:5432" 和 "${DB_PORT:-54321}:5432" 两种格式
    if grep -qE '54321.*5432' docker-compose.infra.yml; then
        check_pass "PostgreSQL 端口映射: 54321:5432"
    else
        check_fail "PostgreSQL 端口映射不是 54321:5432"
    fi

    if grep -qE '16379.*6379' docker-compose.infra.yml; then
        check_pass "Redis 端口映射: 16379:6379"
    else
        check_fail "Redis 端口映射不是 16379:6379"
    fi
else
    check_fail "docker-compose.infra.yml 文件不存在"
fi

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查 docker-compose.app.yml${NC}"
if [ -f docker-compose.app.yml ]; then
    # 支持 "19090:19090" 和 "${SERVER_PORT:-19090}:19090" 两种格式
    if grep -qE '19090.*19090' docker-compose.app.yml; then
        check_pass "Backend 端口映射: 19090:19090"
    else
        check_fail "Backend 端口映射不是 19090:19090"
    fi
else
    check_warn "docker-compose.app.yml 文件不存在"
fi

# =============================================================================
# 4. 检查前端配置文件
# =============================================================================
section "📋 检查 4/5: 前端配置文件"

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "${MAGENTA}检查 vite.config.ts${NC}"
if [ -f vite.config.ts ]; then
    if grep -q "port: 15175" vite.config.ts; then
        check_pass "Vite 端口: 15175"
    else
        check_fail "Vite 端口不是 15175"
    fi

    if grep -q "hmr.*15175" vite.config.ts; then
        check_pass "Vite HMR 端口: 15175"
    else
        # HMR port 可能在单独的配置中
        [ "$VERBOSE" = true ] && echo -e "  ${CYAN}ℹ${NC} Vite HMR 端口配置需确认" || true
    fi || true

    if grep -q "19090" vite.config.ts; then
        check_pass "API proxy 目标: localhost:19090"
    else
        check_fail "API proxy 目标未配置为 19090"
    fi
else
    check_fail "vite.config.ts 文件不存在"
fi || true

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查 playwright.config.ts${NC}"
if [ -f playwright.config.ts ]; then
    if grep -q "localhost:15175" playwright.config.ts; then
        check_pass "Playwright BASE_URL: localhost:15175"
    else
        check_warn "Playwright BASE_URL 未配置为 localhost:15175"
    fi
else
    info "playwright.config.ts 不存在（E2E 测试配置可选）" || true
fi || true

# =============================================================================
# 5. 检查服务运行状态
# =============================================================================
section "📋 检查 5/5: 服务运行状态"

# 检查 Docker 容器
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "${MAGENTA}检查 Docker 服务${NC}"
if command -v docker &> /dev/null; then
    # 检查 PostgreSQL
    if docker ps --format '{{.Names}}' | grep -q 'nexus-db'; then
        DB_CONTAINER_PORT=$(docker port nexus-db 5432 2>/dev/null | head -1 | cut -d':' -f2 || echo "")
        if [ "$DB_CONTAINER_PORT" = "$EXPECTED_DB_PORT_LOCAL" ]; then
            check_pass "PostgreSQL 运行在端口 $EXPECTED_DB_PORT_LOCAL"
        else
            check_fail "PostgreSQL 端口: $DB_CONTAINER_PORT (期望: $EXPECTED_DB_PORT_LOCAL)"
        fi
    else
        check_warn "PostgreSQL 容器未运行"
    fi

    # 检查 Redis
    if docker ps --format '{{.Names}}' | grep -q 'nexus-redis'; then
        REDIS_CONTAINER_PORT=$(docker port nexus-redis 6379 2>/dev/null | head -1 | cut -d':' -f2 || echo "")
        if [ "$REDIS_CONTAINER_PORT" = "$EXPECTED_REDIS_PORT_LOCAL" ]; then
            check_pass "Redis 运行在端口 $EXPECTED_REDIS_PORT_LOCAL"
        else
            check_fail "Redis 端口: $REDIS_CONTAINER_PORT (期望: $EXPECTED_REDIS_PORT_LOCAL)"
        fi
    else
        check_warn "Redis 容器未运行"
    fi
else
    check_warn "Docker 未安装或未运行"
fi

# 检查后端 API
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查后端 API${NC}"
if command -v curl &> /dev/null; then
    if curl -s http://localhost:19090/api/health > /dev/null 2>&1; then
        check_pass "后端 API 运行在端口 19090"
    else
        check_warn "后端 API 未响应 (端口 19090)"
    fi
else
    info "curl 不可用，跳过 API 检查"
fi

# 检查前端
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
echo -e "\n${MAGENTA}检查前端服务${NC}"
if command -v curl &> /dev/null; then
    if curl -s http://localhost:15175 > /dev/null 2>&1; then
        check_pass "前端服务运行在端口 15175"
    else
        check_warn "前端服务未响应 (端口 15175)"
    fi
else
    info "curl 不可用，跳过前端检查"
fi

# =============================================================================
# 总结
# =============================================================================
echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}📊 验证总结${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "检查项目数: ${GREEN}$TOTAL_CHECKS${NC}"
echo -e "发现问题数: ${RED}$TOTAL_ISSUES${NC}"

if [ "$FIX_MODE" = true ]; then
    echo -e "已修复问题: ${GREEN}$FIXED_COUNT${NC}"
fi

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}📖 期望的端口配置${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  前端:       ${GREEN}15175${NC}  (Vite 开发服务器)"
echo -e "  后端 API:   ${GREEN}19090${NC}  (Spring Boot)"
echo -e "  PostgreSQL: ${GREEN}54321${NC}  (本地映射) / 5432 (容器内)"
echo -e "  Redis:      ${GREEN}16379${NC}  (本地映射) / 6379 (容器内)"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $TOTAL_ISSUES -eq 0 ]; then
    echo -e "\n${GREEN}✅ 所有配置验证通过！${NC}"
    exit 0
else
    echo -e "\n${RED}❌ 发现 $TOTAL_ISSUES 个配置问题${NC}"
    echo -e "${YELLOW}💡 运行 'bash scripts/validate-config.sh --fix' 自动修复部分问题${NC}"
    exit 1
fi
