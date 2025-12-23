#!/bin/bash
# Input: Shell、mkdir、rm、docker
# Output: 运维脚本逻辑
# Pos: 后端运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# 使用 Docker 生成密钥库（无需本地 Java 环境）

set -e

KEYSTORE_DIR="${KEYSTORE_DIR:-./keystore}"
KEYSTORE_FILE="${KEYSTORE_FILE:-signature.p12}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
CERT_ALIAS="${CERT_ALIAS:-signing-cert}"
VALIDITY_DAYS="${VALIDITY_DAYS:-3650}"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        log_info "安装方法: https://docs.docker.com/get-docker/"
        exit 1
    fi
    log_info "Docker 已找到"
}

# 生成密钥库
generate_keystore() {
    log_info "开始使用 Docker 生成密钥库..."
    
    # 创建目录
    mkdir -p "$KEYSTORE_DIR"
    KEYSTORE_PATH="$KEYSTORE_DIR/$KEYSTORE_FILE"
    
    # 如果密钥库已存在，询问是否覆盖
    if [ -f "$KEYSTORE_PATH" ]; then
        log_warn "密钥库已存在: $KEYSTORE_PATH"
        read -p "是否覆盖？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "取消操作"
            exit 0
        fi
        rm -f "$KEYSTORE_PATH"
    fi
    
    # 获取绝对路径
    ABS_KEYSTORE_DIR=$(cd "$(dirname "$KEYSTORE_PATH")" && pwd)
    ABS_KEYSTORE_FILE=$(basename "$KEYSTORE_PATH")
    
    log_info "使用 OpenJDK Docker 镜像生成密钥库..."
    
    # 使用 Docker 运行 keytool
    docker run --rm \
        -v "$ABS_KEYSTORE_DIR:/keystore" \
        openjdk:17-jdk-slim \
        keytool -genkeypair \
            -alias "$CERT_ALIAS" \
            -keyalg RSA \
            -keysize 2048 \
            -sigalg SHA256withRSA \
            -validity "$VALIDITY_DAYS" \
            -keystore "/keystore/$ABS_KEYSTORE_FILE" \
            -storetype PKCS12 \
            -storepass "$KEYSTORE_PASSWORD" \
            -keypass "$KEYSTORE_PASSWORD" \
            -dname "CN=NexusArchive Signing Certificate, OU=IT Department, O=NexusArchive, L=Beijing, ST=Beijing, C=CN" \
            -ext "KeyUsage=digitalSignature,nonRepudiation" \
            -ext "ExtendedKeyUsage=codeSigning,emailProtection"
    
    if [ $? -eq 0 ]; then
        log_info "密钥库生成成功: $KEYSTORE_PATH"
        
        # 显示证书信息
        log_info "证书信息:"
        docker run --rm \
            -v "$ABS_KEYSTORE_DIR:/keystore" \
            openjdk:17-jdk-slim \
            keytool -list -v \
                -keystore "/keystore/$ABS_KEYSTORE_FILE" \
                -storepass "$KEYSTORE_PASSWORD" \
                -alias "$CERT_ALIAS" | grep -E "(Alias|Valid from|Certificate fingerprints)"
        
        # 显示配置提示
        echo ""
        log_info "配置提示:"
        echo "  在 application.yml 中添加:"
        echo "  signature:"
        echo "    keystore:"
        echo "      path: $KEYSTORE_PATH"
        echo "      password: $KEYSTORE_PASSWORD"
    else
        log_error "密钥库生成失败"
        exit 1
    fi
}

# 主函数
main() {
    log_info "=== 密钥库生成脚本（Docker 版本） ==="
    echo ""
    
    check_docker
    echo ""
    
    generate_keystore
    echo ""
    
    log_info "密钥库生成完成！"
    log_info "文件位置: $KEYSTORE_DIR/$KEYSTORE_FILE"
    log_info "证书别名: $CERT_ALIAS"
    log_info "有效期: $VALIDITY_DAYS 天"
}

# 如果直接执行脚本
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi











