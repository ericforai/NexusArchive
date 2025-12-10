---
description: 生产部署标准操作流程 - 按SOP指导完成部署
---

# 生产部署工作流

当用户触发 `/deploy` 时，按以下步骤执行部署：

## 前置检查

1. 确认部署版本号和变更内容
2. 确认 CR（变更请求）是否已审批
3. 确认目标环境（测试/生产）

## 步骤 1：信创基线检测

提醒用户在目标服务器执行基线检测：

```bash
./nexus-installer.sh --check-baseline
```

确认以下检测项：
- [ ] CPU 架构 (x86_64/aarch64)
- [ ] OS 版本 (麒麟/统信)
- [ ] 数据库版本
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
ls -la nexusarchive-java/target/*.jar 2>/dev/null || echo "未找到JAR包，需要先构建"
```

如需构建：
```bash
cd nexusarchive-java && mvn clean package -DskipTests
```

## 步骤 4：部署策略确认

向用户确认部署策略：
- **蓝绿部署**（推荐）：需要双倍资源，但可快速回滚
- **滚动更新**：资源占用少，但回滚较慢

## 步骤 5：执行部署

根据用户选择执行相应的部署命令。

### 传统部署（安装包）：
```bash
./nexus-installer.sh --mode=upgrade --backup-before-upgrade=true
```

### Docker 部署：
```bash
docker-compose -f deploy/docker-compose.yml up -d
```

## 步骤 6：部署后验证

### 健康检查
// turbo
```bash
curl -s http://localhost:8080/actuator/health | jq .
```

### 冒烟测试
提醒用户执行：
- [ ] 管理员登录
- [ ] 查询历史归档
- [ ] 上传测试文件
- [ ] OFD 预览

### 四性检测（合规必须项）
```bash
./nexus-cli.sh four-nature-check --sample=100
```

## 步骤 7：部署完成确认

- [ ] 所有检查通过
- [ ] 监控告警已配置
- [ ] 相关方已通知

## 回滚（如需要）

如部署后发现问题，执行紧急回滚：

```bash
# 1. 暂停归档入口
curl -X POST http://localhost:8080/api/system/pause-ingestion

# 2. 执行回滚
./nexus-installer.sh --rollback

# 3. 恢复入口
curl -X POST http://localhost:8080/api/system/resume-ingestion
```

---

参考文档：[production-deployment-sop.md](file:///Users/user/nexusarchive/docs/deployment/production-deployment-sop.md)
