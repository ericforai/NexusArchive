# 统一开发环境设计方案

**日期**: 2025-01-05
**作者**: Claude + User
**状态**: ✅ 已完成实施

---

## 一、背景与问题

### 1.1 当前问题

1. **环境混乱**：本地环境和 Docker 环境并存，数据经常混淆
2. **两台 Mac 不同步**：公司和家里的开发环境不一致
3. **部署出错**：每次部署到服务器都需要修改配置，经常出错
4. **数据库连错风险**：端口不固定，容易连到错误的数据库

### 1.2 设计目标

1. **统一环境配置**：所有机器使用同一套基础设施配置
2. **数据可同步**：通过 Git 同步测试数据
3. **一键部署**：服务器部署自动化，无需手动修改配置
4. **防错机制**：固定端口，避免连错数据库

---

## 二、架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Git Repository                          │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │   代码      │  │  配置模板    │  │   Seed Data          │   │
│  │  (src/)     │  │ (.env.example)│  │ (db/seed-data.sql)  │   │
│  └─────────────┘  └──────────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
           │ pull/push                    │ pull/push
           ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│   公司 Mac          │         │   家里 Mac          │
├─────────────────────┤         ├─────────────────────┤
│ ┌─────────────────┐ │         │ ┌─────────────────┐ │
│ │ Docker Infra    │ │         │ │ Docker Infra    │ │
│ │  ┌──────────┐   │ │         │ │  ┌──────────┐   │ │
│ │  │  DB      │   │ │         │ │  │  DB      │   │ │
│ │  │  Redis   │   │ │         │ │  │  Redis   │   │ │
│ │  └──────────┘   │ │         │ │  └──────────┘   │ │
│ └─────────────────┘ │         │ └─────────────────┘ │
│ ┌─────────────────┐ │         │ ┌─────────────────┐ │
│ │ 本地应用        │ │         │ │ 本地应用        │ │
│ │  ┌──────────┐   │ │         │ │  ┌──────────┐   │ │
│ │  │ Backend  │   │ │         │ │  │ Backend  │   │ │
│ │  │ Frontend │   │ │         │ │  │ Frontend │   │ │
│ │  └──────────┘   │ │         │ │  └──────────┘   │ │
│ └─────────────────┘ │         │ └─────────────────┘ │
└─────────────────────┘         └─────────────────────┘
           │                               │
           │         pull                  │
           └───────────────┬───────────────┘
                           ▼
                  ┌─────────────────┐
                  │   生产服务器    │
                  ├─────────────────┤
                  │ ┌─────────────┐ │
                  │ │ Docker 全栈  │ │
                  │ │ DB+Redis+   │ │
                  │ │ Backend+Web │ │
                  │ └─────────────┘ │
                  └─────────────────┘
```

### 2.2 环境区分

| 环境 | DB 端口 | Redis 端口 | 应用运行方式 |
|------|---------|-----------|-------------|
| 本地开发 | 54321 | 16379 | 本地 (mvn/npm) |
| 生产部署 | 5432 (内部) | 6379 (内部) | Docker 容器 |

---

## 三、目录结构

```
nexusarchive/
├── db/
│   ├── migration/              # Flyway 迁移脚本（已存在）
│   └── seed-data.sql           # 测试数据快照（新增，提交 Git）
│
├── docker-compose.infra.yml    # 基础设施：DB + Redis（新增）
├── docker-compose.app.yml      # 应用服务：部署用（新增）
│
├── .env.example                # 环境变量模板（新增，提交 Git）
├── .env.local                  # 本地开发（gitignore）
├── .env.server                 # 服务器部署（gitignore）
│
├── scripts/
│   ├── dev.sh                  # 本地开发启动（新增）
│   ├── dev-stop.sh             # 停止开发环境（新增）
│   ├── db-dump.sh              # 导出数据（新增）
│   ├── db-load.sh              # 导入数据（新增）
│   ├── db-reset.sh             # 重置数据库（新增）
│   └── deploy.sh               # 服务器部署（新增）
│
├── nexusarchive-java/
│   └── src/main/resources/
│       ├── application.yml     # 调整默认端口
│       └── application-dev.yml # 开发环境配置
│
└── package.json                # 添加快捷命令
```

---

## 四、配置文件详情

### 4.1 docker-compose.infra.yml

基础设施配置，本地开发和服务器通用：

```yaml
services:
  nexus-db:
    image: postgres:14-alpine
    container_name: nexus-db
    environment:
      POSTGRES_DB: ${DB_NAME:-nexusarchive}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "${DB_PORT:-54321}:5432"
    volumes:
      - nexus-db-data:/var/lib/postgresql/data
      - ./db/seed-data.sql:/docker-entrypoint-initdb.d/99-seed.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]

  nexus-redis:
    image: redis:7-alpine
    container_name: nexus-redis
    ports:
      - "${REDIS_PORT:-16379}:6379"
    volumes:
      - nexus-redis-data:/data

