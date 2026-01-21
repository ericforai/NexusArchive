# Product Guidelines: NexusArchive

## 1.0 Documentation Philosophy: AI-First & Context-Aware
本文档系统专为 AI 辅助开发设计。所有文档必须具备高上下文密度、结构化清晰且逻辑自洽，以最大化 LLM (Large Language Models) 的理解与维护能力。

### 1.1 "Context is King" Rule
- **Explicit Linking**: 所有文档必须通过相对路径明确引用相关联的文件（如 spec -> plan -> code）。
- **Self-Contained Modules**: 每个目录下的 `README.md` 必须充当该模块的“上下文锚点”，清晰定义该模块的职责、输入/输出和关键依赖。
- **No Ambiguity**: 避免使用模糊的代词（如“它”、“这个功能”），必须使用全称或确切的标识符。

## 2.0 Documentation Structure Standards

### 2.1 Directory Context Files (README.md)
每个主要源码目录（如 `src/features/xxx`, `src/components/xxx`）**必须**包含一个 `README.md`，格式如下：
```markdown
# [Module Name]

## Role
[一句话描述该模块在系统中的作用]

## Capabilities
- [功能点 1]: [描述]
- [功能点 2]: [描述]

## Key Files
- `index.ts`: 公共导出接口
- `types.ts`: 类型定义
```

### 2.2 File Headers (Source Code)
关键逻辑文件（Controller, Service, Complex Components）顶部**必须**包含 AI 导读注释：
```typescript
/**
 * @module [模块名称]
 * @purpose [该文件的核心职责]
 * @input [主要输入参数/Props]
 * @output [主要输出/返回值]
 * @dependencies [依赖的关键外部模块]
 */
```

## 3.0 Maintenance Protocol: "Documentation as Code"
文档与代码视为同等重要的一等公民。

- **Atomic Updates**: 任何代码变更（Refactor, Feat, Fix）必须包含对应的文档更新。禁止“稍后补文档”。
- **Consistency Check**: 每次提交前，必须验证 `docs/` 和 `conductor/` 下的文档是否反映了最新的代码状态。

## 4.0 Visual & UI Guidelines (Reference)
虽然以功能为主，但 UI 实现需遵循：
- **Ant Design Pro** 默认规范。
- **Dense Mode**: 针对财务场景，默认使用紧凑型布局。
- **Feedback First**: 所有异步操作（加载、提交）必须有明确的 UI 状态反馈。