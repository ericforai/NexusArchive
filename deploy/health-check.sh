#!/bin/bash
# Input: NexusArchive 健康检查脚本
# Output: 服务状态检查
# Pos: 健康监控
# 用法: ./deploy/health-check.sh [--verbose]
# Cron: */5 * * * * /opt/nexusarchive/scripts/health-check.sh

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

VERBOSE=${1:-false}
COMPOSE_FILES="-f docker-compose.infra.yml -f docker-compose.app.yml -f docker-compose.prod.yml"
DOMAIN=$(grep server_name nginx/nginx.prod.conf 2>/dev/null | head -1 | awk '{print $2}' || echo "localhost")

info() { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
error() { echo -e "${RED}[✗]${NC} $1"; }
header() { echo -e "\n${GREEN}=== $1 ===${NC}"; }

# 总体状态
STATUS=0

echo "=========================================="
echo "   NexusArchive 健康检查"
echo "=========================================="
info "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"

# 1. Docker 服务检查
header "Docker 服务"

SERVICES=("nexus-backend" "nexus-frontend" "nexus-db" "nexus-redis")
for SERVICE in "${SERVICES[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${SERVICE}$"; then
        STATUS_COLOR=$GREEN
        info "$SERVICE: 运行中"
    else
        STATUS_COLOR=$RED
        STATUS=1
        error "$SERVICE: 未运行"
    fi
done

# 2. 容器健康状态
header "容器健康状态"

for SERVICE in "${SERVICES[@]}"; do
    HEALTH=$(docker inspect --format='{{.State.Health.Status}}' $SERVICE 2>/dev/null || echo "no-healthcheck")
    case $HEALTH in
        healthy)
            info "$SERVICE: 健康"
            ;;
        unhealthy)
            error "$SERVICE: 不健康"
            STATUS=1
            ;;
        starting)
            warn "$SERVICE: 启动中"
            ;;
        *)
            warn "$SERVICE: 无健康检查 ($HEALTH)"
            ;;
    esac
done

# 3. HTTP 响应检查
header "HTTP 响应"

# 前端检查
if curl -sf -o /dev/null -w "%{http_code}" http://localhost/ | grep -q "200\|301\|302"; then
    info "前端: 响应正常"
else
    error "前端: 响应异常"
    STATUS=1
fi

# API 健康检查
API_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" http://localhost/api/health 2>/dev/null || echo "000")
if [ "$API_STATUS" = "200" ]; then
    info "API: 响应正常"
else
    error "API: 响应异常 (HTTP $API_STATUS)"
    STATUS=1
fi

# 4. 数据库连接检查
header "数据库连接"

if docker exec nexus-db pg_isready -U postgres > /dev/null 2>&1; then
    info "PostgreSQL: 连接正常"

    # 检查数据库大小
    DB_SIZE=$(docker exec nexus-db psql -U postgres -d nexusarchive -t -c "SELECT pg_size_pretty(pg_database_size('nexusarchive'));" 2>/dev/null | xargs || echo "N/A")
    info "数据库大小: $DB_SIZE"
else
    error "PostgreSQL: 连接失败"
    STATUS=1
fi

# 5. Redis 连接检查
header "Redis 连接"

if docker exec nexus-redis redis-cli ping > /dev/null 2>&1; then
    REDIS_MEMORY=$(docker exec nexus-redis redis-cli info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    info "Redis: 连接正常 (内存: ${REDIS_MEMORY:-N/A})"
else
    error "Redis: 连接失败"
    STATUS=1
fi

# 6. 磁盘空间检查
header "磁盘空间"

DISK_USAGE=$(df -h /opt/nexusarchive | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -lt 80 ]; then
    info "磁盘使用率: ${DISK_USAGE}% (正常)"
elif [ "$DISK_USAGE" -lt 90 ]; then
    warn "磁盘使用率: ${DISK_USAGE}% (警告)"
else
    error "磁盘使用率: ${DISK_USAGE}% (危急)"
    STATUS=1
fi

# 7. SSL 证书检查
header "SSL 证书"

if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
    CERT_EXPIRY=$(openssl x509 -enddate -noout -in "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" | cut -d= -f2)
    CERT_DAYS=$(( ($(date -d "$CERT_EXPIRY" +%s) - $(date +%s)) / 86400 ))

    if [ $CERT_DAYS -gt 30 ]; then
        info "SSL 证书: 有效 (剩余 $CERT_DAYS 天)"
    elif [ $CERT_DAYS -gt 7 ]; then
        warn "SSL 证书: 即将过期 (剩余 $CERT_DAYS 天)"
    else
        error "SSL 证书: 即将过期 (剩余 $CERT_DAYS 天)"
        STATUS=1
    fi
else
    warn "SSL 证书: 未配置"
fi

# 8. 最近日志错误检查
if [ "$VERBOSE" = "--verbose" ]; then
    header "最近错误日志"

    echo "--- 后端错误 (最近 10 条) ---"
    docker logs nexus-backend --tail 50 2>&1 | grep -i "error\|exception" | tail -5 || echo "无错误日志"

    echo "--- Nginx 错误 (最近 10 条) ---"
    docker exec nexus-frontend tail -20 /var/log/nginx/error.log 2>/dev/null || echo "无错误日志"
fi

# 总结
echo ""
echo "=========================================="
if [ $STATUS -eq 0 ]; then
    echo -e "${GREEN}   ✅ 所有检查通过${NC}"
else
    echo -e "${RED}   ⚠️  发现问题，请检查上述错误${NC}"
fi
echo "=========================================="

exit $STATUS
