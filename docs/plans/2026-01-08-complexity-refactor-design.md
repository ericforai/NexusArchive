# NexusArchive 极简重构框架设计

**日期**: 2026-01-08
**目标**: 建立全库通用的代码复杂度控制与重构机制

---

## 一、设计原则

- **删减优先**: 删除重复代码、未使用抽象、过度设计
- **强制规则化**: 明确阈值，自动检测，构建失败即修复
- **最大优先**: 按文件行数/复杂度降序处理
- **渐进式**: 基础设施 → Top 10 → 全库应用

---

## 二、核心规则定义

### 2.1 前端规则（TypeScript/React）

| 指标 | 阈值 | 检测工具 |
|------|------|----------|
| 组件行数 | ≤300 | ESLint |
| Hooks数量 | ≤10 | 自定义脚本 |
| 函数行数 | ≤50 | ESLint |
| 嵌套层级 | ≤4 | ESLint |
| Props数量 | ≤10 | TypeScript |

### 2.2 后端规则（Java/Spring）

| 指标 | 阈值 | 检测工具 |
|------|------|----------|
| 类行数 | ≤500 | ArchUnit |
| 方法行数 | ≤50 | PMD |
| 参数数量 | ≤6 | PMD |
| 依赖注入数 | ≤10 | ArchUnit |
| 圈复杂度 | ≤10 | Checkstyle |

### 2.3 违反后果

- ❌ CI 构建失败
- ⚠️ PR 无法合并
- 📄 生成自动修复建议

---

## 三、自动化检测机制

### 3.1 第一层：本地开发（即时反馈）

- IDE 实时标红
- 保存时自动检查
- Git pre-commit 拦截

### 3.2 第二层：CI/CD（强制门禁）

- PR 自动运行检查
- 输出详细报告
- 失败显示修复命令

### 3.3 第三层：周报监控（趋势分析）

- 每周生成趋势报告
- 识别恶化文件
- 通知责任人

### 3.4 检测工具栈

- 前端: ESLint + complexity-plugin + vitest
- 后端: ArchUnit + PMD + Checkstyle
- 报告: JSON + Markdown 生成器

---

## 四、自动修复模板

### 4.1 模板A：超大组件拆分（前端）

```
原文件: ArchiveBatchView.tsx (927行, 29 hooks)
↓
├── ArchiveBatchView.tsx          # 主组件 (~150行)
├── hooks/
│   ├── useArchiveBatchData.ts    # 数据获取
│   ├── useBatchModals.ts         # 弹窗状态
│   └── useBatchSelection.ts      # 选择逻辑
├── components/
│   ├── BatchTable.tsx
│   ├── BatchStats.tsx
│   └── BatchDetailDrawer.tsx
└── columns.config.ts             # 列配置
```

### 4.2 模板B：超大Service拆分（后端）

```
原文件: YonSuiteErpAdapter.java (728行)
↓
├── YonSuiteErpAdapter.java       # 门面 (~150行)
├── client/
│   ├── YonSuiteAuthClient.java
│   ├── YonSuiteVoucherClient.java
│   └── YonSuitePeriodClient.java
├── parser/YonSuiteResponseParser.java
└── handler/YonSuiteErrorHandler.java
```

### 4.3 模板C：Hooks提取模式

自动识别相关状态归类，提取为自定义hook。

---

## 五、实施路线图

### 阶段一：基础设施搭建（1周）

- [x] 配置检测规则
- [x] 编写 pre-commit 钩子
- [x] 创建 CI 检查任务
- [x] 生成基线报告

### 阶段二：Top 10 修复（2周）

| 排名 | 文件 | 行数 | 修复方案 |
|------|------|------|----------|
| 1 | ProductWebsite.tsx | 1041 | 提取section组件 |
| 2 | YonSuiteErpAdapter.java | 728 | 拆分5个client |
| 3 | ArchiveBatchView.tsx | 927 | 提取3个hooks |
| 4 | OriginalVoucherService.java | 659 | 拆分3个service |
| 5 | LegacyImportPage.tsx | 822 | 提取导入流程组件 |
| 6 | ArcFileContent.java | 635 | Entity瘦身 |
| 7 | OCRProcessingView.tsx | 739 | 提取OCR hooks |
| 8 | YonSuiteClient.java | 619 | 拆分API域 |
| 9 | PoolController.java | 617 | 按功能分组 |
| 10 | BatchUploadView.tsx | 667 | 提取上传hooks |

### 阶段三：全库应用（持续）

- 新代码默认应用规则
- 存量代码逐步重构
- 每周审查趋势

---

## 六、产出物清单

### 配置文件

- `.eslintrc.complexity.json`
- `archunit/ComplexityRules.java`
- `.husky/pre-commit-complexity.sh`
- `.github/workflows/complexity-check.yml`

### 脚本工具

- `scripts/complexity-report.sh`
- `scripts/refactor-scaffold.ts`
- `scripts/complexity-trend.sh`

### 文档

- `docs/development/complexity-rules.md`
- `docs/development/refactor-templates.md`

---

## 七、当前基线数据

### 前端超标文件（>600行）

| 文件 | 行数 | Hooks |
|------|------|-------|
| ProductWebsite.tsx | 1041 | - |
| ArchiveBatchView.tsx | 927 | 29 |
| LegacyImportPage.tsx | 822 | - |
| OCRProcessingView.tsx | 739 | 19 |
| BatchUploadView.tsx | 667 | 17 |
| ArchiveListView.tsx | 667 | - |
| FondsHistoryPage.tsx | 621 | - |
| ArchiveApprovalView.tsx | 607 | 19 |

### 后端超标文件（>600行）

| 文件 | 行数 |
|------|------|
| YonSuiteErpAdapter.java | 728 |
| OriginalVoucherService.java | 659 |
| ArcFileContent.java | 635 |
| YonSuiteClient.java | 619 |
| PoolController.java | 617 |
| CollectionBatchServiceImpl.java | 614 |
| IngestServiceImpl.java | 569 |
