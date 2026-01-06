#!/bin/bash
#===============================================================================
# Git Pre-commit Hook: 检查 Bash 脚本的环境变量导出
#===============================================================================
# 用途: 防止提交忘记导出环境变量的脚本
#
# 检查规则:
#   - 如果脚本中有 'source .env' 或 'source .env.*'
#   - 则必须前面有 'set -a'，后面有 'set +a'
#
# 修复方法:
#   source .env.local
#   改为:
#   set -a
#   source .env.local
#   set +a
#===============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 计数器
ERRORS=0
FIXED=0

echo -e "${YELLOW}🔍 检查 Bash 脚本的环境变量导出...${NC}"

# 获取所有 staged 的 .sh 文件
SH_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.sh$' || true)

if [ -z "$SH_FILES" ]; then
    echo -e "${GREEN}✅ 没有 Bash 脚本变更，跳过检查${NC}"
    exit 0
fi

# 检查每个文件
for FILE in $SH_FILES; do
    if [ ! -f "$FILE" ]; then
        continue
    fi

    echo "  检查: $FILE"

    # 检查是否有 source .env 但没有 set -a
    if grep -q "source.*\.env" "$FILE" 2>/dev/null; then
        # 检查是否有正确的 set -a ... set +a 包裹
        CONTENT=$(cat "$FILE")
        HAS_SET_A=0
        HAS_SET_PLUS_A=0
        HAS_SOURCE_ENV=0
        LINE_NUM=0

        while IFS= read -r LINE; do
            LINE_NUM=$((LINE_NUM + 1))

            # 跳过注释和空行
            if [[ "$LINE" =~ ^[[:space:]]*# ]]; then
                continue
            fi

            # 检查 source .env
            if echo "$LINE" | grep -q "source.*\.env"; then
                HAS_SOURCE_ENV=1
                SOURCE_LINE=$LINE_NUM
            fi

            # 检查 set -a
            if echo "$LINE" | grep -q "set.*-a"; then
                HAS_SET_A=1
            fi

            # 检查 set +a
            if echo "$LINE" | grep -q "set.*+a"; then
                HAS_SET_PLUS_A=1
            fi
        done < "$FILE"

        # 如果有 source .env 但没有正确的 set -a 包裹
        if [ $HAS_SOURCE_ENV -eq 1 ] && ([ $HAS_SET_A -eq 0 ] || [ $HAS_SET_PLUS_A -eq 0 ]); then
            echo -e "    ${RED}❌ 第 $SOURCE_LINE 行: source .env 没有正确的 set -a 包裹${NC}"
            echo -e "    ${YELLOW}修复方法:${NC}"
            echo -e "      将:"
            echo -e "        ${YELLOW}source .env.local${NC}"
            echo -e "      改为:"
            echo -e "        ${GREEN}set -a         # 自动导出所有后续变量${NC}"
            echo -e "        ${GREEN}source .env.local${NC}"
            echo -e "        ${GREEN}set +a         # 停止自动导出${NC}"
            ERRORS=$((ERRORS + 1))
        else
            echo -e "    ${GREEN}✓ 环境变量导出正确${NC}"
        fi
    else
        echo -e "    ${GREEN}✓ 无环境变量加载${NC}"
    fi
done

echo ""

# 如果有错误，阻止提交
if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}❌ 发现 $ERRORS 个问题，提交被阻止！${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}💡 为什么需要这个检查？${NC}"
    echo -e "  Bash 的 'source' 命令不会自动导出变量到子进程。"
    echo -e "  如果脚本中 'source .env.local' 后启动子进程（如 mvn spring-boot:run），"
    echo -e "  环境变量将无法传递，导致配置失效。"
    echo ""
    echo -e "${YELLOW}💡 快速修复:${NC}"
    echo -e "  1. 按照上面的修复方法修改脚本"
    echo -e "  2. 再次 git add 和 git commit"
    echo ""
    echo -e "${YELLOW}💡 想跳过检查？（不推荐）${NC}"
    echo -e "  git commit --no-verify"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 所有检查通过！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

exit 0
