#!/bin/bash
# Input: Shell
# Output: 健康检查
# Pos: 后端运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# ==============================================================================
# NexusArchive 健康检查脚本 (Health Check Script)
# 用于生产环境部署后验证系统可用性
# 适用环境：内网/离线 Linux 服务器
# ==============================================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 配置 (可通过环境变量覆盖)
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-nexus}"
INSTALL_DIR="${INSTALL_DIR:-/opt/nexusarchive}"
MIN_DISK_GB="${MIN_DISK_GB:-10}"

echo -e "${YELLOW}====================================================${NC}"
echo -e "${YELLOW}   NexusArchive 系统健康检查 (Health Check)          ${NC}"
echo -e "${YELLOW}====================================================${NC}"

PASS_COUNT=0
FAIL_COUNT=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASS_COUNT++))
}

check_fail() {
    echo -e "${RED}✗ $1${NC}"
    ((FAIL_COUNT++))
}

check_warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# 1. 后端服务健康检查
echo ""
echo -e "${YELLOW}[1/5] 检查后端服务...${NC}"
if curl -s --max-time 5 "${BACKEND_URL}/actuator/health" | grep -q '"status":"UP"'; then
    check_pass "后端服务正常 (${BACKEND_URL})"
else
    if curl -s --max-time 5 "${BACKEND_URL}/api/auth/check" > /dev/null 2>&1; then
        check_warn "后端服务可达但 Actuator 未启用"
        ((PASS_COUNT++))
    else
        check_fail "后端服务不可用 (${BACKEND_URL})"
    fi
fi

# 2. 数据库连接检查
echo ""
echo -e "${YELLOW}[2/5] 检查数据库连接...${NC}"
if command -v psql &> /dev/null; then
    if [ -z "$DB_PASSWORD" ]; then
        check_fail "DB_PASSWORD 环境变量未设置，无法检查数据库连接"
    elif PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
        check_pass "PostgreSQL 连接正常 (${DB_HOST}:${DB_PORT}/${DB_NAME})"
    else
        check_fail "PostgreSQL 连接失败"
    fi
else
    check_warn "psql 命令不可用，跳过数据库检查"
fi

# 3. 磁盘空间检查
echo ""
echo -e "${YELLOW}[3/5] 检查磁盘空间...${NC}"
if [ -d "$INSTALL_DIR" ]; then
    AVAILABLE_GB=$(df -BG "$INSTALL_DIR" | tail -1 | awk '{print $4}' | tr -d 'G')
    if [ "$AVAILABLE_GB" -ge "$MIN_DISK_GB" ]; then
        check_pass "磁盘空间充足: ${AVAILABLE_GB}GB 可用 (最低要求: ${MIN_DISK_GB}GB)"
    else
        check_fail "磁盘空间不足: ${AVAILABLE_GB}GB 可用 (最低要求: ${MIN_DISK_GB}GB)"
    fi
else
    check_warn "安装目录不存在: $INSTALL_DIR"
fi

# 4. 存储目录权限检查
echo ""
echo -e "${YELLOW}[4/5] 检查存储目录...${NC}"
STORAGE_PATH="${INSTALL_DIR}/storage"
if [ -d "$STORAGE_PATH" ]; then
    if [ -w "$STORAGE_PATH" ]; then
        check_pass "存储目录可写: ${STORAGE_PATH}"
    else
        check_fail "存储目录不可写: ${STORAGE_PATH}"
    fi
else
    check_warn "存储目录不存在: ${STORAGE_PATH}"
fi

# 5. 安全配置检查
echo ""
echo -e "${YELLOW}[5/5] 检查安全配置...${NC}"
ENV_FILE="${INSTALL_DIR}/.env"
if [ -f "$ENV_FILE" ]; then
    # 检查 SM4 密钥是否设置
    if grep -q "SM4_KEY=" "$ENV_FILE" && ! grep -q "SM4_KEY=$" "$ENV_FILE"; then
        check_pass "SM4 加密密钥已配置"
    else
        check_fail "SM4 加密密钥未配置"
    fi
    
    # 检查 JWT 密钥是否设置
    if grep -q "JWT_SECRET=" "$ENV_FILE" && ! grep -q "JWT_SECRET=$" "$ENV_FILE"; then
        check_pass "JWT 密钥已配置"
    else
        check_fail "JWT 密钥未配置"
    fi
else
    check_warn "环境配置文件不存在: ${ENV_FILE}"
fi

# 汇总结果
echo ""
echo -e "${YELLOW}====================================================${NC}"
if [ "$FAIL_COUNT" -eq 0 ]; then
    echo -e "${GREEN}   健康检查通过！ (${PASS_COUNT} 项检查全部通过)       ${NC}"
    echo -e "${YELLOW}====================================================${NC}"
    exit 0
else
    echo -e "${RED}   健康检查发现问题！                               ${NC}"
    echo -e "${RED}   通过: ${PASS_COUNT} 项 | 失败: ${FAIL_COUNT} 项      ${NC}"
    echo -e "${YELLOW}====================================================${NC}"
    exit 1
fi