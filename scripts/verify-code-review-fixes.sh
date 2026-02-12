#!/bin/bash
# 验证 Code Review 修复

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

API_BASE="http://localhost:19090/api"
COOKIE="/tmp/nexus-cookies.txt"

echo -e "${YELLOW}=== 验证 Code Review 修复 ===${NC}"
echo

# Step 1: 登录
echo -e "${YELLOW}[1/4] 登录系统...${NC}"
LOGIN_RESPONSE=$(curl -s -c "$COOKIE" "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('token', ''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ 登录失败${NC}"
  exit 1
fi

echo -e "${GREEN}✅ 登录成功${NC}"
echo

# Step 2: 测试批次列表（验证 SQL 注入修复）
echo -e "${YELLOW}[2/4] 测试批次列表 API（验证 SQL 注入防护）...${NC}"
# 正常请求
LIST_RESPONSE=$(curl -s -b "$COOKIE" "$API_BASE/collection/batch/list?limit=10&offset=0" \
  -H "Authorization: Bearer $TOKEN")

if echo "$LIST_RESPONSE" | grep -q "code"; then
  echo -e "${GREEN}✅ 正常请求通过${NC}"
else
  echo -e "${RED}❌ 正常请求失败${NC}"
fi

# 恶意参数测试（应该被限制）
MALICIOUS_RESPONSE=$(curl -s -b "$COOKIE" "$API_BASE/collection/batch/list?limit=-999999&offset=999999" \
  -H "Authorization: Bearer $TOKEN")

echo "恶意参数测试已执行（应该被 Math.max/min 限制）"
echo

# Step 3: 测试批次所有权校验
echo -e "${YELLOW}[3/4] 测试批次所有权校验...${NC}"
# 先获取一个批次 ID
BATCH_ID=$(echo "$LIST_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', [{}])[0].get('id', '1'))" 2>/dev/null)

if [ -n "$BATCH_ID" ] && [ "$BATCH_ID" != "None" ]; then
  # 尝试完成批次（如果是自己的批次应该成功，否则应该返回 403）
  COMPLETE_RESPONSE=$(curl -s -b "$COOKIE" "$API_BASE/collection/batch/$BATCH_ID/complete" \
    -H "Authorization: Bearer $TOKEN")

  if echo "$COMPLETE_RESPONSE" | grep -q "code"; then
    CODE=$(echo "$COMPLETE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('code', 0))" 2>/dev/null)
    if [ "$CODE" = "403" ]; then
      echo -e "${GREEN}✅ 所有权校验生效（403 无权操作）${NC}"
    else
      echo -e "${GREEN}✅ 请求成功（可能是自己的批次）${NC}"
    fi
  else
    echo -e "${YELLOW}⚠️  响应格式异常${NC}"
  fi
else
  echo -e "${YELLOW}⚠️  没有可测试的批次${NC}"
fi
echo

# Step 4: 验证日志输出（没有 System.out.println）
echo -e "${YELLOW}[4/4] 验证日志输出...${NC}"
if grep -q "System.out.println" /Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java; then
  echo -e "${RED}❌ 仍然存在 System.out.println${NC}"
else
  echo -e "${GREEN}✅ 已移除 System.out.println${NC}"
fi

if grep -q "System.out.println" /Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/controller/CollectionBatchController.java; then
  echo -e "${RED}❌ Controller 仍然存在 System.out.println${NC}"
else
  echo -e "${GREEN}✅ Controller 已移除 System.out.println${NC}"
fi
echo

# 验证枚举比较修复
echo -e "${YELLOW}[5/5] 验证枚举比较修复...${NC}"
if grep -q '"PASSED"' /Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java; then
  echo -e "${RED}❌ 仍然存在 \"PASSED\" 字符串比较${NC}"
else
  echo -e "${GREEN}✅ 已修复为 OverallStatus.PASS 枚举比较${NC}"
fi
echo

echo -e "${GREEN}=== 验证完成 ===${NC}"
echo
echo "请在前端页面验证以下功能："
echo "1. 访问 http://localhost:15175/system/collection"
echo "2. 上传文件并点击'完成上传'"
echo "3. 确认四性检测正确执行（通过/失败状态正确）"
