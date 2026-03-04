#!/usr/bin/env bash
# Input: Shell、docker、psql、文件系统
# Output: 生产附件巡检报告（判断数据缺失 vs 文件缺失，并给出可恢复来源）
# Pos: 生产排查脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/nexusarchive}"
ENV_FILE="${ENV_FILE:-/etc/nexusarchive/nexusarchive.env}"
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-nexusarchive}"
HEALTH_URL_PUBLIC="${HEALTH_URL_PUBLIC:-https://www.digivoucher.cn/api/health}"
HEALTH_URL_LOCAL="${HEALTH_URL_LOCAL:-http://127.0.0.1:19090/api/health}"
SCAN_LIMIT="${SCAN_LIMIT:-200000}"
SAMPLE_LIMIT="${SAMPLE_LIMIT:-300}"
TARGET_FILE_ID="${TARGET_FILE_ID:-f4653466-b670-a083-acff-19ff6d55be02}"

if ! [[ "$SCAN_LIMIT" =~ ^[0-9]+$ ]]; then
  echo "SCAN_LIMIT must be an integer"
  exit 1
fi

if ! [[ "$SAMPLE_LIMIT" =~ ^[0-9]+$ ]]; then
  echo "SAMPLE_LIMIT must be an integer"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/prod_attachment_audit.sql"
TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${OUT_DIR:-/tmp/nexusarchive_attachment_audit_${TS}}"
mkdir -p "$OUT_DIR"

SQL_SUMMARY_FILE="${OUT_DIR}/sql_summary.txt"
FILE_ROWS_FILE="${OUT_DIR}/file_rows.tsv"
MISSING_FILE="${OUT_DIR}/missing_files.tsv"
RECOVERY_FILE="${OUT_DIR}/recovery_candidates.tsv"
REPORT_FILE="${OUT_DIR}/report.txt"

