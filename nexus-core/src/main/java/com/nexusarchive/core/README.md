一旦我所属的文件夹有所变化，请更新我。
本目录存放 Sprint 0 核心逻辑。
用于全宗隔离与数据库适配原型。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `adapter/` | 目录入口 | 数据库适配层原型 |
| `ArchiveYearContextFilter.java` | 过滤器 | 请求链路注入 archive_year |
| `ArchiveYearContext.java` | 类 | 归档年度上下文 |
| `FondsContext.java` | 类 | 全宗上下文管理 |
| `FondsContextFilter.java` | 过滤器 | 请求链路注入 fonds_no |
| `FondsIsolationException.java` | 类 | 隔离异常定义 |
| `FondsIsolationInterceptor.java` | 类 | 全宗隔离拦截器 |
| `MybatisPlusConfig.java` | 配置 | MyBatis-Plus 拦截器配置 |
| `NexusCoreApplication.java` | 启动入口 | Sprint 0 应用入口 |
| `SqlAuditGuard.java` | 类 | SQL 审计守卫（必填字段校验） |
| `SqlAuditGuardInterceptor.java` | 类 | SQL 审计守卫拦截器 |
| `SqlAuditGuardProperties.java` | 配置 | SQL 审计守卫配置属性 |
| `SqlAuditRules.java` | 类 | SQL 审计规则快照 |
| `SqlAuditRulesDictionaryProvider.java` | 类 | SQL 审计规则字典加载 |
| `SqlAuditRulesProvider.java` | 接口 | SQL 审计规则来源扩展点 |
| `SqlAuditRulesResolver.java` | 类 | SQL 审计规则解析 |
