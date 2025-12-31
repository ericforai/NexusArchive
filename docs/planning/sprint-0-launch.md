# Sprint 0 启动计划：内核与合规奠基

> **目标**: 扫清 "信创适配" 与 "强合规" 的技术障碍，构建可验证的**最小内核 (Kernel)**。
> **周期**: 1 周
> **交付物**: 可运行的 Skeleton 代码库 + 关键技术 Spike 报告

## 💥 核心攻坚 (Spike Goals)
1.  **架构排雷**: 对比 `MyBatis 拦截器` 与 `Sharding-JDBC` 的隔离能力，写 Demo 验证 “手写 SQL 绕过” 是否可防。
2.  **信创排雷**: 启用严格规则，**禁止任何 PG 特有函数/语法**进入代码库（CI 直接阻断）。
3.  **合规排雷**: 跑通 BouncyCastle 的 **SM3**，量化性能损耗并给出参数建议。

## 🎯 核心里程碑 (Milestones)

### M1: 基础设施 (Infrastructure)
*   **任务**: 搭建 "双模" 开发环境。
*   **产出**: `docker-compose.dev.yml`
    *   `postgres`: 作为主开发库。
    *   `middleware`: Redis (缓存), MinIO (模拟对象存储)。
    *   **关键约束**: 禁用 PG 特有插件 (如 PostGIS)，确保 ANSI SQL 兼容性以适配未来达梦迁移。

### M2: 架构内核 (Arch Kernel)
*   **任务**: 全宗隔离拦截器 (Fonds Barrier) + Sharding-JDBC 对比 Spike。
*   **产出**: `FondsIsolationInterceptor.java`
    *   拦截 MyBatis `Prepare` 阶段。
    *   硬性注入 `WHERE fonds_no = 'CURRENT_FONDS'`。
    *   **验收**: 单元测试 `IsolationTest.java`，试图绕过拦截器必须抛出异常。
*   **产出**: `sharding-poc/` 或 `spike-isolation-report.md`（对比拦截器 vs Sharding-JDBC 的结论与风险）

### M3: 适配层原型 (Adapter Spike)
*   **任务**: 数据库适配层 POC。
*   **产出**: `DbAdapter` 接口与 `SchemaManager`。
    *   证明同一套 Entity 可通过适配层在不同数据库通过检查（模拟）。
    *   建立 `DataTypeMapping` (Java Type -> PG Type / Dameng Type)。

### M4: 合规引擎 (Compliance Engine)
*   **任务**: 国密与四性基础。
*   **产出**: `FourNatureService` 原型。
    *   集成 `BouncyCastle`，跑通 SM3 哈希计算。
    *   实现 "Magic Number" 校验工具类。
    *   **性能基线**: SM3 哈希吞吐/延迟基准（记录 CPU 占用与数据规模）。

---

## 📅 执行清单 (Action Items)

### Day 1: 初始化
- [x] 初始化 Spring Boot 3.1.6 工程 (`nexus-core`)。
- [x] 配置 CheckStyle/SpotBugs + SQL Lint（**禁止 PG 特有函数/语法**）。
- [x] 提交 `docker-compose.dev.yml`。

### Day 2: 隔离与安全
- [x] 实现 `CurrentUser` 上下文注入 (ThreadLocal)。
- [x] 开发 `FondsIsolationInterceptor`。
- [x] 编写 "反向测试用例" (Red Team Test)，模拟手写 SQL 绕过。

### Day 3: 数据库适配
- [x] 定义 `BaseEntity` (含 `fonds_no`, `archive_year`, `version`)。
- [x] 编写 DDL 生成器原型 (根据 Dialect 生成不同 SQL)。
- [x] Sharding-JDBC 隔离 POC（与拦截器对比，输出结论）。

### Day 4: 审计与四性 (SM3 国密)
- [x] 引入 `BouncyCastle bcprov-jdk18on:1.77` 依赖。
- [x] 实现 `Sm3HashService` (纯算法封装 + SHA256 降级)。
- [x] 实现 `FileHashService` (流式大文件哈希)。
- [x] SM3 性能基线测试 (1KB~100MB 各规模吞吐/延迟)。
- [x] 实现 `MagicNumberValidator` (PDF/OFD/XML/JPG)。
- [x] 搭建 `FourNatureCheckService` 骨架。

### Day 5: 决策收敛与总结
- [x] 整合隔离体系与四性引擎 (`ArchiveSubmitService` 调用链)。
- [x] 输出 `spike-isolation-report.md` 最终版（拦截器 vs Sharding 决策）。
- [x] 汇总 Sprint 0 技术结论与风险清单 (`sprint0-summary.md`)。
- [x] 同步 CHANGELOG / change-list。

---

## 🛠 开发规约 (Strict Rules)

1.  **No JSONB Index**: 严禁使用 `@Type(JsonBinaryType.class)` 进行索引查询。JSON 仅用于 Blob 存储可变字段。
2.  **Explicit SQL**: 不要过度依赖 MP 的 Wrapper，复杂的跨表查询必须手写 xml，且严禁使用数据库特有函数 (如 `jsonb_extract_path`)。
3.  **DB Neutral**: SQL Lint 必须阻断 PG 专有语法/函数（仅允许在适配层隔离模块出现）。
4.  **Trace Everything**: 所有 Service 入口必须绑定 `TraceID`。
