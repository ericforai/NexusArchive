# 2025-12-18 Phase 4 大规模性能调优与集成加固

本文档记录了 Phase 4 阶段关于大规模数据性能调优、集成中心加固、合规监控等核心功能的开发实施。

---

## 1. 核心架构变更

### 1.1 并行对账引擎 (Parallel Reconciliation Engine)

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ReconciliationServiceImpl.java`

**设计要点**:
- 使用 `ExecutorService` 创建固定大小线程池 (最多8线程)
- 按月份切分时间范围，实现分片并行对账
- 使用 `CompletableFuture.supplyAsync()` 实现异步任务调度
- 批量 `IN` 查询替代 N+1 查询，显著降低数据库 IO

```java
// 线程池配置
private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.min(Runtime.getRuntime().availableProcessors(), 8));

// 按月分片
List<YearMonth> periods = new ArrayList<>();
YearMonth start = YearMonth.from(startDate);
while (!start.isAfter(end)) {
    periods.add(start);
    start = start.plusMonths(1);
}

// 并行执行
List<CompletableFuture<PeriodResult>> futures = periods.stream()
    .map(period -> CompletableFuture.supplyAsync(() -> {
        // 各月份独立核对逻辑
    }, executorService))
    .collect(Collectors.toList());
```

**性能预期**: 12个月对账任务理论耗时缩短至 1/3。

---

### 1.2 异步归档流水线 (Async Archival Pipeline)

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java`

**设计要点**:
- 引入 `ARCHIVING` (归档处理中) 中间状态，实现状态机锁定
- 使用专用线程池 `archivalExecutor` (4线程) 处理高耗时操作
- 实现失败补偿机制：异常时自动回滚状态至 `PENDING_ARCHIVE`

```java
// 异步归档线程池
private final ExecutorService archivalExecutor = Executors.newFixedThreadPool(4);

// 状态机锁定
for (String id : poolItemIds) {
    ArcFileContent file = arcFileContentMapper.selectById(id);
    if (file != null && !PreArchiveStatus.ARCHIVED.getCode().equals(file.getPreArchiveStatus())) {
        file.setPreArchiveStatus(PreArchiveStatus.ARCHIVING.getCode());
        arcFileContentMapper.updateById(file);
    }
}

// 提交异步任务
archivalExecutor.submit(() -> {
    try {
        performArchivingTask(poolItemIds, userId);
    } catch (Exception e) {
        // 补偿：恢复状态为待归档
        file.setPreArchiveStatus(PreArchiveStatus.PENDING_ARCHIVE.getCode());
        arcFileContentMapper.updateById(file);
    }
});
```

**合规依据**: 状态机锁定机制满足专家组关于"数据一致性"的红线要求。

---

### 1.3 ERP 映射引擎 (ErpMappingEngine)

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/engine/ErpMappingEngine.java`

**功能特性**:
| 映射类型 | 语法示例 | 说明 |
|---------|---------|------|
| 直接映射 | `billNo` | 从 JSON 取值 |
| 常量定义 | `CONST:AC01` | 固定常量 |
| 模板字符串 | `凭证-${billNo}` | 变量替换 |
| 表达式转换 | `${date:substring(0,4)}` | 子串截取 |

**核心方法**:
- `mapToArchive(JSONObject, JSONObject)` — 转换为 Archive 实体
- `mapToArcFileContent(JSONObject, JSONObject)` — 转换为预归档实体

---

## 2. 合规增强

### 2.1 PreArchiveStatus 枚举扩展

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/entity/enums/PreArchiveStatus.java`

新增状态：
```java
ARCHIVING("ARCHIVING", "归档处理中")
```

完整状态流转：
```
PENDING_CHECK → CHECK_FAILED / PENDING_METADATA / PENDING_ARCHIVE
                                              ↓
                                    PENDING_APPROVAL
                                              ↓ (审批通过)
                                         ARCHIVING
                                              ↓ (异步完成)
                                          ARCHIVED
```

