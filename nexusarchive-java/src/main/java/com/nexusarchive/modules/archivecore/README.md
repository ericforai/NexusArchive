一旦我所属的文件夹有所变化，请更新我。
本目录存放档案核心模块的分层实现。

## 目录结构

- `api/`: 对外契约（请求 DTO）
- `app/`: 应用层（Facade / 用例编排）
- `domain/`: 预留给档案核心领域模型
- `infra/`: 预留给档案核心基础设施适配

## 对外契约

- 仅允许依赖 `com.nexusarchive.modules.archivecore.app..` 与 `com.nexusarchive.modules.archivecore.api.dto..`
- 禁止外部直接访问 `domain/` / `infra/`
