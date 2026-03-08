一旦我所属的文件夹有所变化，请更新我。

# app

档案核心模块应用层。
负责编排档案读写、文件查询与 DTO 到实体的映射。

- `ArchiveApplicationService`: facade 实现，协调读写服务与文件查询
- `ArchiveFileQueryService`: 模块内聚合档案直接文件、附件与关联原始凭证文件
