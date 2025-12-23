#!/bin/bash
# Input: Shell、mkdir、tar、chmod
# Output: 安装流程
# Pos: 部署脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

#====================================================================
# NexusArchive 离线安装脚本 v2.0
# 适用于无外网环境的一键部署
#====================================================================
set -e

# --- 颜色定义 ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- 全局变量 ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_VERSION="2.0.0"
CONFIG_FILE="${SCRIPT_DIR}/install.conf"
LOG_FILE="/tmp/nexusarchive_install_$(date +%Y%m%d_%H%M%S).log"

# --- 默认配置 ---
DEFAULT_INSTALL_DIR="/opt/nexusarchive"
DEFAULT_DATA_DIR="/opt/nexusarchive/data"
DEFAULT_LOG_DIR="/var/log/nexusarchive"
DEFAULT_SERVER_PORT="8080"
DEFAULT_DB_TYPE="postgresql"
DEFAULT_DB_PORT="5432"

#====================================================================
# 辅助函数
#====================================================================

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_step() {
    echo -e "\n${BLUE}==>${NC} ${BLUE}$1${NC}" | tee -a "$LOG_FILE"
}

confirm() {
    local prompt="$1"
    local default="${2:-N}"
    local reply
    
    if [[ "$default" == "Y" ]]; then
        prompt="$prompt [Y/n]: "
    else
        prompt="$prompt [y/N]: "
    fi
    
    read -rp "$prompt" reply
    reply=${reply:-$default}
    [[ "$reply" =~ ^[Yy]$ ]]
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "此脚本必须以 root 用户运行"
        exit 1
    fi
}

#====================================================================
# 环境检测
#====================================================================

detect_os() {
    log_step "检测操作系统..."
    
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS_NAME="$NAME"
        OS_VERSION="$VERSION_ID"
        OS_ID="$ID"
    else
        OS_NAME="Unknown"
        OS_VERSION="Unknown"
        OS_ID="unknown"
    fi
    
    # 检测包管理器
    if command -v apt-get &> /dev/null; then
        PKG_MANAGER="apt"
    elif command -v yum &> /dev/null; then
        PKG_MANAGER="yum"
    elif command -v dnf &> /dev/null; then
        PKG_MANAGER="dnf"
    else
        PKG_MANAGER="unknown"
    fi
    
    # 检测 CPU 架构
    ARCH=$(uname -m)
    
    log_info "操作系统: $OS_NAME $OS_VERSION"
    log_info "CPU架构: $ARCH"
    log_info "包管理器: $PKG_MANAGER"
    
    # 信创系统检测
    if [[ "$OS_NAME" == *"Kylin"* ]] || [[ "$OS_NAME" == *"银河麒麟"* ]]; then
        log_info "🇨🇳 检测到信创环境: 麒麟操作系统"
        IS_XINCHUANG=true
    elif [[ "$OS_NAME" == *"UOS"* ]] || [[ "$OS_NAME" == *"统信"* ]]; then
        log_info "🇨🇳 检测到信创环境: 统信 UOS"
        IS_XINCHUANG=true
    else
        IS_XINCHUANG=false
    fi
}

check_disk_space() {
    log_step "检测磁盘空间..."
    
    local install_dir="${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}"
    local parent_dir=$(dirname "$install_dir")
    
    # 确保父目录存在
    mkdir -p "$parent_dir" 2>/dev/null || true
    
    local available_mb=$(df -m "$parent_dir" 2>/dev/null | awk 'NR==2 {print $4}')
    local required_mb=500  # 最低要求 500MB
    
    if [[ -z "$available_mb" ]]; then
        log_warn "无法检测磁盘空间，继续安装"
        return 0
    fi
    
    log_info "可用空间: ${available_mb}MB (需要至少 ${required_mb}MB)"
    
    if [[ "$available_mb" -lt "$required_mb" ]]; then
        log_error "磁盘空间不足！需要至少 ${required_mb}MB"
        exit 1
    fi
}

