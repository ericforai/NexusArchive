#!/usr/bin/env bash
# Input: Shell、docker、psql、文件系统
# Output: 附件缺失回补报告（可选回写 DB）
# Pos: 生产修复脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/nexusarchive}"
ENV_FILE="${ENV_FILE:-/etc/nexusarchive/nexusarchive.env}"
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-nexusarchive}"

APPLY_DB="${APPLY_DB:-0}"                 # 0=dry-run, 1=apply
MISSING_TSV="${MISSING_TSV:-auto}"        # 指向 missing_files.tsv，或 auto 自动发现
SCAN_LIMIT="${SCAN_LIMIT:-200000}"        # 仅在自动审计时生效
SAMPLE_LIMIT="${SAMPLE_LIMIT:-300}"       # 仅在自动审计时生效
TARGET_FILE_ID="${TARGET_FILE_ID:-f4653466-b670-a083-acff-19ff6d55be02}"  # 仅在自动审计时生效

TS="$(date +%Y%m%d_%H%M%S)"
WORK_DIR="${WORK_DIR:-/tmp/nexusarchive_attachment_repair_${TS}}"
mkdir -p "$WORK_DIR"

RESOLVED_TSV="${WORK_DIR}/resolved.tsv"
UNRESOLVED_TSV="${WORK_DIR}/unresolved.tsv"
UPDATES_SQL="${WORK_DIR}/apply_updates.sql"
REPORT_FILE="${WORK_DIR}/repair_report.txt"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUDIT_SCRIPT="${SCRIPT_DIR}/prod_attachment_audit.sh"

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
LATEST_PRE_MIGRATE_DIR="$(ls -dt "${ARCHIVE_ROOT_PATH}".pre_migrate_* 2>/dev/null | head -n 1 || true)"
LATEST_FILES_BACKUP="$(ls -t "${APP_DIR}"/migration_backups/files_before_migrate_*.tgz 2>/dev/null | head -n 1 || true)"
LATEST_MIGRATION_PACKAGE="$(ls -t "${APP_DIR}"/migration_packages/full_migration_*.tar.gz 2>/dev/null | head -n 1 || true)"

AUDIT_OUT_DIR=""
if [ "$MISSING_TSV" = "auto" ]; then
  if [ -x "$AUDIT_SCRIPT" ]; then
    AUDIT_OUT_DIR="${WORK_DIR}/audit"
    mkdir -p "$AUDIT_OUT_DIR"
    OUT_DIR="$AUDIT_OUT_DIR" TARGET_FILE_ID="$TARGET_FILE_ID" SCAN_LIMIT="$SCAN_LIMIT" SAMPLE_LIMIT="$SAMPLE_LIMIT" bash "$AUDIT_SCRIPT" >/dev/null
    MISSING_TSV="${AUDIT_OUT_DIR}/missing_files.tsv"
  else
    LATEST_AUDIT_DIR="$(ls -dt /tmp/nexusarchive_attachment_audit_* 2>/dev/null | head -n 1 || true)"
    if [ -n "$LATEST_AUDIT_DIR" ] && [ -f "${LATEST_AUDIT_DIR}/missing_files.tsv" ]; then
      MISSING_TSV="${LATEST_AUDIT_DIR}/missing_files.tsv"
    fi
  fi
fi

if [ ! -f "$MISSING_TSV" ]; then
  echo "missing_files.tsv not found: ${MISSING_TSV}" >&2
  exit 1
fi

if [ ! -d "$ARCHIVE_ROOT_PATH" ]; then
  echo "ARCHIVE_ROOT_PATH not found: ${ARCHIVE_ROOT_PATH}" >&2
  exit 1
fi

escape_sql() {
  printf "%s" "$1" | sed "s/'/''/g"
}

