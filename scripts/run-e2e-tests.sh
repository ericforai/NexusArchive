#!/bin/bash

# Input: 无
# Output: E2E测试执行脚本
# Pos: 测试执行脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   NexusArchive 端到端测试执行${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查环境变量
BASE_URL=${BASE_URL:-"http://localhost:15175"}
PW_USER=${PW_USER:-"admin"}
PW_PASS=${PW_PASS:-"admin123"}

echo -e "${YELLOW}测试环境配置:${NC}"
echo "  BASE_URL: $BASE_URL"
echo "  PW_USER: $PW_USER"
echo "  PW_PASS: $PW_PASS"
echo ""

# 检查服务是否运行
echo -e "${YELLOW}检查服务状态...${NC}"
if ! curl -s "$BASE_URL" > /dev/null; then
    echo -e "${RED}错误: 无法连接到前端服务 ($BASE_URL)${NC}"
    echo "请确保前端服务正在运行: npm run dev"
    exit 1
fi

# 检查Playwright是否安装
if ! command -v npx &> /dev/null; then
    echo -e "${RED}错误: 未找到 npx${NC}"
    exit 1
fi

# 测试模式选择
echo -e "${YELLOW}请选择测试模式:${NC}"
echo "  1) 快速冒烟测试 (Smoke Tests)"
echo "  2) P0功能测试 (核心功能)"
echo "  3) P1功能测试 (重要功能)"
echo "  4) 完整测试套件 (All Tests)"
echo "  5) API集成测试"
echo "  6) 业务流程测试"
echo ""

read -p "请输入选项 (1-6): " TEST_MODE

case $TEST_MODE in
    1)
        echo -e "${GREEN}执行快速冒烟测试...${NC}"
        npx playwright test tests/playwright/ui/smoke_core_paths.spec.ts \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    2)
        echo -e "${GREEN}执行P0功能测试...${NC}"
        npx playwright test tests/playwright/ui/ \
            --grep "@P0" \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    3)
        echo -e "${GREEN}执行P1功能测试...${NC}"
        npx playwright test tests/playwright/ui/ \
            --grep "@P1" \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    4)
        echo -e "${GREEN}执行完整测试套件...${NC}"
        npx playwright test tests/playwright/ \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    5)
        echo -e "${GREEN}执行API集成测试...${NC}"
        npx playwright test tests/playwright/api/ \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    6)
        echo -e "${GREEN}执行业务流程测试...${NC}"
        npx playwright test tests/playwright/workflows/ \
            --base-url="$BASE_URL" \
            --env BASE_URL="$BASE_URL",PW_USER="$PW_USER",PW_PASS="$PW_PASS"
        ;;
    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac

# 显示测试报告
echo ""
echo -e "${GREEN}测试完成！${NC}"
echo -e "${YELLOW}查看详细报告:${NC}"
echo "  npx playwright show-report"
echo ""