check_java() {
    log_step "检测 Java 环境..."
    
    # 优先使用配置的 JAVA_HOME
    if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
    elif command -v java &> /dev/null; then
        JAVA_CMD=$(command -v java)
    else
        JAVA_CMD=""
    fi
    
    if [[ -z "$JAVA_CMD" ]]; then
        log_warn "未检测到 Java，尝试从离线包安装..."
        install_bundled_java
        return
    fi
    
    # 检查版本
    local java_version=$("$JAVA_CMD" -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    
    if [[ "$java_version" -lt 17 ]]; then
        log_error "Java 版本过低 (需要 17+，当前: $java_version)"
        log_info "尝试从离线包安装..."
        install_bundled_java
    else
        log_info "Java 版本: $java_version ✓"
    fi
}

install_bundled_java() {
    local jdk_archive="${SCRIPT_DIR}/deps/jdk-17_linux-${ARCH}.tar.gz"
    
    if [[ ! -f "$jdk_archive" ]]; then
        log_error "未找到离线 JDK 包: $jdk_archive"
        log_error "请手动安装 OpenJDK 17+ 或将 JDK 放入 deps/ 目录"
        exit 1
    fi
    
    log_info "正在安装离线 JDK..."
    mkdir -p /opt/java
    tar -xzf "$jdk_archive" -C /opt/java
    
    # 找到解压后的目录
    local jdk_dir=$(ls -d /opt/java/jdk-17* 2>/dev/null | head -n1)
    
    if [[ -z "$jdk_dir" ]]; then
        log_error "JDK 解压失败"
        exit 1
    fi
    
    # 设置环境变量
    export JAVA_HOME="$jdk_dir"
    export PATH="$JAVA_HOME/bin:$PATH"
    JAVA_CMD="$JAVA_HOME/bin/java"
    
    # 写入 profile
    echo "export JAVA_HOME=$JAVA_HOME" > /etc/profile.d/java.sh
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile.d/java.sh
    
    log_info "JDK 安装完成: $JAVA_HOME"
}

#====================================================================
# 配置向导
#====================================================================

run_config_wizard() {
    log_step "配置向导"
    
    # 如果配置文件存在，询问是否使用
    if [[ -f "$CONFIG_FILE" ]]; then
        log_info "检测到现有配置文件: $CONFIG_FILE"
        if confirm "是否使用现有配置?" "Y"; then
            source "$CONFIG_FILE"
            return 0
        fi
    fi
    
    echo ""
    echo "=========================================="
    echo "   NexusArchive 安装配置向导"
    echo "=========================================="
    echo ""
    
    # 安装目录
    read -rp "安装目录 [$DEFAULT_INSTALL_DIR]: " INSTALL_DIR
    INSTALL_DIR="${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}"
    
    # 数据目录
    read -rp "数据存储目录 [$DEFAULT_DATA_DIR]: " DATA_DIR
    DATA_DIR="${DATA_DIR:-$DEFAULT_DATA_DIR}"
    
    # 服务端口
    read -rp "服务端口 [$DEFAULT_SERVER_PORT]: " SERVER_PORT
    SERVER_PORT="${SERVER_PORT:-$DEFAULT_SERVER_PORT}"
    
    echo ""
    echo "--- 数据库配置 ---"
    
    # 数据库类型
    echo "数据库类型:"
    echo "  1) PostgreSQL"
    echo "  2) 达梦 (DM8)"
    echo "  3) 人大金仓 (Kingbase)"
    read -rp "选择 [1]: " db_choice
    case "$db_choice" in
        2) DB_TYPE="dameng"; DEFAULT_DB_PORT="5236" ;;
        3) DB_TYPE="kingbase"; DEFAULT_DB_PORT="54321" ;;
        *) DB_TYPE="postgresql"; DEFAULT_DB_PORT="5432" ;;
    esac
    
    read -rp "数据库主机 [127.0.0.1]: " DB_HOST
    DB_HOST="${DB_HOST:-127.0.0.1}"
    
    read -rp "数据库端口 [$DEFAULT_DB_PORT]: " DB_PORT
    DB_PORT="${DB_PORT:-$DEFAULT_DB_PORT}"
    
    read -rp "数据库名称 [nexusarchive]: " DB_NAME
    DB_NAME="${DB_NAME:-nexusarchive}"
    
    read -rp "数据库用户名 [nexus]: " DB_USER
    DB_USER="${DB_USER:-nexus}"
    
    while true; do
        read -rsp "数据库密码 (必填): " DB_PASSWORD
        echo ""
        if [[ -n "$DB_PASSWORD" ]]; then
            break
        fi
        log_warn "密码不能为空，请重新输入"
    done
    
    # JWT 密钥
    log_info "生成 JWT 密钥..."
    JWT_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
    
    # 保存配置
    save_config
    
    echo ""
    log_info "配置已保存到 $CONFIG_FILE"
}

save_config() {
    cat > "$CONFIG_FILE" << EOF
# NexusArchive 安装配置
# 生成时间: $(date)

# 目录配置
INSTALL_DIR=$INSTALL_DIR
DATA_DIR=$DATA_DIR
LOG_DIR=$DEFAULT_LOG_DIR

# 服务配置
SERVER_PORT=$SERVER_PORT

# 数据库配置
DB_TYPE=$DB_TYPE
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD

# JWT 密钥
JWT_SECRET=$JWT_SECRET

# Java 路径 (留空则自动检测)
JAVA_HOME=
EOF
    chmod 600 "$CONFIG_FILE"
}

#====================================================================
# 安装过程
#====================================================================

create_directories() {
    log_step "创建目录结构..."
    
    mkdir -p "$INSTALL_DIR"/{bin,config,logs}
    mkdir -p "$DATA_DIR"/{archives,temp}
    mkdir -p "$DEFAULT_LOG_DIR"
    
    log_info "目录创建完成"
}

