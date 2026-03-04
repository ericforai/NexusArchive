#!/usr/bin/env bash
# ==============================================================================
# 生产环境发布脚本（systemd + 本机进程模式）
# 用途:
#   1) 备份数据库
#   2) 拉取最新代码
#   3) 构建前端/后端
#   4) 重启并检查 systemd 服务
#   5) 校验 Flyway 迁移与健康状态
#
# 使用:
#   bash scripts/deploy-prod.sh
#
# 可选环境变量:
#   PROJECT_ROOT            项目根目录（默认 /opt/nexusarchive）
#   SERVICE_NAME            systemd 服务名（默认 nexusarchive）
#   DB_CONTAINER            PostgreSQL 容器名（默认 nexus-db）
#   DB_HOST                 PostgreSQL 主机（默认 127.0.0.1）
#   DB_PORT                 PostgreSQL 端口（默认 54321）
#   DB_NAME                 数据库名（默认 nexusarchive）
#   DB_USER                 数据库用户（默认 postgres）
#   DB_PASSWORD             数据库密码（默认 pPSB3+HqgkqxM3WL）
#   ATTACHMENT_GATE_ENABLED 是否启用附件门禁（默认 1）
#   ATTACHMENT_GATE_SCAN_LIMIT 附件门禁扫描上限（默认 200000）
#   ATTACHMENT_GATE_SAMPLE_LIMIT 附件门禁恢复探测采样（默认 300）
#   ATTACHMENT_GATE_MAX_MISSING_ALLOWED 附件门禁允许的缺失文件数（默认 0）
#   ATTACHMENT_GATE_TARGET_FILE_ID 门禁巡检目标 file_id（默认 f4653466-b670-a083-acff-19ff6d55be02）
# ==============================================================================

set -euo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-/opt/nexusarchive}"
SERVICE_NAME="${SERVICE_NAME:-nexusarchive}"
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-54321}"
DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-pPSB3+HqgkqxM3WL}"
HEALTH_URL="${HEALTH_URL:-https://www.digivoucher.cn/api/health}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-120}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-5}"
SKIP_GIT_PULL="${SKIP_GIT_PULL:-0}"
ATTACHMENT_GATE_ENABLED="${ATTACHMENT_GATE_ENABLED:-1}"
ATTACHMENT_GATE_SCAN_LIMIT="${ATTACHMENT_GATE_SCAN_LIMIT:-200000}"
ATTACHMENT_GATE_SAMPLE_LIMIT="${ATTACHMENT_GATE_SAMPLE_LIMIT:-300}"
ATTACHMENT_GATE_MAX_MISSING_ALLOWED="${ATTACHMENT_GATE_MAX_MISSING_ALLOWED:-0}"
ATTACHMENT_GATE_TARGET_FILE_ID="${ATTACHMENT_GATE_TARGET_FILE_ID:-f4653466-b670-a083-acff-19ff6d55be02}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker 未安装或不可用"
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: mvn 未安装或不可用"
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "ERROR: npm 未安装或不可用"
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "ERROR: systemctl 不可用"
  exit 1
fi

if [[ ! -d "$PROJECT_ROOT" ]]; then
  echo "ERROR: 项目目录不存在: $PROJECT_ROOT"
  exit 1
fi

echo "[0/7] 备份数据库..."
BACKUP_FILE="$PROJECT_ROOT/backup_$(date +%F_%H%M%S).sql"
docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" > "$BACKUP_FILE"
echo "数据库备份完成: $BACKUP_FILE"

echo "[1/7] 拉取最新代码..."
cd "$PROJECT_ROOT"
if [[ "$SKIP_GIT_PULL" == "1" ]]; then
  echo "已跳过 git pull（SKIP_GIT_PULL=1）"
else
  git pull origin main
fi

echo "[2/7] 构建前端..."
npm ci
npm run build

echo "[3/7] 构建后端..."
cd "$PROJECT_ROOT/nexusarchive-java"
mvn -DskipTests -Dmaven.compiler.failOnWarning=false clean package

echo "[4/7] 附件巡检门禁..."
if [[ "$ATTACHMENT_GATE_ENABLED" == "1" ]]; then
  GATE_SCRIPT="$PROJECT_ROOT/scripts/prod_attachment_gate.sh"
  if [[ ! -f "$GATE_SCRIPT" ]]; then
    echo "ERROR: 附件门禁脚本不存在: $GATE_SCRIPT"
    exit 1
  fi
  chmod +x "$GATE_SCRIPT"
  APP_DIR="$PROJECT_ROOT" \
  SCAN_LIMIT="$ATTACHMENT_GATE_SCAN_LIMIT" \
  SAMPLE_LIMIT="$ATTACHMENT_GATE_SAMPLE_LIMIT" \
  MAX_MISSING_ALLOWED="$ATTACHMENT_GATE_MAX_MISSING_ALLOWED" \
  TARGET_FILE_ID="$ATTACHMENT_GATE_TARGET_FILE_ID" \
  WORK_DIR="/tmp/nexusarchive_attachment_gate_deploy_$(date +%Y%m%d_%H%M%S)" \
  bash "$GATE_SCRIPT"
else
  echo "已跳过附件门禁（ATTACHMENT_GATE_ENABLED=${ATTACHMENT_GATE_ENABLED}）"
fi

echo "[5/7] 重启服务..."
systemctl restart "$SERVICE_NAME"
systemctl status "$SERVICE_NAME" --no-pager -l

echo "[6/7] 检查 Flyway 迁移状态..."
docker exec -e "PGPASSWORD=$DB_PASSWORD" "$DB_CONTAINER" \
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" \
  -c "select version,description,success from flyway_schema_history order by installed_rank desc limit 10;"

echo "[7/7] 健康检查（等待服务就绪）..."
ELAPSED=0
until curl -fsS "$HEALTH_URL" >/dev/null 2>&1; do
  if (( ELAPSED >= HEALTH_TIMEOUT_SECONDS )); then
    echo "ERROR: 健康检查超时（${HEALTH_TIMEOUT_SECONDS}s）: $HEALTH_URL"
    systemctl status "$SERVICE_NAME" --no-pager -l || true
    journalctl -u "$SERVICE_NAME" -n 80 --no-pager || true
    exit 1
  fi
  echo "等待中... ${ELAPSED}s/${HEALTH_TIMEOUT_SECONDS}s"
  sleep "$HEALTH_INTERVAL_SECONDS"
  ELAPSED=$((ELAPSED + HEALTH_INTERVAL_SECONDS))
done

curl -i "$HEALTH_URL"
curl -I https://www.digivoucher.cn/system

echo "发布完成"
