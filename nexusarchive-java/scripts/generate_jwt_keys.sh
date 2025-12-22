#!/bin/bash
# Input: Shell、mkdir、rm、openssl
# Output: 运维脚本逻辑
# Pos: 后端运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# 生成 JWT RS256 密钥对脚本
# 用于 JWT 鉴权功能

set -e

# 密钥输出目录（应用 classpath 路径）
KEYSTORE_DIR="${KEYSTORE_DIR:-./src/main/resources/keystore}"
PRIVATE_KEY="$KEYSTORE_DIR/jwt_private.pem"
PUBLIC_KEY="$KEYSTORE_DIR/jwt_public.pem"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查 openssl 是否可用
check_openssl() {
    if ! command -v openssl &> /dev/null; then
        log_error "openssl 未找到，请安装 OpenSSL"
        exit 1
    fi
    log_info "openssl 已找到: $(which openssl)"
}

# 生成密钥对
generate_keys() {
    log_info "开始生成 JWT 密钥对..."
    
    # 创建目录
    mkdir -p "$KEYSTORE_DIR"
    
    # 如果密钥已存在，询问是否覆盖
    if [ -f "$PRIVATE_KEY" ] || [ -f "$PUBLIC_KEY" ]; then
        log_warn "密钥文件已存在"
        read -p "是否覆盖？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "取消操作"
            exit 0
        fi
        rm -f "$PRIVATE_KEY" "$PUBLIC_KEY"
    fi
    
    # 生成 2048 位 RSA 私钥 (PKCS#8 格式)
    log_info "生成 RSA 2048 位私钥 (PKCS#8 格式)..."
    openssl genpkey -algorithm RSA -out "$PRIVATE_KEY" -pkeyopt rsa_keygen_bits:2048
    
    # 设置私钥权限为仅所有者可读
    chmod 600 "$PRIVATE_KEY"
    
    # 从私钥导出公钥
    log_info "导出公钥..."
    openssl rsa -pubout -in "$PRIVATE_KEY" -out "$PUBLIC_KEY"
    
    log_info "密钥对生成成功！"
    echo ""
    log_info "私钥位置: $PRIVATE_KEY"
    log_info "公钥位置: $PUBLIC_KEY"
    echo ""
    log_info "配置 .env 文件:"
    echo "  JWT_PUBLIC_KEY_LOCATION=classpath:keystore/jwt_public.pem"
    echo "  JWT_PRIVATE_KEY_LOCATION=classpath:keystore/jwt_private.pem"
    echo ""
    log_warn "注意：私钥文件不应提交到版本控制！"
}

# 主函数
main() {
    echo ""
    log_info "=== JWT RS256 密钥生成脚本 ==="
    echo ""
    
    check_openssl
    echo ""
    
    generate_keys
    echo ""
    
    log_info "完成！"
}

# 如果直接执行脚本
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi