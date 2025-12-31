# Sprint 0 Spike: 全宗隔离方案最终决策报告

> 日期：2025-12-31
> 版本：Final
> 目标：明确 Sprint 0 隔离方案的最终选择，并记录决策依据与后续演进路径。

## 🎯 决策结论

### 主方案: MyBatis 拦截器 + SQL 审计守卫
**选择理由**:
1. **强制阻断**: 缺失 `fonds_no` / `archive_year` 的 SQL 直接抛异常，无法绕过。
2. **自动注入**: SELECT 查询自动补全隔离字段，开发者无需手动添加。
3. **低侵入**: 无需更改现有 Mapper 代码，拦截器透明工作。
4. **配置灵活**: 支持 YAML + 数据库字典双模式配置。

### 辅助方案: Sharding-JDBC 复合分片 (储备)
**定位**: 物理分区储备，暂不启用。
**适用场景**:
- 数据量超过单库承载能力 (>1亿条)
- 需要按全宗/年度物理隔离存储

## 📊 方案对比

| 维度 | MyBatis 拦截器 | Sharding-JDBC |
| --- | --- | --- |
| 目标定位 | SQL 强制注入/阻断 | 数据源/表路由 |
| 缺失隔离字段 | **阻断** | 可能广播 |
| 实现复杂度 | 低 | 中 |
| 运维成本 | 低 | 高 |
| 性能开销 | 极低 | 低 |
| 适配多库 | 不直接帮助 | 有 |

## 🏗 最终架构

```
请求层
├── FondsContextFilter (设置 fonds_no)
└── ArchiveYearContextFilter (设置 archive_year)
    ↓
业务层
├── ArchiveSubmitService
│   └── FourNatureCheckService.check() (四性检测)
    ↓
持久层
├── SqlAuditGuardInterceptor (审计阻断)
└── FondsIsolationInterceptor (自动注入)
    ↓
数据库
```

## ⚠️ 风险与缓解

| 风险 | 影响 | 缓解措施 |
| --- | --- | --- |
| 复杂 SQL 正则边界 | 注入失败 | 增加单元测试覆盖 |
| 性能开销 | 延迟增加 | 基准测试 < 1ms，可忽略 |
| 多数据源兼容 | 拦截器失效 | 每个数据源独立注册 |

## 📈 后续演进路径

### Sprint 1
- 完善四性检测 (完整性元数据校验)
- 引入 ClamAV 病毒扫描

### Sprint 2+
- 数据量增长后评估 Sharding 分区必要性
- 考虑 RLS (Row-Level Security) 数据库级兜底

## ✅ 验收确认

| 检查项 | 状态 |
| --- | --- |
| 拦截器阻断无隔离字段 SQL | ✅ |
| 拦截器自动注入 SELECT | ✅ |
| SQL 审计守卫配置化 | ✅ |
| 字典优先策略 | ✅ |
| Sharding POC 验证 | ✅ |
| 性能基准 | ✅ |