volumes:
  nexus-db-data:
    name: nexusarchive-db
  nexus-redis-data:
    name: nexusarchive-redis
```

### 4.2 .env.example

环境变量模板，提交到 Git：

```bash
# ========== 数据库配置 ==========
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=54321                     # 本地开发端口

# ========== Redis 配置 ==========
REDIS_PORT=16379

# ========== 应用端口 ==========
SERVER_PORT=19090
FRONTEND_PORT=15175

# ========== 安全配置 ==========
SM4_KEY=a1b2c3d4e5f67890a1b2c3d4e5f67890
AUDIT_LOG_HMAC_KEY=dev_hmac_key

# ========== Spring Profile ==========
SPRING_PROFILES_ACTIVE=dev

# ========== Seed Data ==========
SEED_AUTO_IMPORT=true

# ========== CORS ==========
APP_SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:15175
```

### 4.3 application.yml 调整

修改默认端口，确保连到 Docker 的 DB：

```yaml
spring:
  data:
    redis:
      port: ${REDIS_PORT:16379}       # 改为 16379

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:54321}/${DB_NAME:nexusarchive}...
    # 改为默认端口 54321
```

---

## 五、数据同步机制

### 5.1 数据导出 (scripts/db-dump.sh)

```bash
#!/bin/bash
# 从 Docker PostgreSQL 导出完整数据

DB_NAME=${DB_NAME:-nexusarchive}

docker exec nexus-db pg_dump -U postgres \
    --no-owner --no-acl \
    --schema-public \
    $DB_NAME > db/seed-data.sql

echo "✅ 数据已导出到 db/seed-data.sql"
```

### 5.2 数据导入 (scripts/db-load.sh)

```bash
#!/bin/bash
# 从 seed-data.sql 导入数据

DB_NAME=${DB_NAME:-nexusarchive}

# 清空现有数据
docker exec nexus-db psql -U postgres -d $DB_NAME \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 导入 seed data
cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d $DB_NAME

echo "✅ 数据已导入"
```

### 5.3 首次启动自动导入

PostgreSQL 容器的 `POSTGRES_INITDB_SCRIPTS` 机制会在首次创建时自动执行 `seed-data.sql`。

---

## 六、启动脚本

### 6.1 本地开发启动 (scripts/dev.sh)

```bash
#!/bin/bash
set -e

# 检查 .env.local
if [ ! -f .env.local ]; then
    cp .env.example .env.local
fi

# 启动基础设施
docker-compose -f docker-compose.infra.yml --env-file .env.local up -d

# 等待数据库就绪
until docker exec nexus-db pg_isready -U postgres > /dev/null 2>&1; do sleep 1; done

# 检查是否需要导入 seed data
USER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM archive_user" 2>/dev/null || echo "0")
if [ "$USER_COUNT" -eq "0" ] && [ -f db/seed-data.sql ]; then
    cat db/seed-data.sql | docker exec -i nexus-db psql -U postgres -d nexusarchive > /dev/null
fi

# 启动后端
cd nexusarchive-java
mvn spring-boot:run > ../backend.log 2>&1 &
echo $! > .backend.pid
cd ..

# 等待后端就绪
until curl -s http://localhost:19090/api/health > /dev/null 2>&1; do sleep 2; done

# 启动前端
npm run dev &
echo $! > .frontend.pid

