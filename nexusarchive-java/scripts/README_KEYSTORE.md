# 密钥库生成说明

## 🚀 快速开始

### 方法一：使用简化脚本（推荐，交互式）

```bash
cd nexusarchive-java
bash scripts/generate_keystore_simple.sh
```

按提示输入信息即可。

### 方法二：使用完整脚本（需要环境变量）

```bash
cd nexusarchive-java
export KEYSTORE_PASSWORD=changeit
bash scripts/generate_keystore.sh
```

### 方法三：手动生成（最灵活）

```bash
cd nexusarchive-java
mkdir -p keystore

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
  -dname "CN=NexusArchive Signing Certificate, OU=IT Department, O=NexusArchive, L=Beijing, ST=Beijing, C=CN"
```

## 📋 输入示例

当使用交互式脚本时，按以下示例输入：

```
输入密钥库密码: changeit
再次输入新密码: changeit

您的名字与姓氏是什么?
  [Unknown]: NexusArchive Signing Certificate

您的组织单位名称是什么?
  [Unknown]: IT Department

您的组织名称是什么?
  [Unknown]: NexusArchive

您所在的城市或区域名称是什么?
  [Unknown]: Beijing

您所在的州或省份名称是什么?
  [Unknown]: Beijing

该单位的双字母国家/地区代码是什么?
  [Unknown]: CN

CN=NexusArchive Signing Certificate, OU=IT Department, O=NexusArchive, L=Beijing, ST=Beijing, C=CN 是否正确?
  [否]: y

输入 <signing-cert> 的密钥密码
        (如果和密钥库密码相同, 按回车): [直接回车]
```

## ✅ 验证生成结果

```bash
# 查看密钥库内容
keytool -list -v \
  -keystore keystore/signature.p12 \
  -storepass changeit

# 查看证书有效期
keytool -list -v \
  -keystore keystore/signature.p12 \
  -storepass changeit \
  -alias signing-cert | grep -E "(Valid from|Valid until)"
```

## 🔧 配置应用

生成密钥库后，在 `application.yml` 中配置：

```yaml
signature:
  keystore:
    path: ./keystore/signature.p12  # 或绝对路径
    password: changeit  # 您设置的密码
```

## ⚠️ 注意事项

1. **密码安全**：生产环境请使用强密码
2. **文件权限**：设置适当的文件权限 `chmod 600 keystore/signature.p12`
3. **备份**：定期备份密钥库文件
4. **有效期**：证书有效期10年，过期前需要更新

## 🐛 故障排除

### 找不到 keytool

**macOS**:
```bash
# 使用 Homebrew 安装
brew install openjdk

# 或设置 JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH=$JAVA_HOME/bin:$PATH
```

**Linux**:
```bash
sudo apt-get install openjdk-17-jdk
# 或
sudo yum install java-17-openjdk-devel
```

### 权限问题

```bash
chmod +x scripts/generate_keystore_simple.sh
chmod +x scripts/generate_keystore.sh
```

---

详细说明请查看：`docs/guides/密钥库生成指南.md`


