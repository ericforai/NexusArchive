#!/usr/bin/env bash
# Input: unresolved.tsv、文件系统/tar/对象存储 CLI（可选）
# Output: 外部来源定位结果 + 二次回补 SQL（可选回写 DB）
# Pos: 生产附件外部回补脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/nexusarchive}"
ENV_FILE="${ENV_FILE:-/etc/nexusarchive/nexusarchive.env}"
DB_CONTAINER="${DB_CONTAINER:-nexus-db}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-nexusarchive}"

APPLY_DB="${APPLY_DB:-0}"  # 0=dry-run, 1=apply
UNRESOLVED_TSV="${UNRESOLVED_TSV:-auto}"
DOWNLOAD_FOUND_OBJECT="${DOWNLOAD_FOUND_OBJECT:-0}"  # 0=仅定位, 1=下载后自动回补
ALLOW_PLACEHOLDER_FALLBACK="${ALLOW_PLACEHOLDER_FALLBACK:-0}"  # 0=关闭, 1=允许占位回补
PLACEHOLDER_SOURCE_DIRS="${PLACEHOLDER_SOURCE_DIRS:-/tmp/prod-demo-placeholders,/opt/nexusarchive/public/demo,/opt/nexusarchive/data/archives/demo,/opt/nexusarchive/public}"

EXTERNAL_FS_DIRS="${EXTERNAL_FS_DIRS:-/opt/nexusarchive,/mnt,/data/backups,/backup}"
EXTERNAL_TAR_GLOBS="${EXTERNAL_TAR_GLOBS:-/opt/nexusarchive/migration_backups/*.tgz,/opt/nexusarchive/migration_packages/*.tar.gz,/opt/nexusarchive/backups/*.tgz,/opt/nexusarchive/backups/*.tar.gz}"
S3_URI_PREFIXES="${S3_URI_PREFIXES:-}"
OSS_URI_PREFIXES="${OSS_URI_PREFIXES:-}"
MINIO_URI_PREFIXES="${MINIO_URI_PREFIXES:-}"

TARGET_RECOVER_BASE="${TARGET_RECOVER_BASE:-uploads/recovered/external}"
WORK_DIR="${WORK_DIR:-/tmp/nexusarchive_attachment_external_recovery_$(date +%Y%m%d_%H%M%S)}"

mkdir -p "$WORK_DIR"
RESOLVED_TSV="${WORK_DIR}/resolved_external.tsv"
UNRESOLVED_LEFT_TSV="${WORK_DIR}/unresolved_external.tsv"
PROBE_TSV="${WORK_DIR}/external_probe.tsv"
UPDATES_SQL="${WORK_DIR}/apply_updates_second.sql"
REPORT_FILE="${WORK_DIR}/external_recovery_report.txt"

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf "%s" "$s"
}

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

discover_latest_unresolved_tsv() {
  local dir=""
  while IFS= read -r dir; do
    [ -n "$dir" ] || continue
    if [ -f "${dir}/unresolved.tsv" ]; then
      printf "%s\n" "${dir}/unresolved.tsv"
      return 0
    fi
  done < <(ls -dt /tmp/nexusarchive_attachment_repair_* 2>/dev/null || true)
  return 1
}

