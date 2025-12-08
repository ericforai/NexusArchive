## 健康检查与监控建议

### 已提供的接口
- `/api/health`：基础状态 + uptime + license 信息。
- `/api/health/self-check`（HealthController）：可扩展 DB/磁盘检测。
- `/api/ops/self-check`（OpsController）：磁盘容量、JVM 堆、数据库连通性。

### 监控建议
- 暴露 Prometheus 指标：在有网络环境下添加 `micrometer-registry-prometheus` 并开启 `/actuator/prometheus`，纳入 CPU/内存/HTTP/DB 连接池指标。
- 日志采集：推荐 filebeat/ELK 或 Loki/Promtail；将 `backend.log`、`frontend.log` 纳入采集。
- 告警：存储使用率、DB 连接池耗尽、接口 5xx 比例、License 即将到期（预留 LicenseService 到期提醒）。

### 自检脚本
- 可结合 `curl /api/ops/self-check` 做定时探活，校验 DB、磁盘空间阈值，并在失败时告警。
