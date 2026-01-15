# NexusArchive 服务器部署完整计划

> **版本**: 2.0
> **创建日期**: 2025-01-12
> **更新日期**: 2025-01-12
> **部署方式**: Docker + HTTPS + Let's Encrypt

---

## 📋 部署前检查清单

### 服务器环境检查

| 检查项 | 命令 | 预期结果 |
|--------|------|----------|
| 操作系统版本 | `cat /etc/os-release` | Ubuntu 20.04+ / CentOS 7+ |
| 内存 | `free -h` | ≥4GB |
| 磁盘空间 | `df -h /` | ≥50GB |
| Docker | `docker --version` | 20.10+ |
| docker-compose | `docker-compose --version` | 2.0+ (或 `docker compose version`) |
| 端口 80 开放 | `netstat -tlnp \| grep :80` | 监听中 |
| 端口 443 开放 | `netstat -tlnp \| grep :443` | 监听中 |

### 域名和 DNS 检查

| 检查项 | 命令 | 预期结果 |
|--------|------|----------|
| DNS 解析 | `nslookup 你的域名` | 指向服务器 IP |
| 80 端口可访问 | `curl http://你的域名` | 能连接 |
| 443 端口开放 | `telnet 你的域名 443` | 能连接 |

### 安全组/防火墙配置

```bash
# 云服务器安全组需要开放：
# - TCP 80   (HTTP)
# - TCP 443  (HTTPS)
# - TCP 22   (SSH，建议限制 IP)

# 服务器防火墙（如果启用 ufw）
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw enable
```

---

## 🎯 部署策略选择

### 镜像构建策略（二选一）

| 策略 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **A: 本地构建镜像** | 快速、无需服务器源码 | 需要传输大镜像 | 开发环境、频繁更新 |
| **B: 服务器构建镜像** | 无需传输镜像 | 需要完整源码、构建慢 | 正式环境、CI/CD |

**本文档采用策略 A**：本地构建镜像，服务器仅运行预构建镜像。

---

## 🚀 完整部署流程

### 阶段 1: 准备部署包（本地执行）

```bash
# ========== 在开发机执行 ==========

# 1. 拉取最新代码
git pull
git log -1 --oneline  # 记录当前版本

# 2. 构建前端
npm run build
ls -la dist/          # 确认构建产物

# 3. 构建后端
cd nexusarchive-java
mvn clean package -DskipTests
ls -la target/*.jar   # 确认 JAR 文件
cd ..

# 4. 构建镜像（本地构建，避免服务器需要源码）
docker build -t nexusarchive-backend:latest -f nexusarchive-java/Dockerfile nexusarchive-java
docker build -t nexusarchive-web:latest -f Dockerfile.frontend.prod .

# 5. 导出镜像为 tar 文件
docker save nexusarchive-backend:latest | gzip > nexusarchive-backend.tar.gz
docker save nexusarchive-web:latest | gzip > nexusarchive-web.tar.gz

# 6. 创建部署包
tar -czf nexusarchive-prod-$(date +%Y%m%d).tar.gz \
    deploy/ \
    docker-compose.infra.yml \
    docker-compose.app.yml \
    docker-compose.prod.yml \
    nginx/nginx.prod.template \
    nginx/nginx.http.template \
    deploy/.env.prod.example \
    nexusarchive-backend.tar.gz \
    nexusarchive-web.tar.gz \
    db/seed-data.sql

# 7. 上传到服务器
scp nexusarchive-prod-*.tar.gz user@服务器IP:/tmp/
```

### 阶段 2: 服务器初始部署（服务器执行）