normalize_rel_from_storage() {
  local old_storage_path="$1"
  if [[ "$old_storage_path" == /tmp/nexusarchive/* ]]; then
    printf "%s\n" "${old_storage_path#/tmp/nexusarchive/}"
    return 0
  fi
  if [[ "$old_storage_path" == */uploads/* ]]; then
    printf "uploads/%s\n" "${old_storage_path#*/uploads/}"
    return 0
  fi
  if [[ "$old_storage_path" == /* ]]; then
    printf "%s\n" "${old_storage_path#/}"
    return 0
  fi
  printf "%s\n" "${old_storage_path#./}"
}

escape_sql() {
  printf "%s" "$1" | sed "s/'/''/g"
}

candidate_keys() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local base_name
  local rel_norm
  local ext

  base_name="$(basename "$old_storage_path")"
  rel_norm="$(normalize_rel_from_storage "$old_storage_path")"
  ext=""
  if [[ "$base_name" == *.* ]]; then
    ext=".${base_name##*.}"
  fi

  {
    printf "%s\n" "$base_name"
    printf "%s\n" "$rel_norm"
    if [[ "$old_storage_path" == /* ]]; then
      printf "%s\n" "${old_storage_path#/}"
    fi
    if [ -n "$ext" ]; then
      printf "%s%s\n" "$row_id" "$ext"
    fi
    printf "%s\n" "$file_name"
  } | awk 'NF && !seen[$0]++'
}

ARCHIVE_ROOT_PATH="$(resolve_archive_root)"
if [ ! -d "$ARCHIVE_ROOT_PATH" ]; then
  echo "ARCHIVE_ROOT_PATH not found: ${ARCHIVE_ROOT_PATH}" >&2
  exit 1
fi

if [ "$UNRESOLVED_TSV" = "auto" ]; then
  UNRESOLVED_TSV="$(discover_latest_unresolved_tsv || true)"
fi

if [ -z "$UNRESOLVED_TSV" ] || [ ! -f "$UNRESOLVED_TSV" ]; then
  echo "unresolved.tsv not found: ${UNRESOLVED_TSV:-<empty>}" >&2
  exit 1
fi

IFS=',' read -r -a EXTERNAL_FS_DIR_ARR <<< "$EXTERNAL_FS_DIRS"
IFS=',' read -r -a EXTERNAL_TAR_GLOB_ARR <<< "$EXTERNAL_TAR_GLOBS"
IFS=',' read -r -a S3_PREFIX_ARR <<< "$S3_URI_PREFIXES"
IFS=',' read -r -a OSS_PREFIX_ARR <<< "$OSS_URI_PREFIXES"
IFS=',' read -r -a MINIO_PREFIX_ARR <<< "$MINIO_URI_PREFIXES"
IFS=',' read -r -a PLACEHOLDER_DIR_ARR <<< "$PLACEHOLDER_SOURCE_DIRS"

for i in "${!EXTERNAL_FS_DIR_ARR[@]}"; do
  EXTERNAL_FS_DIR_ARR[$i]="$(trim "${EXTERNAL_FS_DIR_ARR[$i]}")"
done
for i in "${!EXTERNAL_TAR_GLOB_ARR[@]}"; do
  EXTERNAL_TAR_GLOB_ARR[$i]="$(trim "${EXTERNAL_TAR_GLOB_ARR[$i]}")"
done
for i in "${!S3_PREFIX_ARR[@]}"; do
  S3_PREFIX_ARR[$i]="$(trim "${S3_PREFIX_ARR[$i]}")"
done
for i in "${!OSS_PREFIX_ARR[@]}"; do
  OSS_PREFIX_ARR[$i]="$(trim "${OSS_PREFIX_ARR[$i]}")"
done
for i in "${!MINIO_PREFIX_ARR[@]}"; do
  MINIO_PREFIX_ARR[$i]="$(trim "${MINIO_PREFIX_ARR[$i]}")"
done
for i in "${!PLACEHOLDER_DIR_ARR[@]}"; do
  PLACEHOLDER_DIR_ARR[$i]="$(trim "${PLACEHOLDER_DIR_ARR[$i]}")"
done

TAR_FILE_LIST=()
for pattern in "${EXTERNAL_TAR_GLOB_ARR[@]}"; do
  [ -n "$pattern" ] || continue
  while IFS= read -r tar_file; do
    [ -f "$tar_file" ] || continue
    TAR_FILE_LIST+=("$tar_file")
  done < <(compgen -G "$pattern" || true)
done

if [ "${#TAR_FILE_LIST[@]}" -gt 0 ]; then
  DEDUPED_TAR_FILE_LIST=()
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    DEDUPED_TAR_FILE_LIST+=("$f")
  done < <(printf "%s\n" "${TAR_FILE_LIST[@]}" | sort -u)
  TAR_FILE_LIST=("${DEDUPED_TAR_FILE_LIST[@]}")
fi

PLACEHOLDER_FILES=()
if [ "$ALLOW_PLACEHOLDER_FALLBACK" = "1" ]; then
  for dir in "${PLACEHOLDER_DIR_ARR[@]}"; do
    [ -n "$dir" ] || continue
    [ -d "$dir" ] || continue
    while IFS= read -r f; do
      [ -f "$f" ] || continue
      PLACEHOLDER_FILES+=("$f")
    done < <(find "$dir" -type f \( -name "*.pdf" -o -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" -o -name "*.webp" \) 2>/dev/null | sort)
  done
  if [ "${#PLACEHOLDER_FILES[@]}" -gt 0 ]; then
    DEDUPED_PLACEHOLDER_FILES=()
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      DEDUPED_PLACEHOLDER_FILES+=("$f")
    done < <(printf "%s\n" "${PLACEHOLDER_FILES[@]}" | awk '!seen[$0]++')
    PLACEHOLDER_FILES=("${DEDUPED_PLACEHOLDER_FILES[@]}")
  fi
fi

find_placeholder_file() {
  local slot="${1:-0}"
  if [ "$ALLOW_PLACEHOLDER_FALLBACK" != "1" ]; then
    return 1
  fi
  if [ "${#PLACEHOLDER_FILES[@]}" -eq 0 ]; then
    return 1
  fi
  local idx=$((slot % ${#PLACEHOLDER_FILES[@]}))
  local selected="${PLACEHOLDER_FILES[$idx]}"
  printf "%s\tPLACEHOLDER:%s\n" "$selected" "$selected"
}

find_in_external_fs() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local rel_norm="$4"
  local base_name
  local dir
  local hit

  base_name="$(basename "$old_storage_path")"
  for dir in "${EXTERNAL_FS_DIR_ARR[@]}"; do
    [ -n "$dir" ] || continue
    [ -d "$dir" ] || continue

    if [ -n "$rel_norm" ] && [ -f "${dir}/${rel_norm}" ]; then
      printf "%s\tFS_REL:%s\n" "${dir}/${rel_norm}" "$dir"
      return 0
    fi
    if [ -f "${dir}/${base_name}" ]; then
      printf "%s\tFS_BASENAME:%s\n" "${dir}/${base_name}" "$dir"
      return 0
    fi

    hit="$(find "$dir" -type f \( -name "$base_name" -o -name "$file_name" -o -name "${row_id}.*" \) 2>/dev/null | head -n 1 || true)"
    if [ -n "$hit" ]; then
      printf "%s\tFS_FIND:%s\n" "$hit" "$dir"
      return 0
    fi
  done
  return 1
}

find_in_tar_archives() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local rel_norm="$4"
  local base_name
  local tar_file
  local member
  local extract_dir
  local extracted

  base_name="$(basename "$old_storage_path")"
  for tar_file in "${TAR_FILE_LIST[@]-}"; do
    [ -n "$tar_file" ] || continue
    member=""
    if [ -n "$rel_norm" ]; then
      member="$(tar -tf "$tar_file" 2>/dev/null | grep -F "$rel_norm" | head -n 1 || true)"
    fi
    if [ -z "$member" ]; then
      member="$(tar -tf "$tar_file" 2>/dev/null | grep -F "/$base_name" | head -n 1 || true)"
    fi
    if [ -z "$member" ] && [ -n "$file_name" ]; then
      member="$(tar -tf "$tar_file" 2>/dev/null | grep -F "/$file_name" | head -n 1 || true)"
    fi
    if [ -z "$member" ]; then
      continue
    fi

    extract_dir="${WORK_DIR}/extracted/${row_id}"
    mkdir -p "$extract_dir"
    tar -xf "$tar_file" -C "$extract_dir" "$member" >/dev/null 2>&1 || continue
    if [[ "$member" == ./* ]]; then
      extracted="${extract_dir}/${member#./}"
    else
      extracted="${extract_dir}/${member}"
    fi
    if [ -f "$extracted" ]; then
      printf "%s\tTAR:%s:%s\n" "$extracted" "$tar_file" "$member"
      return 0
    fi
  done
  return 1
}

find_in_s3() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local prefix
  local key
  local uri
  local dl

  command -v aws >/dev/null 2>&1 || return 1
  [ "${#S3_PREFIX_ARR[@]}" -gt 0 ] || return 1

  while IFS= read -r key; do
    [ -n "$key" ] || continue
    for prefix in "${S3_PREFIX_ARR[@]}"; do
      [ -n "$prefix" ] || continue
      uri="${prefix%/}/${key}"
      if aws s3 ls "$uri" >/dev/null 2>&1; then
        if [ "$DOWNLOAD_FOUND_OBJECT" = "1" ]; then
          dl="${WORK_DIR}/object_downloads/${row_id}_$(basename "$key")"
          mkdir -p "$(dirname "$dl")"
          if aws s3 cp "$uri" "$dl" >/dev/null 2>&1; then
            printf "%s\tS3:%s\n" "$dl" "$uri"
            return 0
          fi
        else
          printf "__REMOTE__\tS3:%s\n" "$uri"
          return 0
        fi
      fi
    done
  done < <(candidate_keys "$row_id" "$file_name" "$old_storage_path")
  return 1
}

find_in_oss() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local prefix
  local key
  local uri
  local dl

  command -v ossutil >/dev/null 2>&1 || return 1
  [ "${#OSS_PREFIX_ARR[@]}" -gt 0 ] || return 1

  while IFS= read -r key; do
    [ -n "$key" ] || continue
    for prefix in "${OSS_PREFIX_ARR[@]}"; do
      [ -n "$prefix" ] || continue
      uri="${prefix%/}/${key}"
      if ossutil ls "$uri" >/dev/null 2>&1; then
        if [ "$DOWNLOAD_FOUND_OBJECT" = "1" ]; then
          dl="${WORK_DIR}/object_downloads/${row_id}_$(basename "$key")"
          mkdir -p "$(dirname "$dl")"
          if ossutil cp -f "$uri" "$dl" >/dev/null 2>&1; then
            printf "%s\tOSS:%s\n" "$dl" "$uri"
            return 0
          fi
        else
          printf "__REMOTE__\tOSS:%s\n" "$uri"
          return 0
        fi
      fi
    done
  done < <(candidate_keys "$row_id" "$file_name" "$old_storage_path")
  return 1
}

find_in_minio() {
  local row_id="$1"
  local file_name="$2"
  local old_storage_path="$3"
  local prefix
  local key
  local uri
  local dl

  command -v mc >/dev/null 2>&1 || return 1
  [ "${#MINIO_PREFIX_ARR[@]}" -gt 0 ] || return 1

  while IFS= read -r key; do
    [ -n "$key" ] || continue
    for prefix in "${MINIO_PREFIX_ARR[@]}"; do
      [ -n "$prefix" ] || continue
      uri="${prefix%/}/${key}"
      if mc stat "$uri" >/dev/null 2>&1; then
        if [ "$DOWNLOAD_FOUND_OBJECT" = "1" ]; then
          dl="${WORK_DIR}/object_downloads/${row_id}_$(basename "$key")"
          mkdir -p "$(dirname "$dl")"
          if mc cp "$uri" "$dl" >/dev/null 2>&1; then
            printf "%s\tMINIO:%s\n" "$dl" "$uri"
            return 0
          fi
        else
          printf "__REMOTE__\tMINIO:%s\n" "$uri"
          return 0
        fi
      fi
    done
  done < <(candidate_keys "$row_id" "$file_name" "$old_storage_path")
  return 1
}

printf "source_table\trow_id\tfile_name\told_storage_path\tnew_storage_path\tsource_file\tsource_hint\tnew_file_size\n" > "$RESOLVED_TSV"
printf "source_table\trow_id\tfile_name\told_storage_path\treason\n" > "$UNRESOLVED_LEFT_TSV"
printf "source_table\trow_id\tfile_name\told_storage_path\tfs_hit\ttar_hit\ts3_hit\toss_hit\tminio_hit\tplaceholder_hit\tselected_source\tstatus\n" > "$PROBE_TSV"

RESOLVED_COUNT=0
UNRESOLVED_COUNT=0
ROW_COUNTER=0

echo "=== NexusArchive External Attachment Recovery ===" | tee "$REPORT_FILE"
echo "work_dir=${WORK_DIR}" | tee -a "$REPORT_FILE"
echo "apply_db=${APPLY_DB}" | tee -a "$REPORT_FILE"
echo "download_found_object=${DOWNLOAD_FOUND_OBJECT}" | tee -a "$REPORT_FILE"
echo "allow_placeholder_fallback=${ALLOW_PLACEHOLDER_FALLBACK}" | tee -a "$REPORT_FILE"
echo "archive_root_path=${ARCHIVE_ROOT_PATH}" | tee -a "$REPORT_FILE"
echo "unresolved_tsv=${UNRESOLVED_TSV}" | tee -a "$REPORT_FILE"
echo "external_fs_dirs=${EXTERNAL_FS_DIRS}" | tee -a "$REPORT_FILE"
echo "external_tar_globs=${EXTERNAL_TAR_GLOBS}" | tee -a "$REPORT_FILE"
echo "tar_candidates_count=${#TAR_FILE_LIST[@]}" | tee -a "$REPORT_FILE"
echo "placeholder_source_dirs=${PLACEHOLDER_SOURCE_DIRS}" | tee -a "$REPORT_FILE"
echo "placeholder_candidates_count=${#PLACEHOLDER_FILES[@]}" | tee -a "$REPORT_FILE"
echo | tee -a "$REPORT_FILE"

while IFS=$'\t' read -r source_table row_id file_name old_storage_path _reason; do
  if [ "$source_table" = "source_table" ]; then
    continue
  fi
  if [ -z "$row_id" ] || [ -z "$old_storage_path" ]; then
    continue
  fi
  ROW_COUNTER=$((ROW_COUNTER + 1))

  fs_hit="no"
  tar_hit="no"
  s3_hit="no"
  oss_hit="no"
  minio_hit="no"
  placeholder_hit="no"
  selected_source="-"
  status="unresolved"

  rel_norm="$(normalize_rel_from_storage "$old_storage_path")"
  source_result=""

  source_result="$(find_in_external_fs "$row_id" "$file_name" "$old_storage_path" "$rel_norm" || true)"
  if [ -n "$source_result" ]; then
    fs_hit="yes"
  fi

  if [ -z "$source_result" ]; then
    source_result="$(find_in_tar_archives "$row_id" "$file_name" "$old_storage_path" "$rel_norm" || true)"
    if [ -n "$source_result" ]; then
      tar_hit="yes"
    fi
  fi

  if [ -z "$source_result" ]; then
    source_result="$(find_in_s3 "$row_id" "$file_name" "$old_storage_path" || true)"
    if [ -n "$source_result" ]; then
      s3_hit="yes"
    fi
  fi

  if [ -z "$source_result" ]; then
    source_result="$(find_in_oss "$row_id" "$file_name" "$old_storage_path" || true)"
    if [ -n "$source_result" ]; then
      oss_hit="yes"
    fi
  fi

  if [ -z "$source_result" ]; then
    source_result="$(find_in_minio "$row_id" "$file_name" "$old_storage_path" || true)"
    if [ -n "$source_result" ]; then
      minio_hit="yes"
    fi
  fi

  if [ -z "$source_result" ]; then
    source_result="$(find_placeholder_file "$ROW_COUNTER" || true)"
    if [ -n "$source_result" ]; then
      placeholder_hit="yes"
    fi
  fi

  if [ -z "$source_result" ]; then
    status="source_not_found"
    UNRESOLVED_COUNT=$((UNRESOLVED_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\n" "$source_table" "$row_id" "$file_name" "$old_storage_path" "$status" >> "$UNRESOLVED_LEFT_TSV"
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$source_table" "$row_id" "$file_name" "$old_storage_path" "$fs_hit" "$tar_hit" "$s3_hit" "$oss_hit" "$minio_hit" "$placeholder_hit" "$selected_source" "$status" >> "$PROBE_TSV"
    continue
  fi

  source_file="$(printf "%s" "$source_result" | awk -F'\t' '{print $1}')"
  source_hint="$(printf "%s" "$source_result" | awk -F'\t' '{print $2}')"
  selected_source="$source_hint"

  if [ "$source_file" = "__REMOTE__" ]; then
    status="object_found_need_download"
    UNRESOLVED_COUNT=$((UNRESOLVED_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\n" "$source_table" "$row_id" "$file_name" "$old_storage_path" "$status" >> "$UNRESOLVED_LEFT_TSV"
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$source_table" "$row_id" "$file_name" "$old_storage_path" "$fs_hit" "$tar_hit" "$s3_hit" "$oss_hit" "$minio_hit" "$placeholder_hit" "$selected_source" "$status" >> "$PROBE_TSV"
    continue
  fi

  if [ ! -f "$source_file" ]; then
    status="source_disappeared"
    UNRESOLVED_COUNT=$((UNRESOLVED_COUNT + 1))
    printf "%s\t%s\t%s\t%s\t%s\n" "$source_table" "$row_id" "$file_name" "$old_storage_path" "$status" >> "$UNRESOLVED_LEFT_TSV"
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
      "$source_table" "$row_id" "$file_name" "$old_storage_path" "$fs_hit" "$tar_hit" "$s3_hit" "$oss_hit" "$minio_hit" "$placeholder_hit" "$selected_source" "$status" >> "$PROBE_TSV"
    continue
  fi

  base_name="$(basename "$old_storage_path")"
  target_rel="${TARGET_RECOVER_BASE}/${row_id}_${base_name}"
  target_abs="${ARCHIVE_ROOT_PATH}/${target_rel}"
  mkdir -p "$(dirname "$target_abs")"
  cp -f "$source_file" "$target_abs"

  new_size="$(wc -c < "$target_abs" | tr -d ' ')"
  if [ "$placeholder_hit" = "yes" ]; then
    status="resolved_placeholder"
  else
    status="resolved"
  fi
  RESOLVED_COUNT=$((RESOLVED_COUNT + 1))

  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "$source_table" "$row_id" "$file_name" "$old_storage_path" "$target_rel" "$source_file" "$source_hint" "$new_size" >> "$RESOLVED_TSV"
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "$source_table" "$row_id" "$file_name" "$old_storage_path" "$fs_hit" "$tar_hit" "$s3_hit" "$oss_hit" "$minio_hit" "$placeholder_hit" "$selected_source" "$status" >> "$PROBE_TSV"
done < "$UNRESOLVED_TSV"

{
  echo "BEGIN;"
  tail -n +2 "$RESOLVED_TSV" | while IFS=$'\t' read -r source_table row_id _file_name _old_storage new_storage _source_file _source_hint new_size; do
    row_id_esc="$(escape_sql "$row_id")"
    storage_esc="$(escape_sql "$new_storage")"
    size_num="${new_size:-0}"
    case "$source_table" in
      arc_file_content)
        echo "UPDATE arc_file_content SET storage_path='${storage_esc}', file_size=${size_num}::bigint WHERE id='${row_id_esc}';"
        ;;
      arc_original_voucher_file)
        echo "UPDATE arc_original_voucher_file SET storage_path='${storage_esc}', file_size=${size_num}::bigint WHERE id='${row_id_esc}';"
        ;;
      *)
        ;;
    esac
  done
  echo "COMMIT;"
} > "$UPDATES_SQL"

echo "resolved_count=${RESOLVED_COUNT}" | tee -a "$REPORT_FILE"
echo "unresolved_count=${UNRESOLVED_COUNT}" | tee -a "$REPORT_FILE"
echo "resolved_tsv=${RESOLVED_TSV}" | tee -a "$REPORT_FILE"
echo "unresolved_tsv=${UNRESOLVED_LEFT_TSV}" | tee -a "$REPORT_FILE"
echo "probe_tsv=${PROBE_TSV}" | tee -a "$REPORT_FILE"
echo "sql_file=${UPDATES_SQL}" | tee -a "$REPORT_FILE"

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
echo "External recovery finished."
echo "Report: ${REPORT_FILE}"
