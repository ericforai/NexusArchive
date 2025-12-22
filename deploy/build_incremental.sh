#!/bin/bash
# Input: Shell、rm、mkdir、mvn
# Output: 构建/打包流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#====================================================================
# NexusArchive 增量更新包构建脚本
# 用于构建包含完整 JAR 和前端的增量包（不含 JDK 等大依赖）
#====================================================================
set -e

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
FROM_VERSION="2.0.0"
TO_VERSION="2.0.1"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/deploy/updates"

log_info() { echo -e "${GREEN}[BUILD]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

#====================================================================
# 参数解析
#====================================================================

show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -f, --from VERSION    起始版本号 (默认: ${FROM_VERSION})"
    echo "  -t, --to VERSION      目标版本号 (默认: ${TO_VERSION})"
    echo "  -h, --help            显示此帮助"
    echo ""
    echo "示例:"
    echo "  $0 -f 2.0.0 -t 2.0.1"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--from) FROM_VERSION="$2"; shift 2 ;;
        -t|--to) TO_VERSION="$2"; shift 2 ;;
        -h|--help) show_help; exit 0 ;;
        *) log_error "未知选项: $1"; show_help; exit 1 ;;
    esac
done

#====================================================================
# 构建函数
#====================================================================

clean_output() {
    log_step "清理输出目录..."
    local package_name="nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}"
    rm -rf "${OUTPUT_DIR}/${package_name}"
    rm -f "${OUTPUT_DIR}/${package_name}.tar.gz"
    mkdir -p "${OUTPUT_DIR}/${package_name}"/{bin,frontend,db/migration,docs}
}

build_backend() {
    log_step "构建后端 JAR..."
    cd "${PROJECT_ROOT}/nexusarchive-java"
    
    # 更新 pom.xml 版本号 (可选)
    # mvn versions:set -DnewVersion=${TO_VERSION} -q
    
    mvn clean package -DskipTests -q
    
    local jar_file="target/nexusarchive-backend-${FROM_VERSION}.jar"
    if [[ ! -f "$jar_file" ]]; then
        jar_file=$(find target -name "nexusarchive-backend-*.jar" -type f | head -1)
    fi
    
    if [[ -f "$jar_file" ]]; then
        cp "$jar_file" "${OUTPUT_DIR}/nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}/bin/app.jar"
        log_info "后端构建完成: $(du -h "$jar_file" | awk '{print $1}')"
    else
        log_error "后端构建失败"
        exit 1
    fi
}

