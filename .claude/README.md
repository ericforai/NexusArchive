# .claude/

Claude Code configuration for NexusArchive project.

## 📋 AI Skills Standard

本项目的 Skills 遵循 **[AI Skills Standard v1.0](../../docs/standards/SKILLS_STANDARD.md)**。

> 标准位置：`docs/standards/SKILLS_STANDARD.md`

## File Manifest

| File/目录 | Purpose |
|----------|---------|
| `README.md` | 本文件 - 目录说明文档 |
| `settings.json` | 项目设置和防护规则 |
| `commands/` | 自定义斜杠命令 |
| `agents/` | Agent 定义（product-strategist, backend-architect） |
| `skills/` | AI Skills 定义（遵循 AISS v1.0） |

## Commands

| Command | Description |
|---------|-------------|
| `/review-backend` | Review Spring Boot code (controller/service/mapper) |
| `/review-frontend` | Review React/TypeScript components |
| `/fix-build` | Diagnose Maven or npm/Vite build failures |
| `/fix-api-500` | Trace HTTP 500 errors through the stack |
| `/db-check` | Verify PostgreSQL/Redis connectivity |

## Skills

Skills 定义位于 `skills/` 目录，遵循 **[AI Skills Standard v1.0](../../docs/standards/SKILLS_STANDARD.md)**：

| Skill | 描述 |
|-------|------|
| `entropy-reduction` | 模块化设计、重构、架构熵减 |
| `architecture-defense` | 架构防御、依赖约束、运行时架构可见性 |
| `minimalist-refactorer` | 极简重构、代码简化 |
| `paranoid-debugging` | 偏执型调试、生产级调试 |
| `self-verifying-tests` | 自验证测试、E2E 测试生成 |

详见：[SKILLS_LIST_CN.md](skills/SKILLS_LIST_CN.md)

## Usage

Commands can be invoked in Claude Code by typing the command name (e.g., `/fix-build`).

Skills are automatically triggered based on the `description` field in SKILL.md frontmatter.

## Related Files

- [`/CLAUDE.md`](../../CLAUDE.md) - Main project documentation for Claude Code
- [`/nexusarchive-java/pom.xml`](../../nexusarchive-java/pom.xml) - Backend dependencies
- [`/package.json`](../../package.json) - Frontend dependencies
- [`docs/standards/SKILLS_STANDARD.md`](../../docs/standards/SKILLS_STANDARD.md) - AI Skills Standard