```bash
# ========== 登录服务器 ==========
ssh user@服务器IP

# ========== 步骤 1: 安装 Docker（如果没有） ==========
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 验证安装
docker --version
docker-compose --version

# ========== 步骤 2: 创建部署目录 ==========
sudo mkdir -p /opt/nexusarchive
sudo chown -R $USER:$USER /opt/nexusarchive
cd /opt/nexusarchive

# ========== 步骤 3: 解压部署包 ==========
tar -xzf /tmp/nexusarchive-prod-*.tar.gz
ls -la

# ========== 步骤 4: 加载 Docker 镜像 ==========
docker load < nexusarchive-backend.tar.gz
docker load < nexusarchive-web.tar.gz

# 验证镜像
docker images | grep nexusarchive

# ========== 步骤 5: 配置环境变量 ==========
cp deploy/.env.prod.example .env.prod
vi .env.prod

# 必须修改的配置项：
# --------------------------------------------
# DB_PASSWORD=你的强密码≥16位
# SM4_KEY=你的32位hex密钥
# AUDIT_LOG_HMAC_KEY=你的hmac密钥
# APP_SECURITY_CORS_ALLOWED_ORIGINS=https://你的域名
# REDIS_PASSWORD=你的redis密码（可选但推荐）

# 生成密钥的命令：
# openssl rand -base64 16   # 数据库密码
# openssl rand -hex 16      # SM4密钥
# openssl rand -hex 32      # HMAC密钥
# openssl rand -base64 16   # Redis密码

# ========== 步骤 6: 生成 Nginx 配置（HTTP 模式，首次部署） ==========
# ⚠️ 首次部署使用 HTTP 模式，证书获取后再切换 HTTPS
DOMAIN="你的域名"
sed "s/{{DOMAIN}}/$DOMAIN/g" nginx/nginx.http.template > nginx/nginx.conf

# 确认生成成功
grep "server_name" nginx/nginx.conf

# ========== 步骤 7: 启动服务（HTTP 模式） ==========
# 修改 docker-compose.app.yml，使用 nginx.conf 而非 nginx.prod.conf
sed 's|nginx/nginx.prod.conf|nginx/nginx.conf|g' docker-compose.app.yml > docker-compose.app.yml.tmp
mv docker-compose.app.yml.tmp docker-compose.app.yml

docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               -f docker-compose.prod.yml \
               --env-file .env.prod up -d

# ========== 步骤 8: 等待服务启动 ==========
echo "等待服务启动..."
sleep 30

# ========== 步骤 9: 验证服务状态 ==========
docker-compose -f docker-compose.prod.yml ps

# 测试 HTTP 访问
curl -I http://localhost/

# 测试 API
curl http://localhost/api/health

# ========== 步骤 10: 查看日志（如有问题） ==========
docker-compose -f docker-compose.prod.yml logs nexus-backend
docker-compose -f docker-compose.prod.yml logs nexus-frontend
```

### 阶段 3: 配置 HTTPS（服务器执行）

```bash
# ========== 步骤 1: 安装 Certbot ==========
sudo apt-get update
sudo apt-get install -y certbot

# ========== 步骤 2: 停止前端容器（释放 443 端口） ==========
docker-compose -f docker-compose.prod.yml stop nexus-frontend

# ========== 步骤 3: 获取 SSL 证书 ==========
sudo certbot certonly --standalone \
  -d 你的域名 \
  --email 你的邮箱 \
  --agree-tos \
  --non-interactive

# 证书将保存在：/etc/letsencrypt/live/你的域名/

# ========== 步骤 4: 验证证书 ==========
sudo ls -la /etc/letsencrypt/live/你的域名/
# 应该看到：fullchain.pem, privkey.pem, chain.pem

# ========== 步骤 5: 检查证书有效期 ==========
sudo certbot certificates

# ========== 步骤 6: 切换到 HTTPS 配置 ==========
DOMAIN="你的域名"
sed "s/{{DOMAIN}}/$DOMAIN/g" nginx/nginx.prod.template > nginx/nginx.prod.conf

# 恢复使用 nginx.prod.conf
sed 's|nginx/nginx.conf|nginx/nginx.prod.conf|g' docker-compose.app.yml > docker-compose.app.yml.tmp
mv docker-compose.app.yml.tmp docker-compose.app.yml

# ========== 步骤 7: 启动前端容器 ==========
docker-compose -f docker-compose.prod.yml start nexus-frontend

# ========== 步骤 8: 验证容器内证书访问 ==========
docker exec nexus-frontend ls -la /etc/letsencrypt/live/你的域名/

# ========== 步骤 9: 验证 HTTPS 访问 ==========
curl -I https://localhost/
curl -I http://localhost/  # 应该返回 301 重定向

# ========== 步骤 10: 配置自动续期 ==========
# Certbot 会自动添加 systemd timer
sudo systemctl status certbot.timer

# 手动测试续期
sudo certbot renew --dry-run

# 配置续期后自动重启 Nginx
sudo tee /etc/letsencrypt/renewal-hooks/post/restart-nginx.sh << 'EOF'
#!/bin/bash
docker restart nexus-frontend
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/post/restart-nginx.sh
```

