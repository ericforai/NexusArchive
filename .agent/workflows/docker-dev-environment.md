---
description: 整合开发流程 - 拉取代码、构建、还原 Docker 环境
---

# Docker 开发环境维护 SOP (Pull-Build-Restore)

本指南说明如何同步最新代码，并构建运行完整的 Docker 开发环境。

## 1. 拉取代码 (Pull)

确保本地仓库与远程同步：

```bash
git pull origin main
```

## 2. 构建项目 (Build)

### 后端构建 (Maven)
```bash
cd nexusarchive-java
mvn clean package -DskipTests
cd ..
```

### 前端构建 (Vite)
```bash
npm install
npm run build
```

## 3. 还原/重启 Docker 环境 (Restore)

使用开发环境专用配置启动：

### 干净启动 (推荐)
停止现有容器并移除卷（清空数据），然后重新构建并启动：

```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d --build
```

### 常用端口
- **前端**: http://localhost:15175
- **后端**: http://localhost:18080
- **数据库 (Postgres)**: localhost:15432
- **Redis**: localhost:16379

## 4. 数据恢复 (Data Restore)

如需从备份还原数据，请参考以下逻辑（详见 [optimization_phase3_private_deployment.md](file:///Users/user/nexusarchive/docs/planning/optimization_phase3_private_deployment.md)）：

1. 获取备份文件 `database.sql` 和 `files/` 目录。
2. 执行还原命令：
```bash
# 恢复数据库
docker exec -i nexus-db psql -U postgres -d nexusarchive < database.sql

# 恢复文件存储
rm -rf ./data/files
cp -r ./backup/files ./data/files
```

## 5. 常见问题 (Troubleshooting)

- **端口冲突**: 确保本地未启动原生 PostgreSQL 或 Redis 服务，以免占用 Docker 映射端口。
- **构建失败**: 检查 `Dockerfile.frontend` 和 `nexusarchive-java/Dockerfile` 是否存在且正确。
- **依赖问题**: 前端构建建议先执行 `npm install`。
