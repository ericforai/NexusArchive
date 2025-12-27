#!/usr/bin/env bash
# Input: Shell、curl、jq
# Output: 完整测试流程结果
# Pos: 测试脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
#
# 完整测试流程自动化脚本
# 12步完整测试：数据清理→ERP同步→凭证池→归档审批→日志审计

set -euo pipefail

# ============================
# 配置
# ============================
BASE_URL="${BASE_URL:-http://localhost:8080/api}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-pass}"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
WARNINGS=0
TOKEN=""

# ============================
# 通用函数
# ============================
log_header() {
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════${NC}"
}

log_step() {
    echo -e "${YELLOW}▶ $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED++))
}

log_fail() {
    echo -e "${RED}✗ $1${NC}"
    ((FAILED++))
}

log_warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
    ((WARNINGS++))
}

check_deps() {
    log_step "检查依赖..."
    command -v curl >/dev/null 2>&1 || { log_fail "curl 未安装"; exit 1; }
    command -v jq >/dev/null 2>&1 || { log_fail "jq 未安装"; exit 1; }
    log_success "依赖检查通过"
}

# ============================
# 认证
# ============================
login() {
    log_step "登录获取Token..."
    
    LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")
    
    TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.token // empty')
    
    if [[ -z "$TOKEN" ]]; then
        log_fail "登录失败: $(echo "$LOGIN_RESP" | jq -r '.message // .code // .')"
        exit 1
    fi
    
    log_success "登录成功，Token已获取"
}

auth_header() {
    echo "Authorization: Bearer $TOKEN"
}

api_get() {
    local path=$1
    curl -s -H "$(auth_header)" "$BASE_URL$path"
}

api_post() {
    local path=$1
    local data=$2
    curl -s -X POST -H "$(auth_header)" -H 'Content-Type: application/json' \
        "$BASE_URL$path" -d "$data"
}

api_delete() {
    local path=$1
    curl -s -X DELETE -H "$(auth_header)" "$BASE_URL$path"
}

# ============================
# 步骤 1: 数据清理
# ============================
step1_cleanup() {
    log_header "步骤 1: 数据清理"
    
    log_step "说明：为保证测试数据完整性，此步骤建议手动执行SQL"
    echo "  建议执行以下SQL（在 psql 中执行）："
    echo "  DELETE FROM arc_file_content WHERE source LIKE 'YONSUITE%';"
    echo "  DELETE FROM acc_archive WHERE data_source LIKE 'YONSUITE%';"
    echo "  DELETE FROM archive_approval WHERE status = 'PENDING';"
    
    log_warn "跳过自动清理（数据安全考虑），继续后续步骤"
}

# ============================
# 步骤 2: ERP同步
# ============================
step2_erp_sync() {
    log_header "步骤 2: ERP同步 (VOUCHER_SYNC)"
    
    log_step "触发YonSuite凭证同步..."
    
    SYNC_RESP=$(curl -s -X POST -H "$(auth_header)" -H 'Content-Type: application/json' \
        "$BASE_URL/integration/yonsuite/vouchers/sync" \
        -d '{"startDate": "2020-01-01", "endDate": "2025-12-31", "pageSize": 100}')
    
    SYNC_CODE=$(echo "$SYNC_RESP" | jq -r '.code // 500')
    SYNC_MSG=$(echo "$SYNC_RESP" | jq -r '.data.message // .message // "未知"')
    SYNC_COUNT=$(echo "$SYNC_RESP" | jq -r '.data.total // .data.newCount // 0')
    
    if [[ "$SYNC_CODE" == "200" ]] || [[ "$SYNC_CODE" == "0" ]]; then
        log_success "ERP同步成功: $SYNC_MSG"
        echo "  同步条数: $SYNC_COUNT"
    else
        log_fail "ERP同步失败: $SYNC_MSG"
        echo "  响应: $(echo "$SYNC_RESP" | jq -c '.')"
    fi
}

