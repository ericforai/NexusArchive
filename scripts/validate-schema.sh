#!/usr/bin/env bash
# Input: psql、Java SchemaValidator
# Output: Schema验证报告
# Pos: 运维脚本 - Schema一致性检查
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#
# Entity-Schema 一致性验证工具 v2.0
# 用途：自动检测Entity类字段与数据库列的不一致
# 原理：
#   1. 调用 Maven 运行 SchemaValidator 工具，扫描 Entity 类并生成预期列名文件
#   2. 读取生成的文件，与数据库实际列进行比对
#

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
JAVA_PROJECT="$ROOT/nexusarchive-java"

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

# 数据库执行函数 (Wrapper for psql)
run_psql() {
    local query="$1"
    local use_tuples_only="${2:-false}"
    
    local psql_opts=""
    if [ "$use_tuples_only" = "true" ]; then
        psql_opts="-t"
    fi

    if command -v psql &> /dev/null; then
        PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" $psql_opts -c "$query"
    else
        # Fallback to docker exec
        # Note: In docker, host is usually 'localhost' (internal)
        docker exec -i nexus-db psql -U "$DB_USER" -d "$DB_NAME" $psql_opts -c "$query"
    fi
}

# 检查数据库连接
check_db_connection() {
    log_info "检查数据库连接: $DB_HOST:$DB_PORT/$DB_NAME"
    
    if ! run_psql "SELECT 1" &>/dev/null; then
        log_error "无法连接到数据库！"
        log_info "提示：请确保数据库已启动 (docker compose -f docker-compose.infra.yml up -d)"
        return 1
    fi
    
    log_success "数据库连接正常"
    return 0
}

# 生成预期Schema文件
generate_schema_files() {
    log_info "正在从 Java Entity 生成预期 Schema..."
    
    pushd "$JAVA_PROJECT" > /dev/null
    
    # 清理旧文件
    rm -f target/*.columns.txt
    
    # 编译并运行工具
    # 使用 -q (quiet) 减少Maven输出干扰，但保留错误信息
    if ! mvn compile exec:java -Dexec.mainClass="com.nexusarchive.tools.SchemaValidator" -Dexec.classpathScope="compile" -q; then
        log_error "Schema 生成失败！请检查 Java 编译错误。"
        popd > /dev/null
        return 1
    fi
    
    popd > /dev/null
    
    # 检查是否有文件生成
    local count=$(ls "$JAVA_PROJECT/target"/*.columns.txt 2>/dev/null | wc -l | xargs)
    if [ "$count" -eq "0" ]; then
        log_error "未生成任何列名文件！"
        return 1
    fi
    
    log_success "成功生成 $count 个表的预期结构"
    return 0
}

# 获取数据库表的所有列
get_db_columns() {
    local table_name=$1
    
    run_psql "
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = '$table_name' 
        ORDER BY ordinal_position;
    " "true" | tr -d ' ' | grep -v '^$'
}

# 验证单个表的Schema
validate_table_schema() {
    local table_name=$1
    local expected_columns_file=$2
    
    # 获取数据库实际列
    local db_columns=$(get_db_columns "$table_name")
    
    # 如果数据库中没有这个表
    if [ -z "$db_columns" ]; then
        log_error "表 $table_name 在数据库中不存在！"
        return 1
    fi
    
    local missing_columns=()
    
    # 读取预期列文件进行比对
    while IFS= read -r col || [ -n "$col" ]; do
        # 忽略空行
        [ -z "$col" ] && continue
        
        if ! echo "$db_columns" | grep -q "^${col}$"; then
            missing_columns+=("$col")
        fi
    done < "$expected_columns_file"
    
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
    echo "   Entity-Schema 一致性验证工具 v2.0"
    echo "=========================================="
    echo ""
    
    # 1. 检查数据库连接
    if ! check_db_connection; then
        exit 1
    fi
    
    echo ""
    
    # 2. 生成预期文件
    if ! generate_schema_files; then
        exit 1
    fi
    
    echo ""
    log_info "开始Schema比对..."
    echo ""
    
    local total_tables=0
    local failed_tables=0
    
    # 3. 遍历所有生成的列名文件进行验证
    for columns_file in "$JAVA_PROJECT/target"/*.columns.txt; do
        table_name=$(basename "$columns_file" .columns.txt)
        
        if ! validate_table_schema "$table_name" "$columns_file"; then
            failed_tables=$((failed_tables + 1))
        fi
        total_tables=$((total_tables + 1))
    done
    
    echo ""
    
    if [ $failed_tables -gt 0 ]; then
        log_error "=========================================="
        log_error "  验证失败！共有 $failed_tables 个表存在不一致。"
        log_error "=========================================="
        echo ""
        log_info "修复建议："
        log_info "1. 检查是否有未应用的Flyway迁移脚本"
        log_info "2. 运行: mvn flyway:migrate"
        log_info "3. 如果是新增Entity字段，请创建对应的迁移脚本"
        echo ""
        exit 1
    fi
    
    log_success "=========================================="
    log_success "  所有 $total_tables 个表 Schema验证通过！"
    log_success "=========================================="
    echo ""
    
    exit 0
}

main "$@"