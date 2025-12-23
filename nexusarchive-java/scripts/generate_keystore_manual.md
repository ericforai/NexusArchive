# 密钥库手动生成步骤

## 📋 前提条件

- Java JDK 已安装（需要 keytool）
- 如果系统没有 Java，需要先安装

## 🚀 生成步骤

### 步骤 1: 创建目录

```bash
cd nexusarchive-java
mkdir -p keystore
```

### 步骤 2: 生成密钥库

**完整命令（非交互式）**：

```bash
keytool -genkeypair \
  -alias signing-cert \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 3650 \
  -keystore keystore/signature.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=NexusArchive Signing Certificate, OU=IT Department, O=NexusArchive, L=Beijing, ST=Beijing, C=CN" \
  -ext "KeyUsage=digitalSignature,nonRepudiation" \
  -ext "ExtendedKeyUsage=codeSigning,emailProtection"
```

**交互式命令（推荐，更简单）**：

```bash
keytool -genkeypair \
  -alias signing-cert \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -keystore keystore/signature.p12 \
  -storetype PKCS12
```

然后按提示输入：
- 密钥库密码：`changeit`（或自定义）
- 再次输入密码：相同密码
- 私钥密码：直接回车（使用相同密码）
- 姓名：`NexusArchive Signing Certificate`
- 组织单位：`IT Department`
- 组织：`NexusArchive`
- 城市：`Beijing`
- 省份：`Beijing`
- 国家代码：`CN`
- 确认：`y`

### 步骤 3: 验证生成结果

```bash
# 查看密钥库内容
keytool -list -v \
  -keystore keystore/signature.p12 \
  -storepass changeit

# 查看证书有效期
keytool -list -v \
  -keystore keystore/signature.p12 \
  -storepass changeit \
  -alias signing-cert | grep "Valid"
```

## 🔧 如果系统没有 Java

### macOS

```bash
# 使用 Homebrew 安装
brew install openjdk@17

# 或使用系统 Java（如果已安装）
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH=$JAVA_HOME/bin:$PATH
```

### Linux

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

### 使用 Docker（如果系统没有 Java）

```bash
# 使用 Docker 容器生成密钥库
docker run --rm -v $(pwd)/keystore:/keystore \
  openjdk:17-jdk-slim \
  keytool -genkeypair \
    -alias signing-cert \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -keystore /keystore/signature.p12 \
    -storetype PKCS12 \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=NexusArchive Signing Certificate, OU=IT Department, O=NexusArchive, L=Beijing, ST=Beijing, C=CN"
```

## 📝 配置应用

生成密钥库后，确保 `application.yml` 中的配置正确：

```yaml
signature:
  keystore:
    path: ./keystore/signature.p12  # 相对路径
    # 或绝对路径: /Users/user/nexusarchive/nexusarchive-java/keystore/signature.p12
    password: changeit  # 您设置的密码
```

## ✅ 验证配置

重启后端服务后，检查签章服务状态：

```bash
curl http://localhost:8080/api/signature/status
```

应该返回：
```json
{
  "code": 200,
  "data": {
    "available": true,
    "serviceType": "SM2",
    "message": "签章服务可用"
  }
}
```

---

**提示**: 如果遇到问题，请查看 `docs/guides/密钥库生成指南.md` 获取详细说明。












