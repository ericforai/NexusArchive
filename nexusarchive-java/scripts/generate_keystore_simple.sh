#!/bin/bash
# 简化版密钥库生成脚本（交互式）

set -e

KEYSTORE_DIR="./keystore"
KEYSTORE_FILE="signature.p12"
KEYSTORE_PATH="$KEYSTORE_DIR/$KEYSTORE_FILE"

echo "=== 密钥库生成工具（简化版） ==="
echo ""

# 检查 keytool
if ! command -v keytool &> /dev/null; then
    echo "❌ 错误: keytool 未找到"
    echo ""
    echo "请确保已安装 Java JDK："
    echo "  - macOS: brew install openjdk"
    echo "  - Linux: sudo apt-get install openjdk-17-jdk"
    echo "  - 或设置 JAVA_HOME 环境变量"
    exit 1
fi

echo "✅ keytool 已找到"
echo ""

# 创建目录
mkdir -p "$KEYSTORE_DIR"

# 如果密钥库已存在，询问是否覆盖
if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  密钥库已存在: $KEYSTORE_PATH"
    read -p "是否覆盖？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "操作已取消"
        exit 0
    fi
    rm -f "$KEYSTORE_PATH"
fi

echo "开始生成密钥库..."
echo ""
echo "请按提示输入信息："
echo "  - 密钥库密码: 建议使用 'changeit'（开发环境）或强密码（生产环境）"
echo "  - 私钥密码: 与密钥库密码相同（直接回车）"
echo "  - 姓名: NexusArchive Signing Certificate"
echo "  - 组织单位: IT Department"
echo "  - 组织: NexusArchive"
echo "  - 城市: Beijing"
echo "  - 省份: Beijing"
echo "  - 国家代码: CN"
echo ""

# 生成密钥库（交互式）
keytool -genkeypair \
  -alias signing-cert \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 3650 \
  -keystore "$KEYSTORE_PATH" \
  -storetype PKCS12

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 密钥库生成成功！"
    echo ""
    echo "文件位置: $KEYSTORE_PATH"
    echo ""
    echo "配置 application.yml:"
    echo "  signature:"
    echo "    keystore:"
    echo "      path: $KEYSTORE_PATH"
    echo "      password: [您输入的密码]"
    echo ""
    echo "查看证书信息:"
    echo "  keytool -list -v -keystore $KEYSTORE_PATH -storepass [您的密码]"
else
    echo ""
    echo "❌ 密钥库生成失败"
    exit 1
fi




