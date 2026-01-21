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

# 2. 创建批次
echo "Creating batch..."
BATCH_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Fonds-No: BR-GROUP" \
  -H "Content-Type: application/json" \
  -d '{
    "batchName": "E2E Shell Test Batch",
    "fondsCode": "BR-GROUP",
    "fiscalYear": "2025",
    "archivalCategory": "VOUCHER",
    "totalFiles": 1
  }')

BATCH_ID=$(echo $BATCH_RESPONSE | grep -o '"batchId":[0-9]*' | cut -d':' -f2)

if [ -z "$BATCH_ID" ]; then
  echo "❌ Batch creation failed: $BATCH_RESPONSE"
  exit 1
fi
echo "✅ Batch created: ID=$BATCH_ID"

# 3. 上传文件
echo "Uploading file..."
echo "dummy content for E2E test" > test_api_upload.pdf
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test_api_upload.pdf")

if echo "$UPLOAD_RESPONSE" | grep -q '"status":"UPLOADED"'; then
  echo "✅ File uploaded successfully."
else
  echo "❌ File upload failed: $UPLOAD_RESPONSE"
  rm test_api_upload.pdf
  exit 1
fi

# 4. 完成批次
echo "Completing batch..."
COMPLETE_RESPONSE=$(curl -s -X POST "$BASE_URL/collection/batch/$BATCH_ID/complete" \
  -H "Authorization: Bearer $TOKEN")

if echo "$COMPLETE_RESPONSE" | grep -q '"status"'; then
  echo "✅ Batch completion triggered."
  echo "   Response: $COMPLETE_RESPONSE"
else
  echo "❌ Batch completion failed: $COMPLETE_RESPONSE"
  rm test_api_upload.pdf
  exit 1
fi

# 5. 验证批次详情
echo "Verifying batch status..."
DETAIL_RESPONSE=$(curl -s -X GET "$BASE_URL/collection/batch/$BATCH_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "   Final Status: $(echo $DETAIL_RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)"

# 6. 清理
rm test_api_upload.pdf
echo "🏁 API Integration Test Passed!"
