# 全量迁移与附件预览故障排查 Runbook

## 1. 适用范围
- 生产环境执行“整库 + 文件全量覆盖迁移”。
- 页面出现附件 404、500、无限加载、内容错配。
- 重启后问题复发。

## 2. 先看结论（高频根因）
1. 数据库 `storage_path` 有记录，但物理文件不存在。
2. `ARCHIVE_ROOT_PATH` 未显式配置，运行目录漂移。
3. 迁移包包含 macOS 元数据（`._*`、`.DS_Store`），导致恢复异常。
4. 健康检查地址填错（应使用 `/api/health`）。
5. 启动脚本误触发 seed 导入，重启后覆盖修复结果。

## 3. 10 分钟快速定位
1. 验证服务是否可用：
```bash
curl -sS http://127.0.0.1:19090/api/health
```
2. 验证归档根目录配置：
```bash
grep -n '^ARCHIVE_ROOT_PATH=' /etc/nexusarchive/nexusarchive.env || echo "未配置"
```
3. 验证目标文件是否存在（替换为真实文件）：
```bash
ls -lh /opt/nexusarchive/nexusarchive-java/data/archives/uploads/demo/<文件名>.pdf
```
4. 验证数据库路径（以文件 ID 为例）：
```bash
docker exec -i nexus-db psql -U postgres -d nexusarchive -c \
"select id, archival_code, storage_path, file_name from arc_file_content where id='<file_id>';"
```
5. 判断一致性：
- DB `storage_path` + `ARCHIVE_ROOT_PATH` 拼接后存在文件：进入前端验收。
- 不存在：先补文件，再重启服务。

## 4. 迁移执行标准（必须）
1. Actions 工作流：`Migrate Prod Full Overwrite via SSH`。
2. 参数：
- `server_package_path`: 服务器绝对路径。
- `migration_package_sha256`: 打包脚本输出的 SHA256。
- `confirm_text`: `I_UNDERSTAND_FULL_OVERWRITE`。
- `healthcheck_url`: `https://www.digivoucher.cn/api/health`。
3. 触发方式：只用 `Run workflow`（不要 `Re-run jobs` 旧记录）。

## 5. 典型故障 -> 处理动作
1. `404 Physical file not found`：
- 查 DB `storage_path` 与物理文件是否一致。
- 从迁移包定向解出目标文件补齐，重启服务。
2. 附件一直加载：
- 检查后端响应 `Content-Length` 是否与物理文件长度一致。
3. 内容显示错文件：
- 检查同名文件是否存在多份副本，统一运行目录与源目录文件。
4. 重启后复发：
- 检查 `scripts/dev.sh` 或启动流程是否每次导入 seed。

## 6. 防复发清单（上线前/后）
1. 服务器 `nexusarchive.env` 显式配置 `ARCHIVE_ROOT_PATH`。
2. 使用已修复打包脚本（排除 `._*`、`.DS_Store`）。
3. 迁移后执行页面级验收：目标全宗 + 目标附件逐一打开。
4. 执行一次“重启回归”：
```bash
systemctl restart nexusarchive
curl -sS http://127.0.0.1:19090/api/health
```
5. 保留本次备份路径（DB + files）用于快速回滚。

## 7. 常用应急命令
1. 从迁移包提取单个文件（按关键字匹配）：
```bash
PKG=/opt/nexusarchive/migration_packages/full_migration_xxx.tar.gz
ROOT=/opt/nexusarchive/nexusarchive-java/data/archives
TMP=$(mktemp -d /tmp/nx_fix_XXXXXX)
INNER=$(tar -tzf "$PKG" | grep -E '(^|/)files/archive_root\.tgz$' | head -n 1)
tar -xzf "$PKG" -C "$TMP" "$INNER"
MEMBER=$(tar -tzf "$TMP/$INNER" | grep '<关键字>' | grep -v '/\._' | head -n 1)
tar -xzf "$TMP/$INNER" -C "$ROOT" "$MEMBER"
```
2. 检查端口与容器：
```bash
ss -lntp | grep 19090
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

## 8. 文档关联
- 迁移操作：`docs/guides/生产全量覆盖迁移指南.md`
- 故障复盘：`docs/knowledge/2026-02-12-panorama-attachment-mismatch-postmortem.md`
