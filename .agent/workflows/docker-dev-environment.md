# Docker 开发环境维护 SOP（Host Dev + Docker Deps）& 生产发布 SOP（镜像 TAG）

本指南定义两条工作流：

* **开发（Host Dev）**：宿主机运行 Frontend（Vite 15175）+ Backend（Maven 19090），Docker 只运行依赖（Postgres/Redis）。
* **生产发布（Release）**：构建并发布固定 TAG 的镜像，生产只换 TAG 拉起，可回滚。

---

## 🚀 Quick Start：一键升级脚本

> **推荐方式**：使用 `scripts/upgrade-dev.sh` 自动完成开发环境升级。

### 首次安装（新机器）

```bash
git clone git@github.com:ericforai/NexusArchive.git
cd NexusArchive
./scripts/upgrade-dev.sh
```

### 日常升级（已有环境）

```bash
cd NexusArchive
./scripts/upgrade-dev.sh
```

### 脚本功能

| 步骤 | 功能 | 说明 |
|-----|------|-----|
| 0 | 检查 Docker | 确保 Docker Desktop 已启动 |
| 1 | `git pull` | 拉取最新代码 |
| 2 | 智能 npm install | 仅当 `package.json` 有变化时才执行 |
| 3 | 启动 Docker 依赖 | 确保数据库和 Redis 运行 |
| 4 | 提示/自动启动 | 询问是否自动启动后端和前端 |

> 如需手动操作，请参考下方详细步骤。

---

## Part A｜开发环境 SOP（Host Dev + Docker 依赖）

### A1. 拉取代码（Pull）

```bash
git pull origin main
```

### A2. 构建项目（Build）（可选）

> Host Dev 日常调试不要求先构建生产产物；只有在你需要验证生产构建是否通过时才执行。

#### 后端（可选）

```bash
cd nexusarchive-java
mvn clean package -DskipTests
cd ..
```

#### 前端（可选）

```bash
npm install
npm run build
```

### A3. 启动/重启 Docker 依赖（Restore）

> 仅启动 DB/Redis（你的真实现状）。**不要用 `docker-compose.dev.yml down -v` 作为默认动作。**

```bash
docker compose -f docker-compose.deps.yml up -d
docker compose -f docker-compose.deps.yml ps
```

#### 常用端口（对齐你的现状）

