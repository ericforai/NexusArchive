# NexusArchive Docker 生产部署指南

本文档说明如何使用 Docker 部署生产环境。

**更新日期**: 2025-01-05

---

## 架构概览

```
┌────────────────────────────────────────────────────────────────┐
│                    Production Server                            │
│                                                                │
│  ┌─────────┐  ┌─────────┐  ┌─────────────────┐  ┌───────────┐ │
│  │ Frontend│──│ Backend │──│   PostgreSQL    │  │  Redis    │ │
│  │  :80    │  │ :19090  │──│     :5432       │  │  :6379    │ │
│  │ (Nginx) │  │         │  │                 │  │           │ │
│  └─────────┘  └─────────┘  └─────────────────┘  └───────────┘ │
│                            ▲                   ▲              │
│                            │                   │              │
└────────────────────────────┼───────────────────┼──────────────┘
                             │                   │
                        Docker Internal Network
```

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.infra.yml` | 基础设施（PostgreSQL + Redis） |
| `docker-compose.app.yml` | 应用服务（Backend + Frontend） |
| `.env.server` | 生产环境变量（敏感，不提交 Git） |

---

## 部署步骤

### 方式一：一键部署（推荐）

```bash
npm run deploy
```

脚本会自动：
1. 构建后端和前端镜像
2. 上传到服务器
3. 重启服务

### 方式二：手动部署

#### 1. 准备配置文件

在服务器创建 `.env.server`：

```env
# 数据库
DB_HOST=nexus-db
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=<强密码>

# Redis
REDIS_HOST=nexus-redis
REDIS_PORT=6379

# 安全配置
SM4_KEY=<Base64编码的SM4密钥>
AUDIT_LOG_HMAC_KEY=<HMAC密钥>
JWT_PUBLIC_KEY_LOCATION=file:/app/keystore/jwt_public.pem
JWT_PRIVATE_KEY_LOCATION=file:/app/keystore/jwt_private.pem

# 应用配置
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=19090
FRONTEND_PORT=80

# CORS
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://yourdomain.com

# 病毒扫描
VIRUS_SCAN_TYPE=clamav
VIRUS_SCAN_HOST=localhost
VIRUS_SCAN_PORT=3310

# Schema 验证
SCHEMA_VALIDATION_ENABLED=false
SCHEMA_VALIDATION_FAIL=false
```

#### 2. 上传文件

```bash
scp .env.server server:~/nexusarchive/
```

#### 3. 启动服务

```bash
ssh server
cd ~/nexusarchive
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

#### 4. 验证部署

```bash
# 检查容器
docker ps

# 健康检查
curl -s http://localhost:19090/api/health

# 前端访问
curl -I http://localhost/
```

---

## 服务管理

### 查看日志

```bash
# 所有服务
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml logs -f

# 仅后端
docker logs nexus-backend -f

# 仅数据库
docker logs nexus-db -f
```

### 重启服务

```bash
# 重启后端
docker restart nexus-backend

# 重启前端
docker restart nexus-frontend
```

### 更新版本

```bash
# 方式一：使用部署脚本
npm run deploy

# 方式二：手动更新
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml build
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml up -d
```

---

## 数据备份

### 数据库备份

```bash
docker exec nexus-db pg_dump -U postgres nexusarchive > backup_$(date +%Y%m%d).sql
```

### 归档文件备份

```bash
tar -czf archives_$(date +%Y%m%d).tar.gz /path/to/archives
```

### 自动备份脚本

添加到 crontab：
```bash
# 每日凌晨 2 点备份数据库
0 2 * * * docker exec nexus-db pg_dump -U postgres nexusarchive > /backup/db_$(date +\%Y\%m\%d).sql
```

---

## 回滚

### 版本回滚

```bash
# 1. 停止当前服务
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml down

# 2. 修改镜像版本
# 编辑 .env.server 中的 TAG=v1.0.0

# 3. 重新启动
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

### 数据库回滚

```bash
docker exec -i nexus-db psql -U postgres -d nexusarchive < backup_20250105.sql
```

---

## 安全建议

1. **密码强度**：所有密码使用 32 位以上随机字符串
2. **网络隔离**：数据库和 Redis 仅容器内网通信
3. **HTTPS**：生产必须启用 SSL/TLS
4. **定期备份**：配置每日自动备份
5. **密钥管理**：JWT 密钥和 SM4 密钥妥善保管
6. **防火墙**：仅开放必要端口（80, 443）

---

## 常见问题

### Q: 容器启动失败？

```bash
# 查看详细日志
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml logs nexus-backend

# 常见原因：
# 1. 数据库未就绪 → 等待健康检查
# 2. 端口冲突 → 检查端口占用
# 3. 配置错误 → 检查 .env.server
```

### Q: 数据库连接失败？

```bash
# 检查数据库容器
docker ps | grep nexus-db

# 检查数据库日志
docker logs nexus-db

# 测试连接
docker exec nexus-db pg_isready -U postgres
```

### Q: 如何清空重来？

```bash
# 停止并删除所有容器和数据卷
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml down -v

# 重新启动
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

---

## 相关文档

- [开发环境指南](docker-development.md)
- [环境管理手册](环境管理手册.md)
- [故障排除指南](故障排除指南.md)