# ============================
# 步骤 3: 电子凭证池验证
# ============================
step3_pool_check() {
    log_header "步骤 3: 电子凭证池验证"
    
    log_step "查询凭证池状态统计..."
    
    STATS_RESP=$(api_get "/pool/stats")
    STATS_CODE=$(echo "$STATS_RESP" | jq -r '.code // 500')
    
    if [[ "$STATS_CODE" == "200" ]] || [[ "$STATS_CODE" == "0" ]]; then
        echo "  凭证池状态统计:"
        echo "$STATS_RESP" | jq -r '.data | to_entries | .[] | "    \(.key): \(.value)"' 2>/dev/null || echo "    (解析失败)"
        
        TOTAL=$(echo "$STATS_RESP" | jq -r '.data.total // .data.PENDING_ARCHIVE // 0')
        if [[ "$TOTAL" -gt 0 ]]; then
            log_success "凭证池有数据 (总计: $TOTAL)"
        else
            log_warn "凭证池为空，需要先同步数据"
        fi
    else
        log_fail "获取凭证池统计失败"
    fi
    
    log_step "获取凭证池列表..."
    POOL_RESP=$(api_get "/pool")
    POOL_COUNT=$(echo "$POOL_RESP" | jq -r '.data | length // 0')
    echo "  当前凭证池项目数: $POOL_COUNT"
}

# ============================
# 步骤 4: 凭证关联验证
# ============================
step4_archive_check() {
    log_header "步骤 4: 凭证关联验证 (acc_archive)"
    
    log_step "查询档案列表 (AC01: 会计凭证)..."
    
    ARCHIVE_RESP=$(api_get "/archives?categoryCode=AC01&page=1&limit=10")
    ARCHIVE_CODE=$(echo "$ARCHIVE_RESP" | jq -r '.code // 500')
    
    if [[ "$ARCHIVE_CODE" == "200" ]] || [[ "$ARCHIVE_CODE" == "0" ]]; then
        ARCHIVE_TOTAL=$(echo "$ARCHIVE_RESP" | jq -r '.data.total // .data.records | length // 0')
        log_success "档案列表查询成功 (总计: $ARCHIVE_TOTAL)"
        
        if [[ "$ARCHIVE_TOTAL" -gt 0 ]]; then
            echo "  最近档案:"
            echo "$ARCHIVE_RESP" | jq -r '.data.records[:3] | .[] | "    - \(.archivalCode // .id): \(.title // .memo // "无标题")"' 2>/dev/null || true
        fi
    else
        log_fail "档案列表查询失败"
        echo "  响应: $(echo "$ARCHIVE_RESP" | jq -c '.' 2>/dev/null || echo "$ARCHIVE_RESP")"
    fi
}

# ============================
# 步骤 5: 创建原始凭证
# ============================
step5_create_voucher() {
    log_header "步骤 5: 创建原始凭证"
    
    log_step "此步骤需要通过前端上传实际文件"
    log_step "前端地址: http://localhost:5173/archive/voucher-matching"
    log_warn "手动步骤：请通过前端上传测试发票/合同文件"
    
    # 检查是否有现有的原始凭证
    log_step "查询现有原始凭证..."
    VOUCHER_RESP=$(api_get "/archives?categoryCode=AC04&page=1&limit=10")
    VOUCHER_COUNT=$(echo "$VOUCHER_RESP" | jq -r '.data.total // .data.records | length // 0')
    echo "  现有原始凭证数量: $VOUCHER_COUNT"
}

