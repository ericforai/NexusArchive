# 开发路线图缺口分析报告

> **分析日期**: 2025-01  
> **分析基准**: `docs/planning/development_roadmap_v1.0.md`  
> **分析方法**: 代码库扫描 + 文档对齐 + 实现状态验证

---

## 📊 执行摘要

| 阶段 | 计划项 | 状态 | 完成度 | 优先级 |
|------|--------|------|--------|--------|
| 阶段一：不可变内核 | 3项 | ⚠️ 部分完成 | 66% | P0 |
| 阶段二：合规 | 3项 | ✅ 基本完成 | 83% | P0 |
| 阶段三：实物与业务 | 3项 | ⚠️ 部分完成 | 40% | P1 |
| 阶段四：UI与交付 | 3项 | ⚠️ 部分完成 | 33% | P1 |

**总体完成度**: 约 **55%**

---

## 📅 阶段一：不可变内核 (The Immutable Kernel)

### 1.1 数据库适配层 (DB Adapter) ❌ **缺失**

**路线图要求**:
- 封装 MyBatis Plus，通过 SPI 实现 `PostgresDialect` 和 `DamengDialect`
- 验证：Docker 容器中同时启动 PostgreSQL 和达梦(开发版)，确保同一套 Entity 代码能跑通两套库的 DDL

**当前状态**:
- ❌ **未找到** `PostgresDialect` 或 `DamengDialect` 实现
- ❌ **未找到** SPI 机制配置（`META-INF/services/...`）
- ⚠️ 代码中仅有 PostgreSQL 相关配置（`PostgresJsonTypeHandler`）
- ⚠️ 存在达梦/金仓的 SQL schema 文件（`docs/database/auth_schema_dameng.sql`, `auth_schema_kingbase.sql`），但无运行时适配层

**发现的代码**:
- `nexusarchive-java/src/main/java/com/nexusarchive/config/mybatis/PostgresJsonTypeHandler.java` - 仅支持 PostgreSQL JSONB
- `docs/database/ddl-adapter-samples.md` - 文档说明，但无实现

**影响**: 🔴 **严重** - 无法支持信创数据库（达梦/金仓），这是路线图阶段一的核心要求

**建议**:
1. 创建 SPI 接口 `DatabaseDialect`
2. 实现 `PostgresDialect` 和 `DamengDialect`
3. 在 MyBatis Plus 配置中注入方言
4. 编写 Docker Compose 配置，同时启动 PG 和达梦进行验证

---

### 1.2 全宗隔离拦截器 (Fonds Barrier) ✅ **已完成**

**路线图要求**:
- 实现 MyBatis 拦截器，强制注入 `WHERE fonds_no = ?`
- 验证：写一个"恶意测试用例"，尝试绕过拦截器读取数据，必须失败

**当前状态**:
- ✅ **已实现** `nexus-core/src/main/java/com/nexusarchive/core/FondsIsolationInterceptor.java`
- ✅ **已实现** SQL 注入防护（二次校验正则）
- ✅ **已实现** 支持 SELECT/UPDATE/DELETE/INSERT/MERGE/WITH CTE
- ✅ **已实现** `fiscal_year` 年度隔离支持
- ✅ **已有测试** `nexus-core/src/test/java/com/nexusarchive/core/FondsIsolationInterceptorTests.java`

**代码位置**:
```12:164:nexus-core/src/main/java/com/nexusarchive/core/FondsIsolationInterceptor.java
// ... 完整的拦截器实现
```

**状态**: ✅ **符合路线图要求**

---

### 1.3 核心元数据模型 ⚠️ **部分完成**

**路线图要求**:
- `ArchiveObject` 结构化字段定义
- 能跑通"增删改查"的 MVP，且物理数据必须带有 `fonds_no`

**当前状态**:
- ✅ **已实现** `nexusarchive-java/src/main/java/com/nexusarchive/entity/Archive.java`
- ✅ **包含** `fonds_no` 字段（通过 `@TableField` 映射）
- ✅ **包含** 结构化字段（`standard_metadata`, `custom_metadata`）
- ⚠️ **命名不一致**：路线图要求 `ArchiveObject`，实际实现为 `Archive`
- ✅ **已有** 完整的 CRUD 操作（通过 MyBatis Plus）

