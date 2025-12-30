---
description: 开发环境启动流程 - Docker 优先，本地备选
---

# 开发环境启动工作流

## 启动方式对比

| 方式 | 命令 | 适用场景 |
|------|------|---------|
| **Docker 全栈** | `./scripts/dev-start.sh` | 网络稳定，无需本地环境 |
| **本地一键** | `./scripts/dev-local.sh` | 网络不稳定，已有 JDK/Node |

---

## 方式一：Docker 全栈（推荐）

// turbo
```bash
./scripts/dev-start.sh
```

自动完成：构建镜像 → 启动全部服务 → 健康检查

> ⚠️ 首次需拉取 Docker 镜像，网络不稳定时可能失败

---

## 方式二：本地一键启动

// turbo
```bash
./scripts/dev-local.sh
```

自动完成：
1. 启动数据库和 Redis（Docker）
2. 启动后端（本地 Maven）
3. 启动前端（本地 npm）
4. 健康检查

**停止服务**：
```bash
./scripts/dev-local-stop.sh
```

---

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:15175 |
| 后端 API | http://localhost:19090/api |
| 数据库 | localhost:54321 |
| Redis | localhost:16379 |

---

## 常见问题

### Docker 启动失败（网络超时）

改用本地方式：
```bash
./scripts/dev-local.sh
```

### 端口被占用

```bash
lsof -ti :19090 | xargs kill -9
lsof -ti :15175 | xargs kill -9
```

### 前端报 500 错误

后端未启动。检查：
```bash
curl -s http://localhost:19090/api/health
```
