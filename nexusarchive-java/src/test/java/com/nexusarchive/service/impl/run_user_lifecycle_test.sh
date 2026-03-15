#!/bin/bash
# UserLifecycleServiceImpl 测试执行脚本

echo "========================================="
echo "UserLifecycleServiceImpl 测试执行"
echo "========================================="
echo ""

cd /Users/user/nexusarchive/nexusarchive-java

echo "1. 编译测试类..."
mvn test-compile -DskipTests 2>&1 | grep -E "(BUILD SUCCESS|BUILD FAILURE|ERROR)" | head -10

echo ""
echo "2. 运行 UserLifecycleServiceImplTest..."
mvn test -Dtest=UserLifecycleServiceImplTest -DfailIfNoTests=false 2>&1 | grep -A 20 "UserLifecycleServiceImplTest"

echo ""
echo "3. 查看测试结果摘要..."
mvn test -Dtest=UserLifecycleServiceImplTest -DfailIfNoTests=false 2>&1 | grep -E "(Tests run|Failures|Errors|Skipped)" | tail -5

echo ""
echo "4. 生成覆盖率报告..."
mvn jacoco:report -Dtest=UserLifecycleServiceImplTest 2>&1 | grep -E "(BUILD SUCCESS|BUILD FAILURE|ERROR)" | head -5

echo ""
echo "测试完成！"
echo "测试报告位置: target/surefire-reports/"
echo "覆盖率报告位置: target/site/jacoco/index.html"
echo ""
