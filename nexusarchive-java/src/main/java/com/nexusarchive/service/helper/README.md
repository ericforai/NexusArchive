一旦我所属的文件夹有所变化，请更新我。

## 目录功能
本目录存放业务逻辑辅助类（Helpers），用于拆解过于庞大的 Service 和 Controller 类。
通过提取数据转换、第三方集成、复杂校验等非核心流程代码，降低主业务类的复杂度。

## 文件清单
| 文件 | 地位 | 功能描述 |
| :--- | :--- | :--- |
| AuditLogHelper.java | Java 类 | 封装审计日志的请求上下文解析、数据脱敏及快照构建逻辑 |
| CollectionBatchHelper.java | Java 类 | 封装采集批次的初始化、状态统计、四性检测执行及 DTO 映射 |
| ComplianceCheckHelper.java | Java 类 | 封装基于 DA/T 94 标准的档案合规性细则检查逻辑 |
| FourNatureAsyncHelper.java | Java 类 | 负责异步四性检测的结果合并、报告构建及签名日志持久化 |
| IngestHelper.java | Java 类 | 处理 SIP 包的业务规则校验、临时文件准备及 ERP 反馈构建 |
| OriginalVoucherHelper.java | Java 类 | 提供原始凭证类型别名映射及分类解析辅助 |
| PoolHelper.java | Java 类 | 处理凭证池详情映射、字段更新及演示数据生成逻辑 |
| PreviewHelper.java | Java 类 | 封装流式预览的文件传输、Range 请求处理及媒体类型识别 |
| RelationGraphHelper.java | Java 类 | 负责关联关系图谱的节点解析、路径查找及最终图数据构建 |
