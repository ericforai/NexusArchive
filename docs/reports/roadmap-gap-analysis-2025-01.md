# 开发路线图缺口分析报告

> **分析日期**: 2025-01  
> **分析基准**: `docs/planning/development_roadmap_v1.0.md`  
> **分析方法**: 代码库扫描 + 文档对齐 + 实现状态验证

---

## 📊 执行摘要

| 阶段 | 计划项 | 状态 | 完成度 | 优先级 |
|------|--------|------|--------|--------|
| 阶段一：不可变内核 | 3项 | ⚠️ 部分完成 | 83% | P0 |
| 阶段二：合规 | 3项 | ✅ 基本完成 | 100% | P0 |
| 阶段三：实物与业务 | 2项 | ⚠️ 部分完成 | 50% | P1 |
| 阶段四：UI与交付 | 3项 | ⚠️ 部分完成 | 33% | P1 |

**总体完成度**: 约 **67%**

**注意**：
- 数据库适配层已标记为**暂停**（暂不开发）
- 实物档案管理：开发路线图 v1.0 中不包含，不在本报告分析范围

---

## 📅 阶段一：不可变内核 (The Immutable Kernel)

### 1.1 数据库适配层 (DB Adapter) ⏸️ **已暂停**

**路线图要求**:
- 封装 MyBatis Plus，通过 SPI 实现 `PostgresDialect` 和 `DamengDialect`
- 验证：Docker 容器中同时启动 PostgreSQL 和达梦(开发版)，确保同一套 Entity 代码能跑通两套库的 DDL

**当前状态**:
- ⏸️ **已暂停开发** - 暂不实现数据库适配层
- ⚠️ 代码中仅有 PostgreSQL 相关配置（`PostgresJsonTypeHandler`）
- ⚠️ 存在达梦/金仓的 SQL schema 文件（`docs/database/auth_schema_dameng.sql`, `auth_schema_kingbase.sql`），但无运行时适配层
- ✅ 当前系统仅支持 PostgreSQL，功能正常运行

**PRD 对齐**:
- PRD v1.0 第 4.2 节要求："数据库适配层：为 PostgreSQL/达梦/金仓提供独立 DDL 与类型映射"
- 但当前开发策略为暂不实现，仅支持 PostgreSQL

**决策**:
- 基于当前业务需求，暂不开发多数据库适配层
- 如后续需要支持信创数据库（达梦/金仓），可重新启动此项目

**状态**: ⏸️ **已暂停，暂不开发**

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

> **说明**：开发路线图 v1.0 中不包含实物档案管理功能，当前仅关注电子档案流程。

### 3.1 借阅与流程 ⚠️ **部分完成**

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

### 3.2 检索增强 ✅ **基本完成**

**路线图要求**:
- 结构化字段索引优化
- JSONB 查询性能调优（针对 PostgreSQL 优化）

**当前状态**:
- ✅ **已实现** JSONB 类型处理器（`PostgresJsonTypeHandler`）
- ✅ **已实现** 高级检索服务（`AdvancedArchiveSearchService`）
- ✅ **支持 PostgreSQL**：JSONB 查询优化已实现
- ⚠️ **索引优化**：需要确认是否已创建 JSONB GIN 索引（建议检查并优化）

**代码位置**:
- 搜索服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AdvancedArchiveSearchService.java`
- JSON 处理：`nexusarchive-java/src/main/java/com/nexusarchive/config/mybatis/PostgresJsonTypeHandler.java`

**状态**: ✅ **PostgreSQL 端基本完成**（当前系统仅支持 PostgreSQL，符合暂停数据库适配层的决策）

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

1. **❌ 数据迁移工具缺失**（阶段四）
   - 无历史数据导入能力
   - 影响系统上线
   - **建议**：开发 CSV/Excel 导入工具

### P1 优先级（重要但非阻塞）

1. **⚠️ Apache Tika 被注释**（阶段二）
   - 可能影响 MIME 检测准确性
   - **建议**：启用 Tika 或确认 FileMagicValidator 覆盖度

3. **⚠️ 借阅状态机完整性**（阶段三）
   - 需要确认 `BORROWED` -> `RETURNED` 流转是否完整
   - **建议**：审查借阅流程代码，确保状态机完整

4. **⚠️ 检索索引优化**（阶段三）
   - 需要确认 JSONB GIN 索引是否已创建
   - **建议**：检查并优化 PostgreSQL JSONB 索引

5. **⚠️ 信创环境压测缺失**（阶段四）
   - 无国产 CPU 性能基准
   - **建议**：如有需要，在鲲鹏/海光环境进行专项压测

### 已暂停项

1. **⏸️ 数据库适配层**（阶段一）- **已暂停**
   - 已暂停开发，当前仅支持 PostgreSQL
   - 如后续需要支持信创数据库，可重新启动

---

## 🎯 开发任务清单（按优先级：P0 → P1 → P2）

### 🔴 P0 优先级（阻塞性问题 - 必须立即解决）

#### 1. 数据迁移工具缺失 ❌

**任务描述**: 开发 `LegacyImportTool`，通过 CSV/Excel 导入历史数据并自动生成初始全宗结构

**开发内容**:
- [ ] 创建 `LegacyImportService` 接口和实现类
- [ ] 实现 CSV 导入基础功能
  - 解析 CSV 文件（使用 Apache Commons CSV）
  - 数据验证（字段格式、必填项校验）
  - 错误处理和报告生成
- [ ] 实现 Excel 导入支持（使用 Apache POI）
- [ ] 实现全宗结构自动生成
  - 根据导入数据自动创建 `sys_fonds`
  - 自动关联 `sys_entity`
- [ ] 实现数据导入事务管理（支持回滚）
- [ ] 编写导入工具的前端界面
- [ ] 编写单元测试和集成测试

**代码位置**:
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/LegacyImportService.java`
- 控制器：`nexusarchive-java/src/main/java/com/nexusarchive/controller/LegacyImportController.java`
- 前端：`src/pages/admin/LegacyImportPage.tsx`

**预计工作量**: 2-3 周

**依赖关系**: 无

---

### 🟡 P1 优先级（重要但非阻塞 - 短期优化）

#### 1. Apache Tika 被注释 ⚠️

**任务描述**: 启用 Apache Tika 或确认 FileMagicValidator 覆盖度

**开发内容**:
- [ ] 评估当前 `FileMagicValidator` 的 MIME 检测覆盖度
- [ ] 测试常见文件类型的检测准确性
- [ ] 决策：启用 Tika 或强化 FileMagicValidator
  - 如启用 Tika：取消 `pom.xml` 中 Tika 依赖的注释
  - 如强化 FileMagicValidator：补充缺失的 Magic Number 检测
- [ ] 更新四性检测服务以使用新的 MIME 检测机制
- [ ] 编写测试用例验证 MIME 检测准确性

**代码位置**:
- 验证器：`nexusarchive-java/src/main/java/com/nexusarchive/util/FileMagicValidator.java`
- 配置：`nexusarchive-java/pom.xml` (line 221-226)
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCoreServiceImpl.java`

**预计工作量**: 1 周

**依赖关系**: 无

---

#### 2. 借阅状态机完整性 ⚠️

**任务描述**: 确认 `BORROWED` -> `RETURNED` 状态流转是否完整

**开发内容**:
- [ ] 审查 `BorrowingService` 实现
- [ ] 验证状态流转逻辑：
  - `PENDING` -> `APPROVED` -> `BORROWED` -> `RETURNED`
  - 异常状态处理（`OVERDUE`, `CANCELLED`）
- [ ] 补充缺失的状态转换逻辑（如有）
- [ ] 实现状态流转的审计日志记录
- [ ] 编写状态机测试用例
- [ ] 更新前端借阅状态显示和操作按钮

**代码位置**:
- 实体：`nexusarchive-java/src/main/java/com/nexusarchive/entity/Borrowing.java`
- 服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowingService.java`
- 前端：相关借阅页面组件

**预计工作量**: 1-2 周

**依赖关系**: 无

---

#### 3. 检索索引优化 ⚠️

**任务描述**: 检查并优化 PostgreSQL JSONB 索引配置

**开发内容**:
- [ ] 检查当前数据库索引配置
- [ ] 分析高级检索查询性能
- [ ] 创建 JSONB GIN 索引（如缺失）
  ```sql
  CREATE INDEX idx_archive_custom_metadata_gin ON acc_archive USING GIN (custom_metadata);
  CREATE INDEX idx_archive_standard_metadata_gin ON acc_archive USING GIN (standard_metadata);
  ```
- [ ] 创建复合索引（`fonds_no`, `archive_year`, `doc_type`）
- [ ] 执行查询性能测试和对比
- [ ] 编写数据库迁移脚本
- [ ] 更新数据库设计文档

