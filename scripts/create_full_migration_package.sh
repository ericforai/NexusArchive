#!/bin/bash
# ==============================================================================
# 全量迁移包生成脚本（整库 + 文件存储）
# ==============================================================================
# 产物结构:
#   <output_dir>/
#     db/full_dump.sql
#     files/archive_root.tgz
#     manifest.txt
#     checksums.sha256
#
# 用法:
#   bash scripts/create_full_migration_package.sh
#   ARCHIVE_ROOT_PATH=/opt/nexusarchive/data/archives bash scripts/create_full_migration_package.sh
# ==============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TS="$(date +%Y%m%d_%H%M%S)"
OUT_BASE="${ROOT_DIR}/migration_packages"
OUT_DIR="${OUT_BASE}/full_migration_${TS}"
PKG_FILE="${OUT_BASE}/full_migration_${TS}.tar.gz"

DB_NAME="${DB_NAME:-nexusarchive}"
DB_USER="${DB_USER:-postgres}"
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"

# 避免 macOS 将 AppleDouble 与扩展属性打进 tar 包
export COPYFILE_DISABLE=1

# 读取 .env.local（如果存在）
if [ -f "${ROOT_DIR}/.env.local" ]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env.local"
  set +a
fi

ARCHIVE_ROOT_PATH="${ARCHIVE_ROOT_PATH:-./nexusarchive-java/data/archives}"
case "${ARCHIVE_ROOT_PATH}" in
  /*) ARCHIVE_ROOT_ABS="${ARCHIVE_ROOT_PATH}" ;;
  *) ARCHIVE_ROOT_ABS="${ROOT_DIR}/${ARCHIVE_ROOT_PATH}" ;;
esac

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}开始生成全量迁移包${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo "DB_CONTAINER=${DB_CONTAINER}"
echo "DB_NAME=${DB_NAME}"
echo "ARCHIVE_ROOT=${ARCHIVE_ROOT_ABS}"

if ! command -v docker >/dev/null 2>&1; then
  echo -e "${RED}❌ docker 未安装${NC}"
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}\$"; then
  echo -e "${RED}❌ 数据库容器未运行: ${DB_CONTAINER}${NC}"
  exit 1
fi

if [ ! -d "${ARCHIVE_ROOT_ABS}" ]; then
  echo -e "${RED}❌ 文件存储目录不存在: ${ARCHIVE_ROOT_ABS}${NC}"
  exit 1
fi

mkdir -p "${OUT_DIR}/db" "${OUT_DIR}/files"

TAR_EXCLUDES=(
  --exclude=".DS_Store"
  --exclude="*/.DS_Store"
  --exclude="._*"
  --exclude="*/._*"
  --exclude="__MACOSX"
  --exclude="*/__MACOSX/*"
)

echo -e "${YELLOW}[1/4] 导出数据库...${NC}"
docker exec "${DB_CONTAINER}" pg_dump -U "${DB_USER}" --no-owner --no-acl "${DB_NAME}" > "${OUT_DIR}/db/full_dump.sql"

echo -e "${YELLOW}[2/4] 打包文件存储目录...${NC}"
tar -czf "${OUT_DIR}/files/archive_root.tgz" "${TAR_EXCLUDES[@]}" -C "${ARCHIVE_ROOT_ABS}" .

echo -e "${YELLOW}[3/4] 生成清单...${NC}"
DB_SIZE="$(wc -c < "${OUT_DIR}/db/full_dump.sql" | tr -d ' ')"
FILE_ARCHIVE_SIZE="$(wc -c < "${OUT_DIR}/files/archive_root.tgz" | tr -d ' ')"
DB_SHA="$(sha256sum "${OUT_DIR}/db/full_dump.sql" | awk '{print $1}')"
FILES_SHA="$(sha256sum "${OUT_DIR}/files/archive_root.tgz" | awk '{print $1}')"

cat > "${OUT_DIR}/manifest.txt" <<EOF
created_at=${TS}
db_container=${DB_CONTAINER}
db_name=${DB_NAME}
archive_root_path=${ARCHIVE_ROOT_ABS}
db_dump_size=${DB_SIZE}
db_dump_sha256=${DB_SHA}
files_archive_size=${FILE_ARCHIVE_SIZE}
files_archive_sha256=${FILES_SHA}
EOF

(
  cd "${OUT_DIR}"
  sha256sum db/full_dump.sql files/archive_root.tgz manifest.txt > checksums.sha256
)

echo -e "${YELLOW}[4/4] 生成发布包...${NC}"
tar -czf "${PKG_FILE}" "${TAR_EXCLUDES[@]}" -C "${OUT_DIR}" .
PKG_SHA="$(sha256sum "${PKG_FILE}" | awk '{print $1}')"
PKG_SIZE="$(wc -c < "${PKG_FILE}" | tr -d ' ')"

echo -e "${GREEN}✅ 迁移包生成完成${NC}"
echo "目录: ${OUT_DIR}"
echo "压缩包: ${PKG_FILE}"
echo "大小: ${PKG_SIZE} bytes"
echo "SHA256: ${PKG_SHA}"
echo
echo "GitHub Actions 输入建议:"
echo "migration_package_url=<你的可下载URL>"
echo "migration_package_sha256=${PKG_SHA}"
echo "confirm_text=I_UNDERSTAND_FULL_OVERWRITE"
