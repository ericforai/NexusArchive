# NexusArchive 生产服务器升级标准操作流程 (SOP)

> **目标**: 5-10 分钟完成生产升级

---

## 一、日常升级（5分钟）

### 前提条件
- SSH 已登录服务器
- `.env.server` 已配置好（首次需要设置）

### 执行命令
```bash
cd /root/nexusarchive
./scripts/upgrade-prod.sh
```

### 脚本自动完成
| 步骤 | 操作 |
|-----|------|
| 1 | 检查前置条件 |
| 2 | 可选备份数据库 |
| 3 | 拉取最新代码 |
| 4 | 构建 JAR |
| 5 | 构建 Docker 镜像 |
| 6 | 更新 TAG 并重启 |
| 7 | 验证服务 |

---

## 二、首次部署（30分钟）

### 1. 克隆代码
```bash
git clone git@github.com:ericforai/NexusArchive.git /root/nexusarchive
cd /root/nexusarchive
```

### 2. 创建 .env.server
```bash
cp .env.server.template .env.server
nano .env.server  # 修改密码和密钥
```

### 3. 生成 JWT 密钥
```bash
mkdir -p nexusarchive-java/keystore
openssl genrsa -out nexusarchive-java/keystore/jwt_private.pem 2048
openssl rsa -in nexusarchive-java/keystore/jwt_private.pem -pubout -out nexusarchive-java/keystore/jwt_public.pem
```

### 4. 构建并启动
```bash
./scripts/upgrade-prod.sh
```

---

## 三、数据迁移

### 从本地备份恢复
```bash
# 1. 本地打包
./scripts/backup-dev-data.sh

# 2. 上传到服务器
scp nexusarchive_backup_xxxx.tar.gz root@服务器IP:/root/nexusarchive/

# 3. 服务器恢复
tar -xzf nexusarchive_backup_xxxx.tar.gz
docker exec -i nexus-db psql -U postgres -d nexusarchive < backup_xxxx/database.sql
cp -r backup_xxxx/data/* nexusarchive-java/data/
```

---

## 四、回滚

```bash
./scripts/rollback-prod.sh
```

---

## 五、问题排查

| 问题 | 命令 |
|-----|------|
| 查看后端日志 | `docker logs nexus-backend --tail 50` |
| 查看前端日志 | `docker logs nexus-frontend --tail 20` |
| 检查服务状态 | `docker compose -f docker-compose.infra.yml -f docker-compose.app.yml ps` |
| 测试 API | `curl http://localhost/api/health` |

---

## 六、环境变量说明

| 变量 | 说明 | 必须 |
|-----|------|------|
| `DB_PASSWORD` | 数据库密码 | ✅ |
| `SM4_KEY` | 国密加密密钥 | ✅ |
| `AUDIT_LOG_HMAC_KEY` | 审计日志密钥 | ✅ |
| `VIRUS_SCAN_TYPE` | 病毒扫描（skip/clamav） | ✅ |
| `YONSUITE_APP_KEY` | 用友集成 | 可选 |
| `YONSUITE_APP_SECRET` | 用友集成 | 可选 |
