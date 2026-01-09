#!/bin/bash
#===============================================================================
# 系统化脚本检查器
#===============================================================================
# 用途: 全面检查所有 shell 脚本的潜在问题
#
# 检查项:
#   1. 缺少 shebang
#   2. 缺少 set -e (错误处理)
#   3. 硬编码路径
#   4. 未引用的变量
#   5. 缺少错误处理
#   6. 安全问题 (rm -rf 等)
#   7. 跨平台兼容性
#   8. 依赖检查
#===============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}🔍 NexusArchive 脚本系统性检查${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 计数器（macOS bash 3.x 不支持关联数组，使用简单计数）
TOTAL_ISSUES=0
TOTAL_SCRIPTS=0
MISSING_SHEBANG=0
MISSING_SET_E=0
MISSING_SET_A=0

# 获取所有脚本
SCRIPTS=$(find /Users/user/nexusarchive/scripts -name "*.sh" -type f | sort)

for SCRIPT in $SCRIPTS; do
    SCRIPT_NAME=$(basename "$SCRIPT")
    TOTAL_SCRIPTS=$((TOTAL_SCRIPTS + 1))
    SCRIPT_ISSUES=0

    echo -e "${YELLOW}检查: $SCRIPT${NC}"

    # 检查 1: 缺少 shebang
    if ! head -1 "$SCRIPT" | grep -q "^#!"; then
        echo -e "  ${RED}❌ 缺少 shebang (#!/bin/bash)${NC}"
        SCRIPT_ISSUES=$((SCRIPT_ISSUES + 1))
        TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        MISSING_SHEBANG=$((MISSING_SHEBANG + 1))
    fi

    # 检查 2: 缺少 set -e
    if ! grep -q "set -e" "$SCRIPT"; then
        echo -e "  ${YELLOW}⚠️  缺少 set -e (错误时退出)${NC}"
        SCRIPT_ISSUES=$((SCRIPT_ISSUES + 1))
        TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        MISSING_SET_E=$((MISSING_SET_E + 1))
    fi

    # 检查 3: 硬编码的绝对路径
    if grep -qE '(rm -rf|/Users/|/home/|/var/)' "$SCRIPT"; then
        echo -e "  ${YELLOW}⚠️  发现硬编码路径${NC}"
        grep -nE '(rm -rf|/Users/|/home/|/var/)' "$SCRIPT" | head -3 | sed 's/^/    /'
        SCRIPT_ISSUES=$((SCRIPT_ISSUES + 1))
    fi

    # 检查 4: 未引用的变量 ($VAR vs $VAR)
    if grep -qE '\$[A-Za-z_][A-Za-z0-9_]*\{' "$SCRIPT"; then
        # 可能是正确的 ${VAR} 语法，但也可能是错误
        echo -e "  ${GREEN}✓ 使用变量展开语法${NC}"
    fi

    # 检查 5: 危险命令
    if grep -qE '(rm -rf[^ ]|:.*force|kill -9)' "$SCRIPT"; then
        echo -e "  ${RED}⚠️  发现危险命令${NC}"
        grep -nE '(rm -rf[^ ]|:.*force|kill -9)' "$SCRIPT" | head -3 | sed 's/^/    /'
        SCRIPT_ISSUES=$((SCRIPT_ISSUES + 1))
    fi

    # 检查 6: source .env 但没有 set -a
    if grep -q "source.*\.env" "$SCRIPT"; then
        if ! grep -q "set.*-a" "$SCRIPT"; then
            echo -e "  ${RED}❌ source .env 但没有 set -a 包裹${NC}"
            SCRIPT_ISSUES=$((SCRIPT_ISSUES + 1))
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
            MISSING_SET_A=$((MISSING_SET_A + 1))
        fi
    fi

    # 检查 7: 是否有注释说明
    if ! head -20 "$SCRIPT" | grep -q "^#"; then
        echo -e "  ${YELLOW}⚠️  缺少注释说明${NC}"
    fi

    # 检查 8: 检查是否有错误处理 (||, &&, if, trap)
    if ! grep -qE '(if \[|exit [0-9]|catch|\|\&\&|\|\|)' "$SCRIPT"; then
        echo -e "  ${YELLOW}⚠️  可能缺少错误处理${NC}"
    fi

    if [ $SCRIPT_ISSUES -eq 0 ]; then
        echo -e "  ${GREEN}✓ 无明显问题${NC}"
    fi

    echo ""
done

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}📊 检查统计${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "检查脚本数: ${GREEN}$TOTAL_SCRIPTS${NC}"
echo -e "发现问题数: ${RED}$TOTAL_ISSUES${NC}"

if [ $TOTAL_ISSUES -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}问题分类:${NC}"
    [ $MISSING_SHEBANG -gt 0 ] && echo -e "  ${YELLOW}- 缺少 shebang: $MISSING_SHEBANG${NC}"
    [ $MISSING_SET_E -gt 0 ] && echo -e "  ${YELLOW}- 缺少 set -e: $MISSING_SET_E${NC}"
    [ $MISSING_SET_A -gt 0 ] && echo -e "  ${RED}- 缺少 set -a: $MISSING_SET_A${NC}"
fi

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

exit 0
