一旦我所属的文件夹有所变化，请更新我。
本目录存放 GitHub Actions 工作流。
用于自动化测试与检查。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `architecture-check.yml` | 配置文件 | 架构检查（按模块/规则） |
| `architecture.yml` | 配置文件 | 架构相关检查入口 |
| `complexity-check.yml` | 配置文件 | 复杂度检查 |
| `deploy-prod-manual.yml` | 配置文件 | 生产手动部署 |
| `deploy-prod-via-ssh.yml` | 配置文件 | 通过 SSH 部署生产（含附件巡检强制门禁） |
| `frontend-quality.yml` | 配置文件 | 前端增量质量检查（ESLint/TS） |
| `integration-gate.yml` | 配置文件 | 联调门禁（YonSuite/Delivery 等外部依赖场景） |
| `migrate-prod-via-ssh.yml` | 配置文件 | 生产迁移 |
| `prod-attachment-audit-via-ssh.yml` | 配置文件 | 生产附件巡检（远程 SSH 执行 Runbook） |
| `prod-attachment-external-recovery-via-ssh.yml` | 配置文件 | 生产附件外部回补（按 unresolved.tsv 定位外部备份并生成二次 SQL，支持占位回补开关） |
| `prod-attachment-repair-via-ssh.yml` | 配置文件 | 生产附件回补（dry-run/apply，自动归档修复结果） |
| `permission-tests.yml` | 配置文件 | permission-tests 配置 |
| `update-modules.yml` | 配置文件 | 模块清单自动更新 |
