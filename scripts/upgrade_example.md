## 升级/补丁流程示例

1) 备份：
```
PGHOST=... PGPORT=... PGUSER=... PGPASSWORD=... ./scripts/backup_postgres.sh nexusarchive ./backups
```

2) 执行迁移脚本（按版本顺序）：
```
psql -h ... -p ... -U ... -d nexusarchive -f db/migrate/Vx__*.sql
```

3) 替换包/镜像：
- 容器：更新镜像标签并 `docker-compose pull && docker-compose up -d`，或 `helm upgrade ...`
- 离线包：替换后端 JAR、前端 dist

4) 重启服务：`./scripts/stop.sh && ./scripts/start.sh`（或容器重启）

5) 验证：`/api/health`、`/api/ops/self-check`，关键功能冒烟。

回滚：使用备份恢复数据库，并回退到旧包/旧镜像。