# ============================
# 步骤 6: 智能匹配
# ============================
step6_matching() {
    log_header "步骤 6: 智能匹配"
    
    log_step "获取待匹配凭证..."
    
    # 获取未匹配的档案
    UNMATCHED=$(api_get "/archives?categoryCode=AC01&status=PENDING&page=1&limit=5")
    UNMATCHED_IDS=$(echo "$UNMATCHED" | jq -r '.data.records[].id // empty' | head -5)
    
    if [[ -z "$UNMATCHED_IDS" ]]; then
        # 尝试获取任意凭证
        UNMATCHED=$(api_get "/archives?categoryCode=AC01&page=1&limit=5")
        UNMATCHED_IDS=$(echo "$UNMATCHED" | jq -r '.data.records[].id // empty' | head -5)
    fi
    
    if [[ -z "$UNMATCHED_IDS" ]]; then
        log_warn "没有找到可匹配的凭证"
        return
    fi
    
    # 转换为JSON数组
    IDS_ARRAY=$(echo "$UNMATCHED_IDS" | jq -R -s -c 'split("\n") | map(select(length > 0))')
    echo "  待匹配凭证ID: $IDS_ARRAY"
    
    log_step "执行批量匹配..."
    MATCH_RESP=$(api_post "/matching/execute/batch" "$IDS_ARRAY")
    MATCH_CODE=$(echo "$MATCH_RESP" | jq -r '.code // 500')
    
    if [[ "$MATCH_CODE" == "200" ]] || [[ "$MATCH_CODE" == "0" ]]; then
        MATCH_TOTAL=$(echo "$MATCH_RESP" | jq -r '.data.total // 0')
        MATCH_COMPLETED=$(echo "$MATCH_RESP" | jq -r '.data.completed // 0')
        log_success "匹配任务完成 (处理: $MATCH_COMPLETED/$MATCH_TOTAL)"
    else
        log_fail "匹配失败"
        echo "  响应: $(echo "$MATCH_RESP" | jq -c '.' 2>/dev/null)"
    fi
}

# ============================
# 步骤 7: 四性检测
# ============================
step7_compliance() {
    log_header "步骤 7: 四性检测"
    
    log_step "获取合规性统计..."
    COMP_STATS=$(api_get "/compliance/statistics")
    COMP_CODE=$(echo "$COMP_STATS" | jq -r '.code // 500')
    
    if [[ "$COMP_CODE" == "200" ]] || [[ "$COMP_CODE" == "0" ]]; then
        echo "  合规性统计:"
        echo "$COMP_STATS" | jq -r '.data | "    总数: \(.totalCount // 0), 完全合规: \(.fullyCompliant // 0), 有警告: \(.compliantWithWarnings // 0), 不合规: \(.nonCompliant // 0)"' 2>/dev/null || true
        log_success "合规性统计获取成功"
    else
        log_warn "合规性统计获取失败"
    fi
    
    # 随机选择一个档案进行检测
    ARCHIVE_RESP=$(api_get "/archives?categoryCode=AC01&page=1&limit=1")
    TEST_ID=$(echo "$ARCHIVE_RESP" | jq -r '.data.records[0].id // empty')
    
    if [[ -n "$TEST_ID" ]]; then
        log_step "执行单档案四性检测 (ID: $TEST_ID)..."
        CHECK_RESP=$(api_get "/compliance/$TEST_ID")
        CHECK_CODE=$(echo "$CHECK_RESP" | jq -r '.code // 500')
        
        if [[ "$CHECK_CODE" == "200" ]] || [[ "$CHECK_CODE" == "0" ]]; then
            echo "  检测结果:"
            echo "$CHECK_RESP" | jq -r '.data | "    真实性: \(.checks.authenticity // "N/A"), 完整性: \(.checks.integrity // "N/A"), 可用性: \(.checks.usability // "N/A"), 安全性: \(.checks.security // "N/A")"' 2>/dev/null || \
            echo "    $(echo "$CHECK_RESP" | jq -c '.data' 2>/dev/null)"
            log_success "四性检测完成"
        else
            log_fail "四性检测失败"
        fi
    else
        log_warn "没有可用的档案进行检测"
    fi
}

