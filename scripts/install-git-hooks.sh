#!/bin/bash
#===============================================================================
# Git Hooks 安装脚本
#===============================================================================
# 用途: 自动安装项目所需的 Git hooks
#
# 使用:
#   bash scripts/install-git-hooks.sh
#
# 说明:
#   - 创建 .git/hooks 目录的符号链接
#   - 每次提交时自动运行检查
#===============================================================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}🔧 安装 Git Hooks${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 检查是否在 Git 仓库中
if [ ! -d .git ]; then
    echo -e "${RED}❌ 错误: 当前目录不是 Git 仓库根目录${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}📁 创建 Git hooks 目录...${NC}"
mkdir -p .git/hooks

echo -e "${YELLOW}🔗 安装 hooks...${NC}"

# 安装 pre-commit hook
HOOK_NAME="pre-commit"
HOOK_FILE="scripts/git-hooks/check-env-export.sh"
HOOK_PATH=".git/hooks/$HOOK_NAME"

# 删除已存在的 hook
if [ -f "$HOOK_PATH" ]; then
    echo -e "  ${YELLOW}⚠️  已存在 $HOOK_NAME，将覆盖${NC}"
    rm "$HOOK_PATH"
fi

# 创建 hook 脚本
cat > "$HOOK_PATH" << HOOK_EOF
#!/bin/bash
# Git $HOOK_NAME hook
# 此文件由 scripts/install-git-hooks.sh 自动生成
# 请勿手动编辑

SCRIPT_DIR="\$(git rev-parse --show-toplevel)/scripts"
HOOK_SCRIPT="\$SCRIPT_DIR/git-hooks/check-env-export.sh"

if [ -f "\$HOOK_SCRIPT" ]; then
    bash "\$HOOK_SCRIPT"
    EXIT_CODE=\$?
    if [ \$EXIT_CODE -ne 0 ]; then
        exit \$EXIT_CODE
    fi
fi
HOOK_EOF

chmod +x "$HOOK_PATH"
echo -e "  ${GREEN}✓ $HOOK_NAME → $HOOK_FILE${NC}"

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Git hooks 安装完成！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${CYAN}📋 已安装的 hooks:${NC}"
echo -e "  ${YELLOW}pre-commit${NC}      - 检查 Bash 脚本的环境变量导出"
echo ""
echo -e "${CYAN}💡 提示:${NC}"
echo -e "  • Hook 在每次提交时自动运行"
echo -e "  • 如需跳过检查: ${YELLOW}git commit --no-verify${NC}"
echo -e "  • 如需重新安装: ${YELLOW}bash scripts/install-git-hooks.sh${NC}"
echo ""
echo -e "${CYAN}📚 了解更多:${NC}"
echo -e "  查看 scripts/git-hooks/ 目录中的检查脚本"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