### 2.2 SyncHistory 审计增强

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/entity/SyncHistory.java`

新增字段：
| 字段 | 类型 | 说明 |
|------|------|------|
| `operatorId` | Long | 操作人ID |
| `clientIp` | String | 操作客户端IP |
| `fourNatureSummary` | String | JSON格式四性检测摘要 |

**合规依据**: 满足 GB/T 39362 对采集过程的审计追溯要求。

### 2.3 存证反馈机制

**接口**: `ErpAdapter.feedbackArchivalStatus(ErpConfig, voucherNo, archivalCode, status)`

**YonSuite 实现**: `YonSuiteErpAdapter.feedbackArchivalStatus()` 调用 `YonSuiteClient.feedbackArchivalStatus()` 将归档状态回写 ERP。

---

## 3. 数据库迁移

| 版本 | 文件 | 内容 |
|------|------|------|
| V59 | `V59__integration_audit_enhancement.sql` | sys_sync_history 添加 operator_id, client_ip 审计字段 |
| V60 | `V60__integration_templates_and_sub_interfaces.sql` | ERP 配置模板预置 (离线可用) |
| V61 | `V61__sync_history_compliance_enhancement.sql` | 添加 four_nature_summary 字段 |
| V62 | `V62__reconciliation_engine_schema.sql` | 创建 arc_reconciliation_record 对账记录表 |

---

## 4. 前端组件

### 4.1 合规雷达图 (ComplianceRadar)

**文件**: `src/components/common/ComplianceRadar.tsx`

基于 recharts 实现的四性检测可视化组件：
- 真实性 (Authenticity)
- 完整性 (Integrity)
- 可用性 (Usability)
- 安全性 (Security)

### 4.2 对账报告组件 (ReconciliationReport)

**文件**: `src/components/common/ReconciliationReport.tsx`

展示"账、凭、证"三位一体对账结果：
- ERP 财务账卡片 (发生额、凭证数)
- 系统归档卡片 (入库金额、存档数)
- 原始证据卡片 (附件数、覆盖率进度条)
- 差异明细表格

### 4.3 归档状态显示

**文件**: `src/components/ArchiveListView.tsx`

新增 `ARCHIVING` 状态显示：
```tsx
ARCHIVING: { 
  label: '归档中', 
  color: 'bg-blue-100 text-blue-800', 
  icon: <Loader2 className="animate-spin" />, 
  description: '归档流水线正在异步处理中...' 
}
```

---

## 5. 私有化部署

### 5.1 一键安装脚本

**文件**: `setup.sh`

**功能流程**:
1. 操作系统检测 (Ubuntu/Debian/CentOS/RHEL)
2. 依赖安装 (Java 17, Nginx, PostgreSQL)
3. 数据库初始化 (创建 nexusarchive 库)
4. 安全密钥生成 (SM4_KEY, JWT_SECRET)
5. 环境配置文件生成 (/opt/nexusarchive/.env)

---

## 6. API 端点

### 6.1 监控 API
```
GET /api/monitoring/integration
```
返回：同步成功率、存证覆盖率、每日趋势等

### 6.2 对账 API
```
POST /api/reconciliation/trigger
Body: { configId, subjectCode, startDate, endDate, operatorId }
```
返回：ReconciliationRecord (包含差异报告)

### 6.3 一键诊断 API
```
GET /api/erp/config/{id}/diagnose
```
返回：URL检查、网络连通、SSL证书、业务鉴权等诊断步骤

---

## 7. 问题修复记录

| 问题 | 解决方案 |
|------|---------|
| MonitoringController 404 | 路径从 `/api/monitoring` 改为 `/monitoring` (移除重复前缀) |
| ReconciliationController 404 | 路径从 `/api/reconciliation` 改为 `/reconciliation` |
| IngestFlowTest 编译失败 | 更新构造函数参数，添加 ErpConfigMapper, ErpAdapterFactory, ArchiveSecurityService 依赖 |
| snapshotData JSONB 插入失败 | 临时设为 null (待完善 JacksonTypeHandler 与 PostgreSQL JSONB 绑定) |

---

## 8. 关联文档

- [Phase 4 实施计划](file:///Users/user/nexusarchive/docs/planning/implementation_plan_phase4.md)
- [专家评审报告](file:///Users/user/nexusarchive/docs/planning/expert_review_phase4.md)
- [性能调优方案](file:///Users/user/.gemini/antigravity/brain/1a530434-61b2-49f0-ad8e-35bf7faf4054/implementation_performance_tuning.md)

---

## 9. 存证溯源增强 (ERP Feedback Enhancement)

### 9.1 FeedbackResult DTO

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/FeedbackResult.java`

