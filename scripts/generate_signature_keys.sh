#!/bin/bash
# 生成 SM2 签章证书 (用于四性检测签章校验)
# 依赖: openssl 1.1.1+ (支持 SM2)

set -e

OUTPUT_DIR="${1:-./certs}"
PASSWORD="${2:-changeit}"
ALIAS="${3:-nexusarchive}"

mkdir -p "$OUTPUT_DIR"

echo "=== 生成 SM2 签章证书 ==="
echo "输出目录: $OUTPUT_DIR"
echo "证书别名: $ALIAS"

# 检查 OpenSSL SM2 支持
if ! openssl ecparam -list_curves | grep -q "SM2"; then
    echo "警告: 当前 OpenSSL 不支持 SM2 曲线"
    echo "将使用 secp256k1 作为替代 (仅开发环境)"
    CURVE="secp256k1"
else
    CURVE="SM2"
fi

# 1. 生成私钥
openssl ecparam -name "$CURVE" -genkey -noout \
    -out "$OUTPUT_DIR/private.pem"

# 2. 生成自签名证书
openssl req -new -x509 -key "$OUTPUT_DIR/private.pem" \
    -out "$OUTPUT_DIR/certificate.pem" \
    -days 3650 \
    -subj "/CN=NexusArchive Signature/O=Company/C=CN"

# 3. 打包为 PKCS12
openssl pkcs12 -export \
    -in "$OUTPUT_DIR/certificate.pem" \
    -inkey "$OUTPUT_DIR/private.pem" \
    -out "$OUTPUT_DIR/sm2.p12" \
    -name "$ALIAS" \
    -password "pass:$PASSWORD"

# 清理临时文件
rm -f "$OUTPUT_DIR/private.pem" "$OUTPUT_DIR/certificate.pem"

echo ""
echo "=== 生成完成 ==="
echo "证书文件: $OUTPUT_DIR/sm2.p12"
echo "证书密码: $PASSWORD"
echo ""
echo "配置方式:"
echo "  export SIGNATURE_KEYSTORE_PATH=$OUTPUT_DIR/sm2.p12"
echo "  export SIGNATURE_KEYSTORE_PASSWORD=$PASSWORD"
