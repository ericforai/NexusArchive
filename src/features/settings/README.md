一旦我所属的文件夹有所变化，请更新我。
本目录存放系统设置功能模块（DDD-ish 分层：domain/application/infrastructure）。
仅对外暴露 `index.ts` 作为模块入口。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `index.ts` | 聚合入口 | 设置功能导出 |
| `domain/index.ts` | 领域层 | 领域类型导出 |
| `application/*` | 应用层 | 用例/Facade hooks |
| `infrastructure/*` | 基础设施层 | API 封装 |
