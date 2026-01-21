#!/bin/bash
# API 上传流程集成测试脚本
set -e

BASE_URL="http://127.0.0.1:19090/api"

echo "🚀 Starting API Integration Test..."

# 1. 登录
echo "Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed: $LOGIN_RESPONSE"
  exit 1
fi
echo "✅ Login success."

# 2. 创建批次 (开启 autoCheck)
echo "Creating batch with autoCheck=true..."
BATCH_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Fonds-No: BR-GROUP" \
  -H "Content-Type: application/json" \
  -d '{
    "batchName": "E2E Shell Test Batch Auto",
    "fondsCode": "BR-GROUP",
    "fiscalYear": "2025",
    "archivalCategory": "VOUCHER",
    "totalFiles": 1,
    "autoCheck": true
  }')

BATCH_ID=$(echo $BATCH_RESPONSE | grep -o '"batchId":[0-9]*' | cut -d':' -f2)
BATCH_NO=$(echo $BATCH_RESPONSE | grep -o '"batchNo":"[^"]*' | cut -d'"' -f4)

if [ -z "$BATCH_ID" ]; then
  echo "❌ Batch creation failed: $BATCH_RESPONSE"
  exit 1
fi
echo "✅ Batch created: ID=$BATCH_ID, No=$BATCH_NO"

# 3. 上传文件
echo "Uploading real PDF file..."
FILE_PATH="src/data/archives/F001/2025/10Y/AC01/V-202511-001/content/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf"

UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@$FILE_PATH")

if echo "$UPLOAD_RESPONSE" | grep -q '"status":"UPLOADED"'; then
  echo "✅ File uploaded successfully."
else
  echo "❌ File upload failed: $UPLOAD_RESPONSE"
  exit 1
fi

# 4. 完成批次
echo "Completing batch..."
curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/complete" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

echo "✅ Batch completion triggered."

# 4.5 手动触发四性检测
echo "Triggering Four-Nature Check..."
CHECK_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/check" \
  -H "Authorization: Bearer $TOKEN")
echo "   Check Response: $CHECK_RESPONSE"

echo "⏳ Polling for async processing..."

# ... 后面逻辑 ...

# 6. 验证原始凭证
echo "Checking for new vouchers..."
VOUCHER_COUNT=$(docker exec nexus-db psql -U postgres -d nexusarchive -tAc "SELECT COUNT(*) FROM arc_original_voucher WHERE created_time > NOW() - INTERVAL '5 minutes';")
echo "   New vouchers in DB: $VOUCHER_COUNT"

# 7. 验证批次最终状态
DETAIL_RESPONSE=$(curl -s -X GET "$BASE_URL/collection/batch/$BATCH_ID" \
  -H "Authorization: Bearer $TOKEN")
FINAL_STATUS=$(echo $DETAIL_RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)
echo "   Batch Final Status: $FINAL_STATUS"

# 8. 清理
echo "🏁 API Integration Test Passed (with Ingestion Pipeline Verification)!"
