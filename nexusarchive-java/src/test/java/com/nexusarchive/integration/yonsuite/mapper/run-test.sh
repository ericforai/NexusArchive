#!/bin/bash
# 快速运行 YonVoucherMapperTest 的脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../../../.." && pwd)"

echo "========================================="
echo "YonVoucherMapper Test Runner"
echo "========================================="
echo "Project Root: $PROJECT_ROOT"
echo "Test Class: YonVoucherMapperTest"
echo ""

cd "$PROJECT_ROOT"

echo "Step 1: 清理并编译主代码..."
mvn clean compile -DskipTests -q

echo "Step 2: 编译测试代码..."
mvn test-compile -DskipTests -q

echo "Step 3: 运行 YonVoucherMapperTest..."
mvn surefire:test -Dtest=YonVoucherMapperTest

echo ""
echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "查看覆盖率报告:"
echo "  open $PROJECT_ROOT/target/site/jacoco/index.html"