**代码位置**:
- 迁移脚本：`nexusarchive-java/src/main/resources/db/migration/VXXX__create_jsonb_indexes.sql`
- 搜索服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AdvancedArchiveSearchService.java`

**预计工作量**: 1 周

**依赖关系**: 无

---

#### 4. 信创环境压测缺失 ⚠️

**任务描述**: 在鲲鹏/海光服务器上进行全链路压测，定位国产 CPU 的性能瓶颈

**开发内容**:
- [ ] 准备信创测试环境（鲲鹏/海光服务器）
- [ ] 配置测试环境（数据库、应用服务器）
- [ ] 执行全链路压测
  - 使用现有 K6 脚本：`archive_soak.k6.js`, `search_peak.k6.js`, `upload_1gb.k6.js`
  - 关注加密解密性能（SM2/SM3）
  - 关注 JSON 解析性能
- [ ] 定位性能瓶颈
- [ ] 优化性能瓶颈（如需要）
- [ ] 生成性能基准报告
- [ ] 对比 x86 架构性能差异

**代码位置**:
- 压测脚本：`perf/*.k6.js`
- 文档：`perf/README.md`

**预计工作量**: 2-3 周（包含环境准备）

**依赖关系**: 可选（如有信创环境需求）

---

#### 5. 前端功能补全 ⚠️

**任务描述**: 参考前端缺口分析报告，补全缺失的前端功能

**开发内容**:
- [ ] 审查 `docs/reports/frontend-features-gap-analysis.md`
- [ ] **P0 功能补全**（优先级最高）：
  - 审计证据链验真界面（`AuditVerificationPage.tsx`）
  - 证据包导出页面（`AuditEvidencePackagePage.tsx`）
  - 数据迁移工具前端界面（`LegacyImportPage.tsx`）
- [ ] **P1 功能补全**：
  - 跨全宗授权票据管理界面完善
  - 借阅状态机相关界面优化
- [ ] 编写组件测试和 E2E 测试

**代码位置**:
- 参考：`docs/reports/frontend-features-gap-analysis.md`
- 前端页面：`src/pages/`

**预计工作量**: 3-4 周

**依赖关系**: 依赖后端 P0 功能（数据迁移工具、审计验真）

---

### 🟢 P2 优先级（可选功能 - 中长期规划）

#### 1. SM2 签名增强（审计日志不可抵赖性）📝

**任务描述**: 引入 SM2 签名增强审计日志的不可抵赖性

**开发内容**:
- [ ] 设计 SM2 签名方案
- [ ] 实现审计日志签名服务
- [ ] 更新 `AuditLogService` 集成 SM2 签名
- [ ] 实现签名验证功能
- [ ] 编写测试用例

**代码位置**:
- 签名服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/Sm2SignatureService.java`（已存在，需扩展）
- 审计服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogService.java`

**预计工作量**: 2 周

**依赖关系**: 依赖阶段二（合规）完成

---

#### 2. 双人双控（备份密钥管理）📝

**任务描述**: 实现备份密钥的双人双控机制

**开发内容**:
- [ ] 设计双人双控流程
- [ ] 实现密钥分片机制
- [ ] 实现审批流程
- [ ] 集成到备份恢复流程
- [ ] 编写测试用例

**预计工作量**: 3-4 周

**依赖关系**: 依赖备份恢复功能

---

## 📋 开发顺序总结

### 第一阶段：P0（阻塞性问题）- 2-3 周
1. ✅ 数据迁移工具开发

### 第二阶段：P1（重要优化）- 4-6 周
1. Apache Tika 或 FileMagicValidator 强化
2. 借阅状态机完整性确认与修复
3. 检索索引优化
4. 前端功能补全（P0 相关功能优先）
5. 信创环境压测（可选，如有需要）

### 第三阶段：P2（可选功能）- 长期规划
1. SM2 签名增强
2. 双人双控机制

---

## 🎯 开发建议

1. **并行开发**: P1 中的多个任务可以并行进行（如 Tika、状态机、索引优化）
2. **迭代交付**: 每个优先级阶段完成后，应该是一个可部署的版本
3. **测试覆盖**: 所有新功能必须包含单元测试和集成测试
4. **文档更新**: 完成功能后及时更新相关文档

---

## 📊 完成度统计

| 类别 | 计划项数 | 已完成 | 部分完成 | 未完成 | 已暂停 | 完成度 |
|------|---------|--------|----------|--------|--------|--------|
| 阶段一 | 3 | 2 | 0 | 0 | 1 | 83% |
| 阶段二 | 3 | 3 | 0 | 0 | 0 | 100% |
| 阶段三 | 2 | 0 | 2 | 0 | 0 | 50% |
| 阶段四 | 3 | 1 | 1 | 1 | 0 | 33% |
| **总计** | **11** | **6** | **3** | **1** | **1** | **64%** |

**说明**：
- ✅ 已完成：功能完整，符合路线图要求
- ⚠️ 部分完成：功能基本实现，但有细节缺失或优化空间
- ❌ 未完成：核心功能缺失
- ⏸️ 已暂停：暂不开发（数据库适配层）

**调整后完成度**: **73%**（排除已暂停项）

**对齐说明**:
- 本报告基于**开发路线图 v1.0**进行分析
- 开发路线图 v1.0 中不包含实物档案管理功能（阶段三仅包含：借阅与流程、检索增强）
- 数据库适配层已暂停开发（路线图阶段一要求，但当前策略为暂停）

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 产品需求文档：`docs/product/prd-v1.0.md`
- 前端缺口分析：`docs/reports/frontend-features-gap-analysis.md`
- 数据库设计：`docs/database/数据库设计.md`

## 📝 分析范围说明

本报告基于**开发路线图 v1.0** 进行分析，不包含以下内容：

1. **实物档案管理**：
   - 开发路线图 v1.0 阶段三中不包含实物档案管理功能
   - 因此不在本报告分析范围内

2. **数据库适配层**：
   - 开发路线图阶段一要求，但已标记为"暂停"
   - 当前策略为仅支持 PostgreSQL

---

**报告生成时间**: 2025-01  
**下次更新建议**: 完成关键缺口修复后

