#!/bin/bash
# Input: Shell、mkdir、rm、cp
# Output: 构建/打包流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#====================================================================
# NexusArchive 热补丁包构建脚本
# 用于构建仅包含变更文件的轻量补丁包
#====================================================================
set -e

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
BASE_VERSION="2.0.0"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/deploy/patches"

log_info() { echo -e "${GREEN}[PATCH]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

#====================================================================
# 辅助函数
#====================================================================

# 获取下一个补丁版本号
get_next_patch_version() {
    local patch_num=1
    while [[ -f "${OUTPUT_DIR}/nexusarchive-patch-${BASE_VERSION}-p${patch_num}.tar.gz" ]]; do
        ((patch_num++))
    done
    echo "p${patch_num}"
}

# 计算文件哈希 (优先使用 SM3，回退到 SHA256)
calc_hash() {
    local file=$1
    if command -v sm3sum &> /dev/null; then
        sm3sum "$file" | awk '{print $1}'
    else
        shasum -a 256 "$file" | awk '{print $1}'
    fi
}

# 显示使用帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -b, --base VERSION    指定基础版本号 (默认: ${BASE_VERSION})"
    echo "  -c, --commit HASH     指定基准 commit (默认: 上一个 tag)"
    echo "  -f, --files FILE,...  手动指定要包含的文件 (逗号分隔)"
    echo "  -m, --message MSG     补丁说明信息"
    echo "  -h, --help            显示此帮助"
    echo ""
    echo "示例:"
    echo "  $0                           # 自动检测变更"
    echo "  $0 -c abc123                 # 从指定 commit 开始"
    echo "  $0 -f src/main/java/A.java   # 手动指定文件"
}

#====================================================================
# 参数解析
#====================================================================

BASE_COMMIT=""
MANUAL_FILES=""
PATCH_MESSAGE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--base) BASE_VERSION="$2"; shift 2 ;;
        -c|--commit) BASE_COMMIT="$2"; shift 2 ;;
        -f|--files) MANUAL_FILES="$2"; shift 2 ;;
        -m|--message) PATCH_MESSAGE="$2"; shift 2 ;;
        -h|--help) show_help; exit 0 ;;
        *) log_error "未知选项: $1"; show_help; exit 1 ;;
    esac
done

#====================================================================
# 构建流程
#====================================================================

detect_changes() {
    log_step "检测代码变更..."
    
    if [[ -n "$MANUAL_FILES" ]]; then
        # 手动指定文件
        echo "$MANUAL_FILES" | tr ',' '\n'
        return
    fi
    
    cd "$PROJECT_ROOT"
    
    # 确定基准点
    if [[ -z "$BASE_COMMIT" ]]; then
        # 使用最近的 tag
        BASE_COMMIT=$(git describe --tags --abbrev=0 2>/dev/null || echo "HEAD~10")
    fi
    
    log_info "基准 commit: $BASE_COMMIT"
    
    # 获取变更文件
    git diff --name-only "$BASE_COMMIT" HEAD -- \
        'nexusarchive-java/src/**/*.java' \
        'src/**/*.ts' \
        'src/**/*.tsx' \
        'src/**/*.css' \
        '*.sql'
}

