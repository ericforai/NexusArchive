一旦我所属的文件夹有所变化，请更新我。
本目录存放 Sprint 0 核心测试。
用于全宗隔离与适配层原型验证。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `DbAdapterPrototypeTests.java` | 测试 | 适配层原型 DDL 生成测试 |
| `FondsIsolationInterceptorTests.java` | 测试 | 隔离拦截器红队测试 |
| `FondsContextFilterTests.java` | 测试 | 全宗上下文注入过滤器测试 |
| `NexusCoreApplicationTests.java` | 测试 | Spring Boot 启动测试 |
| `SqlAuditRulesResolverTests.java` | 测试 | SQL 审计字典优先策略验证 |
| `compliance/` | 目录入口 | 四性检测与哈希测试 |
| `sharding/` | 目录入口 | Sharding-JDBC 隔离 POC |