# ============================
# 步骤 8: 提交归档
# ============================
step8_submit() {
    log_header "步骤 8: 提交归档"
    
    log_step "此步骤通过归档批次管理完成"
    log_step "API: POST /archive-submit-batch"
    
    # 查询现有批次
    BATCH_RESP=$(api_get "/archive-submit-batch?page=1&size=5")
    BATCH_CODE=$(echo "$BATCH_RESP" | jq -r '.code // 500')
    
    if [[ "$BATCH_CODE" == "200" ]] || [[ "$BATCH_CODE" == "0" ]]; then
        BATCH_COUNT=$(echo "$BATCH_RESP" | jq -r '.data.total // .data.records | length // 0')
        echo "  现有归档批次: $BATCH_COUNT"
        
        if [[ "$BATCH_COUNT" -gt 0 ]]; then
            echo "  最近批次:"
            echo "$BATCH_RESP" | jq -r '.data.records[:3] | .[] | "    - ID: \(.id), 状态: \(.status // "未知")"' 2>/dev/null || true
        fi
        log_success "归档批次查询成功"
    else
        log_warn "无法查询归档批次"
    fi
    
    log_warn "手动步骤：请通过前端创建归档批次并提交"
}

# ============================
# 步骤 9: 归档审批
# ============================
step9_approval() {
    log_header "步骤 9: 归档审批"
    
    log_step "查询待审批列表..."
    
    APPROVAL_RESP=$(api_get "/archive-approval/list?status=PENDING&page=1&limit=10")
    APPROVAL_CODE=$(echo "$APPROVAL_RESP" | jq -r '.code // 500')
    
    if [[ "$APPROVAL_CODE" == "200" ]] || [[ "$APPROVAL_CODE" == "0" ]]; then
        PENDING_COUNT=$(echo "$APPROVAL_RESP" | jq -r '.data.total // .data.records | length // 0')
        echo "  待审批数量: $PENDING_COUNT"
        
        if [[ "$PENDING_COUNT" -gt 0 ]]; then
            echo "  待审批列表:"
            echo "$APPROVAL_RESP" | jq -r '.data.records[:3] | .[] | "    - ID: \(.id), 申请人: \(.applicantName // "未知")"' 2>/dev/null || true
            log_success "有待审批的归档申请"
        else
            log_warn "没有待审批的归档申请"
        fi
    else
        log_fail "审批列表查询失败"
    fi
}

# ============================
# 步骤 10: 会计档案查询
# ============================
step10_query() {
    log_header "步骤 10: 会计档案查询"
    
    log_step "查询已归档的记账凭证..."
    
    ARCHIVED=$(api_get "/archives?categoryCode=AC01&status=ARCHIVED&page=1&limit=10")
    ARCHIVED_CODE=$(echo "$ARCHIVED" | jq -r '.code // 500')
    
    if [[ "$ARCHIVED_CODE" == "200" ]] || [[ "$ARCHIVED_CODE" == "0" ]]; then
        ARCHIVED_COUNT=$(echo "$ARCHIVED" | jq -r '.data.total // .data.records | length // 0')
        log_success "已归档凭证数量: $ARCHIVED_COUNT"
        
        if [[ "$ARCHIVED_COUNT" -gt 0 ]]; then
            echo "  最近归档:"
            echo "$ARCHIVED" | jq -r '.data.records[:3] | .[] | "    - \(.archivalCode // .id): \(.title // .memo // "无标题")"' 2>/dev/null || true
        fi
    else
        log_fail "查询失败"
    fi
}