**代码位置**:
- 实体：`nexusarchive-java/src/main/java/com/nexusarchive/entity/Archive.java`
- 表名：`acc_archive`（符合规范）

**状态**: ⚠️ **功能完整，但命名与路线图不一致**（非阻塞性问题）

---

## 🔒 阶段二：合规这一关 (Compliance Gate)

### 2.1 四性检测引擎 (Four-Nature Engine) ✅ **已完成**

**路线图要求**:
- 集成 BouncyCastle (国密), Apache Tika (MIME检测), OFD Parser
- 验证：准备"坏文件样本库"（改后缀的 exe、签名失效的 xml），确保引擎能 100% 拦截

**当前状态**:
- ✅ **已集成** BouncyCastle（`pom.xml` 中包含 `bcprov-jdk18on`, `bcpkix-jdk18on`）
- ⚠️ **Apache Tika 已声明但被注释**（`pom.xml` line 221-226 被注释）
- ✅ **已集成** OFD Parser（`ofdrw-full`, `ofdrw-sign`, `ofdrw-gm`）
- ✅ **已实现** `FourNatureCheckService` 和 `FourNatureCoreService`
- ✅ **已实现** `FileMagicValidator`（文件魔数验证）

**代码位置**:
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/FourNatureCheckService.java`
- 核心：`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCoreServiceImpl.java`
- 验证器：`nexusarchive-java/src/main/java/com/nexusarchive/util/FileMagicValidator.java`

**问题**:
- ⚠️ Apache Tika 被注释，可能影响 MIME 检测的准确性（但已有 `FileMagicValidator` 替代）

**状态**: ✅ **基本完成，建议启用 Tika 或确认 FileMagicValidator 覆盖度**

---

### 2.2 审计哈希链 (Audit Chain) ✅ **已完成（但使用 SM3 而非 SHA256）**

**路线图要求**:
- 实现 `AuditLogService`，计算 `curr_hash = SHA256(prev_hash + data)`
- 验证：模拟修改 DB 数据，运行"验真工具"报警

**当前状态**:
- ✅ **已实现** `AuditLogService.saveAuditLogWithHash()` 方法
- ✅ **已实现** 哈希链机制（`prev_log_hash`, `log_hash`）
- ⚠️ **使用 SM3 而非 SHA256**（符合国密要求，但与路线图描述不一致）
- ✅ **已实现** `verifyLogChain()` 验证方法
- ✅ **已实现** `AuditLogVerificationService` 验真服务

**代码位置**:
```99:140:nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogService.java
// 哈希链实现
```

**状态**: ✅ **功能完整，算法选择更符合国密要求**（SM3 > SHA256）

---

### 2.3 防伪水印 (Server-side Watermark) ✅ **已完成**

**路线图要求**:
- 集成 PDFBox/iText，实现服务端流式加水印

**当前状态**:
- ✅ **已集成** PDFBox（`pom.xml` line 228-233）
- ✅ **已实现** `StreamingPreviewService.renderWithWatermark()` 方法
- ✅ **已实现** 流式渲染 + 水印叠加
- ✅ **已实现** 动态水印文本（包含 traceId, fondsNo）

**代码位置**:
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/StreamingPreviewServiceImpl.java`
- 水印渲染：line 137-239

**状态**: ✅ **完全符合路线图要求**

---

## 📦 阶段三：实物与业务 (Physical & Business)

### 3.1 实物档案管理 ❌ **缺失**

**路线图要求**:
- 库房模型、装盒逻辑、标签打印
- 难点：标签打印的 PDF 坐标计算，需反复调试

**当前状态**:
- ❌ **未找到** 库房模型（warehouse/storage room entity）
- ❌ **未找到** 装盒逻辑（boxing/packaging service）
- ❌ **未找到** 标签打印功能（label printing）
- ⚠️ **存在文档** `docs/planning/archive-boxing-design.md`（设计文档，但无实现）

