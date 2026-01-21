#!/bin/bash
set -e

echo "🧪 Verifying Compliance Logic..."

# 1. 重置数据并运行检测测试
bash tests/verify_seed_data.sh > /dev/null
OUTPUT=$(bash tests/integration/compliance_check_test.sh)

echo "$OUTPUT"

# 2. 验证检测结果是否包含失败计数 (因为数据是伪造的，所以必然失败)
if echo "$OUTPUT" | grep -q '"failedFiles":2'; then
    echo "✅ SUCCESS: Detection logic correctly flagged invalid files as FAILED."
    exit 0
else
    echo "❌ FAILURE: Detection logic did not report failure as expected."
    exit 1
fi
