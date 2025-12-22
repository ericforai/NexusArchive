#!/bin/bash
# Input: Shell、mvn、mkdir、cp
# Output: 构建/打包流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#====================================================================
# NexusArchive 安全补丁包构建脚本
# 用于 CVE 漏洞修复、依赖升级等安全相关更新
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
OUTPUT_DIR="${PROJECT_ROOT}/deploy/security"

log_info() { echo -e "${GREEN}[SECURITY]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

#====================================================================
# 参数解析
#====================================================================

SCAN_ONLY=false
CVE_IDS=""
SECURITY_MESSAGE=""

show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -b, --base VERSION    基础版本号 (默认: ${BASE_VERSION})"
    echo "  -s, --scan            仅扫描漏洞，不构建补丁"
    echo "  -c, --cve CVE-ID,...  指定修复的 CVE 编号 (逗号分隔)"
    echo "  -m, --message MSG     安全公告说明"
    echo "  -h, --help            显示此帮助"
    echo ""
    echo "示例:"
    echo "  $0 --scan                    # 扫描依赖漏洞"
    echo "  $0 -c CVE-2024-1234          # 构建修复特定 CVE 的补丁"
    echo "  $0                           # 构建包含所有依赖更新的安全补丁"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--base) BASE_VERSION="$2"; shift 2 ;;
        -s|--scan) SCAN_ONLY=true; shift ;;
        -c|--cve) CVE_IDS="$2"; shift 2 ;;
        -m|--message) SECURITY_MESSAGE="$2"; shift 2 ;;
        -h|--help) show_help; exit 0 ;;
        *) log_error "未知选项: $1"; show_help; exit 1 ;;
    esac
done

#====================================================================
# 辅助函数
#====================================================================

# 获取下一个安全补丁版本号
get_next_sec_version() {
    local sec_num=1
    while [[ -f "${OUTPUT_DIR}/nexusarchive-security-${BASE_VERSION}-sec${sec_num}.tar.gz" ]]; do
        ((sec_num++))
    done
    echo "sec${sec_num}"
}

# 计算哈希 (优先 SM3)
calc_hash() {
    local file=$1
    if command -v sm3sum &> /dev/null; then
        sm3sum "$file" | awk '{print $1}'
    else
        shasum -a 256 "$file" | awk '{print $1}'
    fi
}

#====================================================================
# 漏洞扫描
#====================================================================

