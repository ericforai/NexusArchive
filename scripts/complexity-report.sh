#!/bin/bash
# ==============================================================================
# 复杂度报告生成脚本
# ==============================================================================
# 用途: 生成前端和后端的综合复杂度分析报告
#
# 使用:
#   bash scripts/complexity-report.sh
#   或: npm run complexity:report
#
# 输出:
#   - reports/complexity-report-YYYYMMDD-HHMMSS.json  (前端 ESLint 详细报告)
#   - reports/complexity-report-YYYYMMDD-HHMMSS.md    (综合 Markdown 报告)
#
# 说明:
#   - 前端: 使用 ESLint 复杂度规则检查 .eslintrc.complexity.cjs
#   - 后端: 运行 ArchUnit 架构测试捕获控制台输出
#   - 报告包含时间戳，便于历史对比
# ==============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ==============================================================================
# 辅助函数
# ==============================================================================

# 跨平台获取文件修改时间
get_file_timestamp() {
    local file="$1"
    if [ ! -f "$file" ]; then
        date +"%Y-%m-%d %H:%M:%S"
        return
    fi

    # macOS: stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S"
    # Linux: stat -c "%y" (需要截断小数部分)
    if stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$file" >/dev/null 2>&1; then
        # macOS
        stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$file"
    elif stat -c "%y" "$file" >/dev/null 2>&1; then
        # Linux - 截断小数点后的时间
        stat -c "%y" "$file" | cut -d'.' -f1
    else
        # 回退到当前时间
        date +"%Y-%m-%d %H:%M:%S"
    fi
}

