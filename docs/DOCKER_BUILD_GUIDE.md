# Docker 构建最佳实践指南

> 本文档记录了Docker构建中遇到的问题及解决方案，防止问题再次发生。

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
- `docker-compose.dev.yml:116` - 前端健康检查
- `docker-compose.dev.yml:90` - 后端健康检查
- `docker-compose.prod.yml:82` - 后端健康检查
- `docker-compose.prod.yml:105` - 前端健康检查

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

## 开发工作流

### 首次设置

```bash
# 1. 预先拉取基础镜像（一次性，约30分钟）
docker pull maven:3.9-eclipse-temurin-17
docker pull eclipse-temurin:17-jdk
docker pull node:20-alpine

# 2. 运行健康检查
./scripts/check-docker-health.sh

# 3. 启动开发环境
docker-compose -f docker-compose.dev.yml up -d
```

### 日常开发

```bash
# 启动服务
docker-compose -f docker-compose.dev.yml up -d

# 查看状态（应显示 healthy）
docker ps

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f
```

### 重建镜像（当依赖变化时）

```bash
# 前端（约3分钟）
docker-compose -f docker-compose.dev.yml build nexus-frontend

# 后端（约10分钟，前提是已预先拉取基础镜像）
docker-compose -f docker-compose.dev.yml build nexus-backend
```

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
| **代码变更** | 修改代码后，只重建需要的服务 |

## 性能对比

| 指标 | 修复前 | 修复后 | 改进 |
|------|--------|--------|------|
| 前端健康检查 | unhealthy | healthy | ✅ |
| 后端健康检查 | 未测试 | healthy | ✅ |
| 构建上下文 | 1.2GB | 337KB | ↓99.97% |
| 前端镜像大小 | 1.81GB | <400MB | ↓78% |
| 后端首次构建 | 数小时 | ~10分钟 | ↓95% |

## 故障排查

### 健康检查失败

```bash
# 检查健康状态
docker ps

# 查看健康检查日志
docker inspect nexus-frontend-dev | grep -A 10 Health

# 手动测试
docker exec nexus-frontend-dev wget -q --spider http://127.0.0.1:15175
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

## 参考资料

- [Docker Compose健康检查文档](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
- [.dockerignore最佳实践](https://docs.docker.com/engine/reference/builder/#dockerignore-file)
- [Alpine Linux与IPv6](https://wiki.alpinelinux.org/wiki/IPv6)

---

**最后更新**: 2026-01-04
**维护者**: 开发团队
