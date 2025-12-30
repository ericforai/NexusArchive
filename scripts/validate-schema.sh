#!/usr/bin/env bash
# Input: psql、Java反射、数据库连接
# Output: Schema验证报告
# Pos: 运维脚本 - Schema一致性检查
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#
# Entity-Schema 一致性验证工具
# 用途：检测Entity类字段与数据库列的不一致
# 使用场景：
#   1. CI/CD流水线中的自动检查
#   2. 开发环境启动前的预检查
#   3. 生产部署前的Schema验证
#

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 加载环境变量
if [ -f "$ROOT/.env" ]; then
    set -a
    source "$ROOT/.env"
    set +a
fi

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-54321}
DB_NAME=${DB_NAME:-nexusarchive}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}

# 检查数据库连接
check_db_connection() {
    log_info "检查数据库连接: $DB_HOST:$DB_PORT/$DB_NAME"
    
    if ! PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &>/dev/null; then
        log_error "无法连接到数据库！"
        log_info "提示：请确保数据库已启动 (docker compose -f docker-compose.deps.yml up -d)"
        return 1
    fi
    
    log_success "数据库连接正常"
    return 0
}

# 获取数据库表的所有列
get_db_columns() {
    local table_name=$1
    
    PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = '$table_name' 
        ORDER BY ordinal_position;
    " | tr -d ' ' | grep -v '^$'
}

# 验证关键表的Schema
validate_table_schema() {
    local table_name=$1
    local expected_columns=$2
    
    log_info "验证表: $table_name"
    
    # 获取数据库实际列
    local db_columns=$(get_db_columns "$table_name")
    
    # 检查每个预期列是否存在
    local missing_columns=()
    for col in $expected_columns; do
        if ! echo "$db_columns" | grep -q "^${col}$"; then
            missing_columns+=("$col")
        fi
    done
    
    if [ ${#missing_columns[@]} -gt 0 ]; then
        log_error "表 $table_name 缺少以下列："
        for col in "${missing_columns[@]}"; do
            echo "  - $col"
        done
        return 1
    fi
    
    log_success "表 $table_name Schema验证通过"
    return 0
}

# 主验证流程
main() {
    echo ""
    echo "=========================================="
    echo "   Entity-Schema 一致性验证工具"
    echo "=========================================="
    echo ""
    
    # 检查数据库连接
    if ! check_db_connection; then
        exit 1
    fi
    
    echo ""
    log_info "开始Schema验证..."
    echo ""
    
    # 验证 arc_file_content 表（最关键的表）
    # 这些列来自 ArcFileContent.java Entity
    ARC_FILE_CONTENT_COLUMNS="
        id
        archival_code
        file_name
        file_type
        file_size
        file_hash
        hash_algorithm
        storage_path
        item_id
        original_hash
        current_hash
        timestamp_token
        sign_value
        certificate
        pre_archive_status
        check_result
        checked_time
        archived_time
        fiscal_year
        voucher_type
        creator
        fonds_code
        source_system
        business_doc_no
        erp_voucher_no
        source_data
        batch_id
        sequence_in_batch
        summary
        voucher_word
        doc_date
        highlight_meta
        created_time
    "
    
    local validation_failed=0
    
    if ! validate_table_schema "arc_file_content" "$ARC_FILE_CONTENT_COLUMNS"; then
        validation_failed=1
    fi
    
    echo ""
    
    if [ $validation_failed -eq 1 ]; then
        log_error "=========================================="
        log_error "  Schema验证失败！"
        log_error "=========================================="
        echo ""
        log_info "修复建议："
        log_info "1. 检查是否有未应用的Flyway迁移脚本"
        log_info "2. 运行: mvn flyway:migrate（或重启后端应用）"
        log_info "3. 如果是新增Entity字段，请创建对应的迁移脚本"
        echo ""
        exit 1
    fi
    
    echo ""
    log_success "=========================================="
    log_success "  所有Schema验证通过！"
    log_success "=========================================="
    echo ""
    
    exit 0
}

main "$@"