create_user() {
    log_step "创建服务用户..."
    
    if id "nexus" &>/dev/null; then
        log_info "用户 'nexus' 已存在"
    else
        useradd -r -s /sbin/nologin -d "$INSTALL_DIR" nexus
        log_info "创建用户 'nexus'"
    fi
}

deploy_application() {
    log_step "部署应用程序..."
    
    # 复制 JAR
    local jar_file="${SCRIPT_DIR}/bin/nexusarchive-backend-${APP_VERSION}.jar"
    if [[ -f "$jar_file" ]]; then
        cp "$jar_file" "$INSTALL_DIR/app.jar"
        log_info "后端应用已部署"
    else
        log_error "未找到后端 JAR 文件: $jar_file"
        exit 1
    fi
    
    # 复制前端
    if [[ -d "${SCRIPT_DIR}/frontend" ]]; then
        cp -r "${SCRIPT_DIR}/frontend" "$INSTALL_DIR/"
        log_info "前端资源已部署"
    else
        log_warn "未找到前端资源目录"
    fi
}

#====================================================================
# 参数解析
#====================================================================

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --check-only    仅执行环境基线检测"
    echo "  --help          显示此帮助信息"
    echo ""
}

# 默认模式
MODE="install"

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --check-only)
                MODE="check"
                shift
                ;;
            --help)
                usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
        shift
    done
}

#====================================================================
# 主流程
#====================================================================

show_banner() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║                                                          ║"
    echo "║     NexusArchive 电子会计档案管理系统                    ║"
    echo "║     离线安装程序 v${APP_VERSION}                                 ║"
    echo "║                                                          ║"
    echo "║     支持: PostgreSQL | 达梦 | 人大金仓                   ║"
    echo "║                                                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
}

show_completion() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║                                                          ║"
    echo "║     ✅ 安装完成!                                         ║"
    echo "║                                                          ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║                                                          ║"
    echo "║     访问地址: http://$(hostname -I | awk '{print $1}'):80        "
    echo "║     API 地址: http://127.0.0.1:${SERVER_PORT}/api                "
    echo "║                                                          ║"
    echo "║     默认管理员:                                          ║"
    echo "║       用户名: admin                                      ║"
    echo "║       密码: admin123                                     ║"
    echo "║                                                          ║"
    echo "║     管理命令:                                            ║"
    echo "║       启动: systemctl start nexusarchive                 ║"
    echo "║       停止: systemctl stop nexusarchive                  ║"
    echo "║       状态: systemctl status nexusarchive                ║"
    echo "║       日志: journalctl -u nexusarchive -f                ║"
    echo "║                                                          ║"
    echo "║     配置文件: $INSTALL_DIR/.env             "
    echo "║     安装日志: $LOG_FILE   "
    echo "║                                                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
}

create_env_file() {
    log_step "生成环境配置..."
    
    # 根据数据库类型选择基础 Profile
    case "$DB_TYPE" in
        dameng)   BASE_PROFILE="prod-dameng" ;;
        kingbase) BASE_PROFILE="prod-kingbase" ;;
        *)        BASE_PROFILE="prod" ;;
    esac

    # 如果外部已经设置了 SPRING_PROFILES_ACTIVE（例如 demo,prod），则优先保留
    # 否则使用基础 Profile
    if [[ -n "$SPRING_PROFILES_ACTIVE" ]]; then
        FINAL_PROFILE="$SPRING_PROFILES_ACTIVE"
        log_info "使用自定义 Profile: $FINAL_PROFILE"
    else
        FINAL_PROFILE="$BASE_PROFILE"
    fi
    
    cat > "$INSTALL_DIR/.env" << EOF
# NexusArchive 运行时配置
# 生成时间: $(date)

SPRING_PROFILES_ACTIVE=$FINAL_PROFILE
SERVER_PORT=$SERVER_PORT

# 数据库
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD

# JWT
JWT_SECRET=$JWT_SECRET

# 存储路径
ARCHIVE_ROOT_PATH=$DATA_DIR/archives
ARCHIVE_TEMP_PATH=$DATA_DIR/temp
EOF
    
    chmod 600 "$INSTALL_DIR/.env"
    chown nexus:nexus "$INSTALL_DIR/.env"
    log_info "环境配置已生成"
}


main() {
    # 解析参数
    parse_args "$@"

    show_banner
    
    check_root
    detect_os
    check_disk_space
    check_java
    
    # 如果是仅检测模式，在此退出
    if [[ "$MODE" == "check" ]]; then
        echo ""
        log_info "✅ 环境基线检测通过"
        exit 0
    fi

    run_config_wizard
    
    # 加载配置
    source "$CONFIG_FILE"
    
    create_directories
    create_user
    deploy_application
    create_env_file
    install_systemd_service
    configure_nginx
    set_permissions
    start_service
    
    show_completion
}

# 运行主函数
main "$@"