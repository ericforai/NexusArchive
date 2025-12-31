#!/bin/bash
# Input: keytool、证书文件
# Output: truststore.p12
# Pos: 后端运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

TRUSTSTORE_DIR="${TRUSTSTORE_DIR:-./keystore}"
TRUSTSTORE_FILE="${TRUSTSTORE_FILE:-truststore.p12}"
TRUSTSTORE_PASSWORD="${TRUSTSTORE_PASSWORD:-changeit}"
TRUSTED_CERTS="${TRUSTED_CERTS:-}"

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

check_keytool() {
    if ! command -v keytool &> /dev/null; then
        log_error "keytool 未找到，请确保已安装 Java JDK"
        exit 1
    fi
}

parse_cert_list() {
    if [[ -z "$TRUSTED_CERTS" ]]; then
        log_error "未指定 TRUSTED_CERTS，请使用逗号分隔的证书路径"
        exit 1
    fi
    IFS=',' read -r -a CERT_LIST <<< "$TRUSTED_CERTS"

    if [[ ${#CERT_LIST[@]} -eq 0 ]]; then
        log_error "TRUSTED_CERTS 解析为空"
        exit 1
    fi
}

prepare_truststore() {
    mkdir -p "$TRUSTSTORE_DIR"
    TRUSTSTORE_PATH="$TRUSTSTORE_DIR/$TRUSTSTORE_FILE"

    if [[ -f "$TRUSTSTORE_PATH" ]]; then
        log_warn "truststore 已存在: $TRUSTSTORE_PATH"
        read -p "是否覆盖？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "取消操作"
            exit 0
        fi
        rm -f "$TRUSTSTORE_PATH"
    fi
}

import_certificates() {
    local index=1
    for cert in "${CERT_LIST[@]}"; do
        cert=$(echo "$cert" | xargs)
        if [[ -z "$cert" ]]; then
            continue
        fi
        if [[ ! -f "$cert" ]]; then
            log_error "证书不存在: $cert"
            exit 1
        fi
        local alias="trusted-${index}"
        log_info "导入证书: $cert (alias=$alias)"
        keytool -importcert \
            -noprompt \
            -alias "$alias" \
            -file "$cert" \
            -keystore "$TRUSTSTORE_PATH" \
            -storetype PKCS12 \
            -storepass "$TRUSTSTORE_PASSWORD"
        index=$((index + 1))
    done
}

print_summary() {
    log_info "truststore 生成成功: $TRUSTSTORE_PATH"
    log_info "包含证书数量: $(keytool -list -keystore "$TRUSTSTORE_PATH" -storepass "$TRUSTSTORE_PASSWORD" | grep -c trusted-)"
    echo ""
    log_info "配置提示 (默认严格模式):"
    echo "  signature:"
    echo "    truststore:"
    echo "      path: $TRUSTSTORE_PATH"
    echo "      password: $TRUSTSTORE_PASSWORD"
    echo "  compliance:"
    echo "    strict-mode: true"
}

main() {
    log_info "=== truststore.p12 生成脚本 ==="
    check_keytool
    parse_cert_list
    prepare_truststore
    import_certificates
    print_summary
}

if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi
