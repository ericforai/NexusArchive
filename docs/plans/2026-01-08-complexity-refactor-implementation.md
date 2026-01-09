# 代码复杂度控制框架实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 建立全库代码复杂度自动检测与强制控制机制，支持前后端

**架构:**
- 本地层: ESLint/ArchUnit规则 + Git pre-commit钩子
- CI层: GitHub Actions工作流，PR合并门禁
- 报告层: JSON/Markdown报告生成器

**技术栈:** ESLint, ArchUnit, PMD, Husky, GitHub Actions

---

## 任务分组

- **阶段一**: 基础设施搭建（规则配置、脚本、钩子、CI）
- **阶段二**: Top 10 文件修复
- **阶段三**: 文档与验证

---

# 阶段一：基础设施搭建

## Task 1: 创建前端复杂度 ESLint 规则

**Files:**
- Create: `.eslintrc.complexity.json`
- Modify: `package.json` (添加 scripts)

**Step 1: 创建 ESLint 复杂度配置文件**

创建 `.eslintrc.complexity.json`:

```json
{
  "overrides": [
    {
      "files": ["src/**/*.tsx", "src/**/*.ts"],
      "rules": {
        "max-lines": ["error", {
          "max": 300,
          "skipBlankLines": true,
          "skipComments": true
        }],
        "max-lines-per-function": ["error", {
          "max": 50,
          "skipBlankLines": true,
          "skipComments": true
        }],
        "max-depth": ["error", 4],
        "max-params": ["error", 10],
        "complexity": ["error", 10],
        "max-nested-callbacks": ["error", 4]
      }
    },
    {
      "files": ["src/pages/**/*.tsx"],
      "rules": {
        "max-lines": ["warn", {
          "max": 600,
          "skipBlankLines": true,
          "skipComments": true
        }]
      }
    }
  ]
}
```

**Step 2: 添加 NPM 脚本**

修改 `package.json`，在 `scripts` 中添加:

```json
{
  "scripts": {
    "complexity:check": "eslint --config .eslintrc.complexity.json 'src/**/*.{ts,tsx}'",
    "complexity:report": "eslint --config .eslintrc.complexity.json --format json 'src/**/*.{ts,tsx}' > reports/complexity-frontend.json 2>&1 || true"
  }
}
```

**Step 3: 创建报告目录**

运行: `mkdir -p reports`

**Step 4: 验证配置**

运行: `npm run complexity:check`

预期: 看到违规报告（当前有超标文件）

**Step 5: 提交**

```bash
git add .eslintrc.complexity.json package.json reports/
git commit -m "feat: add ESLint complexity rules for frontend"
```

---

## Task 2: 创建后端 ArchUnit 复杂度规则

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/architecture/ComplexityRulesTest.java`

**Step 1: 创建 ArchUnit 测试类**

创建 `nexusarchive-java/src/test/java/com/nexusarchive/architecture/ComplexityRulesTest.java`:

```java
package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("architecture")
@DisplayName("代码复杂度架构测试")
class ComplexityRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.nexusarchive");

    @Test
    @DisplayName("Service类行数不应超过500行")
    void serviceClassesShouldNotExceed500Lines() {
        ArchRule rule = classes()
            .that().resideInAPackage("..service..")
            .and().areNotEnums()
            .should(new LineCountCondition(500));

        rule.check(classes);
    }

    @Test
    @DisplayName("Controller类行数不应超过600行")
    void controllerClassesShouldNotExceed600Lines() {
        ArchRule rule = classes()
            .that().resideInAPackage("..controller..")
            .should(new LineCountCondition(600));

        rule.check(classes);
    }

    @Test
    @DisplayName("Entity类行数不应超过400行")
    void entityClassesShouldNotExceed400Lines() {
        ArchRule rule = classes()
            .that().resideInAPackage("..entity..")
            .should(new LineCountCondition(400));

        rule.check(classes);
    }

    @Test
    @DisplayName("方法行数不应超过50行")
    void methodsShouldNotExceed50Lines() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..service..")
            .or().resideInAPackage("..controller..")
            .should().containMethodsWithLineCountGreaterThan(50);

        rule.check(classes);
    }

    @Test
    @DisplayName("生成复杂度报告")
    void generateComplexityReport() {
        StringBuilder report = new StringBuilder();
        report.append("# 后端代码复杂度报告\n\n");
        report.append("| 文件 | 行数 | 类型 | 状态 |\n");
        report.append("|------|------|------|------|\n");

        for (JavaClass clazz : classes) {
            int lineCount = clazz.getSource().getCodeStatistics().getNumberOfLines();
            String status = lineCount > 500 ? "❌ 超标" : "✅ 正常";

            if (lineCount > 300) {  // 只显示较大文件
                report.append(String.format("| %s | %d | %s | %s |\n",
                    clazz.getSimpleName(),
                    lineCount,
                    clazz.getSimpleTypeName(),
                    status
                ));
            }
        }

        System.out.println(report.toString());
    }
}
```

**Step 2: 创建行数条件类**

创建 `nexusarchive-java/src/test/java/com/nexusarchive/architecture/LineCountCondition.java`:

```java
package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import java.util.ArrayList;
import java.util.List;

public class LineCountCondition extends ArchCondition<JavaClass> {

    private final int maxLines;
    private final List<String> violations = new ArrayList<>();

    public LineCountCondition(int maxLines) {
        super("行数不超过 " + maxLines);
        this.maxLines = maxLines;
    }

    @Override
    public void finish(ConditionEvents events) {
        for (String violation : violations) {
            events.addMessage(violation);
        }
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        int lineCount = item.getSource().getCodeStatistics().getNumberOfLines();

        if (lineCount > maxLines) {
            String message = String.format(
                "❌ %s: %d行 (超过限制%d行, 超出%d行)",
                item.getFullName(),
                lineCount,
                maxLines,
                lineCount - maxLines
            );
            violations.add(message);
            events.violate(item, message);
        } else {
            events.addMessage(String.format(
                "✅ %s: %d行 (正常)",
                item.getFullName(),
                lineCount
            ));
        }
    }
}
```

**Step 3: 运行测试验证**

运行: `cd nexusarchive-java && mvn test -Dtest=ComplexityRulesTest`

预期: 看到违规报告

**Step 4: 提交**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/architecture/
git commit -m "feat: add ArchUnit complexity rules for backend"
```

---

## Task 3: 创建复杂度报告生成脚本

**Files:**
- Create: `scripts/complexity-report.sh`
- Modify: `package.json` (添加脚本引用)

**Step 1: 创建报告脚本**

创建 `scripts/complexity-report.sh`:

```bash
#!/bin/bash
# 代码复杂度报告生成脚本
# Usage: npm run complexity:report

set -e

REPORT_DIR="reports"
TIMESTAMP=$(date +%Y-%m-%d)
FRONTEND_REPORT="$REPORT_DIR/complexity-frontend-$TIMESTAMP.json"
BACKEND_REPORT="$REPORT_DIR/complexity-backend-$TIMESTAMP.txt"
SUMMARY_REPORT="$REPORT_DIR/complexity-summary-$TIMESTAMP.md"

mkdir -p "$REPORT_DIR"

echo "🔍 正在分析前端代码..."
npm run complexity:report -- --format json "$FRONTEND_REPORT" 2>/dev/null || true

echo "🔍 正在分析后端代码..."
cd nexusarchive-java
mvn test -Dtest=ComplexityRulesTest#generateComplexityReport > "../$BACKEND_REPORT" 2>&1 || true
cd ..

echo "📊 生成综合报告..."
cat > "$SUMMARY_REPORT" << EOF
# 代码复杂度报告

生成时间: $TIMESTAMP

## 前端超标文件

EOF

# 解析前端报告
if [ -f "$FRONTEND_REPORT" ]; then
  echo "\`\`\`json" >> "$SUMMARY_REPORT"
  cat "$FRONTEND_REPORT" >> "$SUMMARY_REPORT"
  echo "\`\`\`" >> "$SUMMARY_REPORT"
fi

echo "" >> "$SUMMARY_REPORT"
echo "## 后端超标文件" >> "$SUMMARY_REPORT"
echo "" >> "$SUMMARY_REPORT"

if [ -f "$BACKEND_REPORT" ]; then
  grep "❌" "$BACKEND_REPORT" || echo "无超标文件" >> "$SUMMARY_REPORT"
fi

echo "✅ 报告已生成: $SUMMARY_REPORT"
cat "$SUMMARY_REPORT"
```

**Step 2: 添加执行权限**

运行: `chmod +x scripts/complexity-report.sh`

**Step 3: 添加 NPM 脚本**

修改 `package.json`:

```json
{
  "scripts": {
    "complexity:report": "bash scripts/complexity-report.sh"
  }
}
```

**Step 4: 验证脚本**

运行: `npm run complexity:report`

预期: 在 `reports/` 目录生成报告文件

**Step 5: 提交**

```bash
git add scripts/complexity-report.sh package.json
git commit -m "feat: add complexity report generation script"
```

---

## Task 4: 创建 Git Pre-commit 钩子

**Files:**
- Create: `.husky/pre-commit-complexity`
- Modify: `package.json` (配置 husky)

**Step 1: 安装 husky（如果未安装）**

运行: `npm install -D husky`

**Step 2: 初始化 husky**

运行: `npx husky install`

**Step 3: 创建 pre-commit 复杂度检查钩子**

创建 `.husky/pre-commit-complexity`:

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# 复杂度检查
echo "🔍 检查代码复杂度..."

# 检查前端
if git diff --cached --name-only | grep -E '\.(ts|tsx)$' | grep -q '^src/'; then
  echo "检查前端复杂度..."
  npm run complexity:check -- --quiet || true
fi

# 检查后端
if git diff --cached --name-only | grep -E '\.java$' | grep -q '^nexusarchive-java/'; then
  echo "检查后端复杂度..."
  cd nexusarchive-java
  mvn test -Dgroups=architecture -q || true
  cd ..
fi

echo "✅ 复杂度检查完成"
```

**Step 4: 添加执行权限**

运行: `chmod +x .husky/pre-commit-complexity`

**Step 5: 配置 package.json 启用钩子**

修改 `package.json`:

```json
{
  "scripts": {
    "prepare": "husky install",
    "postinstall": "husky install"
  }
}
```

**Step 6: 测试钩子**

运行: `npx husky add .husky/pre-commit "npx lint-staged"`

**Step 7: 提交**

```bash
git add .husky/ package.json package-lock.json
git commit -m "feat: add pre-commit complexity check"
```

---

## Task 5: 创建 CI 检查工作流

**Files:**
- Create: `.github/workflows/complexity-check.yml`

**Step 1: 创建 GitHub Actions 工作流**

创建 `.github/workflows/complexity-check.yml`:

```yaml
name: Complexity Check

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]

jobs:
  frontend-complexity:
    name: Frontend Complexity
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm ci

      - name: Run complexity check
        run: npm run complexity:check
        continue-on-error: true

      - name: Generate report
        if: always()
        run: npm run complexity:report

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: complexity-report
          path: reports/complexity-*.json
          retention-days: 30

      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const reportPath = 'reports/complexity-frontend-*.json';
            // 读取并格式化报告
            // 发送到PR评论

  backend-complexity:
    name: Backend Complexity
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run ArchUnit tests
        run: |
          cd nexusarchive-java
          mvn test -Dtest=ComplexityRulesTest

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-complexity-report
          path: nexusarchive-java/target/archunit-reports/
```

**Step 2: 提交**

```bash
git add .github/workflows/complexity-check.yml
git commit -m "feat: add CI complexity check workflow"
```

---

# 阶段二：Top 10 文件修复

## Task 6: 修复 ProductWebsite.tsx (1041行)

**Files:**
- Modify: `src/pages/ProductWebsite.tsx`
- Create: `src/pages/product-website/components/`
- Create: `src/pages/product-website/data/sections.ts`

**Step 1: 创建组件目录结构**

运行: `mkdir -p src/pages/product-website/components src/pages/product-website/data`

**Step 2: 提取静态数据到配置文件**

创建 `src/pages/product-website/data/sections.ts`:

```typescript
export const HERO_SECTION = {
  badge: {
    icon: 'Shield',
    text: '符合 DA/T 94-2022 国家标准',
  },
  title: '让每一张凭证都成为\n合法的数字资产',
  subtitle: '告别纸质档案库房，开启单套制归档新时代。\n专为大型企业打造，从 ERP 到档案库，实现全链路自动化、无纸化、合规化。',
  cta: [
    { text: '立即体验', primary: true, link: '/system' },
    { text: '预约专家顾问', primary: false },
  ],
};
```

**Step 3: 提取 HeroSection 组件**

创建 `src/pages/product-website/components/HeroSection.tsx`:

```typescript
import React from 'react';
import { ChevronRight } from 'lucide-react';
import { HERO_SECTION } from '../data/sections';

