#!/bin/bash
# 生成测试环境 JWT 密钥对
# 用于 CI/CD 环境

set -e

KEYSTORE_DIR="nexusarchive-java/src/main/resources/keystore"
mkdir -p "$KEYSTORE_DIR"

echo "[test-jwt-keys] 生成测试用 JWT 密钥对..."

# 生成私钥
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYSTORE_DIR/jwt_private.pem"

# 从私钥提取公钥
openssl rsa -pubout -in "$KEYSTORE_DIR/jwt_private.pem" -out "$KEYSTORE_DIR/jwt_public.pem"

echo "[test-jwt-keys] ✅ 测试 JWT 密钥对已生成"
echo "[test-jwt-keys] - 私钥: $KEYSTORE_DIR/jwt_private.pem"
echo "[test-jwt-keys] - 公钥: $KEYSTORE_DIR/jwt_public.pem"