# ==============================================================================
# 生成时间戳
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
REPORT_DIR="reports"
FRONTEND_JSON="${REPORT_DIR}/complexity-report-${TIMESTAMP}.json"
FRONTEND_JSON_LATEST="${REPORT_DIR}/complexity-report-latest.json"
MARKDOWN_REPORT="${REPORT_DIR}/complexity-report-${TIMESTAMP}.md"
MARKDOWN_REPORT_LATEST="${REPORT_DIR}/complexity-report-latest.md"
BACKEND_OUTPUT="${REPORT_DIR}/archunit-backend-${TIMESTAMP}.txt"

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}📊 生成 NexusArchive 复杂度报告${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "时间戳: ${TIMESTAMP}"
echo -e ""

# ==============================================================================
# 1. 创建报告目录
# ==============================================================================
echo -e "${YELLOW}📁 创建报告目录...${NC}"
mkdir -p "$REPORT_DIR"
echo -e "${GREEN}✅ 报告目录: ${REPORT_DIR}${NC}"

# ==============================================================================
# 2. 前端复杂度检查 (ESLint)
# ==============================================================================
echo ""
echo -e "${YELLOW}🔍 检查前端复杂度 (ESLint)...${NC}"

FRONTEND_STATUS="✅ 通过"
FRONTEND_VIOLATIONS=0
FRONTEND_WARNINGS=0

if npx eslint src --ext .ts,.tsx -c .eslintrc.complexity.cjs --format json --output-file "$FRONTEND_JSON" 2>/dev/null; then
    # 统计违规数量
    if [ -f "$FRONTEND_JSON" ]; then
        FRONTEND_VIOLATIONS=$(node -e "
            try {
                const data = require('./${FRONTEND_JSON}');
                let count = 0;
                for (const file of data) {
                    count += (file.messages || []).length;
                }
                console.log(count);
            } catch (e) {
                console.error('Error:', e.message);
                process.exit(1);
            }
        " 2>/dev/null || echo "0")
        FRONTEND_WARNINGS=$FRONTEND_VIOLATIONS
    fi
    echo -e "${GREEN}✅ 前端检查完成${NC}"
else
    FRONTEND_STATUS="⚠️ 发现违规"
    echo -e "${YELLOW}⚠️  前端发现复杂度问题${NC}"
fi

# 创建最新副本
cp "$FRONTEND_JSON" "$FRONTEND_JSON_LATEST" 2>/dev/null || true

echo -e "   违规数量: ${FRONTEND_VIOLATIONS}"
echo -e "   报告文件: ${FRONTEND_JSON}"

# ==============================================================================
# 3. 后端复杂度检查 (ArchUnit)
# ==============================================================================
echo ""
echo -e "${YELLOW}🔍 检查后端复杂度 (ArchUnit)...${NC}"

BACKEND_STATUS="✅ 通过"
BACKEND_VIOLATIONS=0
BACKEND_SUMMARY=""

cd nexusarchive-java

# 捕获 ArchUnit 测试输出
if mvn test -Dtest=ComplexityRulesTest -q 2>&1 | tee "../${BACKEND_OUTPUT}" | grep -q "BUILD SUCCESS"; then
    echo -e "${GREEN}✅ 后端检查完成${NC}"
    BACKEND_STATUS="✅ 通过"
else
    # 检查是否是复杂度违规导致失败
    if grep -q "should not exceed" "../${BACKEND_OUTPUT}" 2>/dev/null; then
        BACKEND_STATUS="⚠️ 发现违规"
        echo -e "${YELLOW}⚠️  后端发现复杂度问题${NC}"
    else
        BACKEND_STATUS="⚠️ 测试失败"
        echo -e "${YELLOW}⚠️  后端测试执行失败${NC}"
    fi
fi

# 解析 ArchUnit 输出
BACKEND_VIOLATIONS=$(grep -o "Violations found: [0-9]*" "../${BACKEND_OUTPUT}" 2>/dev/null | grep -o "[0-9]*" || echo "0")
BACKEND_TOTAL=$(grep -o "Total classes analyzed: [0-9]*" "../${BACKEND_OUTPUT}" 2>/dev/null | grep -o "[0-9]*" || echo "0")

# 提取违规详情
BACKEND_VIOLATION_DETAILS=$(sed -n '/Found.*complexity violation/,/Summary:/p' "../${BACKEND_OUTPUT}" 2>/dev/null || echo "")

cd ..

echo -e "   分析类数: ${BACKEND_TOTAL}"
echo -e "   违规数量: ${BACKEND_VIOLATIONS}"
echo -e "   原始输出: ${BACKEND_OUTPUT}"

# ==============================================================================
# 4. 生成综合 Markdown 报告
# ==============================================================================
echo ""
echo -e "${YELLOW}📝 生成综合报告...${NC}"

# 获取文件修改时间（跨平台兼容）
FILE_TIME=$(get_file_timestamp "$FRONTEND_JSON")

cat > "$MARKDOWN_REPORT" << EOF
# NexusArchive 复杂度报告

**生成时间**: \`${FILE_TIME}\`
**报告版本**: \`${TIMESTAMP}\`

---

## 📊 执行摘要

| 层级 | 状态 | 违规数 | 详情 |
|------|------|--------|------|
| **前端 (React/TypeScript)** | ${FRONTEND_STATUS} | ${FRONTEND_VIOLATIONS} | [查看详细 JSON](${FRONTEND_JSON}) |
| **后端 (Java/Spring)** | ${BACKEND_STATUS} | ${BACKEND_VIOLATIONS} | [查看 ArchUnit 输出](${BACKEND_OUTPUT}) |

---

## 🔍 前端复杂度分析 (ESLint)

### 规则配置

| 规则 | 阈值 | 说明 |
|------|------|------|
| \`max-lines\` | 300 (普通) / 600 (页面) | 单文件最大行数 |
| \`max-lines-per-function\` | 50 | 单函数最大行数 |
| \`max-depth\` | 4 | 最大嵌套深度 |
| \`max-params\` | 10 | 最大参数数量 |
| \`complexity\` | 10 | 圈复杂度 |
| \`max-nested-callbacks\` | 4 | 最大嵌套回调数 |

### 违规详情

EOF

# 添加前端违规详情
if [ "$FRONTEND_VIOLATIONS" -gt 0 ]; then
    node -e "
        try {
            const fs = require('fs');
            const data = JSON.parse(fs.readFileSync('${FRONTEND_JSON}', 'utf8'));
            const violations = {};

            for (const file of data) {
                if (file.messages && file.messages.length > 0) {
                    const filePath = file.filePath.replace(process.cwd(), '');
                    violations[filePath] = file.messages;
                }
            }

            if (Object.keys(violations).length > 0) {
                console.log('| 文件 | 规则 | 消息 | 行号 |');
                console.log('|------|------|------|------|');
                for (const [filePath, messages] of Object.entries(violations)) {
                    for (const msg of messages.slice(0, 10)) { // 最多显示 10 条
                        console.log(\`|\${filePath}|\${msg.ruleId}|\${msg.message}|\${msg.line}|\`);
                    }
                    if (messages.length > 10) {
                        console.log(\`|...|\${messages.length - 10} 更多违规|||\`);
                    }
                }
            } else {
                console.log('> 无违规记录 ✅');
            }
        } catch (e) {
            console.error('Error parsing violations:', e.message);
            process.exit(1);
        }
    " 2>/dev/null || echo "> 解析失败，请查看 JSON 文件" >> "$MARKDOWN_REPORT"
else
    echo "> 无违规记录 ✅" >> "$MARKDOWN_REPORT"
fi

cat >> "$MARKDOWN_REPORT" << EOF

---

## ☕ 后端复杂度分析 (ArchUnit)

### 规则配置

| 规则 | 阈值 | 说明 |
|------|------|------|
| Service 类行数 | 500 | Service 类最大行数 |
| Controller 类行数 | 600 | Controller 类最大行数 |
| Entity 类行数 | 400 | Entity 类最大行数 |
| 方法行数 | 50 | 单个方法最大行数 |

### 分析统计

- **总类数**: ${BACKEND_TOTAL}
- **违规数**: ${BACKEND_VIOLATIONS}

### 违规详情

EOF

# 添加后端违规详情
if [ -n "$BACKEND_VIOLATION_DETAILS" ]; then
    echo "\`\`\`" >> "$MARKDOWN_REPORT"
    echo "$BACKEND_VIOLATION_DETAILS" >> "$MARKDOWN_REPORT"
    echo "\`\`\`" >> "$MARKDOWN_REPORT"
else
    if [ "$BACKEND_VIOLATIONS" -eq 0 ]; then
        echo "> 无违规记录 ✅" >> "$MARKDOWN_REPORT"
    else
        echo "> 请查看 [ArchUnit 原始输出](${BACKEND_OUTPUT})" >> "$MARKDOWN_REPORT"
    fi
fi

cat >> "$MARKDOWN_REPORT" << EOF

---

## 📈 历史对比

最近 5 次报告:
EOF

# 列出最近的报告
ls -t ${REPORT_DIR}/complexity-report-*.md 2>/dev/null | head -5 | while read report; do
    report_name=$(basename "$report")
    report_date=$(echo "$report_name" | sed 's/complexity-report-\([0-9]*\)-\([0-9]*\).md/\1 \2/' | sed 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/')
    echo "- [\`${report_name}\`](${REPORT_DIR}/${report_name})" >> "$MARKDOWN_REPORT"
done

cat >> "$MARKDOWN_REPORT" << EOF

---

## 🛠️ 改进建议

### 高复杂度文件处理
1. **拆分大文件**: 将超过行数限制的文件拆分为多个模块
2. **提取方法**: 将长方法拆分为更小的、职责单一的方法
3. **减少嵌套**: 使用早返回 (early return) 减少嵌套深度
4. **参数对象**: 使用参数对象替代过多的函数参数

### 架构层面
1. **模块化**: 确保每个模块职责单一
2. **依赖倒置**: 高层模块不应依赖低层模块
3. **边界清晰**: 严格遵循模块边界规则

---

*此报告由 \`scripts/complexity-report.sh\` 自动生成*
EOF

# 创建最新副本
cp "$MARKDOWN_REPORT" "$MARKDOWN_REPORT_LATEST"

echo -e "${GREEN}✅ 综合报告生成完成${NC}"
echo -e "   报告文件: ${MARKDOWN_REPORT}"

# ==============================================================================
# 5. 输出汇总
# ==============================================================================
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📊 复杂度报告生成完成${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${CYAN}报告文件:${NC}"
echo -e "  📄 综合报告: ${GREEN}${MARKDOWN_REPORT}${NC}"
echo -e "  📄 最新副本: ${GREEN}${MARKDOWN_REPORT_LATEST}${NC}"
echo -e "  📄 前端 JSON: ${GREEN}${FRONTEND_JSON}${NC}"
echo -e "  📄 后端输出: ${GREEN}${BACKEND_OUTPUT}${NC}"
echo ""
echo -e "${CYAN}汇总:${NC}"
echo -e "  前端违规: ${YELLOW}${FRONTEND_VIOLATIONS}${NC}"
echo -e "  后端违规: ${YELLOW}${BACKEND_VIOLATIONS}${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