build_patch() {
    local patch_version=$(get_next_patch_version)
    local patch_name="nexusarchive-patch-${BASE_VERSION}-${patch_version}"
    local temp_dir="${OUTPUT_DIR}/${patch_name}"
    
    log_info "开始构建补丁: ${patch_name}"
    
    mkdir -p "$temp_dir"/{backend,frontend,db,manifest}
    
    # 收集变更文件
    local changed_files=$(detect_changes)
    
    if [[ -z "$changed_files" ]]; then
        log_warn "未检测到变更文件，退出"
        rm -rf "$temp_dir"
        exit 0
    fi
    
    echo "$changed_files" | head -20
    local file_count=$(echo "$changed_files" | wc -l | tr -d ' ')
    log_info "共检测到 ${file_count} 个变更文件"
    
    # 分类处理文件
    local backend_changed=false
    local frontend_changed=false
    local db_changed=false
    
    while IFS= read -r file; do
        if [[ "$file" == nexusarchive-java/* ]]; then
            backend_changed=true
        elif [[ "$file" == src/* ]]; then
            frontend_changed=true
        elif [[ "$file" == *.sql ]]; then
            db_changed=true
            cp "$PROJECT_ROOT/$file" "$temp_dir/db/" 2>/dev/null || true
        fi
    done <<< "$changed_files"
    
    # 构建后端 (如果有变更)
    if [[ "$backend_changed" == true ]]; then
        log_step "构建后端 JAR..."
        cd "${PROJECT_ROOT}/nexusarchive-java"
        mvn clean package -DskipTests -q
        cp target/nexusarchive-backend-*.jar "$temp_dir/backend/"
    fi
    
    # 构建前端 (如果有变更)
    if [[ "$frontend_changed" == true ]]; then
        log_step "构建前端资源..."
        cd "$PROJECT_ROOT"
        npm run build --silent 2>/dev/null || npm run build
        cp -r dist/* "$temp_dir/frontend/"
    fi
    
    # 生成 manifest
    log_step "生成清单文件..."
    cat > "$temp_dir/manifest/patch.json" << EOF
{
    "patch_version": "${patch_version}",
    "base_version": "${BASE_VERSION}",
    "build_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "build_host": "$(hostname)",
    "message": "${PATCH_MESSAGE:-热补丁}",
    "contains": {
        "backend": ${backend_changed},
        "frontend": ${frontend_changed},
        "database": ${db_changed}
    },
    "files_changed": ${file_count}
}
EOF

    # 生成应用脚本
    cat > "$temp_dir/apply.sh" << 'APPLY_SCRIPT'
#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${1:-/opt/nexusarchive}"

GREEN='\033[0;32m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 热补丁应用程序                          ║"
echo "╚══════════════════════════════════════════════════════════╝"

# 读取 manifest
if [[ -f "$SCRIPT_DIR/manifest/patch.json" ]]; then
    echo "补丁版本: $(grep -o '"patch_version"[^,]*' "$SCRIPT_DIR/manifest/patch.json" | cut -d'"' -f4)"
fi

# 备份
BACKUP_DIR="${INSTALL_DIR}_patch_backup_$(date +%Y%m%d_%H%M%S)"
echo -e "${GREEN}[1/4]${NC} 备份当前版本到 $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"
cp "$INSTALL_DIR/app.jar" "$BACKUP_DIR/" 2>/dev/null || true
cp -r "$INSTALL_DIR/frontend" "$BACKUP_DIR/" 2>/dev/null || true

# 应用后端
if [[ -f "$SCRIPT_DIR/backend/"*.jar ]]; then
    echo -e "${GREEN}[2/4]${NC} 应用后端补丁..."
    systemctl stop nexusarchive 2>/dev/null || true
    cp "$SCRIPT_DIR/backend/"*.jar "$INSTALL_DIR/app.jar"
fi

# 应用前端
if [[ -d "$SCRIPT_DIR/frontend" ]] && [[ "$(ls -A "$SCRIPT_DIR/frontend")" ]]; then
    echo -e "${GREEN}[3/4]${NC} 应用前端补丁..."
    rm -rf "$INSTALL_DIR/frontend"
    cp -r "$SCRIPT_DIR/frontend" "$INSTALL_DIR/"
fi

# 重启服务
echo -e "${GREEN}[4/4]${NC} 重启服务..."
systemctl start nexusarchive 2>/dev/null || true
systemctl reload nginx 2>/dev/null || true

echo ""
echo "✅ 补丁应用完成!"
echo "回滚命令: cp $BACKUP_DIR/* $INSTALL_DIR/"
APPLY_SCRIPT
    chmod +x "$temp_dir/apply.sh"
    
    # 打包
    log_step "打包补丁..."
    cd "$OUTPUT_DIR"
    tar -czf "${patch_name}.tar.gz" -C "$temp_dir" .
    rm -rf "$temp_dir"
    
    local patch_size=$(du -h "${OUTPUT_DIR}/${patch_name}.tar.gz" | awk '{print $1}')
    
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     ✅ 热补丁构建完成                                    ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║     输出文件: ${OUTPUT_DIR}/${patch_name}.tar.gz"
    echo "║     文件大小: ${patch_size}"
    echo "║                                                          ║"
    echo "║     使用方法:                                            ║"
    echo "║     1. 上传到客户服务器                                  ║"
    echo "║     2. 解压: tar -xzf ${patch_name}.tar.gz               ║"
    echo "║     3. 运行: sudo ./apply.sh                             ║"
    echo "╚══════════════════════════════════════════════════════════╝"
}

#====================================================================
# 主流程
#====================================================================

main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     NexusArchive 热补丁构建工具                          ║"
    echo "║     基础版本: ${BASE_VERSION}                                ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    
    mkdir -p "$OUTPUT_DIR"
    build_patch
}

main "$@"