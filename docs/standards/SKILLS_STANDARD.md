# AI Skills Standard (AISS) v1.0

> **跨平台 AI 技能定义标准** - 让 Claude Code、GitHub Copilot、Google Gemini 等不同 AI Coding 工具能够共享和使用同一套技能定义。

## 目录

- [1. 概述](#1-概述)
- [2. 元数据格式](#2-元数据格式)
- [3. 文件结构](#3-文件结构)
- [4. 内容章节规范](#4-内容章节规范)
- [5. 写作规范](#5-写作规范)
- [6. 触发机制](#6-触发机制)
- [7. 平台兼容性](#7-平台兼容性)
- [8. 验证规则](#8-验证规则)
- [9. 附录](#9-附录)

---

## 1. 概述

### 1.1 设计目标

**AI Skills Standard (AISS)** 是一个跨平台的 AI 技能定义标准，旨在解决以下问题：

| 问题 | 解决方案 |
|------|----------|
| 各平台 Skill 格式不互通 | 统一的元数据和内容规范 |
| 无法在不同 AI 工具间复用 | 平台无关的文件结构 |
| 缺乏版本管理 | 标准化的版本字段 |
| 上下文膨胀 | 渐进式加载机制 |

### 1.2 核心原则

| 原则 | 说明 |
|------|------|
| **工具无关** | 标准独立于任何特定 AI 平台 |
| **人机可读** | Markdown 格式，开发者可直接阅读编辑 |
| **向后兼容** | 基于 Claude Skills 格式扩展，保持兼容 |
| **渐进增强** | 核心字段必需，扩展字段可选 |
| **YAGNI** | 只包含必要信息，不创建冗余文档 |

### 1.3 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2025-01-16 | 初始版本，基于 Claude Skills 格式 |

---

## 2. 元数据格式

### 2.1 SKILL.md Frontmatter（必需）

每个 Skill 必须以 **YAML Frontmatter** 开头，定义核心元数据：

```yaml
---
name: skill-name
description: |
  [技能描述]. Use when [触发场景].
  中文关键词, 关键词, 关键词
license: MIT
---
```

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | Skill 唯一标识（kebab-case） |
| `description` | string | ✅ | 技能描述 + 触发场景 + 关键词 |
| `license` | string | ⚪ | 许可证类型 |

**⚠️ 重要**：Claude 只读取 `name` 和 `description` 来决定何时使用 Skill。不要在 frontmatter 中添加其他字段。

### 2.2 description 字段规范

`description` 是 Skill 的**主要触发机制**，必须清晰描述：

```yaml
description: |
  Comprehensive [功能描述] with support for [特性].

  Use when:
  1. [场景一]
  2. [场景二]
  3. [场景三]

  中文关键词, English Keywords, 关键词
```

**示例**：
```yaml
description: |
  Use when designing modular architectures, refactoring spaghetti code, or reviewing module boundaries.
  Use when facing over-engineered systems, circular dependencies, or unclear separation of concerns.

  模块化设计, 重构, 代码审查, 架构熵增, 循环依赖, 职责分离, 过度工程, 依赖倒置, SOLID
```

### 2.3 skill-metadata.yml（可选，跨平台用）

扩展元数据文件，供其他平台使用：

```yaml
# skill-metadata.yml
version: "1.0.0"
author: "团队/作者"
skillFormat: "aiss-1.0"

tags: [category, subcategory]
platforms: [claude, copilot, gemini]

requires:
  - other-skill

depends:
  - tool: "dependency-cruiser"
    version: ">=14.0.0"

triggers:
  keywords:
    - "模块化"
    - "重构"
    - "架构"
  filePatterns:
    - "**/src/**/*.ts"
    - "**/components/**/*.tsx"
  excludePatterns:
    - "**/node_modules/**"
    - "**/dist/**"
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `version` | string | 语义化版本 |
| `author` | string | 维护者 |
| `skillFormat` | string | AISS 版本（如 "aiss-1.0"） |
| `tags` | array | 分类标签 |
| `platforms` | array | 兼容平台 |
| `requires` | array | 依赖的其他 Skills |
| `depends` | array | 外部依赖（工具、服务等） |
| `triggers` | object | 触发配置（见第6节） |

---

## 3. 文件结构

### 3.1 标准目录结构

每个 Skill 必须采用**文件夹结构**：

```
skill-name/
├── SKILL.md              # 必需：核心技能定义
├── skill-metadata.yml    # 可选：扩展元数据
│
├── scripts/              # 可选：可执行脚本
│   ├── script.py
│   └── script.sh
│
├── references/           # 可选：参考文档（按需加载）
│   ├── api.md
│   └── patterns.md
│
└── assets/               # 可选：输出资源（模板、图标等）
    ├── template.html
    └── logo.png
```

### 3.2 文件说明

| 文件/目录 | 必需 | 说明 | 何时使用 |
|-----------|------|------|----------|
| `SKILL.md` | ✅ | 技能核心定义，包含 frontmatter 和指令内容 | **所有 Skill** |
| `skill-metadata.yml` | ⚪ | 扩展元数据（版本、作者、平台兼容性） | 需要跨平台或版本管理时 |
| `scripts/` | ⚪ | 可执行代码，高确定性任务 | 重复执行的可靠操作 |
| `references/` | ⚪ | 领域文档、API、规范等参考材料 | 大于 10k 字的详细参考 |
| `assets/` | ⚪ | 输出用文件（模板、样例） | 生成输出时需要复制的文件 |

### 3.3 禁止的文件

**⚠️ 官方标准明确禁止创建以下冗余文档**：

```
❌ README.md
❌ INSTALLATION_GUIDE.md
❌ QUICK_REFERENCE.md
❌ CHANGELOG.md
❌ 任何关于 Skill 本身的文档
```

**原则**：Skill 只包含执行任务所需的信息，不包含"关于 Skill 的文档"。

### 3.4 渐进式加载原则

Skill 采用三级加载机制管理上下文：

| 级别 | 内容 | 大小限制 | 加载时机 |
|------|------|----------|----------|
| **1. 元数据** | name + description | ~100 词 | **始终加载** |
| **2. SKILL.md body** | 指令内容 | < 500 行 | Skill 触发时 |
| **3. Bundled Resources** | scripts/references/assets | 无限制 | 按需加载 |

---

## 4. 内容章节规范

### 4.1 推荐章节结构

SKILL.md body 应采用以下章节结构：

| 章节 | 必需 | 说明 |
|------|------|------|
| `# [Skill Name]` | ✅ | 标题，与 frontmatter `name` 一致 |
| `## Overview` | ✅ | 一句话概述核心原则 |
| `## When to Use` | ⚠️ | **仅用于人类阅读**，Claude 从 description 触发 |
| `## [核心工作流]` | ✅ | 主要步骤/指令 |
| `## Examples` | ⚪ | 代码示例（优先简短） |
| `## References` | ⚪ | 指向 bundled resources 的链接 |

### 4.2 SKILL.md 模板

```markdown
---
name: skill-name
description: |
  [技能描述]. Use when [场景].
  中文关键词
---

# Skill Name

## Overview

[一句话概述核心原则]

## When to Use

> 注意：此章节仅供人类阅读。Claude 从 frontmatter `description` 触发。

- [场景一]
- [场景二]
- [场景三]

## Core Workflow

1. [步骤一]
2. [步骤二]
3. [步骤三]

## Examples

\`\`\`language
// 简短示例
\`\`\`

## References

- **Advanced patterns**: [PATTERNS.md](references/PATTERNS.md)
- **API docs**: [API.md](references/API.md)
```

---

## 5. 写作规范

### 5.1 语言风格

| 规则 | 示例 |
|------|------|
| 使用祈使句/不定式 | ✅ "Extract text with pdfplumber"<br>❌ "You should extract text..." |
| 直接、简洁 | ✅ "Validate input before processing"<br>❌ "It is important to note that you should validate..." |
| 代码 > 文字 | ✅ 用代码示例说明<br>❌ 用长篇文字解释 |
| 示例 > 解释 | ✅ 用具体示例<br>❌ 用抽象描述 |

### 5.2 代码块规范

```markdown
\`\`\`python
# 好的做法：简短、可运行
import pdfplumber

def extract_text(path):
    with pdfplumber.open(path) as pdf:
        return page.extract_text()
\`\`\`
```

### 5.3 内容拆分原则

当 SKILL.md 超过 **500 行**时，必须拆分到 `references/`：

**拆分信号**：
- 详细文档 > 10k 字
- 多个变体的模式/示例
- 特定框架/选项的深度说明

**引用方式**：
```markdown
## Advanced

For **pattern X**: See [PATTERNS.md](references/PATTERNS.md)
For **framework Y**: See [FRAMEWORK-Y.md](references/framework-y.md)
```

### 5.4 Progressive Disclosure 模式

**模式 1：高层指导 + 引用**
```markdown
# PDF Processing

Extract text with pdfplumber:
\`\`\`python
import pdfplumber
\`\`\`

## Advanced
- **Form filling**: See [FORMS.md](references/FORMS.md)
- **API reference**: See [REFERENCE.md](references/REFERENCE.md)
```

**模式 2：按领域分片**
```
skill-name/
├── SKILL.md          # 概览 + 导航
└── references/
    ├── finance.md    # 财务领域
    ├── sales.md      # 销售领域
    └── marketing.md  # 市场领域
```

---

## 6. 触发机制

### 6.1 Claude Code

通过 `description` 字段自动触发：
- AI 读取所有 Skills 的 `name` + `description`
- 根据用户请求匹配触发

### 6.2 其他平台

通过 `skill-metadata.yml` 中的 `triggers` 配置：

```yaml
# skill-metadata.yml
triggers:
  keywords:
    - "模块化"
    - "重构"
    - "架构"

  filePatterns:
    - "**/src/**/*.ts"
    - "**/components/**/*.tsx"

  excludePatterns:
    - "**/node_modules/**"
    - "**/dist/**"

  # 可选：上下文触发
  contextRequired:
    - "git"
    - "typescript"
```

### 6.3 触发配置说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `keywords` | array | 触发关键词列表 |
| `filePatterns` | array | 匹配的文件模式（glob） |
| `excludePatterns` | array | 排除的文件模式（glob） |
| `contextRequired` | array | 需要的上下文（工具、环境） |

---

## 7. 平台兼容性

### 7.1 兼容性矩阵

| 平台 | SKILL.md | skill-metadata.yml | scripts/ | references/ | assets/ |
|------|----------|-------------------|----------|-------------|---------|
| Claude Code | ✅ 完全支持 | ⚠️ 部分支持 | ✅ | ✅ | ✅ |
| GitHub Copilot | ⚠️ 需适配 | ✅ | ⚠️ 待定 | ⚠️ 待定 | ⚠️ 待定 |
| Google Gemini | ⚠️ 需适配 | ✅ | ⚠️ 待定 | ⚠️ 待定 | ⚠️ 待定 |

### 7.2 向后兼容承诺

| 版本 | 兼容性 |
|------|--------|
| `aiss-1.0` | 兼容 Claude Skills 格式 |
| `aiss-1.x` | 向后兼容，不破坏现有 Skills |
| `aiss-2.0` | 可能不兼容，需要迁移 |

---

## 8. 验证规则

### 8.1 必需检查

有效的 Skill 必须通过以下检查：

| 检查项 | 规则 | 错误信息 |
|--------|------|----------|
| Frontmatter | 包含 `name` 和 `description` | "Missing required frontmatter fields" |
| 描述质量 | description 包含 "Use when" 或触发场景 | "Description must include trigger scenarios" |
| 文件组织 | 禁止 README.md 等冗余文件 | "Redundant documentation files found" |
| 引用完整性 | SKILL.md 中的链接指向存在的文件 | "Broken reference link" |
| 大小限制 | SKILL.md < 500 行 | "SKILL.md exceeds 500 lines, consider splitting" |

### 8.2 推荐检查

| 检查项 | 规则 | 警告信息 |
|--------|------|----------|
| 描述长度 | description < 500 词 | "Description is too long, consider summarizing" |
| 引用深度 | references 只有一层嵌套 | "Deep reference nesting detected" |
| 代码可执行 | scripts/ 中的脚本可运行 | "Script execution failed" |

---

## 9. 附录

### 9.1 示例 Skill

完整的 Skill 示例请参考：
- `/.claude/skills/entropy-reduction/SKILL.md`
- `/.claude/skills/architecture-defense/SKILL.md`
- `/.claude/skills/minimalist-refactorer/SKILL.md`

### 9.2 参考资料

- [Claude Skills 官方仓库](https://github.com/anthropics/skills)
- [Claude Skills 官方文档](https://github.com/anthropics/skills/blob/main/skills/skill-creator/SKILL.md)
- [Model Context Protocol](https://modelcontextprotocol.io/)

### 9.3 更新日志

| 日期 | 版本 | 变更 |
|------|------|------|
| 2025-01-16 | 1.0.0 | 初始版本发布 |

### 9.4 贡献指南

如需对本标准提出改进建议，请在项目仓库提交 Issue 或 Pull Request。

---

*AI Skills Standard v1.0 | 最后更新：2025-01-16*