结构化回写结果对象：
```java
@Data
@Builder
public class FeedbackResult {
    private boolean success;       // 是否成功
    private String voucherId;      // ERP 凭证 ID
    private String archivalCode;   // 生成的档号
    private String erpType;        // ERP 类型
    private LocalDateTime timestamp; // 回写时间
    private String errorMessage;   // 错误信息
    private boolean mocked;        // 是否为模拟执行
}
```

### 9.2 接口签名变更

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/ErpAdapter.java`

```diff
- default boolean feedbackArchivalStatus(ErpConfig config, String voucherNo, 
-         String archivalCode, String status) { return false; }
+ default FeedbackResult feedbackArchivalStatus(ErpConfig config, String voucherNo, 
+         String archivalCode, String status) { 
+     return FeedbackResult.failure(voucherNo, archivalCode, "UNKNOWN", "Not implemented"); 
+ }
```

### 9.3 增强日志输出

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java`

归档完成后输出结构化日志：
```
╔═══════════════════════════════════════════════════════════════╗
║            [存证溯源] 开始回写归档状态至 ERP                    ║
╠═══════════════════════════════════════════════════════════════╣
║ 源系统: YonSuite
║ 凭证号: V-1234567890
║ 档号: QZ001-2025-30Y-FIN-AC01-V0001
╚═══════════════════════════════════════════════════════════════╝
✓ [存证溯源] 回写成功 - voucher=V-1234567890, mocked=true
```

### 9.4 回写失败重试队列

**文件**: `nexusarchive-java/src/main/resources/db/migration/V64__erp_feedback_queue.sql`

```sql
CREATE TABLE sys_erp_feedback_queue (
    id BIGSERIAL PRIMARY KEY,
    voucher_id VARCHAR(64) NOT NULL,
    archival_code VARCHAR(128) NOT NULL,
    erp_type VARCHAR(32) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    last_error TEXT,
    status VARCHAR(16) DEFAULT 'PENDING',  -- PENDING/RETRYING/SUCCESS/FAILED
    next_retry_time TIMESTAMP
);
```

**后续扩展**: Phase 5 可实现定时任务读取此表进行失败重试。

---

## 10. 健康检查脚本

### 10.1 脚本清单

| 文件 | 用途 |
|------|------|
| `setup.sh` (根目录) | 生产环境一键部署 |
| `nexusarchive-java/setup.sh` | 开发环境初始化 |
| `nexusarchive-java/scripts/health_check.sh` | 生产环境健康检查 |

### 10.2 健康检查项

**文件**: `nexusarchive-java/scripts/health_check.sh`

| 检查项 | 说明 |
|--------|------|
| 后端服务 | 检查 `/actuator/health` 返回 `UP` |
| 数据库连接 | 使用 `psql` 执行 `SELECT 1` |
| 磁盘空间 | 确保可用空间 ≥ 10GB |
| 存储目录 | 确认可写权限 |
| 安全配置 | 检查 SM4_KEY、JWT_SECRET 是否设置 |

**使用方法**:
```bash
chmod +x scripts/health_check.sh
./scripts/health_check.sh
```

**输出示例**:
```
====================================================
   NexusArchive 系统健康检查 (Health Check)
====================================================

[1/5] 检查后端服务...
✓ 后端服务正常 (http://localhost:8080)

[2/5] 检查数据库连接...
✓ PostgreSQL 连接正常 (127.0.0.1:5432/nexusarchive)

[3/5] 检查磁盘空间...
✓ 磁盘空间充足: 50GB 可用 (最低要求: 10GB)

[4/5] 检查存储目录...
✓ 存储目录可写: /opt/nexusarchive/storage

[5/5] 检查安全配置...
✓ SM4 加密密钥已配置
✓ JWT 密钥已配置

====================================================
   健康检查通过！ (7 项检查全部通过)
====================================================
```

