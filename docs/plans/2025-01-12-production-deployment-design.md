# NexusArchive 生产环境部署设计

> **版本**: 1.1
> **创建日期**: 2025-01-12
> **更新日期**: 2025-01-12
> **适用环境**: 云服务器 + Docker + HTTPS
> **状态**: ✅ 已修复审查问题，可实施

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1 | 2025-01-12 | 修复审查发现的所有阻断问题 |
| 1.0 | 2025-01-12 | 初始设计 |

## 审查问题修复状态

| 序号 | 问题 | 状态 | 修复说明 |
|------|------|------|----------|
| 1 | .env.prod.example 文件缺失 | ✅ 已确认 | 文件已存在 |
| 2 | 后端容器未挂载归档目录 | ✅ 已修复 | docker-compose.app.yml 已添加归档目录挂载 |
| 3 | SSL 证书路径不一致 | ✅ 已修复 | 统一使用 /etc/letsencrypt 路径 |
| 4 | 备份保留期不满足合规要求 | ✅ 已修复 | 实现分层备份策略（日/周/月/年） |
| 5 | Redis 未配置密码 | ✅ 已修复 | 添加 REDIS_PASSWORD 环境变量 |
| 6 | 部署脚本路径问题 | ✅ 已修复 | 添加 cd 命令确保工作目录正确 |

---

## 目录

