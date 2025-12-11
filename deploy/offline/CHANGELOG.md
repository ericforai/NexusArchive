# NexusArchive Offline Installer Changelog

## v2.0.2 (2025-12-11)
### Fixed
- **License 持久化**: 修复重启后 License 丢失的严重 Bug。
  - License 现在会自动保存到 `data/license.json`。
  - 服务启动时自动加载已保存的 License，无需重复导入。
- **YonSuite 集成**: 修复 API Key 环境变量未加载的问题。

### Added
- `GenLicense.java`: 独立 License 生成工具（无外部依赖）。
- `.env` 环境变量支持：敏感配置通过 `.env` 文件管理。

## v2.0.1 (2025-12-11)
### Changed
- **Frontend**: Updated frontend assets to latest build (Strict Mode + Unified Auth).
  - Fixed "Dashboard data loading failed" issue.
  - Fixed "403 Forbidden" errors by unifying token management.
  - Enforced strict type checking.

## v2.0.0 (Initial Release)
- Base version.