echo "✅ 开发环境启动完成"
echo "  前端: http://localhost:15175"
echo "  后端: http://localhost:19090/api"
```

### 6.2 服务器部署 (scripts/deploy.sh)

```bash
#!/bin/bash
set -e

if [ ! -f .env.server ]; then
    echo "❌ .env.server 不存在！"
    exit 1
fi

# 拉取最新代码
git pull

# 停止旧服务
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server down

# 构建镜像
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server build

# 启动服务
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d

echo "✅ 部署完成"
```

---

## 七、工作流程

### 7.1 场景 1：公司开发 → 回家继续

**公司离开前**：
```bash
# 1. 导出当前数据
npm run db:dump

# 2. 提交到 Git
git add db/seed-data.sql
git commit -m "sync: update seed data"
git push
```

**回到家**：
```bash
# 1. 拉取最新代码
git pull

# 2. 导入数据（自动或手动）
npm run db:load

# 3. 启动开发环境
npm run dev
```

### 7.2 场景 2：回家开发 → 回公司继续

**回家离开前**：
```bash
# 1. 导出当前数据
npm run db:dump

# 2. 提交到 Git
git add db/seed-data.sql
git add .                    # 别忘了提交代码更改
git commit -m "sync: update seed data from home"
git push
```

**回到公司**：
```bash
# 1. 拉取最新代码
git pull

# 2. 导入数据
npm run db:load

# 3. 启动开发环境
npm run dev
```

### 7.3 场景 3：部署到生产

```bash
# 服务器上
cd /path/to/nexusarchive
git pull
npm run deploy
```

### 7.4 场景 4：全新环境初始化

```bash
git clone <repo> nexusarchive
cd nexusarchive
cp .env.example .env.local
npm run dev
```

---

## 八、package.json 命令

```json
{
  "scripts": {
    "dev": "bash scripts/dev.sh",
    "dev:stop": "bash scripts/dev-stop.sh",
    "db:dump": "bash scripts/db-dump.sh",
    "db:load": "bash scripts/db-load.sh",
    "db:reset": "bash scripts/db-reset.sh",
    "deploy": "bash scripts/deploy.sh"
  }
}
```

---

## 九、.gitignore 更新

```gitignore
# 环境变量文件
.env.local
.env.server

# PID 文件
.backend.pid
.frontend.pid

# 日志
backend.log

# 提交到 Git
.env.example
db/seed-data.sql
```

---

## 十、实施计划

| 步骤 | 任务 | 文件 | 预估时间 |
|------|------|------|----------|
| 1 | 创建基础设施配置 | docker-compose.infra.yml | 10 min |
| 2 | 创建应用配置 | docker-compose.app.yml | 10 min |
| 3 | 创建环境变量模板 | .env.example | 5 min |
| 4 | 修改默认端口 | application.yml | 5 min |
| 5 | 编写数据脚本 | scripts/db-*.sh | 30 min |
| 6 | 编写启动脚本 | scripts/dev*.sh | 30 min |
| 7 | 编写部署脚本 | scripts/deploy.sh | 15 min |
| 8 | 更新 package.json | package.json | 5 min |
| 9 | 更新 .gitignore | .gitignore | 5 min |
| 10 | 测试验证 | - | 60 min |
| 11 | 写用户文档 | docs/guides/... | 30 min |

**总预估时间**: 约 3 小时

---

## 十一、后续优化

1. **Seed Data 增量同步**：当前是全量覆盖，可优化为增量更新
2. **数据版本控制**：在 seed-data.sql 中添加版本信息
3. **自动化测试**：部署后自动运行健康检查
4. **回滚机制**：保存历史版本的 seed data

---

## 十二、附录

### 12.1 端口分配

| 服务 | 本地开发 | 服务器 |
|------|----------|--------|
| PostgreSQL | 54321 | 5432 (内部) |
| Redis | 16379 | 6379 (内部) |
| Backend API | 19090 | 19090 |
| Frontend | 15175 | 80 |

### 12.2 Volume 命名

- `nexusarchive-db` - 开发/测试数据库
- `nexus-redis` - Redis 缓存

### 12.3 相关文档

- `docs/DOCKER_BUILD_GUIDE.md` - Docker 构建指南
- `docs/guides/development/` - 开发指南
