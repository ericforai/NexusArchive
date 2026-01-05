# NexusArchive 开发环境指南

本文档详细说明开发环境的配置和使用。

**更新日期**: 2025-01-05
**架构模式**: 混合模式（Docker 基础设施 + 本地应用）

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                         Host Machine                             │
│                                                                  │
│  ┌──────────────┐          ┌──────────────┐                     │
│  │   Backend    │          │   Frontend   │                     │
│  │  (Local JVM) │          │  (Vite HMR)  │                     │
│  │    :19090    │          │    :15175    │                     │
│  └──────┬───────┘          └──────┬───────┘                     │
│         │                         │                             │
│         └───────────┬─────────────┘                             │
│                     │                                           │
│  ┌──────────────────▼──────────────────┐                       │
│  │         Docker Network              │                       │
│  │  ┌────────────┐    ┌────────────┐   │                       │
│  │  │ nexus-db   │    │nexus-redis │   │                       │
│  │  │  :5432     │    │   :6379    │   │                       │
│  │  └─────┬──────┘    └─────┬──────┘   │                       │
│  └────────┼────────────────┼───────────┘                       │
│           │54321:5432      │16379:6379                          │
└───────────┼────────────────┼───────────────────────────────────┘
            │                │
       localhost:54321   localhost:16379
```

**设计理念**：
- **DB + Redis 在 Docker**：避免本地安装，数据隔离，易于重置
- **应用在本地**：快速热重载，调试方便，性能最佳
- **多 Mac 同步**：通过 `db/seed-data.sql` 实现数据同步

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.infra.yml` | 基础设施配置（PostgreSQL + Redis） |
| `docker-compose.app.yml` | 应用服务配置（仅服务器部署使用） |
| `.env.local` | 本地开发环境变量 |
| `scripts/dev.sh` | 开发环境启动脚本 |
| `scripts/dev-stop.sh` | 开发环境停止脚本 |
| `scripts/db-dump.sh` | 数据库导出脚本 |
| `scripts/db-load.sh` | 数据库导入脚本 |

---

## 快速启动

### 一键启动

```bash
npm run dev
```

自动完成：
1. 启动 PostgreSQL + Redis（Docker）
2. 等待数据库就绪
3. 导入 seed data（首次或 volume 为空时）
4. 启动后端（本地）
5. 启动前端（本地）

### 停止环境

```bash
npm run dev:stop
```

---

## 服务端口

| 服务 | 容器内端口 | 宿主机端口 | 协议 |
|------|-----------|-----------|------|
| PostgreSQL | 5432 | **54321** | TCP |
| Redis | 6379 | **16379** | TCP |
| Backend | - | **19090** | HTTP |
| Frontend | - | **15175** | HTTP |

> **注意**: DB 和 Redis 使用非标准端口（54321、16379），避免与本地服务冲突。

---

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:15175 | Vite 开发服务器 |
| 后端 API | http://localhost:19090/api | REST API |
| API 文档 | http://localhost:19090/api/swagger-ui.html | Swagger UI |
| 数据库 | localhost:54321 | PostgreSQL |
| Redis | localhost:16379 | Redis CLI |

### 默认账号

- **管理员**: `admin` / `admin123`

---

## 数据同步（多台 Mac）

### 工作流程

```bash
# 1. 离开公司前 - 导出数据
npm run db:dump
git add db/seed-data.sql
git commit -m "db: update seed data"
git push

# 2. 回到家后 - 拉取并导入
git pull
npm run db:load
```

### 重置数据库

```bash
npm run db:reset
```

> **警告**: 这会删除所有数据并重新初始化！

---

## 手动操作

### 仅启动基础设施

```bash
docker-compose -f docker-compose.infra.yml up -d
```

### 单独启动后端

```bash
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 单独启动前端

```bash
npm run dev:vite
```

### 数据库操作

```bash
# 连接数据库
docker exec -it nexus-db psql -U postgres -d nexusarchive

# 备份数据库
docker exec nexus-db pg_dump -U postgres nexusarchive > backup.sql

# 恢复数据库
docker exec -i nexus-db psql -U postgres -d nexusarchive < backup.sql
```

### Redis 操作

```bash
# 连接 Redis
docker exec -it nexus-redis redis-cli

# 查看所有 key
docker exec nexus-redis redis-cli KEYS "*"

# 清空所有数据
docker exec nexus-redis redis-cli FLUSHALL
```

---

## 故障排除

### 后端启动失败

```bash
# 查看后端日志
tail -f backend.log

# 检查数据库连接
docker exec nexus-db pg_isready -U postgres
```

### 前端报 500 错误

**原因**: 后端未启动或未就绪
**解决**:
```bash
curl http://localhost:19090/api/health
```

### 端口被占用

```bash
# 查看端口占用
lsof -ti :19090
lsof -ti :15175
lsof -ti :54321
lsof -ti :16379

# 杀掉进程
lsof -ti :PORT | xargs kill -9
```

### 数据库连接失败

```bash
# 检查 Docker 容器状态
docker ps | grep nexus

# 重启数据库
docker restart nexus-db

# 等待数据库就绪
docker exec nexus-db pg_isready -U postgres
```

### 清空重来

```bash
# 停止并删除所有容器和数据卷
npm run dev:stop
docker volume rm nexusarchive-db nexusarchive-redis

# 重新启动
npm run dev
```

---

## 开发技巧

### 后端热重载

Spring Boot DevTools 已启用，修改 Java 文件后自动重启。

### 前端 HMR

Vite 支持热模块替换，修改代码后自动刷新浏览器。

### 查看实时日志

```bash
# 后端
tail -f backend.log

# Docker 容器
docker-compose -f docker-compose.infra.yml logs -f
```

---

## 相关文档

- [生产部署指南](docker-production.md)
- [环境管理手册](环境管理手册.md)
- [故障排除指南](故障排除指南.md)
