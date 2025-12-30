# NexusArchive Docker 生产部署指南

本文档说明如何使用 Docker 部署生产环境。

---

## 架构概览

```
┌─────────────────────────────────────────────────┐
│              Production Server                   │
│                                                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────────────┐  │
│  │ Nginx   │──│ Backend │──│   PostgreSQL    │  │
│  │  :80    │  │ :19090  │  │     :5432       │  │
│  └─────────┘  └─────────┘  └─────────────────┘  │
│       │                           │              │
│       └───────────┬───────────────┘              │
│                   │                              │
│              ┌─────────┐                         │
│              │  Redis  │                         │
│              │  :6379  │                         │
│              └─────────┘                         │
└──────────────────────────────────────────────────┘
```

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.prod.yml` | 生产环境编排 |
| `.env.prod` | 生产环境变量（敏感，不提交 Git） |

---

## 部署步骤

### 1. 准备配置文件

在服务器创建 `.env.prod`：

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
JWT_PUBLIC_KEY_PATH=/app/keystore/jwt_public.pem

# 应用配置
SPRING_PROFILES_ACTIVE=prod
```

### 2. 上传文件

```bash
scp docker-compose.prod.yml .env.prod server:~/nexusarchive/
```

### 3. 启动服务

```bash
ssh server
cd ~/nexusarchive
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 4. 验证部署

```bash
# 检查容器
docker-compose -f docker-compose.prod.yml ps

# 健康检查
curl -s http://localhost/api/health

# 前端访问
curl -I http://localhost/
```

---

## 服务管理

### 查看日志

```bash
# 所有服务
docker-compose -f docker-compose.prod.yml logs -f

# 仅后端
docker-compose -f docker-compose.prod.yml logs -f nexus-backend
```

### 重启服务

```bash
docker-compose -f docker-compose.prod.yml restart nexus-backend
```

### 更新版本

```bash
# 拉取最新镜像
docker-compose -f docker-compose.prod.yml pull

# 重建并启动
docker-compose -f docker-compose.prod.yml up -d
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

---

## 回滚

### 版本回滚

```bash
# 1. 停止当前服务
docker-compose -f docker-compose.prod.yml down

# 2. 修改镜像版本（或切换 TAG）
# 编辑 .env.prod 中的 TAG

# 3. 重新启动
docker-compose -f docker-compose.prod.yml up -d
```

### 数据库回滚

```bash
docker exec -i nexus-db psql -U postgres -d nexusarchive < backup_20251229.sql
```

---

## 安全建议

1. **密码强度**：所有密码使用 32 位以上随机字符串
2. **网络隔离**：数据库和 Redis 仅容器内网通信
3. **HTTPS**：生产必须启用 SSL/TLS
4. **定期备份**：配置每日自动备份
