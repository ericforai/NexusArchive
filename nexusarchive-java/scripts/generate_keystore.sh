#!/bin/bash
# 生成 PKCS12 格式的密钥库脚本
# 用于电子签章功能

set -e

KEYSTORE_DIR="${KEYSTORE_DIR:-./keystore}"
KEYSTORE_FILE="${KEYSTORE_FILE:-signature.p12}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
CERT_ALIAS="${CERT_ALIAS:-signing-cert}"
VALIDITY_DAYS="${VALIDITY_DAYS:-3650}"  # 10年

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

# 检查 keytool 是否可用
check_keytool() {
    if ! command -v keytool &> /dev/null; then
        log_error "keytool 未找到，请确保已安装 Java JDK"
        exit 1
    fi
    log_info "keytool 已找到: $(which keytool)"
}

# 检查 BouncyCastle 是否可用（用于 SM2）
check_bouncycastle() {
    log_info "检查 BouncyCastle 支持..."
    # 注意：keytool 默认不支持 SM2，需要使用 BouncyCastle 提供者
    # 这里生成 RSA 证书作为示例，SM2 证书需要额外配置
    log_warn "注意：keytool 默认生成 RSA 证书，SM2 证书需要 BouncyCastle 支持"
}

# 生成密钥库
generate_keystore() {
    log_info "开始生成密钥库..."
    
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
    
    # 生成自签名证书（RSA 2048位）
    log_info "生成自签名证书（RSA 2048位）..."
    keytool -genkeypair \
        -alias "$CERT_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -sigalg SHA256withRSA \
        -validity "$VALIDITY_DAYS" \
        -keystore "$KEYSTORE_PATH" \
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
        keytool -list -v \
            -keystore "$KEYSTORE_PATH" \
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

# 生成 SM2 证书（需要 BouncyCastle）
generate_sm2_keystore() {
    log_warn "SM2 证书生成需要 BouncyCastle 提供者"
    log_warn "当前脚本仅生成 RSA 证书作为示例"
    log_warn "如需 SM2 证书，请使用 BouncyCastle 工具或联系证书颁发机构"
}

# 主函数
main() {
    log_info "=== 密钥库生成脚本 ==="
    echo ""
    
    check_keytool
    check_bouncycastle
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




