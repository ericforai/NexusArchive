---
description: 生产部署标准操作流程 - 按SOP指导完成部署
---

# 生产部署工作流

当用户触发 `/deploy` 时，按以下步骤执行部署：

## 前置检查

1. 确认部署版本号和变更内容
2. 确认 CR（变更请求）是否已审批
3. 确认目标环境（测试/生产/演示）
4. 确认是否需要加载演示数据（demo profile）

## 步骤 1：信创基线检测

提醒用户在目标服务器执行基线检测：

```bash
cd deploy/offline && ./install.sh --check-only
```

确认以下检测项：
- [ ] CPU 架构 (x86_64/aarch64)
- [ ] OS 版本 (麒麟/统信/CentOS)
- [ ] 数据库版本 (PostgreSQL/达梦/人大金仓)
- [ ] 磁盘空间 ≥ 500GB
- [ ] 内存 ≥ 16GB

## 步骤 2：数据库备份确认

提醒用户确认数据库备份：
- [ ] 完整备份已完成
- [ ] 备份恢复测试已验证
- [ ] DDL 变更已审批（如有）

## 步骤 3：制品准备

检查制品是否已构建：
// turbo
```bash
ls -la nexusarchive-java/target/nexusarchive-backend-*.jar 2>/dev/null || echo "未找到JAR包，需要先构建"
```

如需构建：
```bash
cd nexusarchive-java && mvn clean package -DskipTests
```

## 步骤 4：部署策略确认

向用户确认部署方式和环境配置：

### 环境配置选择
| 环境 | SPRING_PROFILES_ACTIVE | 演示数据 |
|------|------------------------|----------|
| 开发环境 | `dev` | ❌ 不加载 |
| 演示环境 | `demo,prod` | ✅ 加载泊冉集团数据 |
| 生产环境 | `prod` | ❌ 不加载 |

### 部署方式选择
- **离线安装包**（推荐）：适用于内网环境
- **Docker 部署**：适用于容器化环境

## 步骤 5：执行部署

### 方式一：离线安装包部署（推荐）

```bash
cd deploy/offline

# 首次安装
./install.sh

# 升级部署
./upgrade.sh
```

### 方式二：Docker 部署

```bash
# 构建镜像（如需）
docker build -t nexusarchive/backend:latest -f nexusarchive-java/Dockerfile .
docker build -t nexusarchive/frontend:latest -f Dockerfile .

# 启动服务
docker-compose -f deploy/docker-compose.yml up -d
```

### 环境变量配置（重要）

演示环境需设置：
```bash
export SPRING_PROFILES_ACTIVE=demo,prod
```

## 步骤 6：部署后验证

### 健康检查
// turbo
```bash
# 离线部署检查（端口19090）
curl -s http://localhost:19090/api/actuator/health | jq .

# Docker部署检查（端口8080）
curl -s http://localhost:8080/api/actuator/health | jq .
```

### 冒烟测试
提醒用户执行：
- [ ] 管理员登录（admin/admin123）
- [ ] 查询历史归档
- [ ] 上传测试文件
- [ ] OFD 预览

### 演示环境验证（如启用demo profile）
- [ ] 确认泊冉集团全宗数据已加载
- [ ] 确认16条档案记录可查询
- [ ] 确认借阅/审批流程数据存在

## 步骤 7：部署完成确认

- [ ] 所有检查通过
- [ ] 监控告警已配置
- [ ] 相关方已通知

## 回滚（如需要）

如部署后发现问题，执行紧急回滚：

### 离线安装包回滚（手动）
```bash
# 1. 停止服务
systemctl stop nexusarchive

# 2. 恢复应用文件（从 upgrade.sh 生成的备份目录）
cp /opt/nexusarchive_backup_YYYYMMDD_HHMMSS/app.jar /opt/nexusarchive/
cp -r /opt/nexusarchive_backup_YYYYMMDD_HHMMSS/frontend /opt/nexusarchive/
cp /opt/nexusarchive_backup_YYYYMMDD_HHMMSS/.env /opt/nexusarchive/

# 3. 恢复数据库（需DBA介入）
# 参照 SOP 文档中的 "5.4 迁移回滚" 章节

# 4. 启动服务
systemctl start nexusarchive
```

### Docker 回滚
```bash
# 停止当前版本
docker-compose -f deploy/docker-compose.yml down

# 恢复上一版本镜像
docker tag nexusarchive/backend:previous nexusarchive/backend:latest
docker-compose -f deploy/docker-compose.yml up -d
```


---

参考文档：
- [离线部署简易手册](file:///Users/user/nexusarchive/docs/guides/离线部署简易手册.md)
- [production-deployment-sop.md](file:///Users/user/nexusarchive/docs/deployment/production-deployment-sop.md)
