# NexusArchive 生产部署操作手册

> **版本**: 1.0
> **更新日期**: 2025-01-12
> **适用人员**: 运维人员、开发人员
> **部署方式**: Docker + HTTPS

---

## 目录

1. [快速开始](#1-快速开始)
2. [首次部署](#2-首次部署)
3. [代码更新](#3-代码更新)
4. [日常运维](#4-日常运维)
5. [故障排查](#5-故障排查)
6. [附录](#6-附录)

---

## 1. 快速开始

### 1.1 前置条件

| 项目 | 要求 |
|------|------|
| 服务器 | Ubuntu 20.04+ / CentOS 7+ |
| RAM | ≥4GB |
| 磁盘 | ≥50GB |
| Docker | 20.10+ |
| docker-compose | 2.0+ |
| 域名 | 已解析到服务器 IP |
| 端口 | 80/443 已开放 |

### 1.2 一键部署（推荐）

```bash
# 1. 上传部署包到服务器
scp nexusarchive-prod.tar.gz user@server:/tmp/

# 2. 登录服务器
ssh user@server

# 3. 解压并部署
cd /opt
mkdir -p nexusarchive
cd nexusarchive
tar -xzf /tmp/nexusarchive-prod.tar.gz
./deploy/deploy-prod.sh archive.yourcompany.com

# 4. 配置 HTTPS
sudo certbot certonly --nginx -d archive.yourcompany.com
docker restart nexus-frontend
```

---

## 2. 首次部署

### 2.1 本地准备

```bash
# ========== 在开发机执行 ==========

# 1. 拉取最新代码
git pull

# 2. 构建前端
npm run build

# 3. 构建后端
cd nexusarchive-java
mvn clean package -DskipTests
cd ..

# 4. 打包部署文件
tar -czf nexusarchive-prod.tar.gz \
    deploy/ \
    docker-compose.infra.yml \
    docker-compose.app.yml \
    docker-compose.prod.yml \
    nginx/nginx.prod.template \
    .env.prod.example \
    dist/ \
    nexusarchive-java/target/*.jar

# 5. 上传到服务器
scp nexusarchive-prod.tar.gz user@server:/tmp/
```

### 2.2 服务器部署

```bash
# ========== 在服务器执行 ==========

# 1. 解压
cd /opt
mkdir -p nexusarchive
cd nexusarchive
tar -xzf /tmp/nexusarchive-prod.tar.gz

# 2. 配置环境变量
cp .env.prod.example .env.prod
vi .env.prod  # 修改以下配置：

# 必须修改的配置
DB_PASSWORD=<强密码≥16位>
SM4_KEY=<openssl rand -hex 16 输出>
AUDIT_LOG_HMAC_KEY=<openssl rand -hex 32 输出>
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://你的域名

# 3. 生成 Nginx 配置
sed 's/{{DOMAIN}}/你的域名/g' nginx/nginx.prod.template > nginx/nginx.prod.conf

# 4. 启动服务
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               -f docker-compose.prod.yml \
               --env-file .env.prod up -d

# 5. 查看状态
docker-compose -f docker-compose.prod.yml ps

# 6. 查看日志
docker-compose -f docker-compose.prod.yml logs -f nexus-backend
```

### 2.3 配置 HTTPS

```bash
# 1. 安装 Certbot
sudo apt-get update
sudo apt-get install certbot python3-certbot-nginx

# 2. 获取 SSL 证书
sudo certbot certonly --nginx -d 你的域名

# 3. 重启前端容器
docker restart nexus-frontend

# 4. 验证
curl https://你的域名/api/health
```

### 2.4 配置自动备份

```bash
# 1. 复制脚本到执行目录
mkdir -p /opt/nexusarchive/scripts
cp deploy/backup.sh /opt/nexusarchive/scripts/
cp deploy/health-check.sh /opt/nexusarchive/scripts/
chmod +x /opt/nexusarchive/scripts/*.sh

# 2. 配置 Cron 任务
crontab -e

# 添加以下内容：
0 2 * * * /opt/nexusarchive/scripts/backup.sh daily >> /var/log/nexus-backup.log 2>&1
0 3 * * 0 /opt/nexusarchive/scripts/backup.sh weekly >> /var/log/nexus-backup.log 2>&1
0 4 1 * * /opt/nexusarchive/scripts/backup.sh monthly >> /var/log/nexus-backup.log 2>&1
0 5 1 1 * /opt/nexusarchive/scripts/backup.sh yearly >> /var/log/nexus-backup.log 2>&1
*/5 * * * * /opt/nexusarchive/scripts/health-check.sh >> /var/log/nexus-health.log 2>&1
```

---

## 3. 代码更新

### 3.1 判断是否需要重新构建

| 修改内容 | 操作 |
|----------|------|
| 前端代码 (src/) | 重新构建前端镜像 |
| 后端 Java 代码 | 重新构建后端镜像 |
| 环境变量 | 修改 .env.prod 后重启 |
| Nginx 配置 | 修改后重启前端容器 |
| Docker 配置 | 修改后重新 up -d |

### 3.2 代码更新流程

```bash
# ========== 方案 A: 服务器有 Git（推荐） ==========

# 1. 登录服务器
ssh user@server
cd /opt/nexusarchive

# 2. 拉取最新代码
git pull

# 3. 重新构建镜像
docker build -t nexusarchive-backend:latest -f nexusarchive-java/Dockerfile nexusarchive-java
docker build -t nexusarchive-web:latest -f Dockerfile.frontend.prod .

# 4. 重启服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# ========== 方案 B: 服务器无 Git ==========

# 1. 本地构建
npm run build
cd nexusarchive-java && mvn package && cd ..

# 2. 上传 JAR 和 dist
scp nexusarchive-java/target/*.jar user@server:/opt/nexusarchive/nexusarchive-java/target/
scp -r dist/* user@server:/opt/nexusarchive/dist/

# 3. 服务器上重新构建
ssh user@server
cd /opt/nexusarchive
docker build -t nexusarchive-backend:latest -f nexusarchive-java/Dockerfile nexusarchive-java
docker build -t nexusarchive-web:latest -f Dockerfile.frontend.prod .
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 3.3 仅更新配置（不重建）

```bash
# 1. 修改配置
vi .env.prod
vi nginx/nginx.prod.conf

# 2. 重启相关服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod restart nexus-backend
docker-compose -f docker-compose.prod.yml restart nexus-frontend
```

---

## 4. 日常运维

### 4.1 服务管理

```bash
# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f nexus-backend
docker-compose -f docker-compose.prod.yml logs -f nexus-frontend

# 重启单个服务
docker restart nexus-backend
docker restart nexus-frontend

# 停止所有服务
docker-compose -f docker-compose.prod.yml down

# 启动所有服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 4.2 数据备份

```bash
# 手动备份
/opt/nexusarchive/scripts/backup.sh daily

# 查看备份
ls -la /opt/nexusarchive/backups/

# 备份统计
echo "日备份: $(find /opt/nexusarchive/backups/daily -type f 2>/dev/null | wc -l)"
echo "周备份: $(find /opt/nexusarchive/backups/weekly -type f 2>/dev/null | wc -l)"
echo "月备份: $(find /opt/nexusarchive/backups/monthly -type f 2>/dev/null | wc -l)"
echo "年备份: $(find /opt/nexusarchive/backups/yearly -type f 2>/dev/null | wc -l)"
```

### 4.3 数据恢复

```bash
# 回滚到指定备份
/opt/nexusarchive/deploy/rollback.sh 20250112_020000

# 仅恢复数据库
docker exec -i nexus-db psql -U postgres nexusarchive < backup.sql

# 恢复归档文件
tar -xzf archives.tar.gz -C /opt/nexusarchive/archives
```

### 4.4 SSL 证书续期

```bash
# Certbot 会自动续期，手动验证
sudo certbot renew --dry-run

# 手动续期
sudo certbot renew
docker restart nexus-frontend
```

### 4.5 健康检查

```bash
# 运行健康检查脚本
/opt/nexusarchive/scripts/health-check.sh

# 手动检查
curl http://localhost/api/health
docker exec nexus-db pg_isready -U postgres
docker exec nexus-redis redis-cli ping
```

---

## 5. 故障排查

### 5.1 快速诊断清单

```bash
# 1. 检查容器状态
docker ps -a

# 2. 检查容器日志
docker logs nexus-backend --tail 100
docker logs nexus-frontend --tail 100
docker logs nexus-db --tail 100
docker logs nexus-redis --tail 100

# 3. 检查磁盘空间
df -h

# 4. 检查端口占用
netstat -tlnp | grep -E '80|443|19090|5432|6379'

# 5. 测试数据库连接
docker exec nexus-db psql -U postgres -c "SELECT 1;"

# 6. 测试 Redis 连接
docker exec nexus-redis redis-cli ping
```

### 5.2 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 前端 502 | 后端未启动 | `docker logs nexus-backend` |
| 登录后 401 | Redis 连接失败 | `docker exec nexus-redis redis-cli ping` |
| 数据库连接失败 | 密码错误 | 检查 `.env.prod` |
| SSL 证书过期 | 未自动续期 | `certbot renew` |
| 磁盘满 | 日志/备份堆积 | `du -sh /opt/nexusarchive/*` |
| 镜像构建失败 | 网络问题 | 检查 Docker Hub 连接 |

### 5.3 紧急回滚

```bash
# 1. 停止当前服务
docker-compose -f docker-compose.prod.yml down

# 2. 恢复上一版本镜像
docker tag nexusarchive-backend:old nexusarchive-backend:latest
docker tag nexusarchive-web:old nexusarchive-web:latest

# 3. 重启服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

## 6. 附录

### 6.1 目录结构

```
/opt/nexusarchive/
├── archives/               # 归档文件存储
├── backups/                # 备份文件
│   ├── daily/             # 日备份（30天）
│   ├── weekly/            # 周备份（1年）
│   ├── monthly/           # 月备份（10年）
│   └── yearly/            # 年备份（永久）
├── data/                   # 数据文件
├── scripts/                # 运维脚本
│   ├── backup.sh
│   └── health-check.sh
├── deploy/                 # 部署脚本
│   ├── deploy-prod.sh
│   ├── backup.sh
│   ├── rollback.sh
│   └── health-check.sh
├── nginx/                  # Nginx 配置
│   ├── nginx.prod.conf
│   └── nginx.prod.template
├── nexusarchive-java/      # 后端代码
│   ├── target/            # JAR 文件
│   └── keystore/          # 密钥
├── dist/                   # 前端构建产物
├── docker-compose.*.yml    # Docker 配置
└── .env.prod               # 环境变量
```

### 6.2 环境变量说明

| 变量 | 必须 | 说明 |
|------|------|------|
| `DB_PASSWORD` | ✅ | 数据库密码（≥16位） |
| `SM4_KEY` | ✅ | 国密加密密钥（32位Hex） |
| `AUDIT_LOG_HMAC_KEY` | ✅ | 审计日志HMAC密钥 |
| `REDIS_PASSWORD` | 推荐 | Redis密码 |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS` | ✅ | CORS白名单 |

### 6.3 密钥生成命令

```bash
# 数据库密码（16位）
openssl rand -base64 16

# SM4 密钥（32位Hex）
openssl rand -hex 16

# HMAC 密钥（32位Hex）
openssl rand -hex 32

# JWT RSA 密钥对
mkdir -p keystore
openssl genrsa -out keystore/jwt_private.pem 2048
openssl rsa -in keystore/jwt_private.pem -pubout -out keystore/jwt_public.pem
chmod 600 keystore/jwt_private.pem
```

### 6.4 有用的命令

```bash
# 查看容器资源使用
docker stats

# 清理未使用的镜像
docker image prune -a

# 查看容器详细信息
docker inspect nexus-backend

# 进入容器
docker exec -it nexus-backend bash

# 数据库备份
docker exec nexus-db pg_dump -U postgres nexusarchive > backup.sql

# 数据库恢复
docker exec -i nexus-db psql -U postgres nexusarchive < backup.sql
```

### 6.5 端口说明

| 端口 | 服务 | 外部访问 |
|------|------|----------|
| 80 | HTTP | 是（重定向） |
| 443 | HTTPS | 是 |
| 19090 | 后端 API | 否（容器内部） |
| 5432 | PostgreSQL | 否（容器内部） |
| 6379 | Redis | 否（容器内部） |

---

**文档版本**: 1.0
**最后更新**: 2025-01-12
