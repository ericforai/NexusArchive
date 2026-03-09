一旦我所属的文件夹有所变化，请更新我。
本目录存放领域实体。
用于映射数据库表结构。
借阅实体已迁移至 `com.nexusarchive.modules.borrowing.domain`。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `AbnormalVoucher.java` | Java 类 | 异常凭证实体 |
| `ArcFileContent.java` | Java 类 | 档案文件内容实体（含四性检测） |
| `ArcFileMetadataIndex.java` | Java 类 | 档案元数据索引实体 |
| `ArchivalCodeSequence.java` | Java 类 | 档案号序列实体 |
| `Archive.java` | Java 类 | 档案主表实体 |
| `ArchiveApproval.java` | Java 类 | 归档审批实体 |
| `ArchiveAttachment.java` | Java 类 | 档案附件实体 |
| `ArchiveBatch.java` | Java 类 | 归档批次实体 |
| `ArchiveRelation.java` | Java 类 | 档案关联关系实体 |
| `ArcSignatureLog.java` | Java 类 | 电子签章日志实体 |
| `AuditInspectionLog.java` | Java 类 | 审计验真日志实体 |
| `BasFonds.java` | Java 类 | 全宗实体 |
| `ConvertLog.java` | Java 类 | 格式转换日志实体 |
| `Destruction.java` | Java 类 | 销毁实体 |
| `enums/` | 目录入口 | 枚举子目录 |
| `ErpConfig.java` | 配置类 | ERP 配置实体（账套映射、AI 适配器、路由校验方法） |
| `ErpScenario.java` | Java 类 | ERP 场景实体 |
| `ErpSubInterface.java` | Java 类 | ERP 子接口实体 |
| `es/` | 目录入口 | ES 子目录 |
| `IngestRequestStatus.java` | Java 类 | 归档请求状态实体 |
| `Location.java` | Java 类 | 存储位置实体 |
| `OpenAppraisal.java` | Java 类 | 开放鉴定实体 |
| `Permission.java` | Java 类 | 权限实体 |
| `Position.java` | Java 类 | 岗位实体 |
| `ReconciliationRecord.java` | Java 类 | 对账记录实体 |
| `Role.java` | Java 类 | 角色实体 |
| `SyncHistory.java` | Java 类 | 同步历史实体 |
| `SysAuditLog.java` | Java 类 | 审计日志实体（SM3 哈希链、防篡改、trace/source/target/authTicket 上下文） |
| `SysEntity.java` | Java 类 | 法人实体（含 parentId/orderNum 支持树形结构） |
| `SystemSetting.java` | Java 类 | 系统设置实体 |
| `User.java` | Java 类 | 用户实体 |
| `Volume.java` | Java 类 | 案卷实体 |