1. [方案概述](#1-方案概述)
2. [部署架构](#2-部署架构)
3. [HTTPS 配置](#3-https-配置)
4. [安全配置](#4-安全配置)
5. [部署流程](#5-部署流程)
6. [运维管理](#6-运维管理)
7. [文件清单](#7-文件清单)

---

## 1. 方案概述

### 1.1 部署目标

将 NexusArchive 从开发环境部署到生产服务器，实现：
- 对外展示访问（HTTPS）
- 数据持久化存储
- 自动备份与恢复
- SSL 证书自动续期

### 1.2 技术选型

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 服务器 | 云服务器 | 阿里云/腾讯云/AWS |
| 容器化 | Docker + Docker Compose | 统一管理所有服务 |
| 数据库 | PostgreSQL 14 (Docker) | 与开发环境一致 |
| 缓存 | Redis 7 (Docker) | Session 存储 |
| 反向代理 | Nginx (Alpine) | 静态文件 + API 代理 |
| SSL 证书 | Let's Encrypt + Certbot | 免费自动续期 |

---

## 2. 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                        云服务器                              │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │               Docker Compose                         │    │
│  │                                                      │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌───────────┐  │    │
│  │  │ Nginx (前端) │──│ 后端         │──│ PostgreSQL│  │    │
│  │  │              │  │ (Spring Boot)│  │           │  │    │
│  │  │ :80/:443     │  │ :19090       │  │ :5432     │  │    │
│  │  │              │  │              │  │           │  │    │
│  │  │ + SSL 证书   │  │              │  │           │  │    │
│  │  └──────────────┘  └──────────────┘  └───────────┘  │    │
│  │                      │                  │            │    │
│  │                      └──────────────────┘            │    │
│  │                              │                        │    │
│  │  ┌──────────────┐           │                        │    │
│  │  │ Redis        │◄──────────┘                        │    │
│  │  │ :6379        │                                    │    │
│  │  └──────────────┘                                    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  持久化卷:                                                   │
│  - nexusarchive-prod-db-data (PostgreSQL 数据)              │
│  - nexusarchive-prod-redis-data (Redis 数据)                │
│  - /opt/nexusarchive/archives (归档文件，宿主机挂载)         │
└─────────────────────────────────────────────────────────────┘

外部访问: HTTPS (443) → Nginx → 静态文件 / API 反向代理
```

### 2.2 服务说明

| 服务 | 容器名 | 端口 | 作用 |
|------|--------|------|------|
| Nginx | nexus-frontend | 80/443 | 静态文件托管、SSL 终止、API 反向代理 |
| 后端 | nexus-backend | 19090 | Spring Boot API 服务 |
| PostgreSQL | nexus-db | 5432 | 主数据库 |
| Redis | nexus-redis | 6379 | 缓存、Session 存储 |

---

## 3. HTTPS 配置

### 3.1 SSL 证书获取

使用 Let's Encrypt 免费 SSL 证书：

```bash
# 1. 安装 Certbot
sudo apt-get install certbot python3-certbot-nginx

# 2. 获取证书（确保域名已解析）
sudo certbot certonly --nginx -d archive.yourcompany.com

# 3. 验证自动续期
sudo certbot renew --dry-run
```

### 3.2 Nginx 配置

配置文件：`nginx/nginx.prod.conf`

```nginx
# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name archive.yourcompany.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 服务器
server {
    listen 443 ssl http2;
    server_name archive.yourcompany.com;

    ssl_certificate /etc/letsencrypt/live/archive.yourcompany.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/archive.yourcompany.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://nexus-backend:19090/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        client_max_body_size 100M;
    }
}
```

### 3.3 证书自动续期

Certbot 会自动添加 systemd timer 或 cron 任务。可手动验证：

```bash
# 检查续期任务
systemctl list-timers | grep certbot

# 手动添加续期后重启 Nginx
cat > /opt/nexusarchive/scripts/renew-ssl.sh << 'EOF'
#!/bin/bash
certbot renew --quiet --no-self-upgrade
docker exec nexus-frontend nginx -s reload
EOF

# 添加到 crontab（每天 2:30 检查）
# 30 2 * * * /opt/nexusarchive/scripts/renew-ssl.sh >> /var/log/certbot-renew.log 2>&1
```

---

## 4. 安全配置

### 4.1 环境变量安全

生产环境必须使用强随机密钥：

| 配置项 | 开发环境 | 生产环境 | 生成方式 |
|--------|---------|---------|----------|
| `DB_PASSWORD` | postgres | ≥16 位随机 | `openssl rand -base64 16` |
| `SM4_KEY` | 固定值 | 32位 Hex | `openssl rand -hex 16` |
| `AUDIT_LOG_HMAC_KEY` | dev 密钥 | 随机强密钥 | `openssl rand -hex 32` |
| `REDIS_PASSWORD` | 无 | 建议启用 | `openssl rand -base64 16` |

### 4.2 安全开关

| 配置项 | 开发环境 | 生产环境 | 说明 |
|--------|---------|---------|------|
| `APP_DEBUG_ENABLED` | true | **false** | 禁用调试接口 |
| `SCHEMA_VALIDATION_ENABLED` | false | **true** | 启用 Schema 验证 |
| `SPRING_PROFILES_ACTIVE` | dev | **prod** | 生产环境 profile |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS` | localhost | **实际域名** | CORS 白名单 |

### 4.3 端口配置

| 端口 | 服务 | 外部访问 |
|------|------|----------|
| 80 | Nginx HTTP | 是（重定向到 HTTPS） |
| 443 | Nginx HTTPS | 是 |
| 19090 | 后端 API | 否（仅容器内部） |
| 5432 | PostgreSQL | 否（仅容器内部） |
| 6379 | Redis | 否（仅容器内部） |

---

## 5. 部署流程

### 5.1 前置准备

**服务器要求**：
- OS: Ubuntu 20.04+ / CentOS 7+
- RAM: ≥4GB
- 磁盘: ≥50GB
- Docker: 20.10+
- docker-compose: 2.0+

**安装 Docker**：
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

### 5.2 部署步骤

```bash
# ========== 步骤 1: 上传部署包 ==========
# 在本地构建并打包
npm run build
cd nexusarchive-java && mvn clean package -DskipTests && cd ..
tar -czf nexusarchive-prod.tar.gz \
    docker-compose.*.yml \
    .env.prod.example \
    nginx/nginx.prod.template \
    deploy/*.sh \
    dist/ \
    nexusarchive-java/target/*.jar

# 上传到服务器
scp nexusarchive-prod.tar.gz user@server:/tmp/

# ========== 步骤 2: 服务器上解压 ==========
ssh user@server
cd /opt/nexusarchive
tar -xzf /tmp/nexusarchive-prod.tar.gz

# ========== 步骤 3: 配置环境变量 ==========
cp .env.prod.example .env.prod
# 编辑 .env.prod，填入实际配置

# ========== 步骤 4: 生成 Nginx 配置 ==========
sed 's/{{DOMAIN}}/archive.yourcompany.com/g' \
    nginx/nginx.prod.template > nginx/nginx.prod.conf

# ========== 步骤 5: 启动服务 ==========
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# ========== 步骤 6: 等待服务就绪 ==========
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f

# ========== 步骤 7: 配置 HTTPS ==========
sudo certbot certonly --nginx -d archive.yourcompany.com
docker restart nexus-frontend

# ========== 步骤 8: 验证 ==========
curl https://archive.yourcompany.com/api/health
```

### 5.3 一键部署

使用提供的部署脚本：

```bash
./deploy/deploy-prod.sh archive.yourcompany.com
```

脚本自动完成：
- 环境检查
- 生成随机密钥
- 构建 Docker 镜像
- 启动服务
- 显示后续配置提示

---

## 6. 运维管理

### 6.1 日常维护

| 任务 | 脚本 | 频率 | Cron |
|------|------|------|------|
| 日备份 | `deploy/backup.sh daily` | 每日 2:00 | `0 2 * * *` |
| 周备份 | `deploy/backup.sh weekly` | 每周日 3:00 | `0 3 * * 0` |
| 月备份 | `deploy/backup.sh monthly` | 每月 1 号 4:00 | `0 4 1 * *` |
| 年备份 | `deploy/backup.sh yearly` | 每年 1 月 1 号 | `0 5 1 1 *` |
| 健康检查 | `deploy/health-check.sh` | 每 5 分钟 | `*/5 * * * *` |

**分层备份保留策略**（满足《会计档案管理办法》10-30 年保管要求）：

| 备份类型 | 保留期限 | 说明 |
|----------|----------|------|
| 日备份 | 30 天 | 用于快速恢复近期数据 |
| 周备份 | 1 年 | 用于恢复月内历史数据 |
| 月备份 | 10 年 | 满足会计档案最低保管期限 |
| 年备份 | 永久 | 重要档案长期保存 |

### 6.2 服务管理

```bash
# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f nexus-backend

# 重启服务
docker-compose -f docker-compose.prod.yml restart nexus-backend

# 停止所有服务
docker-compose -f docker-compose.prod.yml down

# 更新服务
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

### 6.3 备份与恢复

**备份**：
```bash
# 日备份（每日）
./deploy/backup.sh daily

# 周备份（每周日）
./deploy/backup.sh weekly

# 月备份（每月 1 号）
./deploy/backup.sh monthly

# 年备份（每年 1 月 1 号）
./deploy/backup.sh yearly

# 查看备份统计
ls -la /opt/nexusarchive/backups/{daily,weekly,monthly,yearly}/
```

**恢复**：
```bash
# 回滚到指定版本
./deploy/rollback.sh 20250112_020000

# 仅恢复数据库
docker exec -i nexus-db psql -U postgres nexusarchive < backup.sql

# 恢复归档文件
tar -xzf archives.tar.gz -C /opt/nexusarchive/archives

# 恢复环境变量（含 HMAC 密钥）
cp env.prod /opt/nexusarchive/.env.prod
```

### 6.4 故障排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 前端 502 | 后端未启动 | `docker-compose logs nexus-backend` |
| 登录后 401 | Redis 连接失败 | `docker exec nexus-redis redis-cli ping` |
| 数据库连接失败 | 密码错误 | 检查 `.env.prod` |
| SSL 证书过期 | 未自动续期 | `certbot renew` |
| 磁盘满 | 日志/备份堆积 | `du -sh /opt/nexusarchive/*` |

---

## 7. 文件清单

### 7.1 新增文件

| 文件 | 说明 |
|------|------|
| `nginx/nginx.prod.template` | Nginx 生产配置模板 |
| `deploy/deploy-prod.sh` | 一键部署脚本 |
| `deploy/backup.sh` | 数据备份脚本 |
| `deploy/rollback.sh` | 版本回滚脚本 |
| `deploy/health-check.sh` | 健康检查脚本 |
| `.env.prod.example` | 生产环境变量模板（已更新） |

### 7.2 已有文件

| 文件 | 说明 |
|------|------|
| `docker-compose.infra.yml` | 基础设施配置 |
| `docker-compose.app.yml` | 应用服务配置 |
| `docker-compose.prod.yml` | 生产环境组合配置 |
| `Dockerfile.frontend.prod` | 前端生产 Dockerfile |
| `nexusarchive-java/Dockerfile` | 后端 Dockerfile |

### 7.3 目录结构

```
/opt/nexusarchive/
├── archives/           # 归档文件存储（宿主机挂载）
├── backups/            # 备份文件
├── data/               # 数据文件
├── logs/               # 日志文件
├── scripts/            # 运维脚本
│   ├── backup.sh
│   ├── renew-ssl.sh
│   └── health-check.sh
├── nginx/
│   ├── nginx.prod.conf
│   └── ssl/            # SSL 证书目录
└── .env.prod           # 环境变量（不提交）
```

---

## 8. 后续优化

| 优化项 | 优先级 | 说明 |
|--------|--------|------|
| 监控告警 | 高 | Prometheus + Grafana |
| 日志聚合 | 高 | ELK Stack 或 Loki |
| CI/CD | 中 | 自动构建和部署 |
| 负载均衡 | 中 | 多实例部署 |
| 数据库主从 | 低 | 高可用方案 |

---

**文档状态**: ✅ 已修复审查问题，可实施

**最后更新**: 2025-01-12

---

## 附录 A: JWT 密钥对生成

生产环境应使用独立的 RSA 密钥对，不要与开发环境共用：

```bash
# 生成生产环境 RSA 密钥对
mkdir -p /opt/nexusarchive/keystore
cd /opt/nexusarchive/keystore

# 生成私钥
openssl genrsa -out jwt_private.pem 2048

# 生成公钥
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem

# 设置权限
chmod 600 jwt_private.pem
chmod 644 jwt_public.pem

# 更新 docker-compose.app.yml 中的密钥挂载路径
```

---

## 附录 B: 部署前检查清单

- [ ] 服务器已安装 Docker 20.10+
- [ ] 服务器已安装 docker-compose 2.0+
- [ ] 服务器 80/443 端口已开放
- [ ] 域名 DNS 解析已生效
- [ ] 已创建 .env.prod 环境变量文件
- [ ] 已生成生产环境 JWT RSA 密钥对
- [ ] 数据库密码 ≥16 位强密码
- [ ] SM4_KEY 已生成 32 位 Hex
- [ ] AUDIT_LOG_HMAC_KEY 已生成
- [ ] Redis 密码已配置（可选但推荐）
- [ ] 已配置分层备份 Cron 任务