extract_from_tar_by_filename() {
  local tar_file="$1"
  local filename="$2"
  local out_base="$3"
  local member=""
  member="$(tar -tzf "$tar_file" | grep -F "/${filename}" | grep -v '/\._' | head -n 1 || true)"
  if [ -z "$member" ]; then
    return 1
  fi
  mkdir -p "$out_base"
  tar -xzf "$tar_file" -C "$out_base" "$member" >/dev/null 2>&1 || return 1
  if [[ "$member" == ./* ]]; then
    printf "%s\n" "${out_base}/${member#./}"
  else
    printf "%s\n" "${out_base}/${member}"
  fi
}

extract_inner_archive_from_package() {
  local pkg="$1"
  local out_dir="$2"
  local inner=""
  inner="$(tar -tzf "$pkg" | awk '/(^|\/)files\/archive_root\.tgz$/ {print; exit}')"
  if [ -z "$inner" ]; then
    return 1
  fi
  mkdir -p "$out_dir"
  tar -xzf "$pkg" -C "$out_dir" "$inner" >/dev/null 2>&1 || return 1
  if [[ "$inner" == ./* ]]; then
    printf "%s\n" "${out_dir}/${inner#./}"
  else
    printf "%s\n" "${out_dir}/${inner}"
  fi
}

find_source_for_file() {
  local file_name="$1"
  local local_found=""
  local source_tag=""

  local CANDIDATE_DIRS=(
    "${ARCHIVE_ROOT_PATH}"
    "${ARCHIVE_ROOT_PATH}/uploads/demo"
    "${APP_DIR}/uploads/demo"
    "${APP_DIR}/docs/demo数据"
  )

  if [ -n "$LATEST_PRE_MIGRATE_DIR" ]; then
    CANDIDATE_DIRS+=("${LATEST_PRE_MIGRATE_DIR}")
  fi

  for dir in "${CANDIDATE_DIRS[@]}"; do
    [ -d "$dir" ] || continue
    local_found="$(find "$dir" -type f -name "$file_name" 2>/dev/null | head -n 1 || true)"
    if [ -n "$local_found" ]; then
      printf "%s\t%s\n" "$local_found" "FS:${dir}"
      return 0
    fi
  done

  if [ -n "$LATEST_FILES_BACKUP" ] && [ -f "$LATEST_FILES_BACKUP" ]; then
    local out_dir="${WORK_DIR}/extracted_from_files_backup"
    local_found="$(extract_from_tar_by_filename "$LATEST_FILES_BACKUP" "$file_name" "$out_dir" || true)"
    if [ -n "$local_found" ] && [ -f "$local_found" ]; then
      source_tag="TAR:files_before_migrate"
      printf "%s\t%s\n" "$local_found" "$source_tag"
      return 0
    fi
  fi

  if [ -n "$LATEST_MIGRATION_PACKAGE" ] && [ -f "$LATEST_MIGRATION_PACKAGE" ]; then
    local inner_archive=""
    inner_archive="$(extract_inner_archive_from_package "$LATEST_MIGRATION_PACKAGE" "${WORK_DIR}/migration_pkg_inner" || true)"
    if [ -n "$inner_archive" ] && [ -f "$inner_archive" ]; then
      local out_dir2="${WORK_DIR}/extracted_from_migration_package"
      local_found="$(extract_from_tar_by_filename "$inner_archive" "$file_name" "$out_dir2" || true)"
      if [ -n "$local_found" ] && [ -f "$local_found" ]; then
        source_tag="TAR:migration_package"
        printf "%s\t%s\n" "$local_found" "$source_tag"
        return 0
      fi
    fi
  fi

  return 1
}

derive_target_rel_path() {
  local row_id="$1"
  local file_name="$2"
  local old_path="$3"

  if [[ "$old_path" == uploads/* ]]; then
    printf "%s\n" "$old_path"
    return 0
  fi

  if [[ "$old_path" == */uploads/* ]]; then
    printf "uploads/%s\n" "${old_path#*/uploads/}"
    return 0
  fi

  printf "uploads/recovered/%s_%s\n" "$row_id" "$file_name"
}

echo "=== NexusArchive Attachment Repair ===" | tee "$REPORT_FILE"
echo "time=${TS}" | tee -a "$REPORT_FILE"
echo "work_dir=${WORK_DIR}" | tee -a "$REPORT_FILE"
echo "apply_db=${APPLY_DB}" | tee -a "$REPORT_FILE"
echo "archive_root_path=${ARCHIVE_ROOT_PATH}" | tee -a "$REPORT_FILE"
echo "missing_tsv=${MISSING_TSV}" | tee -a "$REPORT_FILE"
echo "latest_pre_migrate_dir=${LATEST_PRE_MIGRATE_DIR:-<none>}" | tee -a "$REPORT_FILE"
echo "latest_files_backup=${LATEST_FILES_BACKUP:-<none>}" | tee -a "$REPORT_FILE"
echo "latest_migration_package=${LATEST_MIGRATION_PACKAGE:-<none>}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

printf "source_table\trow_id\tfile_name\told_storage_path\tnew_storage_path\tsource_file\tsource_hint\tnew_file_size\n" > "$RESOLVED_TSV"
printf "source_table\trow_id\tfile_name\told_storage_path\treason\n" > "$UNRESOLVED_TSV"

RESOLVED_COUNT=0
UNRESOLVED_COUNT=0

while IFS=$'\t' read -r source_table row_id _code file_name old_storage_path _rel; do
  if [ "$row_id" = "row_id" ]; then
    continue
  fi
  if [ -z "$row_id" ] || [ -z "$file_name" ]; then
    continue
  fi

  source_result="$(find_source_for_file "$file_name" || true)"
  if [ -z "$source_result" ]; then
    UNRESOLVED_COUNT=$((UNRESOLVED_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\n" "$source_table" "$row_id" "$file_name" "$old_storage_path" "source_not_found" >> "$UNRESOLVED_TSV"
    continue
  fi

  source_file="$(printf "%s" "$source_result" | awk -F'\t' '{print $1}')"
  source_hint="$(printf "%s" "$source_result" | awk -F'\t' '{print $2}')"

  if [ ! -f "$source_file" ]; then
    UNRESOLVED_COUNT=$((UNRESOLVED_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\n" "$source_table" "$row_id" "$file_name" "$old_storage_path" "source_disappeared" >> "$UNRESOLVED_TSV"
    continue
  fi

  target_rel="$(derive_target_rel_path "$row_id" "$file_name" "$old_storage_path")"
  target_abs="${ARCHIVE_ROOT_PATH}/${target_rel}"
  mkdir -p "$(dirname "$target_abs")"

  if [ -f "$target_abs" ]; then
    if ! cmp -s "$source_file" "$target_abs"; then
      target_rel="uploads/recovered/${row_id}_${file_name}"
      target_abs="${ARCHIVE_ROOT_PATH}/${target_rel}"
      mkdir -p "$(dirname "$target_abs")"
      cp -f "$source_file" "$target_abs"
    fi
  else
    cp -f "$source_file" "$target_abs"
  fi

  new_size="$(wc -c < "$target_abs" | tr -d ' ')"
  RESOLVED_COUNT=$((RESOLVED_COUNT + 1))
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "$source_table" "$row_id" "$file_name" "$old_storage_path" "$target_rel" "$source_file" "$source_hint" "$new_size" >> "$RESOLVED_TSV"
done < "$MISSING_TSV"

echo "resolved_count=${RESOLVED_COUNT}" | tee -a "$REPORT_FILE"
echo "unresolved_count=${UNRESOLVED_COUNT}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

{
  echo "BEGIN;"
  tail -n +2 "$RESOLVED_TSV" | while IFS=$'\t' read -r source_table row_id _file_name _old_storage new_storage _source_file _source_hint new_size; do
    row_id_esc="$(escape_sql "$row_id")"
    storage_esc="$(escape_sql "$new_storage")"
    size_num="${new_size:-0}"
    case "$source_table" in
      arc_file_content)
        echo "UPDATE arc_file_content SET storage_path='${storage_esc}', file_size=${size_num}::bigint, last_modified_time=NOW() WHERE id='${row_id_esc}';"
        ;;
      arc_original_voucher_file)
        echo "UPDATE arc_original_voucher_file SET storage_path='${storage_esc}', file_size=${size_num}::bigint, last_modified_time=NOW() WHERE id='${row_id_esc}';"
        ;;
      *)
        ;;
    esac
  done
  echo "COMMIT;"
} > "$UPDATES_SQL"

echo "sql_file=${UPDATES_SQL}" | tee -a "$REPORT_FILE"
echo "resolved_tsv=${RESOLVED_TSV}" | tee -a "$REPORT_FILE"
echo "unresolved_tsv=${UNRESOLVED_TSV}" | tee -a "$REPORT_FILE"

if [ "$APPLY_DB" = "1" ]; then
  if [ "$RESOLVED_COUNT" -gt 0 ]; then
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$UPDATES_SQL" >/dev/null
    echo "db_apply=done" | tee -a "$REPORT_FILE"
  else
    echo "db_apply=skipped_no_resolved_rows" | tee -a "$REPORT_FILE"
  fi
else
  echo "db_apply=dry_run" | tee -a "$REPORT_FILE"
fi

echo
echo "Attachment repair finished."
echo "Report: ${REPORT_FILE}"

