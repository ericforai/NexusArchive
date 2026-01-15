一旦我所属的文件夹有所变化，请更新我。
本目录存放借阅模块的分层实现。

## 目录结构

- `api/`: 对外接口（Controller + DTO）
- `app/`: 应用层（Facade / 用例编排）
- `domain/`: 领域模型（Entity、Status 等）
- `infra/`: 基础设施（Mapper、策略实现）

## 对外契约

- 仅允许依赖 `com.nexusarchive.modules.borrowing.app..` 与 `com.nexusarchive.modules.borrowing.api.dto..`
- 禁止外部访问 `domain` / `infra`
