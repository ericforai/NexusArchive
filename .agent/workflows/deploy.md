---
description: 生产部署标准操作流程 - 按SOP指导完成部署
---

# 生产部署工作流

## 前置检查

1. 确认部署版本号和变更内容
2. 确认目标环境（测试/生产/演示）
3. 确认数据库备份已完成

---

## 方式一：Docker 部署（推荐）

### 1. 准备配置文件

在服务器上创建 `.env.prod`：

```env
# 版本控制
TAG=latest

# 数据库
DB_HOST=nexus-db
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=your_secure_password

# Redis
REDIS_HOST=nexus-redis
REDIS_PORT=6379

# 安全
SM4_KEY=your_sm4_key_base64
JWT_PUBLIC_KEY_PATH=/app/keystore/jwt_public.pem

# 应用
SPRING_PROFILES_ACTIVE=prod
```

### 2. 上传配置到服务器

// turbo
```bash
scp docker-compose.prod.yml .env.prod server:~/nexusarchive/
```

### 3. 启动服务

```bash
ssh server
cd ~/nexusarchive
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 4. 健康检查

// turbo
```bash
# 检查容器状态
docker-compose -f docker-compose.prod.yml ps

# 检查 API
curl -s http://localhost/api/health

# 检查前端
curl -I http://localhost/
```

---

## 方式二：离线安装包

适用于无法连接外网的私有化环境。

### 1. 构建离线包

```bash
cd deploy/offline && ./build_offline_package.sh
```

### 2. 传输到服务器

通过 U盘/堡垒机传输 `nexusarchive-offline-*.tar.gz`

### 3. 安装

```bash
tar -xzf nexusarchive-offline-*.tar.gz
cd nexusarchive-offline
./install.sh
```

---

## 回滚

### Docker 回滚

```bash
# 1. 修改 .env.prod 中的 TAG 为上一版本
# 2. 重启服务
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 离线包回滚

```bash
cd deploy/offline && ./rollback.sh
```

---

## 日志查看

```bash
# 所有服务日志
docker-compose -f docker-compose.prod.yml logs -f

# 仅后端
docker-compose -f docker-compose.prod.yml logs -f nexus-backend

# 仅前端
docker-compose -f docker-compose.prod.yml logs -f nexus-frontend
```

---

## 参考文档

- [Docker 生产部署指南](file:///Users/user/nexusarchive/docs/deployment/docker-production.md)
- [离线部署手册](file:///Users/user/nexusarchive/docs/guides/离线部署简易手册.md)
