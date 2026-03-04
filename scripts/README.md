一旦我所属的文件夹有所变化，请更新我。
本目录集中存放运维与本地开发脚本。
用于备份恢复、健康检查、交付与启动控制。

## 文件清单

### 核心开发脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `create-module.sh` | DDD 模块生成 | 自动生成四层模块结构（api/app/domain/infra）|
| `dev.sh` | 启动开发环境 | 一键启动 DB+Redis+Backend+Frontend（后端未就绪时失败退出；可接管已有监听进程，避免误判） |
| `dev-stop.sh` | 停止开发环境 | 停止所有开发服务 |
| `dev-logs.sh` | 查看开发日志 | 实时查看应用日志 |

### 数据管理脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `db-dump.sh` | 导出数据库 | 离开公司前导出数据 |
| `db-load.sh` | 导入数据库 | 回到家后导入数据 |
| `db-reset.sh` | 重置数据库 | 删除 volume 并重新初始化 |

### 部署脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `deploy.sh` | 服务器部署 | 一键部署到服务器 |
| `deploy-prod.sh` | 生产发布（systemd） | 备份数据库 + 拉代码 + 构建 + 重启 + 验证 |
| `build-offline-bundle.sh` | 构建离线包 | 构建离线安装包 |
| `rollback-prod.sh` | 生产回滚 | 回滚到上一版本 |

### 运维脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `auth_smoke.sh` | 认证健康检查 | 验证登录流程 |
| `backup-dev-data.sh` | 开发数据备份 | 备份开发环境数据 |
| `backup_postgres.sh` | PostgreSQL 备份 | 数据库备份 |
| `restore_postgres.sh` | PostgreSQL 恢复 | 数据库恢复 |
| `prod_attachment_audit.sh` | 生产附件巡检 | 判断“数据缺失 vs 文件缺失”，并探测可恢复来源 |
| `prod_attachment_gate.sh` | 生产附件发布门禁 | 执行附件巡检并在缺失文件超阈值时阻断发布 |
| `prod_attachment_repair.sh` | 生产附件回补 | 基于缺失清单自动找回文件并生成/可选执行 DB 修复 SQL |
| `prod_attachment_external_recovery.sh` | 生产附件外部回补 | 基于 unresolved.tsv 从外部备份源定位文件并生成二次回补 SQL，支持可控占位回补 |
| `check-collection-batch-sequence.sh` | 数据库序列健康检查 | 检测并修复 `collection_batch_id_seq` 序列回退问题（防止主键冲突） |
| `check-docker-health.sh` | Docker 健康检查 | 检查容器健康状态 |
| `create_demo_files.sh` | 生成演示文件 | 创建测试数据 |
| `diagnose_fonds_scope.sh` | 全宗范围诊断 | 诊断全宗配置 |
| `generate_signature_keys.sh` | 生成签名密钥 | JWT 签名密钥生成 |
| `self_check.sh` | 环境自检 | 校验环境配置 |
| `validate-schema.sh` | Schema 验证 v2.0 | 自动对比 Entity 定义与数据库结构（Maven驱动） |
| `verify_attachment_download_smoke.sh` | 附件下载冒烟校验 | 启动后校验关键附件下载接口（防止历史 404 复发） |
| `verify_panorama_attachments.sh` | 全景附件一致性校验 | 校验 demo 源目录与运行时目录一致性，并可选验接口头 |
| `verify-sequence-check-is-called.sh` | 序列检查脚本自测 | 验证 `check-collection-batch-sequence.sh` 能正确识别 `is_called=false` 隐患场景 |

### 升级脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `upgrade-dev.sh` | 开发环境升级 | 升级开发环境 |
| `upgrade-prod.sh` | 生产环境升级 | 升级生产环境 |

### 测试脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `run-e2e-tests.sh` | E2E 测试 | 运行端到端测试 |

### Git 钩子

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `git-pre-commit-hook.sh` | 提交前检查 | Git pre-commit 钩子 |

### 设置脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `setup.sh` | 环境设置 | 初始化开发环境 |
| `setup-java.sh` | Java 环境设置 | 配置 Java 环境 |

### SQL 脚本

| 文件 | 功能 | 使用场景 |
| --- | --- | --- |
| `seed_roles.sql` | 初始化角色 | 创建初始角色和权限 |
| `update_role_perms.sql` | 更新角色权限 | 更新角色权限映射 |
| `prod_attachment_audit.sql` | 生产附件巡检 SQL | 统计附件表、空路径、目标 file_id 元数据 |
| `prod_attachment_repair_template.sql` | 生产附件修复 SQL 模板 | 作为回补 SQL 模板，实际执行文件由修复脚本生成 |

## 快速命令参考

```bash
# DDD 模块创建
./scripts/create-module.sh Payment    # 生成 payment 模块
./scripts/create-module.sh Voucher    # 生成 voucher 模块

# 开发环境
npm run dev        # 启动开发环境
npm run dev:stop   # 停止开发环境

# 数据同步
npm run db:dump    # 导出数据
npm run db:load    # 导入数据
npm run db:reset   # 重置数据库

# 部署
npm run deploy     # 部署到服务器
bash scripts/deploy-prod.sh  # 生产发布（systemd 模式）
```

## 相关文档

- [后端模块创建 SOP](../docs/architecture/backend-module-creation-sop.md)
- [模块边界规范](../docs/architecture/module-boundaries.md)
- [启动指南](../docs/deployment/启动指南.md)
- [Docker 开发指南](../docs/deployment/docker-production.md)
