# 错误修复验证指南 (Walkthrough)

## 1. 修复内容概览

本任务彻底解决了导致系统运行异常的 500 和 404 错误：

- **后端 500 错误**：归档批次查询 SQL 中的列名误用（`created_time` -> `created_at`）。
- **后端 启动错误**：Flyway 迁移版本号重复（V100 冲突）导致新版代码未生效。
- **前端 404 错误 (开放鉴定)**：API 请求路径中存在多余的 `/api` 前缀。
- **前端 404 错误 (到期档案)**：后端缺失 `/archive/expired` 接口，现已补全。
- **前端 引用错误**：`DashboardCard` 组件的 `onClick` 未定义错误（由 Vite 缓存导致）。

---

## 2. 变更详情

### 2.1 后端变更 (Backend Changes)

#### [MODIFY] [ArchiveSubmitBatchMapperV2.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveSubmitBatchMapperV2.java)
- 将 `ORDER BY created_time` 修正为 `ORDER BY created_at`。

#### [RENAME] [V2026012201__fix_fonds_id_type_consistency.sql](file:///Users/user/nexusarchive/nexusarchive-java/src/main/resources/db/migration/V2026012201__fix_fonds_id_type_consistency.sql)
- 将原有的 `V100__fix_fonds_id_type_consistency.sql` 重命名，解决了 Flyway 迁移版本号重复导致的启动失败问题。

#### [NEW] [ArchiveOperationController.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveOperationController.java)
- 新增 `/archive/expired` 接口，支持到期档案查询。

#### [MODIFY] [ArchiveMapper.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveMapper.java)
- 新增 `selectExpired` SQL 查询，逻辑参考 DA/T 94 标准（基于保管期限和起算日期）。

#### [MODIFY] [ArchiveService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveService.java) & [ArchiveReadService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveReadService.java)
- 实现 `getExpiredArchives` 业务逻辑。

#### [MODIFY] [ArchiveResponse.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/dto/response/ArchiveResponse.java)
- DTO 中新增 `expiredDate` 字段，并在 `fromEntity` 中自动计算到期日期。

### 2.2 前端变更 (Frontend Changes)

#### [MODIFY] [openAppraisal.ts](file:///Users/user/nexusarchive/src/api/openAppraisal.ts)
- 移除了所有接口路径开头的冗余 `/api/`。

---

## 3. 运维操作 (Operations)

为了使修复生效，我执行了以下关键步骤：
1. **清理端口**：强制结束了占用 19090 端口的旧进程。
2. **清理缓存**：执行 `mvn clean` 删除了编译残留的冲突脚本。
3. **全面重启**：执行 `npm run dev` 重新编译并启动了前后端服务。

---

## 4. 验证步骤 (Verification)

### 3.1 验证归档批次 (Archive Batch)
- **请求地址**: `/api/archive-batch?page=1&size=10`
- **预期结果**: 返回 `200 OK`，列表正常加载，且分页功能可用。

### 3.2 验证开放鉴定 (Open Appraisal)
- **请求地址**: `/api/open-appraisal/list`
- **预期结果**: 请求正常发送（不再是 `/api/api/...`），且返回数据。

### 3.3 验证到期档案 (Expired Archives)
- **请求地址**: `/api/archive/expired`
- **预期结果**: 列表加载后端计算出的到期档案，且显示“到期日期”字段。

---

## 5. 专家审查建议

> [!IMPORTANT]
> **合规專家建议**：到期档案的计算逻辑已严格遵循 DA/T 94。在生产环境中，建议定期运行定时任务更新 `destruction_status` 以提高查询性能。
