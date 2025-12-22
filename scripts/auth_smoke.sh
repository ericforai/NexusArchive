#!/usr/bin/env bash
# Input: Shell、curl
# Output: 运维脚本逻辑
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

# 简易后端鉴权冒烟脚本
# 依赖: curl, jq

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-pass}"

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl 未安装"
command -v jq >/dev/null 2>&1 || fail "jq 未安装"

echo "登录用户: $USERNAME"
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.token // empty')
[[ -z "$TOKEN" ]] && fail "登录失败: $(echo "$LOGIN_RESP" | jq -r '.message // .code')"

echo "[OK] 登录成功，获取到 token"

echo "[CHECK] 访问 /admin/users (应 200)"
curl -sf -H "Authorization: Bearer $TOKEN" "$BASE_URL/admin/users" >/dev/null \
  || fail "/admin/users 访问失败"
echo "[OK] /admin/users 正常"

echo "[CHECK] 登出"
curl -sf -X POST -H "Authorization: Bearer $TOKEN" "$BASE_URL/auth/logout" >/dev/null \
  || fail "登出失败"
echo "[OK] 已登出，token 已入黑名单"

echo "[CHECK] 使用旧 token 再访问 /admin/users (应 401)"
if curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/admin/users" | grep -q "^401$"; then
  echo "[OK] 旧 token 已失效"
else
  fail "旧 token 未失效或接口未返回 401"
fi

echo "全部鉴权冒烟检查通过。"