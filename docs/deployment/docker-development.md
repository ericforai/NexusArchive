# NexusArchive Docker 开发指南

本文档详细说明 Docker 开发环境的配置和使用。

---

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ nexus-db │  │  redis   │  │ backend  │  │ frontend │ │
│  │  :5432   │  │  :6379   │  │  :19090  │  │  :15175  │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
│       ▲              ▲              ▲            ▲       │
└───────┼──────────────┼──────────────┼────────────┼───────┘
        │              │              │            │
   54321:5432     16379:6379    19090:19090  15175:15175
        │              │              │            │
   ─────┴──────────────┴──────────────┴────────────┴─────
                      Host (localhost)
```

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.dev.yml` | 开发环境编排配置 |
| `.env.dev` | 开发环境变量 |
| `nexusarchive-java/Dockerfile` | 后端多阶段构建 |
| `Dockerfile.frontend` | 前端开发镜像 |

---

## 快速启动

```bash
# 一键启动
./scripts/dev-start.sh

# 停止
./scripts/dev-stop.sh

# 查看日志
./scripts/dev-logs.sh
```

---

## 服务端口

| 服务 | 容器内端口 | 宿主机端口 |
|------|-----------|-----------|
| PostgreSQL | 5432 | **54321** |
| Redis | 6379 | **16379** |
| Backend | 19090 | **19090** |
| Frontend | 15175 | **15175** |

---

## 常用命令

### 容器管理

```bash
# 查看状态
docker-compose -f docker-compose.dev.yml ps

# 重启单个服务
docker-compose -f docker-compose.dev.yml restart nexus-backend

# 重建镜像
docker-compose -f docker-compose.dev.yml up -d --build nexus-backend
```

### 日志查看

```bash
# 所有服务
docker-compose -f docker-compose.dev.yml logs -f

# 仅后端
docker-compose -f docker-compose.dev.yml logs -f nexus-backend
```

### 数据库操作

```bash
# 连接数据库
docker exec -it nexus-db-dev psql -U postgres -d nexusarchive

# 备份
docker exec nexus-db-dev pg_dump -U postgres nexusarchive > backup.sql

# 恢复
docker exec -i nexus-db-dev psql -U postgres -d nexusarchive < backup.sql
```

---

## 后端多阶段构建

`nexusarchive-java/Dockerfile` 采用多阶段构建：

```dockerfile
# Stage 1: 编译（Maven）
FROM maven:3.9-eclipse-temurin-17 AS builder
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: 运行（JRE）
FROM eclipse-temurin:17-jdk
COPY --from=builder /build/target/*.jar app.jar
```

**优势**：
- 无需本地安装 Maven
- 镜像体积更小（仅包含 JRE）
- 构建环境一致性

---

## 故障排除

### 后端启动失败

```bash
# 查看详细日志
docker-compose -f docker-compose.dev.yml logs nexus-backend

# 常见原因
# 1. 编译错误 → 检查 Java 代码
# 2. 数据库连接失败 → 确认 nexus-db 已启动并健康
```

### 清空重来

```bash
# 删除容器和数据卷
docker-compose -f docker-compose.dev.yml down -v

# 重新启动
docker-compose -f docker-compose.dev.yml up -d
```