### 阶段 4: 配置自动备份（服务器执行）

```bash
# ========== 步骤 1: 创建脚本目录 ==========
sudo mkdir -p /opt/nexusarchive/scripts
sudo chown $USER:$USER /opt/nexusarchive/scripts

# ========== 步骤 2: 复制备份脚本 ==========
cp deploy/backup.sh /opt/nexusarchive/scripts/
cp deploy/health-check.sh /opt/nexusarchive/scripts/
cp deploy/rollback.sh /opt/nexusarchive/scripts/
chmod +x /opt/nexusarchive/scripts/*.sh

# ========== 步骤 3: 配置 Cron ==========
crontab -e

# 添加以下内容：
# NexusArchive 备份任务
0 2 * * * /opt/nexusarchive/scripts/backup.sh daily >> /var/log/nexus-backup.log 2>&1
0 3 * * 0 /opt/nexusarchive/scripts/backup.sh weekly >> /var/log/nexus-backup.log 2>&1
0 4 1 * * /opt/nexusarchive/scripts/backup.sh monthly >> /var/log/nexus-backup.log 2>&1
0 5 1 1 * /opt/nexusarchive/scripts/backup.sh yearly >> /var/log/nexus-backup.log 2>&1
*/5 * * * * /opt/nexusarchive/scripts/health-check.sh >> /var/log/nexus-health.log 2>&1

# ========== 步骤 4: 验证 Cron ==========
crontab -l

# ========== 步骤 5: 备份恢复测试（重要！） ==========
# ⚠️  "没有测试过的备份等于没有备份"

# 5.1 执行一次手动备份
/opt/nexusarchive/scripts/backup.sh daily

# 5.2 验证备份文件生成
ls -la /opt/nexusarchive/backups/daily/

# 5.3 解压备份进行验证
LATEST_BACKUP=$(ls -t /opt/nexusarchive/backups/daily/*.tar.gz 2>/dev/null | head -1)
if [ -z "$LATEST_BACKUP" ]; then
    echo "错误：没有找到备份文件"
    exit 1
fi

# 创建临时目录
TEMP_DIR=$(mktemp -d)
cd $TEMP_DIR

# 解压备份
tar -xzf $LATEST_BACKUP

BACKUP_DIR=$(basename $LATEST_BACKUP .tar.gz)
cd $BACKUP_DIR

# 5.4 验证备份内容
echo "=== 备份文件清单 ==="
ls -lh

# 验证 checksums
if [ -f checksums.txt ]; then
    echo "=== 校验和验证 ==="
    sha256sum -c checksums.txt
fi

# 5.5 验证数据库备份完整性（仅检查，不执行）
echo "=== 数据库备份完整性检查 ==="
head -20 database.sql
tail -20 database.sql
grep "PostgreSQL database dump" database.sql && echo "✓ 数据库备份格式正确" || echo "✗ 数据库备份格式异常"

# 5.6 验证关键配置已备份
echo "=== 关键配置验证 ==="
grep -q "SM4_KEY" env.prod && echo "✓ SM4_KEY 已备份" || echo "✗ SM4_KEY 缺失"
grep -q "AUDIT_LOG_HMAC_KEY" env.prod && echo "✓ HMAC_KEY 已备份" || echo "✗ HMAC_KEY 缺失"
grep -q "DB_PASSWORD" env.prod && echo "✓ DB_PASSWORD 已备份" || echo "✗ DB_PASSWORD 缺失"

# 清理临时目录
cd /opt/nexusarchive
rm -rf $TEMP_DIR

echo ""
echo "=========================================="
echo "✅ 备份恢复测试完成！"
echo "=========================================="
echo "如果所有验证都通过，说明备份配置正确。"
echo "建议每季度进行一次完整的恢复测试。"
```