resolve_archive_root() {
  local raw=""
  if [ -f "$ENV_FILE" ]; then
    raw="$(grep -E '^ARCHIVE_ROOT_PATH=' "$ENV_FILE" | tail -n 1 | cut -d '=' -f2- || true)"
  fi
  if [ -z "$raw" ]; then
    raw="${APP_DIR}/nexusarchive-java/data/archives"
  fi
  if [[ "$raw" == /* ]]; then
    printf "%s\n" "$raw"
  else
    printf "%s\n" "${APP_DIR}/${raw}"
  fi
}

ARCHIVE_ROOT_PATH="$(resolve_archive_root)"

echo "=== NexusArchive Production Attachment Audit ===" | tee "$REPORT_FILE"
echo "Time: ${TS}" | tee -a "$REPORT_FILE"
echo "APP_DIR: ${APP_DIR}" | tee -a "$REPORT_FILE"
echo "ARCHIVE_ROOT_PATH: ${ARCHIVE_ROOT_PATH}" | tee -a "$REPORT_FILE"
echo "DB_CONTAINER: ${DB_CONTAINER}" | tee -a "$REPORT_FILE"
echo "DB_NAME: ${DB_NAME}" | tee -a "$REPORT_FILE"
echo "TARGET_FILE_ID: ${TARGET_FILE_ID}" | tee -a "$REPORT_FILE"
echo "OUT_DIR: ${OUT_DIR}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

echo "[1/7] Health checks..." | tee -a "$REPORT_FILE"
if curl -fsS -m 8 "$HEALTH_URL_PUBLIC" >/dev/null 2>&1; then
  echo "  - Public health OK: ${HEALTH_URL_PUBLIC}" | tee -a "$REPORT_FILE"
else
  echo "  - Public health FAILED: ${HEALTH_URL_PUBLIC}" | tee -a "$REPORT_FILE"
fi
if curl -fsS -m 8 "$HEALTH_URL_LOCAL" >/dev/null 2>&1; then
  echo "  - Local health OK: ${HEALTH_URL_LOCAL}" | tee -a "$REPORT_FILE"
else
  echo "  - Local health FAILED: ${HEALTH_URL_LOCAL}" | tee -a "$REPORT_FILE"
fi
echo | tee -a "$REPORT_FILE"

echo "[2/7] SQL summary..." | tee -a "$REPORT_FILE"
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 \
  -v target_file_id="$TARGET_FILE_ID" \
  < "$SQL_FILE" | tee "$SQL_SUMMARY_FILE" >/dev/null
echo "  - SQL summary saved: ${SQL_SUMMARY_FILE}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

scalar_query() {
  local sql="$1"
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -c "$sql" | tr -d '[:space:]'
}

OV_COUNT="$(scalar_query "SELECT COUNT(*) FROM arc_original_voucher;")"
AFC_COUNT="$(scalar_query "SELECT COUNT(*) FROM arc_file_content;")"
AOVF_COUNT="$(scalar_query "SELECT COUNT(*) FROM arc_original_voucher_file;")"
AFC_EMPTY_PATH="$(scalar_query "SELECT COUNT(*) FROM arc_file_content WHERE storage_path IS NULL OR btrim(storage_path) = '';")"
AOVF_EMPTY_PATH="$(scalar_query "SELECT COUNT(*) FROM arc_original_voucher_file WHERE storage_path IS NULL OR btrim(storage_path) = '';")"

echo "[3/7] Export attachment rows (limit=${SCAN_LIMIT})..." | tee -a "$REPORT_FILE"
PATH_EXPORT_SQL=$(cat <<SQL
SELECT * FROM (
  SELECT
    'arc_file_content'::text AS source_table,
    id::text AS row_id,
    COALESCE(archival_code, '') AS code,
    COALESCE(file_name, '') AS file_name,
    COALESCE(storage_path, '') AS storage_path
  FROM arc_file_content
  WHERE storage_path IS NOT NULL AND btrim(storage_path) <> ''
  UNION ALL
  SELECT
    'arc_original_voucher_file'::text AS source_table,
    id::text AS row_id,
    COALESCE(voucher_no, '') AS code,
    COALESCE(file_name, '') AS file_name,
    COALESCE(storage_path, '') AS storage_path
  FROM arc_original_voucher_file
  WHERE storage_path IS NOT NULL AND btrim(storage_path) <> ''
) t
LIMIT ${SCAN_LIMIT};
SQL
)

docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -At -F $'\t' -c "$PATH_EXPORT_SQL" > "$FILE_ROWS_FILE"
TOTAL_EXPORTED="$(wc -l < "$FILE_ROWS_FILE" | tr -d '[:space:]')"
echo "  - Exported rows: ${TOTAL_EXPORTED}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

normalize_rel_path() {
  local storage_path="$1"
  local rel=""
  if [[ "$storage_path" == /* ]]; then
    if [[ "$storage_path" == "$ARCHIVE_ROOT_PATH/"* ]]; then
      rel="${storage_path#"$ARCHIVE_ROOT_PATH"/}"
    elif [[ "$storage_path" == "$APP_DIR/"* ]]; then
      rel="${storage_path#"$APP_DIR"/}"
      rel="${rel#/}"
    else
      rel="${storage_path#/}"
    fi
  else
    rel="${storage_path#./}"
  fi
  printf "%s\n" "$rel"
}

exists_in_tar() {
  local tar_file="$1"
  local rel="$2"
  [ -n "$tar_file" ] && [ -f "$tar_file" ] || return 1
  tar -tzf "$tar_file" "$rel" >/dev/null 2>&1 || tar -tzf "$tar_file" "./$rel" >/dev/null 2>&1
}

echo "[4/7] Check physical existence..." | tee -a "$REPORT_FILE"
: > "$MISSING_FILE"
{
  printf "source_table\trow_id\tcode\tfile_name\tstorage_path\trelative_path\n" > "$MISSING_FILE"
}

MISSING_COUNT=0
while IFS=$'\t' read -r source_table row_id code file_name storage_path; do
  [ -n "$storage_path" ] || continue

  rel_path="$(normalize_rel_path "$storage_path")"
  found=0

  if [[ "$storage_path" == /* ]] && [ -f "$storage_path" ]; then
    found=1
  fi
  if [ "$found" -eq 0 ] && [ -f "${ARCHIVE_ROOT_PATH}/${rel_path}" ]; then
    found=1
  fi
  if [ "$found" -eq 0 ] && [ -f "${APP_DIR}/${rel_path}" ]; then
    found=1
  fi
  if [ "$found" -eq 0 ] && [ -f "${APP_DIR}/nexusarchive-java/${rel_path}" ]; then
    found=1
  fi

  if [ "$found" -eq 0 ]; then
    MISSING_COUNT=$((MISSING_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$source_table" "$row_id" "$code" "$file_name" "$storage_path" "$rel_path" >> "$MISSING_FILE"
  fi
done < "$FILE_ROWS_FILE"

echo "  - Missing physical files: ${MISSING_COUNT}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

echo "[5/7] Probe recovery sources (sample=${SAMPLE_LIMIT})..." | tee -a "$REPORT_FILE"
LATEST_FILES_BACKUP="$(ls -t "${APP_DIR}"/migration_backups/files_before_migrate_*.tgz 2>/dev/null | head -n 1 || true)"
LATEST_PACKAGE="$(ls -t "${APP_DIR}"/migration_packages/full_migration_*.tar.gz 2>/dev/null | head -n 1 || true)"
LATEST_PRE_MIGRATE_DIR="$(ls -dt "${ARCHIVE_ROOT_PATH}".pre_migrate_* 2>/dev/null | head -n 1 || true)"
DEMO_SOURCE_DIR="${APP_DIR}/uploads/demo"

PKG_INNER_ARCHIVE=""
if [ -n "$LATEST_PACKAGE" ] && [ -f "$LATEST_PACKAGE" ]; then
  INNER_MEMBER="$(tar -tzf "$LATEST_PACKAGE" | awk '/(^|\/)files\/archive_root\.tgz$/ {print; exit}')"
  if [ -n "$INNER_MEMBER" ]; then
    mkdir -p "${OUT_DIR}/pkg_extract"
    tar -xzf "$LATEST_PACKAGE" -C "${OUT_DIR}/pkg_extract" "$INNER_MEMBER" >/dev/null 2>&1 || true
    PKG_INNER_ARCHIVE="${OUT_DIR}/pkg_extract/${INNER_MEMBER}"
  fi
fi

{
  printf "source_table\trow_id\tfile_name\tstorage_path\trelative_path\tin_files_backup\tin_migration_package\tin_pre_migrate_dir\tin_demo_source\trecovery_hint\n" > "$RECOVERY_FILE"
}

if [ "$MISSING_COUNT" -gt 0 ]; then
  tail -n +2 "$MISSING_FILE" | head -n "$SAMPLE_LIMIT" | while IFS=$'\t' read -r source_table row_id _code file_name storage_path rel_path; do
    in_files_backup="no"
    in_package="no"
    in_pre_migrate="no"
    in_demo="no"
    hint="NOT_FOUND_IN_CHECKED_SOURCES"

    if [ -n "$LATEST_FILES_BACKUP" ] && exists_in_tar "$LATEST_FILES_BACKUP" "$rel_path"; then
      in_files_backup="yes"
      hint="RECOVER_FROM_FILES_BACKUP"
    fi

    if [ -n "$PKG_INNER_ARCHIVE" ] && exists_in_tar "$PKG_INNER_ARCHIVE" "$rel_path"; then
      in_package="yes"
      if [ "$hint" = "NOT_FOUND_IN_CHECKED_SOURCES" ]; then
        hint="RECOVER_FROM_MIGRATION_PACKAGE"
      fi
    fi

    if [ -n "$LATEST_PRE_MIGRATE_DIR" ] && [ -f "${LATEST_PRE_MIGRATE_DIR}/${rel_path}" ]; then
      in_pre_migrate="yes"
      if [ "$hint" = "NOT_FOUND_IN_CHECKED_SOURCES" ]; then
        hint="RECOVER_FROM_PRE_MIGRATE_DIR"
      fi
    fi

    if [ -d "$DEMO_SOURCE_DIR" ] && [ -f "${DEMO_SOURCE_DIR}/$(basename "$rel_path")" ]; then
      in_demo="yes"
      if [ "$hint" = "NOT_FOUND_IN_CHECKED_SOURCES" ]; then
        hint="RECOVER_FROM_DEMO_SOURCE"
      fi
    fi

    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$source_table" "$row_id" "$file_name" "$storage_path" "$rel_path" \
      "$in_files_backup" "$in_package" "$in_pre_migrate" "$in_demo" "$hint" >> "$RECOVERY_FILE"
  done
fi

echo "  - Recovery probe file: ${RECOVERY_FILE}" | tee -a "$REPORT_FILE"
echo "  - Latest files backup: ${LATEST_FILES_BACKUP:-<none>}" | tee -a "$REPORT_FILE"
echo "  - Latest migration package: ${LATEST_PACKAGE:-<none>}" | tee -a "$REPORT_FILE"
echo "  - Latest pre_migrate dir: ${LATEST_PRE_MIGRATE_DIR:-<none>}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

echo "[6/7] Classification..." | tee -a "$REPORT_FILE"
if [ "$AFC_COUNT" -eq 0 ] && [ "$AOVF_COUNT" -eq 0 ]; then
  CLASSIFY="数据库侧附件元数据接近空，优先判定为“数据缺失/被覆盖”。"
elif [ "$MISSING_COUNT" -gt 0 ]; then
  CLASSIFY="数据库有附件元数据，但存在物理文件缺失/路径漂移，优先判定为“文件体缺失或路径问题”。"
else
  CLASSIFY="未发现大范围物理缺失；若前端仍报错，继续排查权限链路/接口映射。"
fi
echo "  - ${CLASSIFY}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

echo "[7/7] Summary..." | tee -a "$REPORT_FILE"
{
  echo "table_counts: arc_original_voucher=${OV_COUNT}, arc_file_content=${AFC_COUNT}, arc_original_voucher_file=${AOVF_COUNT}"
  echo "empty_storage_path_rows: arc_file_content=${AFC_EMPTY_PATH}, arc_original_voucher_file=${AOVF_EMPTY_PATH}"
  echo "scanned_attachment_rows=${TOTAL_EXPORTED}"
  echo "missing_physical_files=${MISSING_COUNT}"
  echo
  echo "top_missing_by_table:"
  if [ "$MISSING_COUNT" -gt 0 ]; then
    tail -n +2 "$MISSING_FILE" | awk -F'\t' '{c[$1]++} END {for (k in c) printf "  %s\t%s\n", k, c[k]}' | sort
  else
    echo "  <none>"
  fi
  echo
  echo "top_recovery_hints(sample):"
  if [ -f "$RECOVERY_FILE" ]; then
    tail -n +2 "$RECOVERY_FILE" | awk -F'\t' '{c[$10]++} END {for (k in c) printf "  %s\t%s\n", k, c[k]}' | sort
  else
    echo "  <none>"
  fi
} | tee -a "$REPORT_FILE"

echo
echo "Audit completed."
echo "Report: ${REPORT_FILE}"
echo "Missing rows: ${MISSING_FILE}"
echo "Recovery candidates: ${RECOVERY_FILE}"
echo "SQL summary: ${SQL_SUMMARY_FILE}"