build_frontend() {
    log_step "构建前端资源..."
    cd "${PROJECT_ROOT}"
    
    if [[ ! -d "node_modules" ]]; then
        log_info "安装前端依赖..."
        npm install --silent
    fi
    
    npm run build --silent 2>/dev/null || npm run build
    
    if [[ -d "dist" ]]; then
        cp -r dist/* "${OUTPUT_DIR}/nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}/frontend/"
        log_info "前端构建完成: $(du -sh dist | awk '{print $1}')"
    else
        log_error "前端构建失败"
        exit 1
    fi
}

copy_migrations() {
    log_step "复制数据库迁移脚本..."
    
    local migration_dir="${PROJECT_ROOT}/nexusarchive-java/src/main/resources/db/migration"
    local output_migration_dir="${OUTPUT_DIR}/nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}/db/migration"
    
    # 复制所有迁移脚本
    if [[ -d "$migration_dir" ]]; then
        cp "$migration_dir"/*.sql "$output_migration_dir/" 2>/dev/null || true
        local count=$(ls -1 "$output_migration_dir"/*.sql 2>/dev/null | wc -l | tr -d ' ')
        log_info "复制了 ${count} 个迁移脚本"
    fi
}

generate_changelog() {
    log_step "生成变更日志..."
    
    local changelog_file="${OUTPUT_DIR}/nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}/docs/CHANGELOG.md"
    
    cat > "$changelog_file" << EOF
# NexusArchive 版本更新日志

## ${TO_VERSION} ($(date +%Y-%m-%d))

### 更新内容

$(git log --oneline --since="1 week ago" 2>/dev/null | head -20 || echo "- 常规更新和 Bug 修复")

### 升级说明

1. 备份现有系统
2. 运行升级脚本: \`sudo ./upgrade.sh\`
3. 验证服务状态: \`systemctl status nexusarchive\`

### 回滚方法

如果升级后出现问题，可以通过以下命令回滚:
\`\`\`bash
systemctl stop nexusarchive
cp /opt/nexusarchive_backup_*/app.jar /opt/nexusarchive/app.jar
cp -r /opt/nexusarchive_backup_*/frontend /opt/nexusarchive/
systemctl start nexusarchive
\`\`\`
EOF
    
    log_info "变更日志已生成"
}

create_upgrade_script() {
    log_step "生成升级脚本..."
    
    local package_dir="${OUTPUT_DIR}/nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}"
    
    cat > "${package_dir}/upgrade.sh" << 'EOF'
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${1:-/opt/nexusarchive}"
BACKUP_DIR="${INSTALL_DIR}_backup_$(date +%Y%m%d_%H%M%S)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 增量更新程序                            ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 检查现有安装
if [[ ! -d "$INSTALL_DIR" ]]; then
    echo -e "${RED}错误: 未找到现有安装 ($INSTALL_DIR)${NC}"
    echo "请使用完整安装包进行首次安装"
    exit 1
fi

# 预检查
echo -e "${GREEN}[预检查]${NC} 验证环境..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: Java 未安装${NC}"
    exit 1
fi

# 检查磁盘空间 (至少需要 500MB)
available_space=$(df -m "$INSTALL_DIR" | awk 'NR==2 {print $4}')
if [[ "$available_space" -lt 500 ]]; then
    echo -e "${RED}错误: 磁盘空间不足 (需要 500MB, 可用 ${available_space}MB)${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}目标目录: $INSTALL_DIR${NC}"
echo -e "${YELLOW}备份目录: $BACKUP_DIR${NC}"
read -rp "确认开始升级? [y/N]: " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

# 步骤 1: 备份
echo ""
echo -e "${GREEN}[1/5]${NC} 备份当前版本..."
mkdir -p "$BACKUP_DIR"
cp "$INSTALL_DIR/app.jar" "$BACKUP_DIR/" 2>/dev/null || true
cp -r "$INSTALL_DIR/frontend" "$BACKUP_DIR/" 2>/dev/null || true
cp "$INSTALL_DIR/.env" "$BACKUP_DIR/" 2>/dev/null || true
echo "备份位置: $BACKUP_DIR"

# 步骤 2: 停止服务
echo -e "${GREEN}[2/5]${NC} 停止服务..."
systemctl stop nexusarchive 2>/dev/null || true

# 步骤 3: 更新后端
echo -e "${GREEN}[3/5]${NC} 更新后端..."
if [[ -f "$SCRIPT_DIR/bin/app.jar" ]]; then
    cp "$SCRIPT_DIR/bin/app.jar" "$INSTALL_DIR/app.jar"
fi

# 步骤 4: 更新前端
echo -e "${GREEN}[4/5]${NC} 更新前端..."
if [[ -d "$SCRIPT_DIR/frontend" ]]; then
    rm -rf "$INSTALL_DIR/frontend"
    cp -r "$SCRIPT_DIR/frontend" "$INSTALL_DIR/"
fi

# 步骤 5: 启动服务
echo -e "${GREEN}[5/5]${NC} 启动服务..."
systemctl start nexusarchive

# 重载 Nginx
systemctl reload nginx 2>/dev/null || true

# 验证
sleep 3
if systemctl is-active --quiet nexusarchive; then
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     ✅ 升级完成!                                         ║"
    echo "╚══════════════════════════════════════════════════════════╝"
else
    echo ""
    echo -e "${RED}警告: 服务启动可能失败，请检查日志${NC}"
    echo "查看日志: journalctl -u nexusarchive -f"
    echo ""
    echo "回滚命令:"
    echo "  systemctl stop nexusarchive"
    echo "  cp $BACKUP_DIR/app.jar $INSTALL_DIR/"
    echo "  cp -r $BACKUP_DIR/frontend $INSTALL_DIR/"
    echo "  systemctl start nexusarchive"
fi
EOF
    
    chmod +x "${package_dir}/upgrade.sh"
    log_info "升级脚本已生成"
}

package_all() {
    log_step "打包增量更新包..."
    
    local package_name="nexusarchive-update-${FROM_VERSION}-to-${TO_VERSION}"
    
    cd "$OUTPUT_DIR"
    export COPYFILE_DISABLE=1  # Mac: 禁止生成 ._ 文件
    tar -czf "${package_name}.tar.gz" -C "${package_name}" .
    
    local package_size=$(du -h "${package_name}.tar.gz" | awk '{print $1}')
    
    # 清理临时目录
    rm -rf "${package_name}"
    
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     ✅ 增量更新包构建完成                                ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║     输出文件: ${OUTPUT_DIR}/${package_name}.tar.gz"
    echo "║     文件大小: ${package_size}"
    echo "║                                                          ║"
    echo "║     分发说明:                                            ║"
    echo "║     1. 上传到客户服务器                                  ║"
    echo "║     2. 解压: tar -xzf ${package_name}.tar.gz             ║"
    echo "║     3. 运行: sudo ./upgrade.sh                           ║"
    echo "╚══════════════════════════════════════════════════════════╝"
}

#====================================================================
# 主流程
#====================================================================

main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     NexusArchive 增量更新包构建工具                      ║"
    echo "║     版本: ${FROM_VERSION} -> ${TO_VERSION}               ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    
    mkdir -p "$OUTPUT_DIR"
    
    clean_output
    build_backend
    build_frontend
    copy_migrations
    generate_changelog
    create_upgrade_script
    package_all
}

main "$@"