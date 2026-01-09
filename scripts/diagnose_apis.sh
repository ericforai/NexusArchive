#!/bin/bash
BASE_URL="http://localhost:19090/api"

echo "=== 1. 登录获取 Token ==="
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "登录失败: $LOGIN_RESP"
    exit 1
fi

echo "=== 2. 诊断 /api/pool/list ==="
curl -i -H "Authorization: Bearer $TOKEN" "$BASE_URL/pool/list"

echo -e "\n\n=== 3. 诊断 /api/archive-batch ==="
curl -i -H "Authorization: Bearer $TOKEN" "$BASE_URL/archive-batch?page=1&size=10"

echo -e "\n\n=== 4. 诊断 /api/audit-logs ==="
curl -i -H "Authorization: Bearer $TOKEN" "$BASE_URL/audit-logs?page=1&limit=20"