### 阶段 5: 验证部署（服务器执行）

```bash
# ========== 步骤 1: 健康检查 ==========
/opt/nexusarchive/scripts/health-check.sh

# ========== 步骤 2: 手动验证 ==========
# 1. 检查前端
curl -I https://你的域名/

# 2. 检查 API
curl https://你的域名/api/health

# 3. 检查 SSL 证书
echo | openssl s_client -connect 你的域名:443 2>/dev/null | openssl x509 -noout -dates

# 4. 检查重定向
curl -I http://你的域名/  # 应该返回 301 重定向到 HTTPS

# ========== 步骤 3: 验证数据持久化 ==========
# 确认数据库数据卷已创建
docker volume ls | grep nexusarchive

# 确认数据卷有内容
docker exec nexus-db ls -la /var/lib/postgresql/data

# 验证 Redis 数据卷
docker exec nexus-redis ls -la /data

# ========== 步骤 4: 验证证书挂载 ==========
docker exec nexus-frontend ls -la /etc/letsencrypt/live/你的域名/

# ========== 步骤 5: 内存使用检查 ==========
docker stats --no-stream
# 确认内存使用在限制范围内

# ========== 登录测试 ==========
# 浏览器访问：https://你的域名
# 默认账号：admin / admin123
# 登录后立即修改密码
```

---

## 🔄 代码更新部署

### 更新流程（推荐：本地构建镜像）

```bash
# ========== 本地执行 ==========

# 1. 拉取最新代码
git pull
git log -1 --oneline  # 记录版本

# 2. 构建前端
npm run build

# 3. 构建后端
cd nexusarchive-java && mvn package && cd ..

# 4. 构建镜像
docker build -t nexusarchive-backend:latest -f nexusarchive-java/Dockerfile nexusarchive-java
docker build -t nexusarchive-web:latest -f Dockerfile.frontend.prod .

# 5. 导出镜像
docker save nexusarchive-backend:latest | gzip > nexusarchive-backend.tar.gz
docker save nexusarchive-web:latest | gzip > nexusarchive-web.tar.gz

# 6. 上传到服务器
scp nexusarchive-backend.tar.gz nexusarchive-web.tar.gz user@服务器IP:/opt/nexusarchive/

# ========== 服务器执行 ==========
ssh user@服务器IP
cd /opt/nexusarchive

# 加载新镜像
docker load < nexusarchive-backend.tar.gz
docker load < nexusarchive-web.tar.gz

# 重启服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 验证
curl https://你的域名/api/health
```

---

## 🛠 故障排查

### 证书挂载路径说明

**重要**：确保 Nginx 容器能读取 Let's Encrypt 证书

```
宿主机路径                容器内路径                  用途
/etc/letsencrypt/    →    /etc/letsencrypt/          证书根目录
                      →    /etc/letsencrypt/live/域名/  实际证书文件
                      →    /etc/letsencrypt/archive/域名/ 历史证书
```

**验证挂载是否正确**：
```bash
# 检查容器内的证书文件
docker exec nexus-frontend ls -la /etc/letsencrypt/live/你的域名/

# 应该看到：
# fullchain.pem  -> ../../archive/域名/fullchain2.pem
# privkey.pem    -> ../../archive/域名/privkey2.pem
# chain.pem      -> ../../archive/域名/chain2.pem
```

### HTTPS 相关问题

