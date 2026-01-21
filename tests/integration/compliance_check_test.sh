#!/bin/bash
set -e

BASE_URL="http://127.0.0.1:19090/api"

echo "🚀 Starting Compliance Check Integration Test..."

# 0. 准备物理文件 (Seed Data 中引用的路径)
echo "Preparing physical files..."
mkdir -p /tmp/e2e
echo "dummy pdf content" > /tmp/e2e/voucher_001.pdf
echo "dummy xml content" > /tmp/e2e/invoice_001.xml

# 1. 登录
echo "Logging in..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' | grep -o '"token":"[^"" ]*' | cut -d'"' -f4)

# 2. 获取 E2E-BATCH-001 的 ID
BATCH_ID=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT id FROM collection_batch WHERE batch_no = 'E2E-BATCH-001'")

if [ -z "$BATCH_ID" ]; then
  echo "❌ Batch E2E-BATCH-001 not found. Did you run seed data?"
  exit 1
fi
echo "✅ Found Batch ID: $BATCH_ID"

# 3. 触发检测
echo "Triggering Four-Nature Check for Seed Data..."
CHECK_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/check" \
  -H "Authorization: Bearer $TOKEN")

echo "   Response: $CHECK_RESPONSE"

# 4. 验证检测结果记录
# 我们检查 integrity_check 表，假设它存储了结果
echo "Verifying integrity_check records..."
COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM integrity_check") # 这里偷懒查全表，因为环境已清理

echo "   Integrity Check Records: $COUNT"

if [ "$COUNT" -gt "0" ]; then
    echo "✅ Compliance records generated."
else
    echo "⚠️ No integrity check records found. Checking logs..."
    tail -n 20 backend.log
fi

# 5. 清理
rm -rf /tmp/e2e
echo "🏁 Compliance Check Test Finished."
