一旦我所属的文件夹有所变化，请更新我。
本目录存放签名验签模块的分层实现。

## 目录结构

- `api/`: 对外接口契约预留层
- `app/`: 持久化记录与查询边界
- `domain/`: 验签端口、结果模型、仓储接口
- `infra/`: MyBatis 持久化实现

## 对外契约

- 仅允许依赖 `com.nexusarchive.modules.signature.app..`
- 禁止外部直接依赖 `domain` / `infra`
