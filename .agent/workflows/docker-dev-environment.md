---
description: Docker 开发环境配置与维护
---

# Docker 开发环境 SOP

## 概述

NexusArchive 采用全 Docker 化开发环境，一条命令启动全栈服务。

---

## 🚀 快速开始

### 启动开发环境

```bash
./scripts/dev-start.sh
```

### 停止开发环境

```bash
./scripts/dev-stop.sh
```

### 查看日志

```bash
./scripts/dev-logs.sh
```

---

## 服务架构

| 服务 | 容器名 | 宿主机端口 | 说明 |
|------|--------|-----------|------|
| PostgreSQL | nexus-db-dev | 54321 | 开发数据库 |
| Redis | nexus-redis-dev | 16379 | 缓存服务 |
| Backend | nexus-backend-dev | 19090 | Spring Boot API |
| Frontend | nexus-frontend-dev | 15175 | Vite 开发服务器 |

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.dev.yml` | 开发环境编排 |
| `.env.dev` | 开发环境变量 |

---

## 常用命令

```bash
# 查看容器状态
docker-compose -f docker-compose.dev.yml ps

# 重启单个服务
docker-compose -f docker-compose.dev.yml restart nexus-backend

# 进入容器
docker exec -it nexus-backend-dev bash

# 查看后端日志
docker-compose -f docker-compose.dev.yml logs -f nexus-backend

# 清理重建
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml up -d --build
```

---

## 数据管理

### 数据库连接

```bash
docker exec -it nexus-db-dev psql -U postgres -d nexusarchive
```

### 数据备份

```bash
docker exec nexus-db-dev pg_dump -U postgres nexusarchive > backup.sql
```

### 数据恢复

```bash
docker exec -i nexus-db-dev psql -U postgres -d nexusarchive < backup.sql
```

---

## 故障排除

### 容器启动失败

```bash
# 查看详细日志
docker-compose -f docker-compose.dev.yml logs nexus-backend

# 检查镜像构建
docker-compose -f docker-compose.dev.yml build --no-cache nexus-backend
```

### 端口被占用

```bash
lsof -ti :19090 | xargs kill -9
lsof -ti :15175 | xargs kill -9
```

### 清空数据库重来

```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

---

## 与生产环境对比

| 项目 | 开发环境 | 生产环境 |
|------|---------|---------|
| Compose 文件 | `docker-compose.dev.yml` | `docker-compose.prod.yml` |
| 环境变量 | `.env.dev` | `.env.prod` |
| 后端构建 | 多阶段构建（容器内编译） | 预构建镜像 |
| 前端 | Vite 热重载 | Nginx 静态服务 |