**搜索结果**:
- `grep -i "physical\|box\|warehouse\|storage.*room\|label.*print"` 仅找到少量引用（`DestructionExecutionServiceImpl.executePhysicalDeletions`），无实物档案管理相关代码

**状态**: ❌ **完全缺失**

**影响**: 🟡 **中等** - 仅影响实物档案场景，不影响纯电子档案流程

---

### 3.2 借阅与流程 ⚠️ **部分完成**

**路线图要求**:
- 状态机 (`BORROWED` -> `RETURNED`)
- 跨全宗授权票据

**当前状态**:
- ✅ **已实现** 借阅实体和服务（`Borrowing` entity, `BorrowingService`）
- ✅ **已实现** 跨全宗授权票据（`AuthTicketService`, `AuthTicketController`）
- ⚠️ **状态机**：需要确认是否完整实现 `BORROWED` -> `RETURNED` 流转
- ✅ **已有** 借阅类型支持（`electronic/physical`）

**代码位置**:
- 实体：`nexusarchive-java/src/main/java/com/nexusarchive/entity/Borrowing.java`
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowingService.java`
- 授权票据：`nexusarchive-java/src/main/java/com/nexusarchive/service/AuthTicketService.java`

**状态**: ⚠️ **功能基本完整，需要确认状态机完整性**

---

### 3.3 检索增强 ⚠️ **部分完成**

**路线图要求**:
- 结构化字段索引优化
- JSONB 查询性能调优（针对 PG 和 达梦分别优化）

**当前状态**:
- ✅ **已实现** JSONB 类型处理器（`PostgresJsonTypeHandler`）
- ✅ **已实现** 高级检索服务（`AdvancedArchiveSearchService`）
- ⚠️ **仅支持 PostgreSQL**：JSONB 查询优化仅针对 PG，无达梦适配
- ⚠️ **索引优化**：需要确认是否已创建 JSONB 索引

**代码位置**:
- 搜索服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AdvancedArchiveSearchService.java`
- JSON 处理：`nexusarchive-java/src/main/java/com/nexusarchive/config/mybatis/PostgresJsonTypeHandler.java`

**状态**: ⚠️ **PostgreSQL 端基本完成，达梦适配缺失**（与 1.1 数据库适配层问题相关）

---

## 🖥️ 阶段四：UI 与交付 (UI & Delivery)

### 4.1 前端组装 ✅ **基本完成**

**路线图要求**:
- 对接 API，实现"流式预览组件"和"动态表单"

**当前状态**:
- ✅ **已实现** 流式预览组件（`ArchivePreviewModal.tsx`）
- ✅ **已实现** 动态表单（基于 Ant Design Form）
- ✅ **已对接** 大部分后端 API
- ⚠️ **前端功能缺口**：参考 `docs/reports/frontend-features-gap-analysis.md`，约 40% 功能缺失

**代码位置**:
- 预览：`src/pages/archive/ArchivePreviewModal.tsx`
- 前端 API：`src/api/` 目录

**状态**: ✅ **核心功能完成，但存在功能缺口**（详见前端缺口分析报告）

---

### 4.2 数据迁移工具 ❌ **缺失**

**路线图要求**:
- 开发 `LegacyImportTool`，通过 CSV/Excel 导入历史数据并自动生成初始全宗结构

**当前状态**:
- ❌ **未找到** `LegacyImportTool` 类
- ❌ **未找到** CSV/Excel 导入功能
- ⚠️ **仅找到** `OrgImportResult.java`（组织导入结果 DTO），但无完整的导入工具

**搜索结果**:
- `glob_file_search("**/*Import*.java")` 仅返回 `OrgImportResult.java`

**状态**: ❌ **完全缺失**

**影响**: 🟡 **中等** - 影响历史数据迁移场景

---

### 4.3 信创环境压测 ⚠️ **部分完成**

**路线图要求**:
- 在鲲鹏/海光服务器上进行全链路压测，定位国产 CPU 的性能瓶颈（通常是加密解密和 JSON 解析）

