# 四性检测生产配置指南

## 1. 签章服务配置

```bash
# 环境变量设置
export SIGNATURE_KEYSTORE_PATH=/opt/nexusarchive/certs/sm2.p12
export SIGNATURE_KEYSTORE_PASSWORD=your_password
```

**证书要求**:
- 格式: PKCS12 (.p12)
- 算法: SM2 (国密) 或 EC 椭圆曲线
- 用途: PDF/OFD 签章校验

> ⚠️ 如无签章服务，四性检测将返回 WARNING（不阻断归档）

---

## 2. 病毒扫描配置

```bash
# 切换为 ClamAV (生产环境必须)
export VIRUS_SCAN_TYPE=clamav
export VIRUS_SCAN_HOST=localhost
export VIRUS_SCAN_PORT=3310
```

**ClamAV 安装**:
```bash
# CentOS/RHEL
sudo yum install -y clamav clamav-daemon
sudo systemctl start clamd@scan
sudo systemctl enable clamd@scan
```

---

## 3. 配置验证

```bash
# 检查配置
curl http://localhost:19090/api/actuator/health

# 测试四性检测
curl -H "Authorization: Bearer <token>" \
  http://localhost:19090/api/pool/check/<file-id>
```
