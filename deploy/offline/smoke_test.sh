#!/bin/bash
# NexusArchive 冒烟测试脚本
# 用于部署后自动验证关键接口

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 默认配置
BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT=10
PASSED=0
FAILED=0

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 冒烟测试                                 ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "目标地址: $BASE_URL"
echo ""

# 测试函数
test_endpoint() {
    local name="$1"
    local method="$2"
    local path="$3"
    local expected_code="${4:-200}"
    
    local url="${BASE_URL}${path}"
    local response_code
    
    response_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" -X "$method" "$url" 2>/dev/null || echo "000")
    
    if [[ "$response_code" == "$expected_code" ]]; then
        echo -e "${GREEN}✓${NC} $name (HTTP $response_code)"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $name (期望: $expected_code, 实际: $response_code)"
        ((FAILED++))
    fi
}

# 端口检测
check_port() {
    local port="$1"
    if netstat -tlnp 2>/dev/null | grep -q ":$port " || ss -tlnp 2>/dev/null | grep -q ":$port "; then
        echo -e "${GREEN}✓${NC} 端口 $port 已监听"
        return 0
    else
        echo -e "${RED}✗${NC} 端口 $port 未监听"
        return 1
    fi
}

echo "=== 基础检测 ==="
check_port 8080 || check_port 19090

echo ""
echo "=== 健康检查 ==="
test_endpoint "Actuator Health" "GET" "/api/actuator/health"

echo ""
echo "=== 认证接口 ==="
test_endpoint "登录页面" "POST" "/api/auth/login" "401"

echo ""
echo "=== 核心业务接口 ==="
test_endpoint "档案列表" "GET" "/api/archives" "401"
test_endpoint "全宗列表" "GET" "/api/fonds" "401"
test_endpoint "借阅列表" "GET" "/api/borrowing" "401"

echo ""
echo "=== 静态资源 ==="
# 前端资源（通过 Nginx）
curl -s --max-time "$TIMEOUT" "http://localhost/" -o /dev/null 2>/dev/null && \
    echo -e "${GREEN}✓${NC} 前端首页可访问" && ((PASSED++)) || \
    echo -e "${YELLOW}⚠${NC} 前端首页不可访问（可能 Nginx 未配置）"

echo ""
echo "══════════════════════════════════════════════════════════"
echo -e "测试结果: ${GREEN}通过 $PASSED${NC} / ${RED}失败 $FAILED${NC}"
echo "══════════════════════════════════════════════════════════"

if [[ $FAILED -gt 0 ]]; then
    echo ""
    echo -e "${RED}冒烟测试未完全通过，请检查服务状态${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}所有冒烟测试通过 ✓${NC}"
    exit 0
fi