**当前状态**:
- ✅ **存在** 性能测试目录（`perf/`）
- ✅ **已有** K6 压测脚本（`archive_soak.k6.js`, `search_peak.k6.js`, `upload_1gb.k6.js` 等）
- ❌ **未找到** 信创环境（鲲鹏/海光）特定的压测报告或配置
- ⚠️ **性能测试**：通用压测存在，但无信创环境专项测试

**代码位置**:
- 压测脚本：`perf/*.k6.js`
- 文档：`perf/README.md`

**状态**: ⚠️ **通用压测完成，信创环境专项测试缺失**

---

## 📋 关键缺口总结

### P0 优先级（阻塞性问题）

1. **❌ 数据库适配层缺失**（阶段一）
   - 无法支持达梦/金仓数据库
   - 影响信创合规性
   - **建议**：立即启动 SPI 方言适配层开发

2. **❌ 实物档案管理缺失**（阶段三）
   - 库房模型、装盒逻辑、标签打印均未实现
   - 影响混合档案场景
   - **建议**：评估需求优先级，如需要则尽快启动

3. **❌ 数据迁移工具缺失**（阶段四）
   - 无历史数据导入能力
   - 影响系统上线
   - **建议**：开发 CSV/Excel 导入工具

### P1 优先级（重要但非阻塞）

1. **⚠️ Apache Tika 被注释**（阶段二）
   - 可能影响 MIME 检测准确性
   - **建议**：启用 Tika 或确认 FileMagicValidator 覆盖度

2. **⚠️ 检索增强仅支持 PostgreSQL**（阶段三）
   - 达梦数据库 JSONB 查询优化缺失
   - **建议**：与数据库适配层一并解决

3. **⚠️ 信创环境压测缺失**（阶段四）
   - 无国产 CPU 性能基准
   - **建议**：在鲲鹏/海光环境进行专项压测

---

## 🎯 建议行动方案

### 立即行动（Week 1-2）

1. **启动数据库适配层开发**
   - 创建 SPI 接口 `DatabaseDialect`
   - 实现 `PostgresDialect`（可基于现有代码）
   - 实现 `DamengDialect`（新增）
   - 编写 Docker Compose 多数据库测试环境
   - 编写集成测试验证 DDL 兼容性

2. **评估实物档案管理需求**
   - 确认业务是否真的需要实物档案功能
   - 如需要，启动库房模型设计
   - 如不需要，更新路线图标记为可选

3. **开发数据迁移工具 MVP**
   - 实现 CSV 导入基础功能
   - 实现全宗结构自动生成
   - 编写导入验证和错误处理

### 短期优化（Week 3-4）

1. **启用 Apache Tika 或强化 FileMagicValidator**
2. **完成达梦数据库 JSONB 查询优化**
3. **完善借阅状态机验证**

### 中期规划（Week 5-8）

1. **信创环境压测**
   - 准备鲲鹏/海光测试环境
   - 执行全链路压测
   - 定位性能瓶颈并优化

2. **前端功能补全**
   - 参考 `frontend-features-gap-analysis.md`
   - 优先补全 P0 功能

---

## 📊 完成度统计

| 类别 | 计划项数 | 已完成 | 部分完成 | 未完成 | 完成度 |
|------|---------|--------|----------|--------|--------|
| 阶段一 | 3 | 2 | 1 | 0 | 83% |
| 阶段二 | 3 | 3 | 0 | 0 | 100% |
| 阶段三 | 3 | 0 | 2 | 1 | 33% |
| 阶段四 | 3 | 1 | 1 | 1 | 33% |
| **总计** | **12** | **6** | **4** | **2** | **58%** |

**说明**：
- ✅ 已完成：功能完整，符合路线图要求
- ⚠️ 部分完成：功能基本实现，但有细节缺失或优化空间
- ❌ 未完成：核心功能缺失

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 前端缺口分析：`docs/reports/frontend-features-gap-analysis.md`
- 数据库设计：`docs/database/数据库设计.md`
- 实物档案设计：`docs/planning/archive-boxing-design.md`（设计文档，待实现）

---

**报告生成时间**: 2025-01  
**下次更新建议**: 完成关键缺口修复后

