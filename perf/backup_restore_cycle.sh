#!/bin/bash
# 备份恢复链路时延与成功率测试脚本

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"
BACKUP_DIR="${BACKUP_DIR:-./backups/test}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        log_error "curl 未安装"
        exit 1
    fi
    if ! command -v jq &> /dev/null; then
        log_warn "jq 未安装，JSON 解析可能失败"
    fi
}

# 触发全量备份
trigger_full_backup() {
    log_info "触发全量备份..."
    local start_time=$(date +%s)
    
    local response
    if [ -n "$TOKEN" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/trigger" \
            -d '{"type":"full"}')
    else
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/trigger" \
            -d '{"type":"full"}')
    fi
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
        log_error "备份触发失败，HTTP 状态码: $http_code"
        log_error "响应: $body"
        return 1
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log_info "全量备份触发成功，耗时: ${duration}s"
    
    # 提取备份 ID（如果 API 返回）
    if command -v jq &> /dev/null; then
        echo "$body" | jq -r '.data.id // .id // empty'
    else
        echo ""
    fi
}

# 触发增量备份
trigger_incremental_backup() {
    log_info "触发增量备份..."
    local start_time=$(date +%s)
    
    local response
    if [ -n "$TOKEN" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/trigger" \
            -d '{"type":"incremental"}')
    else
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/trigger" \
            -d '{"type":"incremental"}')
    fi
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
        log_error "增量备份触发失败，HTTP 状态码: $http_code"
        return 1
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log_info "增量备份触发成功，耗时: ${duration}s"
    
    if command -v jq &> /dev/null; then
        echo "$body" | jq -r '.data.id // .id // empty'
    else
        echo ""
    fi
}

# 校验备份文件哈希
verify_backup() {
    local backup_id=$1
    log_info "校验备份文件哈希 (ID: $backup_id)..."
    
    local response
    if [ -n "$TOKEN" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/verify" \
            -d "{\"backupId\":\"$backup_id\"}")
    else
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/verify" \
            -d "{\"backupId\":\"$backup_id\"}")
    fi
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" = "200" ]; then
        log_info "备份文件哈希校验通过"
        return 0
    else
        log_error "备份文件哈希校验失败，HTTP 状态码: $http_code"
        return 1
    fi
}

# 恢复备份
restore_backup() {
    local backup_id=$1
    log_info "恢复备份 (ID: $backup_id)..."
    local start_time=$(date +%s)
    
    local response
    if [ -n "$TOKEN" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/restore" \
            -d "{\"backupId\":\"$backup_id\"}")
    else
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            "${BASE_URL}/api/admin/backup/restore" \
            -d "{\"backupId\":\"$backup_id\"}")
    fi
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
        log_error "恢复失败，HTTP 状态码: $http_code"
        log_error "响应: $body"
        return 1
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log_info "恢复成功，耗时: ${duration}s"
    return 0
}

# 主流程
main() {
    log_info "开始备份恢复链路测试..."
    check_dependencies
    
    # 创建备份目录
    mkdir -p "$BACKUP_DIR"
    
    # 1. 全量备份
    local full_backup_id=$(trigger_full_backup)
    if [ -z "$full_backup_id" ]; then
        log_warn "无法获取全量备份 ID，跳过后续验证"
    else
        sleep 5 # 等待备份完成
        verify_backup "$full_backup_id" || log_warn "备份校验失败，但继续测试"
    fi
    
    # 2. 增量备份
    local inc_backup_id=$(trigger_incremental_backup)
    if [ -n "$inc_backup_id" ]; then
        sleep 5
        verify_backup "$inc_backup_id" || log_warn "增量备份校验失败"
    fi
    
    # 3. 恢复测试（如果提供了备份 ID）
    if [ -n "$full_backup_id" ]; then
        log_warn "恢复操作可能影响生产数据，请手动执行"
        # restore_backup "$full_backup_id"
    fi
    
    log_info "备份恢复链路测试完成"
}

# 如果直接执行脚本
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi












