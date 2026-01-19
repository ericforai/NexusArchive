# 部署复盘与改进计划

> **部署日期**: 2026-01-19
> **服务器**: 115.190.237.184 (火山云 ECS)
> **域名**: www.digivoucher.cn
> **总结**: 部署完成但耗时过长，原计划与实际情况存在偏差

---

## 一、本次部署时间线

| 时间 | 问题 | 解决方案 | 耗时 |
|------|------|----------|------|
| T+0h | 500 错误 - API 不响应 | 排查后发现是数据库列缺失 | ~1h |
| T+1h | 数据库列不匹配 | 手动 ALTER TABLE 添加缺失列 | ~1h |
| T+2h | 数据完全恢复需求 | 使用 pg_dump/pg_restore 完整迁移 | ~0.5h |
| T+2.5h | 域名 HTTPS 配置 | 修改 Nginx 配置 | ~0.5h |
| T+3h | SSL 证书设置 | 误用 acme.sh + DNS API 方案 | ~1.5h |
| T+4.5h | 发现项目已有 setup_ssl.sh | 改用 Certbot standalone | ~0.3h |
| **总计** | | | **~5小时** |

---

## 二、原计划 vs 实际情况

### 问题 1: 部署架构不匹配 🔴 严重

| 项目 | 原计划 (2025-01-12) | 实际部署 |
|------|---------------------|----------|
| 部署方式 | Docker Compose | 直接运行 JAR + Systemd |
| Web 服务器 | Docker 容器内 Nginx | 宿主机 Nginx |
| 数据库 | Docker 容器 | Docker 独立容器 |
| 部署脚本 | docker-compose.yml | deploy/deploy.sh (已存在) |

**影响**: 完全按照原计划操作会导致失败，需要临时调整策略。

**改进**:
- 需要补充"非 Docker 部署"方案
- 或者强制统一为 Docker 部署方式

---

### 问题 2: 数据库 Schema 验证缺失 🔴 严重

**现象**:
```
ERROR: column "accbook_mapping" does not exist
ERROR: column "match_score" does not exist
ERROR: column "summary" does not exist
```

**根本原因**:
- 本地开发数据库已通过 Flyway 迁移到最新版本
- 服务器数据库是旧版本或空白库
- 原计划没有"Schema 同步检查"步骤

**改进**:
```bash
# 添加到部署前检查
# 1. 检查 Flyway 迁移状态
curl http://localhost:19090/actuator/flyway

# 2. 对比本地和服务器 Schema
# 3. 自动执行缺失的迁移
```

---

### 问题 3: 数据迁移方案不完整 🟡 中等

**原计划**: 仅提到 `db/seed-data.sql` 作为演示数据

**实际情况**: 需要将本地开发数据库完整迁移到服务器

**已使用方案**:
```bash
# 本地导出
docker exec nexus-db pg_dump -U postgres -d nexusarchive -Fc > dump.dump

# 服务器恢复
docker exec -i nexus-db pg_restore -U postgres -d nexusarchive < dump.dump
```

**改进**: 在原计划中添加"数据迁移"章节

---

### 问题 4: SSL 证书走了弯路 🟡 中等

**错误路径**:
1. 尝试 acme.sh + Aliyun DNS API
2. 配置 Aliyun AccessKey
3. 多次尝试不同 CA (ZeroSSL, Let's Encrypt, 国内 CA)
4. 全部失败

**正确路径** (项目中已存在):
```bash
# deploy/setup_ssl.sh - 使用 Certbot standalone
certbot certonly --standalone -d digivoucher.cn -d www.digivoucher.cn
```

**问题**: 项目中已有正确的 SSL 设置脚本，但文档中未明确说明

**改进**:
- 在原计划中明确引用 `setup_ssl.sh`
- 添加"SSL 证书快速设置"独立章节

---

### 问题 5: Nginx 配置差异 🟢 轻微

| 项目 | Docker 方式 | 实际方式 |
|------|-------------|----------|
| 配置位置 | 容器内 /etc/nginx/nginx.conf | 宿主机 /etc/nginx/conf.d/ |
| 证书挂载 | volume mount | 直接读取 /etc/letsencrypt/ |
| 重载命令 | docker restart nginx | systemctl reload nginx |

---

## 三、改进计划

### 优先级 P0 (必须修复)

1. **添加部署前 Schema 检查**
   - 检查 Flyway 迁移状态
   - 对比 Entity 与数据库列
   - 自动执行缺失迁移

2. **补充非 Docker 部署方案**
   - Systemd 服务配置
   - 宿主机 Nginx 配置
   - 与 Docker 方案并列

3. **添加数据库迁移章节**
   - pg_dump/pg_restore 完整流程
   - 数据验证步骤

### 优先级 P1 (强烈建议)

4. **明确 SSL 证书设置方法**
   - 直接引用 `setup_ssl.sh`
   - 移除 acme.sh 等复杂方案

5. **添加故障排查章节**
   - 常见 500 错误诊断
   - 数据库列缺失快速修复

### 优先级 P2 (可选优化)

6. **自动化部署脚本改进**
   - integrate setup_ssl.sh into deploy.sh
   - 添加 Schema 自动检查

---

## 四、正确的快速部署流程 (修订版)

### 方式 A: 使用 deploy.sh (推荐，已验证)

```bash
# 1. 本地构建
./deploy/deploy.sh

# 2. 服务器上自动执行
# - 上传 JAR + 静态资源
# - 重启服务
# - 重载 Nginx

# 3. SSL 证书 (首次)
ssh server "cd /opt/nexusarchive && ./deploy/setup_ssl.sh"
```

### 方式 B: 数据库迁移 (需要时)

```bash
# 本地导出
docker exec nexus-db pg_dump -U postgres -d nexusarchive -Fc > dump.dump

# 上传并恢复
scp dump.dump server:/tmp/
ssh server "docker exec -i nexus-db pg_restore -U postgres -d nexusarchive < /tmp/dump.dump"
```

---

## 五、经验教训

1. **文档与代码脱节**: 项目中已有 `setup_ssl.sh`，但文档未同步
2. **Schema 漂移风险**: 本地与服务器 Schema 不同步导致 500 错误
3. **过度工程化**: SSL 证书走了 DNS API 弯路，standalone 模式更简单
4. **缺少预检查**: 部署前未验证数据库 Schema 导致事后补救

---

## 六、下一步行动

- [ ] 更新 `docs/plans/2025-01-12-server-deployment-plan.md`
- [ ] 添加 Schema 验证步骤
- [ ] 补充非 Docker 部署方案
- [ ] 简化 SSL 证书说明
- [ ] 添加故障排查章节

---

**文档创建**: 2026-01-19
