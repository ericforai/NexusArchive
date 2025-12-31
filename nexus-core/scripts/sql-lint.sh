#!/usr/bin/env bash
# Input: SQL/XML 文件
# Output: PG 特有语法扫描结果
# Pos: Sprint 0 SQL Lint
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCAN_DIRS=("${ROOT_DIR}/src/main/resources" "${ROOT_DIR}/src/main/java")
INCLUDE_GLOBS=("*.sql" "*.xml")
EXCLUDE_GLOBS=("**/db/postgresql/**" "**/db/pg/**" "**/adapter/postgresql/**")

BANNED_PATTERNS=(
  "\\bjsonb\\b"
  "\\bjsonb_"
  "\\bto_tsvector\\b"
  "\\bto_tsquery\\b"
  "\\bILIKE\\b"
  "\\bON\\s+CONFLICT\\b"
  "\\bRETURNING\\b"
  "::[A-Za-z]"
  "->>"
)

if command -v rg >/dev/null 2>&1; then
  SEARCH_TOOL="rg"
else
  SEARCH_TOOL="grep"
fi

fail=0

scan_pattern() {
  local pattern="$1"
  local matches=""

  if [ "${SEARCH_TOOL}" = "rg" ]; then
    local args=(rg -n --no-heading --with-filename --hidden)
    for glob in "${INCLUDE_GLOBS[@]}"; do
      args+=(--glob "${glob}")
    done
    for glob in "${EXCLUDE_GLOBS[@]}"; do
      args+=(--glob "!${glob}")
    done
    matches="$(${args[@]} -e "${pattern}" "${SCAN_DIRS[@]}" 2>/dev/null || true)"
  else
    matches="$(grep -RInE --include='*.sql' --include='*.xml' "${pattern}" "${SCAN_DIRS[@]}" 2>/dev/null || true)"
  fi

  if [ -n "${matches}" ]; then
    matches="$(printf '%s\n' "${matches}" | grep -v "sql-lint: allow-pg" || true)"
  fi

  if [ -n "${matches}" ]; then
    echo "[SQL-LINT] 命中 PG 特有语法: ${pattern}"
    printf '%s\n' "${matches}"
    fail=1
  fi
}

for pattern in "${BANNED_PATTERNS[@]}"; do
  scan_pattern "${pattern}"
done

if [ "${fail}" -ne 0 ]; then
  echo "[SQL-LINT] 发现 PG 特有语法，请移至适配层或替换为标准 SQL。"
  exit 1
fi

echo "[SQL-LINT] 未发现 PG 特有语法。"
