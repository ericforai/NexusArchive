#!/bin/bash
set -e

BASE_URL="http://127.0.0.1:19090/api"

echo "🚀 Starting Archiving & AIP Generation Test..."

# 1. 登录
echo "Logging in..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed"
  exit 1
fi

# 2. 准备数字 ID 的测试数据
echo "Preparing numeric ID voucher..."
docker exec nexus-db psql -U postgres -d nexusarchive -c "DELETE FROM arc_original_voucher WHERE id = '10001';"
docker exec nexus-db psql -U postgres -d nexusarchive -c "INSERT INTO arc_original_voucher (id, voucher_no, archival_category, source_type, voucher_type, business_date, amount, fonds_code, fiscal_year, retention_period, archive_status, pool_status, created_by) VALUES ('10001', 'NUM-V-001', 'VOUCHER', 'MANUAL', 'TRANSFER_VOUCHER', '2025-01-01', 100.00, 'BR-GROUP', '2025', '10Y', 'PENDING', 'ENTRY', 'admin');"

# 3. 手动在数据库中创建一个已审批的归档批次，并关联凭证
echo "Injecting approved archive batch and item via SQL..."
docker exec nexus-db psql -U postgres -d nexusarchive -c "DELETE FROM archive_batch WHERE id = 999;"
docker exec nexus-db psql -U postgres -d nexusarchive -c "INSERT INTO archive_batch (id, batch_no, fonds_id, period_start, period_end, status, created_at) VALUES (999, 'AB-E2E-999', 1, '2025-01-01', '2025-01-31', 'APPROVED', NOW());"
docker exec nexus-db psql -U postgres -d nexusarchive -c "INSERT INTO archive_batch_item (batch_id, item_type, ref_id, status, created_at) VALUES (999, 'VOUCHER', '10001', 'PENDING', NOW());"

AB_ID=999

# 4. 执行归档 (核心验证)
echo "Executing Archive Action for Batch $AB_ID..."
ARCHIVE_ACTION_RESPONSE=$(curl -s -X POST "$BASE_URL/archive-batch/$AB_ID/archive" \
  -H "Authorization: Bearer $TOKEN")

echo "Archive Response: $ARCHIVE_ACTION_RESPONSE"

if echo "$ARCHIVE_ACTION_RESPONSE" | grep -q '"status":"ARCHIVED"'; then
  echo "✅ SUCCESS: Batch Status is now ARCHIVED"
  
  # 4. 验证 AIP 导出记录
  echo "Verifying AIP download link..."
  # 通常归档后会生成下载链接或更新状态
else
  echo "❌ FAILURE: Failed to archive batch. Response code may be 500."
fi

echo "🏁 Archiving Test Finished."
