## 离线安装包结构（建议）

```
nexusarchive-offline/
├── backend/
│   └── nexusarchive-backend-2.0.0.jar
├── frontend/
│   └── dist/                # 前端静态文件
├── db/
│   ├── init.sql             # 初始化脚本（用户/角色/权限/配置表）
│   └── migrate/             # 迁移脚本（升级用）
├── config/
│   ├── application.yml.example
│   └── .env.example
├── scripts/
│   ├── self_check.sh        # 环境自检（Java/端口/磁盘/DB 连通）
│   ├── start.sh             # 启动（可调用 java -jar + 配置）
│   ├── stop.sh
│   ├── backup_postgres.sh
│   └── restore_postgres.sh
├── license/
│   └── license.lic          # 授权文件放置位置
└── README.md                # 安装手册
```

### 部署步骤概览
1. 运行 `scripts/self_check.sh` 检查依赖（Java/磁盘/端口/DB）。
2. 配置 `config/application.yml` 或 `.env`（DB、存储、license 路径）。
3. 初始化数据库：`psql -f db/init.sql`（或使用现有数据库）。
4. 启动后端：`./scripts/start.sh`；前端可用静态文件部署 Nginx。
5. 验证健康检查 `/api/health` 和 `/api/ops/self-check`。