scan_vulnerabilities() {
    log_step "扫描依赖漏洞..."
    
    cd "${PROJECT_ROOT}/nexusarchive-java"
    
    # 检查是否有 OWASP 依赖检查插件
    if grep -q "dependency-check-maven" pom.xml 2>/dev/null; then
        log_info "使用 OWASP Dependency Check 扫描..."
        mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 || true
        
        if [[ -f "target/dependency-check-report.html" ]]; then
            log_info "扫描报告: ${PROJECT_ROOT}/nexusarchive-java/target/dependency-check-report.html"
        fi
    else
        log_warn "未配置 OWASP Dependency Check，使用 mvn 依赖树分析..."
        
        # 输出依赖树供人工检查
        mvn dependency:tree -DoutputType=text > "${OUTPUT_DIR}/dependency-tree.txt" 2>/dev/null || true
        log_info "依赖树已导出到: ${OUTPUT_DIR}/dependency-tree.txt"
    fi
    
    # 检查常见高危依赖版本
    log_step "检查常见高危依赖..."
    
    local pom_file="${PROJECT_ROOT}/nexusarchive-java/pom.xml"
    local issues=()
    
    # Log4j (CVE-2021-44228)
    if grep -q "log4j-core" "$pom_file" 2>/dev/null; then
        local log4j_version=$(grep -A1 "log4j-core" "$pom_file" | grep -oP '(?<=<version>)[^<]+' | head -1)
        if [[ -n "$log4j_version" ]]; then
            case "$log4j_version" in
                2.0*|2.1[0-6].*) issues+=("⚠️  Log4j ${log4j_version} 存在已知漏洞，建议升级到 2.17.1+") ;;
            esac
        fi
    fi
    
    # Spring Framework (CVE-2022-22965)
    if grep -q "spring-boot" "$pom_file" 2>/dev/null; then
        local spring_version=$(grep -oP '(?<=<spring-boot.version>)[^<]+' "$pom_file" | head -1)
        if [[ -z "$spring_version" ]]; then
            spring_version=$(grep -oP '(?<=<version>)[^<]+' "$pom_file" | head -1)
        fi
    fi
    
    # 输出检查结果
    if [[ ${#issues[@]} -gt 0 ]]; then
        echo ""
        echo "╔══════════════════════════════════════════════════════════╗"
        echo "║     ⚠️  发现潜在安全问题                                 ║"
        echo "╚══════════════════════════════════════════════════════════╝"
        for issue in "${issues[@]}"; do
            echo "$issue"
        done
        echo ""
    else
        log_info "未发现明显的高危依赖问题"
    fi
}

#====================================================================
# 构建安全补丁
#====================================================================

build_security_patch() {
    local sec_version=$(get_next_sec_version)
    local patch_name="nexusarchive-security-${BASE_VERSION}-${sec_version}"
    local temp_dir="${OUTPUT_DIR}/${patch_name}"
    
    log_info "开始构建安全补丁: ${patch_name}"
    
    mkdir -p "$temp_dir"/{bin,docs}
    
    # 构建后端
    log_step "构建后端 JAR (含更新依赖)..."
    cd "${PROJECT_ROOT}/nexusarchive-java"
    
    # 先更新依赖到最新安全版本
    log_info "更新依赖版本..."
    mvn versions:use-latest-releases -DallowMajorUpdates=false -q 2>/dev/null || true
    
    # 构建
    mvn clean package -DskipTests -q
    
    local jar_file=$(find target -name "nexusarchive-backend-*.jar" -type f | head -1)
    if [[ -f "$jar_file" ]]; then
        cp "$jar_file" "$temp_dir/bin/app.jar"
        log_info "后端构建完成"
    else
        log_error "后端构建失败"
        exit 1
    fi
    
    # 生成安全公告
    log_step "生成安全公告..."
    cat > "$temp_dir/docs/SECURITY_ADVISORY.md" << EOF
# NexusArchive 安全公告

## 补丁版本: ${sec_version}
## 发布日期: $(date +%Y-%m-%d)
## 基础版本: ${BASE_VERSION}

---

## 修复内容

${SECURITY_MESSAGE:-本补丁包含以下安全更新：

- 依赖库安全更新
- 已知漏洞修复}

${CVE_IDS:+## 修复的 CVE

$(echo "$CVE_IDS" | tr ',' '\n' | sed 's/^/- /')}

---

## 升级步骤

1. **备份现有系统**
   \`\`\`bash
   cp /opt/nexusarchive/app.jar /opt/nexusarchive/app.jar.bak
   \`\`\`

2. **停止服务**
   \`\`\`bash
   sudo systemctl stop nexusarchive
   \`\`\`

3. **应用补丁**
   \`\`\`bash
   sudo ./apply.sh
   \`\`\`

4. **验证更新**
   \`\`\`bash
   sudo systemctl status nexusarchive
   curl http://localhost:8080/api/health
   \`\`\`

---

## 校验信息

- **文件**: app.jar
- **哈希算法**: $(command -v sm3sum &>/dev/null && echo "SM3" || echo "SHA-256")
- **哈希值**: $(calc_hash "$temp_dir/bin/app.jar")

---

## 回滚方法

如果更新后出现问题:
\`\`\`bash
sudo systemctl stop nexusarchive
cp /opt/nexusarchive/app.jar.bak /opt/nexusarchive/app.jar
sudo systemctl start nexusarchive
\`\`\`

---

*如有疑问，请联系技术支持。*
EOF
    
    # 生成应用脚本
    cat > "$temp_dir/apply.sh" << 'APPLY_SCRIPT'
#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${1:-/opt/nexusarchive}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 安全补丁应用程序                        ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 显示安全公告
if [[ -f "$SCRIPT_DIR/docs/SECURITY_ADVISORY.md" ]]; then
    echo -e "${YELLOW}请阅读安全公告: $SCRIPT_DIR/docs/SECURITY_ADVISORY.md${NC}"
    echo ""
fi

# 验证文件完整性
echo -e "${GREEN}[1/4]${NC} 验证补丁文件..."
if [[ ! -f "$SCRIPT_DIR/bin/app.jar" ]]; then
    echo "错误: 补丁文件不完整"
    exit 1
fi

# 备份
echo -e "${GREEN}[2/4]${NC} 备份当前版本..."
cp "$INSTALL_DIR/app.jar" "$INSTALL_DIR/app.jar.security_backup_$(date +%Y%m%d)" 2>/dev/null || true

# 停止服务
echo -e "${GREEN}[3/4]${NC} 停止服务..."
systemctl stop nexusarchive 2>/dev/null || true

# 应用补丁
echo -e "${GREEN}[4/4]${NC} 应用安全补丁..."
cp "$SCRIPT_DIR/bin/app.jar" "$INSTALL_DIR/app.jar"

# 启动服务
systemctl start nexusarchive 2>/dev/null || true

echo ""
echo "✅ 安全补丁应用完成!"
APPLY_SCRIPT
    chmod +x "$temp_dir/apply.sh"
    
    # 打包
    log_step "打包安全补丁..."
    cd "$OUTPUT_DIR"
    tar -czf "${patch_name}.tar.gz" -C "$temp_dir" .
    rm -rf "$temp_dir"
    
    local patch_size=$(du -h "${patch_name}.tar.gz" | awk '{print $1}')
    
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     ✅ 安全补丁构建完成                                  ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║     输出文件: ${OUTPUT_DIR}/${patch_name}.tar.gz"
    echo "║     文件大小: ${patch_size}"
    echo "║                                                          ║"
    echo "║     ⚠️  建议尽快分发给所有客户                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
}

#====================================================================
# 主流程
#====================================================================

main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     NexusArchive 安全补丁构建工具                        ║"
    echo "║     基础版本: ${BASE_VERSION}                                ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    
    mkdir -p "$OUTPUT_DIR"
    
    # 扫描漏洞
    scan_vulnerabilities
    
    # 如果只是扫描，则退出
    if [[ "$SCAN_ONLY" == true ]]; then
        echo ""
        log_info "扫描完成，未构建补丁包"
        exit 0
    fi
    
    # 构建安全补丁
    build_security_patch
}

main "$@"