export const HeroSection: React.FC = () => {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-20">
      {/* ... 背景和动效 ... */}
      <div className="relative z-10 max-w-5xl mx-auto px-6 text-center">
        <h1>{HERO_SECTION.title}</h1>
        <p>{HERO_SECTION.subtitle}</p>
        <div className="flex gap-6">
          {HERO_SECTION.cta.map((btn, i) => (
            <button key={i} className={...}>{btn.text}</button>
          ))}
        </div>
      </div>
    </section>
  );
};
```

**Step 4: 重构主组件**

修改 `src/pages/ProductWebsite.tsx`:

```typescript
import React from 'react';
import { HeroSection } from './product-website/components/HeroSection';
import { PainPointsSection } from './product-website/components/PainPointsSection';
// ... 其他section组件

export const ProductWebsite: React.FC = () => {
  return (
    <div className="min-h-screen bg-[#0B1120]">
      <Navigation />
      <HeroSection />
      <PainPointsSection />
      {/* ... 其他sections */}
    </div>
  );
};
```

**Step 5: 验证重构**

运行: `npm run complexity:check`

预期: ProductWebsite.tsx 行数 < 300

**Step 6: 提交**

```bash
git add src/pages/ProductWebsite.tsx src/pages/product-website/
git commit -m "refactor: split ProductWebsite into smaller components"
```

---

## Task 7: 修复 YonSuiteErpAdapter.java (728行)

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/`

**Step 1: 创建客户端目录**

运行: `mkdir -p nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client`

**Step 2: 提取认证客户端**

创建 `YonSuiteAuthClient.java`:

```java
package com.nexusarchive.integration.erp.adapter.client;

import org.springframework.stereotype.Component;

@Component
public class YonSuiteAuthClient {

    public String login(String username, String password) {
        // 登录逻辑
    }

    public void logout(String token) {
        // 登出逻辑
    }

    public boolean validateToken(String token) {
        // Token验证
    }
}
```

**Step 3: 提取凭证客户端**

创建 `YonSuiteVoucherClient.java`:

```java
package com.nexusarchive.integration.erp.adapter.client;

@Component
public class YonSuiteVoucherClient {

    public VoucherResponse getVoucher(String voucherId) {
        // 获取凭证
    }

    public List<VoucherResponse> listVouchers(VoucherQuery query) {
        // 列表查询
    }
}
```

**Step 4: 重构主适配器为门面**

修改 `YonSuiteErpAdapter.java`:

```java
@Service
public class YonSuiteErpAdapter implements ErpAdapter {

    private final YonSuiteAuthClient authClient;
    private final YonSuiteVoucherClient voucherClient;
    private final YonSuitePeriodClient periodClient;

    // 仅作为门面，委托给具体客户端
}
```

**Step 5: 验证重构**

运行: `cd nexusarchive-java && mvn test -Dtest=YonSuiteErpAdapterTest`

**Step 6: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/
git commit -m "refactor: split YonSuiteErpAdapter into specialized clients"
```

---

## Task 8: 修复 ArchiveBatchView.tsx (927行, 29 hooks)

**Files:**
- Modify: `src/pages/operations/ArchiveBatchView.tsx`
- Create: `src/pages/operations/archive-batch/hooks/`
- Create: `src/pages/operations/archive-batch/components/`

**Step 1: 创建目录结构**

运行: `mkdir -p src/pages/operations/archive-batch/hooks src/pages/operations/archive-batch/components`

**Step 2: 提取数据获取 hook**

创建 `useArchiveBatchData.ts`:

```typescript
import { useState, useCallback } from 'react';
import { archiveBatchApi } from '@/api/archiveBatch';