| 服务              | 运行位置   | 端口                                                                                     |
| --------------- | ------ | -------------------------------------------------------------------------------------- |
| 前端（Vite）        | 宿主机    | [http://localhost:15175](http://localhost:15175)                                       |
| 后端（Spring Boot） | 宿主机    | [http://localhost:19090（context-path=/api）](http://localhost:19090（context-path=/api）) |
| PostgreSQL      | Docker | localhost:54321                                                                        |
| Redis           | Docker | localhost:16379                                                                        |

### A4. 启动后端（Host）

建议用环境文件或脚本保证连接的是 Docker DB/Redis（避免"数据库对不上"）：

```bash
# 推荐：用 .env.host.dev 注入（示例）
set -a
source .env.host.dev
set +a

cd nexusarchive-java
mvn spring-boot:run
```

### A5. 启动前端（Host）

```bash
npm run dev
# 端口为 15175（以 vite.config.ts 为准）
```

### A6. 开发自检（强制执行，30 秒定位大部分问题）

```bash
# DB
docker compose -f docker-compose.deps.yml exec -T nexus-db \
  psql -U postgres -d nexusarchive -c "select 1;"

# Redis
docker compose -f docker-compose.deps.yml exec -T nexus-redis \
  redis-cli ping

# 后端健康（如果没有 /api/health，就换成一个稳定 GET 接口）
curl -fsS http://localhost:19090/api/health || echo "backend not ready"

# 前端是否启动
curl -I http://localhost:15175 | head -n 1 || true
```

---

## Part B｜数据恢复 SOP（Data Restore）

### B1. 恢复数据库（Postgres 在 Docker）

```bash
docker exec -i nexus-db psql -U postgres -d nexusarchive < database.sql
```

### B2. 恢复归档文件（Bind Mount 在宿主机）

你的归档目录是：`./nexusarchive-java/data`（映射到容器 `/app/data`）

```bash
rm -rf ./nexusarchive-java/data/*
cp -r ./backup/files/* ./nexusarchive-java/data/
```

---

## Part C｜常见问题（Troubleshooting）

* **数据库对不上 / 查不到表**
  先确认后端连接的是否是 `localhost:54321`（Host Dev 模式），而不是容器内的 `nexus-db:5432`（那是 Prod-like/生产模式）。

* **接口不对 / 404**
  确认后端 context-path 仍是 `/api`，前端请求走 `/api/*`，且 Vite proxy 目标是 `http://localhost:19090`。

* **JWT 密钥缺失**

  ```bash
  bash nexusarchive-java/scripts/generate_jwt_keys.sh
  ```

* **要清空数据库重新来**（谨慎）
  这会删除 named volume（不可逆）：

  ```bash
  docker compose -f docker-compose.deps.yml down
  docker volume rm nexus-db-data
  docker compose -f docker-compose.deps.yml up -d
  ```

---

## Part D｜生产发布 SOP（Release：镜像 TAG，替换旧发版流程）

> 这一节**替换**你当前"同步代码到服务器/现场 build/用 latest"的发布方式。
> 开发工作流不变，只改发布。

### D0. 发布前准备（必须）

#### D0.1 Git 状态检查

```bash
git status                    # 确认无未提交变更
git log -1 --oneline          # 确认当前提交
git pull origin main          # 拉取最新代码
```

#### D0.2 后端 JAR 构建

> ⚠️ **必须先构建 JAR**，否则 Docker 镜像构建会因找不到 `target/*.jar` 而失败。

```bash
cd nexusarchive-java
mvn clean package -DskipTests
# 确认产物存在
ls -la target/nexusarchive-backend-*.jar
cd ..
```

#### D0.3 前端依赖检查（可选）

> 如果前端依赖有更新，先清理再构建：

```bash
rm -rf node_modules package-lock.json
npm install
```

### D1. 定义 TAG（固定版本）

```bash
TAG=$(git rev-parse --short HEAD)
echo "发布版本: $TAG"
```

### D2. 构建镜像（同 TAG）

```bash
# backend
docker build -t nexusarchive-backend:$TAG -f nexusarchive-java/Dockerfile nexusarchive-java

# web
docker build -t nexusarchive-web:$TAG -f Dockerfile.frontend.prod .

# 验证镜像
docker images | grep nexusarchive
```

### D3. 推送镜像（如果你使用镜像仓库）

按你的 registry 执行 `docker tag` / `docker push`（略）。

### D4. 生产服务器发布（只换 TAG）

#### D4.1 准备 `.env.prod` 配置文件

生产服务器准备 `.env.prod`（完整模板）：

```env
# ========== 版本控制 ==========
TAG=xxxxxxx

# ========== 数据库配置 ==========
DB_HOST=nexus-db
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=your_secure_password_here

# ========== Redis 配置 ==========
REDIS_HOST=nexus-redis
REDIS_PORT=6379

# ========== 安全配置 ==========
SM4_KEY=your_sm4_key_base64_here
JWT_SECRET=your_jwt_secret_here

# ========== 应用配置 ==========
SPRING_PROFILES_ACTIVE=prod
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://yourdomain.com

# ========== 文件存储 ==========
FILE_STORAGE_PATH=/app/data
```

#### D4.2 执行发布

```bash
# 拉取镜像（如果使用镜像仓库）
docker compose -f docker-compose.prod.yml --env-file .env.prod pull

# 启动服务
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 查看启动日志
docker compose -f docker-compose.prod.yml logs -f
```

### D5. 发布后自检（固定检查项）

```bash
# 1. 检查容器状态
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

# 2. 检查前端访问
curl -I http://localhost/ | head -n 1

# 3. 检查后端 API
curl -fsS http://localhost/api/health || echo "api not ready"

# 4. 检查数据库连接
docker exec -i nexus-db psql -U postgres -d nexusarchive -c "select 1;"

# 5. 检查 Flyway 迁移状态（可选）
docker compose -f docker-compose.prod.yml logs nexus-backend | grep -i "flyway"
```

### D6. 日志与监控

```bash
# 实时查看所有日志
docker compose -f docker-compose.prod.yml logs -f

# 仅后端日志
docker compose -f docker-compose.prod.yml logs -f nexus-backend

# 仅前端日志
docker compose -f docker-compose.prod.yml logs -f nexus-frontend

# 容器资源使用
docker stats --no-stream
```

### D7. 回滚

把 `.env.prod` 的 TAG 改回上一个版本，重复 D4。

```bash
# 1. 修改 .env.prod 中的 TAG 为上一版本
# TAG=previous_version_hash

# 2. 重新拉起服务
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 3. 验证回滚成功
docker compose -f docker-compose.prod.yml ps
```

---

## Part E｜离线环境发布 SOP（Air-gapped Release）

> 针对无法连接外网/Docker Registry 的私有化部署环境。

### E1. 在有网环境导出镜像

```bash
# 1. 完成 D0-D2 步骤（构建 JAR 和镜像）

# 2. 导出为 tar 包
docker save -o release-$TAG.tar nexusarchive-backend:$TAG nexusarchive-web:$TAG
# 产生 release-xxxx.tar 文件
```

### E2. 传输到离线环境

通过 U盘 / 堡垒机将 `release-$TAG.tar` 和 `.env.prod` 传输到生产服务器。

### E3. 导入镜像并启动

```bash
# 1. 导入镜像
docker load -i release-$TAG.tar

# 2. 修改 .env.prod 中的 TAG
# TAG=xxxx (与 tar 包版本一致)

# 3. 启动服务 (无需 pull)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

## Part F｜SSL/HTTPS 配置（可选）

### F1. 准备证书

```bash
mkdir -p nginx/ssl
# 将证书文件放入目录
# - fullchain.pem（证书链）
# - privkey.pem（私钥）
```

### F2. 启用 HTTPS

编辑 `nginx/nginx.prod.conf`，取消 HTTPS server 块的注释，并注释 HTTP 块中的静态文件服务。

### F3. 验证

```bash
curl -I https://yourdomain.com/
curl -fsS https://yourdomain.com/api/health
```