# ============================
# 步骤 11: 全景视图
# ============================
step11_panorama() {
    log_header "步骤 11: 全景视图 (全局搜索)"
    
    log_step "执行全局搜索 (关键词: 凭证)..."
    
    SEARCH_RESP=$(api_get "/search?q=%E5%87%AD%E8%AF%81")  # URL encoded: 凭证
    
    # 检查是否返回数组或对象
    SEARCH_TYPE=$(echo "$SEARCH_RESP" | jq -r 'type')
    
    if [[ "$SEARCH_TYPE" == "array" ]]; then
        SEARCH_COUNT=$(echo "$SEARCH_RESP" | jq -r 'length')
        log_success "全局搜索返回 $SEARCH_COUNT 条结果"
        
        if [[ "$SEARCH_COUNT" -gt 0 ]]; then
            echo "  搜索结果示例:"
            echo "$SEARCH_RESP" | jq -r '.[0:3] | .[] | "    - \(.type // "未知类型"): \(.title // .name // .id)"' 2>/dev/null || true
        fi
    elif [[ "$SEARCH_TYPE" == "object" ]]; then
        SEARCH_CODE=$(echo "$SEARCH_RESP" | jq -r '.code // 500')
        if [[ "$SEARCH_CODE" == "200" ]] || [[ "$SEARCH_CODE" == "0" ]]; then
            log_success "全局搜索成功"
        else
            log_warn "全局搜索返回异常: $(echo "$SEARCH_RESP" | jq -c '.')"
        fi
    else
        log_fail "全局搜索失败"
    fi
}

# ============================
# 步骤 12: 日志审计
# ============================
step12_audit() {
    log_header "步骤 12: 日志审计"
    
    log_step "查询审计日志..."
    
    AUDIT_RESP=$(api_get "/audit-logs?page=1&limit=20")
    AUDIT_CODE=$(echo "$AUDIT_RESP" | jq -r '.code // 500')
    
    if [[ "$AUDIT_CODE" == "200" ]] || [[ "$AUDIT_CODE" == "0" ]]; then
        AUDIT_COUNT=$(echo "$AUDIT_RESP" | jq -r '.data.total // .data.records | length // 0')
        log_success "审计日志记录数: $AUDIT_COUNT"
        
        if [[ "$AUDIT_COUNT" -gt 0 ]]; then
            echo "  最近操作日志:"
            echo "$AUDIT_RESP" | jq -r '.data.records[:5] | .[] | "    - [\(.createdTime // .createTime // "?")] \(.operationType // .action // "?"): \(.description // .resourceType // "?")"' 2>/dev/null || \
            echo "$AUDIT_RESP" | jq -r '.data.records[:5] | .[] | "    - \(.)"' 2>/dev/null || true
        fi
    else
        log_fail "审计日志查询失败"
        echo "  响应: $(echo "$AUDIT_RESP" | jq -c '.' 2>/dev/null)"
    fi
}

# ============================
# 测试报告
# ============================
print_summary() {
    echo ""
    log_header "测试执行完成"
    echo ""
    echo -e "  ${GREEN}通过: $PASSED${NC}"
    echo -e "  ${RED}失败: $FAILED${NC}"
    echo -e "  ${YELLOW}警告: $WARNINGS${NC}"
    echo ""
    
    if [[ $FAILED -gt 0 ]]; then
        echo -e "${RED}存在失败项，请检查相关功能${NC}"
        exit 1
    elif [[ $WARNINGS -gt 0 ]]; then
        echo -e "${YELLOW}存在警告项，部分功能需要手动验证${NC}"
        exit 0
    else
        echo -e "${GREEN}所有测试项通过！${NC}"
        exit 0
    fi
}

# ============================
# 主流程
# ============================
main() {
    echo ""
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     NexusArchive 完整测试流程 (12步)                      ║${NC}"
    echo -e "${BLUE}║     $(date '+%Y-%m-%d %H:%M:%S')                             ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}"
    
    check_deps
    login
    
    step1_cleanup
    step2_erp_sync
    step3_pool_check
    step4_archive_check
    step5_create_voucher
    step6_matching
    step7_compliance
    step8_submit
    step9_approval
    step10_query
    step11_panorama
    step12_audit
    
    print_summary
}

main "$@"