| 问题 | 症状 | 解决方案 |
|------|------|----------|
| SSL 证书不存在 | 502/证书错误 | `certbot certonly --standalone -d 域名` |
| 证书路径错误 | Nginx 启动失败 | `docker exec nexus-frontend ls /etc/letsencrypt/live/域名/` |
| 证书过期 | 浏览器警告 | `certbot renew && docker restart nexus-frontend` |
| ACME Challenge 失败 | 证书获取失败 | 确认 80 端口可访问，DNS 解析正确 |
| HTTP 无法重定向 | HTTP 访问不跳转 | 检查 Nginx 配置的 return 301 指令 |
| 首次部署无证书 | 容器启动失败 | 使用 `nginx.http.template` 先 HTTP 模式启动 |

### 快速诊断命令

```bash
# 1. 检查 SSL 证书
sudo certbot certificates

# 2. 测试 SSL 连接
openssl s_client -connect 你的域名:443 -servername 你的域名

# 3. 检查证书有效期
echo | openssl s_client -connect 你的域名:443 2>/dev/null | openssl x509 -noout -dates

# 4. 查看 Nginx 错误日志
docker logs nexus-frontend --tail 50

# 5. 测试 HTTP 重定向
curl -v http://你的域名/ 2>&1 | grep -E "HTTP|Location"

# 6. 测试 HTTPS 访问
curl -I https://你的域名/

# 7. 检查容器内证书访问
docker exec nexus-frontend cat /etc/letsencrypt/live/你的域名/fullchain.pem | head -1
```

### Redis 密码配置

**注意**：如果配置了 `REDIS_PASSWORD`，需要确保 Redis 启用时启用认证。

```bash
# 检查 Redis 是否需要密码
docker exec nexus-redis redis-cli ping
# 如果返回 (error) NOAUTH Authentication required，说明需要密码

# 测试密码连接
docker exec nexus-redis redis-cli -a 你的密码 ping

# 如果需要启用 Redis 密码，修改 docker-compose.infra.yml：
# command: redis-server --requirepass ${REDIS_PASSWORD}
```

---

## 📝 部署后配置检查表

部署完成后，请确认以下项目：

### 基础功能
- [ ] HTTP 访问自动重定向到 HTTPS
- [ ] 浏览器地址栏显示锁图标
- [ ] API 健康检查返回 200
- [ ] 登录功能正常

### 数据持久化
- [ ] 数据库数据卷已创建 (`docker volume ls \| grep nexusarchive`)
- [ ] Redis 数据卷已创建
- [ ] 重启容器后数据不丢失

### SSL/HTTPS 配置
- [ ] SSL 证书文件存在于容器内 (`docker exec nexus-frontend ls /etc/letsencrypt/live/域名/`)
- [ ] SSL 证书有效期 > 80 天
- [ ] TLSv1.2/1.3 协议启用

### 安全配置
- [ ] CORS 仅允许生产域名
- [ ] 调试模式已关闭 (`APP_DEBUG_ENABLED=false`)
- [ ] 数据库密码 ≥16 位
- [ ] Redis 密码已配置（与实际使用一致）

### 备份与监控
- [ ] 自动备份任务已配置 (Cron)
- [ ] 健康检查任务已配置
- [ ] 备份恢复测试已通过
- [ ] HMAC 密钥已备份到离线介质
- [ ] 分层备份策略已配置（日/周/月/年）

### 内存限制
- [ ] 后端内存限制已配置 (2GB)
- [ ] 前端内存限制已配置 (512MB)
- [ ] `docker stats` 显示内存使用正常

---

## 📞 紧急联系

如遇到问题：

1. 保存日志：`docker-compose logs > error.log`
2. 记录版本：`git log -1`
3. 记录命令：重现问题的命令
4. 联系技术支持

---

**文档版本**: 2.0
**最后更新**: 2025-01-12
**变更记录**:
- v2.0: 重构部署策略，添加本地构建镜像方案
- v2.0: 添加 HTTP-only 模式解决首次部署证书问题
- v2.0: 修复备份验证流程
- v2.0: 明确 Redis 密码配置
