# .claude/

Claude Code configuration for NexusArchive project.

## File Manifest

| File | Purpose |
|------|---------|
| `README.md` | This file - directory documentation |
| `settings.json` | Project settings and guardrails |
| `commands/` | Custom slash commands |

## Commands

| Command | Description |
|---------|-------------|
| `/review-backend` | Review Spring Boot code (controller/service/mapper) |
| `/review-frontend` | Review React/TypeScript components |
| `/fix-build` | Diagnose Maven or npm/Vite build failures |
| `/fix-api-500` | Trace HTTP 500 errors through the stack |
| `/db-check` | Verify PostgreSQL/Redis connectivity |

## Usage

Commands can be invoked in Claude Code by typing the command name (e.g., `/fix-build`).

## Related Files

- `/CLAUDE.md` - Main project documentation for Claude Code
- `/nexusarchive-java/pom.xml` - Backend dependencies
- `/package.json` - Frontend dependencies
