#!/usr/bin/env bash
# Input: Shell、prod_attachment_audit.sh
# Output: 发布门禁结果（missing_physical_files > 阈值则失败）
# Pos: 生产发布前门禁脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

SCAN_LIMIT="${SCAN_LIMIT:-200000}"
SAMPLE_LIMIT="${SAMPLE_LIMIT:-300}"
MAX_MISSING_ALLOWED="${MAX_MISSING_ALLOWED:-0}"
TARGET_FILE_ID="${TARGET_FILE_ID:-f4653466-b670-a083-acff-19ff6d55be02}"
WORK_DIR="${WORK_DIR:-/tmp/nexusarchive_attachment_gate_$(date +%Y%m%d_%H%M%S)}"

if ! [[ "$SCAN_LIMIT" =~ ^[0-9]+$ ]]; then
  echo "SCAN_LIMIT must be an integer"
  exit 1
fi

if ! [[ "$SAMPLE_LIMIT" =~ ^[0-9]+$ ]]; then
  echo "SAMPLE_LIMIT must be an integer"
  exit 1
fi

if ! [[ "$MAX_MISSING_ALLOWED" =~ ^[0-9]+$ ]]; then
  echo "MAX_MISSING_ALLOWED must be an integer"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUDIT_SCRIPT="${SCRIPT_DIR}/prod_attachment_audit.sh"

if [ ! -x "$AUDIT_SCRIPT" ]; then
  echo "Audit script not found or not executable: ${AUDIT_SCRIPT}"
  exit 1
fi

mkdir -p "$WORK_DIR"
AUDIT_OUT_DIR="${WORK_DIR}/audit"
mkdir -p "$AUDIT_OUT_DIR"

OUT_DIR="$AUDIT_OUT_DIR" \
SCAN_LIMIT="$SCAN_LIMIT" \
SAMPLE_LIMIT="$SAMPLE_LIMIT" \
TARGET_FILE_ID="$TARGET_FILE_ID" \
bash "$AUDIT_SCRIPT" >/dev/null

REPORT_FILE="${AUDIT_OUT_DIR}/report.txt"
if [ ! -f "$REPORT_FILE" ]; then
  echo "Audit report not found: ${REPORT_FILE}"
  exit 1
fi

MISSING_COUNT="$(awk -F'=' '/^missing_physical_files=/{print $2}' "$REPORT_FILE" | tail -n 1 | tr -d '[:space:]')"
if [ -z "$MISSING_COUNT" ] || ! [[ "$MISSING_COUNT" =~ ^[0-9]+$ ]]; then
  MISSING_COUNT="$(awk -F': ' '/Missing physical files:/{print $2}' "$REPORT_FILE" | tail -n 1 | tr -d '[:space:]')"
fi

if [ -z "$MISSING_COUNT" ] || ! [[ "$MISSING_COUNT" =~ ^[0-9]+$ ]]; then
  echo "Unable to parse missing_physical_files from ${REPORT_FILE}"
  exit 1
fi

echo "=== Attachment Gate ==="
echo "work_dir=${WORK_DIR}"
echo "report_file=${REPORT_FILE}"
echo "missing_physical_files=${MISSING_COUNT}"
echo "max_missing_allowed=${MAX_MISSING_ALLOWED}"

if [ "$MISSING_COUNT" -gt "$MAX_MISSING_ALLOWED" ]; then
  echo "Attachment gate FAILED: missing_physical_files=${MISSING_COUNT} > ${MAX_MISSING_ALLOWED}"
  exit 42
fi

echo "Attachment gate PASSED."
