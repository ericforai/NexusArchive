一旦我所属的文件夹有所变化，请更新我。
本目录集中存放运维与本地开发脚本。
用于备份恢复、健康检查、交付与启动控制。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `auth_smoke.sh` | 运维脚本 | 认证流程健康检查 |
| `backup_postgres.sh` | 运维脚本 | PostgreSQL 备份 |
| `create_demo_files.sh` | 运维脚本 | 生成演示文件 |
| `delivery_gatekeeper.cjs` | 交付工具 | 交付前检查与阻断 |
| `delivery_gatekeeper_v2.cjs` | 交付工具 | 新版交付检查 |
| `dev-start.sh` | 开发脚本 | 本地开发启动 |
| `restart-services.sh` | 运维脚本 | 重启全套服务 |
| `restore_postgres.sh` | 运维脚本 | PostgreSQL 恢复 |
| `seed_roles.sql` | SQL 脚本 | 初始化角色与权限 |
| `self_check.sh` | 运维脚本 | 自检与环境校验 |
| `start.sh` | 运维脚本 | 启动服务 |
| `stop.sh` | 运维脚本 | 停止服务 |
| `update_role_perms.sql` | SQL 脚本 | 更新角色权限映射 |
| `upgrade_example.md` | 文档 | 升级示例说明 |
| `verify_cold_start.cjs` | 验证脚本 | 冷启动稳定性验证 |
| `verify_health_resilience.cjs` | 验证脚本 | 健康与韧性验证 |
