一旦我所属的文件夹有所变化，请更新我。

## 目录功能
本目录存放系统核心业务服务的实现类（Service Implementation）。
负责具体的业务逻辑编排、数据库持久化调用及跨模块协调。
借阅模块服务实现已迁移至 `com.nexusarchive.modules.borrowing.app`。

## 文件清单

| 文件 | 地位 | 功能描述 |
| :--- | :--- | :--- |
| AbnormalVoucherServiceImpl.java | 服务实现 | 处理异常凭证的标记、核销与监控逻辑 |
| ArchivalPackageServiceImpl.java | 服务实现 | 负责 DA/T 94 标准 SIP/AIP 封装包的构建与解析 |
| ArchiveApprovalServiceImpl.java | 服务实现 | 处理归档审批流程及状态同步 |
| ArchiveExportServiceImpl.java | 服务实现 | 负责档案数据的批量导出与格式转换 |
| ArchiveSearchServiceImpl.java | 服务实现 | 提供基础档案检索功能 |
| ArchiveSecurityServiceImpl.java | 服务实现 | 负责存证挂链、防篡改校验等安全核心逻辑 |
| AttachmentServiceImpl.java | 服务实现 | 负责档案附件的关联管理 |
| AuditLogSamplingServiceImpl.java | 服务实现 | 负责审计日志的抽样审计逻辑 |
| AuditLogVerificationServiceImpl.java | 服务实现 | 负责审计日志哈希链的完整性验证 |
| BasFondsServiceImpl.java | 服务实现 | 处理全宗基础信息管理 |
| BatchToArchiveServiceImpl.java | 服务实现 | 负责将采集批次转换为正式档案记录 |
| CollectionBatchServiceImpl.java | 服务实现 | 处理手动上传采集批次的生命周期管理 |
| DestructionServiceImpl.java | 服务实现 | 负责档案销毁申请与执行逻辑 |
| FileStorageServiceImpl.java | 服务实现 | 提供底层文件存储方案的抽象与执行 |
| FondsHistoryServiceImpl.java | 服务实现 | 记录全宗变更历史及关键链路审计快照 |
| FourNatureCheckServiceImpl.java | 服务实现 | 档案“四性”检测（真实、完整、可用、安全）业务逻辑 |
| FourNatureCoreServiceImpl.java | 服务实现 | “四性”检测的核心算法实现 |
| GlobalSearchServiceImpl.java | 服务实现 | 提供跨模块的全局搜索引擎实现 |
| IngestServiceImpl.java | 服务实现 | 负责 SIP 接口入库及异步归档流水线 |
| MonitoringServiceImpl.java | 服务实现 | 负责系统运行状态与性能监控 |
| NotificationServiceImpl.java | 服务实现 | 负责系统内部消息与通知的推送 |
| OfdConvertServiceImpl.java | 服务实现 | 负责 PDF 到国家标准 OFD 格式的转换 |
| OpenAppraisalServiceImpl.java | 服务实现 | 处理档案开放鉴定业务逻辑 |
| PoolServiceImpl.java | 服务实现 | 负责记账凭证池的元数据补录与候选关联 |
| PositionServiceImpl.java | 服务实现 | 负责库房库位排架管理 |
| ReconciliationServiceImpl.java | 服务实现 | 处理 ERP 数据自动核对与归档状态回写 |
| SmartParserServiceImpl.java | 服务实现 | 提供基于规则或 AI 的元数据智能解析 |
| StatsServiceImpl.java | 服务实现 | 负责系统各维度的统计报表生成 |
| StreamingPreviewServiceImpl.java | 服务实现 | 提供支持水印、Range 的高性能流式预览 |
| WarehouseServiceImpl.java | 服务实现 | 处理库房实物档案关联管理 |
| WorkflowServiceImpl.java | 服务实现 | 负责业务流程引擎的驱动与状态流转 |