export const useArchiveBatchData = () => {
  const [loading, setLoading] = useState(false);
  const [batches, setBatches] = useState<ArchiveBatch[]>([]);
  const [total, setTotal] = useState(0);

  const fetchBatches = useCallback(async (params: BatchQueryParams) => {
    setLoading(true);
    try {
      const res = await archiveBatchApi.list(params);
      setBatches(res.data);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  }, []);

  return { loading, batches, total, fetchBatches };
};
```

**Step 3: 提取弹窗状态管理 hook**

创建 `useBatchModals.ts`:

```typescript
import { useState } from 'react';

export const useBatchModals = () => {
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedBatch, setSelectedBatch] = useState<ArchiveBatch | null>(null);

  const openCreateModal = () => setCreateModalVisible(true);
  const closeCreateModal = () => setCreateModalVisible(false);

  const openDetailModal = (batch: ArchiveBatch) => {
    setSelectedBatch(batch);
    setDetailModalVisible(true);
  };
  const closeDetailModal = () => {
    setSelectedBatch(null);
    setDetailModalVisible(false);
  };

  return {
    createModalVisible,
    detailModalVisible,
    selectedBatch,
    openCreateModal,
    closeCreateModal,
    openDetailModal,
    closeDetailModal,
  };
};
```

**Step 4: 重构主组件**

修改 `ArchiveBatchView.tsx`:

```typescript
const ArchiveBatchView: React.FC = () => {
  const { loading, batches, total, fetchBatches } = useArchiveBatchData();
  const {
    createModalVisible,
    detailModalVisible,
    selectedBatch,
    openCreateModal,
    closeCreateModal,
    openDetailModal,
    closeDetailModal,
  } = useBatchModals();

  // 组件逻辑简化，聚焦UI渲染
};
```

**Step 5: 验证重构**

运行: `npm run complexity:check`

预期: hooks数量 < 10

**Step 6: 提交**

```bash
git add src/pages/operations/ArchiveBatchView.tsx src/pages/operations/archive-batch/
git commit -m "refactor: extract hooks from ArchiveBatchView"
```

---

# 阶段三：文档与验证

## Task 9: 创建规则文档

**Files:**
- Create: `docs/development/complexity-rules.md`

**Step 1: 创建规则说明文档**

创建 `docs/development/complexity-rules.md`:

```markdown
# 代码复杂度规则

## 前端规则

| 规则 | 阈值 | 说明 |
|------|------|------|
| max-lines | 300 | 组件行数 |
| max-lines-per-function | 50 | 函数行数 |
| max-depth | 4 | 嵌套层级 |
| max-params | 10 | 参数数量 |

## 后端规则

| 规则 | 阈值 | 说明 |
|------|------|------|
| 类行数 | 500 | Service类 |
| 方法行数 | 50 | 所有方法 |

## 检查命令

\`\`\`bash
# 前端
npm run complexity:check

# 后端
cd nexusarchive-java && mvn test -Dgroups=architecture

# 完整报告
npm run complexity:report
\`\`\`
```

**Step 2: 提交**

```bash
git add docs/development/complexity-rules.md
git commit -m "docs: add complexity rules documentation"
```

---

## Task 10: 最终验证

**Files:**
- None (验证任务)

**Step 1: 运行完整检查**

运行: `npm run complexity:check`

**Step 2: 生成最终报告**

运行: `npm run complexity:report`

**Step 3: 验证 CI 配置**

检查: `.github/workflows/complexity-check.yml` 语法正确

**Step 4: 创建里程碑**

运行: `git tag -a v1.0-complexity-framework -m "代码复杂度控制框架完成"`

**Step 5: 推送变更**

运行: `git push && git push --tags`

---

## 附录：命令速查

| 命令 | 说明 |
|------|------|
| `npm run complexity:check` | 检查前端复杂度 |
| `npm run complexity:report` | 生成完整报告 |
| `mvn test -Dgroups=architecture` | 检查后端架构规则 |
| `npx husky add .husky/pre-commit` | 添加pre-commit钩子 |
