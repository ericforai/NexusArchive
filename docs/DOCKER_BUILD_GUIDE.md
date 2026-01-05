# Docker 构建最佳实践指南

> 本文档记录了Docker构建中遇到的问题及解决方案，防止问题再次发生。

**更新日期**: 2025-01-05

---

## 架构说明

当前采用**混合开发模式**：
- **开发环境**: DB + Redis 在 Docker，应用在本地（支持热重载）
- **生产环境**: 所有服务在 Docker 中

```bash
# 开发环境启动
npm run dev

# 生产环境部署
npm run deploy
```

---

## 问题回顾与修复

### 问题1: 健康检查失败 ✅ 已修复

**症状**:
```bash
docker ps  # 显示 (unhealthy)
```

**根本原因**: Alpine Linux中 `localhost` DNS解析优先返回IPv6地址 `::1`，但Vite/Nginx仅监听IPv4。

**修复方案**:
```yaml
# ❌ 错误配置
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://localhost:15175"]

# ✅ 正确配置
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://127.0.0.1:15175"]
```

**修改文件**:
- `docker-compose.app.yml` - 生产环境健康检查
- `Dockerfile.frontend.prod` - 前端 Dockerfile

### 问题2: 构建上下文过大 ✅ 已修复

**症状**: 每次构建复制1.2GB文件，耗时4.8秒

**根本原因**: `.dockerignore` 不完整，导致备份文件、日志、测试结果被复制

**修复方案**: 完善 `.dockerignore`
```diff
+ *.tar.gz
+ *.zip
+ test-*.json
+ test-*.txt
+ *.tmp
```

**效果**:
- 构建上下文: 1.2GB → 337KB (↓99.97%)
- COPY时间: 4.8秒 → 1.1秒 (↓77%)

### 问题3: 镜像拉取极慢 ⚠️ 需手动操作

**症状**: 拉取 `maven:3.9-eclipse-temurin-17` 耗时30.6分钟

**解决方案**: 首次构建前预先拉取
```bash
docker pull maven:3.9-eclipse-temurin-17
docker pull eclipse-temurin:17-jdk
docker pull node:20-alpine
```

**可选优化**: 配置Docker镜像加速（中国区）
```json
// /etc/docker/daemon.json
{
  "registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]
}
```

---

## 配置文件

| 文件 | 用途 |
|------|------|
| `docker-compose.infra.yml` | 基础设施（PostgreSQL + Redis） |
| `docker-compose.app.yml` | 应用服务（Backend + Frontend） |
| `.env.local` | 本地开发环境变量 |
| `.env.server` | 生产环境变量 |

---

## 开发工作流

### 首次设置

```bash
# 1. 预先拉取基础镜像（一次性，约30分钟）
docker pull postgres:14-alpine
docker pull redis:7-alpine
docker pull eclipse-temurin:17-jdk
docker pull node:20-alpine

# 2. 启动开发环境
npm run dev
```

### 日常开发

```bash
# 启动所有服务
npm run dev

# 仅启动基础设施（DB + Redis）
docker-compose -f docker-compose.infra.yml up -d

# 停止所有服务
npm run dev:stop
```

### 生产部署

```bash
# 方式一：使用部署脚本（推荐）
npm run deploy

# 方式二：手动部署
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml build
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

---

## 防止问题再次发生

### 自动检查

项目中包含自动检查脚本：`scripts/check-docker-health.sh`

**建议**: 在提交代码前运行此脚本
```bash
./scripts/check-docker-health.sh
```

**检查内容**:
1. `.dockerignore` 配置是否完整
2. 健康检查是否使用 `127.0.0.1` 而非 `localhost`
3. 是否存在超大文件（应添加到 `.dockerignore`）
4. Docker基础镜像拉取建议

### CI/CD集成

可以在CI流程中添加检查：
```yaml
# .github/workflows/docker-check.yml
- name: Check Docker configuration
  run: ./scripts/check-docker-health.sh
```

### 规则总结

| 场景 | 规则 |
|------|------|
| **健康检查** | 始终使用 `127.0.0.1`，避免 `localhost` |
| **Docker上下文** | 排除所有 `.log`、`.tar.gz`、测试结果文件 |
| **镜像拉取** | 首次构建前预先拉取基础镜像 |
| **代码变更** | 修改代码后，开发环境支持热重载，无需重建 |

---

## 性能对比

| 指标 | 修复前 | 修复后 | 改进 |
|------|--------|--------|------|
| 前端健康检查 | unhealthy | healthy | ✅ |
| 后端健康检查 | 未测试 | healthy | ✅ |
| 构建上下文 | 1.2GB | 337KB | ↓99.97% |
| 前端镜像大小 | 1.81GB | <400MB | ↓78% |
| 后端首次构建 | 数小时 | ~10分钟 | ↓95% |

---

## 故障排查

### 健康检查失败

```bash
# 检查健康状态
docker ps

# 查看健康检查日志
docker inspect nexus-frontend | grep -A 10 Health

# 手动测试
docker exec nexus-frontend wget -q --spider http://127.0.0.1:80
```

### 构建缓慢

```bash
# 检查构建上下文大小
docker build -t test-context . 2>&1 | grep "build context"

# 清理Docker缓存
docker system prune -a

# 预先拉取镜像
docker pull maven:3.9-eclipse-temurin-17
```

### 开发环境问题

```bash
# 后端启动失败
tail -f backend.log

# 前端报 500 错误
curl http://localhost:19090/api/health

# 数据库连接失败
docker exec nexus-db pg_isready -U postgres
```

---

## 参考资料

- [Docker Compose健康检查文档](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
- [.dockerignore最佳实践](https://docs.docker.com/engine/reference/builder/#dockerignore-file)
- [Alpine Linux与IPv6](https://wiki.alpinelinux.org/wiki/IPv6)

---

## 相关文档

- [开发环境指南](docs/deployment/docker-development.md)
- [生产部署指南](docs/deployment/docker-production.md)
- [启动指南](docs/deployment/启动指南.md)
